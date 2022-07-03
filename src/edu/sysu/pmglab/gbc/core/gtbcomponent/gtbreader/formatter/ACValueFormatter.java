/**
 * @Data        :2021/08/16
 * @Author      :suranyi
 * @Contact     :suranyi.sysu@gamil.com
 * @Description :
 */

package edu.sysu.pmglab.gbc.core.gtbcomponent.gtbreader.formatter;

import edu.sysu.pmglab.gbc.coder.decoder.BEGDecoder;
import edu.sysu.pmglab.gbc.constant.ChromosomeTags;
import edu.sysu.pmglab.gbc.core.gtbcomponent.gtbreader.Variant;

public enum ACValueFormatter implements VariantFormatter<Void, Integer> {
    /**
     * AC 计数器
     */
    INSTANCE;

    @Override
    public Integer apply(Variant variant) {
        int alleleCounts = 0;
        int ploidy = ChromosomeTags.getPloidy(variant.chromosome);
        for (byte code : variant.BEGs) {
            alleleCounts += BEGDecoder.alternativeAlleleNumOf(ploidy, code & 0xFF);
        }
        return alleleCounts;
    }
}