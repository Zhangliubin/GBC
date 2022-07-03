package edu.sysu.pmglab.gbc.core.gtbcomponent.gtbreader.formatter;

import edu.sysu.pmglab.gbc.core.gtbcomponent.gtbreader.Variant;

/**
 * @Data        :2021/08/16
 * @Author      :suranyi
 * @Contact     :suranyi.sysu@gamil.com
 * @Description :
 */

public enum AFValueFormatter implements VariantFormatter<Void, Double> {
    /**
     * AF 计数器
     */
    INSTANCE;

    @Override
    public Double apply(Variant variant) {
        int AN = variant.getAN();
        if (AN == 0) {
            return 0d;
        } else {
            return (double) variant.getAC() / AN;
        }
    }
}