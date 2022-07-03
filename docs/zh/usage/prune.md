# GTB 文件剪枝 {#PruneMode}

使用如下指令对基因型文件 GTB 进行节点修剪，实现快速提取、删除操作：

```bash
prune <input> -o <output> [options]
```

与 `extract` 相比，`prune` 不需要解压任何数据实现节点的提取或删除，具有更快的速度和更低的内存负担，所有操作均能在数秒内完成。

## 程序参数 {#Options}

```bash
语法: prune <input> -o <output> [options]
参数:
  --contig        指定染色体标签文件.
                  默认值: /contig/human/hg38.p13
                  格式: --contig <file> (Exists,File,Inner)
  *--output,-o    设置输出文件名.
                  格式: --output <file>
  --yes,-y        覆盖输出文件.
  --delete-node   删除指定的 GTB 节点.
                  格式: --delete-node <string>:<int>,<int>,... <string>:<int>,<int>,... ...
  --retain-node   保留指定的 GTB 节点.
                  格式: --retain-node <string>:<int>,<int>,... <string>:<int>,<int>,... ...
  --delete-chrom  删除指定的染色体.
                  格式: --delete-chrom <string>,<string>,...
  --retain-chrom  保留指定的染色体.
                  格式: --retain-chrom <string>,<string>,...
```

## 程序实例 {#Examples}

使用 GBC 提取 `1000GP3.gtb` 的性染色体 (chrX 和 chrY)：

```bash
# Linux 或 MacOS
docker run -v `pwd`:`pwd` -w `pwd` --rm -it -m 4g gbc \
prune ./example/1000GP3.gtb -o ./example/1000GP3.chrXY.gtb \
--retain-chrom X,Y \
-y

# Windows
docker run -v %cd%:%cd% -w %cd% --rm -it -m 4g gbc prune ./example/1000GP3.gtb -o ./example/1000GP3.chrXY.gtb --retain-chrom X,Y -y
```

查看提取的 GTB 文件信息：

```bash
# Linux 或 MacOS
docker run -v `pwd`:`pwd` -w `pwd` --rm -it -m 4g gbc \
show ./example/1000GP3.chrXY.gtb --add-tree

# Windows
docker run -v %cd%:%cd% -w %cd% --rm -it -m 4g gbc show ./example/1000GP3.chrXY.gtb --add-tree
```

此时，终端输出以下信息：

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

