# 重设样本名 {#ResetSubjectMode}

使用如下指令重设基因型文件 GTB 的样本名：

```bash
reset-subject <input> -o <output> [options]
```

## 程序参数 {#Options}

```bash
语法: reset-subject <input> -o <output> [options]
参数:
  --contig      指定染色体标签文件.
                默认值: /contig/human/hg38.p13
                格式: --contig <file> (Exists,File,Inner)
  *--output,-o  设置输出文件名.
                格式: --output <file>
  --yes,-y      覆盖输出文件.
  --subject,-s  提取指定样本的基因型信息.
                格式: --subject <string>,<string>,... 或 --subject @file
  --prefix      使用格式 `[prefix][number][suffix]` 重设样本名. `prefix` 用于设置前缀.
                默认值: S_
                格式: --prefix <string>
  --suffix      使用格式 `[prefix][number][suffix]` 重设样本名. `suffix` 用于设置后缀.
                格式: --suffix <string>
  --begin       使用格式 `[prefix][number][suffix]` 重设样本名. `begin` 用于设置起始编码.
                默认值: 1
                格式: --begin <int>
```

## 程序实例 {#Examples}

使用 GBC 压缩示例文件 `rare.disease.hg19.vcf.gz`：

```bash
# Linux 或 MacOS
docker run -v `pwd`:`pwd` -w `pwd` --rm -it -m 4g gbc \
build ./example/rare.disease.hg19.vcf.gz -o ./example/rare.disease.hg19.gtb -y

# Windows
docker run -v %cd%:%cd% -w %cd% --rm -it -m 4g gbc build ./example/rare.disease.hg19.vcf.gz -o ./example/rare.disease.hg19.gtb -y
```

重设样本名为 `CASE_6_1`, `CASE_7_1` 和 `CASE_8_1`：

```bash
# Linux 或 MacOS
docker run -v `pwd`:`pwd` -w `pwd` --rm -it -m 4g gbc \
reset-subject ./example/rare.disease.hg19.gtb -o ./example/out.gtb --prefix CASE_ --begin 6 --suffix _1 -y 

# Windows
docker run -v %cd%:%cd% -w %cd% --rm -it -m 4g gbc reset-subject ./example/rare.disease.hg19.gtb -o ./example/out.gtb --prefix CASE_ --begin 6 --suffix _1 -y 
```

或：

```bash
# Linux 或 MacOS
docker run -v `pwd`:`pwd` -w `pwd` --rm -it -m 4g gbc \
reset-subject ./example/rare.disease.hg19.gtb -o ./example/out.gtb --subject CASE_6_1,CASE_7_1,CASE_8_1 -y 

# Windows
docker run -v %cd%:%cd% -w %cd% --rm -it -m 4g gbc reset-subject ./example/rare.disease.hg19.gtb -o ./example/out.gtb --subject CASE_6_1,CASE_7_1,CASE_8_1 -y 
```

