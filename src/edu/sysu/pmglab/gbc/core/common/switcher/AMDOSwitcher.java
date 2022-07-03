package edu.sysu.pmglab.gbc.core.common.switcher;

import edu.sysu.pmglab.gbc.coder.encoder.BEGEncoder;
import edu.sysu.pmglab.gbc.core.gtbcomponent.gtbreader.Variant;

import java.util.Arrays;

/**
 * @Data :2021/02/23
 * @Author :suranyi
 * @Contact :suranyi.sysu@gamil.com
 * @Description :特征交换器
 */

public enum AMDOSwitcher implements ISwitcher {
    /**
     * 单例模式特征交换器
     */
    INSTANCE;

    @Override
    public void switchingRow(BEGEncoder encoder, Variant<AMDOFeature>[] variants, int variantsNum) {
        for (int i = 0; i < variantsNum; i++) {
            variants[i].property.setFeatureVector(encoder, variants[i]);
        }

        Arrays.sort(variants, 0, variantsNum, AMDOFeature::comparatorBaseOnFeatureVector);
    }
}
