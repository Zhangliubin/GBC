package edu.sysu.pmglab.gbc.core.gtbcomponent;

import edu.sysu.pmglab.container.array.Array;
import edu.sysu.pmglab.container.array.BaseArray;
import edu.sysu.pmglab.easytools.ValueUtils;
import edu.sysu.pmglab.gbc.core.exception.GTBComponentException;

import java.util.Iterator;
import java.util.Objects;

/**
 * @Data :2020/04/20
 * @Author :suranyi
 * @Contact :suranyi.sysu@gamil.com
 * @Description :相同染色体编号下的 GTB 节点集群
 */

public class GTBNodes implements Iterable<GTBNode>, Cloneable {
    public final String chromosome;
    private BaseArray<GTBNode> nodes;

    /**
     * 构造器方法，该方法是不安全的，外部需要保证 nodes 的染色体编号都是一致的，同时该对象不应被外部修改
     *
     * @param nodes 指定节点群设置节点集群
     */
    GTBNodes(GTBNode[] nodes) {
        if (nodes == null || nodes.length == 0) {
            throw new GTBComponentException("syntax error: set GTBNode[] to null");
        }

        this.chromosome = nodes[0].chromosome;
        this.nodes = new Array<>(GTBNode[].class, true);
        this.nodes.addAll(nodes);
    }

    /**
     * 构造器方法
     *
     * @param chromosome 当前节点群的染色体
     */
    public GTBNodes(String chromosome) {
        this(chromosome, 1024);
    }

    /**
     * 构造器方法
     *
     * @param chromosome 当前节点群的染色体
     * @param capacity   初始容量大小
     */
    public GTBNodes(String chromosome, int capacity) {
        if (chromosome == null) {
            throw new GTBComponentException("syntax error: set chromosome to null");
        }

        this.chromosome = chromosome;
        this.nodes = new Array<>(GTBNode[].class, capacity, true);
    }

    /**
     * 绑定指定的根文件，由 GTBTree 调用
     *
     * @param rootIndex 根文件编号
     */
    public void bind(int rootIndex) {
        for (GTBNode node : this.nodes) {
            if (node != null) {
                node.bind(rootIndex);
            }
        }
    }

    /**
     * 添加 GTB 节点，需要检查缓冲池大小
     *
     * @param node 节点头信息
     */
    public void add(GTBNode node) {
        if (nodes == null) {
            throw new GTBComponentException("syntax error: add(null)");
        }

        checkChromosome(node.chromosome);
        this.nodes.add(node);
    }

    /**
     * 批量追加根结点信息，需要检查缓冲池大小
     *
     * @param otherNodes 另一个 GTB 节点二级路径
     */
    public void add(GTBNodes otherNodes) {
        if (otherNodes == null) {
            throw new GTBComponentException("syntax error: add(null)");
        }

        checkChromosome(otherNodes.chromosome);
        this.nodes.addAll(otherNodes.nodes);
    }

    /**
     * 将当前的染色体分支切换为另一个分支 (一般用于替换使用)
     *
     * @param otherNodes 一个完整的 GTBNodes 节点 （即另一个文件的完整染色体分支）
     */
    public void set(GTBNodes otherNodes) {
        if (otherNodes == null) {
            throw new GTBComponentException("syntax error: set(null)");
        }

        checkChromosome(otherNodes.chromosome);
        this.nodes = otherNodes.nodes;
    }

    /**
     * 尝试获得索引为 index 的 GTB 节点
     *
     * @param nodeIndex 节点索引
     */
    public GTBNode get(int nodeIndex) {
        return this.nodes.get(nodeIndex);
    }

    /**
     * 尝试获得索引为 index 的 GTB 节点
     *
     * @param nodeIndexes 节点索引
     */
    public GTBNode[] get(int... nodeIndexes) {
        return (GTBNode[]) this.nodes.get(nodeIndexes);
    }

    /**
     * 批量移除 GTB 节点
     *
     * @param nodeIndexes 节点的索引数组
     */
    public void remove(int... nodeIndexes) {
        // 再执行批量删除
        this.nodes.removeByIndexes(nodeIndexes);
    }

    /**
     * 保留指定的 GTB 节点
     *
     * @param nodeIndexes 节点的索引数组
     */
    public void retain(int... nodeIndexes) {
        this.nodes = new Array<>(get(nodeIndexes), true);
        flush(true);
    }

    /**
     * 移除所有的节点
     */
    public void removeAll() {
        this.nodes.clear();
    }

    /**
     * 写入缓冲数据
     */
    public void flush() {
        this.nodes.filter(gtbNode -> gtbNode != null && gtbNode.numOfVariants() != 0);
        this.nodes.sort(GTBNode::compare);
    }

    /**
     * 写入缓冲数据
     */
    public void flush(boolean dropDuplicates) {
        // 标记真正的数据容量
        if (dropDuplicates) {
            // 将相等的节点剔除
            this.nodes.dropDuplicated();
        }
        flush();
    }

    /**
     * 当前的节点数量
     */
    public int numOfNodes() {
        return this.nodes.size();
    }

    /**
     * 获取块变异位点总数
     */
    public int numOfVariants() {
        int chromosomeVariantsNum = 0;
        for (GTBNode node : this) {
            chromosomeVariantsNum += node.numOfVariants();
        }
        return chromosomeVariantsNum;
    }

    /**
     * 获取块数据段总大小（数据长度），预估值
     */
    public int sizeOfDecompressedNodes(int validSubjectNum) {
        int maxOriginBlockSize = 0;
        int currentNodeEstimateSize;
        for (GTBNode node : this) {
            currentNodeEstimateSize = node.getEstimateDecompressedSize(validSubjectNum);
            if (currentNodeEstimateSize > maxOriginBlockSize) {
                maxOriginBlockSize = currentNodeEstimateSize;

                if (maxOriginBlockSize == Integer.MAX_VALUE - 2) {
                    break;
                }
            }
        }
        return maxOriginBlockSize;
    }

    /**
     * 查看该染色体节点树是否包含指定的节点
     *
     * @param node 待比对的节点
     */
    public boolean intersectPos(GTBNode node) {
        return node.chromosome.equals(this.chromosome) && ValueUtils.intersect(node.minPos, node.maxPos, get(0).minPos, get(-1).maxPos);
    }

    /**
     * 查看该染色体节点树是否有可能包含有序队列 positions 的数据
     *
     * @param orderedPositions 有序的任务队列
     */
    public boolean intersectPos(int[] orderedPositions) {
        if (orderedPositions.length == 0) {
            return false;
        }
        return intersectPos(orderedPositions[0], orderedPositions[orderedPositions.length - 1]);
    }

    /**
     * 查看该染色体节点树是否有可能包含 [start, end] 之间的节点
     *
     * @param start 范围的开始
     * @param end   范围的结束
     */
    public boolean intersectPos(int start, int end) {
        return ValueUtils.intersect(start, end, get(0).minPos, get(-1).maxPos);
    }

    /**
     * 查找包含 pos 的块索引
     */
    public int find(int pos) {
        int index = 0;

        for (GTBNode node : this) {
            if (node.contain(pos)) {
                return index;
            }

            index++;
        }
        return -1;
    }

    /**
     * 检查该文件是否为有序文件 (可快速随机访问)
     */
    public boolean checkOrdered() {
        for (int i = 0; i < this.nodes.size() - 1; i++) {
            if (get(i).maxPos > get(i + 1).minPos) {
                return false;
            }
        }

        return true;
    }

    GTBNodes resetChromosome(String newChromosomeIndex) {
        GTBNodes nodes = new GTBNodes(newChromosomeIndex);
        for (GTBNode node : this) {
            nodes.add(node.resetChromosome(newChromosomeIndex));
        }
        return nodes;
    }

    /**
     * 检查待合并的染色体节点与本节点编号是否一致，不一致则抛出错误
     *
     * @param chromosome 染色体编号
     */
    public void checkChromosome(String chromosome) {
        if (!Objects.equals(this.chromosome, chromosome)) {
            throw new GTBComponentException("current GTB node's chromosome is different from target's chromosome");
        }
    }

    @Override
    public Iterator<GTBNode> iterator() {
        return new Iterator<GTBNode>() {
            private int pointer;

            @Override
            public boolean hasNext() {
                return pointer < nodes.size();
            }

            @Override
            public GTBNode next() {
                return nodes.get(pointer++);
            }
        };
    }

    @Override
    public String toString() {
        StringBuilder out = new StringBuilder(2 << 15);

        // 写入根节点信息
        out.append(getRootInfo());

        if (this.nodes.size() > 0) {
            // 写入子块信息
            String[] nodeInfos = getNodesInfo();
            for (int i = 0; i < this.nodes.size() - 1; i++) {
                out.append(String.format("\n\t ├─ Node %d: %s", i + 1, nodeInfos[i]));
            }

            // 写入最后一个子块信息
            out.append(String.format("\n\t └─ Node %d: %s", this.nodes.size(), nodeInfos[this.nodes.size() - 1]));
        }

        return out.toString();
    }

    /**
     * 获取该染色体节点的汇总信息，汇总信息只有当子节点数量 > 0 时才有意义
     */
    public String getRootInfo() {
        int minPos = Integer.MAX_VALUE;
        int maxPos = Integer.MIN_VALUE;
        for (GTBNode node: this) {
            if (node.minPos < minPos) {
                minPos = node.minPos;
            }

            if (node.maxPos > maxPos) {
                maxPos = node.maxPos;
            }
        }

        if (this.nodes.size() > 0) {
            return String.format("Chromosome %s: posRange=[%d, %d], numOfNodes=%d, numOfVariants=%d",
                    chromosome, minPos, maxPos, this.nodes.size(), numOfVariants());
        } else {
            return String.format("Chromosome %s: No. of nodes=0, No. of variants=0", chromosome);
        }
    }

    /**
     * 获取该染色体节点下的所有子块节点的信息
     */
    public String[] getNodesInfo() {
        String[] nodeInfos = new String[this.nodes.size()];
        int index = 0;
        for (GTBNode node : this) {
            nodeInfos[index++] = node.toString();
        }

        return nodeInfos;
    }

    @Override
    public GTBNodes clone() {
        BaseArray<GTBNode> nodes = new Array<>(GTBNode[].class, true);
        for (GTBNode node : this) {
            nodes.add(node.clone());
        }
        return new GTBNodes(nodes.toArray(new GTBNode[0]));
    }
}
