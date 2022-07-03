/**
 * @Data :2021/08/16
 * @Author :suranyi
 * @Contact :suranyi.sysu@gamil.com
 * @Description :
 */

package edu.sysu.pmglab.gbc.core.gtbcomponent.gtbreader.formatter;

import edu.sysu.pmglab.gbc.coder.decoder.BEGDecoder;
import edu.sysu.pmglab.gbc.constant.ChromosomeTags;
import edu.sysu.pmglab.gbc.core.gtbcomponent.gtbreader.Variant;

public enum ACsValueFormatter implements VariantFormatter<Void, int[]> {
    /**
     * AC 计数器
     */
    INSTANCE;

    @Override
    public int[] apply(Variant variant) {
        int[] alleleCounts = new int[variant.getAlternativeAlleleNum()];

        if (ChromosomeTags.getPloidy(variant.chromosome) == 1) {
            // 单倍型
            for (byte code : variant.BEGs) {
                if (!BEGDecoder.isMiss(code)) {
                    alleleCounts[BEGDecoder.decodeHaplotype(0, code & 0xFF)] += 1;
                }
            }
        } else {
            // 二倍型
            for (byte code : variant.BEGs) {
                if (!BEGDecoder.isMiss(code)) {
                    alleleCounts[BEGDecoder.decodeHaplotype(0, code & 0xFF)] += 1;
                    alleleCounts[BEGDecoder.decodeHaplotype(1, code & 0xFF)] += 1;
                }
            }
        }

        return alleleCounts;
    }
}