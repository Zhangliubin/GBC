package edu.sysu.pmglab.gbc.core.gtbcomponent.gtbreader;

import edu.sysu.pmglab.check.Assert;
import edu.sysu.pmglab.container.VolumeByteStream;
import edu.sysu.pmglab.container.array.Array;
import edu.sysu.pmglab.container.array.BaseArray;
import edu.sysu.pmglab.easytools.ArrayUtils;
import edu.sysu.pmglab.easytools.ByteCode;
import edu.sysu.pmglab.easytools.ValueUtils;
import edu.sysu.pmglab.gbc.coder.BEGTransfer;
import edu.sysu.pmglab.gbc.coder.CoderConfig;
import edu.sysu.pmglab.gbc.coder.decoder.BEGDecoder;
import edu.sysu.pmglab.gbc.coder.encoder.BEGEncoder;
import edu.sysu.pmglab.gbc.constant.ChromosomeTags;
import edu.sysu.pmglab.gbc.core.calculation.ld.ILDModel;
import edu.sysu.pmglab.gbc.core.exception.GTBComponentException;
import edu.sysu.pmglab.gbc.core.gtbcomponent.gtbreader.formatter.*;

import java.util.Arrays;
import java.util.Objects;

/**
 * @author suranyi
 */

public class Variant<T> implements Comparable<Variant<T>> {
    /**
     * GTBReader 读取到的位点对象
     */
    public String chromosome;
    public int position;
    public byte[] REF;
    public byte[] ALT;
    public byte[] BEGs;
    public boolean phased;
    public T property;

    public Variant() {
        BEGs = new byte[0];
    }

    public Variant(int BEGSize) {
        BEGs = new byte[BEGSize];
    }

    public Variant(Variant variant) {
        this.chromosome = variant.chromosome;
        this.position = variant.position;
        this.REF = ArrayUtils.copyOfRange(variant.REF, 0, variant.REF.length);
        this.ALT = ArrayUtils.copyOfRange(variant.ALT, 0, variant.ALT.length);
        this.BEGs = ArrayUtils.copyOfRange(variant.BEGs, 0, variant.BEGs.length);
        this.phased = variant.phased;

        if (variant.property == null) {
            this.property = null;
        } else {
            this.property = (T) variant.property;
        }
    }

    /**
     * 将多个二等位基因/多等位基因位点合并为单个位点
     *
     * @param variants 等位基因列表
     */
    public static Variant join(Array<Variant> variants) {
        Assert.NotEmpty(variants);

        if (variants.size() == 1) {
            return new Variant(variants.get(0));
        }

        Variant targetVariant = new Variant();
        Variant firstVariant = variants.get(0);

        targetVariant.chromosome = firstVariant.chromosome;
        targetVariant.position = firstVariant.position;
        targetVariant.BEGs = new byte[firstVariant.BEGs.length];
        targetVariant.REF = firstVariant.REF;
        targetVariant.phased = firstVariant.phased;

        // 转换编码表
        int[][] transCode = new int[variants.size()][];
        BaseArray<byte[]> newALT = new Array<>(byte[][].class);
        newALT.add(targetVariant.REF);

        for (int i = 0; i < variants.size(); i++) {
            Variant variant = variants.get(i);
            if (!variant.chromosome.equals(targetVariant.chromosome) || variant.position != targetVariant.position) {
                throw new GTBComponentException("variants with different coordinates cannot be combined into a single multi-allelic variant");
            }

            if (variant.BEGs.length != targetVariant.BEGs.length) {
                throw new GTBComponentException("variants with different sample sizes cannot be combined into a single multi-allelic variant");
            }

            targetVariant.phased = targetVariant.phased || variant.phased;
            int[] ACs = variant.getACs();
            transCode[i] = new int[ACs.length];

            out:
            for (int j = 0; j < ACs.length; j++) {
                if (ACs[j] > 0) {
                    byte[] allelej = variant.getAllele(j);
                    for (int k = 0; k < newALT.size(); k++) {
                        if (Arrays.equals(newALT.get(k), allelej)) {
                            transCode[i][j] = k;
                            continue out;
                        }
                    }

                    // 没有找到，说明是新的 alt
                    newALT.add(allelej);
                    transCode[i][j] = (byte) (newALT.size() - 1);
                }
            }
        }

        if (newALT.size() > CoderConfig.MAX_ALLELE_NUM) {
            throw new GTBComponentException("variant contains too many alternative alleles (> " + (CoderConfig.MAX_ALLELE_NUM) + ")");
        }

        // 经过检验, 开始合并位点
        int leftGenotype;
        int rightGenotype;

        BEGEncoder encoder = BEGEncoder.getEncoder(targetVariant.phased);

        if (targetVariant.phased) {
            // phased 时
            for (int i = 0; i < targetVariant.BEGs.length; i++) {
                leftGenotype = -1;
                rightGenotype = -1;

                for (int j = 0; j < variants.size(); j++) {
                    Variant variant = variants.get(j);

                    if (variant.BEGs[i] != 0) {
                        if (leftGenotype == -1 || leftGenotype == 0) {
                            // 还没设置该基因型
                            leftGenotype = transCode[j][BEGDecoder.decodeHaplotype(0, variant.BEGs[i])];
                        }

                        if (rightGenotype == -1 || rightGenotype == 0) {
                            // 还没设置该基因型
                            rightGenotype = transCode[j][BEGDecoder.decodeHaplotype(1, variant.BEGs[i])];
                        }
                    }
                }

                if (leftGenotype == -1 && rightGenotype == -1) {
                    targetVariant.BEGs[i] = encoder.encodeMiss();
                } else {
                    targetVariant.BEGs[i] = encoder.encode(leftGenotype, rightGenotype);
                }
            }
        } else {
            // unphased 时
            for (int i = 0; i < targetVariant.BEGs.length; i++) {
                leftGenotype = -1;
                rightGenotype = -1;

                for (int j = 0; j < variants.size(); j++) {
                    Variant variant = variants.get(j);

                    if (variant.BEGs[i] != 0) {
                        if (leftGenotype == -1 || leftGenotype == 0) {
                            // 还没设置该基因型
                            leftGenotype = transCode[j][BEGDecoder.decodeHaplotype(0, variant.BEGs[i])];

                            if (leftGenotype == 0) {
                                leftGenotype = transCode[j][BEGDecoder.decodeHaplotype(1, variant.BEGs[i])];

                                if (rightGenotype == -1) {
                                    rightGenotype = 0;
                                }
                            } else {
                                rightGenotype = transCode[j][BEGDecoder.decodeHaplotype(1, variant.BEGs[i])];
                            }
                        } else if (rightGenotype == 0) {
                            // 还没设置该基因型
                            rightGenotype = transCode[j][BEGDecoder.decodeHaplotype(0, variant.BEGs[i])];

                            if (rightGenotype == 0) {
                                rightGenotype = transCode[j][BEGDecoder.decodeHaplotype(1, variant.BEGs[i])];
                            }
                        }
                    }
                }

                if (leftGenotype == -1 && rightGenotype == -1) {
                    targetVariant.BEGs[i] = encoder.encodeMiss();
                } else {
                    targetVariant.BEGs[i] = encoder.encode(leftGenotype, rightGenotype);
                }
            }
        }

        VolumeByteStream newALTCache = new VolumeByteStream();
        newALT.popFirst();
        for (byte[] allele : newALT) {
            newALTCache.writeSafety(allele);
            newALTCache.writeSafety(ByteCode.COMMA);
        }

        if (newALTCache.size() == 0) {
            newALTCache.write(ByteCode.PERIOD);
            newALTCache.write(ByteCode.COMMA);
        }

        targetVariant.ALT = newALTCache.rangeOf(0, newALTCache.size() - 1);
        return targetVariant;
    }

    /**
     * 将多个二等位基因/多等位基因位点合并为单个位点
     *
     * @param variants 等位基因列表
     */
    public static Variant join(Variant... variants) {
        return join(new Array<>(variants));
    }

    public Variant(String chromosome, int position, byte[] Allele, byte[] BEGs) {
        this(chromosome, position, Allele, BEGs, true);
    }

    public Variant(String chromosome, int position, byte[] Allele, byte[] BEGs, boolean phased) {
        this(chromosome, position, Allele, Allele, BEGs, phased);
        int sepIndex = ArrayUtils.indexOf(Allele, ByteCode.TAB);
        this.REF = ArrayUtils.copyOfRange(Allele, 0, sepIndex);
        this.ALT = ArrayUtils.copyOfRange(Allele, sepIndex + 1, Allele.length);
    }

    public Variant(String chromosome, int position, byte[] REF, byte[] ALT, byte[] BEGs, boolean phased) {
        this.chromosome = chromosome;
        this.position = position;
        this.REF = REF;
        this.ALT = ALT;
        this.BEGs = BEGs;
        this.phased = phased;
    }

    /**
     * 获取属性
     */
    public T getProperty() {
        return property;
    }

    /**
     * 获取可替代等位基因的个数
     *
     * @return 可替代等位基因个数
     */
    public int getAlternativeAlleleNum() {
        return ArrayUtils.valueCounts(ALT, ByteCode.COMMA) + 2;
    }

    /**
     * 获取 AC 值, 该值已通过倍型校正
     *
     * @return 获取 AC 值
     */
    public int getAC() {
        return apply(ACValueFormatter.INSTANCE);
    }

    /**
     * 获取 AC 值, 该值已通过倍型校正
     *
     * @return 获取 AC 值
     */
    public int[] getACs() {
        return apply(ACsValueFormatter.INSTANCE);
    }

    /**
     * 获取 AN 值, 该值已通过倍型校正
     *
     * @return 获取 AN 值
     */
    public int getAN() {
        return apply(ANValueFormatter.INSTANCE);
    }

    /**
     * 获取 基因型计数值
     */
    public int[] getGenotypeCounts() {
        return apply(GenotypeCountsFormatter.INSTANCE);
    }

    /**
     * 获取缺失样本的个数
     *
     * @return 缺失样本个数
     */
    public int getMissSubjectNum() {
        return apply(MissSubjectNumValueFormatter.INSTANCE);
    }

    /**
     * 获取 AF
     *
     * @return AF 值，等位基因频率
     */
    public double getAF() {
        return apply(AFValueFormatter.INSTANCE);
    }

    /**
     * 获取所有等位基因形式的频率值
     *
     * @return AFs 值，等位基因频率
     */
    public double[] getAFs() {
        return apply(AFsValueFormatter.INSTANCE);
    }

    /**
     * 获取 MAF
     *
     * @return MAF 值，次等位基因频率
     */
    public double getMAF() {
        return apply(MAFValueFormatter.INSTANCE);
    }

    /**
     * 是否有缺失基因型
     */
    public boolean hasMissGenotype() {
        return apply(HasMissGTFormatter.INSTANCE);
    }

    /**
     * 获取位点的 BEG 编码
     */
    public int getBEGCode(int index) {
        return BEGs[index] & 0xFF;
    }

    /**
     * 转为位编码
     */
    public int[] toBitCode(byte[] bitCodeTable) {
        return apply(ToBitCodeFormatter.INSTANCE, bitCodeTable);
    }

    /**
     * 获取位点的 BEG 编码
     *
     * @param index          编码索引
     * @param haplotypeIndex 单倍型索引 (1 / 2)
     */
    public int getGenotypeCode(int index, int haplotypeIndex) {
        return BEGDecoder.decodeHaplotype(haplotypeIndex, BEGs[index]);
    }

    /**
     * 按照新的等位基因信息转换编码
     */
    public void resetAlleles(String newREF, String newALT) {
        resetAlleles(newREF.getBytes(), newALT.getBytes());
    }

    /**
     * 按照新的等位基因信息转换编码
     */
    public void resetAlleles(String newREF) {
        resetAlleles(newREF.getBytes());
    }

    /**
     * 按照新的等位基因信息转换编码
     */
    public void resetAlleles(byte[] newREF, byte[] newALT) {
        if ((ArrayUtils.equal(REF, newREF) && ArrayUtils.startWiths(newALT, ALT) && ((newALT.length == ALT.length) || (newALT[ALT.length] == ByteCode.COMMA)))) {
            ALT = newALT;
        } else {
            // 获取等位基因个数
            int allelesNum = getAlternativeAlleleNum();
            if ((allelesNum == 2) && ArrayUtils.equal(ALT, newREF) && (ArrayUtils.startWiths(newALT, REF) && ((newALT.length == REF.length) || (newALT[REF.length] == ByteCode.COMMA)))) {
                // 二等位基因，并且只需要翻转编码表
                for (int i = 0; i < this.BEGs.length; i++) {
                    BEGs[i] = BEGTransfer.reverse(this.phased, BEGs[i]);
                }

                REF = newREF;
                ALT = newALT;
            } else {
                // 此时处理的情况复杂的多，可能会变成多等位基因位点
                Array<byte[]> newALTCache = new Array<>();
                int[] ACs = this.getACs();
                int[] transCode = new int[ACs.length];
                newALTCache.add(newREF);

                // 虚拟位点
                Variant fakeVariant = new Variant();
                fakeVariant.REF = newREF;
                fakeVariant.ALT = newALT;

                out:
                for (int i = 0; i < fakeVariant.getAlternativeAlleleNum(); i++) {
                    byte[] allelej = fakeVariant.getAllele(i);
                    for (int j = 0; j < newALTCache.size(); j++) {
                        if (Arrays.equals(newALTCache.get(j), allelej)) {
                            continue out;
                        }
                    }

                    // 没有找到，说明是新的 alt
                    newALTCache.add(allelej);
                }

                // 扫描 allele
                out:
                for (int i = 0; i < ACs.length; i++) {
                    if (ACs[i] > 0) {
                        byte[] allelej = getAllele(i);
                        for (int j = 0; j < newALTCache.size(); j++) {
                            if (Arrays.equals(newALTCache.get(j), allelej)) {
                                transCode[i] = j;
                                continue out;
                            }
                        }

                        // 没有找到，说明是新的 alt
                        newALTCache.add(allelej);
                        transCode[i] = (byte) (newALTCache.size() - 1);
                    }
                }

                if (newALTCache.size() > CoderConfig.MAX_ALLELE_NUM) {
                    throw new GTBComponentException("variant contains too many alternative alleles (> " + (CoderConfig.MAX_ALLELE_NUM) + ")");
                }

                // 经过检验, 开始合并位点
                VolumeByteStream cache = new VolumeByteStream();
                newALTCache.popFirst();
                for (byte[] allele : newALTCache) {
                    cache.writeSafety(allele);
                    cache.writeSafety(ByteCode.COMMA);
                }

                if (newALTCache.size() == 0) {
                    cache.write(ByteCode.PERIOD);
                    cache.write(ByteCode.COMMA);
                }

                this.REF = newREF;
                this.ALT = cache.rangeOf(0, cache.size() - 1);

                // 转换编码
                boolean fastMode = true;
                for (int i = 0; i < ACs.length; i++) {
                    if (ACs[i] != i) {
                        // 如果映射关系没有改变, 则为快速模式
                        fastMode = false;
                        break;
                    }
                }

                if (!fastMode) {
                    BEGEncoder encoder = BEGEncoder.getEncoder(this.phased);
                    for (int i = 0; i < this.BEGs.length; i++) {
                        BEGs[i] = BEGs[i] == 0 ? 0 : encoder.encode(transCode[BEGDecoder.decodeHaplotype(0, BEGs[i])], transCode[BEGDecoder.decodeHaplotype(1, BEGs[i])]);
                    }
                }
            }
        }
    }

    /**
     * 按照新的等位基因信息转换编码
     */
    public void resetAlleles(byte[] newREF) {
        if (!Arrays.equals(newREF, this.REF)) {
            resetAlleles(newREF, this.REF);
        }
    }

    /**
     * 横向合并位点 (认为它们来自不同的测序样本)
     *
     * @param otherVariant 另一个变异位点
     */
    public Variant merge(Variant otherVariant) {
        return merge(otherVariant, true);
    }

    /**
     * 横向合并位点 (认为它们来自不同的测序样本)
     *
     * @param otherVariant 另一个变异位点
     */
    public Variant merge(Variant otherVariant, Variant target) {
        return merge(otherVariant, target, true);
    }

    /**
     * 横向合并位点 (认为它们来自不同的测序样本)
     *
     * @param otherVariant     另一个变异位点
     * @param verifyCoordinate 是否验证坐标
     */
    public Variant merge(final Variant otherVariant, boolean verifyCoordinate) {
        return merge(otherVariant, new Variant(), verifyCoordinate);
    }

    /**
     * 横向合并位点 (认为它们来自不同的测序样本)
     *
     * @param otherVariant     另一个变异位点
     * @param target           填充数据到指定的位点
     * @param verifyCoordinate 是否验证坐标
     */
    public Variant merge(final Variant otherVariant, Variant target, boolean verifyCoordinate) {
        if (verifyCoordinate && !(otherVariant.chromosome.equals(this.chromosome) && (otherVariant.position == this.position))) {
            throw new UnsupportedOperationException("merge variant with different coordinates are not allowed");
        }

        target.chromosome = this.chromosome;
        target.position = this.position;
        target.phased = this.phased;

        // 传入位点作为主位点
        if ((ArrayUtils.equal(REF, otherVariant.REF) && (ArrayUtils.equal(ALT, otherVariant.ALT)))) {
            target.REF = this.REF;
            target.ALT = this.ALT;
            if (target.BEGs.length == this.BEGs.length + otherVariant.BEGs.length) {
                System.arraycopy(this.BEGs, 0, target.BEGs, 0, this.BEGs.length);
                System.arraycopy(otherVariant.BEGs, 0, target.BEGs, this.BEGs.length, otherVariant.BEGs.length);
            } else {
                target.BEGs = ArrayUtils.merge(this.BEGs, otherVariant.BEGs);
            }
        } else {
            // 此时处理的情况复杂的多，会变成多等位基因位点
            Array<byte[]> newALT = new Array<>();
            int[] ACs1 = this.getACs();
            int[] ACs2 = otherVariant.getACs();
            int[][] transCode = new int[][]{new int[ACs1.length], new int[ACs2.length]};
            newALT.add(this.REF);

            // 扫描 allele
            out:
            for (int i = 1; i < ACs1.length; i++) {
                if (ACs1[i] > 0) {
                    byte[] allelej = getAllele(i);
                    for (int j = 0; j < newALT.size(); j++) {
                        if (Arrays.equals(newALT.get(j), allelej)) {
                            transCode[0][i] = j;
                            continue out;
                        }
                    }

                    // 没有找到，说明是新的 alt
                    newALT.add(allelej);
                    transCode[0][i] = (byte) (newALT.size() - 1);
                }
            }

            out:
            for (int i = 0; i < ACs2.length; i++) {
                if (ACs2[i] > 0) {
                    byte[] allelej = otherVariant.getAllele(i);
                    for (int j = 0; j < newALT.size(); j++) {
                        if (Arrays.equals(newALT.get(j), allelej)) {
                            transCode[1][i] = j;
                            continue out;
                        }
                    }

                    // 没有找到，说明是新的 alt
                    newALT.add(allelej);
                    transCode[1][i] = (byte) (newALT.size() - 1);
                }
            }

            if (newALT.size() > CoderConfig.MAX_ALLELE_NUM) {
                throw new GTBComponentException("variant contains too many alternative alleles (> " + (CoderConfig.MAX_ALLELE_NUM) + ")");
            }

            // 经过检验, 开始合并位点
            VolumeByteStream newALTCache = new VolumeByteStream();
            newALT.popFirst();
            for (byte[] allele : newALT) {
                newALTCache.writeSafety(allele);
                newALTCache.writeSafety(ByteCode.COMMA);
            }

            if (newALTCache.size() == 0) {
                newALTCache.write(ByteCode.PERIOD);
                newALTCache.write(ByteCode.COMMA);
            }

            target.REF = this.REF;
            target.ALT = newALTCache.rangeOf(0, newALTCache.size() - 1);

            // 转换编码
            BEGEncoder encoder = BEGEncoder.getEncoder(this.phased);
            if (target.BEGs.length != this.BEGs.length + otherVariant.BEGs.length) {
                target.BEGs = new byte[this.BEGs.length + otherVariant.BEGs.length];
            }

            // 尽可能不处理 variant 1 (直接拷贝)
            boolean fastMode = true;

            // 所有的碱基都有计数值
            for (int i = 0; i < ACs1.length; i++) {
                // 在 beginIndex 之后的全都是 0
                if (transCode[0][i] != i) {
                    fastMode = false;
                    break;
                }
            }

            if (fastMode) {
                System.arraycopy(this.BEGs, 0, target.BEGs, 0, this.BEGs.length);
            } else {
                for (int i = 0; i < this.BEGs.length; i++) {
                    target.BEGs[i] = this.BEGs[i] == 0 ? 0 : encoder.encode(transCode[0][BEGDecoder.decodeHaplotype(0, this.BEGs[i])], transCode[0][BEGDecoder.decodeHaplotype(1, this.BEGs[i])]);
                }
            }

            for (int i = 0; i < otherVariant.BEGs.length; i++) {
                target.BEGs[this.BEGs.length + i] = otherVariant.BEGs[i] == 0 ? 0 : encoder.encode(transCode[1][BEGDecoder.decodeHaplotype(0, otherVariant.BEGs[i])], transCode[1][BEGDecoder.decodeHaplotype(1, otherVariant.BEGs[i])]);
            }
        }
        return target;
    }

    /**
     * 转为 VCF 格式
     */
    public byte[] toVCF() {
        return toVCF(false);
    }

    /**
     * 转为 VCF 格式
     *
     * @param cache 输出缓冲区
     */
    public int toVCF(VolumeByteStream cache) {
        return toVCF(false, cache);
    }

    /**
     * 转为 VCF 位点数据 (非基因型数据)
     */
    public byte[] toVCFSite() {
        return apply(VCFSiteVariantFormatter.INSTANCE);
    }

    /**
     * 转为 VCF 位点数据 (非基因型数据)
     *
     * @param cache 输出缓冲区
     */
    public int toVCFSite(VolumeByteStream cache) {
        return apply(VCFSiteVariantFormatter.INSTANCE, cache);
    }

    /**
     * 转为 VCF 格式
     *
     * @param info 是否写入 INFO 信息 (AC, AN, AF)
     */
    public byte[] toVCF(boolean info) {
        if (info) {
            return apply(VCFVariantFormatter.INSTANCE);
        } else {
            return apply(EasyVCFVariantFormatter.INSTANCE);
        }
    }

    /**
     * 转为 VCF 格式
     *
     * @param info 是否写入 INFO 信息 (AC, AN, AF)
     */
    public int toVCF(boolean info, VolumeByteStream cache) {
        if (info) {
            return apply(VCFVariantFormatter.INSTANCE, cache);
        } else {
            return apply(EasyVCFVariantFormatter.INSTANCE, cache);
        }
    }

    /**
     * 转为 unphased 基因型
     */
    public void toUnphased() {
        toUnphased(true);
    }

    /**
     * 转为 unphased 基因型
     *
     * @param inplace 原位改变基因型的向型
     */
    public byte[] toUnphased(boolean inplace) {
        if (this.phased) {
            if (inplace) {
                for (int i = 0; i < this.BEGs.length; i++) {
                    this.BEGs[i] = BEGTransfer.toUnphased(this.BEGs[i]);
                }
                this.phased = false;
                return this.BEGs;
            } else {
                return apply(UnphasedVariantFormatter.INSTANCE);
            }
        } else {
            return this.BEGs;
        }
    }

    /**
     * 将一个多等位基因位点转为多个二等位基因位点
     */
    public Array<Variant> split() {
        int alternativeAlleleNum = getAlternativeAlleleNum();
        Array<Variant> out = new Array<>(Variant[].class, true);

        if (alternativeAlleleNum == 2) {
            // 二等位基因位点
            out.add(new Variant(this));
        } else {
            // 有多个有效等位基因时
            int[] ACs = getACs();

            if (ValueUtils.sum(ArrayUtils.copyOfRange(ACs, 2, ACs.length)) == 0) {
                // 前两个等位基因有计数值, 随后的都没有
                Variant cacheVariant = new Variant();
                cacheVariant.chromosome = this.chromosome;
                cacheVariant.position = this.position;
                cacheVariant.phased = this.phased;
                cacheVariant.REF = ArrayUtils.copyOfRange(this.REF, 0, this.REF.length);
                cacheVariant.ALT = getAllele(1);

                // 此时基因型全为 0/0 0/1 1/0 1/1 或 ./.
                cacheVariant.BEGs = ArrayUtils.copyOfRange(this.BEGs, 0, this.BEGs.length);
                out.add(cacheVariant);
            } else {
                BEGEncoder encoder = BEGEncoder.getEncoder(this.phased);
                for (int i = 1; i < ACs.length; i++) {
                    if (ACs[i] == 0) {
                        continue;
                    }
                    Variant cacheVariant = new Variant();
                    cacheVariant.chromosome = this.chromosome;
                    cacheVariant.position = this.position;
                    cacheVariant.phased = this.phased;
                    cacheVariant.REF = ArrayUtils.copyOfRange(this.REF, 0, this.REF.length);
                    cacheVariant.BEGs = new byte[this.BEGs.length];
                    cacheVariant.ALT = getAllele(i);

                    for (int j = 0; j < this.BEGs.length; j++) {
                        if (this.BEGs[j] == 0) {
                            cacheVariant.BEGs[j] = 0;
                        } else {
                            int leftGenotype = BEGDecoder.decodeHaplotype(0, this.BEGs[j]);
                            int rightGenotype = BEGDecoder.decodeHaplotype(1, this.BEGs[j]);

                            if (leftGenotype == i) {
                                leftGenotype = 1;
                            } else {
                                leftGenotype = 0;
                            }

                            if (rightGenotype == i) {
                                rightGenotype = 1;
                            } else {
                                rightGenotype = 0;
                            }

                            cacheVariant.BEGs[j] = encoder.encode(leftGenotype, rightGenotype);
                        }
                    }

                    out.add(cacheVariant);
                }
            }
        }

        return out;
    }

    /**
     * 简化 alleles, 将 AC = 0 的碱基删除
     */
    public void simplifyAlleles() {
        int[] ACs = getACs();
        int[] transCode = new int[ACs.length];
        int index = 0;

        transCode[0] = index++;

        for (int i = 1; i < ACs.length; i++) {
            if (ACs[i] > 0) {
                transCode[i] = index++;
            }
        }

        if (index == 1 || (index == 2 && ACs[1] != 0)) {
            // 所有的 allele 都是无效的, 此时保持 REF 和 第一个 ALT
            this.ALT = getAllele(1);
            return;
        }

        if (index < ACs.length) {
            // 有效等位基因个数比实际少, 此时才触发精简
            if (ACs[ACs.length - 1] == 0) {
                // 如果最后一个等于 0, 则考虑以快速模式进行精简
                out:
                for (int i = ACs.length - 2; i >= 2; i--) {
                    if (ACs[i] != 0) {
                        for (int j = 1; j < i; j++) {
                            if (ACs[j] == 0) {
                                break out;
                            }
                        }

                        this.ALT = ArrayUtils.copyOfRange(this.ALT, 0, ArrayUtils.indexOfN(this.ALT, ByteCode.COMMA, 0, i));
                        return;
                    }
                }
            }

            // 触发转码
            VolumeByteStream newALT = new VolumeByteStream(this.ALT.length);
            for (int i = 1; i < ACs.length; i++) {
                if (ACs[i] != 0) {
                    newALT.write(getAllele(i));
                    newALT.write(ByteCode.COMMA);
                }
            }

            this.ALT = newALT.cacheOf(0, newALT.size() - 1);

            // 基因型转换
            BEGEncoder encoder = BEGEncoder.getEncoder(this.phased);
            for (int i = 0; i < this.BEGs.length; i++) {
                this.BEGs[i] = this.BEGs[i] == 0 ? 0 : encoder.encode(transCode[BEGDecoder.decodeHaplotype(0, this.BEGs[i])], transCode[BEGDecoder.decodeHaplotype(1, this.BEGs[i])]);
            }
        }
    }

    /**
     * 获取第 i 个等位基因
     */
    public byte[] getAllele(int index) {
        if (index == 0) {
            return this.REF;
        } else if (index == 1) {
            int pointer = ArrayUtils.indexOf(this.ALT, ByteCode.COMMA);
            if (pointer == -1) {
                return this.ALT;
            } else {
                return ArrayUtils.copyOfRange(this.ALT, 0, pointer);
            }
        } else {
            int pointerStart = ArrayUtils.indexOfN(this.ALT, ByteCode.COMMA, 0, index - 1);
            int pointerEnd = ArrayUtils.indexOfN(this.ALT, ByteCode.COMMA, pointerStart + 1, 1);

            if (pointerStart == -1) {
                return null;
            }

            if (pointerEnd == -1) {
                return ArrayUtils.copyOfRange(this.ALT, pointerStart + 1, this.ALT.length);
            }

            return ArrayUtils.copyOfRange(this.ALT, pointerStart + 1, pointerEnd);
        }
    }

    /**
     * 获取第 i 个等位基因的索引
     */
    public int getAlleleIndex(byte[] allele) {
        if (Arrays.equals(allele, this.REF)) {
            return 0;
        } else {
            // 是否包含逗号
            boolean containComma = ArrayUtils.contain(this.ALT, ByteCode.COMMA);

            if (containComma) {
                int start = 0;
                int end;
                int index = 1;
                while (true) {
                    end = ArrayUtils.indexOf(this.ALT, ByteCode.COMMA, start);
                    if (end != -1) {
                        if (ArrayUtils.equal(this.ALT, start, end, allele, 0, allele.length)) {
                            return index;
                        }

                        start = end + 1;
                        if (start < this.ALT.length) {
                            index++;
                        } else {
                            return -1;
                        }
                    } else {
                        // end == -1
                        if (ArrayUtils.equal(this.ALT, start, this.ALT.length, allele, 0, allele.length)) {
                            return index;
                        }
                        return -1;
                    }
                }
            } else {
                if (Arrays.equals(this.ALT, allele)) {
                    return 1;
                } else {
                    return -1;
                }
            }
        }
    }

    /**
     * 计算二等位基因位点的 LD 系数
     *
     * @param model   LD 计算模型
     * @param variant 变异位点
     * @return LD 系数计算结果
     */
    public double calculateLDR2(ILDModel model, Variant variant) {
        return model.calculateLDR2(this, variant);
    }

    /**
     * 计算二等位基因位点的 LD 系数
     *
     * @param model   LD 计算模型
     * @param variant 变异位点
     * @return LD 系数计算结果
     */
    public double calculateLDR(ILDModel model, Variant variant) {
        if (getAlternativeAlleleNum() == 2) {
            if (variant.getAlternativeAlleleNum() == 2) {
                return model.calculateLD(this, variant);
            } else {
                double r = 0;
                double tempR;
                boolean set = false;
                for (Variant spV : (BaseArray<Variant>) variant.split()) {
                    tempR = model.calculateLD(this, spV);
                    if (!Double.isNaN(tempR) && Math.abs(tempR) > r) {
                        r = tempR;
                        set = true;
                    }
                }
                if (set) {
                    return r;
                } else {
                    return Double.NaN;
                }
            }
        } else {
            if (variant.getAlternativeAlleleNum() == 2) {
                double r = 0;
                double tempR;
                boolean set = false;
                for (Variant spV : this.split()) {
                    tempR = model.calculateLD(spV, variant);
                    if (!Double.isNaN(tempR) && Math.abs(tempR) > r) {
                        r = tempR;
                        set = true;
                    }
                }

                if (set) {
                    return r;
                } else {
                    return Double.NaN;
                }
            } else {
                double r = 0;
                double tempR;
                boolean set = false;
                for (Variant spV1: this.split()) {
                    for (Variant spV2 : (BaseArray<Variant>) variant.split()) {
                        tempR = model.calculateLD(spV1, spV2);
                        if (!Double.isNaN(tempR) && Math.abs(tempR) > r) {
                            r = tempR;
                            set = true;
                        }
                    }
                }

                if (set) {
                    return r;
                } else {
                    return Double.NaN;
                }
            }
        }
    }

    /**
     * 转为任意格式
     *
     * @param variantFormatter 序列格式转换器
     */
    public <Out> Out apply(VariantFormatter<Void, Out> variantFormatter) {
        return variantFormatter.apply(this);
    }

    /**
     * 转为任意格式
     *
     * @param variantFormatter 序列格式转换器
     */
    public <In, Out> Out apply(VariantFormatter<In, Out> variantFormatter, In params) {
        return variantFormatter.apply(this, params);
    }

    /**
     * 转为任意格式
     *
     * @param variantFormatter 序列格式转换器
     * @param cache            输出缓冲区
     */
    public <Out> int apply(VariantFormatter<Void, Out> variantFormatter, VolumeByteStream cache) {
        return variantFormatter.apply(this, cache);
    }

    @Override
    public Variant clone() {
        return new Variant(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Variant)) {
            return false;
        }

        Variant variant = (Variant) o;
        return position == variant.position && chromosome.equals(variant.chromosome) && Arrays.equals(REF, variant.REF) && Arrays.equals(ALT, variant.ALT) && Arrays.equals(BEGs, variant.BEGs);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(chromosome, position);
        result = 31 * result + Arrays.hashCode(REF);
        result = 31 * result + Arrays.hashCode(ALT);
        result = 31 * result + Arrays.hashCode(BEGs);
        return result;
    }

    @Override
    public String toString() {
        if (chromosome == null || REF == null || ALT == null) {
            return "Variant{empty}";
        } else {
            return "Variant{" +
                    "chromosome='" + chromosome + '\'' +
                    ", position=" + position +
                    ", REF='" + new String(REF) + '\'' +
                    ", ALT='" + new String(ALT) + '\'' +
                    '}';
        }
    }

    @Override
    public int compareTo(Variant<T> o) {
        int status = ChromosomeTags.chromosomeSorter(this.chromosome, o.chromosome);
        if (status == 0) {
            status = Integer.compare(this.position, o.position);
            if (status == 0) {
                status = Integer.compare(this.getAlternativeAlleleNum(), o.getAlternativeAlleleNum());
                if (status == 0) {
                    for (int i = 0, l = Math.min(this.REF.length, o.REF.length); i < l; i++) {
                        status = Byte.compare(this.REF[i], o.REF[i]);
                        if (status != 0) {
                            return status;
                        }
                    }

                    if (this.REF.length < o.REF.length) {
                        return -1;
                    } else if (this.REF.length > o.REF.length) {
                        return 1;
                    } else {
                        for (int i = 0, l = Math.min(this.ALT.length, o.ALT.length); i < l; i++) {
                            status = Byte.compare(this.ALT[i], o.ALT[i]);
                            if (status != 0) {
                                return status;
                            }
                        }

                        status = Integer.compare(this.ALT.length, o.ALT.length);

                        if (status == 0) {
                            if (o.property instanceof Comparable && this.property instanceof Comparable) {
                                // 按照属性进行排序
                                return ((Comparable) this.property).compareTo(o.property);
                            }
                        }

                        return status;
                    }
                }
            }
        }

        return status;
    }
}