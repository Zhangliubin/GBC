# Prune GTB Tree {#PruneMode}

Use the following command to prune the GTB Tree:

```bash
prune <input> -o <output> [options]
```

Compared with `extract`, `prune` does not need to decompress any data for node extraction or deletion, which is faster and less memory-intensive, and all operations can be completed in seconds.

## Program Options {#Options}

```bash
Usage: prune <input> -o <output> [options]
Options:
  --contig        Specify the corresponding contig file.
                  default: /contig/human/hg38.p13
                  format: --contig <file> (Exists,File,Inner)
  *--output,-o    Set the output file.
                  format: --output <file>
  --yes,-y        Overwrite output file without asking.
  --delete-node   Delete the specified GTBNodes.
                  format: --delete-node <string>:<int>,<int>,... <string>:<int>,<int>,... ...
  --retain-node   Retain the specified GTBNodes.
                  format: --retain-node <string>:<int>,<int>,... <string>:<int>,<int>,... ...
  --delete-chrom  Delete the specified Chromosomes.
                  format: --delete-chrom <string>,<string>,...
  --retain-chrom  Retain the specified Chromosomes.
                  format: --retain-chrom <string>,<string>,...
```

## Example {#Examples}

Use the GBC to extract the sex chromosomes (chrX and chrY) of `1000GP3.gtb`.

```bash
# Linux or MacOS
docker run -v `pwd`:`pwd` -w `pwd` --rm -it -m 4g gbc \
prune ./example/1000GP3.gtb -o ./example/1000GP3.chrXY.gtb \
--retain-chrom X,Y \
-y

# Windows
docker run -v %cd%:%cd% -w %cd% --rm -it -m 4g gbc prune ./example/1000GP3.gtb -o ./example/1000GP3.chrXY.gtb --retain-chrom X,Y -y
```

View the summary information of extracted GTB file:

```bash
# Linux or MacOS
docker run -v `pwd`:`pwd` -w `pwd` --rm -it -m 4g gbc \
show ./example/1000GP3.chrXY.gtb --add-tree

# Windows
docker run -v %cd%:%cd% -w %cd% --rm -it -m 4g gbc show ./example/1000GP3.chrXY.gtb --add-tree
```

Here, the terminal prints the following message:

```bash
Summary of GTB File:
  GTB File Name: /Users/suranyi/Documents/project/GBC/GBC-1.1/example/1000GP3.chrXY.gtb
  GTB File Size: 66.759 MB
  Genome Reference: ftp://ftp.1000genomes.ebi.ac.uk//vol1/ftp/technical/reference/phase2_reference_assembly_sequence/hs37d5.fa.gz
  Suggest To BGZF: false
  Phased: true
  Ordered GTB: true
  BlockSize: 16384 (-bs 7)
  Compression Level: 16 (ZSTD)
  Dimension of Genotypes: 2 chromosomes, 3530137 variants and 2504 subjects
  
Summary of GTB Nodes:
├─ Chromosome X: posRange=[60020, 155260478], numOfNodes=212, numOfVariants=3468095
└─ Chromosome Y: posRange=[2655180, 28770931], numOfNodes=4, numOfVariants=62042
```

