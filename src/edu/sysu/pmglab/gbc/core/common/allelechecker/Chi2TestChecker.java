package edu.sysu.pmglab.gbc.core.common.allelechecker;

import edu.sysu.pmglab.check.Assert;
import edu.sysu.pmglab.gbc.core.gtbcomponent.GTBManager;
import edu.sysu.pmglab.gbc.core.gtbcomponent.gtbreader.Variant;

import java.util.HashSet;

/**
 * @author suranyi
 * @description 简易卡方检验
 */

public class Chi2TestChecker implements AlleleChecker {
    /**
     * 认为两个位点是同一碱基序列的最大可接受 chi2 值
     */
    public final double maxChi2;
    public final double alpha;
    public double maf = DEFAULT_MAF;
    public final static double DEFAULT_ALPHA = 0.05;

    public Chi2TestChecker() {
        this.alpha = DEFAULT_ALPHA;
        this.maxChi2 = setMaxChi2(alpha);
    }

    public Chi2TestChecker(float alpha) {
        this.alpha = alpha;
        this.maxChi2 = setMaxChi2(alpha);
    }

    public Chi2TestChecker(double alpha) {
        this.alpha = alpha;
        this.maxChi2 = setMaxChi2(alpha);
    }

    public Chi2TestChecker(double alpha, double maf) {
        this.alpha = alpha;
        this.maxChi2 = setMaxChi2(alpha);
        this.maf = Assert.valueRange(maf, 0d, 0.5d);
    }

    double setMaxChi2(double alpha) {
        Assert.valueRange(alpha, 1e-6, 0.5);

        if (alpha == 1e-6) {
            return 23.92812697687947;
        } else if (alpha == 0.5) {
            return 0.454936423119572;
        } else {
            // 使用近似公式
            if (alpha >= 1e-6 && alpha < 1e-5) {
                return 2.34586449e+01 + (-4.41838821e+05) * alpha;
            } else if (alpha >= 1e-5 && alpha < 1e-4) {
                return 1.88426276e+01 + (-4.15884234e+04) * alpha + (4.75672979e-09) * Math.pow(alpha, 2);
            } else if (alpha >= 1e-4 && alpha < 1e-3) {
                return 1.55328732e+01 + (-9.13294550e+03) * alpha + (4.60423302e+06) * Math.pow(alpha, 2);
            } else if (alpha >= 1e-3 && alpha < 1e-2) {
                return 1.19016168e+01 + (-1.43556413e+03) * alpha + (1.59139387e+05) * Math.pow(alpha, 2) + (-6.90138384e+06) * Math.pow(alpha, 3) +
                        1.64402259e-09 * Math.pow(alpha, 4);
            } else if (alpha >= 1e-2 && alpha < 1e-1) {
                return 8.53764907e+00 + (-2.48201137e+02) * alpha + (6.18667126e+03) * Math.pow(alpha, 2) + (-9.16823671e+04) * Math.pow(alpha, 3) +
                        7.00663436e+05 * Math.pow(alpha, 4) + (-2.12785905e+06) * Math.pow(alpha, 5);
            } else {
                return 5.07876396 + (-35.12556786) * alpha + (145.19843024) * Math.pow(alpha, 2) + (-364.87267996) * Math.pow(alpha, 3) +
                        490.14028684 * Math.pow(alpha, 4) + (-268.4206007) * Math.pow(alpha, 5);
            }
        }
    }

    /**
     * 2 x 2 列联表卡方检验
     * 卡方值= n (ad-bc)^2/(a+b)(c+d)(a+c)(b+d)
     */
    @Override
    public boolean isEqual(Variant variant1, Variant variant2, double AC11, double AC12, double AC21, double AC22, boolean reverse) {
        double sum = (AC11 + AC12 + AC21 + AC22);

        double fq1 = AC11 / (AC11 + AC12);
        double fq2 = AC21 / (AC21 + AC22);

        // 如果是常见变异，则不能使用这个方法判断
        if (Math.min(fq1, 1 - fq1) > this.maf || Math.min(fq2, 1 - fq2) > this.maf) {
            return false;
        }

        // 卡方值小于给定最大值，接受原假设，两位点碱基序列差异不大
        if (sum >= 40 && AC11 >= 5 && AC12 >= 5 && AC21 >= 5 && AC22 >= 5) {
            AC11 = AC11 / 1000;
            AC12 = AC12 / 1000;
            AC21 = AC21 / 1000;
            AC22 = AC22 / 1000;

            double formula1 = (AC11 * AC22 - AC12 * AC21);
            double formula2 = (AC11 + AC12) * (AC11 + AC21) * (AC12 + AC22) * (AC21 + AC22);

            return sum * formula1 * formula1 / formula2 <= this.maxChi2;
        } else {
            // 使用 Yates 校正
            AC11 = AC11 / 1000;
            AC12 = AC12 / 1000;
            AC21 = AC21 / 1000;
            AC22 = AC22 / 1000;

            double formula1 = Math.abs(AC11 * AC22 - AC12 * AC21) - 0.5 * sum / (1000 * 1000);
            double formula2 = (AC11 + AC12) * (AC11 + AC21) * (AC12 + AC22) * (AC21 + AC22);

            return sum * formula1 * formula1 / formula2 <= this.maxChi2;
        }
    }

    @Override
    public void setReader(GTBManager manager1, GTBManager manager2) {

    }

    @Override
    public void setPosition(HashSet<Integer> position) {

    }

    @Override
    public String toString() {
        if (this.maf == 1) {
            return "chi^2 < " + this.maxChi2 + " (alpha = " + this.alpha + ")";
        } else {
            return "MAF <= " + this.maf + " and chi^2 < " + String.format("%.2f", this.maxChi2) + " (alpha = " + this.alpha + ")";
        }
    }

    @Override
    public void close() throws Exception {

    }

    @Override
    public AlleleChecker clone() {
        return new Chi2TestChecker(alpha, maf);
    }
}
