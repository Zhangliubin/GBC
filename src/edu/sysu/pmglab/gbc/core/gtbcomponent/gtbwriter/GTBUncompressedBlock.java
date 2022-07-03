package edu.sysu.pmglab.gbc.core.gtbcomponent.gtbwriter;

import edu.sysu.pmglab.gbc.core.common.switcher.AMDOFeature;
import edu.sysu.pmglab.gbc.core.common.switcher.ISwitcher;
import edu.sysu.pmglab.gbc.core.gtbcomponent.gtbreader.Variant;

/**
 * GTB 未解压块
 */
public class GTBUncompressedBlock {
    public String chromosome;
    final Variant<AMDOFeature>[] variants;
    public int seek;

    public GTBUncompressedBlock(int validSubjectNum, GTBOutputParam outputParam) {
        this.variants = new Variant[outputParam.getBlockSize()];

        if (outputParam.isReordering()) {
            int windowSize = ISwitcher.getRealWindowSize(outputParam.getWindowSize(), validSubjectNum);
            int distanceFeatureLength = ISwitcher.getFeatureLength(validSubjectNum, windowSize);

            for (int i = 0, l = outputParam.getBlockSize(); i < l; i++) {
                this.variants[i] = new Variant<>();
                this.variants[i].BEGs = new byte[validSubjectNum];
                this.variants[i].phased = outputParam.isPhased();
                this.variants[i].property = new AMDOFeature(windowSize, distanceFeatureLength);
            }
        } else {
            for (int i = 0, l = outputParam.getBlockSize(); i < l; i++) {
                this.variants[i] = new Variant<>();
                this.variants[i].BEGs = new byte[validSubjectNum];
                this.variants[i].phased = outputParam.isPhased();
                this.variants[i].property = new AMDOFeature();
            }
        }
    }

    public void reset() {
        this.seek = 0;
    }

    public Variant<AMDOFeature> getCurrentVariant() {
        return this.variants[this.seek];
    }

    public int remaining() {
        return this.variants.length - this.seek;
    }

    public boolean empty() {
        return this.seek == 0;
    }

    public boolean full() {
        return this.seek == this.variants.length;
    }
}
