package edu.sysu.pmglab.gbc.core.common.qualitycontrol.genotype;

import java.util.Map;

/**
 * @Data :2021/08/29
 * @Author :suranyi
 * @Contact :suranyi.sysu@gamil.com
 * @Description :基因型质控器接口
 */

public interface IGenotypeQC {
    /**
     * 执行过滤，true 时保留该位点
     *
     * @param individual 单个基因型的信息
     * @return 是否保留该位点数据
     */
    boolean filter(Map<String, String> individual);

    /**
     * 是否为空过滤器
     *
     * @return 是否为空过滤器
     */
    default boolean empty() {
        return false;
    }

    /**
     * 注销过滤器
     */
    default void setEmpty() {
    }

    /**
     * 获取关键字
     *
     * @return 关键字
     */
    String getKeyWord();

    /**
     * 获取阈值
     */
    int getMethod();
}
