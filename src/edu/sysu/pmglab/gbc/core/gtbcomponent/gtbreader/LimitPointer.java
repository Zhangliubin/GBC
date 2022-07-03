package edu.sysu.pmglab.gbc.core.gtbcomponent.gtbreader;

import edu.sysu.pmglab.container.array.StringArray;
import edu.sysu.pmglab.easytools.ArrayUtils;
import edu.sysu.pmglab.gbc.core.gtbcomponent.GTBManager;
import edu.sysu.pmglab.gbc.core.gtbcomponent.GTBNode;
import edu.sysu.pmglab.gbc.core.gtbcomponent.GTBNodes;

/**
 * @Data :2021/08/15
 * @Author :suranyi
 * @Contact :suranyi.sysu@gamil.com
 * @Description :
 */

public class LimitPointer extends Pointer {
    /**
     * 边界指针，左闭右开
     */
    int startNodeIndex;
    int endNodeIndex;

    LimitPointer() {
    }

    /**
     * @param manager        管理器
     * @param chromosomeList 限定染色体列表
     * @param startNodeIndex 第一个染色体的起点节点
     * @param endNodeIndex   最后一个染色体的终止节点 (不包含)
     */
    public LimitPointer(GTBManager manager, String[] chromosomeList, int startNodeIndex, int endNodeIndex) {
        this.manager = manager;

        if (startNodeIndex < 0) {
            startNodeIndex = 0;
        }

        if (endNodeIndex < 0) {
            endNodeIndex = 0;
        }

        if (chromosomeList.length == 0) {

        } else if (chromosomeList.length == 1) {
            // 最常见的索引情况
            if (manager.contain(chromosomeList[0])) {
                int numOfNodes = manager.getGTBNodes(chromosomeList[0]).numOfNodes();
                if (endNodeIndex - startNodeIndex <= 0 || startNodeIndex >= numOfNodes) {
                    // 不含任何节点
                    chromosomeList = new String[0];
                    this.startNodeIndex = 0;
                    this.endNodeIndex = 0;
                } else {
                    // 含有至少一个节点
                    if (endNodeIndex > numOfNodes) {
                        endNodeIndex = numOfNodes;
                    }

                    this.startNodeIndex = startNodeIndex;
                    this.endNodeIndex = endNodeIndex;
                }
            }
            // 不包含时不用管, 后面会被去掉
        } else {
            if (manager.contain(chromosomeList[0])) {
                int numOfNodes = manager.getGTBNodes(chromosomeList[0]).numOfNodes();
                if (startNodeIndex >= numOfNodes) {
                    // 不含第一个染色体的任意节点
                    chromosomeList = ArrayUtils.copyOfRange(chromosomeList, 1, chromosomeList.length);
                    this.startNodeIndex = 0;
                } else {
                    this.startNodeIndex = startNodeIndex;
                }
            } else {
                chromosomeList = ArrayUtils.copyOfRange(chromosomeList, 1, chromosomeList.length);
                this.startNodeIndex = 0;
            }

            if (manager.contain(chromosomeList[chromosomeList.length - 1])) {
                int numOfNodes = manager.getGTBNodes(chromosomeList[chromosomeList.length - 1]).numOfNodes();
                if (endNodeIndex > numOfNodes) {
                    // 包含最后一个染色体的所有节点
                    this.endNodeIndex = numOfNodes;
                } else if (endNodeIndex == 0) {
                    // 不含最后一个染色体的任意节点
                    chromosomeList = ArrayUtils.copyOfRange(chromosomeList, 0, chromosomeList.length - 1);
                    this.endNodeIndex = manager.getGTBNodes(chromosomeList[chromosomeList.length - 1]).numOfNodes();
                } else {
                    this.endNodeIndex = endNodeIndex;
                }
            } else {
                chromosomeList = ArrayUtils.copyOfRange(chromosomeList, 0, chromosomeList.length - 1);
                this.endNodeIndex = manager.getGTBNodes(chromosomeList[chromosomeList.length - 1]).numOfNodes();
            }
        }

        this.chromosomeList = (StringArray) StringArray.wrap(chromosomeList).filter(manager::contain);

        // 初始化指针信息, 确保文件内有位点可读
        setChromosome(0);
    }

    @Override
    protected void setNode(int index) {
        // 第一个染色体节点受前端约束
        if (this.chromosomeIndex == 0 && index < this.startNodeIndex) {
            index = this.startNodeIndex;
        }

        // 最后一个染色体受后端约束
        if (this.chromosomeIndex == this.chromosomeList.size() - 1 && index >= this.endNodeIndex) {
            // 定位到最末尾
            this.chromosomeIndex = -1;
            this.nodeLength = 0;
            this.nodeIndex = 0;
            this.node = null;
            this.variantLength = 0;
            this.variantIndex = 0;
            return;
        }

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

    /**
     * 按照标准索引值进行跳转
     *
     * @param chromosomeIndex 染色体索引
     * @param nodeIndex       节点索引
     * @param variantIndex    变异位点索引
     */
    @Override
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

            if (chromosomeIndex == 0 && nodeIndex < this.startNodeIndex) {
                setChromosome(0);
                return;
            }

            if (chromosomeIndex >= chromosomeList.size() || (chromosomeIndex == this.chromosomeList.size() - 1 && nodeIndex >= this.endNodeIndex)) {
                // 定位到最末尾
                this.chromosomeIndex = -1;
                this.nodeLength = 0;
                this.nodeIndex = 0;
                this.node = null;
                this.variantLength = 0;
                this.variantIndex = 0;
                return;
            }

            this.chromosomeIndex = chromosomeIndex;
            // 在中间, 有效染色体编号
            GTBNodes nodes = this.manager.getGTBNodes(this.chromosomeList.get(this.chromosomeIndex));
            this.nodeLength = nodes.numOfNodes();

            if (nodeIndex >= this.nodeLength) {
                setChromosome(this.chromosomeIndex + 1);
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

    public int getStartNodeIndex() {
        return startNodeIndex;
    }

    public int getEndNodeIndex() {
        return endNodeIndex;
    }

    @Override
    public LimitPointer clone() {
        LimitPointer cloner = new LimitPointer();
        cloner.manager = this.manager;
        cloner.chromosomeList = this.chromosomeList;
        cloner.chromosomeIndex = this.chromosomeIndex;
        cloner.nodeIndex = this.nodeIndex;
        cloner.nodeLength = this.nodeLength;
        cloner.variantIndex = this.variantIndex;
        cloner.variantLength = this.variantLength;
        cloner.node = this.node;
        cloner.startNodeIndex = this.startNodeIndex;
        cloner.endNodeIndex = this.endNodeIndex;
        return cloner;
    }
}