package edu.sysu.pmglab.gbc.core.common.allelechecker;

import edu.sysu.pmglab.gbc.core.gtbcomponent.GTBManager;
import edu.sysu.pmglab.gbc.core.gtbcomponent.gtbreader.Variant;

import java.io.IOException;
import java.util.HashSet;

/**
 * @author suranyi
 * @description
 */

public interface AlleleChecker extends AutoCloseable, Cloneable {
    double DEFAULT_MAF = 0.05d;

    /**
     * 碱基是否可以认为相同
     */
    boolean isEqual(Variant variant1, Variant variant2, double AC11, double AC12, double AC21, double AC22, boolean reverse) throws IOException;

    /**
     * 设置文件读取器，用于扫描上下游位点
     *
     * @param manager1 第一个文件读取器
     * @param manager2 第二个文件读取器
     */
    void setReader(GTBManager manager1, GTBManager manager2) throws IOException;

    /**
     * 设置比对位置条件
     *
     * @param position 位置值
     */
    void setPosition(HashSet<Integer> position);

    /**
     * 克隆自身
     */
    AlleleChecker clone();
}
