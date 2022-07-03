package edu.sysu.pmglab.gbc.core.gtbcomponent.gtbreader.formatter;

import edu.sysu.pmglab.container.VolumeByteStream;
import edu.sysu.pmglab.easytools.ByteCode;
import edu.sysu.pmglab.easytools.ValueUtils;
import edu.sysu.pmglab.gbc.core.gtbcomponent.gtbreader.Variant;

/**
 * @author suranyi
 */

public enum VCFSiteVariantFormatter implements VariantFormatter<Void, byte[]> {
    /**
     * VCF 格式转换器 (不包含基因型)
     */
    INSTANCE;

    @Override
    public byte[] apply(Variant variant) {
        VolumeByteStream cache = new VolumeByteStream(estimateSize(variant));
        apply(variant, cache);
        return cache.values();
    }

    @Override
    public int apply(Variant variant, VolumeByteStream cache) {
        int originLength = cache.size();
        cache.makeSureCapacity(estimateSize(variant));

        cache.write(variant.chromosome);
        cache.write(ByteCode.TAB);

        // pos 信息
        cache.write(ValueUtils.stringValueOfAndGetBytes(variant.position));
        cache.write(ByteCode.TAB);

        // id 信息
        cache.write(ByteCode.PERIOD);
        cache.write(ByteCode.TAB);

        // allele 信息
        cache.write(variant.REF);
        cache.write(ByteCode.TAB);
        cache.write(variant.ALT);
        cache.write(ByteCode.TAB);

        // qual 信息
        cache.write(ByteCode.PERIOD);
        cache.write(ByteCode.TAB);

        // filter 信息
        cache.write(ByteCode.PERIOD);
        cache.write(ByteCode.TAB);

        // info 信息
        int alleleCounts = variant.getAC();
        int validAllelesNum = variant.getAN();
        cache.write(ByteCode.AC_STRING);
        cache.write(ValueUtils.stringValueOfAndGetBytes(alleleCounts));
        cache.write(ByteCode.AN_STRING);
        cache.write(ValueUtils.stringValueOfAndGetBytes(validAllelesNum));

        cache.write(ByteCode.AF_STRING);
        if (validAllelesNum == 0) {
            cache.write(ByteCode.PERIOD);
        } else {
            cache.write(ValueUtils.stringValueOfAndGetBytes((double) alleleCounts / validAllelesNum, 6));
        }
        cache.write(ByteCode.TAB);

        // format 信息
        cache.writeSafety(ByteCode.PERIOD);

        return cache.size() - originLength;
    }

    int estimateSize(Variant variant) {
        return variant.chromosome.length() + ValueUtils.byteArrayOfValueLength(variant.position) + variant.REF.length + variant.ALT.length + 50;
    }
}
