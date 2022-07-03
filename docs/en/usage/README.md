# Setup GBC

Launch the GBC in the command line program (terminal) with the following command:

```bash
# The program document is printed by default when no parameters are passed in.
java -Xms4g -Xmx4g -jar gbc.jar

# When passing -h, -help or --help, the program will also print the program document.
java -Xms4g -Xmx4g -jar gbc.jar -h
```

The results are as follows:

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

# Use GBC in interactive mode

When running program commands multiple times with GBC, we recommend doing so in interactive mode (like ipython), which provides real-time feedback on program input and reduces the time required to start the JVM (especially when running in Docker, using interactive mode avoids frequent container creation and destruction). To start interactive mode, run:

```bash
java -Xmx4g -Xms4g -jar gbc.jar -i
```

Of course, the user can also choose to enter interactive mode after running the command once, with the following command:

```bash
java -Xmx4g -Xms4g -jar gbc.jar [mode/tool] [options] -i
```

When you type a command, you no longer need to specify `java -Xmx4g -Xms4g -jar gbc.jar`. The command line interaction mode has four additional parameters in addition to the parameters in command line mode:

| Parameters           | Description                                                  |
| -------------------- | ------------------------------------------------------------ |
| exit, q, quit        | Exit program, exit the command line interaction mode.        |
| clear                | Clearing the screen (actually printing out multiple blank lines). |
| reset                | Clear the data buffer.                                       |
| Lines begin with "#" | For annotation.                                              |

