package edu.sysu.pmglab.gbc.coder.decoder;

/**
 * @Data        :2021/03/22
 * @Author      :suranyi
 * @Contact     :suranyi.sysu@gamil.com
 * @Description :组合解码器接口
 */

public interface MBEGDecoder {
    /**
     * 对 code 进行解包
     * @param code MBEG 编码值
     * @param codeIndex 需要解析的索引 ID
     * @return 对应的 BEG 编码
     */
    byte decode(int code, int codeIndex);

    /**
     * 对 code 进行解包
     * @param code MBEG 编码值
     * @param codeIndex 需要解析的索引 ID
     * @return 对应的 BEG 编码
     */
    default byte decode(byte code, int codeIndex) {
        return decode(code & 0xFF, codeIndex);
    }

    /**
     * 判断当前编码表是否有向
     * @return 向型
     */
    boolean isPhased();

    /**
     * 获取对应的组合编码器
     * @param phased 基因型数据是否有向
     * @return 对应的组合解码器
     */
    static MBEGDecoder getDecoder(boolean phased) {
        if (phased) {
            return PhasedGroupDecoder.getInstance();
        } else {
            return UnPhasedGroupDecoder.getInstance();
        }
    }

    /**
     * 获取所有的组合解码器
     * @return 组合解码器
     */
    static MBEGDecoder[] getDecoders() {
        return new MBEGDecoder[]{UnPhasedGroupDecoder.getInstance(), PhasedGroupDecoder.getInstance()};
    }
}
