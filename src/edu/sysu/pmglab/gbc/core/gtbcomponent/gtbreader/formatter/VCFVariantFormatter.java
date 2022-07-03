package edu.sysu.pmglab.gbc.core.gtbcomponent.gtbreader.formatter;

import edu.sysu.pmglab.container.VolumeByteStream;
import edu.sysu.pmglab.easytools.ByteCode;
import edu.sysu.pmglab.easytools.ValueUtils;
import edu.sysu.pmglab.gbc.coder.decoder.BEGDecoder;
import edu.sysu.pmglab.gbc.constant.ChromosomeTags;
import edu.sysu.pmglab.gbc.core.gtbcomponent.gtbreader.Variant;

/**
 * @author suranyi
 */

public enum VCFVariantFormatter implements VariantFormatter<Void, byte[]> {
    /**
     * VCF 格式转换器 (包含 INFO 信息)
     */
    INSTANCE;

    @Override
    public byte[] apply(Variant variant) {
        VolumeByteStream cache = new VolumeByteStream(100);
        apply(variant, cache);
        return cache.values();
    }

    @Override
    public int apply(Variant variant, VolumeByteStream cache) {
        int originLength = cache.size();
        cache.writeSafety(variant.chromosome);
        cache.writeSafety(ByteCode.TAB);

        // pos 信息
        cache.writeSafety(ValueUtils.stringValueOfAndGetBytes(variant.position));
        cache.writeSafety(ByteCode.TAB);

        // id 信息
        cache.writeSafety(ByteCode.PERIOD);
        cache.writeSafety(ByteCode.TAB);

        // allele 信息
        cache.writeSafety(variant.REF);
        cache.writeSafety(ByteCode.TAB);
        cache.writeSafety(variant.ALT);
        cache.writeSafety(ByteCode.TAB);

        // qual 信息
        cache.writeSafety(ByteCode.PERIOD);
        cache.writeSafety(ByteCode.TAB);

        // filter 信息
        cache.writeSafety(ByteCode.PERIOD);
        cache.writeSafety(ByteCode.TAB);

        // info 信息
        int alleleCounts = variant.getAC();
        int validAllelesNum = variant.getAN();
        cache.writeSafety(ByteCode.AC_STRING);
        cache.writeSafety(ValueUtils.stringValueOfAndGetBytes(alleleCounts));
        cache.writeSafety(ByteCode.AN_STRING);
        cache.writeSafety(ValueUtils.stringValueOfAndGetBytes(validAllelesNum));

        cache.writeSafety(ByteCode.AF_STRING);
        if (validAllelesNum == 0) {
            cache.writeSafety(ByteCode.PERIOD);
        } else {
            cache.writeSafety(ValueUtils.stringValueOfAndGetBytes((double) alleleCounts / validAllelesNum, 6));
        }
        cache.writeSafety(ByteCode.TAB);

        // format 信息
        cache.writeSafety(ByteCode.GT_STRING);

        // genotype 信息
        BEGDecoder decoder = BEGDecoder.getDecoder(variant.phased);
        int ploidy = ChromosomeTags.getPloidy(variant.chromosome);
        for (byte code : variant.BEGs) {
            cache.writeSafety(ByteCode.TAB);
            cache.writeSafety(decoder.decode(ploidy, code));
        }
        return cache.size() - originLength;
    }
}
