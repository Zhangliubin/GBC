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
import java.util.Map;

import static edu.sysu.pmglab.commandParser.CommandItem.*;
import static edu.sysu.pmglab.commandParser.CommandRule.AT_MOST;
import static edu.sysu.pmglab.commandParser.CommandRule.MUTUAL_EXCLUSION;

class LDParser {
    /**
     * build by: CommandParser-1.1
     * time: 2022-06-08 03:33:16
     */
    private static final CommandParser PARSER = new CommandParser(false);


    private final CommandOptions options;
    public final CommandOption<?> help;
    public final CommandOption<File> ld;
    public final CommandOption<File> output;
    public final CommandOption<?> oText;
    public final CommandOption<?> oBgz;
    public final CommandOption<Integer> level;
    public final CommandOption<Integer> threads;
    public final CommandOption<?> yes;
    public final CommandOption<File> contig;
    public final CommandOption<?> hapLd;
    public final CommandOption<?> genoLd;
    public final CommandOption<Integer> windowBp;
    public final CommandOption<Double> minR2;
    public final CommandOption<Double> maf;
    public final CommandOption<String[]> subject;
    public final CommandOption<Map<String, int[]>> range;

    LDParser(String... args) {
        this.options = PARSER.parse(args);
        this.help = new CommandOption<>("--help", this.options);
        this.ld = new CommandOption<>("ld", this.options);
        this.output = new CommandOption<>("--output", this.options);
        this.oText = new CommandOption<>("--o-text", this.options);
        this.oBgz = new CommandOption<>("--o-bgz", this.options);
        this.level = new CommandOption<>("--level", this.options);
        this.threads = new CommandOption<>("--threads", this.options);
        this.yes = new CommandOption<>("--yes", this.options);
        this.contig = new CommandOption<>("--contig", this.options);
        this.hapLd = new CommandOption<>("--hap-ld", this.options);
        this.genoLd = new CommandOption<>("--geno-ld", this.options);
        this.windowBp = new CommandOption<>("--window-bp", this.options);
        this.minR2 = new CommandOption<>("--min-r2", this.options);
        this.maf = new CommandOption<>("--maf", this.options);
        this.subject = new CommandOption<>("--subject", this.options);
        this.range = new CommandOption<>("--range", this.options);
    }

    public static LDParser parse(String... args) {
        return new LDParser(args);
    }

    public static LDParser parse(File argsFile) throws IOException {
        return new LDParser(CommandParser.readFromFile(argsFile));
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
        PARSER.setProgramName("ld <input> -o <output>");
        PARSER.offset(0);
        PARSER.debug(false);
        PARSER.usingAt(true);
        PARSER.setMaxMatchedNum(-1);
        PARSER.setAutoHelp(true);
        PARSER.setUsageStyle(DefaultStyleUsage.UNIX_TYPE_1);


        CommandGroup group001 = PARSER.addCommandGroup("Options");
        group001.register(IType.NONE, "--help", "-help", "-h")
                .addOptions(HELP, HIDDEN);
        group001.register(FILE.VALUE, "ld")
                .addOptions(REQUEST, HIDDEN)
                .validateWith(FILE.validateWith(true, true));


        CommandGroup group002 = PARSER.addCommandGroup("Output Options");
        group002.register(FILE.VALUE, "--contig")
                .defaultTo(ChromosomeTags.DEFAULT_FILE)
                .validateWith(FILE.validateWith(true, true, false, true))
                .setDescription("Specify the corresponding contig file.");
        group002.register(FILE.VALUE, "--output", "-o")
                .addOptions(REQUEST)
                .setDescription("Set the output file.");
        group002.register(IType.NONE, "--o-text")
                .setDescription("Output LD file in text format. (this command will be executed automatically if '--o-bgz' is not passed in and the output file specified by '-o' is not end with '.gz')");
        group002.register(IType.NONE, "--o-bgz")
                .setDescription("Output LD file in bgz format. (this command will be executed automatically if '--o-text' is not passed in and the output file specified by '-o' is end with '.gz')");
        group002.register(INTEGER.VALUE, "--level", "-l")
                .defaultTo(5)
                .validateWith(INTEGER.validateWith(0, 9))
                .setDescription("Set the compression level. (Execute only if --o-bgz is passed in)");
        group002.register(INTEGER.VALUE, "--threads", "-t")
                .defaultTo(IParallelTask.INIT_THREADS)
                .validateWith(INTEGER.validateWith(1))
                .setDescription("Set the number of threads.");
        group002.register(IType.NONE, "--yes", "-y")
                .setDescription("Overwrite output file without asking.");


        CommandGroup group003 = PARSER.addCommandGroup("LD Calculation Options");
        group003.register(IType.NONE, "--hap-ld", "--hap-r2")
                .setDescription("Calculate pairwise the linkage disequilibrium.");
        group003.register(IType.NONE, "--geno-ld", "--gene-r2")
                .setDescription("Calculate pairwise the genotypic correlation.");
        group003.register(INTEGER.VALUE, "--window-bp", "-bp")
                .defaultTo(10000)
                .validateWith(INTEGER.validateWith(1))
                .setDescription("The maximum number of physical bases between the variants being calculated for LD.");
        group003.register(DOUBLE.VALUE, "--min-r2")
                .defaultTo(0.2)
                .validateWith(DOUBLE.validateWith(0.0d, 1.0d))
                .setDescription("Exclude pairs with R2 values less than --min-r2.");
        group003.register(DOUBLE.VALUE, "--maf")
                .defaultTo(0.05)
                .validateWith(DOUBLE.validateWith(0.0d, 0.5d))
                .setDescription("Exclude variants with the minor allele frequency (MAF) per variant < maf.");
        group003.register(STRING.ARRAY_COMMA, "--subject", "-s")
                .setDescription("Calculate the LD for the specified subjects. Subject name can be stored in a file with ',' delimited form, and pass in via '-s @file'.");
        group003.register(INTEGER.LABEL_RANGE, "--range", "-r")
                .arity(-1)
                .setFormat("--range <chrom>:<minPos>-<maxPos> <chrom>:<minPos>-<maxPos> ...")
                .setDescription("Calculate the LD by specified position range.");

        PARSER.addRule(AT_MOST, 1, "--hap-ld", "--geno-ld");
        PARSER.addRule(MUTUAL_EXCLUSION, 1, "--o-text", "--o-bgz", "--level");
    }
}