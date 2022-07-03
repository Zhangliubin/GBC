package edu.sysu.pmglab.gbc.core.gtbcomponent.gtbreader;

import edu.sysu.pmglab.container.File;
import edu.sysu.pmglab.container.VolumeByteStream;
import edu.sysu.pmglab.container.array.Array;
import edu.sysu.pmglab.container.array.BaseArray;
import edu.sysu.pmglab.easytools.ArrayUtils;
import edu.sysu.pmglab.easytools.ByteCode;
import edu.sysu.pmglab.gbc.coder.BEGTransfer;
import edu.sysu.pmglab.gbc.coder.decoder.MBEGDecoder;
import edu.sysu.pmglab.gbc.constant.ChromosomeTags;
import edu.sysu.pmglab.gbc.core.exception.GTBComponentException;
import edu.sysu.pmglab.gbc.core.gtbcomponent.GTBManager;
import edu.sysu.pmglab.gbc.core.gtbcomponent.GTBNode;
import edu.sysu.pmglab.gbc.core.gtbcomponent.GTBNodes;
import edu.sysu.pmglab.gbc.core.gtbcomponent.GTBRootCache;

import java.io.Closeable;
import java.io.IOException;
import java.util.*;

/**
 * @Data :2021/08/14
 * @Author :suranyi
 * @Contact :suranyi.sysu@gamil.com
 * @Description :GTB 读取器
 */

public class GTBReader implements Closeable, AutoCloseable, Iterable<Variant> {
    private final GTBManager manager;
    private final MBEGDecoder groupDecoder;
    private final boolean phased;
    private IndexPair[] pairs;
    private int[] subjectIndexes;
    private final boolean phasedTransfer;

    /**
     * 当前解压信息
     */
    Pointer pointer;
    final DecompressionCache cache;
    final int eachLineSize;
    final Map<String, Boolean> searchEnable;

    /**
     * 缓冲数据
     */
    private final VolumeByteStream headerCache = new VolumeByteStream(0);

    /**
     * 加载重叠群文件
     *
     * @param contig 重叠群文件
     */
    public void loadContig(File contig) throws IOException {
        ChromosomeTags.load(contig);
    }

    public GTBReader(GTBManager manager) throws IOException {
        this(new Pointer(manager));
    }

    public GTBReader(File fileName) throws IOException {
        this(new Pointer(GTBRootCache.get(fileName)));
    }

    public GTBReader(Pointer pointer) throws IOException {
        this(pointer, pointer.manager.isPhased(), true);
    }

    public GTBReader(GTBManager manager, boolean phased) throws IOException {
        this(new Pointer(manager), phased, true);
    }

    public GTBReader(File fileName, boolean phased) throws IOException {
        this(new Pointer(GTBRootCache.get(fileName)), phased, true);
    }

    public GTBReader(GTBManager manager, boolean phased, boolean decompressGT) throws IOException {
        this(new Pointer(manager), phased, decompressGT);
    }

    public GTBReader(File fileName, boolean phased, boolean decompressGT) throws IOException {
        this(new Pointer(GTBRootCache.get(fileName)), phased, decompressGT);
    }

    public GTBReader(Pointer pointer, boolean phased, boolean decompressGT) throws IOException {
        // 获取文件管理器
        this.manager = pointer.manager;

        // 设定解压的ID对
        initPairs(decompressGT);

        // 初始化指针
        this.pointer = pointer;
        this.cache = new DecompressionCache(this.manager, decompressGT);
        HashMap<String, Boolean> searchEnable = new HashMap<>();
        if (this.manager.isOrderedGTB()) {
            for (String chromosome : this.manager.getChromosomeList()) {
                searchEnable.put(chromosome, true);
            }
        } else {
            for (String chromosome : this.manager.getChromosomeList()) {
                searchEnable.put(chromosome, this.manager.getGTBNodes(chromosome).checkOrdered());
            }
        }
        this.searchEnable = Collections.unmodifiableMap(searchEnable);

        int eachCodeGenotypeNum = this.manager.isPhased() ? 3 : 4;
        int resBlockCodeGenotypeNum = this.manager.getSubjectNum() % eachCodeGenotypeNum;
        this.eachLineSize = (this.manager.getSubjectNum() / eachCodeGenotypeNum) + (resBlockCodeGenotypeNum == 0 ? 0 : 1);
        this.phased = phased;
        this.phasedTransfer = this.manager.isPhased() && !this.phased;

        // 锁定解码器
        this.groupDecoder = this.manager.getMBEGDecoder();
    }

    public boolean isPhased() {
        return this.phased;
    }

    /**
     * 限定访问的染色体
     *
     * @param chromosomes 染色体编号
     */
    public void limit(String... chromosomes) {
        this.pointer = new Pointer(this.manager, chromosomes);
    }

    /**
     * 限定访问的染色体及范围
     *
     * @param chromosome     染色体编号
     * @param startNodeIndex 限定访问的节点起始范围
     * @param endNodeIndex   限定访问的节点终止范围
     */
    public void limit(String chromosome, int startNodeIndex, int endNodeIndex) {
        this.pointer = new LimitPointer(this.manager, new String[]{chromosome}, startNodeIndex, endNodeIndex);
    }

    /**
     * 限定访问的染色体及范围
     *
     * @param chromosomes    染色体编号
     * @param startNodeIndex 限定访问的节点起始范围
     * @param endNodeIndex   限定访问的节点终止范围
     */
    public void limit(String[] chromosomes, int startNodeIndex, int endNodeIndex) {
        this.pointer = new LimitPointer(this.manager, chromosomes, startNodeIndex, endNodeIndex);
    }

    /**
     * 限定访问的染色体及范围
     */
    public boolean limit(int nodeOffset, int nodeLength) {
        String[] chromosomes = this.manager.getChromosomeList();

        int count = 0;

        // 先找开始的染色体标记
        for (int startChromosomeIndex = 0; startChromosomeIndex < chromosomes.length; startChromosomeIndex++) {
            if (count + this.manager.getGTBNodes(chromosomes[startChromosomeIndex]).numOfNodes() <= nodeOffset) {
                count += this.manager.getGTBNodes(chromosomes[startChromosomeIndex]).numOfNodes();
            } else {
                // 找到开始染色体标记和节点标记
                int startNodeIndex = nodeOffset - count;

                // 再尝试找结束的染色体标记和节点标记
                if (startNodeIndex + nodeLength <= this.manager.getGTBNodes(chromosomes[startChromosomeIndex]).numOfNodes()) {
                    // 在同一个染色体中
                    this.pointer = new LimitPointer(this.manager, new String[]{chromosomes[startChromosomeIndex]}, startNodeIndex, startNodeIndex + nodeLength);
                } else {
                    // 不在同一个块中
                    count = this.manager.getGTBNodes(chromosomes[startChromosomeIndex]).numOfNodes() - startNodeIndex;
                    for (int endChromosomeIndex = startChromosomeIndex + 1; endChromosomeIndex < chromosomes.length; endChromosomeIndex++) {
                        if (count + this.manager.getGTBNodes(chromosomes[endChromosomeIndex]).numOfNodes() < nodeLength) {
                            count += this.manager.getGTBNodes(chromosomes[endChromosomeIndex]).numOfNodes();
                        } else {
                            // 找到结束标记
                            this.pointer = new LimitPointer(this.manager, ArrayUtils.copyOfRange(chromosomes, startChromosomeIndex, endChromosomeIndex + 1), startNodeIndex, nodeLength - count);
                            return this.pointer.hasNext();
                        }
                    }

                    // 没有找到结束节点
                    this.pointer = new LimitPointer(this.manager, ArrayUtils.copyOfRange(chromosomes, startChromosomeIndex, chromosomes.length), startNodeIndex, Integer.MAX_VALUE);
                }
                return this.pointer.hasNext();
            }
        }

        // 没有找到开始节点
        this.pointer = new LimitPointer(this.manager, new String[0], 0, 0);
        return this.pointer.hasNext();
    }

    public int getNumOfNodes() {
        return this.manager.getGtbTree().numOfNodes();
    }

    private void initHeader() {
        // 重设缓冲信息
        headerCache.expansionTo(2 << 20);

        // 写入块头
        headerCache.write(("##fileformat=VCFv4.2" +
                "\n##FILTER=<ID=PASS,Description=\"All filters passed\">" +
                "\n##source=" + this.manager.getFile() +
                "\n##Version=<gbc_version=1.1,java_version=" + System.getProperty("java.version") + ",zstd_jni=1.4.9-5>"));

        // 参考序列非空时，写入参考序列
        if (!this.manager.isReferenceEmpty()) {
            headerCache.write("\n##reference=");
            headerCache.write(this.manager.getReference());
        }

        // 写入 contig 信息
        for (String chromosomeIndex : manager.getChromosomeList()) {
            headerCache.write(ChromosomeTags.getHeader(chromosomeIndex));
        }

        headerCache.write("\n##INFO=<ID=AC,Number=A,Type=Integer,Description=\"Allele count in genotypes\">" +
                "\n##INFO=<ID=AN,Number=1,Type=Integer,Description=\"Total number of alleles in called genotypes\">" +
                "\n##INFO=<ID=AF,Number=A,Type=Float,Description=\"Allele Frequency\">" +
                "\n##FORMAT=<ID=GT,Number=1,Type=String,Description=\"Genotype\">" +
                "\n#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\tFORMAT");
    }

    private void initPairs(boolean decompressGT) {
        if (decompressGT) {
            int eachGroupNum = this.manager.isPhased() ? 3 : 4;
            if (this.subjectIndexes == null) {
                pairs = new IndexPair[this.manager.getSubjectNum()];

                for (int i = 0; i < pairs.length; i++) {
                    pairs[i] = new IndexPair(i, i / eachGroupNum, i % eachGroupNum);
                }
            }

            this.subjectIndexes = pairs.length == 0 ? new int[0] : ArrayUtils.range(pairs.length - 1);
        } else {
            this.subjectIndexes = new int[]{};
            pairs = new IndexPair[0];
        }
    }

    /**
     * 读取下一个位点
     */
    public Variant readVariant() throws IOException {
        if (pointer.chromosomeIndex != -1) {
            this.cache.fill(pointer, this.pairs.length > 0);
            TaskVariant taskVariant = this.cache.taskVariants[pointer.variantIndex];
            GTBNode node = pointer.getNode();

            byte[] BEGs = new byte[this.pairs.length];
            fillBEGs(taskVariant, node, BEGs, this.phasedTransfer);

            pointer.next();
            return new Variant(node.chromosome, taskVariant.position, taskVariant.REF.values(), taskVariant.ALT.values(), BEGs, this.phased);
        } else {
            // 不存在下一个位点，此时 返回 null
            return null;
        }
    }

    /**
     * 读取下一个位点，带有位点位置约束
     */
    public Variant readVariant(Set<Integer> condition) throws IOException {
        if (condition == null) {
            return readVariant();
        }

        while (pointer.chromosomeIndex != -1) {
            this.cache.fill(pointer, false);
            for (int i = pointer.variantIndex; i < pointer.variantLength; i++) {
                TaskVariant taskVariant = this.cache.taskVariants[i];
                if (condition.contains(taskVariant.position)) {
                    pointer.variantIndex = i;
                    return readVariant();
                }
            }

            pointer.nextNode();
        }

        return null;
    }

    /**
     * 读取下一个位点，带有位点位置约束
     */
    public Variant readVariant(Map<String, Set<Integer>> conditions) throws IOException {
        if (conditions == null) {
            return readVariant();
        }

        TaskVariant taskVariant;

        while (pointer.chromosomeIndex != -1) {
            while (!conditions.containsKey(pointer.chromosomeList.get(pointer.chromosomeIndex))) {
                // 直接跳转到符合的染色体数据
                pointer.nextChromosome();

                if (pointer.chromosomeIndex == -1) {
                    return null;
                }
            }

            Set<Integer> condition = conditions.get(pointer.chromosomeList.get(pointer.chromosomeIndex));

            int chromosomeIndex = pointer.chromosomeIndex;
            int pointerChromosomeIndex = pointer.chromosomeIndex;
            while (chromosomeIndex == pointerChromosomeIndex) {
                this.cache.fill(pointer, false);

                for (int i = pointer.variantIndex; i < pointer.variantLength; i++) {
                    taskVariant = this.cache.taskVariants[i];
                    if (condition.contains(taskVariant.position)) {
                        pointer.variantIndex = i;
                        return readVariant();
                    }
                }

                // 这个节点不包含任务位点
                pointer.nextNode();
                pointerChromosomeIndex = pointer.chromosomeIndex;
            }
        }
        return null;
    }

    /**
     * 读取下一个位点 (公共坐标构成)
     */
    public Variant[] readVariants() throws IOException {
        final Variant variant = readVariant();

        if (variant != null) {
            TaskVariant taskVariant;
            Array<Variant> variants = new Array<>(Variant[].class);
            variants.add(variant);

            while (pointer.chromosomeIndex != -1) {
                this.cache.fill(pointer, this.pairs.length > 0);
                taskVariant = this.cache.taskVariants[pointer.variantIndex];
                GTBNode node = pointer.getNode();

                if (Objects.equals(variant.chromosome, node.chromosome) && taskVariant.position == variant.position) {
                    byte[] BEGs = new byte[this.pairs.length];
                    fillBEGs(taskVariant, node, BEGs, this.phasedTransfer);

                    // 标记位置值
                    variants.add(new Variant(node.chromosome, taskVariant.position, taskVariant.REF.values(), taskVariant.ALT.values(), BEGs, this.phased));
                    pointer.next();
                } else {
                    break;
                }
            }

            return variants.toArray();
        } else {
            // 不存在下一个位点，此时 返回 null
            return null;
        }
    }

    /**
     * 读取下一个位点，带有位点位置约束 (公共坐标构成)
     */
    public Variant[] readVariants(Set<Integer> condition) throws IOException {
        if (condition == null) {
            return readVariants();
        }

        final Variant variant = readVariant(condition);

        if (variant != null) {
            TaskVariant taskVariant;
            Array<Variant> variants = new Array<>(Variant[].class);
            variants.add(variant);

            while (pointer.chromosomeIndex != -1) {
                this.cache.fill(pointer, false);
                taskVariant = this.cache.taskVariants[pointer.variantIndex];
                GTBNode node = pointer.getNode();

                if (Objects.equals(variant.chromosome, node.chromosome) && taskVariant.position == variant.position) {
                    this.cache.fill(pointer, this.pairs.length > 0);
                    byte[] BEGs = new byte[this.pairs.length];
                    fillBEGs(taskVariant, node, BEGs, this.phasedTransfer);

                    // 标记位置值
                    variants.add(new Variant(node.chromosome, taskVariant.position, taskVariant.REF.values(), taskVariant.ALT.values(), BEGs, this.phased));
                    pointer.next();
                } else {
                    break;
                }
            }
            return variants.toArray();
        } else {
            // 不存在下一个位点，此时 返回 null
            return null;
        }
    }

    /**
     * 读取下一个位点
     */
    public boolean readVariant(Variant variant) throws IOException {
        if (pointer.chromosomeIndex != -1) {
            this.cache.fill(pointer, this.pairs.length > 0);
            TaskVariant taskVariant = this.cache.taskVariants[pointer.variantIndex];
            GTBNode node = pointer.getNode();

            if (this.pairs.length != variant.BEGs.length) {
                variant.BEGs = new byte[this.pairs.length];
            }

            byte[] BEGs = variant.BEGs;

            fillBEGs(taskVariant, node, BEGs, this.phasedTransfer);

            variant.chromosome = node.chromosome;
            variant.position = taskVariant.position;
            variant.REF = taskVariant.REF.values();
            variant.ALT = taskVariant.ALT.values();
            variant.phased = this.phased;

            pointer.next();
            return true;
        } else {
            // 不存在下一个位点，此时 返回 null
            variant.chromosome = null;
            variant.position = 0;

            variant.REF = null;
            variant.ALT = null;
            variant.phased = this.phased;
            return false;
        }
    }

    /**
     * 读取下一个位点 (公共坐标构成)
     */
    public boolean readVariant(Variant variant, Set<Integer> condition) throws IOException {
        if (condition == null) {
            return readVariant(variant);
        }

        TaskVariant taskVariant;
        while (pointer.chromosomeIndex != -1) {
            this.cache.fill(pointer, false);
            for (int i = pointer.variantIndex; i < pointer.variantLength; i++) {
                taskVariant = this.cache.taskVariants[i];
                if (condition.contains(taskVariant.position)) {
                    pointer.variantIndex = i;
                    return readVariant(variant);
                }
            }

            pointer.nextNode();
        }

        // 不存在下一个位点，此时 返回 null
        variant.chromosome = null;
        variant.position = 0;
        variant.REF = null;
        variant.ALT = null;
        variant.phased = this.phased;
        return false;
    }

    /**
     * 读取下一个位点 (公共坐标构成)
     */
    public boolean readVariant(Variant variant, Map<String, Set<Integer>> conditions) throws IOException {
        if (conditions == null) {
            return readVariant(variant);
        }

        TaskVariant taskVariant;
        out:
        while (pointer.chromosomeIndex != -1) {
            while (!conditions.containsKey(pointer.chromosomeList.get(pointer.chromosomeIndex))) {
                // 直接跳转到符合的染色体数据
                pointer.nextChromosome();

                if (pointer.chromosomeIndex == -1) {
                    break out;
                }
            }

            Set<Integer> condition = conditions.get(pointer.chromosomeList.get(pointer.chromosomeIndex));

            int chromosomeIndex = pointer.chromosomeIndex;
            int pointerChromosomeIndex = pointer.chromosomeIndex;
            while (chromosomeIndex == pointerChromosomeIndex) {
                this.cache.fill(pointer, false);

                for (int i = pointer.variantIndex; i < pointer.variantLength; i++) {
                    taskVariant = this.cache.taskVariants[i];
                    if (condition.contains(taskVariant.position)) {
                        pointer.variantIndex = i;
                        return readVariant(variant);
                    }
                }

                // 这个节点不包含任务位点
                pointer.nextNode();
                pointerChromosomeIndex = pointer.chromosomeIndex;
            }
        }

        // 不存在下一个位点，此时 返回 null
        variant.chromosome = null;
        variant.position = 0;
        variant.REF = null;
        variant.ALT = null;
        variant.phased = this.phased;
        return false;
    }

    /**
     * 读取下一个位点 (公共坐标构成)
     * 从 variantCache 中获取位点，并将结果放入另一区域
     */
    public Array<Variant> readVariants(BaseArray<Variant> variantCache) throws IOException {
        Variant variant;
        if (variantCache.size() == 0) {
            variant = new Variant();
        } else {
            variant = variantCache.popFirst();
        }

        if (!readVariant(variant)) {
            // 没有位点，返回长度为 0
            variantCache.add(variant);
            return null;
        } else {
            // 记录当前 位点
            int position = variant.position;
            Array<Variant> variants = new Array<>(Variant[].class);
            variants.add(variant);

            TaskVariant taskVariant;
            while (pointer.chromosomeIndex != -1) {
                if (variantCache.size() == 0) {
                    variant = new Variant();
                } else {
                    variant = variantCache.popFirst();
                }

                this.cache.fill(pointer, this.pairs.length > 0);
                taskVariant = this.cache.taskVariants[pointer.variantIndex];
                GTBNode node = pointer.getNode();

                if (Objects.equals(variant.chromosome, node.chromosome) && taskVariant.position == position) {
                    if (this.pairs.length != variant.BEGs.length) {
                        variant.BEGs = new byte[this.pairs.length];
                    }

                    byte[] BEGs = variant.BEGs;

                    fillBEGs(taskVariant, node, BEGs, this.phasedTransfer);

                    variant.chromosome = node.chromosome;
                    variant.position = taskVariant.position;
                    variant.REF = taskVariant.REF.values();
                    variant.ALT = taskVariant.ALT.values();
                    variant.phased = this.phased;
                    variants.add(variant);
                    pointer.next();
                } else {
                    variantCache.add(variant);
                    break;
                }
            }
            return variants;
        }
    }

    /**
     * 读取下一个位点 (公共坐标构成)
     * 从 variantCache 中获取位点，并将结果放入另一区域
     */
    public Array<Variant> readVariants(BaseArray<Variant> variantCache, Set<Integer> condition) throws IOException {
        if (condition == null) {
            return readVariants(variantCache);
        }

        Variant variant;
        if (variantCache.size() == 0) {
            variant = new Variant();
        } else {
            variant = variantCache.popFirst();
        }

        if (!readVariant(variant, condition)) {
            // 没有位点，返回长度为 0
            variantCache.add(variant);
            return null;
        } else {
            // 记录当前 位点
            int position = variant.position;
            Array<Variant> variants = new Array<>(Variant[].class);
            variants.add(variant);

            TaskVariant taskVariant;
            while (pointer.chromosomeIndex != -1) {
                if (variantCache.size() == 0) {
                    variant = new Variant();
                } else {
                    variant = variantCache.popFirst();
                }

                this.cache.fill(pointer, this.pairs.length > 0);
                taskVariant = this.cache.taskVariants[pointer.variantIndex];
                GTBNode node = pointer.getNode();

                if (Objects.equals(variant.chromosome, node.chromosome) && taskVariant.position == position) {
                    if (this.pairs.length != variant.BEGs.length) {
                        variant.BEGs = new byte[this.pairs.length];
                    }

                    byte[] BEGs = variant.BEGs;

                    fillBEGs(taskVariant, node, BEGs, this.phasedTransfer);

                    variant.chromosome = node.chromosome;
                    variant.position = taskVariant.position;
                    variant.REF = taskVariant.REF.values();
                    variant.ALT = taskVariant.ALT.values();
                    variant.phased = this.phased;

                    variants.add(variant);
                    pointer.next();
                } else {
                    variantCache.add(variant);
                    break;
                }
            }
            return variants;
        }
    }

    private void fillBEGs(TaskVariant taskVariant, GTBNode node, byte[] BEGs, boolean phasedTransfer) {
        if (taskVariant.decoderIndex == 0) {
            // 二等位基因位点
            int start = this.eachLineSize * taskVariant.index;
            if (phasedTransfer) {
                for (int j = 0; j < pairs.length; j++) {
                    BEGs[j] = BEGTransfer.toUnphased(this.groupDecoder.decode(this.cache.genotypesCache.cacheOf(start + this.pairs[j].groupIndex) & 0xFF, this.pairs[j].codeIndex));
                }

            } else {
                for (int j = 0; j < pairs.length; j++) {
                    BEGs[j] = this.groupDecoder.decode(this.cache.genotypesCache.cacheOf(start + this.pairs[j].groupIndex) & 0xFF, this.pairs[j].codeIndex);
                }
            }
        } else {
            // 多等位基因位点
            int start = this.eachLineSize * node.subBlockVariantNum[0] + (taskVariant.index - node.subBlockVariantNum[0]) * this.manager.getSubjectNum();

            if (phasedTransfer) {
                for (int j = 0; j < pairs.length; j++) {
                    BEGs[j] = BEGTransfer.toUnphased(this.cache.genotypesCache.cacheOf(start + this.pairs[j].index));
                }
            } else {
                for (int j = 0; j < pairs.length; j++) {
                    BEGs[j] = this.cache.genotypesCache.cacheOf(start + this.pairs[j].index);
                }
            }
        }
    }

    public void skip(int variantNums) throws IOException {
        for (int i = 0; i < variantNums; i++) {
            pointer.next();
        }
    }

    public void reset() {
        pointer.seek(0);
    }

    /**
     * 通过指针类进行跳转，请注意，该方法可能会破坏 limit 信息
     *
     * @param pointer 指针管理类
     */
    public void seek(Pointer pointer) {
        this.pointer = pointer;
    }

    /**
     * 通过指针类进行跳转
     *
     * @param chromosome 染色体编号
     */
    public void seek(String chromosome) {
        seek(chromosome, 0, 0);
    }

    /**
     * 通过指针类进行跳转
     *
     * @param chromosome 染色体编号
     * @param blockIndex 块编号
     */
    public void seek(String chromosome, int blockIndex) {
        seek(chromosome, blockIndex, 0);
    }

    /**
     * 通过指针类进行跳转
     *
     * @param chromosome   染色体编号
     * @param blockIndex   块编号
     * @param variantIndex 节点编号
     */
    public boolean seek(String chromosome, int blockIndex, int variantIndex) {
        int chromosomeIndex = pointer.chromosomeList.indexOf(chromosome);
        if (chromosomeIndex == -1) {
            // 如果是不存在的染色体, 则无法判断其相对位置
            return false;
        } else {
            this.pointer.seek(chromosomeIndex, blockIndex, variantIndex);
            return true;
        }
    }

    /**
     * 搜索, 并设置指针
     *
     * @param chromosome 染色体编号
     * @param position   位置值
     */
    public boolean search(String chromosome, int position) throws IOException {
        // 查看传入的染色体在当前的限定指针中的范围
        int chromosomeIndex = pointer.chromosomeList.indexOf(chromosome);
        if (chromosomeIndex == -1) {
            throw new IOException("pointer exception: chromosome=" + chromosome + " not found");
        }

        if (!this.searchEnable.get(chromosome)) {
            throw new IOException("unordered GTB files do not support random access");
        }

        GTBNodes nodes = this.manager.getGTBNodes(chromosome);

        int startNodeIndex;
        int endNodeIndex;
        if (pointer instanceof LimitPointer) {
            startNodeIndex = getStartNodeIndex(chromosome);
            endNodeIndex = getEndNodeIndex(chromosome);
        } else {
            startNodeIndex = 0;
            endNodeIndex = this.manager.getGTBNodes(chromosome).numOfNodes() - 1;
        }

        // 先检查边界
        if ((nodes.numOfNodes() == 0) || (nodes.get(endNodeIndex).maxPos < position)) {
            pointer.seek(chromosomeIndex + 1, 0, 0);
            return false;
        } else {
            if (nodes.get(startNodeIndex).minPos >= position) {
                pointer.seek(chromosomeIndex, startNodeIndex, 0);
                return nodes.get(startNodeIndex).minPos == position;
            }
        }

        if (!pointer.node.chromosome.equals(chromosome)) {
            pointer.seek(chromosomeIndex);
        }

        // 使用类似二分搜索的方法
        if (pointer.node.minPos > position) {
            for (int i = startNodeIndex; i < pointer.nodeIndex; i++) {
                if (nodes.get(i).contain(position)) {
                    pointer.setNode(i);
                    this.cache.fill(pointer, false);

                    if (this.cache.taskVariants[0].position == position) {
                        pointer.setVariant(0);
                        return true;
                    }

                    for (int j = 0; j < pointer.variantLength - 1; j++) {
                        if ((this.cache.taskVariants[j].position < position) && (position <= this.cache.taskVariants[j + 1].position)) {
                            pointer.setVariant(j + 1);
                            return position == this.cache.taskVariants[j + 1].position;
                        }
                    }
                }

                // 在两个块的间隔区
                if (nodes.get(i).maxPos < position && nodes.get(i + 1).minPos > position) {
                    System.out.println("我在边界哦！");
                    pointer.setNode(i + 1);
                    return false;
                }
            }
        } else {
            for (int i = pointer.nodeIndex; i <= endNodeIndex; i++) {
                if (nodes.get(i).contain(position)) {
                    pointer.setNode(i);
                    this.cache.fill(pointer, false);

                    if (this.cache.taskVariants[0].position == position) {
                        pointer.setVariant(0);
                        return true;
                    }

                    for (int j = 0; j < pointer.variantLength - 1; j++) {
                        if ((this.cache.taskVariants[j].position < position) && (position <= this.cache.taskVariants[j + 1].position)) {
                            pointer.setVariant(j + 1);
                            return position == this.cache.taskVariants[j + 1].position;
                        }
                    }
                }

                // 在两个块的间隔区
                if ((i < endNodeIndex) && (nodes.get(i).maxPos < position) && (nodes.get(i + 1).minPos > position)) {
                    pointer.setNode(i + 1);
                    return false;
                }
            }
        }

        throw new NullPointerException();
    }

    /**
     * 是否可随机访问
     *
     * @param chromosome 染色体编号
     */
    public boolean searchEnable(String chromosome) throws IOException {
        int chromosomeIndex = pointer.chromosomeList.indexOf(chromosome);
        if (chromosomeIndex == -1) {
            throw new IOException("pointer exception: chromosome=" + chromosome + " not found");
        }

        return this.searchEnable.get(chromosome);
    }

    public Pointer tell() {
        return pointer.clone();
    }

    public String[] getChromosomeList() {
        return pointer.chromosomeList.toArray();
    }

    public int getStartNodeIndex(String chromosome) throws IOException {
        if (pointer instanceof LimitPointer) {
            LimitPointer limitPointer = (LimitPointer) pointer;
            int chromosomeIndex = limitPointer.chromosomeList.indexOf(chromosome);
            if (chromosomeIndex == 0) {
                return limitPointer.startNodeIndex;
            } else if (chromosomeIndex == -1) {
                throw new IOException();
            } else {
                return 0;
            }
        } else {
            return 0;
        }
    }

    public int getEndNodeIndex(String chromosome) throws IOException {
        if (pointer instanceof LimitPointer) {
            LimitPointer limitPointer = (LimitPointer) pointer;
            int chromosomeIndex = limitPointer.chromosomeList.indexOf(chromosome);
            if (chromosomeIndex == limitPointer.chromosomeList.size() - 1) {
                return limitPointer.endNodeIndex - 1;
            } else if (chromosomeIndex == -1) {
                throw new IOException();
            } else {
                return this.pointer.manager.getGTBNodes(chromosome).numOfNodes() - 1;
            }
        } else {
            return this.pointer.manager.getGTBNodes(chromosome).numOfNodes() - 1;
        }
    }

    public void selectSubjects(int... subjectIndexes) {
        if (subjectIndexes == null) {
            initPairs(true);
        } else {
            int eachGroupNum = this.manager.isPhased() ? 3 : 4;
            this.subjectIndexes = subjectIndexes;

            pairs = new IndexPair[this.subjectIndexes.length];
            for (int i = 0; i < pairs.length; i++) {
                pairs[i] = new IndexPair(this.subjectIndexes[i], this.subjectIndexes[i] / eachGroupNum, this.subjectIndexes[i] % eachGroupNum);
            }
        }
    }

    public void selectSubjects(String... subjects) {
        selectSubjects(subjects == null ? null : this.manager.getSubjectIndex(subjects));
    }

    public void selectSubjects(BaseArray<String> subjects) {
        selectSubjects(this.manager.getSubjectIndex(subjects));
    }

    public void removeAllSubjects() {
        this.pairs = new IndexPair[0];
        this.subjectIndexes = new int[0];
    }

    public void selectAllSubjects() {
        initPairs(true);
    }

    public String[] getAllSubjects() {
        return this.manager.getSubjectManager().getAllSubjects();
    }

    public String[] getSelectedSubjects() {
        return this.manager.getSubject(subjectIndexes);
    }

    public int[] getSelectedSubjectsIndex() {
        return this.subjectIndexes;
    }

    public GTBManager getManager() {
        return manager;
    }

    public byte[] getHeader() {
        return getHeader(false);
    }

    public byte[] getHeader(boolean hideGT) {
        synchronized (headerCache) {
            if (headerCache.size() == 0) {
                initHeader();
            }
        }

        if (hideGT) {
            return headerCache.values();
        } else {
            VolumeByteStream out = new VolumeByteStream(headerCache.size() + 1 + this.manager.getSubjects().length);
            out.write(headerCache);
            out.write(ByteCode.TAB);

            if (this.subjectIndexes == null) {
                out.write(this.manager.getSubjects());
            } else {
                out.writeSafety(String.join("\t", this.manager.getSubject(this.subjectIndexes)).getBytes());
            }
            return out.values();
        }
    }

    public boolean hasNext() {
        return pointer.hasNext();
    }

    @Override
    public void close() throws IOException {
        this.cache.close();
        this.pointer = null;
    }

    @Override
    public Iterator<Variant> iterator() {
        return new Iterator<Variant>() {
            @Override
            public boolean hasNext() {
                return pointer.chromosomeIndex != -1;
            }

            @Override
            public Variant next() {
                try {
                    return readVariant();
                } catch (IOException e) {
                    throw new GTBComponentException(e.getMessage());
                }
            }
        };
    }
}

