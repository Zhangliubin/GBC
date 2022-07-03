package edu.sysu.pmglab.gbc.coder.encoder;

/**
 * @Data :2021/03/22
 * @Author :suranyi
 * @Contact :suranyi.sysu@gamil.com
 * @Description :组合编码器接口
 */

public interface MBEGEncoder {
    /**
     * 获取对应的组合编码器
     *
     * @param phased 基因型数据是否有向
     * @return 对应的组合编码器
     */
    static MBEGEncoder getEncoder(boolean phased) {
        if (phased) {
            return PhasedGroupEncoder.getInstance();
        } else {
            return UnPhasedGroupEncoder.getInstance();
        }
    }

    /**
     * 获取所有的组合编码器
     *
     * @return 组合编码器
     */
    static MBEGEncoder[] getEncoders() {
        return new MBEGEncoder[]{UnPhasedGroupEncoder.getInstance(), PhasedGroupEncoder.getInstance()};
    }

    /**
     * 将 code1 进行组合
     *
     * @return 将编码 code1 进行组合的结果，缺失部分使用 code1 替代
     */
    default byte encode(byte code1) {
        return encode(code1, code1, code1);
    }

    /**
     * 将 code1, code2 进行组合
     *
     * @return 将编码 code1,code2 进行组合的结果，缺失部分使用 code2 替代
     */
    default byte encode(byte code1, byte code2) {
        return encode(code1, code2, code2);
    }

    /**
     * 将 code1, code2, code3 进行组合
     *
     * @return 将编码 code1,code2,code3 进行组合的结果，缺失部分使用 code3 替代
     */
    byte encode(byte code1, byte code2, byte code3);

    /**
     * 将 code1, code2, code3, code4 进行组合
     *
     * @return 将编码 code1,code2,code3,code4 进行组合的结果，缺失部分使用 code4 替代
     */
    byte encode(byte code1, byte code2, byte code3, byte code4);

    /**
     * 判断当前编码表是否有向
     */
    boolean isPhased();
}
