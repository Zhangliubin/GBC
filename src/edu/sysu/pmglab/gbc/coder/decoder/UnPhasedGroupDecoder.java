package edu.sysu.pmglab.gbc.coder.decoder;

import edu.sysu.pmglab.gbc.coder.MBEGCoderException;

/**
 * @Data        :2021/03/22
 * @Author      :suranyi
 * @Contact     :suranyi.sysu@gamil.com
 * @Description :无向组合解码器
 */

enum UnPhasedGroupDecoder implements MBEGDecoder {
    /**
     * 单例模式组合解码器
     */
    INSTANCE;

    final byte[][] groupDecoder;

    UnPhasedGroupDecoder() {
        this.groupDecoder = new byte[4 * 4 * 4 * 4][4];
        // 无向
        // 注意 3 -> 4
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                for (int k = 0; k < 4; k++) {
                    for (int l = 0; l < 4; l++) {
                        int index = 64 * i + 16 * j + 4 * k + l;
                        this.groupDecoder[index][0] = (byte) i;
                        this.groupDecoder[index][1] = (byte) j;
                        this.groupDecoder[index][2] = (byte) k;
                        this.groupDecoder[index][3] = (byte) l;
                    }
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
                throw new MBEGCoderException("MBEG decode error: unphased BEG code index(" + codeIndex + ") out of range [0, 3]");
            } else {
                throw new MBEGCoderException("MBEG decode error: unphased MBEG code value(" + code + ") out of range [0, " + (this.groupDecoder.length - 1) + "]");
            }
        }
    }

    @Override
    public boolean isPhased() {
        return false;
    }

    static MBEGDecoder getInstance() {
        return INSTANCE;
    }
}
