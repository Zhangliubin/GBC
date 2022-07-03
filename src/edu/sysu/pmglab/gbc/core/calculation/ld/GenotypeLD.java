package edu.sysu.pmglab.gbc.core.calculation.ld;

import edu.sysu.pmglab.gbc.coder.decoder.BEGDecoder;
import edu.sysu.pmglab.container.VolumeByteStream;
import edu.sysu.pmglab.easytools.ByteCode;
import edu.sysu.pmglab.easytools.ValueUtils;
import edu.sysu.pmglab.gbc.core.gtbcomponent.gtbreader.Variant;

/**
 * @author suranyi
 */

enum GenotypeLD implements ILDModel, ILDContext {
    /**
     * Pearson LD 模型
     * 该模型不考虑基因型向型，仅考虑基因型携带等位基因的个数
     */
    INSTANCE;

    static String EXTENSION = ".geno.ld";

    @Override
    public int calculateLDR2(VolumeByteStream lineCache, Variant variant1, Variant variant2, double minR2) {
        VariantProperty propertyA = (VariantProperty) variant1.property;
        VariantProperty propertyB = (VariantProperty) variant2.property;

        int validSampleNum;
        int sumA;
        int sumB;
        int sumAB;
        int sumA_square;
        int sumB_square;

        if (propertyA.hasMiss || propertyB.hasMiss) {
            // 至少有一个位点包含 miss 基因型时，需要重新计算
            validSampleNum = 0;
            int countA_N_ALLELE_NUM_EQ_1 = 0;
            int countA_N_ALLELE_NUM_EQ_2 = 0;
            int countB_N_ALLELE_NUM_EQ_1 = 0;
            int countB_N_ALLELE_NUM_EQ_2 = 0;
            int countAB_ALLELE_NUM_EQ_1_1 = 0;
            int countAB_ALLELE_NUM_EQ_1_2 = 0;
            int countAB_ALLELE_NUM_EQ_2_2 = 0;
            int countA_N_ALLELE_NUM_EQ_1_status;
            int countA_N_ALLELE_NUM_EQ_2_status;
            int countB_N_ALLELE_NUM_EQ_1_status;
            int countB_N_ALLELE_NUM_EQ_2_status;

            for (int i = 0; i < propertyA.groupNum; i++) {
                // 有效等位基因状态及个数
                int validSampleStatus = (propertyA.validSampleFlags[i] & propertyB.validSampleFlags[i]);
                int currentValidSampleNum = Integer.bitCount(validSampleStatus);

                if (currentValidSampleNum > 0) {
                    // 不全是缺失，获取相交等位基因
                    validSampleNum += currentValidSampleNum;

                    // 获得等位基因个数为 1 的位信息
                    countA_N_ALLELE_NUM_EQ_1_status = propertyA.N_ALLELE_NUM_EQ_1[i] & validSampleStatus;
                    countB_N_ALLELE_NUM_EQ_1_status = propertyB.N_ALLELE_NUM_EQ_1[i] & validSampleStatus;

                    // 获得等位基因个数为 2 的位信息
                    countA_N_ALLELE_NUM_EQ_2_status = propertyA.N_ALLELE_NUM_EQ_2[i] & validSampleStatus;
                    countB_N_ALLELE_NUM_EQ_2_status = propertyB.N_ALLELE_NUM_EQ_2[i] & validSampleStatus;

                    countA_N_ALLELE_NUM_EQ_1 += Integer.bitCount(countA_N_ALLELE_NUM_EQ_1_status);
                    countB_N_ALLELE_NUM_EQ_1 += Integer.bitCount(countB_N_ALLELE_NUM_EQ_1_status);
                    countA_N_ALLELE_NUM_EQ_2 += Integer.bitCount(countA_N_ALLELE_NUM_EQ_2_status);
                    countB_N_ALLELE_NUM_EQ_2 += Integer.bitCount(countB_N_ALLELE_NUM_EQ_2_status);

                    countAB_ALLELE_NUM_EQ_1_1 += Integer.bitCount(countA_N_ALLELE_NUM_EQ_1_status & countB_N_ALLELE_NUM_EQ_1_status);
                    countAB_ALLELE_NUM_EQ_1_2 += Integer.bitCount(countA_N_ALLELE_NUM_EQ_1_status & countB_N_ALLELE_NUM_EQ_2_status) + Integer.bitCount(countB_N_ALLELE_NUM_EQ_1_status & countA_N_ALLELE_NUM_EQ_2_status);
                    countAB_ALLELE_NUM_EQ_2_2 += Integer.bitCount(countA_N_ALLELE_NUM_EQ_2_status & countB_N_ALLELE_NUM_EQ_2_status);
                }
            }

            sumAB = countAB_ALLELE_NUM_EQ_1_1 + (countAB_ALLELE_NUM_EQ_1_2 << 1) + (countAB_ALLELE_NUM_EQ_2_2 << 2);
            sumA = countA_N_ALLELE_NUM_EQ_1 + (countA_N_ALLELE_NUM_EQ_2 << 1);
            sumB = countB_N_ALLELE_NUM_EQ_1 + (countB_N_ALLELE_NUM_EQ_2 << 1);
            sumA_square = countA_N_ALLELE_NUM_EQ_1 + (countA_N_ALLELE_NUM_EQ_2 << 2);
            sumB_square = countB_N_ALLELE_NUM_EQ_1 + (countB_N_ALLELE_NUM_EQ_2 << 2);

            if (validSampleNum == 0) {
                // 全是缺失的, 此时认为两位点的 LD 系数为 Nan (因为无法判断)
                return 0;
            }
        } else {
            // 没有任何缺失，此时借助缓冲数据提升速度
            validSampleNum = variant1.BEGs.length;

            int countAB_ALLELE_NUM_EQ_1_1 = 0;
            int countAB_ALLELE_NUM_EQ_1_2 = 0;
            int countAB_ALLELE_NUM_EQ_2_2 = 0;

            for (int i = 0; i < propertyA.groupNum; i++) {
                countAB_ALLELE_NUM_EQ_1_1 += Integer.bitCount(propertyA.N_ALLELE_NUM_EQ_1[i] & propertyB.N_ALLELE_NUM_EQ_1[i]);
                countAB_ALLELE_NUM_EQ_1_2 += Integer.bitCount(propertyA.N_ALLELE_NUM_EQ_1[i] & propertyB.N_ALLELE_NUM_EQ_2[i]) + Integer.bitCount(propertyA.N_ALLELE_NUM_EQ_2[i] & propertyB.N_ALLELE_NUM_EQ_1[i]);
                countAB_ALLELE_NUM_EQ_2_2 += Integer.bitCount(propertyA.N_ALLELE_NUM_EQ_2[i] & propertyB.N_ALLELE_NUM_EQ_2[i]);
            }

            sumA = propertyA.N_ALT;
            sumB = propertyB.N_ALT;
            sumAB = countAB_ALLELE_NUM_EQ_1_1 + (countAB_ALLELE_NUM_EQ_1_2 << 1) + (countAB_ALLELE_NUM_EQ_2_2 << 2);
            sumA_square = propertyA.N_ALLELE_I_SQUARE;
            sumB_square = propertyB.N_ALLELE_I_SQUARE;
        }

        // 分子和分母
        double r_numerator = sumAB - (double) sumA * sumB / validSampleNum;
        double r_denominator_square = (sumA_square - (double) sumA * sumA / validSampleNum) * (sumB_square - (double) sumB * sumB / validSampleNum);

        if (r_denominator_square == 0) {
            return 0;
        }

        double r2 = r_numerator * r_numerator / r_denominator_square;

        if (r2 < minR2) {
            return 0;
        }

        lineCache.reset();
        formatterOut(lineCache, variant1.chromosome, variant1.position, variant2.position, validSampleNum, (float) r2);
        return lineCache.size();
    }

    @Override
    public double calculateLDR2(Variant variant1, Variant variant2) {
        if (variant1.BEGs.length != variant2.BEGs.length) {
            throw new UnsupportedOperationException("LD coefficients are calculated between sites with different sample sizes");
        }

        if (variant1.getAlternativeAlleleNum() > 2 || variant2.getAlternativeAlleleNum() > 2) {
            throw new UnsupportedOperationException("LD coefficients are calculated between sites with number of alternative allele > 2");
        }

        if (!(variant1.property instanceof VariantProperty)) {
            variant1.property = getProperty(variant1.BEGs.length).fillBitCodes(variant1);
        }

        if (!(variant2.property instanceof VariantProperty)) {
            variant2.property = getProperty(variant2.BEGs.length).fillBitCodes(variant2);
        }

        VariantProperty propertyA = (VariantProperty) variant1.property;
        VariantProperty propertyB = (VariantProperty) variant2.property;

        int validSampleNum;
        int sumA;
        int sumB;
        int sumAB;
        int sumA_square;
        int sumB_square;

        if (propertyA.hasMiss || propertyB.hasMiss) {
            // 至少有一个位点包含 miss 基因型时，需要重新计算
            validSampleNum = 0;
            int countA_N_ALLELE_NUM_EQ_1 = 0;
            int countA_N_ALLELE_NUM_EQ_2 = 0;
            int countB_N_ALLELE_NUM_EQ_1 = 0;
            int countB_N_ALLELE_NUM_EQ_2 = 0;
            int countAB_ALLELE_NUM_EQ_1_1 = 0;
            int countAB_ALLELE_NUM_EQ_1_2 = 0;
            int countAB_ALLELE_NUM_EQ_2_2 = 0;
            int countA_N_ALLELE_NUM_EQ_1_status;
            int countA_N_ALLELE_NUM_EQ_2_status;
            int countB_N_ALLELE_NUM_EQ_1_status;
            int countB_N_ALLELE_NUM_EQ_2_status;

            for (int i = 0; i < propertyA.groupNum; i++) {
                // 有效等位基因状态及个数
                int validSampleStatus = (propertyA.validSampleFlags[i] & propertyB.validSampleFlags[i]);
                int currentValidSampleNum = Integer.bitCount(validSampleStatus);

                if (currentValidSampleNum > 0) {
                    // 不全是缺失，获取相交等位基因
                    validSampleNum += currentValidSampleNum;

                    // 获得等位基因个数为 1 的位信息
                    countA_N_ALLELE_NUM_EQ_1_status = propertyA.N_ALLELE_NUM_EQ_1[i] & validSampleStatus;
                    countB_N_ALLELE_NUM_EQ_1_status = propertyB.N_ALLELE_NUM_EQ_1[i] & validSampleStatus;

                    // 获得等位基因个数为 2 的位信息
                    countA_N_ALLELE_NUM_EQ_2_status = propertyA.N_ALLELE_NUM_EQ_2[i] & validSampleStatus;
                    countB_N_ALLELE_NUM_EQ_2_status = propertyB.N_ALLELE_NUM_EQ_2[i] & validSampleStatus;

                    countA_N_ALLELE_NUM_EQ_1 += Integer.bitCount(countA_N_ALLELE_NUM_EQ_1_status);
                    countB_N_ALLELE_NUM_EQ_1 += Integer.bitCount(countB_N_ALLELE_NUM_EQ_1_status);
                    countA_N_ALLELE_NUM_EQ_2 += Integer.bitCount(countA_N_ALLELE_NUM_EQ_2_status);
                    countB_N_ALLELE_NUM_EQ_2 += Integer.bitCount(countB_N_ALLELE_NUM_EQ_2_status);

                    countAB_ALLELE_NUM_EQ_1_1 += Integer.bitCount(countA_N_ALLELE_NUM_EQ_1_status & countB_N_ALLELE_NUM_EQ_1_status);
                    countAB_ALLELE_NUM_EQ_1_2 += Integer.bitCount(countA_N_ALLELE_NUM_EQ_1_status & countB_N_ALLELE_NUM_EQ_2_status) + Integer.bitCount(countB_N_ALLELE_NUM_EQ_1_status & countA_N_ALLELE_NUM_EQ_2_status);
                    countAB_ALLELE_NUM_EQ_2_2 += Integer.bitCount(countA_N_ALLELE_NUM_EQ_2_status & countB_N_ALLELE_NUM_EQ_2_status);
                }
            }

            sumAB = countAB_ALLELE_NUM_EQ_1_1 + (countAB_ALLELE_NUM_EQ_1_2 << 1) + (countAB_ALLELE_NUM_EQ_2_2 << 2);
            sumA = countA_N_ALLELE_NUM_EQ_1 + (countA_N_ALLELE_NUM_EQ_2 << 1);
            sumB = countB_N_ALLELE_NUM_EQ_1 + (countB_N_ALLELE_NUM_EQ_2 << 1);
            sumA_square = countA_N_ALLELE_NUM_EQ_1 + (countA_N_ALLELE_NUM_EQ_2 << 2);
            sumB_square = countB_N_ALLELE_NUM_EQ_1 + (countB_N_ALLELE_NUM_EQ_2 << 2);

            if (validSampleNum == 0) {
                // 全是缺失的, 此时认为两位点的 LD 系数为 Nan (因为无法判断)
                return Double.NaN;
            }
        } else {
            // 没有任何缺失，此时借助缓冲数据提升速度
            validSampleNum = variant1.BEGs.length;

            int countAB_ALLELE_NUM_EQ_1_1 = 0;
            int countAB_ALLELE_NUM_EQ_1_2 = 0;
            int countAB_ALLELE_NUM_EQ_2_2 = 0;

            for (int i = 0; i < propertyA.groupNum; i++) {
                countAB_ALLELE_NUM_EQ_1_1 += Integer.bitCount(propertyA.N_ALLELE_NUM_EQ_1[i] & propertyB.N_ALLELE_NUM_EQ_1[i]);
                countAB_ALLELE_NUM_EQ_1_2 += Integer.bitCount(propertyA.N_ALLELE_NUM_EQ_1[i] & propertyB.N_ALLELE_NUM_EQ_2[i]) + Integer.bitCount(propertyA.N_ALLELE_NUM_EQ_2[i] & propertyB.N_ALLELE_NUM_EQ_1[i]);
                countAB_ALLELE_NUM_EQ_2_2 += Integer.bitCount(propertyA.N_ALLELE_NUM_EQ_2[i] & propertyB.N_ALLELE_NUM_EQ_2[i]);
            }

            sumA = propertyA.N_ALT;
            sumB = propertyB.N_ALT;
            sumAB = countAB_ALLELE_NUM_EQ_1_1 + (countAB_ALLELE_NUM_EQ_1_2 << 1) + (countAB_ALLELE_NUM_EQ_2_2 << 2);
            sumA_square = propertyA.N_ALLELE_I_SQUARE;
            sumB_square = propertyB.N_ALLELE_I_SQUARE;
        }

        // 分子和分母
        double r_numerator = sumAB - (double) sumA * sumB / validSampleNum;
        double r_denominator_square = (sumA_square - (double) sumA * sumA / validSampleNum) * (sumB_square - (double) sumB * sumB / validSampleNum);

        if (r_denominator_square == 0) {
            return Double.NaN;
        }

        return r_numerator * r_numerator / r_denominator_square;
    }

    @Override
    public double calculateLD(Variant variant1, Variant variant2) {
        if (variant1.BEGs.length != variant2.BEGs.length) {
            throw new UnsupportedOperationException("LD coefficients are calculated between sites with different sample sizes");
        }

        if (variant1.getAlternativeAlleleNum() > 2 || variant2.getAlternativeAlleleNum() > 2) {
            throw new UnsupportedOperationException("LD coefficients are calculated between sites with number of alternative allele > 2");
        }

        if (!(variant1.property instanceof VariantProperty)) {
            variant1.property = getProperty(variant1.BEGs.length).fillBitCodes(variant1);
        }

        if (!(variant2.property instanceof VariantProperty)) {
            variant2.property = getProperty(variant2.BEGs.length).fillBitCodes(variant2);
        }

        VariantProperty propertyA = (VariantProperty) variant1.property;
        VariantProperty propertyB = (VariantProperty) variant2.property;

        int validSampleNum;
        int sumA;
        int sumB;
        int sumAB;
        int sumA_square;
        int sumB_square;

        if (propertyA.hasMiss || propertyB.hasMiss) {
            // 至少有一个位点包含 miss 基因型时，需要重新计算
            validSampleNum = 0;
            int countA_N_ALLELE_NUM_EQ_1 = 0;
            int countA_N_ALLELE_NUM_EQ_2 = 0;
            int countB_N_ALLELE_NUM_EQ_1 = 0;
            int countB_N_ALLELE_NUM_EQ_2 = 0;
            int countAB_ALLELE_NUM_EQ_1_1 = 0;
            int countAB_ALLELE_NUM_EQ_1_2 = 0;
            int countAB_ALLELE_NUM_EQ_2_2 = 0;
            int countA_N_ALLELE_NUM_EQ_1_status;
            int countA_N_ALLELE_NUM_EQ_2_status;
            int countB_N_ALLELE_NUM_EQ_1_status;
            int countB_N_ALLELE_NUM_EQ_2_status;

            for (int i = 0; i < propertyA.groupNum; i++) {
                // 有效等位基因状态及个数
                int validSampleStatus = (propertyA.validSampleFlags[i] & propertyB.validSampleFlags[i]);
                int currentValidSampleNum = Integer.bitCount(validSampleStatus);

                if (currentValidSampleNum > 0) {
                    // 不全是缺失，获取相交等位基因
                    validSampleNum += currentValidSampleNum;

                    // 获得等位基因个数为 1 的位信息
                    countA_N_ALLELE_NUM_EQ_1_status = propertyA.N_ALLELE_NUM_EQ_1[i] & validSampleStatus;
                    countB_N_ALLELE_NUM_EQ_1_status = propertyB.N_ALLELE_NUM_EQ_1[i] & validSampleStatus;

                    // 获得等位基因个数为 2 的位信息
                    countA_N_ALLELE_NUM_EQ_2_status = propertyA.N_ALLELE_NUM_EQ_2[i] & validSampleStatus;
                    countB_N_ALLELE_NUM_EQ_2_status = propertyB.N_ALLELE_NUM_EQ_2[i] & validSampleStatus;

                    countA_N_ALLELE_NUM_EQ_1 += Integer.bitCount(countA_N_ALLELE_NUM_EQ_1_status);
                    countB_N_ALLELE_NUM_EQ_1 += Integer.bitCount(countB_N_ALLELE_NUM_EQ_1_status);
                    countA_N_ALLELE_NUM_EQ_2 += Integer.bitCount(countA_N_ALLELE_NUM_EQ_2_status);
                    countB_N_ALLELE_NUM_EQ_2 += Integer.bitCount(countB_N_ALLELE_NUM_EQ_2_status);

                    countAB_ALLELE_NUM_EQ_1_1 += Integer.bitCount(countA_N_ALLELE_NUM_EQ_1_status & countB_N_ALLELE_NUM_EQ_1_status);
                    countAB_ALLELE_NUM_EQ_1_2 += Integer.bitCount(countA_N_ALLELE_NUM_EQ_1_status & countB_N_ALLELE_NUM_EQ_2_status) + Integer.bitCount(countB_N_ALLELE_NUM_EQ_1_status & countA_N_ALLELE_NUM_EQ_2_status);
                    countAB_ALLELE_NUM_EQ_2_2 += Integer.bitCount(countA_N_ALLELE_NUM_EQ_2_status & countB_N_ALLELE_NUM_EQ_2_status);
                }
            }

            sumAB = countAB_ALLELE_NUM_EQ_1_1 + (countAB_ALLELE_NUM_EQ_1_2 << 1) + (countAB_ALLELE_NUM_EQ_2_2 << 2);
            sumA = countA_N_ALLELE_NUM_EQ_1 + (countA_N_ALLELE_NUM_EQ_2 << 1);
            sumB = countB_N_ALLELE_NUM_EQ_1 + (countB_N_ALLELE_NUM_EQ_2 << 1);
            sumA_square = countA_N_ALLELE_NUM_EQ_1 + (countA_N_ALLELE_NUM_EQ_2 << 2);
            sumB_square = countB_N_ALLELE_NUM_EQ_1 + (countB_N_ALLELE_NUM_EQ_2 << 2);

            if (validSampleNum == 0) {
                // 全是缺失的, 此时认为两位点的 LD 系数为 Nan (因为无法判断)
                return Double.NaN;
            }
        } else {
            // 没有任何缺失，此时借助缓冲数据提升速度
            validSampleNum = variant1.BEGs.length;

            int countAB_ALLELE_NUM_EQ_1_1 = 0;
            int countAB_ALLELE_NUM_EQ_1_2 = 0;
            int countAB_ALLELE_NUM_EQ_2_2 = 0;

            for (int i = 0; i < propertyA.groupNum; i++) {
                countAB_ALLELE_NUM_EQ_1_1 += Integer.bitCount(propertyA.N_ALLELE_NUM_EQ_1[i] & propertyB.N_ALLELE_NUM_EQ_1[i]);
                countAB_ALLELE_NUM_EQ_1_2 += Integer.bitCount(propertyA.N_ALLELE_NUM_EQ_1[i] & propertyB.N_ALLELE_NUM_EQ_2[i]) + Integer.bitCount(propertyA.N_ALLELE_NUM_EQ_2[i] & propertyB.N_ALLELE_NUM_EQ_1[i]);
                countAB_ALLELE_NUM_EQ_2_2 += Integer.bitCount(propertyA.N_ALLELE_NUM_EQ_2[i] & propertyB.N_ALLELE_NUM_EQ_2[i]);
            }

            sumA = propertyA.N_ALT;
            sumB = propertyB.N_ALT;
            sumAB = countAB_ALLELE_NUM_EQ_1_1 + (countAB_ALLELE_NUM_EQ_1_2 << 1) + (countAB_ALLELE_NUM_EQ_2_2 << 2);
            sumA_square = propertyA.N_ALLELE_I_SQUARE;
            sumB_square = propertyB.N_ALLELE_I_SQUARE;
        }

        // 分子和分母
        double r_numerator = sumAB - (double) sumA * sumB / validSampleNum;
        double r_denominator_square = (sumA_square - (double) sumA * sumA / validSampleNum) * (sumB_square - (double) sumB * sumB / validSampleNum);

        if (r_denominator_square == 0) {
            return Double.NaN;
        }

        return r_numerator / Math.sqrt(r_denominator_square);
    }

    @Override
    public String getHeader() {
        return "CHR\tPOS1\tPOS2\tN_INDV\tR^2";
    }

    @Override
    public void formatterOut(VolumeByteStream lineCache, String chromosome, int pos1, int pos2, int validSampleNum, float... score) {
        lineCache.write(chromosome);
        lineCache.write(ByteCode.TAB);
        lineCache.write(ValueUtils.stringValueOfAndGetBytes(pos1));
        lineCache.write(ByteCode.TAB);
        lineCache.write(ValueUtils.stringValueOfAndGetBytes(pos2));
        lineCache.write(ByteCode.TAB);
        lineCache.write(ValueUtils.stringValueOfAndGetBytes(validSampleNum));
        lineCache.write(ByteCode.TAB);
        lineCache.write(ValueUtils.stringValueOfAndGetBytes(score[0], 6));
    }

    @Override
    public String getExtension() {
        return EXTENSION;
    }

    @Override
    public String toString() {
        return "Genotype LD (Pearson genotypic correlation of variants)";
    }

    @Override
    public int getGroupNum(int BEGSize) {
        return BEGSize / 32 + (getResNum(BEGSize) == 0 ? 0 : 1);
    }

    @Override
    public int getResNum(int BEGSize) {
        return BEGSize % 32;
    }

    @Override
    public IVariantProperty getProperty(int groupNum, int resNum) {
        return new VariantProperty(groupNum, resNum);
    }

    static class VariantProperty implements IVariantProperty {
        /**
         * 位点扩展属性方法
         */
        public boolean hasMiss;
        public float P_REF;
        public float P_ALT;
        public int N_ALT;
        public int N_ALLELE_I_SQUARE;
        public int[] N_ALLELE_NUM_EQ_1;
        public int[] N_ALLELE_NUM_EQ_2;
        public int[] validSampleFlags;

        /**
         * 长度信息
         */
        public final int groupNum;
        public final int resNum;

        /**
         * 位编码表细节
         * .|.: BEG 0, convert to 0 and 0
         * 0|0: BEG 1, convert to 0 abd 0
         * 0|1: BEG 2, convert to 1 and 0
         * 1|1: BEG 3, convert to 0 and 1
         * 1|0: BEG 4, convert to 1 and 0
         */
        static final byte[][] ALLELE_COUNT = new byte[][]{new byte[]{0, 0}, new byte[]{0, 0}, new byte[]{1, 0}, new byte[]{0, 1}, new byte[]{1, 0}};

        public VariantProperty(int groupNum, int resNum) {
            this.N_ALLELE_NUM_EQ_1 = new int[groupNum];
            this.N_ALLELE_NUM_EQ_2 = new int[groupNum];
            this.validSampleFlags = new int[groupNum];

            this.groupNum = groupNum;
            this.resNum = resNum;
        }

        @Override
        public VariantProperty fillBitCodes(Variant variant) {
            byte begCode;
            int validSampleNum = 0;
            N_ALLELE_I_SQUARE = 0;
            N_ALT = 0;

            for (int i = 0; i < (resNum == 0 ? groupNum : groupNum - 1); i++) {
                N_ALLELE_NUM_EQ_1[i] = 0;
                N_ALLELE_NUM_EQ_2[i] = 0;
                validSampleFlags[i] = 0;

                for (int j = 0; j < 32; j++) {
                    begCode = variant.BEGs[(i << 5) + j];

                    N_ALLELE_NUM_EQ_1[i] = (N_ALLELE_NUM_EQ_1[i] << 1) | ALLELE_COUNT[begCode][0];
                    N_ALLELE_NUM_EQ_2[i] = (N_ALLELE_NUM_EQ_2[i] << 1) | ALLELE_COUNT[begCode][1];

                    // 校验缺失，有效样本标记为 1，否则为 0
                    if (BEGDecoder.isMiss(begCode)) {
                        validSampleFlags[i] = validSampleFlags[i] << 1;
                    } else {
                        validSampleFlags[i] = (validSampleFlags[i] << 1) | 1;
                    }
                }

                validSampleNum += Integer.bitCount(validSampleFlags[i]);
                int count_ALLELE_NUM_EQ_1 = Integer.bitCount(N_ALLELE_NUM_EQ_1[i]);
                int count_ALLELE_NUM_EQ_2 = Integer.bitCount(N_ALLELE_NUM_EQ_2[i]);
                N_ALLELE_I_SQUARE += count_ALLELE_NUM_EQ_1 + (count_ALLELE_NUM_EQ_2 << 2);
                N_ALT += count_ALLELE_NUM_EQ_1 + (count_ALLELE_NUM_EQ_2 << 1);
            }

            if (resNum != 0) {
                // 编码最后一个 bitcode
                int i = groupNum - 1;

                N_ALLELE_NUM_EQ_1[i] = 0;
                N_ALLELE_NUM_EQ_2[i] = 0;
                validSampleFlags[i] = 0;
                for (int j = 0; j < resNum; j++) {
                    begCode = variant.BEGs[(i << 5) + j];
                    N_ALLELE_NUM_EQ_1[i] = (N_ALLELE_NUM_EQ_1[i] << 1) | ALLELE_COUNT[begCode][0];
                    N_ALLELE_NUM_EQ_2[i] = (N_ALLELE_NUM_EQ_2[i] << 1) | ALLELE_COUNT[begCode][1];

                    if (BEGDecoder.isMiss(begCode)) {
                        validSampleFlags[i] = validSampleFlags[i] << 1;
                    } else {
                        validSampleFlags[i] = (validSampleFlags[i] << 1) | 1;
                    }
                }

                validSampleNum += Integer.bitCount(validSampleFlags[i]);
                int count_ALLELE_NUM_EQ_1 = Integer.bitCount(N_ALLELE_NUM_EQ_1[i]);
                int count_ALLELE_NUM_EQ_2 = Integer.bitCount(N_ALLELE_NUM_EQ_2[i]);
                N_ALLELE_I_SQUARE += count_ALLELE_NUM_EQ_1 + (count_ALLELE_NUM_EQ_2 << 2);
                N_ALT += count_ALLELE_NUM_EQ_1 + (count_ALLELE_NUM_EQ_2 << 1);

                // 末尾也标记为缺失
                int shiftingNum = 32 - resNum;
                N_ALLELE_NUM_EQ_1[i] = N_ALLELE_NUM_EQ_1[i] << shiftingNum;
                N_ALLELE_NUM_EQ_2[i] = N_ALLELE_NUM_EQ_2[i] << shiftingNum;
                validSampleFlags[i] = validSampleFlags[i] << shiftingNum;
            }

            // 标记为无缺失基因型
            hasMiss = validSampleNum != (variant.BEGs.length);

            P_ALT = (float) (N_ALT) / (validSampleNum << 1);
            P_REF = 1 - P_ALT;

            return this;
        }

        /**
         * 检查 MAF 是否满足要求
         */
        @Override
        public boolean checkMaf(double maf) {
            return P_ALT >= maf && P_REF >= maf;
        }
    }
}