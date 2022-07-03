/**
 * @Data :2021/08/16
 * @Author :suranyi
 * @Contact :suranyi.sysu@gamil.com
 * @Description :
 */

package edu.sysu.pmglab.gbc.core.gtbcomponent.gtbreader.formatter;

import edu.sysu.pmglab.gbc.core.gtbcomponent.gtbreader.Variant;

public enum GenotypeCountsFormatter implements VariantFormatter<Void, int[]> {
    /**
     * 基因型计数
     */
    INSTANCE;

    @Override
    public int[] apply(Variant variant) {
        int alleleNums = variant.getAlternativeAlleleNum();
        int[] counts = new int[alleleNums * alleleNums + 1];
        for (byte code : variant.BEGs) {
            counts[code] += 1;
        }
        return counts;
    }
}