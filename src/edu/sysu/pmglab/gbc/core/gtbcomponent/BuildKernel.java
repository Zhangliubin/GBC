package edu.sysu.pmglab.gbc.core.gtbcomponent;

import edu.sysu.pmglab.container.File;
import edu.sysu.pmglab.container.VolumeByteStream;
import edu.sysu.pmglab.container.array.Array;
import edu.sysu.pmglab.container.array.BaseArray;
import edu.sysu.pmglab.easytools.ByteCode;
import edu.sysu.pmglab.gbc.coder.encoder.BEGEncoder;
import edu.sysu.pmglab.gbc.constant.ChromosomeTags;
import edu.sysu.pmglab.gbc.core.common.qualitycontrol.genotype.GenotypeQC;
import edu.sysu.pmglab.gbc.core.common.qualitycontrol.variant.VCFNonGenotypeMarker;
import edu.sysu.pmglab.gbc.core.common.qualitycontrol.variant.VariantQC;
import edu.sysu.pmglab.gbc.core.common.switcher.AMDOFeature;
import edu.sysu.pmglab.gbc.core.exception.GBCWorkFlowException;
import edu.sysu.pmglab.gbc.core.gtbcomponent.gtbreader.Variant;
import edu.sysu.pmglab.gbc.core.gtbcomponent.gtbwriter.GTBOutputParam;
import edu.sysu.pmglab.gbc.core.gtbcomponent.gtbwriter.GTBUncompressedBlock;
import edu.sysu.pmglab.gbc.core.gtbcomponent.gtbwriter.GTBWriter;
import edu.sysu.pmglab.threadPool.Block;
import edu.sysu.pmglab.threadPool.DynamicPipeline;
import edu.sysu.pmglab.threadPool.ThreadPool;
import edu.sysu.pmglab.threadPool.ThreadPoolRuntimeException;
import edu.sysu.pmglab.unifyIO.FileStream;
import edu.sysu.pmglab.unifyIO.partreader.IPartReader;

import java.io.IOException;
import java.util.Objects;

/**
 * @Data :2021/02/14
 * @Author :suranyi
 * @Contact :suranyi.sysu@gamil.com
 * @Description :Build 核心任务，由 BuildTask 调用
 */

class BuildKernel {
    /**
     * 一个 kernel 只能绑定一个任务
     */
    final GTBToolkit.Build task;
    final GTBOutputParam outputParam;

    /**
     * IO 数据管道
     */
    DynamicPipeline<FileStream> uncompressedPipLine;

    /**
     * 文件基本信息
     */
    int validSubjectNum;
    int blockSize;

    /**
     * 位点控制器
     */
    final VariantQC variantQC;
    final GenotypeQC genotypeQC;

    /**
     * 根数据管理器，压缩后的数据在此处进行校验
     */
    final BaseArray<GTBNode> GTBNodeCache = new Array<>(1024, true);
    int maxEstimateSize = 0;

    /**
     * 状态值
     */
    boolean status;

    /**
     * 编码器, 将文本基因型编码为字节编码
     */
    final BEGEncoder begEncoder;
    final GTBWriter.Builder builder;

    int maxAlleleNums;
    boolean splitMultiallelics;

    static final byte[] headerLine = "#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\tFORMAT".getBytes();

    /**
     * 对外的提交方法，将任务提交至本类，进行压缩任务
     */
    static void submit(final GTBToolkit.Build task) throws IOException {
        new BuildKernel(task);
    }

    /**
     * 标准构造器，传入 EditTask，根据该提交任务执行工作
     *
     * @param task 待执行任务
     */
    BuildKernel(final GTBToolkit.Build task) throws IOException {
        // 输出格式
        this.outputParam = task.outputParam;

        // 加载 BEG 编码表和 MBEG 编码表
        this.begEncoder = BEGEncoder.getEncoder(this.outputParam.isPhased());

        // 设置任务
        this.task = task;

        // 位点控制器
        this.variantQC = task.outputParam.getVariantQC();
        this.genotypeQC = task.genotypeQC;
        this.maxAlleleNums = task.outputParam.getMaxAlleleNums();
        this.splitMultiallelics = task.outputParam.isSplitMultiallelics();

        // 开始进行工作
        this.builder = new GTBWriter.Builder(null, this.outputParam);

        startWork();
    }

    void startWork() throws IOException {
        // 创建数据管道
        this.uncompressedPipLine = new DynamicPipeline<>(task.nThreads << 2);

        // 创建分块读取器 (只有一个文件，因此通过 index=0 获取)
        IPartReader partReader = IPartReader.getInstance(this.task.inputFiles.get(0).getAbsoluteFilePath());

        // 检验 VCF 文件的样本名序列是否合法
        checkVcfSubject(partReader);

        // 创建线程池
        ThreadPool threadPool = new ThreadPool(this.task.nThreads + 1);

        // 创建 Input 线程
        threadPool.submit(() -> {
            try {
                for (FileStream fileStream : partReader.part(this.task.nThreads)) {
                    this.uncompressedPipLine.put(true, fileStream);
                }

                // 发送关闭信号
                this.uncompressedPipLine.putStatus(this.task.nThreads, false);
            } catch (IOException | Error e) {
                throw new ThreadPoolRuntimeException(e);
            }
        });

        Array<File> tempFiles = new Array<>(File[].class, this.task.nThreads);
        File outputDir = this.task.outputFile.addExtension(".~$temp");
        outputDir.mkdir();
        outputDir.deleteOnExit();
        threadPool.submit(() -> {
            try {
                File outputFile = outputDir.getSubFile("thread" + Thread.currentThread().getId() + ".gtb");
                outputFile.deleteOnExit();

                if (processFileStream(outputFile)) {
                    synchronized (tempFiles) {
                        tempFiles.add(outputFile);
                    }
                }
            } catch (Exception | Error e) {
                throw new ThreadPoolRuntimeException(e);
            }

        }, this.task.nThreads);

        // 关闭线程池，等待任务完成
        threadPool.close();

        // 清除数据区
        this.uncompressedPipLine.clear();

        // 合并数据文件
        GTBToolkit.Concat.instance(tempFiles.toArray(), this.task.outputFile).submit();

        // 删除输出文件
        outputDir.delete();
    }

    /**
     * 初始化 VCF 文件信息
     */
    void checkVcfSubject(IPartReader partReader) throws IOException {
        // 保存标题模版
        byte[] reference = null;
        VolumeByteStream localLineCache = new VolumeByteStream(2 << 21);

        // 初始化为第一行文本
        partReader.readLine(localLineCache);

        // 遍历文件，将注释信息过滤掉
        while ((localLineCache.cacheOf(0) == ByteCode.NUMBER_SIGN) && (localLineCache.cacheOf(1) == ByteCode.NUMBER_SIGN)) {
            // 检验是否为参考序列地址，如果检测到，则将其保存下来，并在后续一并刷入 gtb 文件
            if (localLineCache.startWith(ByteCode.REFERENCE_STRING) && (reference == null)) {
                // 请注意，当存在两行的 ref 时，也只保留第一行
                reference = localLineCache.cacheOf(ByteCode.REFERENCE_STRING.length, localLineCache.size());
                this.builder.setReference(new VolumeByteStream(reference));
            }
            localLineCache.reset(0);

            // 载入下一行
            partReader.readLine(localLineCache);
        }

        /* 校验标题行 */
        if (!(localLineCache.startWith(headerLine))) {
            throw new GBCWorkFlowException("doesn't match to standard VCF file (#CHROM POS ID REF ALT QUAL FILTER INFO FORMAT <S1 ...>)");
        }

        // 样本名信息
        byte[] subjects;
        if (localLineCache.size() == 45) {
            subjects = new byte[0];
        } else {
            subjects = localLineCache.takeOut(46);
        }

        // 如果第一个模版变量没有数据，则填充
        GroupSubjectFormatter subjectManager = new GroupSubjectFormatter(subjects);
        this.validSubjectNum = subjectManager.subjectNum;

        // 关闭本地行缓冲区
        localLineCache.close();

        // 验证块大小参数
        this.builder.setSubject(subjectManager.subjects);
    }

    /**
     * 读取基因组文件
     */
    boolean processFileStream(File outputFile) throws IOException {
        Block<Boolean, FileStream> fileStreamBlock = this.uncompressedPipLine.get();
        // 确认为需要处理的任务块，只有需要这么一些线程的时候，才会创建容器
        if (fileStreamBlock.getStatus()) {
            // 实例化压缩器
            GTBWriter writer;
            synchronized (builder) {
                writer = builder.setOutputFile(outputFile).build();
            }

            // 创建本地 lineCache 缓冲区
            VolumeByteStream localLineCache = new VolumeByteStream(2 << 20);
            GTBUncompressedBlock uncompressedBlock = writer.getActiveBlock();

            // 创建单个位点缓冲区
            VCFNonGenotypeMarker marker = new VCFNonGenotypeMarker(localLineCache);
            do {
                // 提取要处理的文件块
                FileStream fileReader = fileStreamBlock.getData();

                while (fileReader.readLine(localLineCache) != -1) {
                    // 文件没读完就一直读
                    String chromosome = new String(localLineCache.getNBy(ByteCode.TAB, 0));
                    if (ChromosomeTags.contain(chromosome)) {
                        // 获取其标准染色体名
                        if (uncompressedBlock.full() || !Objects.equals(chromosome, uncompressedBlock.chromosome)) {
                            writer.flush();
                        }

                        if (uncompressedBlock.seek == 0) {
                            uncompressedBlock.chromosome = chromosome;
                        }

                        formatVariant(localLineCache, marker, writer, uncompressedBlock);
                    }
                    localLineCache.reset();
                }

                // 该文件已经被读取完毕
                fileReader.close();

                if (!uncompressedBlock.empty()) {
                    writer.flush();
                }

                // 继续读取下一个文件任务
                fileStreamBlock = this.uncompressedPipLine.get();
            } while (fileStreamBlock.getStatus());

            localLineCache.close();
            writer.close();
            return true;
        }

        return false;
    }

    /**
     * 编码基因型数据
     */
    void formatVariant(VolumeByteStream localLineCache, VCFNonGenotypeMarker marker, GTBWriter writer, GTBUncompressedBlock uncompressedBlock) throws IOException {
        Variant<AMDOFeature> variant = uncompressedBlock.getCurrentVariant();
        marker.update();
        if (!this.variantQC.filter(marker)) {
            return;
        }

        // 更新参考序列信息
        variant.chromosome = uncompressedBlock.chromosome;
        variant.position = marker.getPosition();
        variant.REF = marker.getREF();
        variant.ALT = marker.getALT();
        int alleleNums = variant.getAlternativeAlleleNum();

        if (!splitMultiallelics && alleleNums > this.maxAlleleNums) {
            return;
        }

        if (this.validSubjectNum == 0) {
            // 无需编码基因型
            if (splitMultiallelics && alleleNums > 2) {
                for (Variant subVariant : variant.split()) {
                    if (this.variantQC.filter(subVariant)) {
                        Variant<AMDOFeature> cacheVariant = uncompressedBlock.getCurrentVariant();
                        cacheVariant.chromosome = subVariant.chromosome;
                        cacheVariant.position = variant.position;
                        cacheVariant.REF = subVariant.REF;
                        cacheVariant.ALT = subVariant.ALT;
                        cacheVariant.property.encoderIndex = 0;
                        uncompressedBlock.seek++;

                        if (uncompressedBlock.full()) {
                            writer.flush();
                        }
                    }
                }
            } else {
                if (this.variantQC.filter(variant)) {
                    variant.property.encoderIndex = alleleNums == 2 ? 0 : 1;
                    uncompressedBlock.seek++;
                }
            }
            return;
        }

        // 加载基因型格式化匹配器
        byte[] lineCache = localLineCache.getCache();
        int validLength = localLineCache.size();
        String[] formatter = genotypeQC.load(localLineCache, marker.getFormat());

        /* 根据是否仅有 GT，决定是否需要进行过滤 */
        if (formatter == null) {
            int length;
            int seek = marker.getGenotypeStart();

            for (int i = 0; i < this.validSubjectNum - 1; i++) {
                length = 1;
                seek += 2;

                while (lineCache[seek] != ByteCode.TAB) {
                    seek++;
                    length++;
                }

                variant.BEGs[i] = this.begEncoder.encode(lineCache, seek, length);
            }

            length = validLength - seek - 1;
            seek = validLength;
            variant.BEGs[this.validSubjectNum - 1] = this.begEncoder.encode(lineCache, seek, length);
        } else if (genotypeQC.size() == 0) {
            int length, mark, genotypeLength;
            int seek = marker.getGenotypeStart();

            for (int i = 0; i < this.validSubjectNum - 1; i++) {
                length = 1;
                seek += 2;

                // 当前基因型是否为 .
                if (lineCache[seek - length] == ByteCode.PERIOD) {
                    variant.BEGs[i] = this.begEncoder.encodeMiss();
                    seek = localLineCache.indexOf(ByteCode.TAB, seek);
                } else {
                    // 检测 :
                    while (lineCache[seek] != ByteCode.COLON) {
                        seek++;
                        length++;
                    }

                    mark = seek;
                    genotypeLength = length;

                    // 检测 tab 分隔符
                    while (lineCache[seek] != ByteCode.TAB) {
                        seek++;
                        length++;
                    }

                    variant.BEGs[i] = this.begEncoder.encode(lineCache, mark, genotypeLength);
                }
            }

            // 最后一个基因型数据
            genotypeLength = 1;
            mark = seek + 2;

            // 当前基因型是否为 .
            if (lineCache[mark - genotypeLength] == ByteCode.PERIOD) {
                variant.BEGs[this.validSubjectNum - 1] = this.begEncoder.encodeMiss();
            } else {

                // 检测 :
                while (lineCache[mark] != ByteCode.COLON) {
                    mark++;
                    genotypeLength++;
                }

                variant.BEGs[this.validSubjectNum - 1] = this.begEncoder.encode(lineCache, mark, genotypeLength);
            }
        } else {
            int length, mark, genotypeLength;
            int seek = marker.getGenotypeStart();

            for (int i = 0; i < this.validSubjectNum - 1; i++) {
                length = 1;
                seek += 2;

                // 当前基因型是否为 .
                if (lineCache[seek - length] == ByteCode.PERIOD) {
                    variant.BEGs[i] = this.begEncoder.encodeMiss();
                    seek = localLineCache.indexOf(ByteCode.TAB, seek);
                } else {
                    // 检测 :
                    while (lineCache[seek] != ByteCode.COLON) {
                        seek++;
                        length++;
                    }

                    mark = seek;
                    genotypeLength = length;

                    // 检测 tab 分隔符
                    while (lineCache[seek] != ByteCode.TAB) {
                        seek++;
                        length++;
                    }

                    variant.BEGs[i] = this.begEncoder.encode(!genotypeQC.filter(marker.formatGenotype(seek - length, length, formatter)), lineCache, mark, genotypeLength);
                }
            }

            // 最后一个基因型数据
            genotypeLength = 1;
            mark = seek + 2;

            // 当前基因型是否为 .
            if (lineCache[mark - genotypeLength] == ByteCode.PERIOD) {
                variant.BEGs[this.validSubjectNum - 1] = this.begEncoder.encodeMiss();
            } else {
                length = validLength - seek - 1;
                seek = validLength;

                // 检测 :
                while (lineCache[mark] != ByteCode.COLON) {
                    mark++;
                    genotypeLength++;
                }

                variant.BEGs[this.validSubjectNum - 1] = this.begEncoder.encode(!genotypeQC.filter(marker.formatGenotype(seek - length, length, formatter)), lineCache, mark, genotypeLength);
            }
        }

        if (splitMultiallelics && alleleNums > 2) {
            for (Variant subVariant : variant.split()) {
                if (this.variantQC.filter(subVariant)) {
                    Variant<AMDOFeature> cacheVariant = uncompressedBlock.getCurrentVariant();
                    cacheVariant.chromosome = subVariant.chromosome;
                    cacheVariant.position = variant.position;
                    cacheVariant.REF = subVariant.REF;
                    cacheVariant.ALT = subVariant.ALT;
                    cacheVariant.property.encoderIndex = 0;
                    System.arraycopy(subVariant.BEGs, 0, cacheVariant.BEGs, 0, variant.BEGs.length);
                    uncompressedBlock.seek++;

                    if (uncompressedBlock.full()) {
                        writer.flush();
                    }
                }
            }
        } else {
            if (this.variantQC.filter(variant)) {
                variant.property.encoderIndex = alleleNums == 2 ? 0 : 1;
                uncompressedBlock.seek++;
            }
        }
    }
}



