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

import java.util.Arrays;

public enum MAFValueFormatter implements VariantFormatter<Void, Double> {
    /**
     * MAF 计数器
     */
    INSTANCE;

    @Override
    public Double apply(Variant variant) {
        int allelesNum = variant.getAlternativeAlleleNum();
        if (allelesNum == 2) {
            // 二等位基因位点
            double refFreq = variant.getAF();
            return refFreq > 0.5 ? 1 - refFreq : refFreq;
        } else {
            // 多等位基因位点，寻找 MAF 更麻烦一些
            int[] alleleCounts = new int[allelesNum];
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

            Arrays.sort(alleleCounts);
            // 倒数第二个
            return (double) (alleleCounts[alleleCounts.length - 2]) / ((variant.BEGs.length - missSubjectNum) * ploidy);
        }
    }
}