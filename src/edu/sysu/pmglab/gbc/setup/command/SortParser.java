package edu.sysu.pmglab.gbc.setup.command;

import edu.sysu.pmglab.commandParser.CommandGroup;
import edu.sysu.pmglab.commandParser.CommandOption;
import edu.sysu.pmglab.commandParser.CommandOptions;
import edu.sysu.pmglab.commandParser.CommandParser;
import edu.sysu.pmglab.commandParser.types.*;
import edu.sysu.pmglab.commandParser.usage.DefaultStyleUsage;
import edu.sysu.pmglab.container.File;
import edu.sysu.pmglab.gbc.constant.ChromosomeTags;
import edu.sysu.pmglab.gbc.core.IParallelTask;

import java.io.IOException;

import static edu.sysu.pmglab.commandParser.CommandItem.*;
import static edu.sysu.pmglab.commandParser.CommandRule.AT_MOST;
import static edu.sysu.pmglab.commandParser.CommandRule.MUTUAL_EXCLUSION;

class SortParser {
    /**
     * build by: CommandParser-1.1
     * time: 2022-06-07 15:55:29
     */
    private static final CommandParser PARSER = new CommandParser(false);


    private final CommandOptions options;
    public final CommandOption<?> help;
    public final CommandOption<File> sort;
    public final CommandOption<File> contig;
    public final CommandOption<File> output;
    public final CommandOption<Integer> threads;
    public final CommandOption<String[]> subject;
    public final CommandOption<?> yes;
    public final CommandOption<Boolean> phased;
    public final CommandOption<?> biallelic;
    public final CommandOption<?> simply;
    public final CommandOption<Integer> blocksizetype;
    public final CommandOption<?> noReordering;
    public final CommandOption<Integer> windowsize;
    public final CommandOption<String> compressor;
    public final CommandOption<Integer> level;
    public final CommandOption<File> readyparas;
    public final CommandOption<int[]> seqAc;
    public final CommandOption<double[]> seqAf;
    public final CommandOption<int[]> seqAn;
    public final CommandOption<Integer> maxAllele;

    SortParser(String... args) {
        this.options = PARSER.parse(args);
        this.help = new CommandOption<>("--help", this.options);
        this.sort = new CommandOption<>("sort", this.options);
        this.contig = new CommandOption<>("--contig", this.options);
        this.output = new CommandOption<>("--output", this.options);
        this.threads = new CommandOption<>("--threads", this.options);
        this.subject = new CommandOption<>("--subject", this.options);
        this.yes = new CommandOption<>("--yes", this.options);
        this.phased = new CommandOption<>("--phased", this.options);
        this.biallelic = new CommandOption<>("--biallelic", this.options);
        this.simply = new CommandOption<>("--simply", this.options);
        this.blocksizetype = new CommandOption<>("--blockSizeType", this.options);
        this.noReordering = new CommandOption<>("--no-reordering", this.options);
        this.windowsize = new CommandOption<>("--windowSize", this.options);
        this.compressor = new CommandOption<>("--compressor", this.options);
        this.level = new CommandOption<>("--level", this.options);
        this.readyparas = new CommandOption<>("--readyParas", this.options);
        this.seqAc = new CommandOption<>("--seq-ac", this.options);
        this.seqAf = new CommandOption<>("--seq-af", this.options);
        this.seqAn = new CommandOption<>("--seq-an", this.options);
        this.maxAllele = new CommandOption<>("--max-allele", this.options);
    }

    public static SortParser parse(String... args) {
        return new SortParser(args);
    }

    public static SortParser parse(File argsFile) throws IOException {
        return new SortParser(CommandParser.readFromFile(argsFile));
    }

    /**
     * Get CommandParser
     */
    public static CommandParser getParser() {
        return PARSER;
    }

    /**
     * Get the usage of CommandParser
     */
    public static String usage() {
        return PARSER.toString();
    }

    /**
     * Get CommandOptions
     */
    public CommandOptions getOptions() {
        return this.options;
    }

    static {
        PARSER.setProgramName("sort <input> -o <output>");
        PARSER.offset(0);
        PARSER.debug(true);
        PARSER.usingAt(true);
        PARSER.setMaxMatchedNum(-1);
        PARSER.setAutoHelp(true);
        PARSER.setUsageStyle(DefaultStyleUsage.UNIX_TYPE_1);


        CommandGroup group001 = PARSER.addCommandGroup("Options");
        group001.register(IType.NONE, "--help", "-help", "-h")
                .addOptions(HELP, HIDDEN);
        group001.register(FILE.VALUE, "sort")
                .addOptions(REQUEST, HIDDEN)
                .validateWith(FILE.validateWith(true, true));
        group001.register(FILE.VALUE, "--contig")
                .defaultTo(ChromosomeTags.DEFAULT_FILE)
                .validateWith(FILE.validateWith(true, true, false, true))
                .setDescription("Specify the corresponding contig file.");
        group001.register(FILE.VALUE, "--output", "-o")
                .addOptions(REQUEST)
                .setDescription("Set the output file.");
        group001.register(INTEGER.VALUE, "--threads", "-t")
                .defaultTo(IParallelTask.INIT_THREADS)
                .validateWith(INTEGER.validateWith(1))
                .setDescription("Set the number of threads.");
        group001.register(STRING.ARRAY_COMMA, "--subject", "-s")
                .setDescription("Extract the information of the specified subjects. Subject name can be stored in a file with ',' delimited form, and pass in via '-s @file'.");
        group001.register(IType.NONE, "--yes", "-y")
                .setDescription("Overwrite output file without asking.");


        CommandGroup group002 = PARSER.addCommandGroup("GTB Archive Options");
        group002.register(BOOLEAN.VALUE, "--phased", "-p")
                .setDescription("Force-set the status of the genotype. (same as the GTB basic information by default)");
        group002.register(IType.NONE, "--biallelic")
                .setDescription("Split multiallelic variants into multiple biallelic variants.");
        group002.register(IType.NONE, "--simply")
                .setDescription("Delete the alternative alleles (ALT) with allele counts equal to 0.");
        group002.register(INTEGER.VALUE, "--blockSizeType", "-bs")
                .defaultTo(-1)
                .validateWith(INTEGER.validateWith(-1, 7))
                .setDescription("Set the maximum size=2^(7+x) of each block. (-1 means auto-adjustment)");
        group002.register(IType.NONE, "--no-reordering", "-nr")
                .setDescription("Disable the Approximate Minimum Discrepancy Ordering (AMDO) algorithm.");
        group002.register(INTEGER.VALUE, "--windowSize", "-ws")
                .defaultTo(24)
                .validateWith(INTEGER.validateWith(1, 131072))
                .setDescription("Set the window size of the AMDO algorithm.");
        group002.register(STRING.VALUE, "--compressor", "-c")
                .defaultTo("ZSTD")
                .validateWith(STRING.validateWith("ZSTD", "LZMA", "GZIP"))
                .setDescription("Set the basic compressor for compressing processed data.");
        group002.register(INTEGER.VALUE, "--level", "-l")
                .defaultTo(-1)
                .validateWith(INTEGER.validateWith(-1, 31))
                .setDescription("Compression level to use when basic compressor works. (ZSTD: 0~22, 3 as default; LZMA: 0~9, 3 as default; GZIP: 0~9, 5 as default)");
        group002.register(FILE.VALUE, "--readyParas", "-rp")
                .validateWith(FILE.validateWith(true, true))
                .setDescription("Import the template parameters (-p, -bs, -c, -l) from an external GTB file.");
        group002.register(INTEGER.RANGE, "--seq-ac")
                .validateWith(INTEGER.validateWith(0))
                .setDescription("Exclude variants with the alternate allele count (AC) per variant out of the range [minAc, maxAc].");
        group002.register(DOUBLE.RANGE, "--seq-af")
                .validateWith(DOUBLE.validateWith(0.0d, 1.0d))
                .setDescription("Exclude variants with the alternate allele frequency (AF) per variant out of the range [minAf, maxAf].");
        group002.register(INTEGER.RANGE, "--seq-an")
                .validateWith(INTEGER.validateWith(0))
                .setDescription("Exclude variants with the non-missing allele number (AN) per variant out of the range [minAn, maxAn].");
        group002.register(INTEGER.VALUE, "--max-allele")
                .defaultTo(15)
                .validateWith(INTEGER.validateWith(2, 15))
                .setDescription("Exclude variants with alleles over --max-allele.");


        PARSER.addRule(AT_MOST, 1, "--max-allele", "--biallelic");
        PARSER.addRule(AT_MOST, 1, "--no-reordering", "--windowSize");
        PARSER.addRule(MUTUAL_EXCLUSION, 1, "--readyParas", "--phased", "--blockSizeType", "--compressor", "--level");
    }
}