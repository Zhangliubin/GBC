package edu.sysu.pmglab.gbc.core.common.combiner;

import edu.sysu.pmglab.container.VolumeByteStream;
import edu.sysu.pmglab.gbc.coder.encoder.MBEGEncoder;
import edu.sysu.pmglab.gbc.core.common.switcher.AMDOFeature;
import edu.sysu.pmglab.gbc.core.gtbcomponent.gtbreader.Variant;

/**
 * @Data :2021/05/30
 * @Author :suranyi
 * @Contact :suranyi.sysu@gamil.com
 * @Description :无向基因型编码组合器
 */

public class UnphasedCodeCombiner implements ICodeCombiner {
    int validSubjectNum;
    int groupNum;
    int resNum;
    MBEGEncoder encoder = MBEGEncoder.getEncoder(false);

    UnphasedCodeCombiner(int validSubjectNum) {
        this.validSubjectNum = validSubjectNum;
        this.groupNum = validSubjectNum / 4;
        this.resNum = validSubjectNum % 4;
    }

    @Override
    public void combine(Variant<AMDOFeature> variant, VolumeByteStream cache) {
        /* 拼接并压缩基因型数据 */
        if (variant.property.encoderIndex == 0) {
            for (int j = 0; j < groupNum; j++) {
                cache.write(encoder.encode(variant.BEGs[j * 4],
                        variant.BEGs[j * 4 + 1],
                        variant.BEGs[j * 4 + 2],
                        variant.BEGs[j * 4 + 3]));
            }

            if (resNum == 1) {
                cache.write(encoder.encode(variant.BEGs[validSubjectNum - 1]));
            } else if (resNum == 2) {
                cache.write(encoder.encode(variant.BEGs[validSubjectNum - 2], variant.BEGs[validSubjectNum - 1]));
            } else if (resNum == 3) {
                cache.write(encoder.encode(variant.BEGs[validSubjectNum - 3], variant.BEGs[validSubjectNum - 2], variant.BEGs[validSubjectNum - 1]));
            }
        } else {
            cache.write(variant.BEGs, 0, validSubjectNum);
        }
    }
}