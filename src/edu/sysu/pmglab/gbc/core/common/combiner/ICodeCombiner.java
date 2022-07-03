package edu.sysu.pmglab.gbc.core.common.combiner;

import edu.sysu.pmglab.container.VolumeByteStream;
import edu.sysu.pmglab.gbc.core.common.switcher.AMDOFeature;
import edu.sysu.pmglab.gbc.core.gtbcomponent.gtbreader.Variant;

/**
 * @Data :2021/05/30
 * @Author :suranyi
 * @Contact :suranyi.sysu@gamil.com
 * @Description :编码组合器接口
 */

public interface ICodeCombiner {
    /**
     * 构造器，初始化编码组合器
     *
     * @param phased          是否有向
     * @param validSubjectNum 有效样本数
     * @return 根据是否组合编码信息获取对应的组合器
     */
    static ICodeCombiner getInstance(boolean phased, int validSubjectNum) {
        return phased ? new PhasedCodeCombiner(validSubjectNum) : new UnphasedCodeCombiner(validSubjectNum);
    }

    /**
     * 组合编码数据
     *
     * @param cache   写入容器
     * @return 组合编码序列的长度
     */
    void combine(Variant<AMDOFeature> variant, VolumeByteStream cache);
}