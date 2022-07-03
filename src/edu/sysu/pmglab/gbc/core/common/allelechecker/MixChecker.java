package edu.sysu.pmglab.gbc.core.common.allelechecker;

import edu.sysu.pmglab.gbc.core.gtbcomponent.GTBManager;
import edu.sysu.pmglab.gbc.core.gtbcomponent.gtbreader.Variant;

import java.io.IOException;
import java.util.HashSet;


/**
 * @author suranyi
 * @description 检验 LD 系数
 */

public class MixChecker implements AlleleChecker {
    /**
     * common Variant Checker
     */
    AlleleChecker[] checkers;

    public MixChecker(AlleleChecker... checkers) {
        this.checkers = checkers;
    }

    @Override
    public boolean isEqual(Variant variant1, Variant variant2, double AC11, double AC12, double AC21, double AC22, boolean reverse) throws IOException {
        // 断言，位置值一定是相同的
        for (AlleleChecker checker : checkers) {
            if (checker.isEqual(variant1, variant2, AC11, AC12, AC21, AC22, reverse)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void setReader(GTBManager manager1, GTBManager manager2) throws IOException {
        for (AlleleChecker checker : checkers) {
            checker.setReader(manager1, manager2);
        }
    }

    /**
     * 设置比对位置条件
     *
     * @param position 位置值
     */
    @Override
    public void setPosition(HashSet<Integer> position) {
        for (AlleleChecker checker : checkers) {
            checker.setPosition(position);
        }
    }


    @Override
    public String toString() {
        if (this.checkers == null || this.checkers.length == 0) {
            return "";
        } else {
            int cacheSize = checkers.length;
            StringBuilder b = new StringBuilder();
            int i;
            for (i = 0; i < cacheSize; ++i) {
                b.append(this.checkers[i]);
                if (i != cacheSize - 1) {
                    b.append("; ");
                }
            }

            return b.toString();
        }
    }

    @Override
    public void close() throws Exception {
        for (AlleleChecker checker : this.checkers) {
            checker.close();
        }
    }

    @Override
    public AlleleChecker clone() {
        AlleleChecker[] checkers = new AlleleChecker[this.checkers.length];
        for (int i = 0; i < checkers.length; i++) {
            checkers[i] = this.checkers[i].clone();
        }
        return new MixChecker(checkers);
    }
}
