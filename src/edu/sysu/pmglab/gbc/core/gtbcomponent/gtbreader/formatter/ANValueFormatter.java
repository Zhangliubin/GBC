/**
 * @Data        :2021/08/16
 * @Author      :suranyi
 * @Contact     :suranyi.sysu@gamil.com
 * @Description :
 */

package edu.sysu.pmglab.gbc.core.gtbcomponent.gtbreader.formatter;

import edu.sysu.pmglab.gbc.constant.ChromosomeTags;
import edu.sysu.pmglab.gbc.core.gtbcomponent.gtbreader.Variant;

public enum ANValueFormatter implements VariantFormatter<Void, Integer>{
    /**
     * AN 计数器
     */
    INSTANCE;

    @Override
    public Integer apply(Variant variant) {
        return (variant.BEGs.length - variant.getMissSubjectNum()) * ChromosomeTags.getPloidy(variant.chromosome);
    }
}