package edu.sysu.pmglab.gbc.setup.command;

import edu.sysu.pmglab.commandParser.CommandGroup;
import edu.sysu.pmglab.commandParser.CommandOption;
import edu.sysu.pmglab.commandParser.CommandOptions;
import edu.sysu.pmglab.commandParser.CommandParser;
import edu.sysu.pmglab.commandParser.types.FILE;
import edu.sysu.pmglab.commandParser.types.IType;
import edu.sysu.pmglab.commandParser.usage.DefaultStyleUsage;
import edu.sysu.pmglab.container.File;
import edu.sysu.pmglab.gbc.constant.ChromosomeTags;

import java.io.IOException;

import static edu.sysu.pmglab.commandParser.CommandItem.*;
import static edu.sysu.pmglab.commandParser.CommandRule.MUTUAL_EXCLUSION;

class IndexParser {
    /**
     * build by: CommandParser-1.1
     * time: 2022-06-08 03:46:38
     */
    private static final CommandParser PARSER = new CommandParser(false);


    private final CommandOptions options;
    public final CommandOption<?> help;
    public final CommandOption<File> index;
    public final CommandOption<?> deepScan;
    public final CommandOption<File> output;
    public final CommandOption<File> fromContig;
    public final CommandOption<File> toContig;
    public final CommandOption<?> yes;

    IndexParser(String... args) {
        this.options = PARSER.parse(args);
        this.help = new CommandOption<>("--help", this.options);
        this.index = new CommandOption<>("index", this.options);
        this.deepScan = new CommandOption<>("--deep-scan", this.options);
        this.output = new CommandOption<>("--output", this.options);
        this.fromContig = new CommandOption<>("--from-contig", this.options);
        this.toContig = new CommandOption<>("--to-contig", this.options);
        this.yes = new CommandOption<>("--yes", this.options);
    }

    public static IndexParser parse(String... args) {
        return new IndexParser(args);
    }

    public static IndexParser parse(File argsFile) throws IOException {
        return new IndexParser(CommandParser.readFromFile(argsFile));
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
        PARSER.setProgramName("index <input (VCF or GTB)> -o <output>");
        PARSER.offset(0);
        PARSER.debug(false);
        PARSER.usingAt(true);
        PARSER.setMaxMatchedNum(-1);
        PARSER.setAutoHelp(true);
        PARSER.setUsageStyle(DefaultStyleUsage.UNIX_TYPE_1);


        CommandGroup group001 = PARSER.addCommandGroup("Options");
        group001.register(IType.NONE, "--help", "-help", "-h")
                .addOptions(HELP, HIDDEN);
        group001.register(FILE.VALUE, "index")
                .addOptions(REQUEST, HIDDEN)
                .validateWith(FILE.validateWith(true, true));
        group001.register(IType.NONE, "--deep-scan")
                .setDescription("Scan all sites in the file to build the contig file.");
        group001.register(FILE.VALUE, "--output", "-o")
                .addOptions(REQUEST)
                .setDescription("Set the output file.");
        group001.register(FILE.VALUE, "--from-contig", "-from")
                .defaultTo(ChromosomeTags.DEFAULT_FILE)
                .validateWith(FILE.validateWith(true, true, false, true))
                .setDescription("Specify the corresponding contig file.");
        group001.register(FILE.VALUE, "--to-contig", "-to")
                .defaultTo(ChromosomeTags.DEFAULT_FILE)
                .validateWith(FILE.validateWith(true, true, false, true))
                .setFormat("'-to <file>'")
                .setDescription("Reset contig (chromosome marker in each gtb block header) for gtb file directly.");
        group001.register(IType.NONE, "--yes", "-y")
                .setDescription("Overwrite output file without asking.");


        PARSER.addRule(MUTUAL_EXCLUSION, 1, "--deep-scan", "--from-contig", "--to-contig");
    }
}