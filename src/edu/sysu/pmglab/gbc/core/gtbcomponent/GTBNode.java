package edu.sysu.pmglab.gbc.core.gtbcomponent;

import edu.sysu.pmglab.container.VolumeByteStream;
import edu.sysu.pmglab.easytools.ValueUtils;
import edu.sysu.pmglab.gbc.constant.ChromosomeTags;

import java.util.Arrays;
import java.util.Objects;

/**
 * @Data :2020/04/20
 * @Author :suranyi
 * @Contact :suranyi.sysu@gamil.com
 * @Description :GTB 叶子结点，组织一个 block 的信息
 */

public class GTBNode implements Comparable<GTBNode>, Cloneable {
    /**
     * 节点信息
     */
    public final String chromosome;
    public final int minPos;
    public final int maxPos;
    public final long blockSeek;
    public final int blockSize;
    public final short[] subBlockVariantNum;

    /**
     * 数据大小标记信息
     */
    public final int compressedGenotypesSize;
    public final int compressedPosSize;
    public final int compressedAlleleSize;

    /**
     * 魔术码，该码用于提示原数据的近似大小
     */
    public final byte magicCode;

    /**
     * 根结点编号，用于合并不同来源的GTB文件
     */
    private int rootIndex;

    /**
     * 构造器方法
     *
     * @param chromosome         染色体编号 (索引)
     * @param minPos             最小位置
     * @param maxPos             最大位置
     * @param blockSeek          块数据段指针
     * @param genotypeSize       基因型压缩块大小
     * @param posSize            位置压缩块大小
     * @param alleleSize         等位基因压缩块大小
     * @param magicCode          原始数据大小的魔术码
     * @param subBlockVariantNum 子块变异位点数量
     */
    public GTBNode(String chromosome, int minPos, int maxPos, long blockSeek, int genotypeSize, int posSize, int alleleSize,
                   byte magicCode, short[] subBlockVariantNum) {
        this.chromosome = chromosome;
        this.minPos = minPos;
        this.maxPos = maxPos;
        this.blockSeek = blockSeek;
        this.compressedGenotypesSize = genotypeSize;
        this.compressedPosSize = posSize;
        this.compressedAlleleSize = alleleSize;
        this.subBlockVariantNum = subBlockVariantNum;
        this.magicCode = magicCode;
        this.blockSize = alleleSize + genotypeSize + posSize;
    }

    /**
     * 构造器方法
     *
     * @param chromosome         染色体编号
     * @param minPos             最小位置
     * @param maxPos             最大位置
     * @param blockSeek          块数据段指针
     * @param genotypeSize       基因型压缩块大小
     * @param posSize            位置压缩块大小
     * @param alleleSize         等位基因压缩块大小
     * @param subBlockVariantNum 子块变异位点数量
     */
    public GTBNode(String chromosome, int minPos, int maxPos, long blockSeek, int genotypeSize, int posSize, int alleleSize,
                   int originMBEGsSize, int originAllelesSize, short[] subBlockVariantNum) {
        this(chromosome, minPos, maxPos, blockSeek, genotypeSize, posSize, alleleSize, calculateMagicCode(originMBEGsSize, originAllelesSize), subBlockVariantNum);
    }


    /**
     * 获取根索引
     *
     * @return 所属根索引
     */
    public int getRootIndex() {
        return this.rootIndex;
    }

    /**
     * 绑定根结点
     *
     * @param rootIndex 根结点编号
     */
    public void bind(int rootIndex) {
        this.rootIndex = rootIndex;
    }

    /**
     * 获取块总变异位点数量
     *
     * @return 当前块组织的变异位点总数
     */
    public int numOfVariants() {
        return this.subBlockVariantNum[0] + this.subBlockVariantNum[1];
    }

    /**
     * 检验该块是否有可能包含该任务节点
     *
     * @param taskPos 任务位置值
     * @return 请求任务进行边界测试的结果
     */
    public boolean contain(int taskPos) {
        return (taskPos >= this.minPos) && (taskPos <= this.maxPos);
    }

    @Override
    public GTBNode clone() {
        // 克隆方法
        return new GTBNode(this.chromosome, this.minPos, this.maxPos, this.blockSeek, this.compressedGenotypesSize, this.compressedPosSize, this.compressedAlleleSize, this.magicCode,
                new short[]{subBlockVariantNum[0], subBlockVariantNum[1]});
    }

    /**
     * 重设染色体编号
     *
     * @param newChromosome 新染色体编号
     */
    GTBNode resetChromosome(String newChromosome) {
        // 克隆方法
        return new GTBNode(newChromosome, this.minPos, this.maxPos, this.blockSeek, this.compressedGenotypesSize, this.compressedPosSize, this.compressedAlleleSize, this.magicCode,
                new short[]{subBlockVariantNum[0], subBlockVariantNum[1]});
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GTBNode)) {
            return false;
        }
        GTBNode gtbNode = (GTBNode) o;
        return Objects.equals(chromosome, gtbNode.chromosome) &&
                minPos == gtbNode.minPos &&
                maxPos == gtbNode.maxPos &&
                blockSize == gtbNode.blockSize &&
                compressedPosSize == gtbNode.compressedPosSize &&
                compressedAlleleSize == gtbNode.compressedAlleleSize &&
                compressedGenotypesSize == gtbNode.compressedGenotypesSize &&
                magicCode == gtbNode.magicCode &&
                Arrays.equals(subBlockVariantNum, gtbNode.subBlockVariantNum);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(chromosome, minPos, maxPos, compressedGenotypesSize, compressedPosSize, compressedAlleleSize, magicCode);
        result = 31 * result + Arrays.hashCode(subBlockVariantNum);
        return result;
    }

    @Override
    public String toString() {
        return String.format("posRange=[%d, %d], seek=%d, blockSize=%d, variantNum=%d (%d + %d)",
                minPos, maxPos, blockSeek, blockSize, numOfVariants(), subBlockVariantNum[0], subBlockVariantNum[1]);
    }

    /**
     * 转换为易于传输、储存的格式
     *
     * @param cache 输出容器
     */
    public int toTransFormat(VolumeByteStream cache) {
        cache.write(ChromosomeTags.getIndex(chromosome));
        cache.writeIntegerValue(this.minPos);
        cache.writeIntegerValue(this.maxPos);
        cache.writeShortValue(this.subBlockVariantNum[0]);
        cache.writeShortValue(this.subBlockVariantNum[1]);
        cache.writeIntegerValue(this.compressedGenotypesSize);
        cache.write(ValueUtils.value2ByteArray(this.compressedPosSize, 3));
        cache.writeIntegerValue(this.compressedAlleleSize);
        cache.write(this.magicCode);
        return 25;
    }

    /**
     * 节点比较规则：按照染色体编号、最小位点、最大位点、块大小进行排序
     */
    @Override
    public int compareTo(GTBNode otherNode) {
        int order = Integer.compare(ChromosomeTags.getIndex(this.chromosome), ChromosomeTags.getIndex(otherNode.chromosome));

        if (order == 0) {
            order = Integer.compare(this.minPos, otherNode.minPos);

            if (order == 0) {
                order = Integer.compare(this.maxPos, otherNode.maxPos);

                if (order == 0) {
                    order = Integer.compare(this.rootIndex, otherNode.rootIndex);
                }
            }
        }

        return order;
    }

    /**
     * 节点比较方法
     */
    public static int compare(GTBNode o1, GTBNode o2) {
        if (o1 == null) {
            return 1;
        } else if (o2 == null) {
            return -1;
        }

        return o1.compareTo(o2);
    }

    /**
     * 获取该块绑定数据的原始 MBEG 阵列大小（预估值）
     */
    public int getOriginMBEGsSize() {
        return rebuildMagicCode((this.magicCode >> 4) & 0xf);
    }

    /**
     * 获取该块绑定数据的原始 Allele 阵列大小（预估值）
     */
    public int getOriginAllelesSize() {
        return rebuildMagicCode(this.magicCode & 0xf);
    }

    /**
     * 获取该块绑定数据的原始 MBEG 阵列大小（预估值）魔术编码
     */
    public int getOriginMBEGsSizeFlag() {
        return (this.magicCode >> 4) & 0xf;
    }

    /**
     * 获取该块绑定数据的原始 Allele 阵列大小（预估值）魔术编码
     */
    public int getOriginAllelesSizeFlag() {
        return (this.magicCode) & 0xf;
    }

    /**
     * 获取该块的预估大小
     */
    public int getEstimateDecompressedSize(int validSubjectNum) {
        long originSize = (chromosome.length() + ValueUtils.byteArrayOfValueLength(maxPos) + ValueUtils.byteArrayOfValueLength(validSubjectNum * 2) * 2L + 31) * numOfVariants() +
                getOriginAllelesSize() + ((long) validSubjectNum * (subBlockVariantNum[0] * 2 + subBlockVariantNum[1] * 3) * 2);

        return originSize > Integer.MAX_VALUE - 2 ? Integer.MAX_VALUE - 2 : (int) originSize;
    }

    /**
     * Magic code 的前 4 个 bit 表示 originMBEGsSize，后 4 个 bit 表示 originAllelesSize
     *
     * @param originMBEGsSize   原始 MBEGsSize
     * @param originAllelesSize 原始 allelesSize
     */
    public static byte calculateMagicCode(int originMBEGsSize, int originAllelesSize) {
        return (byte) ((calculateMagicCode(originMBEGsSize) << 4) + (calculateMagicCode(originAllelesSize)));
    }

    /**
     * 计算魔术编码
     *
     * @param size 预估数值
     */
    public static byte calculateMagicCode(int size) {
        if (size <= 201326592) {
            // size <= 192 MB
            if (size <= 16777216) {
                // size <= 16 MB
                if (size <= 2097152) {
                    // <= 2 MB
                    return 0;
                } else if (size <= 4194304) {
                    // 2 MB < size <= 4 MB
                    return 1;
                } else if (size <= 8388608) {
                    // 4 MB < size <= 8 MB
                    return 2;
                } else {
                    // 8 MB < size <= 16 MB
                    return 3;
                }
            } else {
                if (size <= 33554432) {
                    // 16 MB < size <= 32 MB
                    return 4;
                } else if (size <= 67108864) {
                    // 32 MB < size <= 64 MB
                    return 5;
                } else if (size <= 134217728) {
                    // 64 MB < size <= 128 MB
                    return 6;
                } else {
                    // 128 MB < size <= 192 MB
                    return 7;
                }
            }
        } else {
            // size <= 768 MB
            if (size <= 805306368) {
                if (size <= 268435456) {
                    // 192 MB < size <= 256 MB
                    return 8;
                } else if (size <= 402653184) {
                    // 256 MB < size <= 384 MB
                    return 9;
                } else if (size <= 536870912) {
                    // 384 MB < size <= 512 MB
                    return 10;
                } else {
                    // 512 MB < size <= 768 MB
                    return 11;
                }
            } else {
                if (size <= 939524096) {
                    // 768 MB < size <= 896 MB
                    return 12;
                } else if (size <= 1073741824) {
                    // 896 MB < size <= 1 GB
                    return 13;
                } else if (size <= 1610612736) {
                    // 1 GB < size <= 1.5 GB
                    return 14;
                } else {
                    // 1.5 GB < size <= 2GB - 2B
                    return 15;
                }
            }
        }
    }

    /**
     * 从魔术编码读取对应的数据
     *
     * @param code 魔术编码
     */
    public static int rebuildMagicCode(int code) {
        switch (code) {
            case 0:
                // 2 MB
                return 2097152;
            case 1:
                // 4 MB
                return 4194304;
            case 2:
                // 8 MB
                return 8388608;
            case 3:
                // 16 MB
                return 16777216;
            case 4:
                // 32 MB
                return 33554432;
            case 5:
                // 64 MB
                return 67108864;
            case 6:
                // 128 MB
                return 134217728;
            case 7:
                // 192 MB
                return 201326592;
            case 8:
                // 256 MB
                return 268435456;
            case 9:
                // 384 MB
                return 402653184;
            case 10:
                // 512 MB
                return 536870912;
            case 11:
                // 768 MB
                return 805306368;
            case 12:
                // 896 MB
                return 939524096;
            case 13:
                // 1 GB
                return 1073741824;
            case 14:
                // 1.5 GB
                return 1610612736;
            case 15:
                // 2GB - 2B
                return 2147483645;
            default:
                // 无大小数据
                return 0;
        }
    }
}
