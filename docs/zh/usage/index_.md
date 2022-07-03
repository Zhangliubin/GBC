# Contig 文件 {#IndexMode}

GBC 程序内部并不显式地定义染色体标签的索引信息，而是通过 Contig 文件进行声明，位于 Contig 文件第一行的染色体索引为 0，第二行染色体的索引为 1... 因此，你可以轻易地扩展或者修改 Contig 文件，以实现基因型倍型、染色体标签的修改。

GBC 内置了人类基因组的染色体标签文件（见下表）。对于非人类的基因组，GBC 可能需要一个不同的 contig 文件来声明指定染色体的标签（如chrX, chrY, chrMT）和它的倍性，使得 GBC 也可以对其他单倍体和二倍体物种的基因型进行处理。

Contig 文件以 "#chromosome,ploidy,length "作为标题行，然后每行代表一个染色体。由于在GTB格式中只保留 1 个字节用于存储染色体数目，我们要求输入的contig文件中的染色体数目不超过 256。

```bash
##reference=https://www.ncbi.nlm.nih.gov/grc/human/data?asm=GRCh38.p13
#chromosome,ploidy,length
1,2,248956422
2,2,242193529
3,2,198295559
4,2,190214555
5,2,181538259
6,2,170805979
7,2,159345973
8,2,145138636
9,2,138394717
10,2,133797422
11,2,135086622
12,2,133275309
13,2,114364328
14,2,107043718
15,2,101991189
16,2,90338345
17,2,83257441
18,2,80373285
19,2,58617616
20,2,64444167
21,2,46709983
22,2,50818468
X,2,156040895
Y,2,57227415
MT,2,4485509
```

# 为 VCF 文件构建 Contig 文件 {#BuildContig}

对于非人类基因型文件，使用如下指令为原始的 VCF 文件构建 Contig 文件：

```bash
index <input> -o <output> [options]
```

该方法将扫描 VCF 文件的注释信息，提取其中的 `##contig=` 字段并构建 Contig 文件。如果 VCF 文件为非标准文件，用户可以通过添加参数 `--deep-scan` 扫描整个 VCF 文件的 `CHROM` 字段，并构建相应的 Contig 文件。

# 重建染色体标签索引 {#RebuildContig}

使用如下指令重建 GTB 文件的染色体标签索引：

```bash
index <input> -o <output> --from-contig <from_contig> --to-contig <to_contig> [options]
```

例如，将 1000GP3 的所有染色体标签修改为 `Unknown`，需要建立如下文件：

```bash
#chromosome,ploidy,length
Unknown,2,0
Unknown,2,0
Unknown,2,0
Unknown,2,0
Unknown,2,0
Unknown,2,0
Unknown,2,0
Unknown,2,0
Unknown,2,0
Unknown,2,0
Unknown,2,0
Unknown,2,0
Unknown,2,0
Unknown,2,0
Unknown,2,0
Unknown,2,0
Unknown,2,0
Unknown,2,0
Unknown,2,0
Unknown,2,0
Unknown,2,0
Unknown,2,0
Unknown,2,0
Unknown,2,0
```

然后运行：

```bash
# Linux 或 MacOS
docker run -v `pwd`:`pwd` -w `pwd` --rm -it -m 4g gbc \
index ./example/1000GP3.gtb -o ./example/1000GP3.unknown.gtb --to-contig ./example/1000GP3.contig

# Windows
docker run -v %cd%:%cd% -w %cd% --rm -it -m 4g gbc index ./example/1000GP3.gtb -o ./example/1000GP3.unknown.gtb --to-contig ./example/1000GP3.contig
```