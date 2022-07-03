package edu.sysu.pmglab.gbc.core.gtbcomponent.gtbreader.formatter;

import edu.sysu.pmglab.container.VolumeByteStream;
import edu.sysu.pmglab.gbc.core.gtbcomponent.gtbreader.Variant;

/**
 * @author suranyi
 */
public interface VariantFormatter<In, Out> {
    /**
     * 格式转换
     * @param variant 转换的位点
     * @return 转换结果
     */
    default Out apply(Variant variant) {
        throw new UnsupportedOperationException();
    }

    /**
     * 格式转换
     * @param variant 转换的位点
     * @param params 参数列表
     * @return 转换结果
     */
    default Out apply(Variant variant, In params) {
        throw new UnsupportedOperationException();
    }

    /**
     * 带有缓冲区的格式转换
     * @param variant 转换的位点
     * @param cache 缓冲区
     * @return 写入数据的长度
     */
    default int apply(Variant variant, VolumeByteStream cache) {
        throw new UnsupportedOperationException();
    }

    /**
     * 带有缓冲区的格式转换
     * @param variant 转换的位点
     * @param params 参数列表
     * @param cache 缓冲区
     * @return 写入数据的长度
     */
    default int apply(Variant variant, In params, VolumeByteStream cache) {
        throw new UnsupportedOperationException();
    }
}
