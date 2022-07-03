package edu.sysu.pmglab.gbc.core.calculation.ld;

import edu.sysu.pmglab.gbc.core.gtbcomponent.gtbreader.Variant;

/**
 * @author suranyi
 */

public interface IVariantProperty {
    /**
     * 填充位块
     * @param variant 待填充的位点
     * @return 返回自身
     */
    IVariantProperty fillBitCodes(Variant variant);

    /**
     * 检查 MAF 是否满足要求
     * @param maf 次级等位基因频率阈值
     * @return 是否过滤
     */
    boolean checkMaf(double maf);
}