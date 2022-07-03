package edu.sysu.pmglab.gbc.core.calculation.ld;

import edu.sysu.pmglab.gbc.core.gtbcomponent.gtbreader.Variant;

/**
 * @author suranyi
 */

public interface ILDModel {
    HaplotypeLD HAPLOTYPE_LD = HaplotypeLD.INSTANCE;
    GenotypeLD GENOTYPE_LD = GenotypeLD.INSTANCE;

    /**
     * 计算两位点的 LD 系数 R2
     *
     * @param variant1 位点 1
     * @param variant2 位点 2
     * @return LD 系数
     */
    double calculateLDR2(Variant variant1, Variant variant2);

    /**
     * 计算两位点的 LD 系数
     *
     * @param variant1 位点 1
     * @param variant2 位点 2
     * @return LD 系数
     */
    double calculateLD(Variant variant1, Variant variant2);
}
