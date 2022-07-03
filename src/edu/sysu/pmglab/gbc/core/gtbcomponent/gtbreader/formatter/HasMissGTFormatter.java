package edu.sysu.pmglab.gbc.core.gtbcomponent.gtbreader.formatter;

import edu.sysu.pmglab.gbc.coder.decoder.BEGDecoder;
import edu.sysu.pmglab.gbc.core.gtbcomponent.gtbreader.Variant;

/**
 * @author suranyi
 */

public enum HasMissGTFormatter implements VariantFormatter<Void, Boolean> {
    /**
     * 是否有缺失基因型
     */
    INSTANCE;

    @Override
    public Boolean apply(Variant variant) {
        for (byte code : variant.BEGs) {
            if (BEGDecoder.isMiss(code)) {
                return true;
            }
        }
        return false;
    }
}