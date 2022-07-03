package edu.sysu.pmglab.gbc.coder.decoder;

import edu.sysu.pmglab.gbc.coder.MBEGCoderException;

/**
 * @Data :2021/03/22
 * @Author :suranyi
 * @Contact :suranyi.sysu@gamil.com
 * @Description :有向组合解码器
 */

enum PhasedGroupDecoder implements MBEGDecoder {
    /**
     * 单例模式组合解码器
     */
    INSTANCE;

    final byte[][] groupDecoder;

    PhasedGroupDecoder() {
        this.groupDecoder = new byte[5 * 5 * 5][3];

        // 有向
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                for (int k = 0; k < 5; k++) {
                    int index = 25 * i + 5 * j + k;
                    this.groupDecoder[index][0] = (byte) i;
                    this.groupDecoder[index][1] = (byte) j;
                    this.groupDecoder[index][2] = (byte) k;
                }
            }
        }
    }

    @Override
    public byte decode(int code, int codeIndex) {
        try {
            return this.groupDecoder[code][codeIndex];
        } catch (ArrayIndexOutOfBoundsException e) {
            if (code >= 0 && code <= this.groupDecoder.length) {
                // codeIndex 出问题
                throw new MBEGCoderException("MBEG decode error: phased BEG code index(" + codeIndex + ") out of range [0, 2]");
            } else {
                throw new MBEGCoderException("MBEG decode error: phased MBEG code(" + code + ") out of range [0, " + (this.groupDecoder.length - 1) + "]");
            }
        }
    }

    @Override
    public boolean isPhased() {
        return true;
    }

    static MBEGDecoder getInstance() {
        return INSTANCE;
    }
}
