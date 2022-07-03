package edu.sysu.pmglab.gbc.core.common.qualitycontrol.variant;

import edu.sysu.pmglab.check.Assert;
import edu.sysu.pmglab.gbc.core.gtbcomponent.gtbreader.Variant;

import java.util.Map;

/**
 * @Data        :2021/03/06
 * @Author      :suranyi
 * @Contact     :suranyi.sysu@gamil.com
 * @Description :等位基因计数控制器
 */

public class AlleleCountController implements IVariantQC {
    int minAc;
    int maxAc;

    public static final String KEYWORD = "AC";
    public static final int DEFAULT = 0;
    public static final int MIN = 0;
    public static final int MAX = Integer.MAX_VALUE;

    public AlleleCountController(int minAc, int maxAc) {
        Assert.valueRange(minAc, MIN, MAX);
        Assert.valueRange(maxAc, MIN, MAX);

        this.minAc = minAc;
        this.maxAc = maxAc;
    }

    @Override
    public boolean filter(int alleleCount, int validAllelesNum) {
        return (alleleCount >= this.minAc) && (alleleCount <= this.maxAc);
    }

    @Override
    public boolean filter(Variant variant) {
        int AC = variant.getAC();
        return (AC >= this.minAc) && (AC <= this.maxAc);
    }

    @Override
    public boolean filter(VCFNonGenotypeMarker marker) {
        Map<String, String> dict = marker.getInfo();
        if (!dict.containsKey(KEYWORD)) {
            return true;
        } else {
            try {
                int AC = Integer.parseInt(dict.get(KEYWORD));
                return (AC >= this.minAc) && (AC <= this.maxAc);
            } catch (NumberFormatException e) {
                return true;
            }
        }
    }

    @Override
    public boolean empty() {
        return (this.minAc == MIN) && (this.maxAc == MAX);
    }

    @Override
    public void setEmpty() {
        this.minAc = MIN;
        this.maxAc = MAX;
    }

    @Override
    public String toString() {
        return (this.maxAc == MAX) ? "AC >= " + this.minAc : this.minAc + " <= AC <= " + this.maxAc;
    }
}
