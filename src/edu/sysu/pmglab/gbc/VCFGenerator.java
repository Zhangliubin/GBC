package edu.sysu.pmglab.gbc;

import edu.sysu.pmglab.unifyIO.FileStream;

import java.io.File;
import java.io.IOException;
import java.util.Random;

/**
 * @Data :2021/07/13
 * @Author :suranyi
 * @Contact :suranyi.sysu@gamil.com
 * @Description :VCF 文件生成器
 */

enum Prop {
    /**
     * 概率配置
     */
    RANDOM(0.5f, 0.5f, 0.2f),
    DEFAULT(0.8f, 0.05f, 0.001f),
    ZERO(1.f, 1.0f, 0);

    /**
     * 位点以 0 作为高频突变的概率
     */
    final float majorAlleleFreq;

    /**
     * 等位基因概率 (假定相等)
     */
    final float alleleFreq;

    /**
     * 缺失基因型概率
     */
    final float missFreq;

    Prop(float majorAlleleFreq, float alleleFreq, float missFreq) {
        this.majorAlleleFreq = majorAlleleFreq;
        this.alleleFreq = alleleFreq;
        this.missFreq = missFreq;
    }
}

public class VCFGenerator {
    static final String outputDir = "./resource/simulate_vcf/";

    /**
     * 随机数发生器
     */
    static final Random random = new Random(0);

    static void generate(Prop prop, String chromosome, int variantNum, int subjectNum, boolean order) throws IOException {
        new File("./resource/simulate_vcf/").mkdir();

        // 4 线程 bgzip 压缩
        FileStream vcf = new FileStream(outputDir + prop.name().toLowerCase() + "simu" + +variantNum + "V_" + subjectNum + "S.chr" + chromosome + ".vcf.gz", FileStream.PARALLEL_BGZIP_WRITER_4);

        // 写入头文件信息
        vcf.write("##fileformat=VCFv4.2\n" +
                "##FILTER=<ID=PASS,Description=\"All filters passed\">\n" +
                "##source=/Users/suranyi/Desktop/blood.dna.gtb\n" +
                "##Version=<gbc_version=1.5,java_version=1.8.0_281,zstd_jni=1.4.9-5>\n" +
                "##contig=<ID=" + chromosome + ",length=248956422,URL=https://www.ncbi.nlm.nih.gov/grc/human/data?asm=GRCh38.p13>\n" +
                "##INFO=<ID=AC,Number=A,Type=Integer,Description=\"Allele count in genotypes\">\n" +
                "##INFO=<ID=AN,Number=1,Type=Integer,Description=\"Total number of alleles in called genotypes\">\n" +
                "##INFO=<ID=AF,Number=A,Type=Float,Description=\"Allele Frequency\">\n" +
                "##FORMAT=<ID=GT,Number=1,Type=String,Description=\"Genotype\">\n" +
                "#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\tFORMAT");

        // 写入样本名
        for (int i = 1; i <= subjectNum; i++) {
            vcf.write("\tTEST-" + i);
        }

        // 碱基序列
        String[] alleles = new String[]{"A", "C", "T", "G"};

        for (int i = 0; i < variantNum; i++) {
            int position;
            if (order) {
                position = 10 * (i + 1) + (1 + random.nextInt(10));
            } else {
                position = 1 + random.nextInt(2 << 29);
            }

            // 生成等位基因
            int allele_ind_1 = random.nextInt(4);
            int allele_ind_2 = random.nextInt(4);
            if (allele_ind_1 == allele_ind_2) {
                allele_ind_2 = allele_ind_2 == alleles.length - 1 ? 0 : allele_ind_2 + 1;
            }

            // 写入 non-genotype 部分
            vcf.write("\n" + chromosome + "\t" + position + "\t.\t" + alleles[allele_ind_1] + "\t" + alleles[allele_ind_2] + "\t.\t.\t.\tGT");

            // 生成主等位基因类型
            String majorAllele, minorAllele;
            if (random.nextFloat() < prop.majorAlleleFreq) {
                majorAllele = "0";
                minorAllele = "1";
            } else {
                majorAllele = "1";
                minorAllele = "0";
            }

            // 该位点以 0 为主
            for (int j = 0; j < subjectNum; j++) {
                vcf.write("\t");
                if (random.nextFloat() < prop.missFreq) {
                    vcf.write(".|.");
                    continue;
                }

                if (random.nextFloat() < prop.alleleFreq) {
                    vcf.write(majorAllele + "|");
                } else {
                    vcf.write(minorAllele + "|");
                }

                if (random.nextFloat() < prop.alleleFreq) {
                    vcf.write(minorAllele);
                } else {
                    vcf.write(majorAllele);
                }
            }
        }

        vcf.close();
    }

    public static void main(String[] args) {
        try {
//            for (int subjectNum : new int[]{1000, 2000, 4000, 6000, 8000, 10000, 15000, 20000, 30000, 50000, 100000, 150000, 200000, 300000, 500000}) {
//                generate(Prop.RANDOM, "1", 30000, subjectNum);
//            }

            for (int variantNum : new int[]{100000}) {
                // 少量位点无序
                //generateUnsorted(dev.Prop.ZERO, "1", variantNum, 1000, 10000, 10, 5);

                // 大量位点无序
                generate(Prop.RANDOM, "1", variantNum, 100, false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}