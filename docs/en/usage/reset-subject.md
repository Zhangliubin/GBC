# Reset the Subject Names {#ResetSubjectMode}

Use the following command to reset subject (or sample) names of GTB:

```bash
reset-subject <input> -o <output> [options]
```

## Program Options {#Options}

```bash
Usage: reset-subject <input> -o <output> [options]
Options:
  --contig      Specify the corresponding contig file.
                default: /contig/human/hg38.p13
                format: --contig <file> (Exists,File,Inner)
  *--output,-o  Set the output file.
                format: --output <file>
  --yes,-y      Overwrite output file without asking.
  --subject     Reset subject names (request that same subject number and no 
                duplicated names) for gtb file directly. Subject names can be 
                stored in a file with ',' delimited form, and pass in via 
                '--subject @file'.
                format: --subject <string>,<string>,...
  --prefix      Use the format `[prefix][number][suffix]` to reset the subject 
                names. 
                default: S_
                format: --prefix <string>
  --suffix      Use the format `[prefix][number][suffix]` to reset the subject 
                names. 
                format: --suffix <string>
  --begin       Use the format `[prefix][number][suffix]` to reset the subject 
                names. 
                default: 1
                format: --begin <int>
```

## Example {#Examples}

Use the GBC to compress an example file `./example/rare.disease.hg19.vcf.gz`：

```bash
# Linux or MacOS
docker run -v `pwd`:`pwd` -w `pwd` --rm -it -m 4g gbc \
build ./example/rare.disease.hg19.vcf.gz -o ./example/rare.disease.hg19.gtb -y

# Windows
docker run -v %cd%:%cd% -w %cd% --rm -it -m 4g gbc build ./example/rare.disease.hg19.vcf.gz -o ./example/rare.disease.hg19.gtb -y
```

Reset the subject names to `CASE_6_1`, `CASE_7_1` and `CASE_8_1`：

```bash
# Linux or MacOS
docker run -v `pwd`:`pwd` -w `pwd` --rm -it -m 4g gbc \
reset-subject ./example/rare.disease.hg19.gtb -o ./example/out.gtb --prefix CASE_ --begin 6 --suffix _1 -y 

# Windows
docker run -v %cd%:%cd% -w %cd% --rm -it -m 4g gbc reset-subject ./example/rare.disease.hg19.gtb -o ./example/out.gtb --prefix CASE_ --begin 6 --suffix _1 -y 
```

or:

```bash
# Linux or MacOS
docker run -v `pwd`:`pwd` -w `pwd` --rm -it -m 4g gbc \
reset-subject ./example/rare.disease.hg19.gtb -o ./example/out.gtb --subject CASE_6_1,CASE_7_1,CASE_8_1 -y 

# Windows
docker run -v %cd%:%cd% -w %cd% --rm -it -m 4g gbc reset-subject ./example/rare.disease.hg19.gtb -o ./example/out.gtb --subject CASE_6_1,CASE_7_1,CASE_8_1 -y 
```

