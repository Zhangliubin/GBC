/**
 * @Data :2021/08/15
 * @Author :suranyi
 * @Contact :suranyi.sysu@gamil.com
 * @Description :
 */

package edu.sysu.pmglab.gbc.core.gtbcomponent.gtbreader;

import edu.sysu.pmglab.compressor.IDecompressor;
import edu.sysu.pmglab.container.VolumeByteStream;
import edu.sysu.pmglab.easytools.ByteCode;
import edu.sysu.pmglab.gbc.core.gtbcomponent.GTBManager;
import edu.sysu.pmglab.gbc.core.gtbcomponent.GTBNode;
import edu.sysu.pmglab.unifyIO.FileStream;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

class DecompressionCache {
    String chromosome = null;
    int nodeIndex = -2;

    final VolumeByteStream undecompressedCache;
    final VolumeByteStream genotypesCache;
    final VolumeByteStream allelesPosCache;
    final FileStream fileStream;
    final IDecompressor decompressor;
    final TaskVariant[] taskVariants;
    boolean isGTDecompress;

    public DecompressionCache(GTBManager manager) throws IOException {
        this(manager, true);
    }

    public DecompressionCache(GTBManager manager, boolean decompressGT) throws IOException {
        if (decompressGT) {
            this.genotypesCache = new VolumeByteStream(manager.getMaxDecompressedMBEGsSize());
            this.allelesPosCache = new VolumeByteStream(manager.getMaxDecompressedAllelesSize());
            this.undecompressedCache = new VolumeByteStream(2 << 20);
            this.decompressor = IDecompressor.getInstance(manager.getCompressorIndex());

            this.taskVariants = new TaskVariant[manager.getBlockSize()];
            for (int i = 0; i < taskVariants.length; i++) {
                this.taskVariants[i] = new TaskVariant();
            }

        } else {
            this.genotypesCache = new VolumeByteStream(0);
            this.allelesPosCache = new VolumeByteStream(manager.getMaxDecompressedAllelesSize());
            this.undecompressedCache = new VolumeByteStream(2 << 20);
            this.decompressor = IDecompressor.getInstance(manager.getCompressorIndex());

            this.taskVariants = new TaskVariant[manager.getBlockSize()];
            for (int i = 0; i < taskVariants.length; i++) {
                this.taskVariants[i] = new TaskVariant();
            }

        }
        this.fileStream = manager.getFileStream();
    }

    public void fill(Pointer pointer) throws IOException {
        fill(pointer, true);
    }

    public void fill(Pointer pointer, boolean decompressGT) throws IOException {
        if ((pointer.nodeIndex != this.nodeIndex || !Objects.equals(pointer.node.chromosome, this.chromosome))) {
            // 位置不在同一个 block 中，此时切换块数据
            this.nodeIndex = pointer.nodeIndex;
            this.chromosome = pointer.node.chromosome;
            GTBNode node = pointer.getNode();

            undecompressedCache.makeSureCapacity(node.compressedAlleleSize, node.compressedGenotypesSize, node.compressedPosSize);

            /* 读取位置数据并解压 */
            undecompressedCache.reset();
            allelesPosCache.reset();
            this.fileStream.seek(node.blockSeek + node.compressedGenotypesSize);
            this.fileStream.read(undecompressedCache, node.compressedPosSize);
            decompressor.decompress(undecompressedCache, allelesPosCache);

            /* 设置位置数据、当前索引 */
            int taskNums = node.numOfVariants();
            for (int i = 0; i < taskNums; i++) {
                this.taskVariants[i].setPosition(allelesPosCache.cacheOf(i << 2), allelesPosCache.cacheOf(1 + (i << 2)),
                                allelesPosCache.cacheOf(2 + (i << 2)), allelesPosCache.cacheOf(3 + (i << 2)))
                        .setIndex(i)
                        .setDecoderIndex(i < node.subBlockVariantNum[0] ? 0 : 1);
            }

            /* 读取 allele 数据并解压 */
            undecompressedCache.reset();
            allelesPosCache.reset();
            this.fileStream.read(undecompressedCache, node.compressedAlleleSize);
            decompressor.decompress(undecompressedCache, allelesPosCache);

            /* 捕获 allele 数据 */
            int lastIndex = 0;
            int startPos;
            int tabPos;
            int endPos = this.taskVariants[0].index == 0 ? -1 : 0;

            for (int i = 0; i < taskNums; i++) {
                this.taskVariants[i].REF.reset();
                this.taskVariants[i].ALT.reset();

                startPos = allelesPosCache.indexOfN(ByteCode.SLASH, endPos, this.taskVariants[i].index - lastIndex);
                tabPos = allelesPosCache.indexOf(ByteCode.TAB, startPos + 2);
                endPos = allelesPosCache.indexOf(ByteCode.SLASH, tabPos + 1);

                this.taskVariants[i].REF.writeSafety(allelesPosCache.cacheOf(startPos + 1, tabPos));
                this.taskVariants[i].ALT.writeSafety(allelesPosCache.cacheOf(tabPos + 1, endPos));

                // 更新 lastIndex 信息
                lastIndex = this.taskVariants[i].index + 1;
            }

            /* 按照 position 进行局部重排序 */
            Arrays.sort(this.taskVariants, 0, taskNums, TaskVariant::compareVariant);

            /* 读取 genotype 数据并解压 */
            decompressGT(node, pointer, decompressGT);
        } else {
            if (decompressGT && !isGTDecompress) {
                decompressGT(pointer.getNode(), pointer, true);
            }
        }
    }

    private void decompressGT(GTBNode node, Pointer pointer, boolean decompressGT) throws IOException {
        /* 读取 genotype 数据并解压 */
        if (decompressGT) {
            undecompressedCache.reset();
            genotypesCache.reset();
            this.fileStream.seek(node.blockSeek);
            this.fileStream.read(undecompressedCache, node.compressedGenotypesSize);
            decompressor.decompress(undecompressedCache, genotypesCache);
            isGTDecompress = true;
        } else {
            isGTDecompress = false;
        }
    }

    public void close() throws IOException {
        undecompressedCache.close();
        genotypesCache.close();
        allelesPosCache.close();
        fileStream.close();
        decompressor.close();
    }
}