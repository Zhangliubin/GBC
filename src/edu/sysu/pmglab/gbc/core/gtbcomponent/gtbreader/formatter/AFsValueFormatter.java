package edu.sysu.pmglab.gbc.core.gtbcomponent.gtbreader.formatter;

import edu.sysu.pmglab.gbc.coder.decoder.BEGDecoder;
import edu.sysu.pmglab.gbc.constant.ChromosomeTags;
import edu.sysu.pmglab.gbc.core.gtbcomponent.gtbreader.Variant;

/**
 * @Data :2021/08/16
 * @Author :suranyi
 * @Contact :suranyi.sysu@gamil.com
 * @Description :
 */

public enum AFsValueFormatter implements VariantFormatter<Void, double[]> {
    /**
     * AF 计数器
     */
    INSTANCE;

    @Override
    public double[] apply(Variant variant) {
        int[] alleleCounts = new int[variant.getAlternativeAlleleNum()];
        int ploidy = ChromosomeTags.getPloidy(variant.chromosome);
        int missSubjectNum = 0;

        if (ploidy == 1) {
            // 单倍型
            for (byte code : variant.BEGs) {
                if (BEGDecoder.isMiss(code)) {
                    missSubjectNum += 1;
                } else {
                    alleleCounts[BEGDecoder.decodeHaplotype(0, code & 0xFF)] += 1;
                }
            }
        } else {
            // 二倍型
            for (byte code : variant.BEGs) {
                if (BEGDecoder.isMiss(code)) {
                    missSubjectNum += 1;
                } else {
                    alleleCounts[BEGDecoder.decodeHaplotype(0, code & 0xFF)] += 1;
                    alleleCounts[BEGDecoder.decodeHaplotype(1, code & 0xFF)] += 1;
                }
            }
        }

        double[] fqs = new double[alleleCounts.length];
        for (int i = 0; i < alleleCounts.length; i++) {
            fqs[i] = (double) alleleCounts[i] / ((variant.BEGs.length - missSubjectNum) * ploidy);
        }

        return fqs;
    }
}