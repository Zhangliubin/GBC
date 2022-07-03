# Align Coordinates and Base Labels {#AlleleCheckMode}

When merging genotypes from different batches, the variants that with the same coordinate may introduce confusion (e.g., batch 1 uses forward-stranded DNA and batch 2 uses reverse-stranded DNA). With the same coordinates and base complementarity (e.g., [A, T] and [C, G]), GBC designs three functions to identify inconsistent allele labels:

- Check for allele frequency: the difference between the allele frequency of variant 1 and the allele frequency of variant 2 is less than the threshold (i.e., $$\mid \text{AF}_1-\text{AF}_2 \mid<\text{gap}$$). This will work for variants with minor allele frequencies much less than 0.5 (say 0.3).
- Check for allele count: $$2\times2$$ column tables were constructed using the number of reference alleles at a variant of two batches to be merged. The chi-square tests were performed. If the hypothesis test rejects the $$H_0$$ hypothesis (i.e., the allele frequencies of the two variants are identical), then the variants in different batches cannot be considered potentially identical. Note that this will not be suitable for the scenario that the two batches are used for cases and controls, respectively.
- Check for LD pattern: identifies other variants in which the absolute value of the genotypic correlation is over a threshold (say, 0.8) in two batches separately. We then count the positive signs of the correlation coefficients in the two batches. If the numbers of signs are very different between the two batches, the allele labels should be flipped; otherwise, the allele labels are not flipped. This function can be used for variants with minor allele frequencies close to 0.5.

Use the following command to correct for potential complementary strand errors:

```bash
allele-check <template_input> <input> -o <output> [options]
```

## Program Options {#Options}

```bash
Usage: allele-check <template_input> <input> -o <output> [options]
Options:
  --contig      Specify the corresponding contig file.
                default: /contig/human/hg38.p13
                format: --contig <file> (Exists,File,Inner)
  *--output,-o  Set the output file.
                format: --output <file>
  --threads,-t  Set the number of threads.
                default: 4
                format: --threads <int> (>= 1)
  --union       Method for handing coordinates in different files (union or 
                intersection, and intersection is the default), the missing 
                genotype is replaced by '.'.
  --yes,-y      Overwrite output file without asking.
Alignment Coordinate Options:
  --p-value              Correct allele labels of rare variants (minor allele 
                         frequency < --maf) with the p-value of chi^2 test >= 
                         --p-value. 
                         default: 0.05
                         format: --p-value <double> (1.0E-6 ~ 0.5)
  --freq-gap             Correct allele labels of rare variants (minor allele 
                         frequency < --maf) with the allele frequency gap <= 
                         --freq-gap. 
                         format: --freq-gap <double> (1.0E-6 ~ 0.5)
  --no-ld                By default, correct allele labels of common variants 
                         (minor allele frequency >= --maf) using the ld pattern 
                         in different files. Disable this function with option 
                         '--no-ld'. 
  --min-r                Exclude pairs with genotypic LD correlation |R| values 
                         less than --min-r.
                         default: 0.8
                         format: --min-r <double> (0.5 ~ 1.0)
  --flip-scan-threshold  Variants with flipped ld patterns (strong correlation 
                         coefficients of opposite signs) that >= threshold 
                         ratio will be corrected.
                         default: 0.8
                         format: --flip-scan-threshold <double> (0.5 ~ 1.0)
  --maf                  For common variants (minor allele frequency >= --maf) 
                         use LD to identify inconsistent allele labels.
                         default: 0.05
                         format: --maf <double> (0.0 ~ 0.5)
  --window-bp,-bp        The maximum number of physical bases between the 
                         variants being calculated for LD.
                         default: 10000
                         format: --window-bp <int> (>= 1)
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

Download the `EAS hg38` dataset from `http://pmglab.top/genotypes` and use the dataset as a template file to examine the allele tags of the local exome sequencing variants `SNP.gtb`:

```bash
# Linux or MacOS
docker run -v `pwd`:`pwd` -w `pwd` --rm -it -m 4g gbc \
allele-check ./example/EAS.gtb ./example/SNP.gtb -o ./example/SNP.checked.gtb --seq-af 0.000001-0.999999 -y

# Windows
docker run -v %cd%:%cd% -w %cd% --rm -it -m 4g gbc allele-check ./example/EAS.gtb ./example/SNP.gtb -o ./example/SNP.checked.gtb --seq-af 0.000001-0.999999 -y
```

Here, the terminal prints the following message:

```shell
2022-06-27 01:16:02 INFO  [ThreadPool-thread-1] AlleleCheck chr11:244961 TempREF=G TempALT=C TempAF=0.8035714285714286 REF=G ALT=C AF=0.6944444444444444 -> REF=C ALT=G
2022-06-27 01:16:02 INFO  [ThreadPool-thread-1] AlleleCheck chr11:251057 TempREF=C TempALT=G TempAF=0.08630952380952381 REF=C ALT=G AF=0.22093023255813954 -> REF=G ALT=C
2022-06-27 01:16:04 INFO  [ThreadPool-thread-1] AlleleCheck chr17:42969194 TempREF=C TempALT=G TempAF=0.9742063492063492 REF=C ALT=G AF=0.02608695652173913 -> REF=G ALT=C
2022-06-27 01:16:06 INFO  [ThreadPool-thread-2] AlleleCheck chr1:1041823 TempREF=G TempALT=C TempAF=0.9990079365079365 REF=G ALT=C AF=0.0045045045045045045 -> REF=C ALT=G
```

