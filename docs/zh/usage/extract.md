# 提取基因型 {#ExtractMode}

使用如下指令对基因型文件 GTB 进行信息提取：

```bash
extract <input> -o <output> [options]
```

- 当 `[options]` 中包含了 `--o-gtb` 或输出文件名以 `.gtb` 结尾时，识别为导出 GTB 文件格式；
- 当 `[options]` 中包含了 `--o-bgz` 或输出文件名以 `.gz` 结尾时，识别为导出 BGZIP 压缩的 VCF 文件格式；
- 当 `[options]` 中包含了 `--o-vcf` 或输出文件名不以 `.gz` 或 `.gtb` 结尾时，识别为导出 VCF 文件格式。

通常生物信息学工具都兼容 BGZIP 压缩的 VCF 文件格式，我们建议用户以 `--o-bgz` 或 `--o-gtb` 格式作为输出，以增强程序的并行输出性能。

## 程序参数 {#Options}

```bash
语法: extract <input> -o <output> [options]
输出参数:
  --contig      指定染色体标签文件.
                默认值: /contig/human/hg38.p13
                格式: --contig <file> (Exists,File,Inner)
  *--output,-o  设置输出文件名.
                格式: --output <file>
  --o-text      以 VCF 文本格式输出位点信息.
  --o-bgz       以 BGZIP 压缩的 VCF 格式输出位点信息.
  --o-gtb       以 GTB 格式输出位点信息.
  --level,-l    基压缩器的压缩级别. (ZSTD: 0~22, 默认为 3; LZMA: 0~9, 默认为 3; 
                GZIP: 0~9, 默认为 5)
                默认值: -1
                格式: --level <int> (-1 ~ 31)
  --no-clm      不使用循环锁定算法 (CLM) 并行输出. 在这个参数下, 并行解压意味着先输出到临
                时文件, 在完成压缩时拼接这些临时文件.
  --threads,-t  设置并行压缩线程数.
                默认值: 4
                格式: --threads <int> (>= 1)
  --phased,-p   设置输出基因型的向型. (默认与输入的 GTB 文件向型一致)
                格式: --phased [true/false]
  --hideGT,-hg  不输出样本基因型数据.
  --yes,-y      覆盖输出文件.
GTB 存档参数:
  --biallelic          将多个多等位基因位点分裂为多个二等位基因位点.
  --simply             删除 ALT 中等位基因计数值为 0 的标签.
  --blockSizeType,-bs  设置每个压缩块的最大位点数, 根据 2^(7+x) 换算得到真实的块大小值.
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
  --readyParas,-rp     从外部 GTB 文件中导入模版参数 (-p, -bs, -c, -l).
                       格式: --readyParas <file> (Exists,File)
子集选择参数:
  --subject,-s   提取指定样本的基因型信息.
                 格式: --subject <string>,<string>,... 或 --subject @file
  --range,-r     按照指定的坐标范围提取基因型信息.
                 格式: --range <chrom>:<minPos>-<maxPos> <chrom>:<minPos>-<maxPos> ...
  --random       按照指定的坐标提取基因型信息. (坐标信息保存在文件中, 每一行的格式为 
                 'chrom,position' 或 'chrom position'，一行代表一个位点)
                 格式: --random <file>
  --retain-node  按照指定的 GTB 节点索引提取基因型信息.
                 格式: --retain-node <string>:<int>-<int> <string>:<int>-<int> ...
  --seq-ac       移除等位基因计数不在 [minAc, maxAc] 范围点的位点.
                 格式: --seq-ac <int>-<int> (>= 0)
  --seq-af       移除等位基因频率不在 [minAf, maxAf] 范围点的位点.
                 格式: --seq-af <double>-<double> (0.0 ~ 1.0)
  --seq-an       移除有效等位基因个数不在 [minAn, maxAn] 范围点的位点.
                 格式: --seq-an <int>-<int> (>= 0)
  --max-allele   移除等位基因种类超过指定值的位点.
                 默认值: 15
                 格式: --max-allele <int> (2 ~ 15)
```

## 程序实例 {#Examples}

使用 GBC 解压示例文件 `./example/assoc.hg19.gtb`，并设置以下参数：

- 基因型设置为 unphased；
- 提取坐标范围大于 1000000 的位点；
- 提取等位基因频率在 [0.4, 0.6] 范围内的位点；
- 提取样本名为 NA18963,NA18977,HG02401,HG02353,HG02064 的基因型。

完成该任务的指令如下：

```bash
# Linux 或 MacOS
docker run -v `pwd`:`pwd` -w `pwd` --rm -it -m 4g gbc \
extract ./example/assoc.hg19.gtb -o ./example/assoc.hg19.extract.vcf \
-p true -r 1:1000000- --seq-af 0.4-0.6 -s NA18963,NA18977,HG02401,HG02353,HG02064 -y

# Windows
docker run -v %cd%:%cd% -w %cd% --rm -it -m 4g gbc extract ./example/assoc.hg19.gtb -o ./example/assoc.hg19.extract.vcf -p true -r 1:1000000- --seq-af 0.4-0.6 -s NA18963,NA18977,HG02401,HG02353,HG02064 -y
```
