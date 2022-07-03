package edu.sysu.pmglab.gbc.core.common.qualitycontrol.variant;

import edu.sysu.pmglab.gbc.core.gtbcomponent.gtbreader.Variant;

/**
 * @Data :2021/03/07
 * @Author :suranyi
 * @Contact :suranyi.sysu@gamil.com
 * @Description :控制器接口
 */

public interface IVariantQC {
    /**
     * 执行过滤，true 时 保留该位点
     *
     * @param alleleCount     该位点的等位基因计数
     * @param validAllelesNum 有效等位基因总数
     * @return 是否保留该位点数据
     */
    default boolean filter(int alleleCount, int validAllelesNum) {
        throw new UnsupportedOperationException("this QC is not applicable");
    }

    /**
     * 执行过滤，true 时 保留该位点
     *
     * @param variant 对位点进行质控
     * @return 是否保留该位点数据
     */
    default boolean filter(Variant variant) {
        throw new UnsupportedOperationException("this QC is not applicable");
    }

    /**
     * 质控方法，true 代表保留，false 代表需要剔除
     * @param marker 位点基本数据信息
     * @return 是否保留该位点数据
     */
    default boolean filter(VCFNonGenotypeMarker marker) {
        throw new UnsupportedOperationException("this QC is not applicable");
    }

    /**
     * 是否为空控制器
     *
     * @return 是否为空控制器
     */
    default boolean empty() {
        return false;
    }

    /**
     * 注销过滤器
     */
    default void setEmpty() {
    }
}
