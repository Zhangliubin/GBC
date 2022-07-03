package edu.sysu.pmglab.gbc.core.common.qualitycontrol.genotype;

import edu.sysu.pmglab.check.Assert;
import edu.sysu.pmglab.container.VolumeByteStream;
import edu.sysu.pmglab.container.array.Array;
import edu.sysu.pmglab.container.array.BaseArray;

import java.util.Iterator;
import java.util.Map;

/**
 * @Data :2021/06/15
 * @Author :suranyi
 * @Contact :suranyi.sysu@gamil.com
 * @Description :基因型水平控制器
 */

public class GenotypeQC implements Iterable<IGenotypeQC> {
    static final String DEFAULT_MARK = "GT";
    final Array<IGenotypeQC> controllers = new Array<>(2, true);

    public GenotypeQC() {
    }

    /**
     * 添加控制器
     *
     * @param controller 控制器
     */
    public void add(IGenotypeQC controller) {
        if (controller != null && !controller.empty()) {
            this.controllers.add(controller);
        }
    }

    /**
     * 移除控制器
     *
     * @param controller 控制器
     */
    public void remove(IGenotypeQC controller) {
        Assert.NotNull(controller);
        this.controllers.remove(controller);
    }

    /**
     * 移除控制器
     * @param tClass 控制器类型
     */
    public void remove(Class<?> tClass) {
        Assert.NotNull(tClass);

        for (IGenotypeQC controller: this) {
            if (controller.getClass().equals(tClass)) {
                controller.setEmpty();
            }
        }

        BaseArray<IGenotypeQC> newController = controllers.filter(controller -> !controller.empty());
        controllers.clear();
        controllers.addAll(newController);
    }

    /**
     * 获取所有的控制器
     */
    public IGenotypeQC[] getGenotypeQCs() {
        return this.controllers.toArray();
    }

    /**
     * 获取指定的控制器
     */
    public IGenotypeQC getGenotypeQC(int index) {
        return this.controllers.get(index);
    }

    /**
     * 控制器数量
     */
    public int size() {
        return this.controllers.size();
    }

    /**
     * 清除所有的控制器
     */
    public void clear() {
        this.controllers.clear();
    }

    /**
     * 根据传入的变异位点序列 FORMAT 信息加载格式匹配器
     *
     * @param variant 变异位点序列
     */
    public String[] load(VolumeByteStream variant, String format) {
        if (format.equals(DEFAULT_MARK)) {
            return null;
        }

        return format.split(":");
    }

    /**
     * 质控对外方法，false 代表保留，true 代表需要剔除
     *
     */
    public boolean filter(Map<String, String> individual) {
        for (int i = 0; i < controllers.size(); i++) {
            if (!this.controllers.get(i).filter(individual)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public String toString() {
        Iterator<IGenotypeQC> it = iterator();

        StringBuilder sb = new StringBuilder();
        while (true) {
            IGenotypeQC e = it.next();
            sb.append(e);
            if (!it.hasNext()) {
                return sb.toString();
            }
            sb.append(',').append(' ');
        }
    }

    @Override
    public Iterator<IGenotypeQC> iterator() {
        return this.controllers.iterator();
    }
}

