# Sort Variants by Coordinates {#SortMode}

Use the following command to sort variants by coordinate from the GTB:

```bash
sort <input> -o <output> [options]
```

Generally, VCF files are ordered and this feature can be ignored. When you convert the reference version (e.g., from hg19 to hg38), the coordinates of the variants may change, causing the file to become unordered.

## Program Options {#Options}

```bash
Usage: sort <input> -o <output> [options]
Options:
  --contig      Specify the corresponding contig file.
                default: /contig/human/hg38.p13
                format: --contig <file> (Exists,File,Inner)
  *--output,-o  Set the output file.
                format: --output <file>
  --threads,-t  Set the number of threads.
                default: 4
                format: --threads <int> (>= 1)
  --subject,-s  Extract the information of the specified subjects. Subject name 
                can be stored in a file with ',' delimited form, and pass in 
                via '-s @file'.
                format: --subject <string>,<string>,...
  --yes,-y      Overwrite output file without asking.
GTB Archive Options:
  --phased,-p          Force-set the status of the genotype. (same as the GTB 
                       basic information by default)
                       format: --phased [true/false]
  --biallelic          Split multiallelic variants into multiple biallelic 
                       variants. 
  --simply             Delete the alternative alleles (ALT) with allele counts 
                       equal to 0.
  --blockSizeType,-bs  Set the maximum size=2^(7+x) of each block. (-1 means 
                       auto-adjustment) 
                       default: -1
                       format: --blockSizeType <int> (-1 ~ 7)
  --no-reordering,-nr  Disable the Approximate Minimum Discrepancy Ordering 
                       (AMDO) algorithm.
  --windowSize,-ws     Set the window size of the AMDO algorithm.
                       default: 24
                       format: --windowSize <int> (1 ~ 131072)
  --compressor,-c      Set the basic compressor for compressing processed data.
                       default: ZSTD
                       format: --compressor <string> ([ZSTD/LZMA/GZIP] or 
                       [0/1/2] (ignoreCase))
  --level,-l           Compression level to use when basic compressor works. 
                       (ZSTD: 0~22, 3 as default; LZMA: 0~9, 3 as default; 
                       GZIP: 0~9, 5 as default)
                       default: -1
                       format: --level <int> (-1 ~ 31)
  --readyParas,-rp     Import the template parameters (-p, -bs, -c, -l) from an 
                       external GTB file.
                       format: --readyParas <file> (Exists,File)
  --seq-ac             Exclude variants with the alternate allele count (AC) 
                       per variant out of the range [minAc, maxAc].
                       format: --seq-ac <int>-<int> (>= 0)
  --seq-af             Exclude variants with the alternate allele frequency 
                       (AF) per variant out of the range [minAf, maxAf].
                       format: --seq-af <double>-<double> (0.0 ~ 1.0)
  --seq-an             Exclude variants with the non-missing allele number (AN) 
                       per variant out of the range [minAn, maxAn].
                       format: --seq-an <int>-<int> (>= 0)
  --max-allele         Exclude variants with alleles over --max-allele.
                       default: 15
                       format: --max-allele <int> (2 ~ 15)
```

## Example {#Examples}

Use the GBC to compress the unordered VCF file `./example/randomsimu100000V_100S.chr1.vcf.gz`:

```bash
# Linux or MacOS
docker run -v `pwd`:`pwd` -w `pwd` --rm -it -m 500m gbc \
build ./example/randomsimu100000V_100S.chr1.vcf.gz -o ./example/randomsimu100000V_100S.chr1.gtb -y

# Windows
docker run -v %cd%:%cd% -w %cd% --rm -it -m 500m gbc build ./example/randomsimu100000V_100S.chr1.vcf.gz -o ./example/randomsimu100000V_100S.chr1.gtb -y
```

Then, use `show` to print the summary information of `./example/randomsimu100000V_100S.chr1.gtb`:

```bash
# Linux or MacOS
docker run -v `pwd`:`pwd` -w `pwd` --rm -it -m 500m gbc \
show ./example/randomsimu100000V_100S.chr1.gtb

# Windows
docker run -v %cd%:%cd% -w %cd% --rm -it -m 500m gbc show ./example/randomsimu100000V_100S.chr1.gtb
```

Here, the terminal prints the following message, note that this file is an `unordered` file:

```bash
Summary of GTB File:
  GTB File Name: /Users/suranyi/Documents/project/GBC/GBC-1.1/example/randomsimu100000V_100S.chr1.gtb
  GTB File Size: 2.764 MB
  Suggest To BGZF: false
  Phased: false
  Ordered GTB: false
  BlockSize: 16384 (-bs 7)
  Compression Level: 3 (ZSTD)
  Dimension of Genotypes: 1 chromosome, 100000 variants and 100 subjects
```

Sort the current GTB file using the `sort` mode:                                                                                                  

```bash
# Linux or MacOS
docker run -v `pwd`:`pwd` -w `pwd` --rm -it -m 500m gbc \
sort ./example/randomsimu100000V_100S.chr1.gtb -o ./example/randomsimu100000V_100S.chr1.order.gtb

# Windows
docker run -v %cd%:%cd% -w %cd% --rm -it -m 500m gbc sort ./example/randomsimu100000V_100S.chr1.gtb -o ./example/randomsimu100000V_100S.chr1.order.gtb
```

