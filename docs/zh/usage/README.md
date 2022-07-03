# 使用 GBC

在命令行程序 (终端) 中使用以下语句查看 GBC 文档：

```bash
# 不传入任何参数时，默认打印文档
java -Xms4g -Xmx4g -jar gbc.jar

# 传入 -h, -help, --help 时，打印文档
java -Xms4g -Xmx4g -jar gbc.jar -h
```

此时，终端打印程序文档：

```bash
Usage: java -jar gbc.jar [mode/tool] [options]
Version: GBC-1.2 (last edited on 2022.06.20, http://pmglab.top/gbc)
Mode:
  build          Compress and build *.gtb for vcf/vcf.gz files.
                 format: build <input(s)> -o <output> [options]
  extract        Retrieve variants from *.gtb file, and export them to 
                 (compressed) VCF format or GTB format.
                 format: extract <input> -o <output> [options]
  show           Display summary of the GTB File.
                 format: show <input> [options]
  sort           Sort variants in GTB by coordinates (chromosome and position).
                 format: sort <input> -o <output> [options]
  concat         Concatenate multiple VCF files. All source files must have the 
                 same subjects columns appearing in the same order with 
                 entirely different sites, and all files must have to be the 
                 same in parameters of the status.
                 format: concat <input(s)> -o <output> [options]
  merge          Merge multiple GTB files (with non-overlapping subject sets) 
                 into a single GTB file.
                 format: merge <input(s)> -o <output> [options]
  reset-subject  Reset subject names (request that same subject number and no 
                 duplicated names) for gtb file directly. Subject names can be 
                 stored in a file with ',' delimited form, and pass in via 
                 '--reset-subject @file'.
                 format: reset-subject <input> -o <output> [options]
  prune          Prune GTB files by node-level or chromosome-level.
                 format: prune <input> -o <output> [options]
  allele-check   Correct for potential complementary strand errors based on 
                 allele labels (A and C, T and G; only biallelic variants are 
                 supported). 
                 format: allele-check <template_input> <input> -o <output> 
                 [options] 
  split          Split a single GTB file into multiple subfiles (e.g. split by 
                 chromosome). 
                 format: split <input> -o <output> [options]
  ld             Calculate pairwise the linkage disequilibrium or genotypic 
                 correlation. 
                 format: ld <input> -o <output> [options]
  index          Index contig file for specified VCF file or reset contig file 
                 for specified GTB file.
                 format: index <input (VCF or GTB)> -o <output> [options]
Tool:
  bgzip     Use parallel bgzip to compress a single file.
            format: bgzip <string> <string> ...
  md5       Calculate a message-digest fingerprint (checksum) for file.
            format: md5 <string> <string> ...
  download  Download resources from an URL address.
            format: download <string> <string> ...
  update    Update GBC software packages.
            default: ./gbc.jar
            format: update <gbc.jar>
```

# 在交互模式下使用 GBC

当多次使用 GBC 运行程序指令时，我们建议在交互模式下进行 (如同 ipython)，它对程序的输入做出反馈，并减少启动 JVM 所需的时间 (尤其是在 Docker 中运行时，使用交互模式可以避免频繁创建、销毁容器)。进入交互模式请运行：

```bash
java -Xmx4g -Xms4g -jar gbc.jar -i
```

当然，用户也可以选择在运行完一次指令后进入交互模式，语句为：

```bash
java -Xmx4g -Xms4g -jar gbc.jar [mode/tool] [options] -i
```

在交互模式下运行指令时，不再需要指定 `java -Xmx4g -Xms4g -jar gbc.jar` 。除了兼容普通命令行参数外，还有以下四种额外的参数：

| 参数            | 描述                              |
| --------------- | --------------------------------- |
| exit, q, quit   | 退出程序                          |
| clear           | 清空屏幕 (实际上打印出多行空白行) |
| reset           | 清空缓冲区                        |
| 以“#”开头的指令 | 注释，不执行任何操作              |

