package edu.sysu.pmglab.gbc.coder;

/**
 * @Data        :2021/08/26
 * @Author      :suranyi
 * @Contact     :suranyi.sysu@gamil.com
 * @Description :编码器
 */

public enum CoderConfig {
    /**
     * 单例编码器配置文件
     */
    INSTANCE;

    public static final int MAX_ALLELE_NUM = 15;
    public static final byte MISS_GENOTYPE_CODE = 0;
    public static final boolean DEFAULT_PHASED_STATUS = false;

    /**
     * 传入基因型数据左右侧数值，获得其编码数值. i | j -> i * allelesNumMax + j, .|. -> 0
     * @param i 左侧基因型
     * @param j 右侧基因型
     * @return 编码整数值
     */
    public static int mapGenotypeTo(int i, int j) {
        if (i >= j) {
            return ((i + 1) * (i + 1) - j);
        } else {
            return (j * j + i + 1);
        }
    }

    /**
     * 获取当前编码表支持的最大等位基因个数
     * @return allelesNumMax
     */
    public static int getAllelesNumMax() {
        return MAX_ALLELE_NUM;
    }
}