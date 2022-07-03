package edu.sysu.pmglab.gbc.core.common.allelechecker;

import edu.sysu.pmglab.check.Assert;
import edu.sysu.pmglab.gbc.core.calculation.ld.ILDModel;
import edu.sysu.pmglab.gbc.core.gtbcomponent.GTBManager;
import edu.sysu.pmglab.gbc.core.gtbcomponent.gtbreader.GTBReader;
import edu.sysu.pmglab.gbc.core.gtbcomponent.gtbreader.Variant;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * @author suranyi
 * @description 检验 LD 系数
 */

public class LDTestChecker implements AlleleChecker {
    /**
     * 次级等位基因频率阈值
     */
    double maf = DEFAULT_MAF;

    /**
     * 最大覆盖的 bp 窗口
     */
    int windowSizeBp = DEFAULT_WINDOW_SIZE_BP;

    /**
     * 扫描阈值 (系数大于此值才认为可行的)
     */
    double minR = DEFAULT_R_VALUE;

    /**
     * 判定阈值
     */
    double alpha = DEFAULT_ALPHA;

    /**
     * 默认扫描阈值, 默认判定阈值
     */
    public final static double DEFAULT_R_VALUE = 0.8;
    public final static double DEFAULT_ALPHA = 0.8;

    /**
     * 计算 LD 模型 (默认为 genotype LD, 一般是先合并再分型)
     */
    public ILDModel ldModel = ILDModel.GENOTYPE_LD;

    /**
     * 默认窗口大小
     */
    public static final int DEFAULT_WINDOW_SIZE_BP = 10000;

    /**
     * 设置当前文件
     */
    GTBReader reader1;
    GTBReader reader2;
    HashSet<Integer> position;

    public LDTestChecker() {
    }

    public LDTestChecker(double alpha) {
        this.alpha = Assert.valueRange(alpha, 0.5d, 1d);
    }

    public LDTestChecker(double minR, double alpha) {
        this.minR = Assert.valueRange(minR, 0.5d, 1d);
        this.alpha = Assert.valueRange(alpha, 0.5d, 1d);
    }

    public LDTestChecker(double alpha, int windowSizeBp) {
        this.alpha = Assert.valueRange(alpha, 0.5d, 1d);
        this.windowSizeBp = Assert.valueRange(windowSizeBp, 1, Integer.MAX_VALUE);
    }

    public LDTestChecker(double minR, double alpha, int windowSizeBp) {
        this.minR = Assert.valueRange(minR, 0.5d, 1d);
        this.alpha = Assert.valueRange(alpha, 0.5d, 1d);
        this.windowSizeBp = Assert.valueRange(windowSizeBp, 1, Integer.MAX_VALUE);
    }

    public LDTestChecker(double alpha, int windowSizeBp, double maf) {
        this.alpha = Assert.valueRange(alpha, 0.5d, 1d);
        this.maf = Assert.valueRange(maf, 0d, 0.5d);
        this.windowSizeBp = Assert.valueRange(windowSizeBp, 1, Integer.MAX_VALUE);
    }

    public LDTestChecker(double minR, double alpha, int windowSizeBp, double maf) {
        this.minR = Assert.valueRange(minR, 0.5d, 1d);
        this.alpha = Assert.valueRange(alpha, 0.5d, 1d);
        this.maf = Assert.valueRange(maf, 0d, 0.5d);
        this.windowSizeBp = Assert.valueRange(windowSizeBp, 1, Integer.MAX_VALUE);
    }

    public LDTestChecker(double alpha, int windowSizeBp, double maf, ILDModel ldModel) {
        this.minR = Assert.valueRange(minR, 0.5d, 1d);
        this.alpha = Assert.valueRange(alpha, 0.5d, 1d);
        this.maf = Assert.valueRange(maf, 0d, 0.5d);
        this.windowSizeBp = Assert.valueRange(windowSizeBp, 1, Integer.MAX_VALUE);
        this.ldModel = ldModel;
    }

    public LDTestChecker(double minR, double alpha, int windowSizeBp, double maf, ILDModel ldModel) {
        this.minR = Assert.valueRange(minR, 0.5d, 1d);
        this.alpha = Assert.valueRange(alpha, 0.5d, 1d);
        this.maf = Assert.valueRange(maf, 0d, 0.5d);
        this.windowSizeBp = Assert.valueRange(windowSizeBp, 1, Integer.MAX_VALUE);
        this.ldModel = ldModel;
    }

    @Override
    public boolean isEqual(Variant variant1, Variant variant2, double AC11, double AC12, double AC21, double AC22, boolean reverse) throws IOException {
        // 断言，位置值一定是相同的
        assert variant1.chromosome.equals(variant2.chromosome);
        assert variant1.position == variant2.position;

        // 验证次级等位基因频率
        double mafMethod1 = (AC11 + AC12) * this.maf;
        double mafMethod2 = (AC21 + AC22) * this.maf;

        // 次级等位基因频率，该方法仅用于校正常见变异
        if (Math.min(AC11, AC12) <= mafMethod1 || Math.min(AC21, AC22) <= mafMethod2) {
            return false;
        }

        // 定位指针
        reader1.limit(variant1.chromosome);
        reader2.limit(variant2.chromosome);

        reader1.search(variant1.chromosome, variant1.position - this.windowSizeBp);
        reader2.search(variant2.chromosome, variant2.position - this.windowSizeBp);

        // 符号向位
        HashMap<Integer, Boolean> signs = new HashMap<>(16);

        // 计数
        int count = 0;
        int trueCount = 0;

        Variant[] readInvVariants;
        while ((readInvVariants = reader1.readVariants(position)) != null && Math.abs(readInvVariants[0].position - variant1.position) <= this.windowSizeBp) {
            if (readInvVariants[0].position != variant1.position) {
                double r = 0;

                for (Variant readInVariant : readInvVariants) {
                    double tempR = readInVariant.calculateLDR(this.ldModel, variant1);
                    if (Math.abs(tempR) > Math.abs(r)) {
                        r = tempR;
                    }
                }

                if (r != 0 && Math.abs(r) >= this.minR) {
                    signs.put(readInvVariants[0].position, r > 0);
                }
            }
        }

        if (signs.size() < 5) {
            // 没有近邻强相关位点
            return false;
        }

        Set<Integer> commonPositions = signs.keySet();
        while ((readInvVariants = reader2.readVariants(commonPositions)) != null && Math.abs(readInvVariants[0].position - variant2.position) <= this.windowSizeBp) {
            if (readInvVariants[0].position != variant2.position) {
                double r = 0;

                for (Variant readInVariant : readInvVariants) {
                    double tempR = readInVariant.calculateLDR(this.ldModel, variant2);
                    if (Math.abs(tempR) > Math.abs(r)) {
                        r = tempR;
                    }
                }

                // 如果符号也都是相同的，则认为可能是一致的!
                if (r != 0 && Math.abs(r) >= this.minR) {
                    if (reverse) {
                        if ((r < 0 && signs.get(readInvVariants[0].position)) || (r > 0 && !signs.get(readInvVariants[0].position))) {
                            trueCount += 1;
                        }
                    } else {
                        if ((r > 0 && signs.get(readInvVariants[0].position)) || (r < 0 && !signs.get(readInvVariants[0].position))) {
                            trueCount += 1;
                        }
                    }

                    count += 1;
                }
            }
        }

        return count >= 5 && trueCount >= count * this.alpha;
    }

    /**
     * 设置文件读取器，用于扫描上下游位点
     *
     * @param manager1 第一个文件读取器
     * @param manager2 第二个文件读取器
     */
    @Override
    public void setReader(GTBManager manager1, GTBManager manager2) throws IOException {
        GTBReader reader1 = new GTBReader(manager1);
        GTBReader reader2 = new GTBReader(manager2);
        if (this.reader1 != null) {
            this.reader1.close();
        }

        if (this.reader2 != null) {
            this.reader2.close();
        }

        this.reader1 = reader1;
        this.reader2 = reader2;
    }

    /**
     * 设置比对位置条件
     *
     * @param position 位置值
     */
    @Override
    public void setPosition(HashSet<Integer> position) {
        this.position = position;
    }

    @Override
    public void close() throws Exception {
        this.position.clear();
        this.position = null;
        if (this.reader1 != null) {
            this.reader1.close();
        }
        if (this.reader2 != null) {
            this.reader2.close();
        }
        this.reader1 = null;
        this.reader2 = null;
    }

    @Override
    public String toString() {
        if (this.maf == 0) {
            return "strongly correlated (|r| >= " + this.minR + ") in the upstream and downstream " + this.windowSizeBp + "bp, threshold = " + this.alpha;
        } else {
            return "MAF > " + this.maf + ", strongly correlated (|r| >= " + this.minR + ") in the upstream and downstream " + this.windowSizeBp + "bp, threshold = " + this.alpha;
        }
    }

    @Override
    public AlleleChecker clone() {
        return new LDTestChecker(minR, alpha, windowSizeBp, maf, ldModel);
    }
}
