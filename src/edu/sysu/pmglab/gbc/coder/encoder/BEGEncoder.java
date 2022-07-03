package edu.sysu.pmglab.gbc.coder.encoder;

import edu.sysu.pmglab.easytools.ByteCode;
import edu.sysu.pmglab.gbc.coder.CoderConfig;
import edu.sysu.pmglab.gbc.coder.MBEGCoderException;

/**
 * @Data :2020/10/08
 * @Author :suranyi
 * @Contact :suranyi.sysu@gamil.com
 * @Description :基因型数据编码器接口
 */

public interface BEGEncoder {
    byte[] SCORE_DICT = initScoreDict();

    /**
     * 获取编码器
     *
     * @param phased 基因型数据是否有向
     * @return 指定向型编码器
     */
    static BEGEncoder getEncoder(boolean phased) {
        if (phased) {
            return PhasedEncoder.getInstance();
        } else {
            return UnPhasedEncoder.getInstance();
        }
    }

    /**
     * 获取所有的编码器
     *
     * @return 编码器列表
     */
    static BEGEncoder[] getEncoders() {
        return new BEGEncoder[]{UnPhasedEncoder.getInstance(), PhasedEncoder.getInstance()};
    }

    /**
     * 编码基因型数据
     *
     * @param i 左侧基因型
     * @param j 右侧基因型
     * @return 基因型 i|j 的编码值
     */
    byte encode(int i, int j);

    /**
     * 编码缺失基因型
     *
     * @return 缺失基因型编码值
     */
    default byte encodeMiss() {
        return CoderConfig.MISS_GENOTYPE_CODE;
    }

    /**
     * 为字节序列编码
     *
     * @param miss    替换为缺失基因型
     * @param variant 变异位点字节序列
     * @param seek    基因型指针
     * @param length  基因型长度
     * @return 基因型数据编码值
     */
    default byte encode(boolean miss, byte[] variant, int seek, int length) {
        if (miss) {
            return CoderConfig.MISS_GENOTYPE_CODE;
        } else {
            return encode(variant, seek, length);
        }
    }

    /**
     * 为字节序列编码
     *
     * @param variant 变异位点字节序列
     * @param seek    基因型指针
     * @param length  基因型长度
     * @return 基因型数据编码值
     */
    default byte encode(byte[] variant, int seek, int length) {
        if (variant[seek - length] == ByteCode.PERIOD) {
            return CoderConfig.MISS_GENOTYPE_CODE;
        }

        // 大多数基因型都是 length == 3 情景，此时使用 if-else 结构较快
        if (length == 3) {
            // a|b 或 a/b 型
            return encode(variant[seek - length] - 48, variant[seek - length + 2] - 48);
        } else {
            int value1, value2;
            if (length == 1) {
                // 单基因型，单倍体染色体中出现，此时扩充为纯合子
                value1 = variant[seek - length] - 48;
                return encode(value1, value1);
            } else if (length == 2) {
                // 单基因型，基因型数值为 2 位数，单倍体染色体中，此时扩充为纯合子
                value1 = (variant[seek - length] - 48) * 10 + (variant[seek - length + 1] - 48);
                return encode(value1, value1);
            } else if (length == 4) {
                // 等于 4，在 1 字节下只可能是 a|bc、ab|c类型
                if ((variant[seek - length + 1] == ByteCode.VERTICAL_BAR) || (variant[seek - length + 1] == ByteCode.SLASH)) {
                    // a|bc 型
                    value1 = variant[seek - length] - 48;
                    value2 = (variant[seek - length + 2] - 48) * 10 + (variant[seek - length + 3] - 48);
                } else {
                    // ab|c 型
                    value1 = (variant[seek - length] - 48) * 10 + (variant[seek - length + 1] - 48);
                    value2 = variant[seek - length + 3] - 48;
                }
                return encode(value1, value2);
            } else if (length == 5) {
                // 等于 5，在 1 字节下只可能是 ab|cd
                value1 = (variant[seek - length] - 48) * 10 + (variant[seek - length + 1] - 48);
                value2 = (variant[seek - length + 3] - 48) * 10 + (variant[seek - length + 4] - 48);
                return encode(value1, value2);
            } else {
                throw new MBEGCoderException("BEG encode error: the number of current variant’s alternative alleles is too large, " + getClass().getSimpleName() + " only supports a maximum of " + CoderConfig.MAX_ALLELE_NUM + ".");
            }
        }
    }

    /**
     * 基因型编码器是否有向
     *
     * @return 编码器向型
     */
    boolean isPhased();

    /**
     * 获取编码表
     *
     * @param phased 是否有向
     * @return 编码表字节数组
     */
    default byte[][] initEncodeDict(boolean phased) {
        // 获取该编码位长最大可编码等位基因数
        byte[][] encodeDict = new byte[CoderConfig.MAX_ALLELE_NUM][CoderConfig.MAX_ALLELE_NUM];

        if (phased) {
            for (int i = 0; i < CoderConfig.MAX_ALLELE_NUM; i++) {
                for (int j = 0; j < CoderConfig.MAX_ALLELE_NUM; j++) {
                    encodeDict[i][j] = (byte) CoderConfig.mapGenotypeTo(i, j);
                }
            }
        } else {
            for (int i = 0; i < CoderConfig.MAX_ALLELE_NUM; i++) {
                for (int j = i; j < CoderConfig.MAX_ALLELE_NUM; j++) {
                    encodeDict[i][j] = (byte) CoderConfig.mapGenotypeTo(i, j);
                }
            }

            for (int i = 0; i < CoderConfig.MAX_ALLELE_NUM; i++) {
                for (int j = 0; j < i; j++) {
                    encodeDict[i][j] = encodeDict[j][i];
                }
            }
        }

        return encodeDict;
    }

    /**
     * 基因型编码值对应的特征 (即 allele = 0 的个数)
     *
     * @param genotypeCode 基因型编码值
     * @return 特征分数
     */
    default byte scoreOf(int genotypeCode) {
        return SCORE_DICT[genotypeCode];
    }

    /**
     * 基因型编码值对应的特征 (即 allele = 0 的个数)
     *
     * @param genotypeCode 基因型编码值
     * @return 特征分数
     */
    default byte scoreOf(byte genotypeCode) {
        try {
            return scoreOf(genotypeCode & 0xFF);
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new MBEGCoderException("BEG encode error: the genotype coding value is out of range [0, 225]");
        }
    }

    /**
     * 获取基因型分数表
     *
     * @return 含有 0 等位基因基因的特征分数表，score[genotype] = I(a)
     */
    static byte[] initScoreDict() {
        byte[] scoreDict = new byte[CoderConfig.MAX_ALLELE_NUM * CoderConfig.MAX_ALLELE_NUM + 1];

        for (int i = 0; i < CoderConfig.MAX_ALLELE_NUM; i++) {
            // genotype: 0|i
            scoreDict[CoderConfig.mapGenotypeTo(i, 0)] += 1;

            // genotype: i|0
            scoreDict[CoderConfig.mapGenotypeTo(0, i)] += 1;
        }

        return scoreDict;
    }
}

