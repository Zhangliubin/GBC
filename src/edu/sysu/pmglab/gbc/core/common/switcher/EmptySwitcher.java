package edu.sysu.pmglab.gbc.core.common.switcher;

import edu.sysu.pmglab.gbc.coder.encoder.BEGEncoder;
import edu.sysu.pmglab.gbc.core.gtbcomponent.gtbreader.Variant;

import java.util.Arrays;

/**
 * @Data        :2021/03/26
 * @Author      :suranyi
 * @Contact     :suranyi.sysu@gamil.com
 * @Description :空交换器
 */

enum EmptySwitcher implements ISwitcher {
    /**
     * 单例模式特征交换器
     */
    INSTANCE;

    @Override
    public void switchingRow(BEGEncoder encoder, Variant<AMDOFeature>[] variants, int variantsNum) {
        Arrays.sort(variants, 0, variantsNum, AMDOFeature::comparatorBaseOnEncoderIndex);
    }
}
