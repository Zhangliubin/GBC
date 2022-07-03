package edu.sysu.pmglab.gbc.setup.command;

import edu.sysu.pmglab.commandParser.CommandGroup;
import edu.sysu.pmglab.commandParser.CommandOption;
import edu.sysu.pmglab.commandParser.CommandOptions;
import edu.sysu.pmglab.commandParser.CommandParser;
import edu.sysu.pmglab.commandParser.usage.DefaultStyleUsage;
import edu.sysu.pmglab.container.File;
import edu.sysu.pmglab.commandParser.types.*;
import edu.sysu.pmglab.gbc.constant.ChromosomeTags;

import java.io.IOException;
import java.util.*;

import static edu.sysu.pmglab.commandParser.CommandRule.*;
import static edu.sysu.pmglab.commandParser.CommandItem.*;

class ShowParser {
    /**
     * build by: CommandParser-1.1
     * time: 2022-06-07 01:01:13
     */
    private static final CommandParser PARSER = new CommandParser(false);


    private final CommandOptions options;
    public final CommandOption<?> help;
    public final CommandOption<File> show;
    public final CommandOption<File> contig;
    public final CommandOption<?> addMd5;
    public final CommandOption<?> addSubject;
    public final CommandOption<?> addTree;
    public final CommandOption<?> addNode;
    public final CommandOption<?> full;
    public final CommandOption<?> listSubjectOnly;
    public final CommandOption<?> listPositionOnly;
    public final CommandOption<?> listSiteOnly;
    public final CommandOption<String[]> subject;
    public final CommandOption<Map<String, int[]>> range;
    public final CommandOption<File> random;
    public final CommandOption<Map<String, int[]>> retainNode;
    public final CommandOption<int[]> seqAc;
    public final CommandOption<double[]> seqAf;
    public final CommandOption<int[]> seqAn;
    public final CommandOption<Integer> maxAllele;

    ShowParser(String... args) {
        this.options = PARSER.parse(args);
        this.help = new CommandOption<>("--help", this.options);
        this.show = new CommandOption<>("show", this.options);
        this.contig = new CommandOption<>("--contig", this.options);
        this.addMd5 = new CommandOption<>("--add-md5", this.options);
        this.addSubject = new CommandOption<>("--add-subject", this.options);
        this.addTree = new CommandOption<>("--add-tree", this.options);
        this.addNode = new CommandOption<>("--add-node", this.options);
        this.full = new CommandOption<>("--full", this.options);
        this.listSubjectOnly = new CommandOption<>("--list-subject-only", this.options);
        this.listPositionOnly = new CommandOption<>("--list-position-only", this.options);
        this.listSiteOnly = new CommandOption<>("--list-site-only", this.options);
        this.subject = new CommandOption<>("--subject", this.options);
        this.range = new CommandOption<>("--range", this.options);
        this.random = new CommandOption<>("--random", this.options);
        this.retainNode = new CommandOption<>("--retain-node", this.options);
        this.seqAc = new CommandOption<>("--seq-ac", this.options);
        this.seqAf = new CommandOption<>("--seq-af", this.options);
        this.seqAn = new CommandOption<>("--seq-an", this.options);
        this.maxAllele = new CommandOption<>("--max-allele", this.options);
    }

    public static ShowParser parse(String... args) {
        return new ShowParser(args);
    }

    public static ShowParser parse(File argsFile) throws IOException {
        return new ShowParser(CommandParser.readFromFile(argsFile));
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
        PARSER.setProgramName("show <input>");
        PARSER.offset(0);
        PARSER.debug(false);
        PARSER.usingAt(true);
        PARSER.setMaxMatchedNum(-1);
        PARSER.setAutoHelp(true);
        PARSER.setUsageStyle(DefaultStyleUsage.UNIX_TYPE_1);


        CommandGroup group001 = PARSER.addCommandGroup("Options");
        group001.register(IType.NONE, "--help", "-help", "-h")
                .addOptions(HELP, HIDDEN);
        group001.register(FILE.VALUE, "show")
                .addOptions(REQUEST, HIDDEN)
                .validateWith(FILE.validateWith(true, true));
        group001.register(FILE.VALUE, "--contig")
                .defaultTo(ChromosomeTags.DEFAULT_FILE)
                .validateWith(FILE.validateWith(true, true, false, true))
                .setDescription("Specify the corresponding contig file.");


        CommandGroup group002 = PARSER.addCommandGroup("Summary View Options");
        group002.register(IType.NONE, "--add-md5")
                .setFormat("")
                .setDescription("Print the message-digest fingerprint (checksum) for file (which may take a long time to calculating for huge files).");
        group002.register(IType.NONE, "--add-subject")
                .setFormat("")
                .setDescription("Print subjects names of the GTB file.");
        group002.register(IType.NONE, "--add-tree")
                .setFormat("")
                .setDescription("Print information of the GTBTrees (chromosome only by default).");
        group002.register(IType.NONE, "--add-node")
                .setFormat("")
                .setDescription("Print information of the GTBNodes.");
        group002.register(IType.NONE, "--full", "-f")
                .setDescription("Print all abstract information of the GTB file (i.e., --list-baseInfo, --list-subject, --list-node).");


        CommandGroup group003 = PARSER.addCommandGroup("GTB View Options");
        group003.register(IType.NONE, "--list-subject-only")
                .setDescription("Print subjects names of the GTB file only.");
        group003.register(IType.NONE, "--list-position-only")
                .setDescription("Print coordinates (i.e., CHROM,POSITION) of the GTB file only.");
        group003.register(IType.NONE, "--list-site-only")
                .setFormat("")
                .setDescription("Print coordinates, alleles and INFOs (i.e., CHROM,POSITION,REF,ALT,INFO) of the GTB file.");


        CommandGroup group004 = PARSER.addCommandGroup("Subset Selection Options");
        group004.register(STRING.ARRAY_COMMA, "--subject", "-s")
                .setDescription("Print the information of the specified subjects. Subject name can be stored in a file with ',' delimited form, and pass in via '-s @file'.");
        group004.register(INTEGER.LABEL_RANGE, "--range", "-r")
                .arity(-1)
                .setFormat("--range <chrom>:<minPos>-<maxPos> <chrom>:<minPos>-<maxPos> ...")
                .setDescription("Print the information by position range.");
        group004.register(FILE.VALUE, "--random")
                .setDescription("Print the information by position. (An inputFile is needed here, with each line contains 'chrom,position' or 'chrom position'.");
        group004.register(INTEGER.LABEL_RANGE, "--retain-node")
                .arity(-1)
                .setFormat("--retain-node <string>:<int>-<int> <string>:<int>-<int> ...")
                .setDescription("Print variants in the specified coordinate range of the specified chromosome.");
        group004.register(INTEGER.RANGE, "--seq-ac")
                .validateWith(INTEGER.validateWith(0))
                .setDescription("Exclude variants with the alternate allele count (AC) per variant out of the range [minAc, maxAc].");
        group004.register(DOUBLE.RANGE, "--seq-af")
                .validateWith(DOUBLE.validateWith(0.0d, 1.0d))
                .setDescription("Exclude variants with the alternate allele frequency (AF) per variant out of the range [minAf, maxAf].");
        group004.register(INTEGER.RANGE, "--seq-an")
                .validateWith(INTEGER.validateWith(0))
                .setDescription("Exclude variants with the non-missing allele number (AN) per variant out of the range [minAn, maxAn].");
        group004.register(INTEGER.VALUE, "--max-allele")
                .defaultTo(15)
                .validateWith(INTEGER.validateWith(2, 15))
                .setDescription("Exclude variants with alleles over --max-allele.");


        PARSER.addRule(MUTUAL_EXCLUSION, 3, "--list-site-only", "--list-position-only", "--list-subject-only", "--full", "--add-node", "--add-tree", "--add-subject", "--add-md5");
        PARSER.addRule(AT_MOST, 1, "--list-subject-only", "--list-position-only", "--list-site-only");
        PARSER.addRule(MUTUAL_EXCLUSION, 8, "--subject", "--range", "--random", "--retain-node", "--seq-ac", "--seq-af", "--seq-an", "--max-allele", "--full", "--add-node", "--add-tree", "--add-subject", "--add-md5", "--list-subject-only");
    }
}