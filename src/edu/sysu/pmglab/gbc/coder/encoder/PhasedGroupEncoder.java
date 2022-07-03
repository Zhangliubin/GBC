package edu.sysu.pmglab.gbc.coder.encoder;

import edu.sysu.pmglab.gbc.coder.MBEGCoderException;

/**
 * @Data :2021/03/22
 * @Author :suranyi
 * @Contact :suranyi.sysu@gamil.com
 * @Description :有向组合编码器
 */

enum PhasedGroupEncoder implements MBEGEncoder {
    /**
     * 单例模式有向组合编码器
     */
    INSTANCE;

    byte[][][] encoder;

    PhasedGroupEncoder() {
        this.encoder = new byte[5][5][5];

        // 有向
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                for (int k = 0; k < 5; k++) {
                    this.encoder[i][j][k] = (byte) (i * 25 + j * 5 + k);
                }
            }
        }
    }

    @Override
    public byte encode(byte code1, byte code2, byte code3) {
        try {
            return this.encoder[code1][code2][code3];
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new MBEGCoderException("MBEG encode error: only biallelic genotypes (code values between 0 ~ 5) can be combined with multiple genotypes using the phased MBEG encoder");
        }
    }

    @Override
    public byte encode(byte code1, byte code2, byte code3, byte code4) {
        throw new MBEGCoderException("MBEG encode error: the phased MBEG encoder combined up to 3 genotypes");
    }

    @Override
    public boolean isPhased() {
        return true;
    }

    static MBEGEncoder getInstance() {
        return INSTANCE;
    }
}
