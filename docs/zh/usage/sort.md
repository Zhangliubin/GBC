# 按照坐标对 GTB 文件排序 {#SortMode}

使用如下指令对基因型文件 GTB 进行排序：

```bash
sort <input> -o <output> [options]
```

通常 VCF 文件都是有序的，该功能可以忽略。当您使用参考本版本转换 (例如从 hg19 切换到 hg38)时，位点的坐标可能会发生变化，导致文件变成无序。

## 程序参数 {#Options}

```bash
语法: sort <input> -o <output> [options]
参数:
  --contig      指定染色体标签文件.
                默认值: /contig/human/hg38.p13
                格式: --contig <file> (Exists,File,Inner)
  *--output,-o  设置输出文件名.
                格式: --output <file>
  --threads,-t  设置并行压缩线程数.
                默认值: 4
                格式: --threads <int> (>= 1)
  --subject,-s  提取指定样本的基因型信息.
                格式: --subject <string>,<string>,... 或 --subject @file
  --yes,-y      覆盖输出文件.
GTB 存档参数:
  --phased,-p          设置输出基因型的向型. (默认与输入的 GTB 文件向型一致)
                       格式: --phased [true/false]
  --biallelic          将多个多等位基因位点分裂为多个二等位基因位点.
  --simply             删除 ALT 中等位基因计数值为 0 的标签.
  --blockSizeType,-bs  设置每个压缩块的最大位点数，根据 2^(7+x) 换算得到真实的块大小值.
                       默认值: -1 (即根据样本量自动设置)
                       格式: --blockSizeType <int> (-1 ~ 7)
  --no-reordering,-nr  禁用 Approximate Minimum Discrepancy Ordering (AMDO) 算法.
  --windowSize,-ws     设置 AMDO 算法的采样窗口大小.
                       默认值: 24
                       格式: --windowSize <int> (1 ~ 131072)
  --compressor,-c      设置基压缩器.
                       默认值: ZSTD
                       格式: --compressor <string> ([ZSTD/LZMA/GZIP] or 
                       [0/1/2] (ignoreCase))
  --level,-l           基压缩器的压缩级别. (ZSTD: 0~22, 默认为 3; LZMA: 0~9, 
                       默认为 3; GZIP: 0~9, 默认为 5)
                       默认值: -1
                       格式: --level <int> (-1 ~ 31)
  --readyParas,-rp     从外部 GTB 文件中导入模版参数 (-p, -bs, -c, -l).
                       格式: --readyParas <file> (Exists,File)
  --seq-ac             移除等位基因计数不在 [minAc, maxAc] 范围点的位点.
                       格式: --seq-ac <int>-<int> (>= 0)
  --seq-af             移除等位基因频率不在 [minAf, maxAf] 范围点的位点.
                       格式: --seq-af <double>-<double> (0.0 ~ 1.0)
  --seq-an             移除有效等位基因个数不在 [minAn, maxAn] 范围点的位点.
                       格式: --seq-an <int>-<int> (>= 0)
  --max-allele         移除等位基因种类超过指定值的位点.
                       默认值: 15
                       格式: --max-allele <int> (2 ~ 15)
```

## 程序实例 {#Examples}

使用 GBC 压缩无序的 VCF 文件 `./example/randomsimu100000V_100S.chr1.vcf.gz`：

```bash
# Linux 或 MacOS
docker run -v `pwd`:`pwd` -w `pwd` --rm -it -m 500m gbc \
build ./example/randomsimu100000V_100S.chr1.vcf.gz -o ./example/randomsimu100000V_100S.chr1.gtb -y

# Windows
docker run -v %cd%:%cd% -w %cd% --rm -it -m 500m gbc build ./example/randomsimu100000V_100S.chr1.vcf.gz -o ./example/randomsimu100000V_100S.chr1.gtb -y
```

查看 `./example/randomsimu100000V_100S.chr1.gtb` 文件摘要信息

```bash
# Linux 或 MacOS
docker run -v `pwd`:`pwd` -w `pwd` --rm -it -m 500m gbc \
show ./example/randomsimu100000V_100S.chr1.gtb

# Windows
docker run -v %cd%:%cd% -w %cd% --rm -it -m 500m gbc show ./example/randomsimu100000V_100S.chr1.gtb
```

此时，终端打印如下信息，提示该文件是 `unordered` 的文件：

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

使用 `sort` 指令为该文件排序：

```bash
# Linux 或 MacOS
docker run -v `pwd`:`pwd` -w `pwd` --rm -it -m 500m gbc \
sort ./example/randomsimu100000V_100S.chr1.gtb -o ./example/randomsimu100000V_100S.chr1.order.gtb

# Windows
docker run -v %cd%:%cd% -w %cd% --rm -it -m 500m gbc sort ./example/randomsimu100000V_100S.chr1.gtb -o ./example/randomsimu100000V_100S.chr1.order.gtb
```

