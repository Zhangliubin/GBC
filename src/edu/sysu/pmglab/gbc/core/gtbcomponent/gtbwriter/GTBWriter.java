package edu.sysu.pmglab.gbc.core.gtbcomponent.gtbwriter;

import edu.sysu.pmglab.container.File;
import edu.sysu.pmglab.container.VolumeByteStream;
import edu.sysu.pmglab.container.array.BaseArray;
import edu.sysu.pmglab.gbc.core.common.qualitycontrol.variant.VariantQC;
import edu.sysu.pmglab.gbc.core.common.switcher.AMDOFeature;
import edu.sysu.pmglab.gbc.core.gtbcomponent.BlockSizeParameter;
import edu.sysu.pmglab.gbc.core.gtbcomponent.GTBReferenceManager;
import edu.sysu.pmglab.gbc.core.gtbcomponent.GTBSubjectManager;
import edu.sysu.pmglab.gbc.core.gtbcomponent.gtbreader.Variant;

import java.io.Closeable;
import java.io.IOException;
import java.util.Objects;

/**
 * @author :suranyi
 */

public class GTBWriter implements AutoCloseable, Closeable {
    /**
     * 位点控制器
     */
    final VariantQC variantQC;
    int maxAlleleNums;
    boolean splitMultiallelics;
    boolean simplyAllele;

    /**
     * 当前状态
     */
    GTBUncompressedBlock activeBlock;

    /**
     * 压缩上下文
     */
    GTBCompressionContext context;

    GTBWriter(Builder buildTask) throws IOException {
        // 位点控制器
        this.variantQC = buildTask.outputParam.getVariantQC();
        this.maxAlleleNums = buildTask.outputParam.getMaxAlleleNums();
        this.splitMultiallelics = buildTask.outputParam.isSplitMultiallelics();
        this.simplyAllele = buildTask.outputParam.isSimplyAllele();

        // 创建压缩上下文
        this.context = new GTBCompressionContext(buildTask.outputParam, buildTask.outputFile, buildTask.referenceManager, buildTask.subjectManager);

        // 创建缓冲区
        this.activeBlock = new GTBUncompressedBlock(buildTask.subjectManager.getSubjectNum(), buildTask.outputParam);
    }

    /**
     * 写入一个位点
     */
    public int write(Variant variant) throws IOException {
        if (variant == null) {
            return 0;
        }

        if (activeBlock.full() || !Objects.equals(variant.chromosome, activeBlock.chromosome)) {
            flush();
        }

        if (activeBlock.seek == 0) {
            activeBlock.chromosome = variant.chromosome;
        }

        if (simplyAllele) {
            variant.simplifyAlleles();
        }

        int alleleNums = variant.getAlternativeAlleleNum();
        if (alleleNums == 2) {
            if (this.variantQC.filter(variant)) {
                Variant<AMDOFeature> cacheVariant = activeBlock.getCurrentVariant();
                cacheVariant.chromosome = variant.chromosome;
                cacheVariant.position = variant.position;
                cacheVariant.REF = variant.REF;
                cacheVariant.ALT = variant.ALT;
                cacheVariant.property.encoderIndex = 0;
                System.arraycopy(variant.BEGs, 0, cacheVariant.BEGs, 0, variant.BEGs.length);
                activeBlock.seek++;
                return 1;
            } else {
                return 0;
            }
        } else if (splitMultiallelics) {
            // 将这些位点写入缓冲
            int count = 0;
            for (Variant subVariant : (BaseArray<Variant>) variant.split()) {
                if (this.variantQC.filter(subVariant)) {
                    count++;
                    Variant<AMDOFeature> cacheVariant = activeBlock.getCurrentVariant();
                    cacheVariant.chromosome = variant.chromosome;
                    cacheVariant.position = variant.position;
                    cacheVariant.REF = subVariant.REF;
                    cacheVariant.ALT = subVariant.ALT;
                    cacheVariant.property.encoderIndex = 0;
                    System.arraycopy(subVariant.BEGs, 0, cacheVariant.BEGs, 0, variant.BEGs.length);
                    activeBlock.seek++;

                    if (activeBlock.full()) {
                        flush();
                    }
                }
            }

            return count;
        } else {
            if (alleleNums > this.maxAlleleNums) {
                // 过滤此位点
                return 0;
            }

            if (this.variantQC.filter(variant)) {
                Variant<AMDOFeature> cacheVariant = activeBlock.getCurrentVariant();
                cacheVariant.chromosome = variant.chromosome;
                ;
                cacheVariant.position = variant.position;
                cacheVariant.REF = variant.REF;
                cacheVariant.ALT = variant.ALT;
                cacheVariant.property.encoderIndex = 1;
                System.arraycopy(variant.BEGs, 0, cacheVariant.BEGs, 0, variant.BEGs.length);
                activeBlock.seek++;
                return 1;
            } else {
                return 0;
            }
        }
    }


    /**
     * 写入多个位点
     */
    public int write(Variant[] variants) throws IOException {
        int count = 0;
        for (Variant variant : variants) {
            count += write(variant);
        }
        return count;
    }

    public GTBUncompressedBlock getActiveBlock() {
        return activeBlock;
    }

    public void flush() throws IOException {
        if (!activeBlock.empty()) {
            this.context.process(this.activeBlock);
            this.activeBlock.reset();
        }
    }

    @Override
    public void close() throws IOException {
        flush();
        this.context.close();
    }

    public static class Builder {
        GTBOutputParam outputParam;
        File outputFile;
        GTBSubjectManager subjectManager = new GTBSubjectManager();
        GTBReferenceManager referenceManager = new GTBReferenceManager();

        public Builder(GTBOutputParam outputParam) {
            this.outputFile = null;
            this.outputParam = outputParam == null ? new GTBOutputParam() : outputParam;
        }

        public Builder(File outputFile, GTBOutputParam outputParam) {
            this.outputFile = outputFile;
            this.outputParam = outputParam == null ? new GTBOutputParam() : outputParam;
        }

        public GTBWriter build() throws IOException {
            // 检查
            this.outputParam.setBlockSizeType(BlockSizeParameter.getSuggestBlockSizeType(outputParam.getBlockSizeType(), subjectManager.getSubjectNum()));

            if (outputFile == null) {
                throw new IOException("no output file set");
            }
            return new GTBWriter(this);
        }

        public Builder setSubject(String... subjects) {
            subjectManager = new GTBSubjectManager();
            subjectManager.load(String.join("\t", subjects).getBytes());
            return this;
        }

        public Builder setSubject(byte[] subjects) {
            subjectManager = new GTBSubjectManager();
            subjectManager.load(subjects);
            return this;
        }

        public Builder setReference(String reference) {
            referenceManager = new GTBReferenceManager();
            referenceManager.load(reference);
            return this;
        }

        public Builder setReference(VolumeByteStream reference) {
            referenceManager = new GTBReferenceManager();
            referenceManager.load(reference);
            return this;
        }

        public Builder setOutputParam(GTBOutputParam outputParam) {
            this.outputParam = outputParam == null ? new GTBOutputParam() : outputParam;
            return this;
        }

        public Builder setOutputFile(File outputFile) {
            this.outputFile = outputFile;
            return this;
        }
    }
}

