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

class ConcatParser {
    /**
     * build by: CommandParser-1.1
     * time: 2022-06-07 01:53:55
     */
    private static final CommandParser PARSER = new CommandParser(false);
    
    private final CommandOptions options;
    public final CommandOption<?> help;
    public final CommandOption<File[]> concat;
    public final CommandOption<File> contig;
    public final CommandOption<File> output;
    public final CommandOption<?> yes;

    ConcatParser(String... args) {
        this.options = PARSER.parse(args);
        this.help = new CommandOption<>("--help", this.options);
        this.concat = new CommandOption<>("concat", this.options);
        this.contig = new CommandOption<>("--contig", this.options);
        this.output = new CommandOption<>("--output", this.options);
        this.yes = new CommandOption<>("--yes", this.options);
    }

    public static ConcatParser parse(String... args) {
        return new ConcatParser(args);
    }

    public static ConcatParser parse(File argsFile) throws IOException {
        return new ConcatParser(CommandParser.readFromFile(argsFile));
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
        PARSER.setProgramName("concat <input(s)> -o <output>");
        PARSER.offset(0);
        PARSER.debug(true);
        PARSER.usingAt(true);
        PARSER.setMaxMatchedNum(-1);
        PARSER.setAutoHelp(true);
        PARSER.setUsageStyle(DefaultStyleUsage.UNIX_TYPE_1);

        CommandGroup group001 = PARSER.addCommandGroup("Options");
        group001.register(IType.NONE, "--help", "-help", "-h")
                .addOptions(HELP, HIDDEN);
        group001.register(FILE.ARRAY, "concat")
                .arity(-1)
                .addOptions(REQUEST, HIDDEN)
                .validateWith(FILE.validateWith(true));
        group001.register(FILE.VALUE, "--contig")
                .defaultTo(ChromosomeTags.DEFAULT_FILE)
                .validateWith(FILE.validateWith(true, true, false, true))
                .setDescription("Specify the corresponding contig file.");
        group001.register(FILE.VALUE, "--output", "-o")
                .addOptions(REQUEST)
                .setDescription("Set the output file.");
        group001.register(IType.NONE, "--yes", "-y")
                .setDescription("Overwrite output file without asking.");
    }
}