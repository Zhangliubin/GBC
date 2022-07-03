package edu.sysu.pmglab.gbc.core.common.allelechecker;

import edu.sysu.pmglab.check.Assert;
import edu.sysu.pmglab.gbc.core.gtbcomponent.GTBManager;
import edu.sysu.pmglab.gbc.core.gtbcomponent.gtbreader.Variant;

import java.util.HashSet;

/**
 * @author suranyi
 * @description 等位基因频率差值检验
 */

public class AlleleFreqGapTestChecker implements AlleleChecker {
    public final double alpha;
    public double maf = DEFAULT_MAF;
    public final static double DEFAULT_ALPHA = 0.05;

    public AlleleFreqGapTestChecker() {
        this(DEFAULT_ALPHA);
    }

    public AlleleFreqGapTestChecker(float alpha) {
        this((double) alpha);
    }

    public AlleleFreqGapTestChecker(double alpha) {
        Assert.valueRange(alpha, 1e-6, 0.5);
        this.alpha = alpha;
    }

    public AlleleFreqGapTestChecker(double alpha, double maf) {
        Assert.valueRange(alpha, 1e-6, 0.5);
        this.alpha = alpha;
        this.maf = Assert.valueRange(maf, 0d, 0.5d);
    }

    @Override
    public boolean isEqual(Variant variant1, Variant variant2, double AC11, double AC12, double AC21, double AC22, boolean reverse) {
        // 原假设: 观测频数没有区别 (即两位点的碱基序列相同)
        double fq1 = AC11 / (AC11 + AC12);
        double fq2 = AC21 / (AC21 + AC22);

        // 如果是常见变异，则不能使用这个方法判断
        return Math.min(fq1, 1 - fq1) <= this.maf && Math.min(fq2, 1 - fq2) <= this.maf && Math.abs(fq1 - fq2) <= this.alpha;
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
            return "|af1 - af2| < " + this.alpha;
        } else {
            return "MAF <= " + this.maf + " and |af1 - af2| < " + this.alpha;
        }
    }

    @Override
    public void close() throws Exception {

    }

    @Override
    public AlleleChecker clone() {
        return new AlleleFreqGapTestChecker(alpha, maf);
    }
}
