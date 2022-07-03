package edu.sysu.pmglab.gbc.constant;

import edu.sysu.pmglab.check.Assert;
import edu.sysu.pmglab.container.File;
import edu.sysu.pmglab.container.VolumeByteStream;
import edu.sysu.pmglab.container.array.Array;
import edu.sysu.pmglab.container.array.BaseArray;
import edu.sysu.pmglab.easytools.ArrayUtils;
import edu.sysu.pmglab.easytools.ByteCode;
import edu.sysu.pmglab.gbc.core.exception.GTBComponentException;
import edu.sysu.pmglab.unifyIO.FileStream;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

/**
 * @author :suranyi
 * @description : 全局染色体标签规范
 */

public class ChromosomeTags {
    private static final String[] prefixes = new String[]{"chr"};
    public static final File DEFAULT_FILE = new File("/contig/human/hg38.p13", true);
    private static File activeFile = null;

    private static final HashMap<String, ChromosomeTag> chromosomeIndexes = new HashMap<>();
    private static final BaseArray<ChromosomeTag> chromosomes = new Array<>(ChromosomeTag[].class, true);

    static {
        try {
            load(DEFAULT_FILE);
        } catch (IOException e) {
            for (int i = 0; i < 255; i++) {
                ChromosomeTag tag = new ChromosomeTag(i, String.valueOf(i + 1), 2, -1);
                chromosomeIndexes.put(tag.chromosomeString, tag);
                chromosomes.add(tag);
                activeFile = null;
            }
        }
    }

    /**
     * 初始化
     */
    private ChromosomeTags() {
    }

    /**
     * 获取当前的文件名
     */
    public static File getActiveFile() {
        return activeFile;
    }

    /**
     * 从 vcf 文件中构建 contig 文件
     */
    public static void build(File inputFile, File outputFile, boolean deep) throws IOException {
        Assert.that(!inputFile.equals(outputFile), "inputFileName = outputFileName");

        FileStream in = new FileStream(inputFile, inputFile.withExtension(".gz") ? FileStream.BGZIP_READER : FileStream.DEFAULT_READER);
        FileStream out = new FileStream(outputFile, FileStream.DEFAULT_WRITER);

        VolumeByteStream outputCache = new VolumeByteStream();
        VolumeByteStream lineCache = new VolumeByteStream();
        boolean searchRef = false;

        if (deep) {
            Array<byte[]> chromosomes = new Array<>();
            out:
            while (in.readLine(lineCache) != -1) {
                if (!lineCache.startWith(ByteCode.NUMBER_SIGN)) {
                    // 不是以 # 开头
                    byte[] chromosome = lineCache.getNBy(ByteCode.TAB, 0);

                    for (int i = 0; i < chromosomes.size(); i++) {
                        if (Arrays.equals(chromosomes.get(i), chromosome)) {
                            lineCache.reset();
                            continue out;
                        }
                    }

                    chromosomes.add(chromosome);
                    outputCache.writeSafety(ByteCode.NEWLINE);
                    outputCache.writeSafety(chromosome);
                    outputCache.writeSafety(ByteCode.COMMA);
                    outputCache.writeSafety(ByteCode.TWO);
                    outputCache.writeSafety(ByteCode.COMMA);
                    outputCache.writeSafety(ByteCode.ZERO);
                }
                lineCache.reset();
            }
        } else {
            byte[] contigStartFlag = "##contig=".getBytes();
            while (in.readLine(lineCache) != -1) {
                if (lineCache.startWith(ByteCode.NUMBER_SIGN)) {
                    if (!searchRef && lineCache.startWith(ByteCode.REFERENCE_STRING)) {
                        out.write(lineCache);
                        searchRef = true;
                    } else {
                        if (lineCache.startWith(contigStartFlag)) {
                            // 捕获 contig 信息
                            byte[] chromosome = null;
                            byte[] length = null;

                            for (int i = contigStartFlag.length; i < lineCache.size(); i++) {
                                if (lineCache.startWith(i, ByteCode.ID_STRING)) {
                                    if (lineCache.startWith(i + 3, ByteCode.CHR_STRING)) {
                                        i = i + 3;
                                    }
                                    for (int j = i + 3; j < lineCache.size(); j++) {
                                        if (lineCache.cacheOf(j) == ByteCode.COMMA || lineCache.cacheOf(j) == 0x3e) {
                                            chromosome = lineCache.cacheOf(i + 3, j);
                                            break;
                                        }
                                    }
                                }

                                if (lineCache.startWith(i, "length=".getBytes())) {
                                    for (int j = i + 7; j < lineCache.size(); j++) {
                                        if (lineCache.cacheOf(j) == ByteCode.COMMA || lineCache.cacheOf(j) == 0x3e) {
                                            length = lineCache.cacheOf(i + 7, j);
                                            break;
                                        }
                                    }
                                }
                            }

                            if (chromosome != null) {
                                outputCache.writeSafety(ByteCode.NEWLINE);
                                outputCache.writeSafety(chromosome);

                                outputCache.writeSafety(ByteCode.COMMA);
                                outputCache.writeSafety(ByteCode.TWO);

                                outputCache.writeSafety(ByteCode.COMMA);
                                if (length != null) {
                                    outputCache.writeSafety(length);
                                } else {
                                    outputCache.writeSafety(ByteCode.ZERO);
                                }
                            }
                        }
                    }
                } else {
                    break;
                }
                lineCache.reset();
            }
        }

        if (searchRef) {
            out.write("\n#chromosome,ploidy,length");
        } else {
            out.write("#chromosome,ploidy,length");
        }
        out.write(outputCache);
        in.close();
        out.close();
    }

    /**
     * 为染色体标签添加前缀串
     */
    public static void addPrefixChromosome() {
        for (String prefix : prefixes) {
            for (ChromosomeTag tag : chromosomes) {
                chromosomeIndexes.put(prefix + tag.chromosomeString, tag);
            }
        }
    }

    /**
     * 加载 contig 资源文件
     *
     * @param resourceFile 资源文件对象
     */
    public static void load(File resourceFile) throws IOException {
        if (resourceFile == null || (activeFile != null && activeFile.equals(resourceFile))) {
            // 文件为空, 或者没有修改时, 不做重载
            return;
        }


        // 打开文件资源
        FileStream fileStream = resourceFile.open();
        VolumeByteStream lineCache = new VolumeByteStream(128);
        fileStream.readLine(lineCache);

        // 解析注释行
        String reference = null;
        while ((lineCache.cacheOf(0) == ByteCode.NUMBER_SIGN) && (lineCache.cacheOf(1) == ByteCode.NUMBER_SIGN)) {
            if (lineCache.startWith(ByteCode.REFERENCE_STRING)) {
                reference = new String(lineCache.getCache(), ByteCode.REFERENCE_STRING.length, lineCache.size() - ByteCode.REFERENCE_STRING.length);
            }

            lineCache.reset();
            fileStream.readLine(lineCache);
        }

        // 解析正文字段
        int count = 0;
        BaseArray<ChromosomeTag> loadInChromosomes = new Array<>(ChromosomeTag[].class, true);
        String[] fields = new String(lineCache.cacheOf(1, lineCache.size())).split(",");
        int chromosomeIndex = ArrayUtils.indexOf(fields, "chromosome");
        int ploidyIndex = ArrayUtils.indexOf(fields, "ploidy");
        int lengthIndex = ArrayUtils.indexOf(fields, "length");

        if (chromosomeIndex == -1) {
            throw new GTBComponentException("doesn't match to standard Chromosome Config file");
        }

        lineCache.reset();
        while (fileStream.readLine(lineCache) != -1) {
            String[] groups = new String(lineCache.values()).split(",");
            loadInChromosomes.add(new ChromosomeTag(count, groups[chromosomeIndex], ploidyIndex == -1 ? 2 : Integer.parseInt(groups[ploidyIndex]), ploidyIndex == -1 ? 2 : lengthIndex == -1 ? -1 : Integer.parseInt(groups[lengthIndex]), reference));
            count++;
            lineCache.reset();
        }

        fileStream.close();

        if (count > 256) {
            throw new GTBComponentException("too much chromosome input (> 256)");
        }

        ChromosomeTags.chromosomes.clear();
        ChromosomeTags.chromosomeIndexes.clear();
        ChromosomeTags.chromosomes.addAll(loadInChromosomes);
        for (int i = 0; i < loadInChromosomes.size(); i++) {
            ChromosomeTag tag = loadInChromosomes.get(i);
            if (!ChromosomeTags.chromosomeIndexes.containsKey(tag.chromosomeString)) {
                ChromosomeTags.chromosomeIndexes.put(tag.chromosomeString, tag);
            }
        }
        activeFile = resourceFile;

        // 添加前缀串
        addPrefixChromosome();
    }

    /**
     * 根据染色体的字符串值获取对应的染色体信息
     *
     * @param chromosomeString 染色体字符串
     */
    public static ChromosomeTag get(String chromosomeString) {
        if (chromosomeIndexes.containsKey(chromosomeString)) {
            return chromosomeIndexes.get(chromosomeString);
        }

        throw new UnsupportedOperationException("unable to identify chromosome=" + chromosomeString + " (supported: " + Arrays.toString(ChromosomeTags.supportedChromosomeList()) + ")");
    }

    /**
     * 根据染色体的字符串值获取对应的染色体信息
     *
     * @param chromosomeString 染色体字符串
     */
    public static boolean contain(String chromosomeString) {
        return chromosomeIndexes.containsKey(chromosomeString);
    }

    /**
     * 根据本类中定义的染色体信息类顺序，获取对应的索引值
     *
     * @param chromosome 染色体索引值
     */
    public static byte[] getBytes(String chromosome) {
        return get(chromosome).chromosomeByteArray;
    }

    /**
     * 根据本类中定义的染色体信息类顺序，获取对应的字符串值
     *
     * @param chromosomeIndex 染色体索引值
     */
    public static String getString(int chromosomeIndex) {
        return chromosomes.get(chromosomeIndex).chromosomeString;
    }

    /**
     * 根据本类中定义的染色体信息类顺序，获取对应的倍体
     *
     * @param chromosome 染色体索引值
     */
    public static int getPloidy(String chromosome) {
        return chromosomeIndexes.get(chromosome).ploidy;
    }

    /**
     * 获取染色体索引对应的 header 信息
     *
     * @param chromosome 染色体索引信息
     */
    public static String getHeader(String chromosome) {
        String header = "\n##contig=<ID=" + chromosome;

        ChromosomeTag tag = chromosomeIndexes.get(chromosome);
        if (tag.length > 0) {
            header += ",length=" + tag.length;
        }

        if (tag.reference != null) {
            header += ",URL=" + tag.reference;
        }

        header += ">";

        return header;
    }

    /**
     * 获取本类支持全部的染色体字符串
     */
    public static String[] supportedChromosomeList() {
        String[] list = new String[chromosomes.size()];
        for (int i = 0; i < chromosomes.size(); i++) {
            list[i] = chromosomes.get(i).chromosomeString;
        }

        return list;
    }

    /**
     * 将字符串形式的染色体数据批量生成索引值
     *
     * @return 识别出来的染色体索引值
     */
    public static int[] getIndexes(String... chromosomes) {
        int[] chromosomeIndexes = new int[chromosomes.length];

        for (int i = 0; i < chromosomes.length; i++) {
            chromosomeIndexes[i] = ChromosomeTags.getIndex(chromosomes[i]);
        }

        return chromosomeIndexes;
    }

    /**
     * 将字符串形式的染色体数据批量生成索引值
     *
     * @param chromosomeString 获取染色体编号
     * @return 识别出来的染色体索引值
     */
    public static int getIndex(String chromosomeString) {
        return get(chromosomeString).chromosomeIndex;
    }

    /**
     * 获取染色体排序器, 取决于 contig 文件的染色体排列顺序
     */
    public static int chromosomeSorter(String chromosome1, String chromosome2) {
        return Integer.compare(getIndex(chromosome1), getIndex(chromosome2));
    }
}
