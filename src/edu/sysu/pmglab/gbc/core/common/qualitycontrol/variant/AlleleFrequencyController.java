package edu.sysu.pmglab.gbc.core.common.qualitycontrol.variant;

import edu.sysu.pmglab.check.Assert;
import edu.sysu.pmglab.gbc.core.gtbcomponent.gtbreader.Variant;

import java.util.Map;

/**
 * @Data        :2021/03/06
 * @Author      :suranyi
 * @Contact     :suranyi.sysu@gamil.com
 * @Description :等位基因频率控制器
 */

public class AlleleFrequencyController implements IVariantQC {
    double minAf;
    double maxAf;

    public static final String KEYWORD = "AF";
    public static final double DEFAULT = 0;
    public static final double MIN = 0;
    public static final double MAX = 1;

    public AlleleFrequencyController(double minAf, double maxAf) {
        Assert.valueRange(minAf, MIN, MAX);
        Assert.valueRange(maxAf, MIN, MAX);

        this.minAf = minAf;
        this.maxAf = maxAf;
    }

    @Override
    public boolean filter(int alleleCount, int validAllelesNum) {
        double minAc = validAllelesNum * this.minAf;
        double maxAc = validAllelesNum * this.maxAf;

        return (alleleCount >= minAc) && (alleleCount <= maxAc);
    }

    @Override
    public boolean filter(Variant variant) {
        double AF = variant.getAF();
        return (AF >= this.minAf) && (AF <= this.maxAf);
    }

    @Override
    public boolean filter(VCFNonGenotypeMarker marker) {
        Map<String, String> dict = marker.getInfo();
        if (!dict.containsKey(KEYWORD)) {
            return true;
        } else {
            try {
                double AF = Double.parseDouble(dict.get(KEYWORD));
                if (Double.isNaN(AF)) {
                    return true;
                }
                return (AF >= this.minAf) && (AF <= this.maxAf);
            } catch (NumberFormatException e) {
                return true;
            }
        }
    }

    @Override
    public boolean empty() {
        return (this.minAf == MIN) && (this.maxAf == MAX);
    }

    @Override
    public void setEmpty() {
        this.minAf = MIN;
        this.maxAf = MAX;
    }

    @Override
    public String toString() {
        return this.minAf + " <= AF <= " + this.maxAf;
    }
}
