package edu.sysu.pmglab.gbc.coder.encoder;

import edu.sysu.pmglab.gbc.coder.MBEGCoderException;

/**
 * @Data :2021/03/22
 * @Author :suranyi
 * @Contact :suranyi.sysu@gamil.com
 * @Description :无向组合编码器
 */

enum UnPhasedGroupEncoder implements MBEGEncoder {
    /**
     * 单例模式无向组合编码器
     */
    INSTANCE;

    byte[][][][] encoder;

    UnPhasedGroupEncoder() {
        this.encoder = new byte[4][4][4][4];
        // 无向
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                for (int k = 0; k < 4; k++) {
                    for (int l = 0; l < 4; l++) {
                        this.encoder[i][j][k][l] = (byte) (i * 64 + j * 16 + k * 4 + l);
                    }
                }
            }
        }
    }

    @Override
    public byte encode(byte code1, byte code2, byte code3) {
        return encode(code1, code2, code3, code3);
    }

    @Override
    public byte encode(byte code1, byte code2, byte code3, byte code4) {
        try {
            return this.encoder[code1][code2][code3][code4];
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new MBEGCoderException("MBEG encode error: only biallelic genotypes (code values between 0 ~ 4) can be combined with multiple genotypes using the unphased MBEG encoder");
        }
    }

    @Override
    public boolean isPhased() {
        return false;
    }

    static MBEGEncoder getInstance() {
        return INSTANCE;
    }
}
