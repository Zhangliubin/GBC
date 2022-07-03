package edu.sysu.pmglab.gbc.core.calculation.ld;

import edu.sysu.pmglab.container.VolumeByteStream;
import edu.sysu.pmglab.gbc.core.gtbcomponent.gtbreader.Variant;

/**
 * @author suranyi
 * @description
 */

interface ILDContext {

    /**
     * 计算两位点的 LD 系数
     *
     * @param vbs      输出缓冲器
     * @param variant1 位点 1
     * @param variant2 位点 2
     * @param minR2    R^2 阈值
     * @return 缓冲区数据段长度
     */
    int calculateLDR2(VolumeByteStream vbs, Variant variant1, Variant variant2, double minR2);


    /**
     * 获取该 LD 模型的标题行
     *
     * @return 输出标题行信息
     */
    String getHeader();

    /**
     * 格式化输出信息
     *
     * @param vbs            输出缓冲器
     * @param chromosome     染色体编号
     * @param pos1           位置1
     * @param pos2           位置2
     * @param validSampleNum 有效样本数
     * @param score          分数值
     */
    void formatterOut(VolumeByteStream vbs, String chromosome, int pos1, int pos2, int validSampleNum, float... score);

    /**
     * 获取位编码
     *
     * @param BEGCode 转换的 BEG 编码值
     * @return 转换的位编码值，便于计算
     */
    default byte getBitCode(int BEGCode) {
        throw new UnsupportedOperationException("unsupported");
    }

    /**
     * 获取文件扩展名
     *
     * @return 默认的文件扩展名
     */
    default String getExtension() {
        return ".ld";
    }

    /**
     * 获取指定 beg 编码长度下，可以组合的个数
     *
     * @param BEGSize beg 编码长度 (样本总长度)
     * @return 有效组合个数
     */
    int getGroupNum(int BEGSize);

    /**
     * 获取指定 beg 编码长度下，最后一个编码组的样本个数
     *
     * @param BEGSize beg 编码长度 (样本总长度)
     * @return 最后一组的个数 (0 代表最后一个是完整的编码组)
     */
    int getResNum(int BEGSize);

    /**
     * 获取位点属性
     *
     * @param groupNum 成组的个数
     * @param resNum   余数个数
     * @return 位点属性
     */
    IVariantProperty getProperty(int groupNum, int resNum);

    /**
     * 格式化位点属性
     *
     * @param validSampleNum 有效样本个数
     * @return 位点属性
     */
    default IVariantProperty getProperty(int validSampleNum) {
        return getProperty(getGroupNum(validSampleNum), getResNum(validSampleNum));
    }

}
