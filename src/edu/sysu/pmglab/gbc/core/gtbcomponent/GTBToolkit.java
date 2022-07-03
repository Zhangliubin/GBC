package edu.sysu.pmglab.gbc.core.gtbcomponent;

import edu.sysu.pmglab.bgztools.BGZToolkit;
import edu.sysu.pmglab.check.Value;
import edu.sysu.pmglab.compressor.IDecompressor;
import edu.sysu.pmglab.container.File;
import edu.sysu.pmglab.container.Pair;
import edu.sysu.pmglab.container.VolumeByteStream;
import edu.sysu.pmglab.container.array.Array;
import edu.sysu.pmglab.container.array.BaseArray;
import edu.sysu.pmglab.container.array.IntArray;
import edu.sysu.pmglab.container.array.StringArray;
import edu.sysu.pmglab.easytools.ArrayUtils;
import edu.sysu.pmglab.easytools.ByteCode;
import edu.sysu.pmglab.easytools.ValueUtils;
import edu.sysu.pmglab.gbc.constant.ChromosomeTags;
import edu.sysu.pmglab.gbc.core.IParallelTask;
import edu.sysu.pmglab.gbc.core.common.allelechecker.AlleleChecker;
import edu.sysu.pmglab.gbc.core.common.allelechecker.AlleleFreqGapTestChecker;
import edu.sysu.pmglab.gbc.core.common.allelechecker.MixChecker;
import edu.sysu.pmglab.gbc.core.common.qualitycontrol.genotype.GenotypeDPController;
import edu.sysu.pmglab.gbc.core.common.qualitycontrol.genotype.GenotypeGQController;
import edu.sysu.pmglab.gbc.core.common.qualitycontrol.genotype.GenotypeQC;
import edu.sysu.pmglab.gbc.core.common.qualitycontrol.genotype.IGenotypeQC;
import edu.sysu.pmglab.gbc.core.common.qualitycontrol.variant.IVariantQC;
import edu.sysu.pmglab.gbc.core.common.qualitycontrol.variant.VariantDPController;
import edu.sysu.pmglab.gbc.core.common.qualitycontrol.variant.VariantMQController;
import edu.sysu.pmglab.gbc.core.common.qualitycontrol.variant.VariantPhredQualityScoreController;
import edu.sysu.pmglab.gbc.core.exception.GBCWorkFlowException;
import edu.sysu.pmglab.gbc.core.gtbcomponent.gtbreader.GTBReader;
import edu.sysu.pmglab.gbc.core.gtbcomponent.gtbreader.LimitPointer;
import edu.sysu.pmglab.gbc.core.gtbcomponent.gtbreader.Variant;
import edu.sysu.pmglab.gbc.core.gtbcomponent.gtbreader.formatter.VCFSiteVariantFormatter;
import edu.sysu.pmglab.gbc.core.gtbcomponent.gtbreader.formatter.VCFVariantFormatter;
import edu.sysu.pmglab.gbc.core.gtbcomponent.gtbreader.formatter.VariantFormatter;
import edu.sysu.pmglab.gbc.core.gtbcomponent.gtbwriter.GTBOutputParam;
import edu.sysu.pmglab.gbc.core.gtbcomponent.gtbwriter.GTBWriter;
import edu.sysu.pmglab.threadPool.Block;
import edu.sysu.pmglab.threadPool.DynamicPipeline;
import edu.sysu.pmglab.threadPool.ThreadPool;
import edu.sysu.pmglab.threadPool.ThreadPoolRuntimeException;
import edu.sysu.pmglab.unifyIO.BGZIPWriterStream;
import edu.sysu.pmglab.unifyIO.FileStream;
import edu.sysu.pmglab.unifyIO.clm.MultiThreadsWriter;
import edu.sysu.pmglab.unifyIO.partwriter.BGZOutputParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * @author suranyi
 * @description GTB 简单工具集
 */

public class GTBToolkit {
    public interface Task {
        /**
         * 提交任务
         */
        boolean submit() throws IOException;

        static void notNull(File file) throws IOException {
            if (file == null) {
                throw new IOException("file cannot be null");
            }
        }

        static void notNull(File[] files) throws IOException {
            if (files.length == 0) {
                throw new IOException("inputFiles not set (files.length == 0)");
            }

            for (File file : files) {
                notNull(file);
            }
        }

        static void fileExists(File file) throws IOException {
            if (!file.isExists()) {
                throw new IOException(file + " not found");
            }
        }

        static void fileExists(File[] files) throws IOException {
            for (File file : files) {
                fileExists(file);
            }
        }
    }

    public static class Build implements Task {
        /**
         * 压缩 vcf 文件为 gtb 文件
         */
        final File outputFile;
        GTBOutputParam outputParam;
        final BaseArray<File> inputFiles;

        int nThreads;

        /**
         * 质控相关方法: 基因型质控 (不满足替换为 .), 位点质控 (是否删除该位点, 位点质控分为前质控和后质控)
         */
        final GenotypeQC genotypeQC = new GenotypeQC();

        /**
         * 构造器
         *
         * @param inputFile  输入文件名，可以是单个文件或文件夹
         * @param outputFile 输出文件名，只能是单个文件名
         */
        public static Build instance(File inputFile, File outputFile) throws IOException {
            return new Build(new File[]{inputFile}, outputFile);
        }

        /**
         * 构造器
         *
         * @param inputFiles 输入文件名，可以是单个文件或文件夹
         * @param outputFile 输出文件名，只能是单个文件名
         */
        public static Build instance(File[] inputFiles, File outputFile) throws IOException {
            return new Build(inputFiles, outputFile);
        }

        /**
         * 构造器
         *
         * @param inputFiles 输入文件名，可以是单个文件或文件夹
         * @param outputFile 输出文件名，只能是单个文件名
         */
        Build(File[] inputFiles, File outputFile) throws IOException {
            this.nThreads = IParallelTask.checkParallel(-1);
            this.outputParam = new GTBOutputParam();
            if (inputFiles == null) {
                throw new GBCWorkFlowException("syntax error: input file is empty");
            }

            if (outputFile == null) {
                throw new GBCWorkFlowException("syntax error: output file is empty");
            }

            // 检验文件是否都存在
            this.inputFiles = new Array<>(File[].class, true);
            for (File inputFile : inputFiles) {
                if (inputFile == null || !inputFile.isExists()) {
                    throw new GBCWorkFlowException("syntax error: " + inputFile + " not found");
                }

                if (inputFile.isDirectory()) {
                    File[] subFiles = inputFile.listFilesDeeply();
                    for (File subFile : subFiles) {
                        if (subFile.withExtension("vcf") || subFile.withExtension("gz")) {
                            if (outputFile.equals(subFile)) {
                                throw new GBCWorkFlowException("syntax error: output file cannot be the same as the input file (" + outputFile + ")");
                            }
                            this.inputFiles.add(subFile);
                        }
                    }
                } else {
                    if (inputFile.withExtension("vcf") || inputFile.withExtension("gz")) {
                        if (outputFile.equals(inputFile)) {
                            throw new GBCWorkFlowException("syntax error: output file cannot be the same as the input file (" + outputFile + ")");
                        }

                        this.inputFiles.add(inputFile);
                    }
                }
            }


            if (this.inputFiles.size() == 0) {
                throw new GBCWorkFlowException("syntax error: no valid VCF files were found");
            }

            this.outputFile = outputFile;
        }

        /**
         * 设置输出格式
         *
         * @param outputParam 输出格式, GTB 输出参数实例
         */
        public Build setOutputParam(GTBOutputParam outputParam) {
            this.outputParam = outputParam == null ? new GTBOutputParam() : outputParam;
            return this;
        }

        /**
         * 不进行质量控制
         */
        public Build controlDisable() {
            clearVariantQC();
            clearGenotypeQC();
            return this;
        }

        /**
         * 设置质控的基因型 DP 阈值
         *
         * @param genotypeQualityControlDp 质控的 DP 值
         */
        public Build setGenotypeQualityControlDp(int genotypeQualityControlDp) {
            return addGenotypeQC(new GenotypeDPController(genotypeQualityControlDp));
        }

        /**
         * 设置质控的基因型 GQ 阈值
         *
         * @param genotypeQualityControlGq 质控的 GQ 值
         */
        public Build setGenotypeQualityControlGq(int genotypeQualityControlGq) {
            return addGenotypeQC(new GenotypeGQController(genotypeQualityControlGq));
        }

        /**
         * 设置质控的位点 DP 阈值
         *
         * @param variantQualityControlDp 质控的 GQ 值
         */
        public Build setVariantQualityControlDp(int variantQualityControlDp) {
            return addVariantQC(new VariantDPController(variantQualityControlDp));
        }

        /**
         * 设置质控的位点 MQ 阈值
         *
         * @param variantQualityControlMq 质控的 MQ 值
         */
        public Build setVariantQualityControlMq(int variantQualityControlMq) {
            return addVariantQC(new VariantMQController(variantQualityControlMq));
        }

        /**
         * 设置质控的位点 QUAL 阈值
         *
         * @param variantPhredQualityScore 质控的 QUAL 阈值
         */
        public Build setVariantPhredQualityScore(int variantPhredQualityScore) {
            return addVariantQC(new VariantPhredQualityScoreController(variantPhredQualityScore));
        }

        /**
         * 添加基因型质量控制器
         *
         * @param genotypeQC 控制器
         */
        public Build addGenotypeQC(IGenotypeQC genotypeQC) {
            this.genotypeQC.add(genotypeQC);
            return this;
        }

        /**
         * 添加位点质量控制器
         *
         * @param variantQC 控制器
         */
        public Build addVariantQC(IVariantQC variantQC) {
            this.outputParam.addVariantQC(variantQC);
            return this;
        }

        /**
         * 清空基因型质量控制器
         */
        public Build clearGenotypeQC() {
            this.genotypeQC.clear();
            return this;
        }

        /**
         * 清空非基因型质量控制器器
         */
        public Build clearVariantQC() {
            this.outputParam.clearVariantQC();
            return this;
        }

        /**
         * 设置并行线程数
         *
         * @param nThreads 线程数
         */
        public Build setThreads(int nThreads) {
            this.nThreads = IParallelTask.checkParallel(nThreads);
            return this;
        }

        @Override
        public boolean submit() throws IOException {
            // 构建核心任务
            if (this.inputFiles.size() > 1) {
                BuildKernelMultiFile.submit(this);
            } else {
                BuildKernel.submit(this);
            }

            return true;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder("Build *.gtb for vcf/vcf.gz files:");
            if (inputFiles.size() > 1) {
                builder.append("\n\tinputFiles: " + inputFiles);
            } else {
                builder.append("\n\tinputFile: " + inputFiles);
            }
            builder.append("\n\toutputFile: " + outputFile);
            builder.append("\n\tthreads: " + nThreads);
            builder.append("\n\t");
            builder.append(this.outputParam.toString().replace("\n", "\n\t"));
            if (this.genotypeQC.size() > 0) {
                builder.append("\n\tgenotype QC: " + this.genotypeQC);
            }
            return builder.toString();
        }
    }

    public static class Subset implements Task {
        /**
         * 并行筛选子集任务 (GTB 文件)
         */
        final GTBManager inputManager;
        final File outputFile;
        GTBOutputParam outputParam;
        String[] subjects;

        /**
         * 优先级: pruner > range > taskPositions > condition
         */
        Function<GTBTree, GTBTree> pruner;
        Function<Variant, Boolean> condition;
        Map<String, int[]> positions;
        Map<String, int[]> ranges;
        int nThreads;

        /**
         * 构造器
         *
         * @param inputFile  输入文件名, 只能是单个文件名
         * @param outputFile 输出文件名，只能是单个文件名
         */
        Subset(File inputFile, File outputFile) throws IOException {
            this.inputManager = GTBRootCache.get(inputFile);
            this.outputFile = outputFile;
            this.outputParam = new GTBOutputParam(inputFile);
            this.subjects = inputManager.getAllSubjects();
            this.pruner = null;
            this.condition = variant -> true;
            this.positions = null;
            this.ranges = null;
            this.nThreads = IParallelTask.checkParallel(-1);
        }

        /**
         * 构造器
         *
         * @param inputFile  输入文件名, 只能是单个文件名
         * @param outputFile 输出文件名，只能是单个文件名
         */
        public static Subset instance(File inputFile, File outputFile) throws IOException {
            Task.notNull(inputFile);
            Task.notNull(outputFile);
            Task.fileExists(inputFile);

            return new Subset(inputFile, outputFile);
        }

        /**
         * 设置输出格式
         *
         * @param outputParam 输出格式, GTB 输出参数实例
         */
        public Subset setOutputParam(GTBOutputParam outputParam) throws IOException {
            this.outputParam = outputParam == null ? new GTBOutputParam(inputManager) : outputParam;
            return this;
        }

        /**
         * 筛选样本子集
         *
         * @param subjects 子样本序列
         */
        public Subset setSubjects(String[] subjects) {
            this.subjects = subjects == null ? inputManager.getAllSubjects() : subjects;
            return this;
        }

        /**
         * 设置修剪器, 将 当前 GTBTree 修剪为另一 GTBTree
         *
         * @param pruner 修剪器
         */
        public Subset setPruner(Function<GTBTree, GTBTree> pruner) {
            this.pruner = pruner;
            return this;
        }

        /**
         * 设置位点过滤条件
         *
         * @param condition 位点过滤条件
         */
        public Subset setCondition(Function<Variant, Boolean> condition) {
            this.condition = condition == null ? variant -> true : condition;
            return this;
        }

        /**
         * 筛选位点子集 (随机访问)
         *
         * @param positions 位点子集, 当 value = null 时表示选中对应染色体的所有位点
         */
        public Subset setPositions(Map<String, int[]> positions) {
            this.positions = positions == null ? null : Collections.unmodifiableMap(positions);
            return this;
        }

        /**
         * 筛选位点子集 (位点范围)
         *
         * @param ranges 位点子集, 当 value = null 时表示选中对应染色体的所有位点, 否则为 start-end
         */
        public Subset setRanges(Map<String, int[]> ranges) {
            if (ranges != null) {
                for (String chromosome : ranges.keySet()) {
                    if (ranges.get(chromosome) != null && ranges.get(chromosome).length != 2) {
                        throw new GBCWorkFlowException("the range of position not in 'null' (means all variants of specified chromosome) or 'int[]{start, end}'");
                    }
                }
            }

            this.ranges = ranges == null ? null : Collections.unmodifiableMap(ranges);
            return this;
        }

        /**
         * 设置并行线程数
         *
         * @param nThreads 线程数
         */
        public Subset setThreads(int nThreads) {
            this.nThreads = IParallelTask.checkParallel(nThreads);
            return this;
        }

        @Override
        public boolean submit() throws IOException {
            if (pruner != null) {
                GTBTree tree = pruner.apply(inputManager.getGtbTree().clone());
                inputManager.getGtbTree().clear();
                inputManager.getGtbTree().add(tree);
            }

            if (ranges != null) {
                inputManager.getGtbTree().retain(ArrayUtils.getStringKey(ranges));
            }

            if (positions != null) {
                inputManager.getGtbTree().retain(ArrayUtils.getStringKey(positions));
            }

            // 设置默认条件
            final boolean phased = outputParam.isPhased();
            GTBWriter.Builder builder = new GTBWriter.Builder(outputParam)
                    .setReference(inputManager.getReferenceManager().getReference())
                    .setSubject(subjects);

            // 并行
            ThreadPool pool = new ThreadPool(nThreads);
            BaseArray<File> tempFiles = new Array<>(File[].class, nThreads);

            // 创建临时文件夹
            File tempDir = outputFile.addExtension(".~$temp");
            tempDir.mkdir();
            tempDir.deleteOnExit();

            // 计算所有节点总数
            int length = (int) Math.ceil((double) inputManager.getGtbTree().numOfNodes() / nThreads);
            AtomicInteger offset = new AtomicInteger(0);

            pool.submit(() -> {
                try {
                    GTBReader reader = new GTBReader(inputManager, phased);
                    reader.selectSubjects(subjects);
                    GTBWriter writer;

                    synchronized (offset) {
                        if (!reader.limit(offset.get(), length)) {
                            reader.close();
                            return;
                        }
                        File tempFile = tempDir.getSubFile("node" + offset.get() + "-" + (offset.get() + length) + ".gtb");
                        tempFile.deleteOnExit();
                        tempFiles.add(tempFile);
                        writer = builder.setOutputFile(tempFile).build();
                        offset.addAndGet(length);
                    }

                    // 获取限定指针
                    LimitPointer pointer = (LimitPointer) reader.tell();
                    String[] chromosomeList = reader.getChromosomeList();

                    if (chromosomeList.length > 0) {
                        String markStartChromosome = chromosomeList[0];
                        String markEndChromosome = chromosomeList[chromosomeList.length - 1];
                        int markStartNode = pointer.getStartNodeIndex();
                        int markEndNode = pointer.getEndNodeIndex();

                        Variant variant = new Variant();
                        for (String chromosome : chromosomeList) {
                            reader.limit(chromosome, chromosome.equals(markStartChromosome) ? markStartNode : 0, chromosome.equals(markEndChromosome) ? markEndNode : Integer.MAX_VALUE);

                            GTBNodes nodes = this.inputManager.getGTBNodes(chromosome);
                            if ((this.ranges != null && !this.ranges.containsKey(chromosome)) || this.positions != null && !this.positions.containsKey(chromosome)) {
                                continue;
                            }

                            int minPos = Integer.MAX_VALUE;
                            int maxPos = Integer.MIN_VALUE;

                            if (reader.searchEnable(chromosome)) {
                                // 有序文件
                                minPos = nodes.get(reader.getStartNodeIndex(chromosome)).minPos;
                                maxPos = nodes.get(reader.getEndNodeIndex(chromosome)).maxPos;
                            } else {
                                for (int nodeIndex = reader.getStartNodeIndex(chromosome), l = reader.getEndNodeIndex(chromosome); nodeIndex <= l; nodeIndex++) {
                                    GTBNode node = nodes.get(nodeIndex);
                                    if (node.minPos < minPos) {
                                        minPos = node.minPos;
                                    }

                                    if (node.maxPos > maxPos) {
                                        maxPos = node.maxPos;
                                    }
                                }
                            }

                            // 根据范围执行快速过滤
                            BaseArray<Integer> position = null;

                            if (this.ranges != null && this.ranges.containsKey(chromosome) && this.ranges.get(chromosome) != null) {
                                minPos = Math.max(this.ranges.get(chromosome)[0], minPos);
                                maxPos = Math.min(this.ranges.get(chromosome)[1], maxPos);
                            }

                            if (this.positions != null && this.positions.containsKey(chromosome) && this.positions.get(chromosome) != null) {
                                int finalMinPos = minPos;
                                int finalMaxPos = maxPos;
                                position = new IntArray(this.positions.get(chromosome)).filter(pos -> pos >= finalMinPos && pos <= finalMaxPos);
                                if (position.size() == 0) {
                                    continue;
                                }

                                // 去重排序
                                position.dropDuplicated();
                                position.sort();

                                minPos = Math.max(position.get(0), minPos);
                                maxPos = Math.min(position.get(-1), maxPos);
                            }

                            if (minPos > maxPos) {
                                continue;
                            }

                            // 开始搜索
                            if (reader.searchEnable(chromosome)) {
                                if (position != null) {
                                    // 此时只需要以 position 为准
                                    while (position.size() > 0) {
                                        if (reader.search(chromosome, position.popFirst())) {
                                            for (Variant v : reader.readVariants()) {
                                                if (condition.apply(v)) {
                                                    writer.write(v);
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    reader.search(chromosome, minPos);
                                    while (reader.readVariant(variant)) {
                                        if (variant.position > maxPos) {
                                            break;
                                        }

                                        if (condition.apply(variant)) {
                                            writer.write(variant);
                                        }
                                    }
                                }
                            } else {
                                if (position != null) {
                                    // 将 position 转为 set
                                    Set<Integer> sets = position.toSet();
                                    while (reader.readVariant(variant, sets)) {
                                        if (condition.apply(variant)) {
                                            writer.write(variant);
                                        }
                                    }
                                } else {
                                    while (reader.readVariant(variant)) {
                                        if (variant.position >= minPos && variant.position <= maxPos && condition.apply(variant)) {
                                            writer.write(variant);
                                        }
                                    }
                                }
                            }
                        }
                    }

                    writer.close();
                    reader.close();
                } catch (Error | Exception e) {
                    throw new ThreadPoolRuntimeException(e);
                }
            }, nThreads);

            pool.close();

            Concat.instance(tempFiles.toArray(), outputFile).submit();
            tempDir.delete();

            if (pruner != null || ranges != null || positions != null) {
                // 触发了剪枝时清除缓冲数据
                GTBRootCache.clear(inputManager);
            }
            return true;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder("Retrieve variants from *.gtb file:");
            builder.append("\n\tinputFile: " + this.inputManager.getFile());
            builder.append("\n\toutputFile: " + this.outputFile);
            builder.append("\n\tthreads: " + this.nThreads);
            builder.append("\n\toutput format: GTB");
            builder.append("\n\t");
            builder.append(this.outputParam.toString().replace("\n", "\n\t"));
            if (this.subjects != null) {
                builder.append("\n\tselect subjects: " + StringArray.wrap(this.subjects).toString(5));
            }

            if (this.ranges != null) {
                builder.append("\n\trange of position: ");

                if (this.ranges.size() > 0) {
                    StringArray temp = new StringArray();
                    for (String chromosome : this.ranges.keySet()) {
                        if (this.ranges.get(chromosome) == null) {
                            temp.add("chr" + chromosome);
                        } else {
                            temp.add("chr" + chromosome + ":" + this.ranges.get(chromosome)[0] + "-" + this.ranges.get(chromosome)[1]);
                        }
                    }
                    builder.append(temp.join(", "));
                } else {
                    builder.append("<empty>");
                }
            }

            if (positions != null) {
                builder.append("\n\tspecific position: ");

                if (this.positions.size() > 0) {
                    StringArray temp = new StringArray();
                    for (String chromosome : this.positions.keySet()) {
                        if (this.positions.get(chromosome) == null) {
                            temp.add("chr" + chromosome);
                        } else {
                            temp.add("chr" + chromosome + ":" + IntArray.wrap(this.positions.get(chromosome)).toString(5));
                        }
                    }
                    builder.append(temp.join(", "));
                } else {
                    builder.append("<empty>");
                }
            }

            return builder.toString();
        }
    }

    public static class Formatter implements Task {
        /**
         * 格式化数据, 包括位点注释的并行化调用
         */

        final GTBManager inputManager;
        final File outputFile;
        BGZOutputParam outputParam;
        String[] subjects;
        boolean phased;
        boolean clm;

        /**
         * 优先级: pruner > range > taskPositions > condition
         */
        Function<GTBTree, GTBTree> pruner;
        Function<Variant, Boolean> condition;
        Map<String, int[]> positions;
        Map<String, int[]> ranges;
        FileFormatter formatter;

        int nThreads;

        public static class FileFormatter {
            Function<Formatter, byte[]> header;
            Function<Formatter, byte[]> tailer;
            VariantFormatter<?, VolumeByteStream> variantFormatter;

            public FileFormatter(Function<Formatter, byte[]> header, Function<Formatter, byte[]> tailer, VariantFormatter<?, VolumeByteStream> formatter) {
                this.header = header;
                this.tailer = tailer;
                this.variantFormatter = formatter;
            }

            public static final FileFormatter VCFFormat = new FileFormatter(formatter -> {
                VolumeByteStream cache = new VolumeByteStream();
                cache.writeSafety(("##fileformat=VCFv4.2" +
                        "\n##FILTER=<ID=PASS,Description=\"All filters passed\">" +
                        "\n##source=" + formatter.inputManager.getFile() +
                        "\n##Version=<gbc_version=1.1,java_version=" + System.getProperty("java.version") + ",zstd_jni=1.4.9-5>"));
                for (String chromosome : formatter.inputManager.getChromosomeList()) {
                    cache.writeSafety(ChromosomeTags.getHeader(chromosome));
                }
                cache.writeSafety("\n##INFO=<ID=AC,Number=A,Type=Integer,Description=\"Allele count in genotypes\">" +
                        "\n##INFO=<ID=AN,Number=1,Type=Integer,Description=\"Total number of alleles in called genotypes\">" +
                        "\n##INFO=<ID=AF,Number=A,Type=Float,Description=\"Allele Frequency\">" +
                        "\n##FORMAT=<ID=GT,Number=1,Type=String,Description=\"Genotype\">" +
                        "\n#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\tFORMAT");

                for (String subject : formatter.subjects) {
                    cache.writeSafety(ByteCode.TAB);
                    cache.writeSafety(subject);
                }
                return cache.values();
            }, null, new VariantFormatter<Object, VolumeByteStream>() {
                @Override
                public int apply(Variant variant, VolumeByteStream cache) {
                    cache.write(ByteCode.NEWLINE);
                    return VCFVariantFormatter.INSTANCE.apply(variant, cache);
                }
            }) {
                @Override
                public String toString() {
                    return "VCF";
                }
            };

            public static final FileFormatter VCFFormat_WithoutGenotype = new FileFormatter(formatter -> {
                VolumeByteStream cache = new VolumeByteStream();
                cache.writeSafety(("##fileformat=VCFv4.2" +
                        "\n##FILTER=<ID=PASS,Description=\"All filters passed\">" +
                        "\n##source=" + formatter.inputManager.getFile() +
                        "\n##Version=<gbc_version=1.1,java_version=" + System.getProperty("java.version") + ",zstd_jni=1.4.9-5>"));
                for (String chromosome : formatter.inputManager.getChromosomeList()) {
                    cache.writeSafety(ChromosomeTags.getHeader(chromosome));
                }
                cache.writeSafety("\n##INFO=<ID=AC,Number=A,Type=Integer,Description=\"Allele count in genotypes\">" +
                        "\n##INFO=<ID=AN,Number=1,Type=Integer,Description=\"Total number of alleles in called genotypes\">" +
                        "\n##INFO=<ID=AF,Number=A,Type=Float,Description=\"Allele Frequency\">" +
                        "\n##FORMAT=<ID=GT,Number=1,Type=String,Description=\"Genotype\">" +
                        "\n#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\tFORMAT");

                return cache.values();
            }, null, new VariantFormatter<Object, VolumeByteStream>() {
                @Override
                public int apply(Variant variant, VolumeByteStream cache) {
                    cache.write(ByteCode.NEWLINE);
                    return VCFSiteVariantFormatter.INSTANCE.apply(variant, cache);
                }
            }) {
                @Override
                public String toString() {
                    return "VCF";
                }
            };
        }

        /**
         * 构造器
         *
         * @param inputFile  输入文件名, 只能是单个文件名
         * @param outputFile 输出文件名，只能是单个文件名
         */
        Formatter(File inputFile, File outputFile) {
            this.inputManager = GTBRootCache.get(inputFile);
            this.outputFile = outputFile;
            this.outputParam = this.outputFile.withExtension(".gz") ? new BGZOutputParam() : null;
            this.subjects = inputManager.getAllSubjects();
            this.phased = this.inputManager.isPhased();
            this.pruner = null;
            this.condition = variant -> true;
            this.positions = null;
            this.ranges = null;
            this.nThreads = IParallelTask.checkParallel(-1);
            this.formatter = FileFormatter.VCFFormat;
            this.clm = true;
        }

        /**
         * 构造器
         *
         * @param inputFile  输入文件名, 只能是单个文件名
         * @param outputFile 输出文件名，只能是单个文件名
         */
        public static Formatter instance(File inputFile, File outputFile) throws IOException {
            Task.notNull(inputFile);
            Task.notNull(outputFile);
            Task.fileExists(inputFile);

            return new Formatter(inputFile, outputFile);
        }

        /**
         * 设置输出格式
         *
         * @param outputParam 输出格式, GTB 输出参数实例
         */
        public Formatter setOutputParam(BGZOutputParam outputParam) {
            this.outputParam = outputParam;
            return this;
        }

        /**
         * 设置基因型的向型
         *
         * @param phased 基因型向型
         */
        public Formatter setPhased(boolean phased) {
            this.phased = phased;
            return this;
        }

        /**
         * 筛选样本子集
         *
         * @param subjects 子样本序列
         */
        public Formatter setSubjects(String[] subjects) {
            this.subjects = subjects == null ? inputManager.getAllSubjects() : subjects;
            return this;
        }

        /**
         * 设置修剪器, 将 当前 GTBTree 修剪为另一 GTBTree
         *
         * @param pruner 修剪器
         */
        public Formatter setPruner(Function<GTBTree, GTBTree> pruner) {
            this.pruner = pruner;
            return this;
        }

        /**
         * 设置位点过滤条件
         *
         * @param condition 位点过滤条件
         */
        public Formatter setCondition(Function<Variant, Boolean> condition) {
            this.condition = condition == null ? variant -> true : condition;
            return this;
        }

        /**
         * 筛选位点子集 (随机访问)
         *
         * @param positions 位点子集, 当 value = null 时表示选中对应染色体的所有位点
         */
        public Formatter setPositions(Map<String, int[]> positions) {
            this.positions = positions == null ? null : Collections.unmodifiableMap(positions);
            return this;
        }

        /**
         * 筛选位点子集 (位点范围)
         *
         * @param ranges 位点子集, 当 value = null 时表示选中对应染色体的所有位点, 否则为 start-end
         */
        public Formatter setRanges(Map<String, int[]> ranges) {
            if (ranges != null) {
                for (String chromosome : ranges.keySet()) {
                    if (ranges.get(chromosome) != null && ranges.get(chromosome).length != 2) {
                        throw new GBCWorkFlowException("the range of position not in 'null' (means all variants of specified chromosome) or 'int[]{start, end}'");
                    }
                }
            }

            this.ranges = ranges == null ? null : Collections.unmodifiableMap(ranges);
            return this;
        }

        /**
         * 设置并行线程数
         *
         * @param nThreads 线程数
         */
        public Formatter setThreads(int nThreads) {
            this.nThreads = IParallelTask.checkParallel(nThreads);
            return this;
        }

        /**
         * 设置位点格式化器 (可能需要包含 \n)
         *
         * @param formatter 位点格式化器
         */
        public Formatter setFileFormat(FileFormatter formatter) {
            if (formatter == null) {
                throw new GBCWorkFlowException("no formatter set");
            }
            this.formatter = formatter;
            return this;
        }

        /**
         * 使用 CLM 算法 (不输出临时文件)
         *
         * @param clm 使用 clm 算法
         */
        public Formatter setCLM(boolean clm) {
            this.clm = clm;
            return this;
        }

        @Override
        public boolean submit() throws IOException {
            if (pruner != null) {
                GTBTree tree = pruner.apply(inputManager.getGtbTree().clone());
                inputManager.getGtbTree().clear();
                inputManager.getGtbTree().add(tree);
            }

            if (ranges != null) {
                inputManager.getGtbTree().retain(ArrayUtils.getStringKey(ranges));
            }

            if (positions != null) {
                inputManager.getGtbTree().retain(ArrayUtils.getStringKey(positions));
            }

            if (this.clm) {
                processUseCLM();
            } else {
                process();
            }

            if (pruner != null || ranges != null || positions != null) {
                GTBRootCache.clear(inputManager);
            }

            return true;
        }

        private void processUseCLM() throws IOException {
            // 并行
            ThreadPool pool = new ThreadPool(nThreads + 1);
            final MultiThreadsWriter writer = outputParam == null ? new MultiThreadsWriter(outputFile, nThreads) : new MultiThreadsWriter(outputFile, outputParam, nThreads);

            // 添加 header 信息
            if (this.formatter.header != null) {
                byte[] data = this.formatter.header.apply(this);
                if (data.length > 0) {
                    writer.write(data);
                }
            }

            DynamicPipeline<TaskGTBNode> taskPipLine = new DynamicPipeline<>(nThreads << 2);
            pool.submit(() -> {
                // 发送具有特定任务的节点信息
                for (String chromosome : inputManager.getChromosomeList()) {
                    if ((this.ranges != null && !this.ranges.containsKey(chromosome)) || (this.positions != null && !this.positions.containsKey(chromosome))) {
                        continue;
                    }

                    int minPos = Integer.MAX_VALUE;
                    int maxPos = Integer.MIN_VALUE;

                    GTBNodes nodes = inputManager.getGTBNodes(chromosome);
                    for (int nodeIndex = 0, l = nodes.numOfNodes(); nodeIndex < l; nodeIndex++) {
                        GTBNode node = nodes.get(nodeIndex);
                        if (node.minPos < minPos) {
                            minPos = node.minPos;
                        }

                        if (node.maxPos > maxPos) {
                            maxPos = node.maxPos;
                        }
                    }

                    // 根据范围执行快速过滤
                    BaseArray<Integer> position = null;

                    if (this.ranges != null && this.ranges.containsKey(chromosome) && this.ranges.get(chromosome) != null) {
                        minPos = Math.max(this.ranges.get(chromosome)[0], minPos);
                        maxPos = Math.min(this.ranges.get(chromosome)[1], maxPos);
                    }

                    if (this.positions != null && this.positions.containsKey(chromosome) && this.positions.get(chromosome) != null) {
                        int finalMinPos = minPos;
                        int finalMaxPos = maxPos;
                        position = new IntArray(this.positions.get(chromosome)).filter(pos -> pos >= finalMinPos && pos <= finalMaxPos);
                        if (position.size() == 0) {
                            continue;
                        }

                        // 去重排序
                        position.dropDuplicated();
                        position.sort();

                        minPos = Math.max(position.get(0), minPos);
                        maxPos = Math.min(position.get(-1), maxPos);
                    }

                    if (minPos > maxPos) {
                        continue;
                    }

                    if (position == null) {
                        for (int nodeIndex = 0, l = nodes.numOfNodes(); nodeIndex < l; nodeIndex++) {
                            GTBNode node = nodes.get(nodeIndex);
                            if ((minPos <= node.minPos) && (maxPos >= node.maxPos)) {
                                taskPipLine.put(true, TaskGTBNode.of(chromosome, nodeIndex));
                            } else {
                                if (ValueUtils.intersect(minPos, maxPos, node.minPos, node.maxPos)) {
                                    taskPipLine.put(true, TaskGTBNode.of(chromosome, nodeIndex, Value.of(minPos, node.minPos, node.maxPos), Value.of(maxPos, node.minPos, node.maxPos)));
                                }
                            }
                        }
                    } else {
                        for (int nodeIndex = 0, l = nodes.numOfNodes(); nodeIndex < l; nodeIndex++) {
                            GTBNode node = nodes.get(nodeIndex);
                            int taskMinPos = Value.of(minPos, node.minPos, node.maxPos);
                            int taskMaxPos = Value.of(maxPos, node.minPos, node.maxPos);
                            BaseArray<Integer> positions = position.filter(position1 -> position1 >= taskMinPos && position1 <= taskMaxPos);
                            if (positions.size() > 0) {
                                taskPipLine.put(true, TaskGTBNode.of(chromosome, nodeIndex, positions));
                            }
                        }
                    }
                }

                taskPipLine.putStatus(nThreads, false);
            });

            pool.submit(() -> {
                try {
                    Pair<Integer, Block<Boolean, TaskGTBNode>> task = writer.getContextId(taskPipLine::get);
                    int localId = task.key;
                    Block<Boolean, TaskGTBNode> taskBlock = task.value;

                    if (!taskBlock.getStatus()) {
                        return;
                    }

                    GTBReader reader = new GTBReader(inputManager, phased);
                    reader.selectSubjects(subjects);
                    VolumeByteStream lineCache = new VolumeByteStream(1024);

                    do {
                        reader.limit(taskBlock.getData().chromosome, taskBlock.getData().nodeIndex, taskBlock.getData().nodeIndex + 1);
                        Variant variant = new Variant();
                        if (taskBlock.getData().taskType == 0) {
                            while (reader.readVariant(variant)) {
                                if (condition.apply(variant)) {
                                    formatter.variantFormatter.apply(variant, lineCache);
                                    writer.write(localId, lineCache);
                                    lineCache.reset();
                                }
                            }
                        } else if (taskBlock.getData().taskType == 1) {
                            int minPos = taskBlock.getData().minPos;
                            int maxPos = taskBlock.getData().maxPos;
                            reader.search(taskBlock.getData().chromosome, minPos);
                            while (reader.readVariant(variant)) {
                                if (variant.position >= minPos && variant.position <= maxPos && condition.apply(variant)) {
                                    formatter.variantFormatter.apply(variant, lineCache);
                                    writer.write(localId, lineCache);
                                    lineCache.reset();
                                }
                            }
                        } else {
                            Set<Integer> position = taskBlock.getData().taskPos.toSet();
                            reader.search(taskBlock.getData().chromosome, taskBlock.getData().taskPos.get(0));
                            while (reader.readVariant(variant)) {
                                if (position.contains(variant.position) && condition.apply(variant)) {
                                    formatter.variantFormatter.apply(variant, lineCache);
                                    writer.write(localId, lineCache);
                                    lineCache.reset();
                                }
                            }
                        }

                        taskBlock = writer.flush(localId, taskPipLine::get);
                    } while (taskBlock.getStatus());

                    reader.close();
                    lineCache.close();
                } catch (Error | Exception e) {
                    throw new ThreadPoolRuntimeException(e);
                }
            }, nThreads);

            pool.close();

            // 添加 tailer 信息
            if (this.formatter.tailer != null) {
                byte[] data = this.formatter.tailer.apply(this);
                if (data.length > 0) {
                    writer.write(data);
                }
            }
            writer.close();
        }

        static class TaskGTBNode {
            final String chromosome;
            final int nodeIndex;

            /**
             * 任务类型, type = 0 解压全部数据, type = 1 解压指定范围数据，type = 2 解压任务数据
             */
            final int taskType;
            int minPos;
            int maxPos;
            final BaseArray<Integer> taskPos;

            static TaskGTBNode of(String chromosome, int nodeIndex) {
                return new TaskGTBNode(chromosome, nodeIndex);
            }

            static TaskGTBNode of(String chromosome, int nodeIndex, int minPos, int maxPos) {
                return new TaskGTBNode(chromosome, nodeIndex, minPos, maxPos);
            }

            static TaskGTBNode of(String chromosome, int nodeIndex, BaseArray<Integer> taskPos) {
                return new TaskGTBNode(chromosome, nodeIndex, taskPos);
            }

            TaskGTBNode(String chromosome, int nodeIndex) {
                this.chromosome = chromosome;
                this.nodeIndex = nodeIndex;
                this.taskType = 0;
                this.taskPos = null;
            }

            TaskGTBNode(String chromosome, int nodeIndex, int minPos, int maxPos) {
                this.chromosome = chromosome;
                this.nodeIndex = nodeIndex;
                this.taskPos = null;
                this.taskType = 1;
                this.minPos = minPos;
                this.maxPos = maxPos;
            }

            TaskGTBNode(String chromosome, int nodeIndex, BaseArray<Integer> taskPos) {
                this.chromosome = chromosome;
                this.nodeIndex = nodeIndex;
                this.taskType = 2;
                this.taskPos = taskPos;
            }
        }

        private void process() throws IOException {
            // 并行
            ThreadPool pool = new ThreadPool(nThreads);
            BaseArray<File> tempFiles = new Array<>(File[].class, nThreads + 2);

            // 创建临时文件夹
            File tempDir = outputFile.addExtension(".~$temp");
            tempDir.mkdir();
            tempDir.deleteOnExit();

            // 添加 header 信息
            if (this.formatter.header != null) {
                byte[] data = this.formatter.header.apply(this);
                if (data.length > 0) {
                    File header = tempDir.getSubFile("header.~$temp");
                    header.deleteOnExit();
                    FileStream headerStream = outputParam == null ? new FileStream(header) : new FileStream(new BGZIPWriterStream(header, outputParam.level));
                    headerStream.write(data);
                    headerStream.close();
                    tempFiles.add(header);
                }
            }

            // 计算所有节点总数
            int length = (int) Math.ceil((double) inputManager.getGtbTree().numOfNodes() / nThreads);
            AtomicInteger offset = new AtomicInteger(0);

            pool.submit(() -> {
                try {
                    GTBReader reader = new GTBReader(inputManager, phased);
                    reader.selectSubjects(subjects);
                    FileStream writer;

                    synchronized (offset) {
                        if (!reader.limit(offset.get(), length)) {
                            reader.close();
                            return;
                        }

                        File tempFile = tempDir.getSubFile("node" + offset.get() + "-" + (offset.get() + length) + ".~$temp");
                        tempFile.deleteOnExit();
                        tempFiles.add(tempFile);
                        writer = outputParam == null ? new FileStream(tempFile, FileStream.DEFAULT_WRITER) : new FileStream(new BGZIPWriterStream(tempFile, outputParam.level));
                        offset.addAndGet(length);
                    }

                    VolumeByteStream lineCache = new VolumeByteStream(1024);
                    // 获取限定指针
                    LimitPointer pointer = (LimitPointer) reader.tell();
                    String[] chromosomeList = reader.getChromosomeList();

                    if (chromosomeList.length > 0) {
                        String markStartChromosome = chromosomeList[0];
                        String markEndChromosome = chromosomeList[chromosomeList.length - 1];
                        int markStartNode = pointer.getStartNodeIndex();
                        int markEndNode = pointer.getEndNodeIndex();

                        Variant variant = new Variant();
                        for (String chromosome : chromosomeList) {
                            reader.limit(chromosome, chromosome.equals(markStartChromosome) ? markStartNode : 0, chromosome.equals(markEndChromosome) ? markEndNode : Integer.MAX_VALUE);

                            GTBNodes nodes = this.inputManager.getGTBNodes(chromosome);
                            if ((this.ranges != null && !this.ranges.containsKey(chromosome)) || this.positions != null && !this.positions.containsKey(chromosome)) {
                                continue;
                            }

                            int minPos = Integer.MAX_VALUE;
                            int maxPos = Integer.MIN_VALUE;

                            if (reader.searchEnable(chromosome)) {
                                // 有序文件
                                minPos = nodes.get(reader.getStartNodeIndex(chromosome)).minPos;
                                maxPos = nodes.get(reader.getEndNodeIndex(chromosome)).maxPos;
                            } else {
                                for (int nodeIndex = reader.getStartNodeIndex(chromosome), l = reader.getEndNodeIndex(chromosome); nodeIndex <= l; nodeIndex++) {
                                    GTBNode node = nodes.get(nodeIndex);
                                    if (node.minPos < minPos) {
                                        minPos = node.minPos;
                                    }

                                    if (node.maxPos > maxPos) {
                                        maxPos = node.maxPos;
                                    }
                                }
                            }

                            // 根据范围执行快速过滤
                            BaseArray<Integer> position = null;

                            if (this.ranges != null && this.ranges.containsKey(chromosome) && this.ranges.get(chromosome) != null) {
                                minPos = Math.max(this.ranges.get(chromosome)[0], minPos);
                                maxPos = Math.min(this.ranges.get(chromosome)[1], maxPos);
                            }

                            if (this.positions != null && this.positions.containsKey(chromosome) && this.positions.get(chromosome) != null) {
                                int finalMinPos = minPos;
                                int finalMaxPos = maxPos;
                                position = new IntArray(this.positions.get(chromosome)).filter(pos -> pos >= finalMinPos && pos <= finalMaxPos);
                                if (position.size() == 0) {
                                    continue;
                                }

                                // 去重排序
                                position.dropDuplicated();
                                position.sort();

                                minPos = Math.max(position.get(0), minPos);
                                maxPos = Math.min(position.get(-1), maxPos);
                            }

                            if (minPos > maxPos) {
                                continue;
                            }

                            // 开始搜索
                            if (reader.searchEnable(chromosome)) {
                                if (position != null) {
                                    // 此时只需要以 position 为准
                                    while (position.size() > 0) {
                                        if (reader.search(chromosome, position.popFirst())) {
                                            for (Variant v : reader.readVariants()) {
                                                if (condition.apply(v)) {
                                                    formatter.variantFormatter.apply(v, lineCache);
                                                    writer.write(lineCache);
                                                    lineCache.reset();
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    reader.search(chromosome, minPos);
                                    while (reader.readVariant(variant)) {
                                        if (variant.position > maxPos) {
                                            break;
                                        }

                                        if (condition.apply(variant)) {
                                            formatter.variantFormatter.apply(variant, lineCache);
                                            writer.write(lineCache);
                                            lineCache.reset();
                                        }
                                    }
                                }
                            } else {
                                if (position != null) {
                                    // 将 position 转为 set
                                    Set<Integer> sets = position.toSet();
                                    while (reader.readVariant(variant, sets)) {
                                        if (condition.apply(variant)) {
                                            formatter.variantFormatter.apply(variant, lineCache);
                                            writer.write(lineCache);
                                            lineCache.reset();
                                        }
                                    }
                                } else {
                                    while (reader.readVariant(variant)) {
                                        if (variant.position >= minPos && variant.position <= maxPos && condition.apply(variant)) {
                                            formatter.variantFormatter.apply(variant, lineCache);
                                            writer.write(lineCache);
                                            lineCache.reset();
                                        }
                                    }
                                }
                            }
                        }
                    }

                    writer.close();
                    reader.close();
                    lineCache.close();
                } catch (Error | Exception e) {
                    throw new ThreadPoolRuntimeException(e);
                }
            }, nThreads);

            pool.close();

            // 添加 tailer 信息
            if (this.formatter.tailer != null) {
                byte[] data = this.formatter.tailer.apply(this);
                if (data.length > 0) {
                    File tailerFile = tempDir.getSubFile("tailer.~$temp");
                    tailerFile.deleteOnExit();
                    FileStream tailerStream = outputParam == null ? new FileStream(tailerFile, FileStream.DEFAULT_WRITER) : new FileStream(new BGZIPWriterStream(tailerFile, outputParam.level));
                    tailerStream.write(data);
                    tailerStream.close();
                    tempFiles.add(tailerFile);
                }
            }

            if (outputParam != null) {
                BGZToolkit.Concat.instance(tempFiles.toArray(), outputFile).submit();
            } else {
                FileStream writer = outputFile.open(FileStream.CHANNEL_WRITER);
                for (File tempFile : tempFiles) {
                    FileStream reader = tempFile.open(FileStream.CHANNEL_READER);
                    reader.writeTo(0, reader.size(), writer.getChannel());
                    reader.close();
                }
                writer.close();
            }

            tempDir.delete();
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder("Retrieve variants from *.gtb file:");
            builder.append("\n\tinputFile: " + this.inputManager.getFile());
            builder.append("\n\toutputFile: " + this.outputFile);
            builder.append("\n\tthreads: " + this.nThreads);
            if (this.outputParam != null) {
                builder.append("\n\toutput format: compressed " + formatter + " with level " + this.outputParam.level);
            } else {
                builder.append("\n\toutput format: " + formatter);
            }

            builder.append("\n\tuse CLM algorithm: " + this.clm);
            if (this.subjects != null) {
                builder.append("\n\tselect subjects: " + StringArray.wrap(this.subjects).toString(5));
            }

            if (this.ranges != null) {
                builder.append("\n\trange of position: ");

                if (this.ranges.size() > 0) {
                    StringArray temp = new StringArray();
                    for (String chromosome : this.ranges.keySet()) {
                        if (this.ranges.get(chromosome) == null) {
                            temp.add("chr" + chromosome);
                        } else {
                            temp.add("chr" + chromosome + ":" + this.ranges.get(chromosome)[0] + "-" + this.ranges.get(chromosome)[1]);
                        }
                    }
                    builder.append(temp.join(", "));
                } else {
                    builder.append("<empty>");
                }
            }

            if (positions != null) {
                builder.append("\n\tspecific position: ");

                if (this.positions.size() > 0) {
                    StringArray temp = new StringArray();
                    for (String chromosome : this.positions.keySet()) {
                        if (this.positions.get(chromosome) == null) {
                            temp.add("chr" + chromosome);
                        } else {
                            temp.add("chr" + chromosome + ":" + IntArray.wrap(this.positions.get(chromosome)).toString(5));
                        }
                    }
                    builder.append(temp.join(", "));
                } else {
                    builder.append("<empty>");
                }
            }

            return builder.toString();
        }
    }

    public static class Sort implements Task {
        /**
         * 并行文件排序
         */
        final GTBManager inputManager;
        final File outputFile;
        GTBOutputParam outputParam;
        String[] subjects;
        int nThreads;

        /**
         * 构造器
         *
         * @param inputFile  输入文件名, 只能是单个文件名
         * @param outputFile 输出文件名，只能是单个文件名
         */
        Sort(File inputFile, File outputFile) throws IOException {
            Task.notNull(inputFile);
            Task.notNull(outputFile);
            Task.fileExists(inputFile);

            this.inputManager = GTBRootCache.get(inputFile);
            this.outputFile = outputFile;
            this.outputParam = new GTBOutputParam(inputFile);
            this.subjects = inputManager.getAllSubjects();
            this.nThreads = IParallelTask.checkParallel(-1);
        }

        /**
         * 构造器
         *
         * @param inputFile  输入文件名, 只能是单个文件名
         * @param outputFile 输出文件名，只能是单个文件名
         */
        public static Sort instance(File inputFile, File outputFile) throws IOException {
            return new Sort(inputFile, outputFile);
        }

        /**
         * 设置输出格式
         *
         * @param outputParam 输出格式, GTB 输出参数实例
         */
        public Sort setOutputParam(GTBOutputParam outputParam) throws IOException {
            this.outputParam = outputParam == null ? new GTBOutputParam(inputManager) : outputParam;
            return this;
        }

        /**
         * 筛选样本子集
         *
         * @param subjects 子样本序列
         */
        public Sort setSubjects(String[] subjects) {
            this.subjects = subjects == null ? inputManager.getAllSubjects() : subjects;
            return this;
        }

        /**
         * 设置并行线程数
         *
         * @param nThreads 线程数
         */
        public Sort setThreads(int nThreads) {
            this.nThreads = IParallelTask.checkParallel(nThreads);
            return this;
        }

        /**
         * 重建节点树
         */
        private static BaseArray<VariantMark> rebuildTree(GTBManager manager, String chromosome) throws IOException {
            VariantMark[] root = new VariantMark[manager.getGtbTree().numOfVariants(chromosome)];
            VolumeByteStream undecompressedCache = new VolumeByteStream();
            VolumeByteStream decompressedPosCache = new VolumeByteStream(manager.getBlockSize() << 2);
            GTBNodes nodes;

            nodes = manager.getGTBNodes(chromosome);
            int variantIndex = 0;

            // 位置解压器
            FileStream fileStream = manager.getFileStream();
            IDecompressor decompressor = IDecompressor.getInstance(manager.getCompressorIndex());

            for (int i = 0; i < nodes.numOfNodes(); i++) {
                GTBNode node = nodes.get(i);
                decompressedPosCache.reset();
                undecompressedCache.reset();
                undecompressedCache.makeSureCapacity(node.compressedPosSize);

                // 读取压缩后的位置数据
                fileStream.seek(node.blockSeek + node.compressedGenotypesSize);
                fileStream.read(undecompressedCache, node.compressedPosSize);
                decompressor.decompress(undecompressedCache, decompressedPosCache);
                undecompressedCache.reset();

                // 还原位置数据
                IntArray positions = new IntArray(manager.getBlockSize());
                for (int j = 0; j < node.numOfVariants(); j++) {
                    positions.add(ValueUtils.byteArray2IntegerValue(decompressedPosCache.cacheOf(j << 2),
                            decompressedPosCache.cacheOf(1 + (j << 2)),
                            decompressedPosCache.cacheOf(2 + (j << 2)),
                            decompressedPosCache.cacheOf(3 + (j << 2))));
                }
                positions.sort();
                for (int j = 0; j < positions.size(); j++) {
                    root[variantIndex++] = new VariantMark(positions.get(j), i, j);
                }
            }
            fileStream.close();

            undecompressedCache.close();
            decompressedPosCache.close();

            ArrayUtils.sort(root, Comparator.comparingInt(o -> o.position));
            return BaseArray.wrap(root);
        }

        private static class VariantMark {
            final int position;

            public final int nodeIndex;
            public final int variantIndex;

            VariantMark(int position, int nodeIndex, int variantIndex) {
                this.position = position;
                this.nodeIndex = nodeIndex;
                this.variantIndex = variantIndex;
            }
        }

        @Override
        public boolean submit() throws IOException {
            if (inputManager.isOrderedGTB()) {
                // 全文件有序, 则直接重写文件
                return Subset.instance(inputManager.getFile(), outputFile)
                        .setOutputParam(outputParam)
                        .setSubjects(subjects).setThreads(nThreads)
                        .submit();
            }

            StringArray chromosomes = StringArray.wrap(inputManager.getChromosomeList());

            // 设置默认格式
            final boolean phased = outputParam.isPhased();

            GTBWriter.Builder builder = new GTBWriter.Builder(outputFile, outputParam)
                    .setReference(inputManager.getReferenceManager().getReference())
                    .setSubject(subjects);

            if (nThreads == 1) {
                // 单线程
                GTBReader reader = new GTBReader(inputManager, phased);
                GTBWriter writer = builder.build();
                for (String chromosome : inputManager.getChromosomeList()) {
                    if (inputManager.getGTBNodes(chromosome).checkOrdered()) {
                        // 该染色体中的所有位点都是有序的, 则无需重建位点树表
                        Variant variant = new Variant();
                        reader.limit(chromosome);
                        while (reader.readVariant(variant)) {
                            writer.write(variant);
                        }
                    } else {
                        // 该染色体中的部分位点无序, 则对该染色体重建树表
                        BaseArray<VariantMark> variants = rebuildTree(inputManager, chromosome);

                        // 提取前 top k 个位点, 按节点排序
                        int blockSize = outputParam.getBlockSize();

                        // 缓存位点
                        Variant variantCache = new Variant();
                        int variantToWrite = blockSize;
                        while (true) {
                            if (variants.size() >= variantToWrite) {
                                BaseArray<VariantMark> taskVariants = variants.popFirst(variantToWrite);

                                // 按照节点编号排序
                                taskVariants.sort(Comparator.comparingInt(o -> o.nodeIndex));

                                // 重构对应的基因型数据, 并加入到 writer 中
                                for (VariantMark variant : taskVariants) {
                                    reader.seek(chromosome, variant.nodeIndex, variant.variantIndex);
                                    reader.readVariant(variantCache);
                                    variantToWrite += writer.write(variantCache);
                                }

                                variantToWrite = blockSize - (variantToWrite % blockSize);
                            } else {
                                // 按照节点编号排序
                                variants.sort(Comparator.comparingInt(o -> o.nodeIndex));

                                // 重构对应的基因型数据, 并加入到 writer 中
                                for (VariantMark variant : variants) {
                                    reader.seek(chromosome, variant.nodeIndex, variant.variantIndex);
                                    reader.readVariant(variantCache);
                                    variantToWrite += writer.write(variantCache);
                                }
                                break;
                            }
                        }
                    }
                }
                reader.close();
                writer.close();
            } else {
                // 多线程, 此时根据是单染色体多线程还是多染色体多线程进行分类
                ThreadPool pool = new ThreadPool(nThreads);
                BaseArray<File> tempFiles = new Array<>(File[].class, true);

                // 创建临时文件夹
                File tempDir = outputFile.addExtension(".~$temp");
                tempDir.deleteOnExit();
                tempDir.mkdir();

                int blockSize = BlockSizeParameter.getBlockSize(BlockSizeParameter.getSuggestBlockSizeType(outputParam.getBlockSize(), subjects.length));
                if (chromosomes.size() == 1) {
                    String chromosome = chromosomes.get(0);
                    BaseArray<VariantMark> variants = rebuildTree(inputManager, chromosome);
                    int numOfVariants = variants.size();
                    int eachThreadProcessNum = (int) Math.ceil((double) numOfVariants / nThreads);
                    AtomicInteger startVariantIndex = new AtomicInteger(0);
                    AtomicInteger endVariantIndex = new AtomicInteger(eachThreadProcessNum);

                    pool.submit(() -> {
                        try {
                            GTBReader reader = new GTBReader(inputManager, phased);
                            GTBWriter writer;
                            BaseArray<VariantMark> currentProcessedVariants;
                            synchronized (startVariantIndex) {
                                currentProcessedVariants = (BaseArray<VariantMark>) variants.get(startVariantIndex.get(), endVariantIndex.get() - startVariantIndex.get());
                                File tempFile = tempDir.getSubFile("chr" + chromosome + ".variant" + startVariantIndex + "-" + (endVariantIndex.get() - 1) + ".gtb");
                                tempFile.deleteOnExit();
                                tempFiles.add(tempFile);
                                writer = builder.setOutputFile(tempFile).build();
                                startVariantIndex.set(endVariantIndex.get());
                                endVariantIndex.set(Value.of(endVariantIndex.get() + eachThreadProcessNum, 0, numOfVariants));
                            }

                            // 缓存位点
                            Variant variantCache = new Variant();
                            int variantToWrite = blockSize;
                            while (true) {
                                if (currentProcessedVariants.size() >= variantToWrite) {
                                    BaseArray<VariantMark> taskVariants = currentProcessedVariants.popFirst(variantToWrite);

                                    // 按照节点编号排序
                                    taskVariants.sort(Comparator.comparingInt(o -> o.nodeIndex));

                                    // 重构对应的基因型数据, 并加入到 writer 中
                                    for (VariantMark variant : taskVariants) {
                                        reader.seek(chromosome, variant.nodeIndex, variant.variantIndex);
                                        reader.readVariant(variantCache);
                                        variantToWrite += writer.write(variantCache);
                                    }

                                    variantToWrite = blockSize - (variantToWrite % blockSize);
                                } else {
                                    // 按照节点编号排序
                                    currentProcessedVariants.sort(Comparator.comparingInt(o -> o.nodeIndex));

                                    // 重构对应的基因型数据, 并加入到 writer 中
                                    for (VariantMark variant : currentProcessedVariants) {
                                        reader.seek(chromosome, variant.nodeIndex, variant.variantIndex);
                                        reader.readVariant(variantCache);
                                        variantToWrite += writer.write(variantCache);
                                    }
                                    break;
                                }
                            }
                            reader.close();
                            writer.close();
                        } catch (Error | Exception e) {
                            throw new ThreadPoolRuntimeException(e);
                        }
                    }, nThreads);
                } else {
                    AtomicInteger chromosomeIndex = new AtomicInteger(0);
                    pool.submit(() -> {
                        try {
                            GTBReader reader = new GTBReader(inputManager, phased);
                            GTBWriter writer;
                            String chromosome;
                            synchronized (chromosomeIndex) {
                                chromosome = chromosomes.get(chromosomeIndex.getAndAdd(1));
                                reader.limit(chromosome);
                                File tempFile = tempDir.getSubFile("chr" + chromosome + ".gtb");
                                tempFile.deleteOnExit();
                                tempFiles.add(tempFile);
                                writer = builder.setOutputFile(tempFile).build();
                            }

                            if (inputManager.getGTBNodes(chromosome).checkOrdered()) {
                                // 该染色体中的所有位点都是有序的, 则无序重建位点树表
                                Variant variant = new Variant();
                                while (reader.readVariant(variant)) {
                                    writer.write(variant);
                                }
                                writer.close();
                                reader.close();
                            } else {
                                // 该染色体中的部分位点无序, 则对该染色体重建树表
                                BaseArray<VariantMark> variants = rebuildTree(inputManager, chromosome);

                                // 缓存位点
                                Variant variantCache = new Variant();
                                int variantToWrite = blockSize;
                                while (true) {
                                    if (variants.size() >= variantToWrite) {
                                        BaseArray<VariantMark> taskVariants = variants.popFirst(variantToWrite);

                                        // 按照节点编号排序
                                        taskVariants.sort(Comparator.comparingInt(o -> o.nodeIndex));

                                        // 重构对应的基因型数据, 并加入到 writer 中
                                        for (VariantMark variant : taskVariants) {
                                            reader.seek(chromosome, variant.nodeIndex, variant.variantIndex);
                                            reader.readVariant(variantCache);
                                            variantToWrite += writer.write(variantCache);
                                        }

                                        variantToWrite = blockSize - (variantToWrite % blockSize);
                                    } else {
                                        // 按照节点编号排序
                                        variants.sort(Comparator.comparingInt(o -> o.nodeIndex));

                                        // 重构对应的基因型数据, 并加入到 writer 中
                                        for (VariantMark variant : variants) {
                                            reader.seek(chromosome, variant.nodeIndex, variant.variantIndex);
                                            reader.readVariant(variantCache);
                                            variantToWrite += writer.write(variantCache);
                                        }
                                        break;
                                    }
                                }
                                reader.close();
                                writer.close();
                            }
                        } catch (Error | Exception e) {
                            throw new ThreadPoolRuntimeException(e);
                        }
                    }, chromosomes.size());
                }

                pool.close();
                Concat.instance(tempFiles.toArray(), outputFile).submit();
                tempDir.delete();
            }
            return true;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder("Sort variants in GTB by coordinates (chromosome and position):");
            builder.append("\n\tinputFile: " + this.inputManager.getFile());
            builder.append("\n\toutputFile: " + this.outputFile);
            builder.append("\n\tthreads: " + this.nThreads);
            builder.append("\n\toutput format: GTB");
            builder.append("\n\t");
            builder.append(this.outputParam.toString().replace("\n", "\n\t"));
            if (this.subjects != null) {
                builder.append("\n\tselect subjects: " + StringArray.wrap(this.subjects).toString(5));
            }

            return builder.toString();
        }
    }

    public static class Concat implements Task {
        /**
         * 连接多个子文件
         */
        final GTBManager[] inputManagers;
        final File outputFile;

        /**
         * 构造器
         *
         * @param inputFiles 输入文件名，可以是单个文件或文件夹
         * @param outputFile 输出文件名，只能是单个文件名
         */
        Concat(File[] inputFiles, File outputFile) throws IOException {
            Task.notNull(inputFiles);
            Task.notNull(outputFile);
            Task.fileExists(inputFiles);

            this.inputManagers = GTBRootCache.get(inputFiles);
            this.outputFile = outputFile;
        }

        /**
         * 构造器
         *
         * @param inputFiles 输入文件名，可以是单个文件或文件夹
         * @param outputFile 输出文件名，只能是单个文件名
         */
        public static Concat instance(File[] inputFiles, File outputFile) throws IOException {
            return new Concat(inputFiles, outputFile);
        }

        @Override
        public boolean submit() throws IOException {
            if (inputManagers.length == 1) {
                // 直接点对点输出文件
                inputManagers[0].getFile().copyTo(outputFile);
                return true;
            }

            // 加载待合并的文件
            Arrays.sort(this.inputManagers, (o1, o2) -> -Integer.compare(o1.getSubjectNum(), o2.getSubjectNum()));
            GTBManager mainManager = this.inputManagers[0];
            mainManager.bind(0);

            // 校验样本信息, 必须是其子集
            for (int i = 1; i < this.inputManagers.length; i++) {
                if ((mainManager.getSubjectNum() != this.inputManagers[i].getSubjectNum()) || (!ArrayUtils.equal(mainManager.getSubjects(), this.inputManagers[i].getSubjects()))) {
                    // 样本序列不一致时，检验 fromGtb[i] 是否为 mainGTB 的子集，如果不是，此处会报错
                    mainManager.getSubjectIndex(this.inputManagers[i].getAllSubjects());
                }
            }

            // 校验 header 信息
            for (int i = 1; i < this.inputManagers.length; i++) {
                boolean fastMode = true;
                if (!mainManager.isPhased() && this.inputManagers[i].isPhased()) {
                    // mainManager 是 unphased 的，但是 fromGTB[i] 是 phased 的
                    // 需要把 fromGTB[i] 的 phased 强制转为 unphased
                    fastMode = false;
                }

                if (mainManager.getCompressorIndex() != this.inputManagers[i].getCompressorIndex()) {
                    // 压缩算法不一样，也必须要强制转换
                    fastMode = false;
                }

                if ((mainManager.getSubjectNum() != this.inputManagers[i].getSubjectNum()) || (!ArrayUtils.equal(mainManager.getSubjects(), this.inputManagers[i].getSubjects()))) {
                    // 样本不同, 此时需要 mapping
                    Variant mappingVariant = new Variant(mainManager.getSubjectNum());
                    int[] indexes = mainManager.getSubjectIndex(this.inputManagers[i].getAllSubjects());

                    File tempFile = this.inputManagers[i].getFile().addExtension(".~$temp");
                    tempFile.deleteOnExit();
                    GTBReader reader = new GTBReader(this.inputManagers[i], mainManager.isPhased());
                    GTBWriter writer = new GTBWriter.Builder(tempFile, new GTBOutputParam(mainManager)).setReference(mainManager.getReference()).setSubject(mainManager.getAllSubjects()).build();
                    Variant variant = new Variant();
                    while (reader.readVariant(variant)) {
                        // 将 variant 投射到 mappingVariant
                        for (int BEGIndex = 0; BEGIndex < indexes.length; BEGIndex++) {
                            mappingVariant.BEGs[indexes[BEGIndex]] = variant.BEGs[BEGIndex];
                        }
                        mappingVariant.chromosome = variant.chromosome;
                        mappingVariant.position = variant.position;
                        mappingVariant.phased = variant.phased;
                        mappingVariant.REF = variant.REF;
                        mappingVariant.ALT = variant.ALT;
                        writer.write(mappingVariant);
                    }
                    writer.close();
                    reader.close();
                    this.inputManagers[i] = GTBRootCache.get(tempFile);
                } else {
                    if (!fastMode) {
                        File tempFile = this.inputManagers[i].getFile().addExtension(".~$temp");
                        tempFile.deleteOnExit();
                        GTBReader reader = new GTBReader(this.inputManagers[i], mainManager.isPhased());
                        GTBWriter writer = new GTBWriter.Builder(tempFile, new GTBOutputParam(mainManager)).setReference(mainManager.getReference()).setSubject(mainManager.getAllSubjects()).build();
                        Variant variant = new Variant();
                        while (reader.readVariant(variant)) {
                            writer.write(variant);
                        }
                        writer.close();
                        reader.close();
                        this.inputManagers[i] = GTBRootCache.get(tempFile);
                    }
                }

                this.inputManagers[i].bind(i);
                mainManager.getGtbTree().add(this.inputManagers[i].getGtbTree());
            }

            /* 节点树合并完成，开始去重，得到最终完整的主根 */
            mainManager.getGtbTree().flush(true);

            // 连接文件
            FileStream[] fromIns = new FileStream[this.inputManagers.length];
            FileStream out = outputFile.open(FileStream.CHANNEL_WRITER);

            // 写入头部信息
            mainManager.checkOrderedGTB();
            mainManager.checkSuggestToBGZF();
            out.write(mainManager.buildHeader());

            for (int i = 0; i < fromIns.length; i++) {
                fromIns[i] = this.inputManagers[i].getFileStream();
            }

            for (GTBNodes nodes : mainManager.getGtbTree()) {
                for (GTBNode node : nodes) {
                    fromIns[node.getRootIndex()].writeTo(node.blockSeek, node.blockSize, out.getChannel());
                }
            }

            for (FileStream fromIn : fromIns) {
                fromIn.close();
            }

            out.write(mainManager.getGtbTree().build());
            out.close();

            // 清除主管理器
            GTBRootCache.clear(mainManager);

            return true;
        }

        @Override
        public String toString() {
            StringArray inputFileNames = (StringArray) BaseArray.wrap(this.inputManagers).apply(manager -> manager.getFile().getFileName());
            StringBuilder builder = new StringBuilder("Concatenate multiple VCF files:");
            builder.append("\n\tinputFile: " + inputFileNames);
            builder.append("\n\toutputFile: " + this.outputFile);
            return builder.toString();
        }
    }

    public static class Merge implements Task {
        /**
         * 合并多个子文件
         * 采用最小堆优化排序, 逐个染色体进行合并
         */
        final File[] inputFiles;
        final File outputFile;
        GTBOutputParam outputParam;
        boolean union;
        Function<Variant, Boolean> condition;
        int nThreads;

        /**
         * 构造器
         *
         * @param inputFiles 输入文件名，可以是单个文件或文件夹
         * @param outputFile 输出文件名，只能是单个文件名
         */
        Merge(File[] inputFiles, File outputFile) throws IOException {
            Task.notNull(inputFiles);
            Task.notNull(outputFile);
            Task.fileExists(inputFiles);

            this.inputFiles = inputFiles;
            this.outputFile = outputFile;
            this.outputParam = new GTBOutputParam(inputFiles[0]);
            this.union = false;
            this.condition = variant -> true;
            this.nThreads = IParallelTask.checkParallel(-1);
        }

        /**
         * 构造器
         *
         * @param inputFiles 输入文件名，可以是单个文件或文件夹
         * @param outputFile 输出文件名，只能是单个文件名
         */
        public static Merge instance(File[] inputFiles, File outputFile) throws IOException {
            return new Merge(inputFiles, outputFile);
        }

        /**
         * 设置输出格式
         *
         * @param outputParam 输出格式, GTB 输出参数实例
         */
        public Merge setOutputParam(GTBOutputParam outputParam) throws IOException {
            this.outputParam = outputParam == null ? new GTBOutputParam(inputFiles[0]) : outputParam;
            return this;
        }

        /**
         * 对多个文件中的位点执行交集/并集 (false/true) 操作
         *
         * @param union 并集, 合并所有文件的位点
         */
        public Merge setUnion(boolean union) {
            this.union = union;
            return this;
        }

        /**
         * 设置位点过滤条件
         *
         * @param condition 位点过滤条件
         */
        public Merge setCondition(Function<Variant, Boolean> condition) {
            this.condition = condition == null ? variant -> true : condition;
            return this;
        }

        /**
         * 设置并行线程数
         *
         * @param nThreads 线程数
         */
        public Merge setThreads(int nThreads) {
            this.nThreads = IParallelTask.checkParallel(nThreads);
            return this;
        }

        @Override
        public boolean submit() throws IOException {
            if (inputFiles.length == 1) {
                // 只有一个文件时, 直接筛选子集
                return Subset.instance(inputFiles[0], outputFile)
                        .setOutputParam(outputParam)
                        .setCondition(condition)
                        .setThreads(nThreads).submit();
            }

            // 所有输入文件的管理器
            GTBManager[] inputManagers = GTBRootCache.get(inputFiles);

            // 先按照样本量进行排序
            final Queue<GTBManager> fileQueue = new PriorityQueue<>(Comparator.comparingInt(GTBManager::getSubjectNum));
            fileQueue.addAll(Arrays.asList(inputManagers));

            // 创建临时文件夹
            File tempDir = outputFile.addExtension(".~$temp");
            tempDir.mkdir();
            tempDir.deleteOnExit();

            // 合并文件池
            StringArray loadInChromosomes = recordChromosome(inputManagers);

            // 先对无序文件进行排序, 再合并
            int index = 0;
            while (fileQueue.size() >= 2) {
                GTBManager manager1 = sort(fileQueue.poll(), tempDir);
                GTBManager manager2 = sort(fileQueue.poll(), tempDir);
                File tempFile = tempDir.getSubFile("mergeTemp." + (index++) + ".gtb");

                if (fileQueue.size() > 0) {
                    // 删除临时文件
                    tempFile.deleteOnExit();
                }

                // 将文件合并至 tempFile, 并加入队列
                merge(manager1, manager2, loadInChromosomes, tempFile);
                fileQueue.add(GTBRootCache.get(tempFile));
            }

            // 弹出最后一个文件，重命名
            GTBManager lastManager = fileQueue.poll();
            lastManager.getFile().rename(outputFile);
            tempDir.delete();
            return true;
        }

        private StringArray recordChromosome(GTBManager[] inputManagers) {
            // 记录坐标，如果坐标太多，可以考虑分染色体读取 (使用 reader.limit 语句)
            HashSet<String> loadInChromosomes = new HashSet<>();

            if (this.union) {
                // 合并所有坐标
                for (GTBManager manager : inputManagers) {
                    loadInChromosomes.addAll(ArrayUtils.toArrayList(manager.getChromosomeList()));
                }
            } else {
                // 取交集 (先保留第一个文件的染色体)
                loadInChromosomes.addAll(ArrayUtils.toArrayList(inputManagers[0].getChromosomeList()));

                for (GTBManager manager : inputManagers) {
                    loadInChromosomes.retainAll(ArrayUtils.toArrayList(manager.getChromosomeList()));
                }
            }

            return StringArray.wrap(loadInChromosomes.toArray(new String[0]));
        }

        /**
         * 对 GTB 文件排序
         */
        private GTBManager sort(GTBManager manager, File tempDir) throws IOException {
            if (!manager.isOrderedGTB()) {
                // 检查文件是否有序, 若无序, 则排序后再合并
                File newFile = tempDir.getSubFile(manager.getFile().getFileName()).changeExtension(".order.gtb", ".gtb");
                newFile.deleteOnExit();
                Sort.instance(manager.getFile(), newFile).setOutputParam(this.outputParam).setThreads(this.nThreads).submit();
                GTBRootCache.clear(manager);
                return GTBRootCache.get(newFile);
            }

            return manager;
        }

        private HashSet<Integer> recordPosition(GTBManager manager1, GTBManager manager2, String chromosome) throws IOException {
            if (this.union) {
                // 并集, 返回 null
                return null;
            } else {
                HashSet<Integer> loadInPosition = new HashSet<>();

                GTBReader reader1 = new GTBReader(manager1, manager1.isPhased(), false);
                reader1.limit(chromosome);

                // 先加载第一个文件全部的位点
                for (Variant variant : reader1) {
                    loadInPosition.add(variant.position);
                }

                reader1.close();

                if (loadInPosition.size() == 0) {
                    return loadInPosition;
                }

                // 再移除第二个文件中没有的位点
                HashSet<Integer> loadInPosition2 = new HashSet<>();
                GTBReader reader2 = new GTBReader(manager2, manager2.isPhased(), false);
                reader2.limit(chromosome);

                for (Variant variant : reader2) {
                    loadInPosition2.add(variant.position);
                }

                reader2.close();

                int nums = loadInPosition.size();
                loadInPosition.retainAll(loadInPosition2);

                loadInPosition2.clear();
                loadInPosition2 = null;

                if (loadInPosition.size() == nums) {
                    // 取了交集后元素数量一致，则转为不做约束
                    loadInPosition.clear();
                    loadInPosition = null;
                    return null;
                }

                return loadInPosition;
            }
        }

        /**
         * 合并 manager1 和 manager 2
         *
         * @param manager1 管理器1
         * @param manager2 管理器2
         */
        private void merge(GTBManager manager1, GTBManager manager2, StringArray loadInChromosomes, File outputFile) throws IOException {
            GTBWriter.Builder builder = new GTBWriter.Builder(outputParam)
                    .setReference(manager1.getReference())
                    .setSubject(ArrayUtils.merge(manager1.getAllSubjects(), manager2.getAllSubjects()));

            ThreadPool pool = new ThreadPool(nThreads);
            AtomicInteger chromosomeIndex = new AtomicInteger(0);

            if (loadInChromosomes.size() > 0) {
                // 有染色体任务，会产生中间文件
                BaseArray<File> tempFiles = new Array<>(File[].class, loadInChromosomes.size());
                File outputDir = outputFile.addExtension(".~$temp");
                outputDir.deleteOnExit();
                outputDir.mkdir();
                pool.submit(() -> {
                    try {
                        GTBReader reader1 = new GTBReader(manager1, outputParam.isPhased());
                        GTBReader reader2 = new GTBReader(manager2, outputParam.isPhased());
                        GTBWriter writer;
                        String chromosome;

                        synchronized (chromosomeIndex) {
                            chromosome = loadInChromosomes.get(chromosomeIndex.getAndAdd(1));
                            File tempFile = outputDir.getSubFile("chr" + chromosome + ".gtb");
                            tempFile.deleteOnExit();
                            writer = builder.setOutputFile(tempFile).build();
                            tempFiles.add(tempFile);
                        }

                        // 元信息
                        Array<Variant> variants1 = null;
                        Array<Variant> variants1Cache = new Array<>();
                        Array<Variant> variants2 = null;
                        Array<Variant> variants2Cache = new Array<>();
                        Variant mergeVariant = new Variant(manager1.getSubjectNum() + manager2.getSubjectNum());
                        Variant mergeVariant1 = new Variant(manager1.getSubjectNum() + manager2.getSubjectNum());
                        Variant mergeVariant2 = new Variant(manager1.getSubjectNum() + manager2.getSubjectNum());

                        variants1Cache.add(new Variant());
                        variants2Cache.add(new Variant());

                        boolean condition1 = false;
                        boolean condition2 = false;
                        HashSet<Integer> position = recordPosition(manager1, manager2, chromosome);

                        if (manager1.contain(chromosome)) {
                            reader1.limit(chromosome);
                            variants1 = reader1.readVariants(variants1Cache, position);
                            condition1 = variants1 != null;
                        }

                        if (manager2.contain(chromosome)) {
                            reader2.limit(chromosome);
                            variants2 = reader2.readVariants(variants2Cache, position);
                            condition2 = variants2 != null;
                        }

                        while (condition1 || condition2) {
                            if (condition1 && !condition2) {
                                // v1 有效位点, v2 无效位点
                                do {
                                    for (Variant variant1 : variants1) {
                                        mergeVariant1.chromosome = variant1.chromosome;
                                        mergeVariant1.position = variant1.position;
                                        mergeVariant1.phased = variant1.phased;
                                        mergeVariant1.ALT = variant1.ALT;
                                        mergeVariant1.REF = variant1.REF;
                                        System.arraycopy(variant1.BEGs, 0, mergeVariant1.BEGs, 0, variant1.BEGs.length);
                                        if (condition.apply(mergeVariant1)) {
                                            writer.write(mergeVariant1);
                                        }
                                    }
                                    variants1Cache.addAll(variants1);
                                    variants1.clear();
                                    variants1 = reader1.readVariants(variants1Cache, position);
                                    condition1 = variants1 != null;
                                } while (condition1);
                            } else if (!condition1) {
                                // v2 有效位点, v1 无效位点
                                do {
                                    for (Variant variant2 : variants2) {
                                        mergeVariant2.chromosome = variant2.chromosome;
                                        mergeVariant2.position = variant2.position;
                                        mergeVariant2.phased = variant2.phased;
                                        mergeVariant2.ALT = variant2.ALT;
                                        mergeVariant2.REF = variant2.REF;
                                        System.arraycopy(variant2.BEGs, 0, mergeVariant2.BEGs, manager1.getSubjectNum(), variant2.BEGs.length);
                                        if (condition.apply(mergeVariant2)) {
                                            writer.write(mergeVariant2);
                                        }
                                    }
                                    variants2Cache.addAll(variants2);
                                    variants2.clear();
                                    variants2 = reader2.readVariants(variants2Cache, position);
                                    condition2 = variants2 != null;
                                } while (condition2);
                            } else {
                                // 都有有效位点, 此时先进行位置大小的比较
                                int position1 = variants1.get(0).position;
                                int position2 = variants2.get(0).position;
                                int compareStatue = position1 - position2;
                                if (compareStatue < 0) {
                                    // 写入所有的 1 位点，并移动 1 的指针
                                    do {
                                        for (Variant variant1 : variants1) {
                                            mergeVariant1.chromosome = variant1.chromosome;
                                            mergeVariant1.position = variant1.position;
                                            mergeVariant1.phased = variant1.phased;
                                            mergeVariant1.ALT = variant1.ALT;
                                            mergeVariant1.REF = variant1.REF;
                                            System.arraycopy(variant1.BEGs, 0, mergeVariant1.BEGs, 0, variant1.BEGs.length);
                                            if (condition.apply(mergeVariant1)) {
                                                writer.write(mergeVariant1);
                                            }
                                        }
                                        variants1Cache.addAll(variants1);
                                        variants1.clear();
                                        variants1 = reader1.readVariants(variants1Cache, position);
                                        condition1 = variants1 != null;
                                        position1 = variants1 != null ? variants1.get(0).position : -1;
                                    } while (condition1 && position1 < position2);

                                } else if (compareStatue > 0) {
                                    // 写入所有的 2 位点，并移动 2 的指针
                                    do {
                                        for (Variant variant2 : variants2) {
                                            mergeVariant2.chromosome = variant2.chromosome;
                                            mergeVariant2.position = variant2.position;
                                            mergeVariant2.phased = variant2.phased;
                                            mergeVariant2.ALT = variant2.ALT;
                                            mergeVariant2.REF = variant2.REF;
                                            System.arraycopy(variant2.BEGs, 0, mergeVariant2.BEGs, manager1.getSubjectNum(), variant2.BEGs.length);
                                            if (condition.apply(mergeVariant2)) {
                                                writer.write(mergeVariant2);
                                            }
                                        }
                                        variants2Cache.addAll(variants2);
                                        variants2.clear();
                                        variants2 = reader2.readVariants(variants2Cache, position);
                                        condition2 = variants2 != null;
                                        position2 = variants2 != null ? variants2.get(0).position : -1;
                                    } while (condition2 && position1 > position2);
                                } else {
                                    int sizeVariants1 = variants1.size();
                                    int sizeVariants2 = variants2.size();

                                    // 逐一配对
                                    if (sizeVariants1 == sizeVariants2 && sizeVariants1 == 1) {
                                        // 只有一个位点，直接合并
                                        variants1.get(0).merge(variants2.get(0), mergeVariant);
                                        if (condition.apply(mergeVariant)) {
                                            writer.write(mergeVariant);
                                        }
                                    } else {
                                        // 多对多 (先找一致项，不一致的再进行匹配)
                                        Array<Variant> variants1new = new Array<>();

                                        out:
                                        for (int i = 0; i < variants1.size(); i++) {
                                            Variant variant1 = variants1.get(i);
                                            for (int j = 0; j < variants2.size(); j++) {
                                                Variant variant2 = variants2.get(j);
                                                if ((Arrays.equals(variant1.REF, variant2.REF) && Arrays.equals(variant1.ALT, variant2.ALT)) ||
                                                        (Arrays.equals(variant1.REF, variant2.ALT) && Arrays.equals(variant1.ALT, variant2.REF))) {
                                                    // 基本逻辑: 完全一致的碱基序列的位点直接合并，并且只合并一次
                                                    variant1.merge(variant2, mergeVariant);

                                                    if (condition.apply(mergeVariant)) {
                                                        writer.write(mergeVariant);
                                                    }

                                                    variants2.remove(variant2);
                                                    variants2Cache.add(variant2);
                                                    continue out;
                                                }
                                            }

                                            // 没有遇到匹配的，此时记录下该位点
                                            variants1new.add(variant1);
                                        }

                                        while (true) {
                                            if (variants1new.size() == 0 && variants2.size() == 0) {
                                                // 所有来自文件 1 和 文件 2 的位点都被匹配完成

                                                break;
                                            } else if (variants1new.size() == 0) {
                                                // 所有来自文件 1 的位点都被匹配完成，此时文件 2 直接写入
                                                for (Variant variant2 : variants2) {
                                                    mergeVariant2.chromosome = variant2.chromosome;
                                                    mergeVariant2.position = variant2.position;
                                                    mergeVariant2.phased = variant2.phased;
                                                    mergeVariant2.ALT = variant2.ALT;
                                                    mergeVariant2.REF = variant2.REF;
                                                    System.arraycopy(variant2.BEGs, 0, mergeVariant2.BEGs, manager1.getSubjectNum(), variant2.BEGs.length);

                                                    if (condition.apply(mergeVariant2)) {
                                                        writer.write(mergeVariant2);
                                                    }
                                                }

                                                break;
                                            } else if (variants2.size() == 0) {
                                                // 所有来自文件 2 的位点都被匹配完成，此时文件 1 直接写入
                                                for (Variant variant1 : variants1new) {
                                                    mergeVariant1.chromosome = variant1.chromosome;
                                                    mergeVariant1.position = variant1.position;
                                                    mergeVariant1.phased = variant1.phased;
                                                    mergeVariant1.ALT = variant1.ALT;
                                                    mergeVariant1.REF = variant1.REF;
                                                    System.arraycopy(variant1.BEGs, 0, mergeVariant1.BEGs, 0, variant1.BEGs.length);

                                                    if (condition.apply(mergeVariant1)) {
                                                        writer.write(mergeVariant1);
                                                    }
                                                }

                                                break;
                                            } else {
                                                // 文件 1 和文件 2 都有位点，此时按照顺序匹配
                                                Array<Variant> variants1NewTemp = new Array<>();
                                                for (int i = 0; i < variants1new.size(); i++) {
                                                    Variant variant1 = variants1new.get(i);
                                                    if (variants2.size() > 0) {
                                                        Variant variant2 = variants2.popFirst();
                                                        variant1.merge(variant2, mergeVariant);

                                                        if (condition.apply(mergeVariant)) {
                                                            writer.write(mergeVariant);
                                                        }

                                                        variants2Cache.add(variant2);
                                                        continue;
                                                    }

                                                    // 没有遇到匹配的，此时记录下该位点
                                                    variants1NewTemp.add(variant1);
                                                }
                                                variants1new = variants1NewTemp;
                                            }
                                        }
                                    }

                                    variants1Cache.addAll(variants1);
                                    variants1.clear();
                                    variants1 = reader1.readVariants(variants1Cache, position);
                                    condition1 = variants1 != null;
                                    variants2Cache.addAll(variants2);
                                    variants2.clear();
                                    variants2 = reader2.readVariants(variants2Cache, position);
                                    condition2 = variants2 != null;
                                }
                            }
                        }
                        reader1.close();
                        reader2.close();
                        writer.close();
                    } catch (Exception | Error e) {
                        throw new ThreadPoolRuntimeException(e);
                    }
                }, loadInChromosomes.size());
                pool.close();

                // 合并至输出文件
                Concat.instance(tempFiles.toArray(), outputFile).submit();
                outputDir.delete();
            } else {
                builder.setOutputFile(outputFile).build().close();
            }
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder("Merge multiple GTB files (with non-overlapping subject sets):");
            builder.append("\n\tinputFile: " + Arrays.toString(this.inputFiles));
            builder.append("\n\toutputFile: " + this.outputFile);
            builder.append("\n\tthreads: " + this.nThreads);
            if (this.union) {
                builder.append("\n\tMerge all variants in different files (union)");
            } else {
                builder.append("\n\tMerge common variants in different files (intersection)");
            }
            builder.append("\n\toutput format: GTB");
            builder.append("\n\t");
            builder.append(this.outputParam.toString().replace("\n", "\n\t"));

            return builder.toString();
        }
    }

    public static class ResetSubject implements Task {
        /**
         * 重设样本序列
         */
        final GTBManager inputManager;
        final File outputFile;
        String[] subjects;

        /**
         * 构造器
         *
         * @param inputFile  输入文件名, 只能是单个文件名
         * @param outputFile 输出文件名，只能是单个文件名
         */
        ResetSubject(File inputFile, File outputFile) throws IOException {
            Task.notNull(inputFile);
            Task.notNull(outputFile);
            Task.fileExists(inputFile);

            this.inputManager = GTBRootCache.get(inputFile);
            this.outputFile = outputFile;
        }

        /**
         * 构造器
         *
         * @param inputFile  输入文件名, 只能是单个文件名
         * @param outputFile 输出文件名，只能是单个文件名
         */
        public static ResetSubject instance(File inputFile, File outputFile) throws IOException {
            return new ResetSubject(inputFile, outputFile);
        }

        /**
         * 设置新样本序列
         *
         * @param subjects 新样本名, 样本个数必须与原文件个数一致
         */
        public ResetSubject setSubjects(String[] subjects) {
            if (subjects == null || subjects.length != inputManager.getSubjectNum()) {
                throw new GBCWorkFlowException("the number of subjects must be equal to " + inputManager.getSubjectNum() + " to overwrite the subject of the original file");
            }

            this.subjects = subjects;
            return this;
        }

        @Override
        public boolean submit() throws IOException {
            if (subjects == null) {
                throw new GBCWorkFlowException("the number of subjects must be equal to " + inputManager.getSubjectNum() + " to overwrite the subject of the original file");
            }

            inputManager.getSubjectManager().load(StringArray.wrap(subjects).join("\t").getBytes());

            // 连接文件
            FileStream in = inputManager.getFileStream();
            FileStream out = outputFile.open(FileStream.CHANNEL_WRITER);

            // 重新生成头部信息
            out.write(inputManager.buildHeader());

            // 连接块数据
            for (GTBNodes nodes : inputManager.getGtbTree()) {
                for (GTBNode node : nodes) {
                    in.writeTo(node.blockSeek, node.blockSize, out.getChannel());
                }
            }

            in.close();
            out.write(inputManager.getGtbTree().build());
            out.close();

            // 清除主管理器
            GTBRootCache.clear(inputManager);

            return true;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder("Reset subject names:");
            builder.append("\n\tinputFile: " + inputManager.getFile());
            builder.append("\n\toutputFile: " + this.outputFile);
            builder.append("\n\torigin subjects: " + StringArray.wrap(inputManager.getAllSubjects()).toString(5));
            builder.append("\n\ttarget subjects: " + StringArray.wrap(subjects).toString(5));
            return builder.toString();
        }
    }

    public static class Prune implements Task {
        /**
         * 快速修剪文件
         */
        final File inputFile;
        final File outputFile;
        Function<GTBTree, GTBTree> pruner;

        /**
         * 构造器
         *
         * @param inputFile  输入文件名, 只能是单个文件名
         * @param outputFile 输出文件名，只能是单个文件名
         */
        Prune(File inputFile, File outputFile) throws IOException {
            Task.notNull(inputFile);
            Task.notNull(outputFile);
            Task.fileExists(inputFile);

            this.inputFile = inputFile;
            this.outputFile = outputFile;
            this.pruner = tree -> tree;
        }

        /**
         * 构造器
         *
         * @param inputFile  输入文件名, 只能是单个文件名
         * @param outputFile 输出文件名，只能是单个文件名
         */
        public static Prune instance(File inputFile, File outputFile) throws IOException {
            return new Prune(inputFile, outputFile);
        }

        /**
         * 设置修剪器, 将 当前 GTBTree 修剪为另一 GTBTree
         *
         * @param pruner 修剪器
         */
        public Prune setPruner(Function<GTBTree, GTBTree> pruner) {
            this.pruner = pruner;
            return this;
        }

        /**
         * 保留指定的染色体编号
         *
         * @param chromosomes 指定的染色体编号
         */
        public Prune retain(String... chromosomes) {
            this.pruner = new Function<GTBTree, GTBTree>() {
                @Override
                public GTBTree apply(GTBTree tree) {
                    tree.retain(chromosomes);
                    return tree;
                }

                @Override
                public String toString() {
                    StringArray temp = new StringArray();
                    for (String chromosome : chromosomes) {
                        temp.add("chr" + chromosome);
                    }
                    return "retain: " + temp.join(", ");
                }
            };
            return this;
        }

        /**
         * 移除指定的染色体编号
         *
         * @param chromosomes 指定的染色体编号
         */
        public Prune remove(String... chromosomes) {
            this.pruner = new Function<GTBTree, GTBTree>() {
                @Override
                public GTBTree apply(GTBTree tree) {
                    tree.remove(chromosomes);
                    return tree;
                }

                @Override
                public String toString() {
                    StringArray temp = new StringArray();
                    for (String chromosome : chromosomes) {
                        temp.add("chr" + chromosome);
                    }
                    return "remove: " + temp.join(", ");
                }
            };
            return this;
        }

        /**
         * 保留指定的染色体编号及其对应的节点编号, 当 value = null 时代表该染色体编号下的所有节点
         *
         * @param chromosomeNodes 指定的染色体编号及其节点信息
         */
        public Prune retain(Map<String, int[]> chromosomeNodes) {
            this.pruner = new Function<GTBTree, GTBTree>() {
                @Override
                public GTBTree apply(GTBTree tree) {
                    tree.retain(chromosomeNodes);
                    return tree;
                }

                @Override
                public String toString() {
                    StringArray temp = new StringArray();
                    for (String chromosome : chromosomeNodes.keySet()) {
                        if (chromosomeNodes.get(chromosome) == null) {
                            temp.add("chr" + chromosome);
                        } else {
                            temp.add("chr" + chromosome + ":" + IntArray.wrap(chromosomeNodes.get(chromosome)).toString(5));
                        }
                    }
                    return "retain: " + temp.join(", ");
                }
            };
            return this;
        }

        /**
         * 保留指定的染色体编号及其对应的节点编号, 当 value = null 时代表该染色体编号下的所有节点
         *
         * @param chromosomeNodes 指定的染色体编号及其节点信息
         */
        public Prune remove(Map<String, int[]> chromosomeNodes) {
            this.pruner = new Function<GTBTree, GTBTree>() {
                @Override
                public GTBTree apply(GTBTree tree) {
                    tree.remove(chromosomeNodes);
                    return tree;
                }

                @Override
                public String toString() {
                    StringArray temp = new StringArray();
                    for (String chromosome : chromosomeNodes.keySet()) {
                        if (chromosomeNodes.get(chromosome) == null) {
                            temp.add("chr" + chromosome);
                        } else {
                            temp.add("chr" + chromosome + ":" + IntArray.wrap(chromosomeNodes.get(chromosome)).toString(5));
                        }
                    }
                    return "remove: " + temp.join(", ");
                }
            };
            return this;
        }

        @Override
        public boolean submit() throws IOException {
            GTBManager inputManager = GTBRootCache.get(inputFile);

            FileStream in = inputFile.open(FileStream.CHANNEL_READER);
            FileStream out = outputFile.open(FileStream.CHANNEL_WRITER);

            GTBManager newManager = new GTBManager(inputManager, pruner.apply(inputManager.getGtbTree().clone()));
            out.write(newManager.buildHeader());

            // 连接块数据
            for (GTBNodes nodes : newManager.getGtbTree()) {
                for (GTBNode node : nodes) {
                    in.writeTo(node.blockSeek, node.blockSize, out.getChannel());
                }
            }

            out.write(newManager.getGtbTree().build());
            in.close();
            out.close();
            return true;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder("Prune GTB files by node-level or chromosome-level:");
            builder.append("\n\tinputFile: " + inputFile);
            builder.append("\n\toutputFile: " + this.outputFile);
            builder.append("\n\tpruner: " + this.pruner);
            return builder.toString();
        }
    }

    public static class AlleleCheck implements Task {
        private static final Logger logger = LoggerFactory.getLogger("AlleleCheck");

        /**
         * 并行检查碱基错配
         */
        File templateFile;
        File inputFile;
        File outputFile;
        GTBOutputParam outputParam;

        int nThreads;

        /**
         * 碱基纠正
         */
        AlleleChecker alleleChecker;
        boolean union;
        static HashMap<Byte, Byte> complementaryBase = new HashMap<>();

        /**
         * 构造器
         *
         * @param templateFile 模版文件, 只能是单个文件名
         * @param inputFile    输入文件名, 只能是单个文件名
         * @param outputFile   输出文件名，只能是单个文件名
         */
        AlleleCheck(File templateFile, File inputFile, File outputFile) throws IOException {
            Task.notNull(inputFile);
            Task.notNull(templateFile);
            Task.notNull(outputFile);
            Task.fileExists(inputFile);
            Task.fileExists(templateFile);

            this.inputFile = inputFile;
            this.templateFile = templateFile;
            this.outputFile = outputFile;
            this.outputParam = new GTBOutputParam(inputFile);
            this.union = false;
            this.alleleChecker = new AlleleFreqGapTestChecker();
            this.nThreads = IParallelTask.checkParallel(-1);
        }

        /**
         * 构造器
         *
         * @param templateFile 模版文件, 只能是单个文件名
         * @param inputFile    输入文件名, 只能是单个文件名
         * @param outputFile   输出文件名，只能是单个文件名
         */
        public static AlleleCheck instance(File templateFile, File inputFile, File outputFile) throws IOException {
            return new AlleleCheck(templateFile, inputFile, outputFile);
        }

        /**
         * 设置碱基检查器
         *
         * @param alleleChecker 碱基检查器
         */
        public AlleleCheck setAlleleChecker(AlleleChecker alleleChecker) {
            this.alleleChecker = alleleChecker == null ? new MixChecker() : alleleChecker;
            return this;
        }

        /**
         * 设置输出格式
         *
         * @param outputParam 输出格式, GTB 输出参数实例
         */
        public AlleleCheck setOutputParam(GTBOutputParam outputParam) throws IOException {
            this.outputParam = outputParam == null ? new GTBOutputParam(inputFile) : outputParam;
            return this;
        }

        /**
         * 对多个文件中的位点执行交集/并集 (false/true) 操作
         *
         * @param union 并集, 合并所有文件的位点
         */
        public AlleleCheck setUnion(boolean union) {
            this.union = union;
            return this;
        }

        /**
         * 设置并行线程数
         *
         * @param nThreads 线程数
         */
        public AlleleCheck setThreads(int nThreads) {
            this.nThreads = IParallelTask.checkParallel(nThreads);
            return this;
        }

        static {
            byte A = 65;
            byte T = 84;
            byte C = 67;
            byte G = 71;
            complementaryBase.put(A, T);
            complementaryBase.put(T, A);
            complementaryBase.put(C, G);
            complementaryBase.put(G, C);
        }

        @Override
        public boolean submit() throws IOException {
            // 输入文件的管理器
            GTBManager templateManager = GTBRootCache.get(templateFile);

            // 排序模版文件
            if (!templateManager.isOrderedGTB()) {
                File tempInputFile = templateFile.addExtension(".~$temp");
                tempInputFile.deleteOnExit();
                Sort.instance(templateFile, tempInputFile).setThreads(this.nThreads).submit();
                templateManager = GTBRootCache.get(tempInputFile);
            }

            // 排序输入文件
            GTBManager inputManager = GTBRootCache.get(inputFile);

            // 排序模版文件
            if (!inputManager.isOrderedGTB()) {
                File tempInputFile = inputFile.addExtension(".~$temp");
                tempInputFile.deleteOnExit();
                Sort.instance(inputFile, tempInputFile).setThreads(this.nThreads).submit();
                inputManager = GTBRootCache.get(tempInputFile);
            }

            // 合并文件池
            GTBWriter.Builder builder = new GTBWriter.Builder(outputParam).setReference(inputManager.getReference()).setSubject(inputManager.getAllSubjects());
            StringArray loadInChromosomes = recordChromosome(inputManager, templateManager);

            if (loadInChromosomes.size() > 0) {
                // 有染色体任务，会产生中间文件
                ThreadPool pool = new ThreadPool(this.nThreads);
                BaseArray<File> tempFiles = new Array<>(File[].class, loadInChromosomes.size());
                File outputDir = outputFile.addExtension(".~$temp");
                outputDir.deleteOnExit();
                outputDir.mkdir();
                GTBManager finalTemplateManager = templateManager;
                GTBManager finalInputManager = inputManager;
                pool.submit(() -> {
                    try {
                        GTBReader reader1 = new GTBReader(finalTemplateManager, outputParam.isPhased());
                        GTBReader reader2 = new GTBReader(finalInputManager, outputParam.isPhased());
                        AlleleChecker checker = this.alleleChecker.clone();
                        GTBWriter writer;
                        String chromosome;

                        synchronized (loadInChromosomes) {
                            chromosome = loadInChromosomes.popFirst();
                            File tempFile = outputDir.getSubFile("chr" + chromosome + ".gtb");
                            writer = builder.setOutputFile(tempFile).build();
                            tempFile.deleteOnExit();
                            tempFiles.add(tempFile);
                        }

                        // 元信息
                        BaseArray<Variant> variants1 = null;
                        BaseArray<Variant> variants1Cache = new Array<>();
                        BaseArray<Variant> variants2 = null;
                        BaseArray<Variant> variants2Cache = new Array<>();
                        checker.setReader(finalTemplateManager, finalInputManager);

                        variants1Cache.add(new Variant());
                        variants2Cache.add(new Variant());

                        boolean condition1 = false;
                        boolean condition2 = false;

                        // 取并集时，条件位点集不起作用
                        HashSet<Integer> position = recordPosition(finalTemplateManager, finalInputManager, chromosome);
                        checker.setPosition(position);

                        if (finalTemplateManager.contain(chromosome)) {
                            reader1.limit(chromosome);
                            reader1.seek(chromosome, 0);
                            variants1 = reader1.readVariants(variants1Cache, position);
                            condition1 = variants1 != null;
                        }

                        if (finalInputManager.contain(chromosome)) {
                            reader2.limit(chromosome);
                            reader2.seek(chromosome, 0);
                            variants2 = reader2.readVariants(variants2Cache, position);
                            condition2 = variants2 != null;
                        }

                        while (condition2) {
                            if (!condition1) {
                                // v2 有效位点, v1 无效位点
                                do {
                                    for (Variant variant2 : variants2) {
                                        writer.write(variant2);
                                    }
                                    variants2Cache.addAll(variants2);
                                    variants2.clear();
                                    variants2 = reader2.readVariants(variants2Cache, position);
                                    condition2 = variants2 != null;
                                } while (condition2);
                            } else {
                                // 都有有效位点, 此时先进行位置大小的比较
                                int position1 = variants1.get(0).position;
                                int position2 = variants2.get(0).position;
                                int compareStatue = position1 - position2;
                                if (compareStatue < 0) {
                                    // 写入所有的 1 位点，并移动 1 的指针
                                    do {
                                        variants1Cache.addAll(variants1);
                                        variants1.clear();
                                        variants1 = reader1.readVariants(variants1Cache, position);
                                        condition1 = variants1 != null;
                                        position1 = condition1 ? variants1.get(0).position : -1;
                                    } while (condition1 && position1 < position2);
                                } else if (compareStatue > 0) {
                                    // 写入所有的 2 位点，并移动 2 的指针
                                    do {
                                        for (Variant variant2 : variants2) {
                                            writer.write(variant2);
                                        }
                                        variants2Cache.addAll(variants2);
                                        variants2.clear();
                                        variants2 = reader2.readVariants(variants2Cache, position);
                                        condition2 = variants2 != null;
                                        position2 = condition2 ? variants2.get(0).position : -1;
                                    } while (condition2 && position1 > position2);
                                } else {
                                    // 多对多 (先找一致项，不一致的再进行匹配)
                                    byte[] ref = variants1.get(0).REF;
                                    variants1Cache.addAll(variants1);

                                    variants1 = variants1.filter(variant -> variant.getAlternativeAlleleNum() == 2);

                                    if (variants1.size() == 0) {
                                        for (Variant variant2 : variants2) {
                                            variant2.resetAlleles(ref);
                                            writer.write(variant2);
                                        }
                                    } else {
                                        out:
                                        for (Variant variant2 : variants2) {
                                            if (variant2.getAlternativeAlleleNum() <= 2) {
                                                for (Variant variant1 : variants1) {
                                                    if (alignVariantWithAlleleCheck(variant1, variant2, checker)) {
                                                        variant2.resetAlleles(ref);
                                                        writer.write(variant2);
                                                        continue out;
                                                    }
                                                }

                                            }
                                            variant2.resetAlleles(ref);
                                            writer.write(variant2);
                                        }
                                    }

                                    variants1.clear();
                                    variants1 = reader1.readVariants(variants1Cache, position);
                                    condition1 = variants1 != null;
                                    variants2Cache.addAll(variants2);
                                    variants2.clear();
                                    variants2 = reader2.readVariants(variants2Cache, position);
                                    condition2 = variants2 != null;
                                }
                            }
                        }

                        if (position != null) {
                            position.clear();
                        }
                        reader1.close();
                        reader2.close();
                        writer.close();
                    } catch (Exception | Error e) {
                        throw new ThreadPoolRuntimeException(e);
                    }
                }, loadInChromosomes.size());
                pool.close();

                // 合并至输出文件
                Concat.instance(tempFiles.toArray(), outputFile).submit();
                outputDir.delete();
            } else {
                builder.setOutputFile(outputFile).build().close();
            }

            return true;
        }

        private StringArray recordChromosome(GTBManager inputManager, GTBManager tempManager) {
            // 记录坐标，如果坐标太多，可以考虑分染色体读取 (使用 reader.limit 语句)
            HashSet<String> loadInChromosomes = new HashSet<>();

            if (this.union) {
                // 合并所有坐标
                loadInChromosomes.addAll(ArrayUtils.toArrayList(inputManager.getChromosomeList()));
                loadInChromosomes.addAll(ArrayUtils.toArrayList(tempManager.getChromosomeList()));
            } else {
                // 取交集 (先保留第一个文件的染色体)
                loadInChromosomes.addAll(ArrayUtils.toArrayList(inputManager.getChromosomeList()));
                loadInChromosomes.retainAll(ArrayUtils.toArrayList(tempManager.getChromosomeList()));
            }

            return StringArray.wrap(loadInChromosomes.toArray(new String[0]));
        }

        private HashSet<Integer> recordPosition(GTBManager manager1, GTBManager manager2, String chromosome) throws IOException {
            if (this.union) {
                // 并集, 返回 null
                return null;
            } else {
                HashSet<Integer> loadInPosition = new HashSet<>();

                GTBReader reader1 = new GTBReader(manager1, manager1.isPhased(), false);
                reader1.limit(chromosome);

                // 先加载第一个文件全部的位点
                for (Variant variant : reader1) {
                    loadInPosition.add(variant.position);
                }

                reader1.close();

                if (loadInPosition.size() == 0) {
                    return loadInPosition;
                }

                // 再移除第二个文件中没有的位点
                HashSet<Integer> loadInPosition2 = new HashSet<>();
                GTBReader reader2 = new GTBReader(manager2, manager2.isPhased(), false);
                reader2.limit(chromosome);

                for (Variant variant : reader2) {
                    loadInPosition2.add(variant.position);
                }

                reader2.close();

                int nums = loadInPosition.size();
                loadInPosition.retainAll(loadInPosition2);

                loadInPosition2.clear();
                loadInPosition2 = null;

                if (loadInPosition.size() == nums) {
                    // 取了交集后元素数量一致，则转为不做约束
                    loadInPosition.clear();
                    loadInPosition = null;
                    return null;
                }

                return loadInPosition;
            }
        }

        private boolean alignVariantWithAlleleCheck(Variant variant1, Variant variant2, AlleleChecker checker) throws IOException {
            int AC12 = variant1.getAC();
            int AN1 = variant1.getAN();
            int AC11 = AN1 - AC12;
            int AC22 = variant2.getAC();
            int AN2 = variant2.getAN();
            int AC21 = AN2 - AC22;

            byte[] inverseAlleleVariant2REF = getInverseAllele(variant2.REF);

            // 都是二等位基因位点
            if (AC22 == 0 && AC12 == 0) {
                // 他们的 AF 都等于 0
                if (Arrays.equals(variant1.REF, inverseAlleleVariant2REF)) {
                    // 只修改标记，不修改基因型信息
                    variant2.REF = variant1.REF;
                    variant2.ALT = getInverseAllele(variant2.ALT);
                    return true;
                }
            } else {
                // 至少有一个 AF 不等于 0
                byte[] inverseAlleleVariant2ALT = getInverseAllele(variant2.ALT);

                if (Arrays.equals(variant1.REF, variant2.REF) && Arrays.equals(variant1.ALT, variant2.ALT)) {
                    // 查看是否有翻转的标签情况
                    if (Arrays.equals(variant1.REF, inverseAlleleVariant2ALT) && checker.isEqual(variant1, variant2, AC11, AC12, AC22, AC21, true)) {
                        // A T   ->   A T
                        // A T        T A
                        logger.info("chr{}:{} TempREF={} TempALT={} TempAF={} REF={} ALT={} AF={} -> REF={} ALT={}", variant1.chromosome, variant1.position,
                                new String(variant1.REF), new String(variant1.ALT), ((double) AC12) / AN1,
                                new String(variant2.REF), new String(variant2.ALT), ((double) AC22) / AN2, new String(inverseAlleleVariant2REF), new String(inverseAlleleVariant2ALT));
                        variant2.REF = inverseAlleleVariant2REF;
                        variant2.ALT = inverseAlleleVariant2ALT;
                    }
                    return true;
                } else if (Arrays.equals(variant1.REF, variant2.ALT) && Arrays.equals(variant1.ALT, variant2.REF)) {
                    if (Arrays.equals(variant1.REF, inverseAlleleVariant2REF) && checker.isEqual(variant1, variant2, AC11, AC12, AC21, AC22, false)) {
                        // A T   ->   A T
                        // T A        A T
                        logger.info("chr{}:{} TempREF={} TempALT={} TempAF={} REF={} ALT={} AF={} -> REF={} ALT={}", variant1.chromosome, variant1.position,
                                new String(variant1.REF), new String(variant1.ALT), ((double) AC12) / AN1,
                                new String(variant2.REF), new String(variant2.ALT), ((double) AC22) / AN2, new String(inverseAlleleVariant2REF), new String(inverseAlleleVariant2ALT));
                        variant2.REF = inverseAlleleVariant2REF;
                        variant2.ALT = inverseAlleleVariant2ALT;
                    }
                    return true;
                } else if (Arrays.equals(variant1.REF, inverseAlleleVariant2REF) && Arrays.equals(variant1.ALT, inverseAlleleVariant2ALT)) {
                    // 查看是否有翻转的标签情况
                    if (checker.isEqual(variant1, variant2, AC11, AC12, AC21, AC22, false)) {
                        // A C   ->   A C
                        // T G        A C
                        logger.info("chr{}:{} TempREF={} TempALT={} TempAF={} REF={} ALT={} AF={} -> REF={} ALT={}", variant1.chromosome, variant1.position,
                                new String(variant1.REF), new String(variant1.ALT), ((double) AC12) / AN1,
                                new String(variant2.REF), new String(variant2.ALT), ((double) AC22) / AN2, new String(inverseAlleleVariant2REF), new String(inverseAlleleVariant2ALT));
                        variant2.REF = inverseAlleleVariant2REF;
                        variant2.ALT = inverseAlleleVariant2ALT;
                    }
                    return true;
                } else if (Arrays.equals(variant1.REF, inverseAlleleVariant2ALT) && Arrays.equals(variant1.ALT, inverseAlleleVariant2REF)) {
                    // 查看是否有翻转的标签情况
                    if (checker.isEqual(variant1, variant2, AC11, AC12, AC22, AC21, true)) {
                        // A C   ->   A C
                        // G T        C A
                        logger.info("chr{}:{} TempREF={} TempALT={} TempAF={} REF={} ALT={} AF={} -> REF={} ALT={}", variant1.chromosome, variant1.position,
                                new String(variant1.REF), new String(variant1.ALT), ((double) AC12) / AN1,
                                new String(variant2.REF), new String(variant2.ALT), ((double) AC22) / AN2, new String(inverseAlleleVariant2REF), new String(inverseAlleleVariant2ALT));
                        variant2.REF = inverseAlleleVariant2REF;
                        variant2.ALT = inverseAlleleVariant2ALT;
                    }
                    return true;
                }
            }
            return false;
        }

        private byte[] getInverseAllele(byte[] allele) {
            byte[] alleleInverse = new byte[allele.length];
            for (int i = 0; i < alleleInverse.length; i++) {
                if (complementaryBase.containsKey(allele[i])) {
                    int code = complementaryBase.get(allele[i]);
                    alleleInverse[i] = (byte) code;
                } else {
                    alleleInverse[i] = allele[i];
                }
            }

            return alleleInverse;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder("Correct for potential complementary strand errors based on allele labels:");
            builder.append("\n\ttemplateFile: " + templateFile);
            builder.append("\n\tinputFile: " + inputFile);
            builder.append("\n\toutputFile: " + this.outputFile);
            builder.append("\n\tthreads: " + this.nThreads);
            if (this.union) {
                builder.append("\n\tRetain all variants (union)");
            } else {
                builder.append("\n\tRetain common variants (intersection)");
            }
            builder.append("\n\tallele checker: " + this.alleleChecker);
            builder.append("\n\toutput format: GTB");
            builder.append("\n\t");
            builder.append(this.outputParam.toString().replace("\n", "\n\t"));

            return builder.toString();
        }
    }

    public static class Split implements Task {
        /**
         * 快速分裂文件
         */
        final File inputFile;
        final File outputDir;
        Function<GTBTree, Map<String, GTBTree>> splitter;

        /**
         * 构造器
         *
         * @param inputFile 输入文件名, 只能是单个文件名
         * @param outputDir 输出文件夹
         */
        Split(File inputFile, File outputDir) throws IOException {
            Task.notNull(inputFile);
            Task.notNull(outputDir);
            Task.fileExists(inputFile);

            this.inputFile = inputFile;
            this.outputDir = outputDir;
            splitByChromosome();
        }

        /**
         * 构造器
         *
         * @param inputFile 输入文件名, 只能是单个文件名
         * @param outputDir 输出文件夹
         */
        public static Split instance(File inputFile, File outputDir) throws IOException {
            return new Split(inputFile, outputDir);
        }

        /**
         * 设置分裂器
         *
         * @param splitter 分类器, 键作为文件名, 值为该文件储存的子树信息
         */
        public Split setSplitter(Function<GTBTree, Map<String, GTBTree>> splitter) {
            this.splitter = splitter;
            return this;
        }

        /**
         * 按照染色体编号分裂文件, 并储存为 outputDir/chr[X].gtb
         */
        public Split splitByChromosome() {
            return setSplitter(new Function<GTBTree, Map<String, GTBTree>>() {
                @Override
                public Map<String, GTBTree> apply(GTBTree tree) {
                    HashMap<String, GTBTree> maps = new HashMap<>();
                    for (String chromosome : tree.getChromosomeList()) {
                        GTBTree newTree = new GTBTree();
                        newTree.add(tree.get(chromosome));
                        maps.put(chromosome + ".gtb", newTree);
                    }

                    return Collections.unmodifiableMap(maps);
                }

                @Override
                public String toString() {
                    return "split by chromosome tags";
                }
            });
        }

        /**
         * 按照染色体编号分裂文件, 并储存为 outputDir/chr[X].node[Y].gtb
         */
        public Split splitByNode() {
            return setSplitter(new Function<GTBTree, Map<String, GTBTree>>() {
                @Override
                public Map<String, GTBTree> apply(GTBTree tree) {
                    HashMap<String, GTBTree> maps = new HashMap<>();
                    for (String chromosome : tree.getChromosomeList()) {
                        GTBNodes nodes = tree.get(chromosome);
                        for (int nodeIndex = 0, l = nodes.numOfNodes(); nodeIndex < l; nodeIndex++) {
                            GTBTree newTree = new GTBTree();
                            newTree.add(nodes.get(nodeIndex));
                            maps.put(chromosome + "." + "node" + nodeIndex + ".gtb", newTree);
                        }
                    }

                    return Collections.unmodifiableMap(maps);
                }

                @Override
                public String toString() {
                    return "split by chromosome-node tags";
                }
            });
        }

        @Override
        public boolean submit() throws IOException {
            GTBManager inputManager = GTBRootCache.get(inputFile);
            this.outputDir.mkdir();

            Map<String, GTBTree> trees = splitter.apply(inputManager.getGtbTree().clone());
            FileStream in = inputFile.open(FileStream.CHANNEL_READER);

            for (String subFileName : trees.keySet()) {
                File subFile = outputDir.getSubFile(subFileName);
                FileStream out = subFile.open(FileStream.CHANNEL_WRITER);
                GTBManager tempManager = new GTBManager(inputManager, trees.get(subFileName));

                // 写入头部信息
                tempManager.checkOrderedGTB();
                tempManager.checkSuggestToBGZF();
                out.write(tempManager.buildHeader());
                // 连接块数据
                for (GTBNodes nodes : trees.get(subFileName)) {
                    for (GTBNode node : nodes) {
                        in.writeTo(node.blockSeek, node.blockSize, out.getChannel());
                    }
                }

                out.write(trees.get(subFileName).build());
                out.close();
            }

            in.close();
            return true;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder("Split a single GTB file into multiple subfiles:");
            builder.append("\n\tinputFile: " + inputFile);
            builder.append("\n\toutputDir: " + this.outputDir);
            builder.append("\n\tsplitter: " + this.splitter);

            return builder.toString();
        }
    }

    public static class ResetContig implements Task {
        /**
         * 重设染色体标签
         */
        final File inputFile;
        final File outputFile;
        File oldContig;
        File newContig;

        /**
         * 构造器
         *
         * @param inputFile  输入文件名, 只能是单个文件名
         * @param outputFile 输出文件名，只能是单个文件名
         */
        ResetContig(File inputFile, File outputFile) throws IOException {
            Task.notNull(inputFile);
            Task.notNull(outputFile);
            Task.fileExists(inputFile);

            this.inputFile = inputFile;
            this.outputFile = outputFile;
            this.oldContig = ChromosomeTags.getActiveFile();
            this.newContig = ChromosomeTags.getActiveFile();
        }

        /**
         * 构造器
         *
         * @param inputFile  输入文件名, 只能是单个文件名
         * @param outputFile 输出文件名，只能是单个文件名
         */
        public static ResetContig instance(File inputFile, File outputFile) throws IOException {
            return new ResetContig(inputFile, outputFile);
        }

        /**
         * 设置旧的重叠群文件
         *
         * @param oldContig 旧的重叠群文件
         */
        public ResetContig setOldContig(File oldContig) throws IOException {
            Task.fileExists(oldContig);
            this.oldContig = oldContig;
            return this;
        }

        /**
         * 设置新的重叠群文件
         *
         * @param newContig 新的重叠群文件
         */
        public ResetContig setNewContig(File newContig) throws IOException {
            Task.fileExists(newContig);
            this.newContig = newContig;
            return this;
        }

        @Override
        public boolean submit() throws IOException {
            if (oldContig.equals(newContig)) {
                inputFile.copyTo(outputFile);
            } else {
                ChromosomeTags.load(oldContig);
                GTBManager inputManager = GTBRootCache.get(inputFile);
                FileStream in = inputFile.open(FileStream.CHANNEL_READER);
                FileStream out = outputFile.open(FileStream.CHANNEL_WRITER);
                inputManager.resetContig(newContig);
                out.write(inputManager.buildHeader());

                // 连接块数据
                for (GTBNodes nodes : inputManager.getGtbTree()) {
                    for (GTBNode node : nodes) {
                        in.writeTo(node.blockSeek, node.blockSize, out.getChannel());
                    }
                }
                out.write(inputManager.getGtbTree().build());
                in.close();
                out.close();
                GTBRootCache.clear(inputManager);
            }
            return true;
        }
    }
}
