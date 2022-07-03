package edu.sysu.pmglab.gbc.core.gtbcomponent.gtbreader;

import edu.sysu.pmglab.container.VolumeByteStream;
import edu.sysu.pmglab.easytools.ByteCode;
import edu.sysu.pmglab.easytools.ValueUtils;

/**
 * @Data :2021/03/10
 * @Author :suranyi
 * @Contact :suranyi.sysu@gamil.com
 * @Description :解压的位点任务
 */

class TaskVariant {
    public int index;
    public int position;
    public int decoderIndex;

    public VolumeByteStream REF = new VolumeByteStream(10);
    public VolumeByteStream ALT = new VolumeByteStream(10);

    /**
     * 设置该位点任务的 position
     */
    public TaskVariant setPosition(byte byte1, byte byte2, byte byte3, byte byte4) {
        this.position = ValueUtils.byteArray2IntegerValue(byte1, byte2, byte3, byte4);
        return this;
    }

    /**
     * 设置该位点的索引
     */
    public TaskVariant setIndex(int index) {
        this.index = index;
        return this;
    }

    /**
     * 设置该位点的索引
     */
    TaskVariant reload() {
        this.index = -1;
        return this;
    }

    /**
     * 设置该位点的索引
     */
    public TaskVariant setDecoderIndex(int index) {
        this.decoderIndex = index;
        return this;
    }

    /**
     * 两个位点任务的比对
     */
    public static int compareVariant(TaskVariant v1, TaskVariant v2) {
        int status = Integer.compare(v1.position, v2.position);
        if (status == 0) {
            status = Integer.compare(v1.getAlternativeAlleleNum(), v2.getAlternativeAlleleNum());
            if (status == 0) {
                for (int i = 0, l = Math.min(v1.REF.size(), v2.REF.size()); i < l; i++) {
                    status = Byte.compare(v1.REF.cacheOf(i), v2.REF.cacheOf(i));
                    if (status != 0) {
                        return status;
                    }
                }

                if (v1.REF.size() < v2.REF.size()) {
                    return -1;
                } else if (v1.REF.size() > v2.REF.size()) {
                    return 1;
                } else {
                    for (int i = 0, l = Math.min(v1.ALT.size(), v2.ALT.size()); i < l; i++) {
                        status = Byte.compare(v1.ALT.cacheOf(i), v2.ALT.cacheOf(i));
                        if (status != 0) {
                            return status;
                        }
                    }

                    status = Integer.compare(v1.ALT.size(), v2.ALT.size());
                    return status;
                }
            }
        }
        return status;
    }

    private int getAlternativeAlleleNum() {
        int count = 0;
        for (byte b : this.ALT) {
            if (b == ByteCode.COMMA) {
                count++;
            }
        }
        return count + 2;
    }
}
