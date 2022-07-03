package edu.sysu.pmglab.gbc.coder.encoder;

import edu.sysu.pmglab.gbc.coder.MBEGCoderException;

/**
 * @Data :2021/03/22
 * @Author :suranyi
 * @Contact :suranyi.sysu@gamil.com
 * @Description :有向编码器
 */

enum PhasedEncoder implements BEGEncoder {
    /**
     * 单例模式组合编码器
     */
    INSTANCE;
    final byte[][] encodeDict = initEncodeDict(true);

    @Override
    public boolean isPhased() {
        return true;
    }

    @Override
    public byte encode(int i, int j) {
        try {
            return this.encodeDict[i][j];
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new MBEGCoderException("MBEG encode error: the haplotype coding value is out of range [0, 14]");
        }
    }

    static BEGEncoder getInstance() {
        return INSTANCE;
    }
}
