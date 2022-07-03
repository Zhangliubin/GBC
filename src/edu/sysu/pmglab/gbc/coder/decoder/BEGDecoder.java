package edu.sysu.pmglab.gbc.coder.decoder;

import edu.sysu.pmglab.easytools.ByteCode;
import edu.sysu.pmglab.gbc.coder.CoderConfig;
import edu.sysu.pmglab.gbc.coder.MBEGCoderException;

/**
 * @Data :2020/10/08
 * @Author :suranyi
 * @Contact :suranyi.sysu@gamil.com
 * @Description :基因型数据解码器接口
 */

public interface BEGDecoder {
    byte[][] ALTERNATIVE_ALLELE_COUNT_DICT = initScoreDict();
    int[][] ALLELE_DICT = initAlleleDict();

    /**
     * 获取解码器
     *
     * @param phased 基因型数据是否有向
     * @return 指定向型解码器
     */
    static BEGDecoder getDecoder(boolean phased) {
        if (phased) {
            return PhasedDecoder.getInstance();
        } else {
            return UnPhasedDecoder.getInstance();
        }
    }

    /**
     * 获取所有的解码器
     *
     * @return 解码器列表
     */
    static BEGDecoder[] getDecoders() {
        return new BEGDecoder[]{UnPhasedDecoder.getInstance(), PhasedDecoder.getInstance()};
    }

    /**
     * 基因型数据解码
     *
     * @param ploidy 倍型
     * @param code   基因型编码值
     * @return 该基因型编码值对应的字符串型基因型
     */
    byte[] decode(int ploidy, int code);

    /**
     * 基因型数据解码
     *
     * @param ploidy 倍型
     * @param code   基因型编码值
     * @return 该基因型编码值对应的字符串型基因型
     */
    default byte[] decode(int ploidy, byte code) {
        return decode(ploidy, code & 0xFF);
    }

    /**
     * 基因型数据解码
     *
     * @param haplotypeIndex 单倍型索引
     * @param code           基因型编码值
     * @return 该基因型编码值对应倍型位置的基因型
     */
    static int decodeHaplotype(int haplotypeIndex, int code) {
        try {
            return ALLELE_DICT[haplotypeIndex][code];
        } catch (ArrayIndexOutOfBoundsException e) {
            if (haplotypeIndex == 0 || haplotypeIndex == 1) {
                throw new MBEGCoderException("BEG decode error: BEG code(" + code + ") out of range [0, 225]");
            } else {
                throw new MBEGCoderException("BEG decode error: the haplotype index of genotype (a|b or a/b) is in the range of [0, 1]");
            }
        }
    }

    /**
     * 基因型数据解码
     *
     * @param haplotypeIndex 单倍型索引
     * @param code           基因型编码值
     * @return 该基因型编码值对应倍型位置的基因型
     */
    static int decodeHaplotype(int haplotypeIndex, byte code) {
        return decodeHaplotype(haplotypeIndex, code & 0xFF);
    }


    /**
     * 基因型解码器是否有向
     *
     * @return 解码器向型
     */
    boolean isPhased();

    /**
     * 获取可替代等位基因个数
     *
     * @param ploidy 倍型
     * @param code   基因型编码值
     * @return 可替代等位基因个数
     */
    static byte alternativeAlleleNumOf(int ploidy, byte code) {
        return alternativeAlleleNumOf(ploidy, code & 0xFF);
    }

    /**
     * 获取可替代等位基因个数
     *
     * @param ploidy 倍型
     * @param code   基因型编码值
     * @return 可替代等位基因个数
     */
    static byte alternativeAlleleNumOf(int ploidy, int code) {
        try {
            return ALTERNATIVE_ALLELE_COUNT_DICT[ploidy][code];
        } catch (ArrayIndexOutOfBoundsException e) {
            if (ploidy == 1 || ploidy == 2) {
                throw new MBEGCoderException("BEG decode error: BEG code(" + code + ") out of range [0, 225]");
            } else {
                throw new MBEGCoderException("BEG decode error: MBEG only supported decode genotype from haploid(ploidy=1) or diploid(ploidy=2) species");
            }
        }
    }

    /**
     * 获取解码表, 1 表示单倍型, 2 表示二倍型
     *
     * @param phased 向型
     * @return 解码表字节数组
     */
    default byte[][][] initDecodeDict(boolean phased) {
        byte[][][] decodeDict = new byte[3][CoderConfig.MAX_ALLELE_NUM * CoderConfig.MAX_ALLELE_NUM + 1][];
        decodeDict[0] = null;

        // 单倍体基因型, 此时数据都是纯合数据
        for (int i = 0; i < CoderConfig.MAX_ALLELE_NUM; i++) {
            if (i <= 9) {
                decodeDict[1][CoderConfig.mapGenotypeTo(i, i)] = new byte[]{(byte) (i + 48)};
            } else {
                decodeDict[1][CoderConfig.mapGenotypeTo(i, i)] = new byte[]{(byte) (i / 10 + 48), (byte) (i % 10 + 48)};
            }
        }

        // 补充缺失基因型解码
        decodeDict[1][CoderConfig.MISS_GENOTYPE_CODE] = new byte[]{ByteCode.PERIOD};

        // 二倍体基因型
        byte sep = phased ? ByteCode.VERTICAL_BAR : ByteCode.SLASH;
        for (int i = 0; i < CoderConfig.MAX_ALLELE_NUM; i++) {
            for (int j = 0; j < CoderConfig.MAX_ALLELE_NUM; j++) {
                if (i <= 9) {
                    if (j <= 9) {
                        decodeDict[2][CoderConfig.mapGenotypeTo(i, j)] = new byte[]{(byte) (i + 48), sep, (byte) (j + 48)};
                    } else {
                        decodeDict[2][CoderConfig.mapGenotypeTo(i, j)] = new byte[]{(byte) (i + 48), sep, (byte) (j / 10 + 48), (byte) (j % 10 + 48)};
                    }
                } else {
                    if (j <= 9) {
                        decodeDict[2][CoderConfig.mapGenotypeTo(i, j)] = new byte[]{(byte) (i / 10 + 48), (byte) (i % 10 + 48), sep, (byte) (j + 48)};
                    } else {
                        decodeDict[2][CoderConfig.mapGenotypeTo(i, j)] = new byte[]{(byte) (i / 10 + 48), (byte) (i % 10 + 48), sep, (byte) (j / 10 + 48), (byte) (j % 10 + 48)};
                    }
                }
            }
        }

        // 补充缺失基因型解码
        decodeDict[2][CoderConfig.MISS_GENOTYPE_CODE] = new byte[]{ByteCode.PERIOD, sep, ByteCode.PERIOD};

        // 如果是无向数据，设置为对称值
        if (!phased) {
            for (int i = 0; i < CoderConfig.MAX_ALLELE_NUM; i++) {
                for (int j = 0; j < i; j++) {
                    decodeDict[2][CoderConfig.mapGenotypeTo(i, j)] = decodeDict[2][CoderConfig.mapGenotypeTo(j, i)];
                }
            }
        }

        return decodeDict;
    }

    /**
     * 获取可替代等位基因数
     *
     * @return 可替代等位基因数数组
     */
    static byte[][] initScoreDict() {
        int genotypeCodeMax = CoderConfig.MAX_ALLELE_NUM * CoderConfig.MAX_ALLELE_NUM + 1;
        byte[][] alternativeAlleleCountDict = new byte[3][genotypeCodeMax];

        // 单倍型
        for (int i = 1; i < CoderConfig.MAX_ALLELE_NUM; i++) {
            alternativeAlleleCountDict[1][CoderConfig.mapGenotypeTo(i, i)] = 1;
        }

        // 二倍型
        for (int i = 0; i < CoderConfig.MAX_ALLELE_NUM; i++) {
            for (int j = 0; j < CoderConfig.MAX_ALLELE_NUM; j++) {
                if (i != 0) {
                    alternativeAlleleCountDict[2][CoderConfig.mapGenotypeTo(i, j)] += 1;
                }

                if (j != 0) {
                    alternativeAlleleCountDict[2][CoderConfig.mapGenotypeTo(i, j)] += 1;
                }
            }
        }

        alternativeAlleleCountDict[0] = null;
        return alternativeAlleleCountDict;
    }

    /**
     * 获取解码表
     *
     * @return 解码表字节数组
     */
    static int[][] initAlleleDict() {
        int genotypeCodeMax = CoderConfig.MAX_ALLELE_NUM * CoderConfig.MAX_ALLELE_NUM + 1;
        int[][] alleleDict = new int[2][genotypeCodeMax];

        // 二倍型
        for (int i = 0; i < CoderConfig.MAX_ALLELE_NUM; i++) {
            for (int j = 0; j < CoderConfig.MAX_ALLELE_NUM; j++) {
                alleleDict[0][CoderConfig.mapGenotypeTo(i, j)] = i;
                alleleDict[1][CoderConfig.mapGenotypeTo(i, j)] = j;
            }
        }

        alleleDict[0][0] = -1;
        alleleDict[1][0] = -1;
        return alleleDict;
    }

    static boolean isMiss(int code) {
        return code == CoderConfig.MISS_GENOTYPE_CODE;
    }
}

