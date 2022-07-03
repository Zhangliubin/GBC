# 计算 LD 系数 {#LDMode}

GBC 集成了 LD 系数的计算方法。使用如下指令计算基因型 GTB 文件的 LD 系数：

```bash
ld <input> -o <output> [options]
```

## 程序参数 {#Options}

```bash
语法: ld <input> -o <output> [options]
输出参数:
  --contig      指定染色体标签文件.
                默认值: /contig/human/hg38.p13
                格式: --contig <file> (Exists,File,Inner)
  *--output,-o  设置输出文件名.
                格式: --output <file>
  --o-text      以文本格式输出位点的 LD 系数信息.
  --o-bgz       以 BGZIP 压缩的格式输出位点的 LD 系数信息.
  --level,-l    设置 BGZIP 压缩器的压缩级别.
  --threads,-t  设置并行压缩线程数.
                默认值: 4
                格式: --threads <int> (>= 1)
  --yes,-y      覆盖输出文件.
LD 计算参数:
  --hap-ld,--hap-r2    计算位点对的单倍型连锁不平衡系数.
  --geno-ld,--gene-r2  计算位点对的基因型连锁不平衡系数 (Pearson 相关系数).
  --window-bp,-bp      设置计算上下游 LD 系数的最大物理距离 (单位: bp).
                       默认值: 10000
                       格式: --window-bp <int> (>= 1)
  --min-r2             在 LD 模式检查中, 排除 R^2 小于 --min-r2 的位点.
                       默认值: 0.2
                       格式: --min-r2 <double> (0.0 ~ 1.0)
  --maf                移除次级等位基因频率 < --maf 的位点.
                       默认值: 0.05
                       format: --maf <double> (0.0 ~ 0.5)
  --subject,-s         计算指定样本的 LD 系数.
                       格式: --subject <string>,<string>,...
  --range,-r           计算指定位点范围的 LD 系数.
                       格式: --range <chrom>:<minPos>-<maxPos> 
                       <chrom>:<minPos>-<maxPos> ...
```

## 程序实例 {#Examples}

计算 `EAS hg38` 数据集的连锁不平衡系数：

```bash
# Linux 或 MacOS
docker run -v `pwd`:`pwd` -w `pwd` --rm -it -m 4g gbc \
ld ./example/EAS.gtb -o ./example/EAS.ld.gz -t 8 -y

# Windows
docker run -v %cd%:%cd% -w %cd% --rm -it -m 4g gbc ld ./example/EAS.gtb -o ./example/EAS.ld.gz -t 8 -y
```

