package edu.sysu.pmglab.gbc.core.gtbcomponent;

import edu.sysu.pmglab.easytools.ValueUtils;
import edu.sysu.pmglab.gbc.core.gtbcomponent.gtbwriter.GTBOutputParam;

import java.util.Arrays;

/**
 * @Data        :2020/08/01
 * @Author      :suranyi
 * @Contact     :suranyi.sysu@gamil.com
 * @Description :文件基本信息管理器
 */

public class FileBaseInfoManager {
    private int estimateDecompressedBlockSizeFlag; // 4 bit
    private boolean orderedGTB;  // 1 bit
    private int compressorIndex;  // 2 bit, 0 means zstd, 1 means lzma
    private boolean phased; // 1 bit
    private int blockSizeType;  // 3 bit
    private int compressionLevel;  // 5 bit

    /**
     * 构造器方法
     * @param GTBBaseInfo1 GTB 文件基本信息第一个字节
     * @param GTBBaseInfo2 GTB 文件基本信息第二个字节
     */
    public FileBaseInfoManager(byte GTBBaseInfo1, byte GTBBaseInfo2) {
        load(GTBBaseInfo1, GTBBaseInfo2);
    }

    /**
     * 构造器方法
     * @param GTBBaseInfo GTB 文件基本信息
     */
    public FileBaseInfoManager(short GTBBaseInfo) {
        load(GTBBaseInfo);
    }

    public FileBaseInfoManager() {
    }

    /**
     * 构造器方法
     * @param orderedGTB 是否支持随机访问
     * @param compressorIndex 压缩器索引
     * @param phased 向型
     * @param blockSizeType 块大小类型参数
     * @param compressionLevel 压缩器级别
     */
    public FileBaseInfoManager(boolean orderedGTB, int compressorIndex, boolean phased, int blockSizeType, int compressionLevel) {
        this.estimateDecompressedBlockSizeFlag = 0;
        this.orderedGTB = orderedGTB;
        this.compressorIndex = compressorIndex;
        this.phased = phased;
        this.blockSizeType = blockSizeType;
        this.compressionLevel = compressionLevel;
    }

    /**
     * 静态构造器方法
     * @param outputParam 文件细节参数
     */
    public static FileBaseInfoManager of(GTBOutputParam outputParam) {
        return new FileBaseInfoManager(false, outputParam.getCompressor(), outputParam.isPhased(), outputParam.getBlockSizeType(), outputParam.getCompressionLevel());
    }

    /**
     * 加载新的基本信息
     * @param GTBBaseInfo GTB 文件基本信息
     */
    public void load(short GTBBaseInfo) {
        this.estimateDecompressedBlockSizeFlag = (((GTBBaseInfo >> 3) & 0x1) << 3) +
                (((GTBBaseInfo >> 2) & 0x1) << 2) +
                (((GTBBaseInfo >> 1) & 0x1) << 1) +
                (GTBBaseInfo & 0x1);

        this.orderedGTB = ((GTBBaseInfo >> 4) & 0x1) == 1;

        this.compressorIndex = (((GTBBaseInfo >> 6) & 0x1) << 1) + ((GTBBaseInfo >> 5) & 0x1);

        this.phased = ((GTBBaseInfo >> 7) & 0x1) == 1;

        this.blockSizeType = (((GTBBaseInfo >> 10) & 0x1) << 2) +
                (((GTBBaseInfo >> 9) & 0x1) << 1) +
                ((GTBBaseInfo >> 8) & 0x1);

        this.compressionLevel = (((GTBBaseInfo >> 15) & 0x1) << 4) +
                (((GTBBaseInfo >> 14) & 0x1) << 3) +
                (((GTBBaseInfo >> 13) & 0x1) << 2) +
                (((GTBBaseInfo >> 12) & 0x1) << 1) +
                ((GTBBaseInfo >> 11) & 0x1);
    }

    /**
     * 加载新的基本信息
     * @param GTBBaseInfo GTB 文件基本信息
     */
    public void load(byte[] GTBBaseInfo) {
        load(GTBBaseInfo[0], GTBBaseInfo[1]);
    }

    /**
     * 加载新的基本信息
     * @param GTBBaseInfo1 GTB 文件基本信息第一个字节
     * @param GTBBaseInfo2 GTB 文件基本信息第二个字节
     */
    public void load(byte GTBBaseInfo1, byte GTBBaseInfo2) {
        this.estimateDecompressedBlockSizeFlag = (((GTBBaseInfo1 >> 3) & 0x1) << 3) + (((GTBBaseInfo1 >> 2) & 0x1) << 2) +
                (((GTBBaseInfo1 >> 1) & 0x1) << 1) + (GTBBaseInfo1 & 0x1);

        this.orderedGTB = ((GTBBaseInfo1 >> 4) & 0x1) == 1;

        this.compressorIndex = (((GTBBaseInfo1 >> 6) & 0x1) << 1) + ((GTBBaseInfo1 >> 5) & 0x1);

        this.phased = ((GTBBaseInfo1 >> 7) & 0x1) == 1;

        this.blockSizeType = ((((GTBBaseInfo2 >> 2) & 0x1) << 2) +
                (((GTBBaseInfo2 >> 1) & 0x1) << 1) +
                ((GTBBaseInfo2) & 0x1));

        this.compressionLevel = ((((GTBBaseInfo2 >> 7) & 0x1) << 4) +
                (((GTBBaseInfo2 >> 6) & 0x1) << 3) +
                (((GTBBaseInfo2 >> 5) & 0x1) << 2) +
                (((GTBBaseInfo2 >> 4) & 0x1) << 1) +
                (((GTBBaseInfo2 >> 3) & 0x1)));
    }

    /**
     * 文件是否完成压缩
     */
    public boolean isFinish() {
        return this.estimateDecompressedBlockSizeFlag > 0;
    }

    /**
     * 获取解压块的预估大小
     */
    public int getEstimateDecompressedBlockSize() {
        switch (this.estimateDecompressedBlockSizeFlag) {
            case 1:
                return 2 * 1024 * 1024;
            case 2:
                return 4 * 1024 * 1024;
            case 3:
                return 8 * 1024 * 1024;
            case 4:
                return 16 * 1024 * 1024;
            case 5:
                return 32 * 1024 * 1024;
            case 6:
                return 64 * 1024 * 1024;
            case 7:
                return 128 * 1024 * 1024;
            case 8:
                return 192 * 1024 * 1024;
            case 9:
                return 256 * 1024 * 1024;
            case 10:
                return 384 * 1024 * 1024;
            case 11:
                return 512 * 1024 * 1024;
            case 12:
                return 768 * 1024 * 1024;
            case 13:
                return 896 * 1024 * 1024;
            case 14:
                return 1024 * 1024 * 1024;
            case 15:
                // WARNING 这一步代表预估数据大小将超过 1GB，需要使用 BGZ 压缩模式。极端情况下确实有可能发生错误
                return (int) (1.25 * 1024 * 1024 * 1024);
            default:
                // 无大小数据
                return 0;
        }
    }

    /**
     * 获取解压块的预估大小参数
     */
    public int getEstimateDecompressedBlockSizeFlag() {
        return this.estimateDecompressedBlockSizeFlag;
    }

    /**
     * 获得GTB文件是否有序
     */
    public boolean orderedGTB() {
        return this.orderedGTB;
    }

    /**
     * 获取压缩器
     */
    public int getCompressorIndex() {
        return this.compressorIndex;
    }

    /**
     * 获得向型
     */
    public boolean isPhased() {
        return this.phased;
    }

    /**
     * 是否建议解压为 bgzf格式
     */
    public boolean isSuggestToBGZF() {
        return this.estimateDecompressedBlockSizeFlag == 15;
    }

    /**
     * 获得块尺寸类型参数
     */
    public int getBlockSizeType() {
        return this.blockSizeType;
    }

    /**
     * 获取块大小
     * @return 2 ^ (7 + blockSizeType)
     */
    public int getBlockSize() {
        return BlockSizeParameter.getBlockSize(blockSizeType);
    }

    /**
     * 获得压缩器参数
     */
    public int getCompressionLevel() {
        return this.compressionLevel;
    }

    /**
     * 设置解压数据大小
     */
    public void setEstimateDecompressedBlockSize(int estimateSize) {
        if (estimateSize <= 128 * 1024 * 1024) {
            // size <= 128 MB
            if (estimateSize <= 8 * 1024 * 1024) {
                // size <= 8 MB
                if (estimateSize <= 2 * 1024 * 1024) {
                    // size <= 2 MB
                    this.estimateDecompressedBlockSizeFlag = 1;
                } else if (estimateSize <= 4 * 1024 * 1024) {
                    // 2 MB < size <= 4 MB
                    this.estimateDecompressedBlockSizeFlag = 2;
                } else {
                    // 4 MB < size <= 8 MB
                    this.estimateDecompressedBlockSizeFlag = 3;
                }
            } else {
                if (estimateSize <= 16 * 1024 * 1024) {
                    // 8 MB < size <= 16 MB
                    this.estimateDecompressedBlockSizeFlag = 4;
                } else if (estimateSize <= 32 * 1024 * 1024) {
                    // 16 MB < size <= 32 MB
                    this.estimateDecompressedBlockSizeFlag = 5;
                } else if (estimateSize <= 64 * 1024 * 1024) {
                    // 32 MB < size <= 64 MB
                    this.estimateDecompressedBlockSizeFlag = 6;
                } else {
                    // 64 MB < size <= 128 MB
                    this.estimateDecompressedBlockSizeFlag = 7;
                }
            }
        } else {
            // size <= 512 MB
            if (estimateSize <= 512 * 1024 * 1024) {
                if (estimateSize <= 192 * 1024 * 1024) {
                    // 128 MB < size <= 192 MB
                    this.estimateDecompressedBlockSizeFlag = 8;
                } else if (estimateSize <= 256 * 1024 * 1024) {
                    // 192 MB < size <= 256 MB
                    this.estimateDecompressedBlockSizeFlag = 9;
                } else if (estimateSize <= 384 * 1024 * 1024) {
                    // 256 MB < size <= 384 MB
                    this.estimateDecompressedBlockSizeFlag = 10;
                } else {
                    // 384 MB < size <= 512 MB
                    this.estimateDecompressedBlockSizeFlag = 11;
                }
            } else {
                if (estimateSize <= 768 * 1024 * 1024) {
                    // 640 MB < size <= 768 MB
                    this.estimateDecompressedBlockSizeFlag = 12;
                } else if (estimateSize <= 896 * 1024 * 1024) {
                    // 768 MB < size <= 896 MB
                    this.estimateDecompressedBlockSizeFlag = 13;
                } else if (estimateSize <= 1024 * 1024 * 1024) {
                    // 896 MB < size <= 1 GB
                    this.estimateDecompressedBlockSizeFlag = 14;
                } else {
                    // size > 1 G
                    this.estimateDecompressedBlockSizeFlag = 15;
                }
            }
        }
    }

    /**
     * 获得GTB文件是否有序
     * @param orderedGTB 是否支持随机访问
     */
    public void setOrderedGTB(boolean orderedGTB) {
        this.orderedGTB = orderedGTB;
    }

    /**
     * 设置压缩器
     * @param compressorIndex 设置压缩器
     */
    public void setCompressorIndex(int compressorIndex) {
        this.compressorIndex = compressorIndex;
    }

    /**
     * 设置向型
     * @param phased 向型
     */
    public void setPhased(boolean phased) {
        this.phased = phased;
    }

    /**
     * 设置块尺寸参数
     * @param blockSizeType 块尺寸参数
     */
    public void setBlockSizeType(int blockSizeType) {
        this.blockSizeType = blockSizeType;
    }

    /**
     * 设置压缩器参数
     * @param compressionLevel 压缩器参数
     */
    public void setCompressionLevel(int compressionLevel) {
        this.compressionLevel = compressionLevel;
    }

    @Override
    public String toString() {
        byte[] codes = build();
        StringBuilder bitCode = new StringBuilder();
        for (byte code : codes) {
            for (byte bit : ValueUtils.getBitCode(code, true)) {
                bitCode.append(bit);
            }
        }
        return "FileBaseInfoManager{" +
                "estimateDecompressedBlockSizeFlag=" + estimateDecompressedBlockSizeFlag +
                ", orderedGTB=" + orderedGTB +
                ", compressor=" + compressorIndex +
                ", phased=" + phased +
                ", blockSizeType=" + blockSizeType +
                ", compressionLevel=" + compressionLevel +
                ", byteCode=" + Arrays.toString(codes) +
                ", bitCode=" + bitCode +
                '}';
    }

    /**
     * 将基本信息组装为 short 类型数值
     */
    public byte[] build() {
        return new byte[]{(byte) ((this.estimateDecompressedBlockSizeFlag) + ((this.orderedGTB ? 1 : 0) << 4) + (this.compressorIndex << 5) + ((this.phased ? 1 : 0) << 7)),
                (byte) ((this.blockSizeType) + (this.compressionLevel << 3))};
    }
}
