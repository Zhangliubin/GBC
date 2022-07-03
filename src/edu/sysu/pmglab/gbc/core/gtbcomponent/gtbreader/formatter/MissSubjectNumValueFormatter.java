/**
 * @Data        :2021/08/16
 * @Author      :suranyi
 * @Contact     :suranyi.sysu@gamil.com
 * @Description :
 */

package edu.sysu.pmglab.gbc.core.gtbcomponent.gtbreader.formatter;

import edu.sysu.pmglab.gbc.coder.decoder.BEGDecoder;
import edu.sysu.pmglab.gbc.core.gtbcomponent.gtbreader.Variant;

public enum MissSubjectNumValueFormatter implements VariantFormatter<Void, Integer>{
    /**
     * AC 计数器
     */
    INSTANCE;

    @Override
    public Integer apply(Variant variant) {
        int missSubjectNum = 0;
        for (byte code : variant.BEGs) {
            missSubjectNum += BEGDecoder.isMiss(code) ? 1 : 0;
        }
        return missSubjectNum;
    }
}