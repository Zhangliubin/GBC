package edu.sysu.pmglab.gbc.core.gtbcomponent;

import edu.sysu.pmglab.check.Assert;
import edu.sysu.pmglab.compressor.ICompressor;
import edu.sysu.pmglab.compressor.IDecompressor;
import edu.sysu.pmglab.container.File;
import edu.sysu.pmglab.container.VolumeByteInputStream;
import edu.sysu.pmglab.container.VolumeByteStream;
import edu.sysu.pmglab.container.array.BaseArray;
import edu.sysu.pmglab.easytools.ByteCode;
import edu.sysu.pmglab.easytools.ValueUtils;
import edu.sysu.pmglab.gbc.coder.decoder.MBEGDecoder;
import edu.sysu.pmglab.gbc.constant.ChromosomeTags;
import edu.sysu.pmglab.gbc.core.exception.GTBComponentException;
import edu.sysu.pmglab.unifyIO.FileStream;

import java.io.IOException;
import java.util.Objects;

/**
 * @Data :2020/06/22
 * @Author :suranyi
 * @Contact :suranyi.sysu@gamil.com
 * @Description :GTBRoot 的核心组件
 */

public class GTBManager {
    private final File file;

    private final GTBReferenceManager reference = new GTBReferenceManager(120);
    private final FileBaseInfoManager fileBaseInfo = new FileBaseInfoManager();
    private final GTBSubjectManager subjectManager = new GTBSubjectManager();
    private final GTBTree gtbTree = new GTBTree();

    /**
     * 标准构造器，统一调用 load 进行构造
     */
    GTBManager(File file) {
        Assert.that(file != null);

        this.file = file;
        if (!this.file.isExists()) {
            throw new GTBComponentException(file + " not found");
        }

        try {
            init();
        } catch (Exception | Error e) {
            throw new GTBComponentException(e.getMessage() + ": " + file + " is not in GTB format.");
        }
    }

    GTBManager(GTBManager manager, GTBTree tree) {
        this.file = null;
        this.reference.load(manager.getReference());
        this.fileBaseInfo.load(manager.getFileBaseInfo());
        this.subjectManager.load(manager.getSubjects());
        this.gtbTree.clear();
        this.gtbTree.add(tree);
    }

    /**
     * 绑定节点树归属的根文件
     *
     * @param rootIndex 根文件编号
     */
    public void bind(int rootIndex) {
        this.gtbTree.bind(rootIndex);
    }

    /**
     * 获取 GTB 文件名
     */
    public File getFile() {
        return this.file;
    }

    /**
     * 获取文件流
     */
    public FileStream getFileStream() throws IOException {
        return new FileStream(this.file, FileStream.CHANNEL_READER);
    }

    /* 节点树操作 */

    /**
     * 获取 gtbTree
     */
    public GTBTree getGtbTree() {
        return this.gtbTree;
    }

    /**
     * 重设重叠群信息
     *
     * @param resource 资源文件
     */
    public void resetContig(File resource) throws IOException {
        this.gtbTree.resetContig(resource);
    }

    /**
     * 获取染色体列表
     */
    public String[] getChromosomeList() {
        return this.gtbTree.getChromosomeList();
    }

    /**
     * 获取指定染色体的 GTB 节点群
     *
     * @param chromosome 指定的染色体
     * @return 染色体对应的 GTB 节点群
     */
    public GTBNodes getGTBNodes(String chromosome) {
        return this.gtbTree.get(chromosome);
    }

    /**
     * 获取指定染色体的 GTB 节点群
     *
     * @param chromosome 指定的染色体
     * @return 染色体对应的 GTB 节点群
     */
    public GTBNodes[] getGTBNodes(String... chromosome) {
        return this.gtbTree.get(chromosome);
    }

    /**
     * 检验是否包含指定的染色体数据
     *
     * @param chromosome 指定的染色体
     */
    public boolean contain(String chromosome) {
        return this.gtbTree.contain(chromosome);
    }

    /**
     * 获取最大的 MBEGs 阵列大小
     */
    public int getMaxDecompressedMBEGsSize() {
        return this.gtbTree.getMaxDecompressedMBEGsSize();
    }

    /**
     * 获取最大的 alleles 阵列大小
     */
    public int getMaxDecompressedAllelesSize() {
        return this.gtbTree.getMaxDecompressedAllelesSize();
    }

    /* 文件标志符工具，重定向到文件基本信息 */

    /**
     * 获取内部的样本管理器
     */
    public FileBaseInfoManager getFileBaseInfoManager() {
        return this.fileBaseInfo;
    }

    /**
     * 获取文件标志符
     */
    public byte[] getFileBaseInfo() {
        return this.fileBaseInfo.build();
    }

    /**
     * 是否完成
     */
    public boolean isFinish() {
        return this.fileBaseInfo.isFinish();
    }

    /**
     * 获取解压块数据预估大小
     */
    public int getEstimateDecompressedBlockSize() {
        return this.fileBaseInfo.getEstimateDecompressedBlockSize();
    }

    /**
     * 获取压缩器索引
     */
    public int getCompressorIndex() {
        return this.fileBaseInfo.getCompressorIndex();
    }

    public IDecompressor getDecompressorInstance() {
        return IDecompressor.getInstance(getCompressorIndex());
    }

    /**
     * 是否有向
     */
    public boolean isPhased() {
        return this.fileBaseInfo.isPhased();
    }

    public MBEGDecoder getMBEGDecoder() {
        return MBEGDecoder.getDecoder(this.isPhased());
    }

    /**
     * 是否支持随机访问
     */
    public boolean isOrderedGTB() {
        return this.fileBaseInfo.orderedGTB();
    }

    /**
     * 获取每个块的最大大小
     */
    public int getBlockSize() {
        return this.fileBaseInfo.getBlockSize();
    }

    /**
     * 获取每个块的最大大小类型参数
     */
    public int getBlockSizeType() {
        return this.fileBaseInfo.getBlockSizeType();
    }

    /**
     * 获取压缩级别
     */
    public int getCompressionLevel() {
        return this.fileBaseInfo.getCompressionLevel();
    }

    /**
     * 是否建议为 bgzf 格式
     */
    public boolean isSuggestToBGZF() {
        return this.fileBaseInfo.isSuggestToBGZF();
    }

    /**
     * 重新检查是否为有序文件 (支持随机访问)，并更正到块头部
     */
    public void checkOrderedGTB() {
        boolean orderedGTB = this.gtbTree.isOrder();
        this.fileBaseInfo.setOrderedGTB(orderedGTB);
    }

    /**
     * 重新检查是否必须解压为 bgzf 格式
     */
    public void checkSuggestToBGZF() {
        this.fileBaseInfo.setEstimateDecompressedBlockSize(this.getGtbTree().getMaxOriginBlockSize(this.subjectManager.getSubjectNum()));
    }

    /* 参考序列管理工具 */

    /**
     * 获取参考序列
     */
    public VolumeByteStream getReference() {
        return this.reference.getReference();
    }

    /**
     * 获取参考序列
     */
    public int getReferenceSize() {
        return this.reference.size();
    }

    /**
     * 获取内部的参考序列管理器
     */
    public GTBReferenceManager getReferenceManager() {
        return this.reference;
    }

    /**
     * 参考序列是否为空
     */
    public boolean isReferenceEmpty() {
        return this.reference.size() == 0;
    }

    /* 样本管理工具 */

    /**
     * 获取 GTB 文件的样本序列
     */
    public byte[] getSubjects() {
        return this.subjectManager.getSubjects();
    }

    /**
     * 获取 GTB 文件的样本数量
     */
    public int getSubjectNum() {
        return this.subjectManager.getSubjectNum();
    }

    /**
     * 获取样本名对应的索引下标
     */
    public int[] getSubjectIndex(String... subjects) {
        return this.subjectManager.get(subjects);
    }

    /**
     * 获取样本名对应的索引下标
     */
    public int[] getSubjectIndex(BaseArray<String> subjects) {
        int[] subjectIndexes = new int[subjects.size()];
        int index = 0;
        for (String subject : subjects) {
            subjectIndexes[index++] = this.subjectManager.getIndex(subject);
        }
        return subjectIndexes;
    }

    /**
     * 获取样本名对应的索引下标
     */
    public int getSubjectIndexOrDefault(String subject, int defaultValue) {
        return this.subjectManager.getOrDefault(subject, defaultValue);
    }

    /**
     * 获取索引对应的样本名
     */
    public String getSubject(int index) {
        return this.subjectManager.get(index);
    }

    /**
     * 获取索引对应的样本名
     */
    public String[] getSubject(int... index) {
        return this.subjectManager.get(index);
    }

    /**
     * 获取索引对应的样本名
     */
    public String[] getAllSubjects() {
        return this.subjectManager.getAllSubjects();
    }

    /**
     * 获取内部的样本管理器
     */
    public GTBSubjectManager getSubjectManager() {
        return this.subjectManager;
    }

    /**
     * 重建 GTB 文件头部信息，头部信息为：文件基本信息 + 总块数 + 参考序列网址 + \n + 样本名
     *
     * @return 保存在定容字节流中的数据
     */
    public VolumeByteStream buildHeader() throws IOException {
        // 构建头部信息
        VolumeByteStream fileHeader = new VolumeByteStream(this.reference.size() + ICompressor.getCompressBound(getCompressorIndex(), this.getSubjects().length) + 10);

        // 重新检验文件基本信息
        fileHeader.write(getFileBaseInfo());

        // 写入块大小
        fileHeader.write(ValueUtils.value2ByteArray(this.gtbTree.numOfNodes(), 3));

        // 写入参考序列地址
        fileHeader.write(this.reference.getReference());

        // 写入换行符
        fileHeader.write(ByteCode.NEWLINE);

        // 写入样本名
        VolumeByteStream subjects = ICompressor.compress(getCompressorIndex(), getCompressionLevel(), this.subjectManager.getSubjects(), 0, this.subjectManager.getSubjects().length);
        fileHeader.writeIntegerValue(subjects.size());
        fileHeader.write(subjects);

        return fileHeader;
    }

    /**
     * 初始化 GTB 文件
     */
    private void init() throws IOException {
        // 清除缓冲数据
        this.gtbTree.clear();

        // 打开 GTB 文件, 以 4MB 缓冲区打开
        FileStream gtbFile = getFileStream();
        VolumeByteStream lineCache = new VolumeByteStream(2 << 20);

        // 获取基本标识信息
        this.fileBaseInfo.load(gtbFile.read(), gtbFile.read());
        if (!this.fileBaseInfo.isFinish()) {
            throw new GTBComponentException("broken GTB: " + this.file);
        }

        // 获取 GTB 节点总数
        int gtbNodeNum = (int) ValueUtils.byteArray2Value(gtbFile.read(3));

        // 写入参考序列
        gtbFile.readLine(lineCache);
        this.reference.load(lineCache.values());

        // 写入样本名
        lineCache.reset();
        gtbFile.read(lineCache, gtbFile.readIntegerValue());

        // 解压样本序列
        VolumeByteStream subjects = IDecompressor.decompress(getCompressorIndex(), lineCache);
        this.subjectManager.load(subjects);
        long seek = gtbFile.tell();

        // 一次性载入所有的块头数据，并包装为 FileStream
        gtbFile.seek(gtbFile.size() - 25L * gtbNodeNum);

        // 将字节数组包装
        VolumeByteInputStream blockHeader = new VolumeByteInputStream(gtbFile.read(gtbNodeNum * 25));
        gtbFile.close();

        for (int i = 0; i < gtbNodeNum; i++) {
            int chromosomeIndex = blockHeader.read();
            int minPos = ValueUtils.byteArray2IntegerValue(blockHeader.read(4));
            int maxPos = ValueUtils.byteArray2IntegerValue(blockHeader.read(4));
            short[] subBlockVariantNum = new short[]{ValueUtils.byteArray2ShortValue(blockHeader.read(2)), ValueUtils.byteArray2ShortValue(blockHeader.read(2))};
            int genotypeSize = ValueUtils.byteArray2IntegerValue(blockHeader.read(4));
            int posSize = (int) ValueUtils.byteArray2Value(blockHeader.read(3));
            int alleleSize = ValueUtils.byteArray2IntegerValue(blockHeader.read(4));
            byte magicCode = (byte) blockHeader.read();

            this.gtbTree.add(new GTBNode(ChromosomeTags.getString(chromosomeIndex), minPos, maxPos, seek, genotypeSize, posSize, alleleSize, magicCode, subBlockVariantNum));
            seek += posSize + alleleSize + genotypeSize;
        }

        // 刷新树结构
        this.gtbTree.flush();
        blockHeader.close();
    }

    /**
     * 打印信息
     */
    @Override
    public String toString() {
        try {
            ManagerStringBuilder builder = new ManagerStringBuilder(this);
            builder.listFileBaseInfo(false);
            builder.listSummaryInfo(true);
            builder.listChromosomeInfo(true);
            return builder.build();
        } catch (IOException e) {
            throw new GTBComponentException(e.getMessage());
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(getFile());
    }

    public ManagerStringBuilder getManagerStringBuilder() {
        try {
            return new ManagerStringBuilder(this);
        } catch (IOException e) {
            throw new GTBComponentException(e.getMessage());
        }
    }

    public void toFile(File outputFile) throws IOException {
        // 修改文件标志信息
        checkOrderedGTB();
        int numOfNodes = getGtbTree().numOfNodes();

        FileStream writer = outputFile.open(FileStream.CHANNEL_WRITER);

        // 样本名信息
        byte[] subjects = subjectManager.getSubjects();

        // 写入初始头信息
        writer.write(fileBaseInfo.build());
        writer.write(ValueUtils.value2ByteArray(numOfNodes, 3));

        // 写入 refer 网址
        writer.write(getReference());

        // 写入换行符
        writer.write(ByteCode.NEWLINE);

        // 写入样本名
        VolumeByteStream subjectsSeq = ICompressor.compress(getCompressorIndex(), getCompressionLevel(), subjects, 0, subjects.length);
        writer.writeIntegerValue(subjectsSeq.size());
        writer.write(subjectsSeq);

        // 写入块数据段
        FileStream reader = getFileStream();
        for (GTBNodes nodes : this.gtbTree) {
            for (GTBNode node : nodes) {
                reader.writeTo(node.blockSeek, node.blockSize, writer.getChannel());
            }
        }

        // 写入块头信息
        int maxEstimateSize = 0;
        VolumeByteStream headerInfo = new VolumeByteStream(numOfNodes * 25);
        for (GTBNodes nodes : this.gtbTree) {
            for (GTBNode node : nodes) {
                node.toTransFormat(headerInfo);
                int estimateSize = node.getEstimateDecompressedSize(this.getSubjectNum());
                if (estimateSize > maxEstimateSize) {
                    maxEstimateSize = estimateSize;
                }
            }
        }

        writer.write(headerInfo);
    }
}
