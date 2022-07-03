package edu.sysu.pmglab.gbc.setup.command;

import edu.sysu.pmglab.commandParser.CommandGroup;
import edu.sysu.pmglab.commandParser.CommandOption;
import edu.sysu.pmglab.commandParser.CommandOptions;
import edu.sysu.pmglab.commandParser.CommandParser;
import edu.sysu.pmglab.commandParser.types.FILE;
import edu.sysu.pmglab.commandParser.types.IType;
import edu.sysu.pmglab.commandParser.types.STRING;
import edu.sysu.pmglab.commandParser.usage.DefaultStyleUsage;
import edu.sysu.pmglab.container.File;
import edu.sysu.pmglab.gbc.constant.ChromosomeTags;

import java.io.IOException;

import static edu.sysu.pmglab.commandParser.CommandItem.*;

class SplitParser {
    /**
     * build by: CommandParser-1.1
     * time: 2022-06-08 03:03:38
     */
    private static final CommandParser PARSER = new CommandParser(false);


    private final CommandOptions options;
    public final CommandOption<?> help;
    public final CommandOption<File> split;
    public final CommandOption<File> contig;
    public final CommandOption<File> output;
    public final CommandOption<String> by;
    public final CommandOption<?> yes;

    SplitParser(String... args) {
        this.options = PARSER.parse(args);
        this.help = new CommandOption<>("--help", this.options);
        this.split = new CommandOption<>("split", this.options);
        this.contig = new CommandOption<>("--contig", this.options);
        this.output = new CommandOption<>("--output", this.options);
        this.by = new CommandOption<>("--by", this.options);
        this.yes = new CommandOption<>("--yes", this.options);
    }

    public static SplitParser parse(String... args) {
        return new SplitParser(args);
    }

    public static SplitParser parse(File argsFile) throws IOException {
        return new SplitParser(CommandParser.readFromFile(argsFile));
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
        PARSER.setProgramName("split <input> -o <output>");
        PARSER.offset(0);
        PARSER.debug(true);
        PARSER.usingAt(true);
        PARSER.setMaxMatchedNum(-1);
        PARSER.setAutoHelp(true);
        PARSER.setUsageStyle(DefaultStyleUsage.UNIX_TYPE_1);


        CommandGroup group001 = PARSER.addCommandGroup("Options");
        group001.register(IType.NONE, "--help", "-help", "-h")
                .addOptions(HELP, HIDDEN);
        group001.register(FILE.VALUE, "split")
                .addOptions(REQUEST, HIDDEN)
                .validateWith(FILE.validateWith(true, true));
        group001.register(FILE.VALUE, "--contig")
                .defaultTo(ChromosomeTags.DEFAULT_FILE)
                .validateWith(FILE.validateWith(true, true, false, true))
                .setDescription("Specify the corresponding contig file.");
        group001.register(FILE.VALUE, "--output", "-o")
                .addOptions(REQUEST)
                .setDescription("Set the output folder.");
        group001.register(STRING.VALUE, "--by")
                .defaultTo("chromosome")
                .validateWith(STRING.validateWith(false, true, "chromosome", "node"))
                .setFormat("--by <string>")
                .setDescription("Split input files by node-level/chromosome-level into multiple subfiles, which can be rejoined by the concat mode.");
        group001.register(IType.NONE, "--yes", "-y")
                .setDescription("Overwrite output file without asking.");
    }
}