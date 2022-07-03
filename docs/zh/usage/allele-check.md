# 等位基因标签检查 {#AlleleCheckMode}

在合并不同批次的基因型时，相同坐标位点上的不同等位基因标签可能带来混淆（例如: 批次 1 使用正链 DNA，批次 2 使用反链 DNA）。GBC 提供了基于等位基因频率、计数、上下游强关联位点的 LD 模式来校正潜在的正反链错配。使用如下指令进行等位基因的标签检查：

```bash
allele-check <template_input> <input> -o <output> [options]
```

在坐标相同、碱基互补的情况下（例如: [A, T] 和 [C, G]），GBC 实现了三种检查方法：

- 等位基因频率绝对差值：校正满足 $$\mid \text{AF}_1-\text{AF}_2\mid<\text{--freq-gap}$$ 的等位基因标签，这种方法只适用于次级等位基因频率远小于 0.5 的情形。
- 等位基因计数的卡方检验：由来自 2 个文件的互补配对碱基的等位基因计数值构造 $$2\times2$$ 的列联表进行卡方检验，零假设为 `这两个位点的互补等位基因计数没有显著差异` （即可以看作标签是相同的）。如果没有拒绝零假设，则互补的碱基会被纠正。
- 上下游窗口的 LD 模式: 先筛选出上下游（窗口通过 `--window-bp` 控制）中公有的与当前位点的基因型相关性（LD 系数）的绝对值在两批中分别超过阈值（例如：0.8，通过 `--min-r` 控制）的位点，然后统计这些公有位点相关系数的正负号。如果正负符号的数量在两批中有较大差距（例如：超过 80% 位点对的符号是相反的，通过 `--flip-scan-threshold` 控制），等位基因标签应该被翻转；否则，等位基因标签不被翻转。这个功能可以用于检查等位基因频率接近 0.5 的常见变异。

## 程序参数 {#Options}

```bash
语法: allele-check <template_input> <input> -o <output> [options]
参数:
  --contig      指定染色体标签文件.
                默认值: /contig/human/hg38.p13
                格式: --contig <file> (Exists,File,Inner)
  *--output,-o  设置输出文件名.
                格式: --output <file>
  --threads,-t  设置并行压缩线程数.
                默认值: 4
                格式: --threads <int> (>= 1)
  --union       处理不同文件中的位点的策略 (默认为取交集, 传入 `--union` 时将取并集).
  --yes,-y      覆盖输出文件.
对齐坐标参数:
  --p-value              翻转次级等位基因频率 <= --maf 的位点中卡方检验 p 值 >=
                         --p-value 的等位基因标签.  
                         默认值: 0.05
                         格式: --p-value <double> (1.0E-6 ~ 0.5)
  --freq-gap             翻转次级等位基因频率 <= --maf 的位点中等位基因频率差值 <=
                         --freq-gap 的等位基因标签. 
                         格式: --freq-gap <double> (1.0E-6 ~ 0.5)
  --no-ld                默认情况下, 翻转次级等位基因频率 > --maf 的位点中上下游强关联位
                         点的 LD 模式超过反转比例阈值的等位基因标签. 使用该参数禁用该检查.
  --min-r                在 LD 模式检查中, 排除基因型相关性小于 --min-r 的位点.
                         默认值: 0.8
                         格式: --min-r <double> (0.5 ~ 1.0)
  --flip-scan-threshold  在 LD 模式检查中, 超过反转比例阈值.
                         默认值: 0.8
                         格式: --flip-scan-threshold <double> (0.5 ~ 1.0)
  --maf                  设置用于区分常见变异和罕见变异的次级等位基因频率阈值.
                         默认值: 0.05
                         格式: --maf <double> (0.0 ~ 1.0)
  --window-bp,-bp        设置计算上下游 LD 系数的最大物理距离 (单位: bp).
                         默认值: 10000
                         格式: --window-bp <int> (>= 1)
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

从 `http://pmglab.top/genotypes` 下载 `EAS hg38` 数据集，使用该数据集作为模版文件，检查本地外显子组测序数据 `SNP.gtb` 的等位基因标签：

```bash
# Linux 或 MacOS
docker run -v `pwd`:`pwd` -w `pwd` --rm -it -m 4g gbc \
allele-check ./example/EAS.gtb ./example/SNP.gtb -o ./example/SNP.checked.gtb --seq-af 0.000001-0.999999 -y

# Windows
docker run -v %cd%:%cd% -w %cd% --rm -it -m 4g gbc allele-check ./example/EAS.gtb ./example/SNP.gtb -o ./example/SNP.checked.gtb --seq-af 0.000001-0.999999 -y
```

程序运行时，终端中输出检查日志：

```shell
2022-06-27 01:16:02 INFO  [ThreadPool-thread-1] AlleleCheck chr11:244961 TempREF=G TempALT=C TempAF=0.8035714285714286 REF=G ALT=C AF=0.6944444444444444 -> REF=C ALT=G
2022-06-27 01:16:02 INFO  [ThreadPool-thread-1] AlleleCheck chr11:251057 TempREF=C TempALT=G TempAF=0.08630952380952381 REF=C ALT=G AF=0.22093023255813954 -> REF=G ALT=C
2022-06-27 01:16:04 INFO  [ThreadPool-thread-1] AlleleCheck chr17:42969194 TempREF=C TempALT=G TempAF=0.9742063492063492 REF=C ALT=G AF=0.02608695652173913 -> REF=G ALT=C
2022-06-27 01:16:06 INFO  [ThreadPool-thread-2] AlleleCheck chr1:1041823 TempREF=G TempALT=C TempAF=0.9990079365079365 REF=G ALT=C AF=0.0045045045045045045 -> REF=C ALT=G
```

