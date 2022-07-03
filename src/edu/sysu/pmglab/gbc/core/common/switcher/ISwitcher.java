package edu.sysu.pmglab.gbc.core.common.switcher;

import edu.sysu.pmglab.check.Value;
import edu.sysu.pmglab.easytools.ValueUtils;
import edu.sysu.pmglab.gbc.coder.encoder.BEGEncoder;
import edu.sysu.pmglab.gbc.core.gtbcomponent.gtbreader.Variant;

/**
 * @Data        :2021/03/26
 * @Author      :suranyi
 * @Contact     :suranyi.sysu@gamil.com
 * @Description :交换器基类
 */

public interface ISwitcher {
    boolean DEFAULT_ENABLE = true;
    int DEFAULT_SIZE = 24;
    int MIN = 1;
    int MAX = 2 << 16;

    /**
     * 构造器，初始化交换器
     * @param permutation 是否进行重排列
     * @return 根据是否重排列信息获取对应的交换器
     */
    static ISwitcher getInstance(boolean permutation) {
        return permutation ? AMDOSwitcher.INSTANCE : EmptySwitcher.INSTANCE;
    }

    /**
     * 交换，获得新的索引
     * @param encoder 编码器
     * @param variants 变异位点列表
     * @param variantsNum 该列表中有效数据的数量
     */
    void switchingRow(BEGEncoder encoder, Variant<AMDOFeature>[] variants, int variantsNum);

    /**
     * 获取真实的 windowSize 值
     * @param currentWindowSize 当前窗口值大小
     * @param validSubjectNum 有效样本个数
     * @return 经约束的真实窗口值
     */
    static int getRealWindowSize(int currentWindowSize, int validSubjectNum) {
        // 大于 46360 时，会发生数值越界，即导致得到的特征值是负数
        return Value.of(currentWindowSize, (validSubjectNum / 46360) + 1, ValueUtils.min(validSubjectNum, MAX));
    }

    /**
     * 获取特征向量的长度
     * @param realWindowSize 当前窗口值大小
     * @param validSubjectNum 有效样本个数
     * @return 特征向量长度
     */
    static int getFeatureLength(int validSubjectNum, int realWindowSize) {
        return (int) Math.ceil((float) validSubjectNum / realWindowSize);
    }
}
