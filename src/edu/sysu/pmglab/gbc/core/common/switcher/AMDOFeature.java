package edu.sysu.pmglab.gbc.core.common.switcher;

import edu.sysu.pmglab.easytools.ValueUtils;
import edu.sysu.pmglab.gbc.coder.encoder.BEGEncoder;
import edu.sysu.pmglab.gbc.core.gtbcomponent.gtbreader.Variant;

/**
 * @author suranyi
 */

public class AMDOFeature {
    /**
     * 特征向量
     */
    public final int[] blockCounts;
    public final int featureLength;
    public int encoderIndex;

    public AMDOFeature(int windowSize, int distanceFeatureLength) {
        this.blockCounts = new int[windowSize];
        this.featureLength = distanceFeatureLength;
    }

    public AMDOFeature() {
        this.blockCounts = null;
        this.featureLength = -1;
    }

    /**
     * 设置特征向量
     */
    public void setFeatureVector(BEGEncoder encoder, Variant variant) {
        int upBound;

        for (int i = 0; i < this.blockCounts.length; i++) {
            this.blockCounts[i] = 0;
            upBound = ValueUtils.min(this.featureLength * (i + 1), variant.BEGs.length);
            for (int j = this.featureLength * i; j < upBound; j++) {
                this.blockCounts[i] += encoder.scoreOf(variant.BEGs[j] & 0xFF) * (upBound - j);
            }
        }
    }

    /**
     * 比对两个位点的顺序
     *
     * @param v1 第一个比对位点
     * @param v2 第二个比对位点
     */
    public static int comparatorBaseOnFeatureVector(Variant<AMDOFeature> v1, Variant<AMDOFeature> v2) {
        AMDOFeature feature1 = v1.property;
        AMDOFeature feature2 = v2.property;

        int status = Integer.compare(feature1.encoderIndex, feature2.encoderIndex);

        if (status == 0) {
            // 若相等
            if (feature1.encoderIndex == 0) {
                for (int i = 0; i < feature1.blockCounts.length; i++) {
                    if (feature1.blockCounts[i] < feature2.blockCounts[i]) {
                        return 1;
                    } else if (feature1.blockCounts[i] > feature2.blockCounts[i]) {
                        return -1;
                    }
                }
            } else {
                for (int i = 0; i < feature1.blockCounts.length; i++) {
                    if (feature1.blockCounts[i] < feature2.blockCounts[i]) {
                        return -1;
                    } else if (feature1.blockCounts[i] > feature2.blockCounts[i]) {
                        return 1;
                    }
                }
            }
        }

        return status;
    }

    /**
     * 比对两个位点的顺序
     *
     * @param v1 第一个比对位点
     * @param v2 第二个比对位点
     */
    public static int comparatorBaseOnEncoderIndex(Variant<AMDOFeature> v1, Variant<AMDOFeature> v2) {
        return Integer.compare(v1.property.encoderIndex, v2.property.encoderIndex);
    }
}
