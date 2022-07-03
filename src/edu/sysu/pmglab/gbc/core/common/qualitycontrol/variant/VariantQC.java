package edu.sysu.pmglab.gbc.core.common.qualitycontrol.variant;

import edu.sysu.pmglab.check.Assert;
import edu.sysu.pmglab.container.array.Array;
import edu.sysu.pmglab.container.array.BaseArray;
import edu.sysu.pmglab.gbc.core.gtbcomponent.gtbreader.Variant;

import java.util.Iterator;

/**
 * @Data :2021/06/08
 * @Author :suranyi
 * @Contact :suranyi.sysu@gamil.com
 * @Description :群体等位基因水平控制器
 */

public class VariantQC implements Iterable<IVariantQC> {
    final Array<IVariantQC> controllers = new Array<>(3, true);

    public VariantQC() {

    }

    /**
     * 添加控制器
     *
     * @param controller 控制器
     */
    public void add(IVariantQC controller) {
        if (controller != null && !controller.empty()) {
            this.controllers.add(controller);
        }
    }

    /**
     * 移除控制器
     *
     * @param controller 控制器
     */
    public void remove(IVariantQC controller) {
        Assert.NotNull(controller);

        this.controllers.remove(controller);
    }

    /**
     * 移除控制器
     *
     * @param tClass 控制器类型
     */
    public void remove(Class<?> tClass) {
        Assert.NotNull(tClass);

        for (IVariantQC controller : this) {
            if (controller.getClass().equals(tClass)) {
                controller.setEmpty();
            }
        }

        BaseArray<IVariantQC> newController = controllers.filter(controller -> !controller.empty());
        controllers.clear();
        controllers.addAll(newController);
    }

    /**
     * 获取所有的控制器
     */
    public IVariantQC[] getGenotypeQCs() {
        return this.controllers.toArray();
    }

    /**
     * 获取指定的控制器
     */
    public IVariantQC getGenotypeQC(int index) {
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

    @Override
    public String toString() {
        Iterator<IVariantQC> it = iterator();

        StringBuilder sb = new StringBuilder();
        while (true) {
            IVariantQC e = it.next();
            sb.append(e);
            if (!it.hasNext()) {
                return sb.toString();
            }
            sb.append(',').append(' ');
        }
    }

    /**
     * 对外执行群体等位基因计数水平过滤
     *
     * @param variant 等位基因计数
     * @return 是否保留该位点 (true 代表保留)
     */
    public boolean filter(Variant variant) {
        for (IVariantQC filter : this.controllers) {
            if (!filter.filter(variant)) {
                return false;
            }
        }

        return true;
    }

    /**
     * 对外执行位点信息过滤
     *
     * @param marker 位点非基因型数据标记
     * @return 是否保留该位点 (true 代表保留)
     */
    public boolean filter(VCFNonGenotypeMarker marker) {
        for (IVariantQC filter : this.controllers) {
            if (!filter.filter(marker)) {
                return false;
            }
        }

        return true;
    }

    /**
     * 对外执行群体等位基因水平过滤
     *
     * @param alleleCounts    等位基因计数
     * @param validAllelesNum 有效等位基因数
     * @return 是否保留该位点 (true 代表保留)
     */
    public boolean filter(int alleleCounts, int validAllelesNum) {
        for (IVariantQC filter : this.controllers) {
            if (!filter.filter(alleleCounts, validAllelesNum)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public Iterator<IVariantQC> iterator() {
        return this.controllers.iterator();
    }
}