package edu.sysu.pmglab.gbc.core.gtbcomponent.gtbwriter;

import edu.sysu.pmglab.compressor.ICompressor;
import edu.sysu.pmglab.container.File;
import edu.sysu.pmglab.container.VolumeByteStream;
import edu.sysu.pmglab.container.array.Array;
import edu.sysu.pmglab.easytools.ByteCode;
import edu.sysu.pmglab.easytools.ValueUtils;
import edu.sysu.pmglab.gbc.coder.encoder.BEGEncoder;
import edu.sysu.pmglab.gbc.core.common.combiner.ICodeCombiner;
import edu.sysu.pmglab.gbc.core.common.switcher.ISwitcher;
import edu.sysu.pmglab.gbc.core.gtbcomponent.*;
import edu.sysu.pmglab.unifyIO.FileStream;

import java.io.IOException;

/**
 * GTB 压缩上下文
 */
class GTBCompressionContext {
    /**
     * 行特征交换器
     */
    private final ISwitcher switcher;

    /**
     * 压缩器与编码器
     */
    private final ICompressor compressor;
    private final BEGEncoder encoder;

    /**
     * 编码组合器
     */
    private final ICodeCombiner codeCombiner;

    /**
     * 缓冲区
     */
    private final VolumeByteStream cache;

    /**
     * 输出文件
     */
    private final FileStream outputFile;
    private final GTBOutputParam outputParam;
    private final int validSubjectNum;
    /**
     * 块节点
     */
    private final Array<GTBNode> GTBNodeCache = new Array<>(1024, true);

    /**
     * @param outputParam 压缩任务
     */
    public GTBCompressionContext(GTBOutputParam outputParam, File outputFile, GTBReferenceManager referenceManager, GTBSubjectManager subjectManager) throws IOException {
        this.compressor = ICompressor.getInstance(outputParam.getCompressor(), outputParam.getCompressionLevel(), Math.max(subjectManager.getSubjectNum() * outputParam.getBlockSize(), outputParam.getBlockSize() * 10));
        this.switcher = ISwitcher.getInstance(outputParam.isReordering());
        this.codeCombiner = ICodeCombiner.getInstance(outputParam.isPhased(), subjectManager.getSubjectNum());
        this.encoder = BEGEncoder.getEncoder(outputParam.isPhased());
        this.validSubjectNum = subjectManager.getSubjectNum();

        // 合并数据缓冲区
        this.cache = new VolumeByteStream(subjectManager.getSubjectNum() * outputParam.getBlockSize());
        this.outputFile = outputFile.open(FileStream.CHANNEL_WRITER);
        this.outputParam = outputParam;

        // 样本名信息
        byte[] subjects = subjectManager.getSubjects();

        // 写入初始头信息
        this.outputFile.write(ValueUtils.value2ByteArray(0, 5));

        // 写入 refer 网址
        this.outputFile.write(referenceManager.getReference());

        // 写入换行符
        this.outputFile.write(ByteCode.NEWLINE);

        // 写入样本名
        VolumeByteStream subjectsSeq = ICompressor.compress(outputParam.getCompressor(), outputParam.getCompressionLevel(), subjects, 0, subjects.length);
        this.outputFile.writeIntegerValue(subjectsSeq.size());
        this.outputFile.write(subjectsSeq);
    }

    /**
     * 处理输入的 block
     *
     * @param block 待处理的 block
     */
    public void process(GTBUncompressedBlock block) throws IOException {
        int variantsNum = block.seek;

        // 最小最大位点, 位点种类计数
        int minPos = block.variants[0].position;
        int maxPos = block.variants[variantsNum - 1].position;
        short[] subBlockVariantNum = new short[2];

        for (int i = 0; i < variantsNum; i++) {
            if (block.variants[i].position < minPos) {
                minPos = block.variants[i].position;
            }

            if (block.variants[i].position > maxPos) {
                maxPos = block.variants[i].position;
            }

            subBlockVariantNum[block.variants[i].getAlternativeAlleleNum() == 2 ? 0 : 1]++;
        }

        // 特征交换
        this.switcher.switchingRow(this.encoder, block.variants, variantsNum);

        // 处理基因型数据并记录压缩流大小
        for (int i = 0; i < variantsNum; i++) {
            this.codeCombiner.combine(block.variants[i], this.cache);
        }
        int originMBEGsSize = this.cache.size();

        // 压缩基因型数据
        int compressedGenotypeSize = this.compressor.compress(this.cache);
        this.outputFile.write(this.compressor.getCache());
        this.compressor.reset();
        this.cache.reset();

        // 压缩 position 数据
        for (int i = 0; i < variantsNum; i++) {
            this.cache.writeIntegerValue(block.variants[i].position);
        }
        int originPosSize = this.cache.size();
        int compressedPosSize = this.compressor.compress(this.cache);
        this.outputFile.write(this.compressor.getCache());
        this.compressor.reset();
        this.cache.reset();

        /* allele 处必须非常小心，有可能超过缓冲容器大小 */
        int originAllelesSize = check(variantsNum, block);
        this.cache.expansionTo(originAllelesSize);
        for (int i = 0; i < variantsNum; i++) {
            this.cache.write(block.variants[i].REF);
            this.cache.write(ByteCode.TAB);
            this.cache.write(block.variants[i].ALT);
            this.cache.write(ByteCode.SLASH);
        }

        int compressedAlleleSize = this.compressor.compress(this.cache);
        this.outputFile.write(this.compressor.getCache());
        this.compressor.reset();
        this.cache.reset();

        // 送出压缩完成的数据
        GTBNode node = new GTBNode(block.chromosome, minPos, maxPos, 0, compressedGenotypeSize, compressedPosSize, compressedAlleleSize,
                originMBEGsSize, Math.max(originAllelesSize, originPosSize), subBlockVariantNum);

        // 写入数据和节点信息
        this.GTBNodeCache.add(node);
    }

    int check(int variantsNum, GTBUncompressedBlock block) {
        int requestSize = 0;
        for (int i = 0; i < variantsNum; i++) {
            requestSize += block.variants[i].ALT.length + block.variants[i].REF.length + 2;
        }

        if (this.cache.getCapacity() < requestSize) {
            return Math.min(requestSize << 1, Integer.MAX_VALUE - 2);
        }

        return requestSize;
    }

    /**
     * 关闭压缩机，清除资源
     */
    public void close() throws IOException {
        this.compressor.close();

        // 写入块头信息
        int maxEstimateSize = 0;
        VolumeByteStream headerInfo = new VolumeByteStream(this.GTBNodeCache.size() * 25);
        for (GTBNode node : this.GTBNodeCache) {
            node.toTransFormat(headerInfo);
            int estimateSize = node.getEstimateDecompressedSize(this.validSubjectNum);
            if (estimateSize > maxEstimateSize) {
                maxEstimateSize = estimateSize;
            }
        }

        this.outputFile.write(headerInfo);

        // 修改文件标志信息
        GTBTree tree = new GTBTree(this.GTBNodeCache);
        FileBaseInfoManager baseInfoManager = FileBaseInfoManager.of(this.outputParam);
        baseInfoManager.setOrderedGTB(tree.isOrder());
        baseInfoManager.setEstimateDecompressedBlockSize(maxEstimateSize);
        this.outputFile.seek(0);
        this.outputFile.write(baseInfoManager.build());
        this.outputFile.write(ValueUtils.value2ByteArray(this.GTBNodeCache.size(), 3));
        this.outputFile.close();
    }
}
