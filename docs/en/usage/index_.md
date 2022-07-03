# Contig File Format {#IndexMode}

The GBC application does not explicitly define the index of chromosome tags internally, but declares it through the contig file, where the index of the chromosome located in the first line of the contig file is 0, and the index of the chromosome in the second line is 1... Therefore, you can easily extend or modify the contig file for chromosome ploidy and chromosome tags modification.

By default, GBC supports genotype compression of human beings. It can also encode and compress genotypes of other haplotypic and diploid species. For non-human genomes, GBC only requires a different contig file to declare the label of the assigned chromosome (e.g., chrX, chrY, chrMT) and its ploidy. A contig file has "#chromosome,ploidy,length" as the header line, and then each line represents one chromosome. Since only 1 byte is reserved for storing chromosome numbers in GTB format, we require that the number of chromosomes in the input contig file does not exceed 256.

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

# Build Contig File For VCF {#BuildContig}

For non-human genotype files, use the following command to build a contig file for the original VCF file.

```bash
index <input> -o <output> [options]
```

This method will scan the comment information of VCF file and extract the `##contig=` field to build a contig file. If the VCF file is non-standard, the user can scan the entire VCF file by adding the parameter `--deep-scan`.

# Reset Chromosome Tags With New Contig File {#RebuildContig}

Use the following command to reset the chromosome tags of the GTB file (e.g. reset multiple chromosome tags to the same tag):

```bash
index <input> -o <output> --from-contig <from_contig> --to-contig <to_contig> [options]
```

For example, to set all the chromosome tags of 1000GP3 to `Unknown`, the following contig file needs to be created:

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

and then, run:

```bash
# Linux or MacOS
docker run -v `pwd`:`pwd` -w `pwd` --rm -it -m 4g gbc \
index ./example/1000GP3.gtb -o ./example/1000GP3.unknown.gtb --to-contig ./example/1000GP3.contig

# Windows
docker run -v %cd%:%cd% -w %cd% --rm -it -m 4g gbc index ./example/1000GP3.gtb -o ./example/1000GP3.unknown.gtb --to-contig ./example/1000GP3.contig
```