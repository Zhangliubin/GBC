# Extract Genotypes {#ExtractMode}

Use the following command to extract genotypes from the GTB:

```bash
extract <input> -o <output> [options]
```

- If the `[options]` contains `--o-gtb` or the output file specified by `-o` is end with `.gtb`, the program will output genotypes in GTB format.
- If the `[options]` contains `--o-bgz` or the output file specified by `-o` is end with `.gz`, the program will output genotypes in BGZIP-compressed VCF format.
- If the `[options]` contains `--o-vcf` or the output file specified by `-o` is not end with `.gz` or `.gtb`, the program will output genotypes in VCF format.

In general, bioinformatics tools (such as PLINK) are compatible with the BGZIP-compressed VCF file format, and we recommend that users use the `--o-bgz` or `--o-gtb` format as output to enhance the parallel output performance of the program.

## Program Options {#Options}

```bash
Usage: extract <input> -o <output> [options]
Output Options:
  --contig      Specify the corresponding contig file.
                default: /contig/human/hg38.p13
                format: --contig <file> (Exists,File,Inner)
  *--output,-o  Set the output file.
                format: --output <file>
  --o-text      Output VCF file in text format. (this command will be executed automatically if 
                '--o-bgz' or '--o-gtb' is not passed in and the output file specified by '-o' is 
                not end with '.gz' or '.gtb')
  --o-bgz       Output VCF file in bgz format. (this command will be executed automatically if 
                '--o-text' or '--o-gtb' is not passed in and the output file specified by '-o' is 
                end with '.gz')
  --o-gtb       Output VCF file in gtb format. (this command will be executed automatically if 
                '--o-text' or '--o-bgz' is not passed in and the output file specified by '-o' is 
                end with '.gtb')
  --level,-l    Compression level to use when basic compressor works. (ZSTD: 0~22, 3 as default; 
                LZMA: 0~9, 3 as default; BGZIP: 0~9, 5 as default)
                default: -1
                format: --level <int> (-1 ~ 31)
  --no-clm      Parallel output is not controlled using the cyclic locking mechanism (CLM). With 
                this parameter, parallel output means output to multiple temporary files and 
                finally concatenating them together.
  --threads,-t  Set the number of threads.
                default: 4
                format: --threads <int> (>= 1)
  --phased,-p   Force-set the status of the genotype. (same as the GTB basic information by 
                default) 
                format: --phased [true/false]
  --hideGT,-hg  Do not output the sample genotypes (only CHROM, POS, REF, ALT, AC, AN, AF).
  --yes,-y      Overwrite output file without asking.
GTB Archive Options:
  --biallelic          Split multiallelic variants into multiple biallelic variants.
  --simply             Delete the alternative alleles (ALT) with allele counts equal to 0.
  --blockSizeType,-bs  Set the maximum size=2^(7+x) of each block. (-1 means auto-adjustment)
                       default: -1
                       format: --blockSizeType <int> (-1 ~ 7)
  --no-reordering,-nr  Disable the Approximate Minimum Discrepancy Ordering (AMDO) algorithm.
  --windowSize,-ws     Set the window size of the AMDO algorithm.
                       default: 24
                       format: --windowSize <int> (1 ~ 131072)
  --compressor,-c      Set the basic compressor for compressing processed data.
                       default: ZSTD
                       format: --compressor <string> ([ZSTD/LZMA/GZIP] or [0/1/2] (ignoreCase))
  --readyParas,-rp     Import the template parameters (-p, -bs, -c, -l) from an external GTB file.
                       format: --readyParas <file> (Exists,File)
Subset Selection Options:
  --subject,-s   Extract the information of the specified subjects. Subject name can be stored in a 
                 file with ',' delimited form, and pass in via '-s @file'.
                 format: --subject <string>,<string>,...
  --range,-r     Extract the information by position range.
                 format: --range <chrom>:<minPos>-<maxPos> <chrom>:<minPos>-<maxPos> ...
  --random       Extract the information by position. (An inputFile is needed here, with each line 
                 contains 'chrom,position' or 'chrom position'.
                 format: --random <file>
  --retain-node  Extract variants in the specified coordinate range of the specified chromosome.
                 format: --retain-node <string>:<int>-<int> <string>:<int>-<int> ...
  --seq-ac       Exclude variants with the alternate allele count (AC) per variant out of the range 
                 [minAc, maxAc].
                 format: --seq-ac <int>-<int> (>= 0)
  --seq-af       Exclude variants with the alternate allele frequency (AF) per variant out of the 
                 range [minAf, maxAf].
                 format: --seq-af <double>-<double> (0.0 ~ 1.0)
  --seq-an       Exclude variants with the non-missing allele number (AN) per variant out of the 
                 range [minAn, maxAn].
                 format: --seq-an <int>-<int> (>= 0)
  --max-allele   Exclude variants with alleles over --max-allele.
                 default: 15
                 format: --max-allele <int> (2 ~ 15)
```

## Example {#Examples}

Use the GBC to decompress the example file `. /example/assoc.hg19.gtb` and set the following properties.

- Store the genotype as unphased.
- Extract the variants with $$\text{POS}\ge 1000000$$.
- Extract the variants with $$\text{AF}\in[0.4, 0.6]$$.
- Extract the genotypes with sample names NA18963,NA18977,HG02401,HG02353,HG02064.

The commands to complete the task are as follows:

```bash
# Linux or MacOS
docker run -v `pwd`:`pwd` -w `pwd` --rm -it -m 4g gbc \
extract ./example/assoc.hg19.gtb -o ./example/assoc.hg19.extract.vcf \
-p true -r 1:1000000- --seq-af 0.4-0.6 -s NA18963,NA18977,HG02401,HG02353,HG02064 -y

# Windows
docker run -v %cd%:%cd% -w %cd% --rm -it -m 4g gbc extract ./example/assoc.hg19.gtb -o ./example/assoc.hg19.extract.vcf -p true -r 1:1000000- --seq-af 0.4-0.6 -s NA18963,NA18977,HG02401,HG02353,HG02064 -y
```
