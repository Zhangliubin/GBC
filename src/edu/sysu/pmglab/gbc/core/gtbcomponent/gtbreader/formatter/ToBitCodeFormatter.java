package edu.sysu.pmglab.gbc.core.gtbcomponent.gtbreader.formatter;

import edu.sysu.pmglab.gbc.core.gtbcomponent.gtbreader.Variant;

public enum ToBitCodeFormatter implements VariantFormatter<byte[], int[]> {
    /**
     * 位编码转换器
     */
    INSTANCE;

    /**
     * Example: 位编码表
     * .|.: BEG 0, convert to 00, that is 0
     * 0|0: BEG 1, convert to 00, that is 0
     * 0|1: BEG 2, convert to 01, that is 1
     * 1|1: BEG 3, convert to 11, that is 3
     * 1|0: BEG 4, convert to 10, that is 2
     * static byte[] BIT_ENCODER = new byte[]{0, 0, 1, 3, 2};
     */

    @Override
    public int[] apply(Variant variant, byte[] encodedTable) {
        // 非对称编码信息
        int groupNum = variant.BEGs.length / 16 + (variant.BEGs.length % 16 == 0 ? 0 : 1);
        int resNum = variant.BEGs.length % 16;

        byte begCode;
        int[] bitCodes = new int[groupNum];

        for (int i = 0; i < (resNum == 0 ? groupNum : groupNum - 1); i++) {
            for (int j = 0; j < 16; j++) {
                begCode = variant.BEGs[(i << 4) + j];

                bitCodes[i] = (int) (((bitCodes[i] << 2) & 0xffffffffL) + encodedTable[begCode]);
            }
        }

        if (resNum != 0) {
            // 编码最后一个 bitcode
            int lastIndex = groupNum - 1;
            for (int i = 0; i < resNum; i++) {
                begCode = variant.BEGs[(lastIndex << 4) + i];
                bitCodes[lastIndex] = (int) (((bitCodes[lastIndex] << 2) & 0xffffffffL) + encodedTable[begCode]);
            }

            // 末尾也标记为缺失
            for (int i = 0; i < 16 - resNum; i++) {
                bitCodes[lastIndex] = (int) (((bitCodes[lastIndex] << 2) & 0xffffffffL) + encodedTable[0]);
            }
        }

        return bitCodes;
    }
}
