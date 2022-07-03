# LD Calculation for GTB {#LDMode}

GBC integrates the calculation of LD coefficients. Use the following command to calculate LD coefficients for GTB file:

```bash
ld <input> -o <output> [options]
```

## Program Options {#Options}

```bash
Usage: ld <input> -o <output> [options]
Output Options:
  --contig      Specify the corresponding contig file.
                default: /contig/human/hg38.p13
                format: --contig <file> (Exists,File,Inner)
  *--output,-o  Set the output file.
                format: --output <file>
  --o-text      Output LD file in text format. (this command will be executed 
                automatically if '--o-bgz' is not passed in and the output file 
                specified by '-o' is not end with '.gz')
  --o-bgz       Output LD file in bgz format. (this command will be executed 
                automatically if '--o-text' is not passed in and the output 
                file specified by '-o' is end with '.gz')
  --level,-l    Set the compression level. (Execute only if --o-bgz is passed 
                in) 
                default: 5
                format: --level <int> (0 ~ 9)
  --threads,-t  Set the number of threads.
                default: 4
                format: --threads <int> (>= 1)
  --yes,-y      Overwrite output file without asking.
LD Calculation Options:
  --hap-ld,--hap-r2    Calculate pairwise the linkage disequilibrium.
  --geno-ld,--gene-r2  Calculate pairwise the genotypic correlation.
  --window-bp,-bp      The maximum number of physical bases between the 
                       variants being calculated for LD.
                       default: 10000
                       format: --window-bp <int> (>= 1)
  --min-r2             Exclude pairs with R2 values less than --min-r2.
                       default: 0.2
                       format: --min-r2 <double> (0.0 ~ 1.0)
  --maf                Exclude variants with the minor allele frequency (MAF) 
                       per variant < maf.
                       default: 0.05
                       format: --maf <double> (0.0 ~ 0.5)
  --subject,-s         Calculate the LD for the specified subjects. Subject 
                       name can be stored in a file with ',' delimited form, 
                       and pass in via '-s @file'.
                       format: --subject <string>,<string>,...
  --range,-r           Calculate the LD by specified position range.
                       format: --range <chrom>:<minPos>-<maxPos> 
                       <chrom>:<minPos>-<maxPos> ...
```

## Example {#Examples}

Calculate the LD coefficients for the `EAS hg38` dataset.

```bash
# Linux or MacOS
docker run -v `pwd`:`pwd` -w `pwd` --rm -it -m 4g gbc \
ld ./example/EAS.gtb -o ./example/EAS.ld.gz -t 8 -y

# Windows
docker run -v %cd%:%cd% -w %cd% --rm -it -m 4g gbc ld ./example/EAS.gtb -o ./example/EAS.ld.gz -t 8 -y
```

