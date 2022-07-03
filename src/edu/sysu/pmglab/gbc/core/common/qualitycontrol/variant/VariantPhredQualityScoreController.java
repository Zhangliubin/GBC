package edu.sysu.pmglab.gbc.core.common.qualitycontrol.variant;

import edu.sysu.pmglab.check.Assert;
import edu.sysu.pmglab.gbc.core.gtbcomponent.gtbreader.Variant;

/**
 * @Data        :2021/06/07
 * @Author      :suranyi
 * @Contact     :suranyi.sysu@gamil.com
 * @Description :位点QUAL控制器
 */

public class VariantPhredQualityScoreController implements IVariantQC {
    public static final int DEFAULT = 30;
    public static final int MIN = 0;
    public static final int MAX = Integer.MAX_VALUE - 2;
    int method;

    public VariantPhredQualityScoreController() {
        this.method = DEFAULT;
    }

    public VariantPhredQualityScoreController(int method) {
        Assert.valueRange(method, MIN, MAX);
        this.method = method;
    }

    @Override
    public boolean filter(int alleleCount, int validAllelesNum) {
        return true;
    }

    @Override
    public boolean filter(Variant variant) {
        return true;
    }

    @Override
    public boolean filter(VCFNonGenotypeMarker marker) {
        double value = marker.getQUAL();
        return Double.isNaN(value) || value >= method;
    }

    @Override
    public boolean empty() {
        return this.method == MIN;
    }

    @Override
    public void setEmpty() {
        this.method = MIN;
    }

    @Override
    public String toString() {
        return "Phred quality score >= " + this.method;
    }
}
