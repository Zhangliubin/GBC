package edu.sysu.pmglab.gbc.core.gtbcomponent.gtbwriter;

import edu.sysu.pmglab.check.Assert;
import edu.sysu.pmglab.compressor.ICompressor;
import edu.sysu.pmglab.container.File;
import edu.sysu.pmglab.easytools.ValueUtils;
import edu.sysu.pmglab.gbc.coder.CoderConfig;
import edu.sysu.pmglab.gbc.core.common.qualitycontrol.variant.*;
import edu.sysu.pmglab.gbc.core.common.switcher.ISwitcher;
import edu.sysu.pmglab.gbc.core.gtbcomponent.BlockSizeParameter;
import edu.sysu.pmglab.gbc.core.gtbcomponent.FileBaseInfoManager;
import edu.sysu.pmglab.gbc.core.gtbcomponent.GTBManager;
import edu.sysu.pmglab.unifyIO.FileStream;

import java.io.IOException;

/**
 * @author suranyi
 */

public class GTBOutputParam {
    /**
     * 设置 GTB 的参数
     */
    private boolean phased = CoderConfig.DEFAULT_PHASED_STATUS;
    private boolean reordering = ISwitcher.DEFAULT_ENABLE;
    private boolean splitMultiallelics = false;
    private boolean simplyAllele = false;
    private int windowSize = ISwitcher.DEFAULT_SIZE;
    private int compressor = ICompressor.DEFAULT;
    private int compressionLevel = ICompressor.getDefaultCompressionLevel(this.compressor);
    private int blockSizeType = BlockSizeParameter.DEFAULT_BLOCK_SIZE_TYPE;
    private int blockSize = BlockSizeParameter.DEFAULT_BLOCK_SIZE;
    private int maxAlleleNums = CoderConfig.MAX_ALLELE_NUM;

    /**
     * 等位基因计数控制器
     */
    private final VariantQC variantQC = new VariantQC();

    public GTBOutputParam() {
    }

    public GTBOutputParam(GTBManager manager) throws IOException {
        readyParas(manager);
    }

    public GTBOutputParam(File managerFile) throws IOException {
        readyParas(managerFile);
    }

    /**
     * 获取当前任务是否将文件编码为有向数据
     */
    public boolean isPhased() {
        return this.phased;
    }

    /**
     * 获取当前任务是否要对位点进行重排列
     */
    public boolean isReordering() {
        return this.reordering;
    }

    /**
     * 获取排列窗口大小
     */
    public int getWindowSize() {
        return this.windowSize;
    }

    /**
     * 获取压缩器, 0 代表默认的快速的 zstd, 1 代表高压缩比的 lzma,
     */
    public int getCompressor() {
        return this.compressor;
    }

    /**
     * 获取 ZSTD 压缩器压缩参数
     */
    public int getCompressionLevel() {
        return this.compressionLevel;
    }

    /**
     * 获取块大小类型
     */
    public int getBlockSizeType() {
        return this.blockSizeType;
    }

    /**
     * 获取块大小
     */
    public int getBlockSize() {
        return this.blockSize;
    }

    /**
     * 获取等位基因过滤器
     */
    public VariantQC getVariantQC() {
        return this.variantQC;
    }

    /**
     * 设置基因型向型, 将作用于 MBEG 编码器
     *
     * @param phased 向型
     */
    public GTBOutputParam setPhased(boolean phased) {
        this.phased = phased;

        return this;
    }

    /**
     * 设置是否使用重排列算法, 能够显著 (> 10%) 提高压缩比
     *
     * @param reordering 使用重排列算法
     */
    public GTBOutputParam setReordering(boolean reordering) {
        this.reordering = reordering;

        return this;
    }

    /**
     * 设置排列窗口大小
     *
     * @param windowSize 窗口大小
     */
    public GTBOutputParam setWindowSize(int windowSize) {
        Assert.valueRange(windowSize, ISwitcher.MIN, ISwitcher.MAX);
        this.windowSize = windowSize;

        return this;
    }

    /**
     * 设置压缩器
     *
     * @param compressorIndex 压缩器索引
     */
    public GTBOutputParam setCompressor(int compressorIndex) {
        this.compressor = compressorIndex;
        this.compressionLevel = ICompressor.getDefaultCompressionLevel(compressorIndex);

        return this;
    }

    /**
     * 设置压缩器
     *
     * @param compressorIndex  压缩器索引
     * @param compressionLevel 压缩参数
     */
    public GTBOutputParam setCompressor(int compressorIndex, int compressionLevel) {
        // -1 表示自动设置
        if (compressionLevel == -1) {
            return setCompressor(compressorIndex);
        }

        Assert.valueRange(compressionLevel, ICompressor.getMinCompressionLevel(compressorIndex), ICompressor.getMaxCompressionLevel(compressorIndex));

        this.compressor = compressorIndex;
        this.compressionLevel = compressionLevel;

        return this;
    }

    /**
     * 设置压缩器
     *
     * @param compressorName 压缩器名
     */
    public GTBOutputParam setCompressor(String compressorName) {
        return setCompressor(ICompressor.getCompressorIndex(compressorName));
    }

    /**
     * 设置压缩器
     *
     * @param compressorName   压缩器名
     * @param compressionLevel 压缩参数
     */
    public GTBOutputParam setCompressor(String compressorName, int compressionLevel) {
        return setCompressor(ICompressor.getCompressorIndex(compressorName), compressionLevel);
    }

    /**
     * 设置块大小参数
     *
     * @param blockSizeType 块参数类型
     */
    public GTBOutputParam setBlockSizeType(int blockSizeType) {
        if (blockSizeType == -1) {
            this.blockSizeType = -1;
            this.blockSize = -1;
        } else {
            Assert.valueRange(blockSizeType, BlockSizeParameter.MIN_BLOCK_SIZE_TYPE, BlockSizeParameter.MAX_BLOCK_SIZE_TYPE);
            this.blockSize = BlockSizeParameter.getBlockSize(blockSizeType);
            this.blockSizeType = blockSizeType;
        }

        return this;
    }

    /**
     * 设置过滤方式
     * <p>
     * minAc 最小 allele count 计数
     * maxAc 最大 allele count 计数
     */
    public GTBOutputParam filterByAC(int... ACRange) {
        if (ACRange == null || ACRange.length == 0) {
            return this;
        }

        if (ACRange.length == 1) {
            return addVariantQC(new AlleleCountController(ACRange[0], AlleleCountController.MAX));
        } else if (ACRange.length == 2) {
            return addVariantQC(new AlleleCountController(ACRange[0], ACRange[1]));
        } else {
            throw new IllegalArgumentException("filterByAC(minAC) or filterByAC(minAC, maxAC)");
        }
    }

    /**
     * 设置过滤方式
     * <p>
     * minAf 最小 allele frequency
     * maxAf 最大 allele frequency
     */
    public GTBOutputParam filterByAF(double... AFRange) {
        if (AFRange == null || AFRange.length == 0) {
            return this;
        }

        if (AFRange.length == 1) {
            return addVariantQC(new AlleleFrequencyController(AFRange[0], AlleleFrequencyController.MAX));
        } else if (AFRange.length == 2) {
            return addVariantQC(new AlleleFrequencyController(AFRange[0], AFRange[1]));
        } else {
            throw new IllegalArgumentException("filterByAF(minAF) or filterByAF(minAF, maxAF)");
        }
    }

    /**
     * 设置过滤方式
     * <p>
     * minAn 最小有效 allele 计数
     * maxAn 最大有效 allele 计数
     */
    public GTBOutputParam filterByAN(int... ANRange) {
        if (ANRange == null || ANRange.length == 0) {
            return this;
        }

        if (ANRange.length == 1) {
            return addVariantQC(new AlleleNumberController(ANRange[0], AlleleNumberController.MAX));
        } else if (ANRange.length == 2) {
            return addVariantQC(new AlleleNumberController(ANRange[0], ANRange[1]));
        } else {
            throw new IllegalArgumentException("filterByAN(minAN) or filterByAN(minAN, maxAN)");
        }
    }

    /**
     * 添加过滤器
     *
     * @param filter 过滤器
     */
    public GTBOutputParam addVariantQC(IVariantQC filter) {
        this.variantQC.add(filter);
        return this;
    }

    /**
     * 清空过滤器
     */
    public GTBOutputParam clearVariantQC() {
        this.variantQC.clear();
        return this;
    }

    /**
     * 设置位点等位基因最大个数
     *
     * @param maxAlleleNums 等位基因最大个数
     */
    public GTBOutputParam setMaxAllelesNum(int maxAlleleNums) {
        Assert.valueRange(maxAlleleNums, 2, CoderConfig.MAX_ALLELE_NUM);
        this.maxAlleleNums = maxAlleleNums;

        return this;
    }

    /**
     * 最大等位基因个数
     */
    public int getMaxAlleleNums() {
        return this.maxAlleleNums;
    }

    /**
     * 是否分解多等位基因位点
     */
    public boolean isSplitMultiallelics() {
        return this.splitMultiallelics;
    }

    /**
     * 是否精简等位基因
     */
    public boolean isSimplyAllele() {
        return simplyAllele;
    }

    /**
     * 从现有的 GTB 文件中加载参数
     * 请注意, 此处没有验证完整的 GTB 文件
     */
    public GTBOutputParam readyParas(File gtbFile) throws IOException {
        if (gtbFile != null) {
            try (FileStream params = gtbFile.open()) {
                FileBaseInfoManager baseInfo = new FileBaseInfoManager(ValueUtils.byteArray2ShortValue(params.read(2)));

                this.phased = baseInfo.isPhased();
                this.compressor = baseInfo.getCompressorIndex();
                this.blockSizeType = baseInfo.getBlockSizeType();
                this.blockSize = baseInfo.getBlockSize();
                this.compressionLevel = baseInfo.getCompressionLevel();
            }
        }

        return this;
    }

    /**
     * 从现有的 GTB 文件中加载参数
     * 请注意, 此处没有验证完整的 GTB 文件
     */
    public GTBOutputParam readyParas(GTBManager manager) throws IOException {
        if (manager != null) {
            try (FileStream params = manager.getFileStream()) {
                FileBaseInfoManager baseInfo = new FileBaseInfoManager(ValueUtils.byteArray2ShortValue(params.read(2)));

                this.phased = baseInfo.isPhased();
                this.compressor = baseInfo.getCompressorIndex();
                this.blockSizeType = baseInfo.getBlockSizeType();
                this.blockSize = baseInfo.getBlockSize();
                this.compressionLevel = baseInfo.getCompressionLevel();
            }
        }

        return this;
    }

    /**
     * 分裂多等位基因位点 (该方法是 setMaxAlleleNums 的补充)
     */
    public GTBOutputParam splitMultiallelics(boolean split) {
        this.splitMultiallelics = split;

        return this;
    }

    /**
     * 精简等位基因位点, 删除 AC = 0 的 ALT 碱基
     *
     * @param simplyAllele 精简等位基因位点
     */
    public GTBOutputParam simplyAllele(boolean simplyAllele) {
        this.simplyAllele = simplyAllele;

        return this;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("phased: " + phased);

        if (this.splitMultiallelics) {
            builder.append("\nsplit multiallelics: true");
        } else {
            builder.append("\nallelesNum: <= " + this.maxAlleleNums);
        }

        if (this.simplyAllele) {
            builder.append("\nsimply alleles: true");
        }

        builder.append("\nblock size: " + this.blockSize);
        builder.append("\nbasic compressor: " + ICompressor.getCompressorName(this.compressor) + " (level: " + this.compressionLevel + ")");

        builder.append("\nAMDO: " + this.reordering + (this.reordering ? " (window size: " + this.windowSize + ")" : ""));

        if (variantQC.size() > 0) {
            builder.append("\nvariant QC: " + this.variantQC);
        }

        return builder.toString();
    }
}
