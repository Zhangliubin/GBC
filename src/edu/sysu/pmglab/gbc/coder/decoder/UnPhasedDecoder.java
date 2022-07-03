package edu.sysu.pmglab.gbc.coder.decoder;

import edu.sysu.pmglab.gbc.coder.MBEGCoderException;

/**
 * @Data        :2021/03/22
 * @Author      :suranyi
 * @Contact     :suranyi.sysu@gamil.com
 * @Description :无向解码器
 */

enum UnPhasedDecoder implements BEGDecoder {
    /**
     * 单例模式解码器
     */
    INSTANCE;

    final byte[][][] decodeDict = initDecodeDict(false);

    @Override
    public byte[] decode(int ploidy, int code) {
        try {
            return decodeDict[ploidy][code];
        } catch (ArrayIndexOutOfBoundsException e) {
            if (ploidy == 1 || ploidy == 2) {
                throw new MBEGCoderException("BEG decode error: BEG code(" + code + ") out of range [0, 225]");
            } else {
                throw new MBEGCoderException("BEG decode error: BEG only supported decode genotype from haploid(ploidy=1) or diploid(ploidy=2) species");
            }
        }
    }

    @Override
    public boolean isPhased() {
        return false;
    }

    static BEGDecoder getInstance() {
        return INSTANCE;
    }
}
