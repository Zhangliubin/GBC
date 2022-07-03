package edu.sysu.pmglab.gbc.core.gtbcomponent;

import edu.sysu.pmglab.container.File;
import edu.sysu.pmglab.container.Pair;
import edu.sysu.pmglab.container.VolumeByteStream;
import edu.sysu.pmglab.container.array.Array;
import edu.sysu.pmglab.easytools.ArrayUtils;
import edu.sysu.pmglab.easytools.ByteCode;
import edu.sysu.pmglab.easytools.ValueUtils;
import edu.sysu.pmglab.gbc.constant.ChromosomeTags;
import edu.sysu.pmglab.gbc.core.common.qualitycontrol.variant.VCFNonGenotypeMarker;
import edu.sysu.pmglab.gbc.core.common.switcher.AMDOFeature;
import edu.sysu.pmglab.gbc.core.exception.GBCWorkFlowException;
import edu.sysu.pmglab.gbc.core.gtbcomponent.gtbreader.Variant;
import edu.sysu.pmglab.gbc.core.gtbcomponent.gtbwriter.GTBUncompressedBlock;
import edu.sysu.pmglab.gbc.core.gtbcomponent.gtbwriter.GTBWriter;
import edu.sysu.pmglab.threadPool.Block;
import edu.sysu.pmglab.threadPool.DynamicPipeline;
import edu.sysu.pmglab.threadPool.ThreadPool;
import edu.sysu.pmglab.threadPool.ThreadPoolRuntimeException;
import edu.sysu.pmglab.unifyIO.FileStream;
import edu.sysu.pmglab.unifyIO.partreader.IPartReader;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

/**
 * @Data :2021/02/14
 * @Author :suranyi
 * @Contact :suranyi.sysu@gamil.com
 * @Description :Build 核心任务，由 BuildTask 调用
 */

class BuildKernelMultiFile extends BuildKernel {
    /**
     * IO 数据管道
     */
    DynamicPipeline<Pair<FileStream, int[]>> uncompressedPipLine;

    /**
     * 对外的提交方法，将任务提交至本类，进行压缩任务
     */
    static void submit(final GTBToolkit.Build task) throws IOException {
        new BuildKernelMultiFile(task);
    }

    /**
     * 标准构造器，传入 EditTask，根据该提交任务执行工作
     *
     * @param task 待执行任务
     */
    BuildKernelMultiFile(final GTBToolkit.Build task) throws IOException {
        super(task);
    }

    @Override
    void startWork() throws IOException {
        // 创建数据管道
        this.uncompressedPipLine = new DynamicPipeline<>(task.nThreads << 2);

        // 创建分块读取器
        IPartReader[] partReaders = new IPartReader[this.task.inputFiles.size()];
        for (int i = 0; i < partReaders.length; i++) {
            partReaders[i] = IPartReader.getInstance(this.task.inputFiles.get(i).getAbsoluteFilePath());
        }

        // 检验 VCF 文件的样本名序列是否合法
        int[][] relativeIndexes = checkVcfSubjects(partReaders);

        // 创建线程池
        ThreadPool threadPool = new ThreadPool(this.task.nThreads + 1);

        // 创建 Input 线程
        threadPool.submit(() -> {
            try {
                for (int i = 0; i < partReaders.length; i++) {
                    for (FileStream fileStream : partReaders[i].part(this.task.nThreads)) {
                        this.uncompressedPipLine.put(true, new Pair<>(fileStream, relativeIndexes[i]));
                    }
                }

                // 发送关闭信号
                this.uncompressedPipLine.putStatus(this.task.nThreads, false);
            } catch (Exception | Error e) {
                throw new ThreadPoolRuntimeException(e);
            }
        });

        Array<File> tempFiles = new Array<>(File[].class, this.task.nThreads);
        File outputDir = this.task.outputFile.addExtension(".~$temp");
        outputDir.mkdir();
        outputDir.deleteOnExit();
        threadPool.submit(() -> {
            File outputFile = outputDir.getSubFile("thread" + Thread.currentThread().getId() + ".gtb");
            outputFile.deleteOnExit();

            if (processFileStream(outputFile)) {
                synchronized (tempFiles) {
                    tempFiles.add(outputFile);
                }
            }

            return false;
        }, this.task.nThreads);

        // 关闭线程池，等待任务完成
        threadPool.close();

        // 清除数据区
        this.uncompressedPipLine.clear();

        // 合并数据文件
        GTBToolkit.Concat.instance(tempFiles.toArray(), this.task.outputFile).submit();

        // 删除输出的临时文件
        outputDir.delete();
    }

    /**
     * 初始化 VCF 文件信息
     */
    int[][] checkVcfSubjects(IPartReader[] partReaders) throws IOException {
        // 保存标题模版
        byte[] reference = null;
        GroupSubjectFormatter[] subjectsManagers = new GroupSubjectFormatter[this.task.inputFiles.size()];
        int[][] relativeIndexes = new int[subjectsManagers.length][];
        int[] subjectsNum = new int[subjectsManagers.length];
        VolumeByteStream localLineCache = new VolumeByteStream(2 << 21);

        // 记录每个 vcf 文件注释信息长度
        for (int i = 0; i < subjectsManagers.length; i++) {
            // 获取文件句柄
            IPartReader vcfReader = partReaders[i];

            // 初始化为第一行文本
            vcfReader.readLine(localLineCache);

            // 遍历文件，将注释信息过滤掉
            while ((localLineCache.cacheOf(0) == ByteCode.NUMBER_SIGN) && (localLineCache.cacheOf(1) == ByteCode.NUMBER_SIGN)) {
                // 检验是否为参考序列地址，如果检测到，则将其保存下来，并在后续一并刷入 gtb 文件
                if ((reference == null) && localLineCache.startWith(ByteCode.REFERENCE_STRING)) {
                    reference = localLineCache.cacheOf(ByteCode.REFERENCE_STRING.length, localLineCache.size());
                    this.builder.setReference(new VolumeByteStream(reference));
                }
                localLineCache.reset(0);

                // 载入下一行
                vcfReader.readLine(localLineCache);
            }

            /* 校验标题行 */
            if (!localLineCache.startWith(headerLine)) {
                throw new GBCWorkFlowException("doesn't match to standard VCF file (#CHROM POS ID REF ALT QUAL FILTER INFO FORMAT <S1 ...>)");
            }

            // 样本名信息
            byte[] subjects;
            if (localLineCache.size() == 45) {
                subjects = new byte[0];
            } else {
                subjects = localLineCache.takeOut(46);
            }

            if (subjectsManagers[0] == null) {
                // 如果第一个模版变量没有数据，则填充
                subjectsManagers[i] = new GroupSubjectFormatter(subjects, true);
            } else {
                // 检验当前文件的样本序列与之前是否一致
                for (int j = 0; j < i; j++) {
                    if (subjectsManagers[j].equal(subjects)) {
                        subjectsManagers[i] = subjectsManagers[j];
                        break;
                    }
                }

                // 如果都不一致，则设置为新的样本管理器
                if (subjectsManagers[i] == null) {
                    subjectsManagers[i] = new GroupSubjectFormatter(subjects, true);
                }
            }

            subjectsNum[i] = subjectsManagers[i].subjectNum;
        }

        // 关闭本地行缓冲区
        localLineCache.close();

        // 将最长的样本序列作为主样本
        int mainSubjectManagerIndex = ValueUtils.argmax(subjectsNum);
        this.validSubjectNum = subjectsManagers[mainSubjectManagerIndex].subjectNum;

        // 主样本设置为 range(1~n)
        relativeIndexes[mainSubjectManagerIndex] = ArrayUtils.range(this.validSubjectNum - 1);
        subjectsManagers[mainSubjectManagerIndex].relativeIndexes = relativeIndexes[mainSubjectManagerIndex];

        // 检验其他所有的样本名管理器
        for (int i = 0; i < subjectsManagers.length; i++) {
            try {
                if (!subjectsManagers[i].equals(subjectsManagers[mainSubjectManagerIndex])) {
                    relativeIndexes[i] = subjectsManagers[mainSubjectManagerIndex].get(new String(subjectsManagers[i].subjects).split("\t"));
                } else {
                    relativeIndexes[i] = relativeIndexes[mainSubjectManagerIndex];
                }
            } catch (NullPointerException e) {
                throw new GBCWorkFlowException("the subjects in " + this.task.inputFiles.get(i) + " are different from the file's received before, maybe they came from different groups of subjects. Please remove it from inputFileNames.");
            }
        }

        this.builder.setSubject(subjectsManagers[mainSubjectManagerIndex].subjects);
        return relativeIndexes;
    }

    /**
     * 读取基因组文件
     */
    @Override
    boolean processFileStream(File outputFile) throws IOException {
        Block<Boolean, Pair<FileStream, int[]>> fileStreamBlock = this.uncompressedPipLine.get();
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
                FileStream fileReader = fileStreamBlock.getData().key;
                int[] relativeIndexes = fileStreamBlock.getData().value;
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

                        formatVariant(localLineCache, marker, writer, uncompressedBlock, relativeIndexes);
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
    void formatVariant(VolumeByteStream localLineCache, VCFNonGenotypeMarker marker, GTBWriter writer, GTBUncompressedBlock uncompressedBlock, final int[] relativeIndexes) throws IOException {
        Variant<AMDOFeature> variant = uncompressedBlock.getCurrentVariant();
        marker.update();
        if (!this.variantQC.filter(marker)) {
            return;
        }

        // 更新参考序列信息
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

        // 是否填充缺失编码
        if (relativeIndexes.length != validSubjectNum) {
            Arrays.fill(variant.BEGs, this.begEncoder.encodeMiss());
        }

        /* 根据是否仅有 GT，决定是否需要进行过滤 */
        if (formatter == null) {
            int length;
            int seek = marker.getGenotypeStart();
            int subjectIndex;

            for (int i = 0; i < relativeIndexes.length - 1; i++) {
                length = 1;
                seek += 2;
                subjectIndex = relativeIndexes[i];

                while (lineCache[seek] != ByteCode.TAB) {
                    seek++;
                    length++;
                }

                variant.BEGs[subjectIndex] = this.begEncoder.encode(lineCache, seek, length);
            }

            length = validLength - seek - 1;
            seek = validLength;
            variant.BEGs[relativeIndexes[relativeIndexes.length - 1]] = this.begEncoder.encode(lineCache, seek, length);
        } else if (this.genotypeQC.size() == 0) {
            int length, mark, genotypeLength;
            int seek = marker.getGenotypeStart();
            int subjectIndex;
            for (int i = 0; i < relativeIndexes.length - 1; i++) {
                length = 1;
                seek += 2;
                subjectIndex = relativeIndexes[i];

                // 当前基因型是否为 .
                if (lineCache[seek - length] == ByteCode.PERIOD) {
                    variant.BEGs[subjectIndex] = this.begEncoder.encodeMiss();
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

                    variant.BEGs[subjectIndex] = this.begEncoder.encode(lineCache, mark, genotypeLength);
                }
            }

            // 最后一个基因型数据
            genotypeLength = 1;
            mark = seek + 2;

            // 获取样本索引
            subjectIndex = relativeIndexes[relativeIndexes.length - 1];

            // 当前基因型是否为 .
            if (lineCache[mark - genotypeLength] == ByteCode.PERIOD) {
                variant.BEGs[subjectIndex] = this.begEncoder.encodeMiss();
            } else {
                length = validLength - seek - 1;
                seek = validLength;

                // 检测 :
                while (lineCache[mark] != ByteCode.COLON) {
                    mark++;
                    genotypeLength++;
                }

                variant.BEGs[subjectIndex] = this.begEncoder.encode(lineCache, mark, genotypeLength);
            }
        } else {
            int length, mark, genotypeLength;
            int seek = marker.getGenotypeStart();
            int subjectIndex;
            for (int i = 0; i < relativeIndexes.length - 1; i++) {
                length = 1;
                seek += 2;
                subjectIndex = relativeIndexes[i];

                // 当前基因型是否为 .
                if (lineCache[seek - length] == ByteCode.PERIOD) {
                    variant.BEGs[subjectIndex] = this.begEncoder.encodeMiss();
                    seek = localLineCache.indexOf(ByteCode.TAB, seek);
                } else {
                    // 检测 :
                    while (localLineCache.cacheOf(seek) != ByteCode.COLON) {
                        seek++;
                        length++;
                    }

                    mark = seek;
                    genotypeLength = length;
                    // 检测 tab 分隔符
                    while (localLineCache.cacheOf(seek) != ByteCode.TAB) {
                        seek++;
                        length++;
                    }

                    variant.BEGs[subjectIndex] = this.begEncoder.encode(!genotypeQC.filter(marker.formatGenotype(seek - length, length, formatter)), lineCache, mark, genotypeLength);
                }
            }

            // 最后一个基因型数据
            genotypeLength = 1;
            mark = seek + 2;

            // 获取样本索引
            subjectIndex = relativeIndexes[relativeIndexes.length - 1];

            // 当前基因型是否为 .
            if (lineCache[mark - genotypeLength] == ByteCode.PERIOD) {
                variant.BEGs[subjectIndex] = this.begEncoder.encodeMiss();
            } else {
                length = validLength - seek - 1;
                seek = validLength;

                // 检测 :
                while (lineCache[mark] != ByteCode.COLON) {
                    mark++;
                    genotypeLength++;
                }

                variant.BEGs[subjectIndex] = this.begEncoder.encode(!genotypeQC.filter(marker.formatGenotype(seek - length, length, formatter)), lineCache, mark, genotypeLength);
            }
        }

        if (splitMultiallelics && alleleNums > 2) {
            for (Variant subVariant : variant.split()) {
                if (this.variantQC.filter(subVariant)) {
                    Variant<AMDOFeature> cacheVariant = uncompressedBlock.getCurrentVariant();
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