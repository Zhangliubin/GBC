package edu.sysu.pmglab.gbc.core.gtbcomponent.gtbreader;

import edu.sysu.pmglab.container.array.StringArray;
import edu.sysu.pmglab.gbc.core.gtbcomponent.GTBManager;
import edu.sysu.pmglab.gbc.core.gtbcomponent.GTBNode;
import edu.sysu.pmglab.gbc.core.gtbcomponent.GTBNodes;

public class Pointer implements Cloneable {
    /**
     * chromosomeIndex = -1 代表达到文件末尾, 此时不会触发解压
     */
    int chromosomeIndex = -1;
    int nodeIndex = 0;
    int nodeLength = 0;
    int variantIndex = 0;
    int variantLength = 0;
    GTBNode node = null;

    GTBManager manager;
    StringArray chromosomeList;

    Pointer() {
    }

    public Pointer(GTBManager manager) {
        // 将 manager 中的空节点、空染色体剪切掉
        this.manager = manager;

        // 确保所有的染色体中至少有一条数据
        this.chromosomeList = StringArray.wrap(manager.getChromosomeList());

        // 初始化指针信息, 确保文件内有位点可读
        setChromosome(0);
    }

    public Pointer(GTBManager manager, String[] chromosomeList) {
        this.manager = manager;

        // 确保所有的染色体中至少有一条数据
        this.chromosomeList = (StringArray) StringArray.wrap(chromosomeList).filter(manager::contain);

        // 初始化指针信息, 确保文件内有位点可读
        setChromosome(0);
    }

    protected void setChromosome(int index) {
        if (index >= chromosomeList.size()) {
            // 定位到最末尾
            this.chromosomeIndex = -1;
            this.nodeLength = 0;
            this.nodeIndex = 0;
            this.node = null;
            this.variantLength = 0;
            this.variantIndex = 0;
        } else {
            if (index < 0) {
                index = 0;
            }

            // 在中间, 有效染色体编号
            if (index != this.chromosomeIndex) {
                this.chromosomeIndex = index;
                this.nodeLength = this.manager.getGTBNodes(this.chromosomeList.get(this.chromosomeIndex)).numOfNodes();
            }

            setNode(0);
        }
    }

    protected void setNode(int index) {
        if (index >= this.nodeLength) {
            // 定位到下一个染色体
            setChromosome(this.chromosomeIndex + 1);
        } else {
            if (index < 0) {
                index = 0;
            }

            // 在中间, 有效节点编号
            GTBNode node = this.manager.getGTBNodes(this.chromosomeList.get(this.chromosomeIndex)).get(index);
            this.nodeIndex = index;
            this.variantIndex = 0;
            this.variantLength = node.numOfVariants();
            this.node = node;
        }
    }

    protected void setVariant(int index) {
        if (index >= this.variantLength) {
            // 定位到下一个节点
            setNode(this.nodeIndex + 1);
        } else {
            if (index < 0) {
                index = 0;
            }

            this.variantIndex = index;
        }
    }

    /**
     * 按照标准索引值进行跳转
     *
     * @param chromosomeIndex 染色体索引
     * @param nodeIndex       节点索引
     * @param variantIndex    变异位点索引
     */
    public void seek(int chromosomeIndex, int nodeIndex, int variantIndex) {
        if (chromosomeList.size() != 0) {
            if (chromosomeIndex < 0) {
                chromosomeIndex = 0;
            }

            if (nodeIndex < 0) {
                nodeIndex = 0;
            }

            if (variantIndex < 0) {
                variantIndex = 0;
            }

            if (chromosomeIndex >= chromosomeList.size()) {
                // 定位到最末尾
                this.chromosomeIndex = -1;
                this.nodeLength = 0;
                this.nodeIndex = 0;
                this.node = null;
                this.variantLength = 0;
                this.variantIndex = 0;
            } else {
                this.chromosomeIndex = chromosomeIndex;

                // 在中间, 有效染色体编号
                GTBNodes nodes = this.manager.getGTBNodes(this.chromosomeList.get(this.chromosomeIndex));
                this.nodeLength = nodes.numOfNodes();

                if (nodeIndex >= this.nodeLength) {
                    setChromosome(chromosomeIndex + 1);
                } else {
                    // 在中间, 有效节点编号
                    this.nodeIndex = nodeIndex;
                    this.node = nodes.get(nodeIndex);
                    this.variantLength = this.node.numOfVariants();

                    if (variantIndex >= this.variantLength) {
                        setNode(nodeIndex + 1);
                    } else {
                        this.variantIndex = variantIndex;
                    }
                }
            }
        }
    }

    public void seek(int chromosomeIndex) {
        seek(chromosomeIndex, 0, 0);
    }

    public void seek(int chromosomeIndex, int nodeIndex) {
        seek(chromosomeIndex, nodeIndex, 0);
    }

    public GTBNode getNode() {
        return this.node;
    }

    /**
     * 指针跳转至下一个位点
     */
    public boolean next() {
        if (hasNext()) {
            this.variantIndex++;

            if (this.variantIndex >= this.variantLength) {
                // 该节点已经没有位点, 则跳转至下一个节点
                setNode(this.nodeIndex + 1);
            }

            return this.chromosomeIndex != -1;
        }

        return false;
    }

    public boolean nextNode() {
        if (hasNext()) {
            setNode(this.nodeIndex + 1);
            return this.chromosomeIndex != -1;
        }
        return false;
    }

    /**
     * 指针跳转至下一个位点
     */
    public boolean nextChromosome() {
        if (hasNext()) {
            seek(this.chromosomeIndex + 1);
            return this.chromosomeIndex != -1;
        }

        return false;
    }

    /**
     * 是否有后继位点
     */
    public boolean hasNext() {
        return chromosomeIndex != -1;
    }

    /**
     * 定位到最末尾
     */
    public void seekToEnd() {
        this.chromosomeIndex = -1;
        this.nodeLength = 0;
        this.nodeIndex = 0;
        this.node = null;
        this.variantLength = 0;
        this.variantIndex = 0;
    }

    /**
     * 任务的克隆方法
     *
     * @return 当前任务的拷贝
     */
    @Override
    public Pointer clone() {
        Pointer cloner = new Pointer();
        cloner.manager = this.manager;
        cloner.chromosomeList = this.chromosomeList;
        cloner.chromosomeIndex = this.chromosomeIndex;
        cloner.nodeIndex = this.nodeIndex;
        cloner.nodeLength = this.nodeLength;
        cloner.variantIndex = this.variantIndex;
        cloner.variantLength = this.variantLength;
        cloner.node = this.node;
        return cloner;
    }
}