package edu.sysu.pmglab.gbc.core.calculation.ld;

import edu.sysu.pmglab.gbc.coder.decoder.BEGDecoder;
import edu.sysu.pmglab.container.VolumeByteStream;
import edu.sysu.pmglab.easytools.ByteCode;
import edu.sysu.pmglab.easytools.ValueUtils;
import edu.sysu.pmglab.gbc.constant.ChromosomeTags;
import edu.sysu.pmglab.gbc.core.gtbcomponent.gtbreader.Variant;

/**
 * @author suranyi
 */

enum HaplotypeLD implements ILDModel, ILDContext {
    /**
     * 单倍型 LD 模型: 分为 D' 法和 r^2 法
     * 该模型会考虑基因型的向型，unphased 和 phased 的文件分析结果不一致
     */
    INSTANCE;

    static String EXTENSION = ".hap.ld";

    @Override
    public int calculateLDR2(VolumeByteStream lineCache, Variant variant1, Variant variant2, double minR2) {
        VariantProperty propertyA = (VariantProperty) variant1.property;
        VariantProperty propertyB = (VariantProperty) variant2.property;
        int validAlleleNum;

        float PA_ALT;
        float PA_REF;
        float PB_ALT;
        float PB_REF;
        float PAB_ALT;

        if (propertyA.hasMiss || propertyB.hasMiss) {
            // 至少有一个位点包含 miss 基因型时，需要重新计算
            int countAB_ALT = 0;
            validAlleleNum = 0;
            int countA_ALT = 0;
            int countB_ALT = 0;

            for (int i = 0; i < propertyA.groupNum; i++) {
                // 有效等位基因状态及个数
                int validAlleleStatus = (propertyA.validAlleleFlags[i] & propertyB.validAlleleFlags[i]);
                int currentValidAlleleNum = Integer.bitCount(validAlleleStatus);

                if (currentValidAlleleNum > 0) {
                    // 不全是缺失
                    countA_ALT += Integer.bitCount(propertyA.bitCodes[i] & validAlleleStatus);
                    countB_ALT += Integer.bitCount(propertyB.bitCodes[i] & validAlleleStatus);
                    countAB_ALT += Integer.bitCount(propertyA.bitCodes[i] & propertyB.bitCodes[i]);
                    validAlleleNum += currentValidAlleleNum;
                }
            }

            if (validAlleleNum == 0 || countA_ALT == 0 || countB_ALT == 0) {
                return 0;
            }

            PA_ALT = (float) countA_ALT / validAlleleNum;
            PB_ALT = (float) countB_ALT / validAlleleNum;
            PA_REF = 1 - PA_ALT;
            PB_REF = 1 - PB_ALT;
            PAB_ALT = (float) countAB_ALT / validAlleleNum;
        } else {
            // 没有任何缺失，此时借助缓冲数据提升速度
            validAlleleNum = variant1.BEGs.length << 1;

            int countAB_ALT = 0;
            for (int i = 0; i < propertyA.groupNum; i++) {
                countAB_ALT += Integer.bitCount(propertyA.bitCodes[i] & propertyB.bitCodes[i]);
            }
            PA_ALT = propertyA.P_ALT;
            PA_REF = propertyA.P_REF;
            PB_ALT = propertyB.P_ALT;
            PB_REF = propertyB.P_REF;
            PAB_ALT = (float) countAB_ALT / validAlleleNum;
        }

        if (validAlleleNum == 0 || PA_ALT == 0 || PB_ALT == 0 || PA_REF == 0 || PB_REF == 0) {
            return 0;
        }


        float D = (PAB_ALT - PA_ALT * PB_ALT);
        float r2 = D * D / (PA_ALT * PB_ALT * PA_REF * PB_REF);
        if (r2 < minR2) {
            return 0;
        }
        float Dprime = D < 0 ? D / Math.min(PA_REF * PB_REF, PA_ALT * PB_ALT) : D / Math.min(PA_REF * PB_ALT, PA_ALT * PB_REF);

        lineCache.reset();
        formatterOut(lineCache, variant1.chromosome, variant1.position, variant2.position, validAlleleNum >> (2 - ChromosomeTags.getPloidy(variant1.chromosome)), D, Dprime, r2);
        return lineCache.size();
    }

    @Override
    public double calculateLDR2(Variant variant1, Variant variant2) {
        if (!(variant1.property instanceof VariantProperty)) {
            variant1.property = getProperty(variant1.BEGs.length).fillBitCodes(variant1);
        }

        if (!(variant2.property instanceof VariantProperty)) {
            variant2.property = getProperty(variant2.BEGs.length).fillBitCodes(variant2);
        }

        VariantProperty propertyA = (VariantProperty) variant1.property;
        VariantProperty propertyB = (VariantProperty) variant2.property;
        int validAlleleNum;

        float PA_ALT;
        float PA_REF;
        float PB_ALT;
        float PB_REF;
        float PAB_ALT;

        if (propertyA.hasMiss || propertyB.hasMiss) {
            // 至少有一个位点包含 miss 基因型时，需要重新计算
            int countAB_ALT = 0;
            validAlleleNum = 0;
            int countA_ALT = 0;
            int countB_ALT = 0;

            for (int i = 0; i < propertyA.groupNum; i++) {
                // 有效等位基因状态及个数
                int validAlleleStatus = (propertyA.validAlleleFlags[i] & propertyB.validAlleleFlags[i]);
                int currentValidAlleleNum = Integer.bitCount(validAlleleStatus);

                if (currentValidAlleleNum > 0) {
                    // 不全是缺失
                    countA_ALT += Integer.bitCount(propertyA.bitCodes[i] & validAlleleStatus);
                    countB_ALT += Integer.bitCount(propertyB.bitCodes[i] & validAlleleStatus);
                    countAB_ALT += Integer.bitCount(propertyA.bitCodes[i] & propertyB.bitCodes[i]);
                    validAlleleNum += currentValidAlleleNum;
                }
            }

            if (validAlleleNum == 0 || countA_ALT == 0 || countB_ALT == 0) {
                return Double.NaN;
            }

            PA_ALT = (float) countA_ALT / validAlleleNum;
            PB_ALT = (float) countB_ALT / validAlleleNum;
            PA_REF = 1 - PA_ALT;
            PB_REF = 1 - PB_ALT;
            PAB_ALT = (float) countAB_ALT / validAlleleNum;
        } else {
            // 没有任何缺失，此时借助缓冲数据提升速度
            validAlleleNum = variant1.BEGs.length << 1;

            int countAB_ALT = 0;
            for (int i = 0; i < propertyA.groupNum; i++) {
                countAB_ALT += Integer.bitCount(propertyA.bitCodes[i] & propertyB.bitCodes[i]);
            }
            PA_ALT = propertyA.P_ALT;
            PA_REF = propertyA.P_REF;
            PB_ALT = propertyB.P_ALT;
            PB_REF = propertyB.P_REF;
            PAB_ALT = (float) countAB_ALT / validAlleleNum;
        }

        if (validAlleleNum == 0 || PA_ALT == 0 || PB_ALT == 0 || PA_REF == 0 || PB_REF == 0) {
            return Double.NaN;
        }


        float D = (PAB_ALT - PA_ALT * PB_ALT);
        float r2 = D * D / (PA_ALT * PB_ALT * PA_REF * PB_REF);

        return r2;
    }

    @Override
    public double calculateLD(Variant variant1, Variant variant2) {
        if (!(variant1.property instanceof VariantProperty)) {
            variant1.property = getProperty(variant1.BEGs.length).fillBitCodes(variant1);
        }

        if (!(variant2.property instanceof VariantProperty)) {
            variant2.property = getProperty(variant2.BEGs.length).fillBitCodes(variant2);
        }

        VariantProperty propertyA = (VariantProperty) variant1.property;
        VariantProperty propertyB = (VariantProperty) variant2.property;
        int validAlleleNum;

        float PA_ALT;
        float PA_REF;
        float PB_ALT;
        float PB_REF;
        float PAB_ALT;

        if (propertyA.hasMiss || propertyB.hasMiss) {
            // 至少有一个位点包含 miss 基因型时，需要重新计算
            int countAB_ALT = 0;
            validAlleleNum = 0;
            int countA_ALT = 0;
            int countB_ALT = 0;

            for (int i = 0; i < propertyA.groupNum; i++) {
                // 有效等位基因状态及个数
                int validAlleleStatus = (propertyA.validAlleleFlags[i] & propertyB.validAlleleFlags[i]);
                int currentValidAlleleNum = Integer.bitCount(validAlleleStatus);

                if (currentValidAlleleNum > 0) {
                    // 不全是缺失
                    countA_ALT += Integer.bitCount(propertyA.bitCodes[i] & validAlleleStatus);
                    countB_ALT += Integer.bitCount(propertyB.bitCodes[i] & validAlleleStatus);
                    countAB_ALT += Integer.bitCount(propertyA.bitCodes[i] & propertyB.bitCodes[i]);
                    validAlleleNum += currentValidAlleleNum;
                }
            }

            if (validAlleleNum == 0 || countA_ALT == 0 || countB_ALT == 0) {
                return Double.NaN;
            }

            PA_ALT = (float) countA_ALT / validAlleleNum;
            PB_ALT = (float) countB_ALT / validAlleleNum;
            PA_REF = 1 - PA_ALT;
            PB_REF = 1 - PB_ALT;
            PAB_ALT = (float) countAB_ALT / validAlleleNum;
        } else {
            // 没有任何缺失，此时借助缓冲数据提升速度
            validAlleleNum = variant1.BEGs.length << 1;

            int countAB_ALT = 0;
            for (int i = 0; i < propertyA.groupNum; i++) {
                countAB_ALT += Integer.bitCount(propertyA.bitCodes[i] & propertyB.bitCodes[i]);
            }
            PA_ALT = propertyA.P_ALT;
            PA_REF = propertyA.P_REF;
            PB_ALT = propertyB.P_ALT;
            PB_REF = propertyB.P_REF;
            PAB_ALT = (float) countAB_ALT / validAlleleNum;
        }

        if (validAlleleNum == 0 || PA_ALT == 0 || PB_ALT == 0 || PA_REF == 0 || PB_REF == 0) {
            return Double.NaN;
        }


        float D = (PAB_ALT - PA_ALT * PB_ALT);
        return D / Math.sqrt(PA_ALT * PB_ALT * PA_REF * PB_REF);
    }

    @Override
    public String getHeader() {
        return "CHR\tPOS1\tPOS2\tN_CHR\tD\tD'\tR^2";
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
        lineCache.write(ByteCode.TAB);
        lineCache.write(ValueUtils.stringValueOfAndGetBytes(score[1], 6));
        lineCache.write(ByteCode.TAB);
        lineCache.write(ValueUtils.stringValueOfAndGetBytes(score[2], 6));
    }

    @Override
    public int getGroupNum(int BEGSize) {
        return BEGSize / 16 + (getResNum(BEGSize) == 0 ? 0 : 1);
    }

    @Override
    public int getResNum(int BEGSize) {
        return BEGSize % 16;
    }

    @Override
    public String getExtension() {
        return EXTENSION;
    }

    @Override
    public String toString() {
        return "Haplotype LD (Linkage disequilibrium coefficient r-square)";
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
        public int[] bitCodes;
        public int[] validAlleleFlags;

        /**
         * 长度信息
         */
        public final int groupNum;
        public final int resNum;


        /**
         * 位编码表细节
         * .|.: BEG 0, convert to 00, that is 0
         * 0|0: BEG 1, convert to 00, that is 0
         * 0|1: BEG 2, convert to 01, that is 1
         * 1|1: BEG 3, convert to 11, that is 3
         * 1|0: BEG 4, convert to 10, that is 2
         */
        static final byte[] BIT_ENCODER = new byte[]{0, 0, 1, 3, 2};

        public VariantProperty(int groupNum, int resNum) {
            this.bitCodes = new int[groupNum];
            this.validAlleleFlags = new int[groupNum];

            this.groupNum = groupNum;
            this.resNum = resNum;
        }

        @Override
        public VariantProperty fillBitCodes(Variant variant) {
            byte begCode;
            int validAllelesNum = 0;
            N_ALT = 0;

            for (int i = 0; i < (resNum == 0 ? groupNum : groupNum - 1); i++) {
                bitCodes[i] = 0;
                validAlleleFlags[i] = 0;

                for (int j = 0; j < 16; j++) {
                    begCode = variant.BEGs[(i << 4) + j];
                    bitCodes[i] = (bitCodes[i] << 2) | BIT_ENCODER[begCode];

                    // 校验缺失，有效基因型标记为 11，否则为 00
                    if (BEGDecoder.isMiss(begCode)) {
                        validAlleleFlags[i] = validAlleleFlags[i] << 2;
                    } else {
                        validAlleleFlags[i] = ((validAlleleFlags[i] << 2) | 3);
                    }
                }

                validAllelesNum += Integer.bitCount(validAlleleFlags[i]);
                N_ALT += Integer.bitCount(bitCodes[i]);
            }

            if (resNum != 0) {
                // 编码最后一个 bitcode
                int i = groupNum - 1;
                bitCodes[i] = 0;
                validAlleleFlags[i] = 0;

                for (int j = 0; j < resNum; j++) {
                    begCode = variant.BEGs[(i << 4) + j];
                    bitCodes[i] = ((bitCodes[i] << 2) | BIT_ENCODER[begCode]);

                    if (BEGDecoder.isMiss(begCode)) {
                        validAlleleFlags[i] = validAlleleFlags[i] << 2;
                    } else {
                        validAlleleFlags[i] = ((validAlleleFlags[i] << 2) | 3);
                    }

                }
                N_ALT += Integer.bitCount(bitCodes[i]);
                validAllelesNum += Integer.bitCount(validAlleleFlags[i]);

                // 末尾也标记为缺失
                int shiftingNum = 16 - resNum;
                bitCodes[i] = bitCodes[i] << (shiftingNum << 1);
                validAlleleFlags[i] = validAlleleFlags[i] << (shiftingNum << 1);
            }

            // 标记为无缺失基因型
            hasMiss = (validAllelesNum != (variant.BEGs.length << 1));

            P_ALT = (float) N_ALT / validAllelesNum;
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