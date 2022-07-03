# Split GTB {#SplitMode}

Use the following command to split the GTB file into multiple independent subfiles:

```bash
split <input> -o <output> [options]
```

![分裂GTB文件](../../assets/分裂GTB文件.png)

## Program Options {#Options}

```
Usage: split <input> -o <output> [options]
Options:
  --contig      Specify the corresponding contig file.
                default: /contig/human/hg38.p13
                format: --contig <file> (Exists,File,Inner)
  *--output,-o  Set the output folder.
                format: --output <file>
  --by          Split input files by node-level/chromosome-level into multiple 
                subfiles, which can be rejoined by the concat mode.
                default: chromosome
                format: --by <string> ([chromosome/node] or [0/1])
  --yes,-y      Overwrite output file without asking.
```

## Example {#Examples}

Split `./example/1000GP3.gtb` by chromosome tags:

```bash
# Linux or MacOS
docker run -v `pwd`:`pwd` -w `pwd` --rm -it -m 500m gbc \
split ./example/1000GP3.gtb -o ./example/1000GP3-chr -y

# Windows
docker run -v %cd%:%cd% -w %cd% --rm -it -m 500m gbc split ./example/1000GP3.gtb -o ./example/1000GP3-chr -y
```

After running this command, 24 GTB subfiles are generated in the folder `./example/1000GP30-chr`.
