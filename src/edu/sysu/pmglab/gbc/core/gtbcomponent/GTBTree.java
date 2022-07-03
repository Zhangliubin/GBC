package edu.sysu.pmglab.gbc.core.gtbcomponent;

import edu.sysu.pmglab.container.File;
import edu.sysu.pmglab.container.VolumeByteStream;
import edu.sysu.pmglab.container.array.Array;
import edu.sysu.pmglab.container.array.BaseArray;
import edu.sysu.pmglab.easytools.ArrayUtils;
import edu.sysu.pmglab.easytools.ByteCode;
import edu.sysu.pmglab.easytools.ValueUtils;
import edu.sysu.pmglab.gbc.constant.ChromosomeTags;
import edu.sysu.pmglab.gbc.core.exception.GTBComponentException;
import edu.sysu.pmglab.unifyIO.FileStream;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @Data :2021/03/04
 * @Author :suranyi
 * @Contact :suranyi.sysu@gamil.com
 * @Description :GTB 树
 */

public class GTBTree implements Iterable<GTBNodes>, Cloneable {
    private final HashMap<String, GTBNodes> rootNodes;
    private int rootIndex;

    /**
     * 构造器方法
     */
    public GTBTree() {
        this(ChromosomeTags.supportedChromosomeList().length);
    }

    /**
     * 构造器方法
     *
     * @param size 树初始尺寸，默认可容纳所有类型的染色体类型
     */
    public GTBTree(int size) {
        this.rootNodes = new HashMap<>(size);
    }

    /**
     * 构造器方法
     */
    public GTBTree(BaseArray<GTBNode> gtbNodes) {
        this(24);

        for (GTBNode node : gtbNodes) {
            add(node);
        }

        flush();
    }

    /**
     * 绑定节点树归属的根文件
     *
     * @param rootIndex 根文件编号
     */
    public void bind(int rootIndex) {
        this.rootIndex = rootIndex;
        for (GTBNodes nodes : this) {
            nodes.bind(rootIndex);
        }
    }

    /**
     * 获取节点树归属的根文件
     */
    public int getRootIndex() {
        return this.rootIndex;
    }

    /**
     * 清除节点数据
     */
    public void clear() {
        this.rootNodes.clear();
    }

    /**
     * 查找包含 pos 的块索引
     */
    public int find(String chromosome, int position) {
        if (!this.rootNodes.containsKey(chromosome)) {
            return -1;
        }

        return this.rootNodes.get(chromosome).find(position);
    }

    /**
     * 从 rootNodes 中获取对应染色体的 GTBNodes
     *
     * @param chromosome 染色体字符串信息
     */
    public GTBNodes get(String chromosome) {
        return this.rootNodes.get(chromosome);
    }

    /**
     * 从 rootNodes 中获取对应染色体的 GTBNodes
     *
     * @param chromosomes 染色体字符串信息
     */
    public GTBNodes[] get(String... chromosomes) {
        GTBNodes[] nodes = new GTBNodes[chromosomes.length];
        int index = 0;
        for (String chromosome : chromosomes) {
            nodes[index++] = get(chromosome);
        }
        return nodes;
    }

    /**
     * 重设重叠群信息
     *
     * @param resourceFile 资源文件
     */
    public void resetContig(File resourceFile) throws IOException {
        // 资源文件名相同时不进行转换
        if (resourceFile != null && !(ChromosomeTags.getActiveFile().equals(resourceFile))) {
            // 打开文件资源
            FileStream fileStream = resourceFile.open(FileStream.DEFAULT_READER);

            // 新节点信息表
            HashMap<String, GTBNodes> newRootNodes = new HashMap<>(this.rootNodes.size());

            VolumeByteStream lineCache = new VolumeByteStream(128);
            fileStream.readLine(lineCache);

            // 解析注释行
            while ((lineCache.cacheOf(0) == ByteCode.NUMBER_SIGN) && (lineCache.cacheOf(1) == ByteCode.NUMBER_SIGN)) {
                lineCache.reset();
                fileStream.readLine(lineCache);
            }

            // 解析正文字段
            String[] fields = new String(lineCache.cacheOf(1, lineCache.size())).split(",");
            Array<String> newChromosomesOrder = new Array<>();
            int chromosomeIndex = ArrayUtils.indexOf(fields, "chromosome");

            if (chromosomeIndex == -1) {
                throw new GTBComponentException("doesn't match to standard Chromosome Config file");
            }

            int count = 0;
            lineCache.reset();
            while (fileStream.readLine(lineCache) != -1) {
                if (count > 256) {
                    throw new GTBComponentException("too much chromosome input (> 256)");
                }

                String[] groups = new String(lineCache.getCache(), 0, lineCache.size()).split(",");
                newChromosomesOrder.add(groups[chromosomeIndex]);
                count++;
                lineCache.reset();
            }

            fileStream.close();

            // order <-> order
            for (String chromosome : this.rootNodes.keySet()) {
                // 获取该染色体在原 contig 文件中的顺序
                int oldIndex = ChromosomeTags.getIndex(chromosome);

                // 无法匹配时抛弃该染色体数据
                if (oldIndex < newChromosomesOrder.size()) {
                    String order = newChromosomesOrder.getString(oldIndex);
                    if (newRootNodes.containsKey(order)) {
                        newRootNodes.get(order).add(this.rootNodes.get(chromosome).resetChromosome(newChromosomesOrder.getString(oldIndex)));
                    } else {
                        newRootNodes.put(order, this.rootNodes.get(chromosome).resetChromosome(newChromosomesOrder.getString(oldIndex)));
                    }
                }
            }

            this.rootNodes.clear();
            this.rootNodes.putAll(newRootNodes);
        }

        // 加载新的资源文件
        ChromosomeTags.load(resourceFile);
    }

    /**
     * 检验是否包含某个染色体数据
     *
     * @param chromosome 染色体编号
     */
    public boolean contain(String chromosome) {
        return this.rootNodes.containsKey(chromosome);
    }

    /**
     * 添加单个 GTB 节点，添加操作后需要使用 flush 保证其顺序
     *
     * @param nodes 待添加的节点群
     */
    public GTBTree add(GTBNode... nodes) {
        for (GTBNode node : nodes) {
            if (!contain(node.chromosome)) {
                this.rootNodes.put(node.chromosome, new GTBNodes(node.chromosome));
            }

            get(node.chromosome).add(node);
        }

        return this;
    }

    /**
     * 添加 GTBNodes 节点群，添加操作后需要使用 flush 保证其顺序
     *
     * @param nodes 待添加的节点群
     */
    public GTBTree add(GTBNodes... nodes) {
        for (GTBNodes node : nodes) {
            if (contain(node.chromosome)) {
                get(node.chromosome).add(node);
            } else {
                this.rootNodes.put(node.chromosome, node);
            }
        }

        return this;
    }

    /**
     * 添加 GTBTree 节点树，添加操作后需要使用 flush 保证其顺序
     *
     * @param trees 待添加的节点树
     */
    public GTBTree add(GTBTree... trees) {
        for (GTBTree tree : trees) {
            for (GTBNodes nodes : tree) {
                add(nodes);
            }
        }

        return this;
    }

    /**
     * 移除染色体
     *
     * @param chromosomes 移除染色体编号列表
     */
    public GTBTree remove(String... chromosomes) {
        for (String chromosome : chromosomes) {
            if (!contain(chromosome)) {
                throw new GTBComponentException("chromosome=" + chromosome + " not in current GTB file");
            }
            this.rootNodes.remove(chromosome);
        }

        return this;
    }

    /**
     * 移除指定染色体对应的节点数据
     *
     * @param chromosomeNodeIndexList 移除节点列表
     */
    public GTBTree remove(Map<String, int[]> chromosomeNodeIndexList) {
        for (String chromosome : chromosomeNodeIndexList.keySet()) {
            if (!contain(chromosome)) {
                throw new GTBComponentException("chromosome=" + chromosome + ") not in current GTB file");
            }

            // 节点树包含该染色体编号，但移除队列中不包含任何信息，则认为是需要移除其所有的节点
            if (chromosomeNodeIndexList.get(chromosome) == null) {
                this.rootNodes.remove(chromosome);
            } else {
                // 取出每个需要剔除的节点
                get(chromosome).remove(chromosomeNodeIndexList.get(chromosome));
            }
        }

        return this;
    }

    /**
     * 保留染色体
     *
     * @param chromosomes 保留的染色体编号列表
     */
    public GTBTree retain(String... chromosomes) {
        HashMap<String, GTBNodes> newRootNodes = new HashMap<>(chromosomes.length);

        for (String chromosome : chromosomes) {
            if (!contain(chromosome)) {
                throw new GTBComponentException("chromosome=" + chromosome + " not in current GTB file");
            }

            newRootNodes.put(chromosome, get(chromosome));
        }

        this.rootNodes.clear();

        for (String chromosome : newRootNodes.keySet()) {
            this.rootNodes.put(chromosome, newRootNodes.get(chromosome));
        }

        return this;
    }

    /**
     * 移除指定染色体对应的节点数据
     *
     * @param chromosomeNodeIndexList 移除节点列表
     */
    public GTBTree retain(Map<String, int[]> chromosomeNodeIndexList) {
        HashMap<String, GTBNodes> newRootNodes = new HashMap<>(chromosomeNodeIndexList.size());
        for (String chromosome : chromosomeNodeIndexList.keySet()) {
            if (!contain(chromosome)) {
                throw new GTBComponentException("chromosome=" + chromosome + " not in current GTB file");
            }

            if (chromosomeNodeIndexList.get(chromosome) == null) {
                newRootNodes.put(chromosome, get(chromosome));
            } else {
                newRootNodes.put(chromosome, new GTBNodes(get(chromosome).get(chromosomeNodeIndexList.get(chromosome))));
            }
        }

        this.rootNodes.clear();

        for (String chromosome : newRootNodes.keySet()) {
            this.rootNodes.put(chromosome, newRootNodes.get(chromosome));
        }

        return this;
    }

    /**
     * 获取总节点数
     */
    public int numOfNodes() {
        int count = 0;
        for (GTBNodes nodes : this) {
            count += nodes.numOfNodes();
        }
        return count;
    }

    /**
     * 获取指定染色体的总节点数
     *
     * @param chromosome 指定的染色体
     */
    public int numOfNodes(String chromosome) {
        if (!contain(chromosome)) {
            throw new GTBComponentException("chromosome=" + chromosome + " not in current GTB file");
        }
        return get(chromosome).numOfNodes();
    }

    /**
     * 获取总节点数
     */
    public int numOfVariants() {
        int count = 0;
        for (GTBNodes nodes : this) {
            count += nodes.numOfVariants();
        }
        return count;
    }

    /**
     * 获取染色体编号对应的变异位点数量
     *
     * @param chromosome 指定的染色体
     */
    public int numOfVariants(String chromosome) {
        if (!contain(chromosome)) {
            throw new GTBComponentException("chromosome=" + chromosome + " not in current GTB file");
        }
        return get(chromosome).numOfVariants();
    }

    /**
     * 获得数据块的最大尺寸
     */
    public int getMaxDecompressedAllelesSize() {
        int maxOriginAllelesSize = 0;
        int currentOriginAllelesSize;
        for (GTBNodes nodes : this) {
            for (GTBNode node : nodes) {
                currentOriginAllelesSize = node.getOriginAllelesSizeFlag();
                if (maxOriginAllelesSize < currentOriginAllelesSize) {
                    maxOriginAllelesSize = currentOriginAllelesSize;
                }
            }
        }
        return GTBNode.rebuildMagicCode(maxOriginAllelesSize);
    }

    /**
     * 获得数据块的最大尺寸
     */
    public int getMaxDecompressedMBEGsSize() {
        int maxOriginMBEGsSize = 0;
        int currentOriginMBEGsSize;
        for (GTBNodes nodes : this) {
            for (GTBNode node : nodes) {
                currentOriginMBEGsSize = node.getOriginMBEGsSizeFlag();
                if (maxOriginMBEGsSize < currentOriginMBEGsSize) {
                    maxOriginMBEGsSize = currentOriginMBEGsSize;
                }
            }
        }
        return GTBNode.rebuildMagicCode(maxOriginMBEGsSize);
    }

    /**
     * 获取染色体编号
     */
    public String[] getChromosomeList() {
        String[] chromosomes = ArrayUtils.getStringKey(this.rootNodes);
        Arrays.sort(chromosomes, ChromosomeTags::chromosomeSorter);
        return chromosomes;
    }

    /**
     * 获取染色体的个数
     */
    public int numOfChromosomes() {
        return this.rootNodes.size();
    }

    /**
     * 构建块头部信息
     */
    public VolumeByteStream build() {
        // 获取总块数
        int nodeNum = numOfNodes();

        // 创建头部信息容器
        VolumeByteStream header = new VolumeByteStream(nodeNum * 25);

        // 写入块头部信息 25 byte
        for (GTBNodes nodes : this) {
            for (GTBNode node : nodes) {
                header.write(ChromosomeTags.getIndex(node.chromosome));
                header.writeIntegerValue(node.minPos);
                header.writeIntegerValue(node.maxPos);
                header.writeShortValue(node.subBlockVariantNum[0]);
                header.writeShortValue(node.subBlockVariantNum[1]);
                header.writeIntegerValue(node.compressedGenotypesSize);
                header.write(ValueUtils.value2ByteArray(node.compressedPosSize, 3));
                header.writeIntegerValue(node.compressedAlleleSize);
                header.write(node.magicCode);
            }
        }

        return header;
    }

    /**
     * 刷新 chromosome 列表
     */
    public void flush() {
        flush(false);
    }

    /**
     * 刷新 chromosome 列表，可传入参数表达是否去重
     */
    public void flush(boolean dropDuplicates) {
        for (GTBNodes nodes : this) {
            nodes.flush(dropDuplicates);
        }

        for (String chromosome : this.rootNodes.keySet()) {
            if (this.rootNodes.get(chromosome).numOfNodes() == 0) {
                this.rootNodes.remove(chromosome);
            }
        }
    }

    /**
     * 是否有序的
     */
    public boolean isOrder() {
        boolean orderedGTB = true;
        for (GTBNodes nodes : this) {
            orderedGTB = (orderedGTB && nodes.checkOrdered());
        }

        return orderedGTB;
    }

    /**
     * 是否有序的
     */
    public boolean isOrder(String chromosome) {
        return get(chromosome).checkOrdered();
    }

    /**
     * 获取最大块大小
     */
    public int getMaxOriginBlockSize(int validSubjectNum) {
        int maxOriginBlockSize = 0;
        int currentNodeEstimateSize;
        for (GTBNodes nodes : this) {
            currentNodeEstimateSize = nodes.sizeOfDecompressedNodes(validSubjectNum);
            if (currentNodeEstimateSize > maxOriginBlockSize) {
                maxOriginBlockSize = currentNodeEstimateSize;

                if (maxOriginBlockSize == Integer.MAX_VALUE - 2) {
                    break;
                }
            }
        }

        return maxOriginBlockSize;
    }

    @Override
    public String toString() {
        return nodeInfo(getChromosomeList());
    }

    public String chromosomeInfo() {
        return chromosomeInfo(getChromosomeList());
    }

    public String chromosomeInfo(String[] chromosomes) {
        for (String chromosome : chromosomes) {
            if (!contain(chromosome)) {
                throw new GTBComponentException("chromosome=" + chromosome + " not in current GTB file");
            }
        }

        if (chromosomes.length > 0 && this.rootNodes.size() > 0) {
            StringBuilder out = new StringBuilder(2 << 15);

            // 输出节点信息
            String[] chromosomeSep = new String[]{"├─ ", "└─ "};

            for (int i = 0; i < chromosomes.length - 1; i++) {
                out.append(chromosomeSep[0]).append(get(chromosomes[i]).getRootInfo());
                out.append("\n");
            }

            out.append(chromosomeSep[1] + get(chromosomes[chromosomes.length - 1]).getRootInfo());
            return out.toString();
        } else {
            return "  <empty>";
        }
    }

    public String nodeInfo(String[] chromosomes) {
        for (String chromosome : chromosomes) {
            if (!contain(chromosome)) {
                throw new GTBComponentException("chromosome=" + chromosome + " not in current GTB file");
            }
        }

        if (chromosomes.length > 0 && this.rootNodes.size() > 0) {
            StringBuilder out = new StringBuilder(2 << 15);

            // 输出节点信息
            String[] chromosomeSep = new String[]{"├─ ", "└─ "};
            String[] nodeSep = new String[]{"│  ", "   "};

            for (int i = 0; i < chromosomes.length - 1; i++) {
                out.append(chromosomeSep[0] + get(chromosomes[i]).getRootInfo());

                int chromosomeSize = get(chromosomes[i]).numOfNodes();
                if (chromosomeSize > 0) {
                    // 写入子块信息
                    String[] nodeInfos = get(chromosomes[i]).getNodesInfo();
                    for (int j = 0; j < chromosomeSize - 1; j++) {
                        out.append(String.format("\n%s ├─ Node %d: %s", nodeSep[0], j + 1, nodeInfos[j]));
                    }

                    // 写入最后一个子块信息
                    out.append(String.format("\n%s └─ Node %d: %s", nodeSep[0], chromosomeSize, nodeInfos[chromosomeSize - 1]));
                }

                out.append("\n");
            }

            int i = chromosomes.length - 1;
            out.append(chromosomeSep[1] + get(chromosomes[i]).getRootInfo());
            int chromosomeSize = get(chromosomes[i]).numOfNodes();
            if (chromosomeSize > 0) {
                // 写入子块信息
                String[] nodeInfos = get(chromosomes[i]).getNodesInfo();
                for (int j = 0; j < chromosomeSize - 1; j++) {
                    out.append(String.format("\n%s ├─ Node %d: %s", nodeSep[1], j + 1, nodeInfos[j]));
                }

                // 写入最后一个子块信息
                out.append(String.format("\n%s └─ Node %d: %s", nodeSep[1], chromosomeSize, nodeInfos[chromosomeSize - 1]));
            }

            return out.toString();
        } else {
            return "  <empty>";
        }
    }

    @Override
    public Iterator<GTBNodes> iterator() {
        return new Iterator<GTBNodes>() {
            private final String[] chromosomeList = getChromosomeList();
            private int seek = 0;

            @Override
            public boolean hasNext() {
                return this.seek < chromosomeList.length;
            }

            @Override
            public GTBNodes next() {
                return get(chromosomeList[this.seek++]);
            }
        };
    }

    @Override
    public GTBTree clone() {
        GTBTree newTree = new GTBTree();
        for (GTBNodes nodes : this) {
            newTree.add(nodes.clone());
        }

        return newTree;
    }
}
