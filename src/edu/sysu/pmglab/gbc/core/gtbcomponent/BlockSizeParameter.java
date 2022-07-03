package edu.sysu.pmglab.gbc.core.gtbcomponent;

import edu.sysu.pmglab.check.Assert;
import edu.sysu.pmglab.easytools.ValueUtils;
import edu.sysu.pmglab.gbc.core.exception.GTBComponentException;

/**
 * @author suranyi
 * @description 块大小参数池 (最大 1 GB 大小设计, i.e., 1073741824 byte)
 */

public enum BlockSizeParameter {
    /**
     * 块大小参数 (blockSize * subjectNum ~ 1073741824)
     */
    ZERO(0, 64, 16777216),
    ONE(1, 256, 4194304),
    TWO(2, 512, 2097152),
    THREE(3, 1024, 1048576),
    FOUR(4, 2048, 524288),
    FIVE(5, 4096, 262144),
    SIX(6, 8192, 131072),
    SEVEN(7, 16384, 65536),

    /**
     * 建议的参数值大小，该数值基于 256 MB 缓冲大小设计，以尽可能提高并行性能  (blockSize * subjectNum ~ 268435456)
     */
    SUGGEST_ZERO(0, 64, 16777216),
    SUGGEST_ONE(1, 256, 1048576),
    SUGGEST_TWO(2, 512, 524288),
    SUGGEST_THREE(3, 1024, 262144),
    SUGGEST_FOUR(4, 2048, 131072),
    SUGGEST_FIVE(5, 4096, 65536),
    SUGGEST_SIX(6, 8192, 32768),
    SUGGEST_SEVEN(7, 16384, 16384);

    final int blockSizeType;
    final int blockSize;
    final int maxSubjectNum;

    public static final BlockSizeParameter[] VALUES = {ZERO, ONE, TWO, THREE, FOUR, FIVE, SIX, SEVEN};
    public static final BlockSizeParameter[] SUGGEST_VALUES = {SUGGEST_ZERO, SUGGEST_ONE, SUGGEST_TWO, SUGGEST_THREE, SUGGEST_FOUR, SUGGEST_FIVE, SUGGEST_SIX, SUGGEST_SEVEN};

    /**
     * 块大小默认参数，-1 代表自动选择
     */
    public static final int DEFAULT_BLOCK_SIZE_TYPE = -1;
    public static final int DEFAULT_BLOCK_SIZE = -1;
    public static final int MIN_BLOCK_SIZE_TYPE = 0;
    public static final int MAX_BLOCK_SIZE_TYPE = 7;

    BlockSizeParameter(int blockSizeType, int blockSize, int maxSubjectNum) {
        this.blockSizeType = blockSizeType;
        this.blockSize = blockSize;
        this.maxSubjectNum = maxSubjectNum;
    }

    /**
     * 传入块参数值对应的块大小真实值
     *
     * @param blockSizeType 块大小参数，该值的取值范围为 0～7
     * @return 块大小参数值
     */
    public static int getBlockSize(int blockSizeType) {
        Assert.valueRange(blockSizeType, MIN_BLOCK_SIZE_TYPE, MAX_BLOCK_SIZE_TYPE);
        return VALUES[blockSizeType].blockSize;
    }

    /**
     * 获取 GTB 最大可容纳的样本数
     *
     * @return 当前最大可处理的样本个数
     */
    public static int getMaxSubjectNum() {
        return VALUES[0].maxSubjectNum;
    }

    /**
     * 获取对应的块大小参数值最大可容纳的样本数
     *
     * @param blockSizeType 块大小参数，该值的取值范围为 0～7
     * @return 该块大小参数值可以处理的最大样本个数
     */
    public static int getMaxSubjectNum(int blockSizeType) {
        Assert.valueRange(blockSizeType, MIN_BLOCK_SIZE_TYPE, MAX_BLOCK_SIZE_TYPE);
        return VALUES[blockSizeType].maxSubjectNum;
    }

    /**
     * 获取支持的最大块大小参数
     * 计算方法：位点数 * 样本数 <= 1GB
     */
    public static int getMaxBlockSizeType(int validSubjectNum) {
        Assert.NotNegativeValue(validSubjectNum);

        // 从后往前检索 (越靠后，能够处理的样本个数越少)
        for (int i = VALUES.length - 1; i >= 0; i--) {
            if (validSubjectNum < VALUES[i].maxSubjectNum) {
                return VALUES[i].blockSizeType;
            }
        }

        throw new GTBComponentException(String.format("validSubjectNum is greater than the current maximum that GBC can handle (%d > %d)", validSubjectNum, VALUES[0].maxSubjectNum));
    }

    /**
     * 获取支持的最大块大小参数
     * 计算方法：位点数 * 样本数 <= 2GB - 2, 取 1GB 大小，以平衡解压时的内存开销
     */
    public static int getSuggestBlockSizeType(int validSubjectNum) {
        Assert.NotNegativeValue(validSubjectNum);

        // 从后往前检索 (越靠后，能够处理的样本个数越少)
        for (int i = SUGGEST_VALUES.length - 1; i >= 0; i--) {
            if (validSubjectNum < SUGGEST_VALUES[i].maxSubjectNum) {
                return SUGGEST_VALUES[i].blockSizeType;
            }
        }

        throw new GTBComponentException(String.format("validSubjectNum is greater than the current maximum that GBC can handle (%d > %d)", validSubjectNum, VALUES[0].maxSubjectNum));
    }

    /**
     * 获取支持的最大块大小参数
     * 计算方法：位点数 * 样本数 <= 256 MB, 以平衡解压时的内存开销
     */
    public static int getSuggestBlockSizeType(int currentBlockSizeType, int validSubjectNum) {
        if (currentBlockSizeType == -1) {
            // 自动选择合适参数
            return getSuggestBlockSizeType(validSubjectNum);
        } else {
            // 判断该参数是否合法，如果不合法则自动向下选择
            return ValueUtils.min(currentBlockSizeType, getMaxBlockSizeType(validSubjectNum));
        }
    }

    /**
     * 获取支持的块大小参数
     */
    public static Integer[] getSupportedBlockSizes() {
        Integer[] sizes = new Integer[VALUES.length + 1];
        sizes[0] = -1;
        for (int i = 0; i < VALUES.length; i++) {
            sizes[i + 1] = VALUES[i].blockSize;
        }
        return sizes;
    }
}