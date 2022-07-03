package edu.sysu.pmglab.gbc.core.common.qualitycontrol.variant;

import edu.sysu.pmglab.check.Assert;
import edu.sysu.pmglab.gbc.core.gtbcomponent.gtbreader.Variant;

import java.util.Map;

/**
 * @Data        :2021/03/06
 * @Author      :suranyi
 * @Contact     :suranyi.sysu@gamil.com
 * @Description :最小有效等位基因数控制器
 */

public class AlleleNumberController implements IVariantQC {
    int minAn;
    int maxAn;

    public static final String KEYWORD = "AN";
    public static final int DEFAULT = 0;
    public static final int MIN = 0;
    public static final int MAX = Integer.MAX_VALUE;

    public AlleleNumberController(int minAn, int maxAn) {
        Assert.valueRange(minAn, MIN, MAX);
        Assert.valueRange(maxAn, MIN, MAX);

        this.minAn = minAn;
        this.maxAn = maxAn;
    }

    @Override
    public boolean filter(int alleleCount, int validAllelesNum) {
        return validAllelesNum >= this.minAn && validAllelesNum <= this.maxAn;
    }

    @Override
    public boolean filter(Variant variant) {
        int AN = variant.getAN();
        return (AN >= this.minAn) && (AN <= this.maxAn);
    }

    @Override
    public boolean filter(VCFNonGenotypeMarker marker) {
        Map<String, String> dict = marker.getInfo();
        if (!dict.containsKey(KEYWORD)) {
            return true;
        } else {
            try {
                int AN = Integer.parseInt(dict.get(KEYWORD));
                return (AN >= this.minAn) && (AN <= this.maxAn);
            } catch (NumberFormatException e) {
                return true;
            }
        }
    }

    @Override
    public boolean empty() {
        return (this.minAn == MIN) && (this.maxAn == MAX);
    }

    @Override
    public void setEmpty() {
        this.minAn = MIN;
        this.maxAn = MAX;
    }

    @Override
    public String toString() {
        return (this.maxAn == MAX) ? "AN >= " + this.minAn : this.minAn + " <= AN <= " + this.maxAn;
    }
}
