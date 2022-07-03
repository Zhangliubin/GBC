package edu.sysu.pmglab.gbc.setup.command;

import edu.sysu.pmglab.commandParser.CommandGroup;
import edu.sysu.pmglab.commandParser.CommandOption;
import edu.sysu.pmglab.commandParser.CommandOptions;
import edu.sysu.pmglab.commandParser.CommandParser;
import edu.sysu.pmglab.commandParser.types.FILE;
import edu.sysu.pmglab.commandParser.types.INTEGER;
import edu.sysu.pmglab.commandParser.types.IType;
import edu.sysu.pmglab.commandParser.types.STRING;
import edu.sysu.pmglab.commandParser.usage.DefaultStyleUsage;
import edu.sysu.pmglab.container.File;
import edu.sysu.pmglab.gbc.constant.ChromosomeTags;

import java.io.IOException;

import static edu.sysu.pmglab.commandParser.CommandItem.*;
import static edu.sysu.pmglab.commandParser.CommandRule.AT_LEAST;
import static edu.sysu.pmglab.commandParser.CommandRule.MUTUAL_EXCLUSION;

class ResetSubjectParser {
    /**
     * build by: CommandParser-1.1
     * time: 2022-06-07 02:58:32
     */
    private static final CommandParser PARSER = new CommandParser(false);


    private final CommandOptions options;
    public final CommandOption<?> help;
    public final CommandOption<File> resetSubject;
    public final CommandOption<File> contig;
    public final CommandOption<File> output;
    public final CommandOption<?> yes;
    public final CommandOption<String[]> subject;
    public final CommandOption<String> prefix;
    public final CommandOption<String> suffix;
    public final CommandOption<Integer> begin;

    ResetSubjectParser(String... args) {
        this.options = PARSER.parse(args);
        this.help = new CommandOption<>("--help", this.options);
        this.resetSubject = new CommandOption<>("reset-subject", this.options);
        this.contig = new CommandOption<>("--contig", this.options);
        this.output = new CommandOption<>("--output", this.options);
        this.yes = new CommandOption<>("--yes", this.options);
        this.subject = new CommandOption<>("--subject", this.options);
        this.prefix = new CommandOption<>("--prefix", this.options);
        this.suffix = new CommandOption<>("--suffix", this.options);
        this.begin = new CommandOption<>("--begin", this.options);
    }

    public static ResetSubjectParser parse(String... args) {
        return new ResetSubjectParser(args);
    }

    public static ResetSubjectParser parse(File argsFile) throws IOException {
        return new ResetSubjectParser(CommandParser.readFromFile(argsFile));
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
        PARSER.setProgramName("reset-subject <input> -o <output>");
        PARSER.offset(0);
        PARSER.debug(true);
        PARSER.usingAt(true);
        PARSER.setMaxMatchedNum(-1);
        PARSER.setAutoHelp(true);
        PARSER.setUsageStyle(DefaultStyleUsage.UNIX_TYPE_1);


        CommandGroup group001 = PARSER.addCommandGroup("Options");
        group001.register(IType.NONE, "--help", "-help", "-h")
                .addOptions(HELP, HIDDEN);
        group001.register(FILE.VALUE, "reset-subject")
                .addOptions(REQUEST, HIDDEN)
                .validateWith(FILE.validateWith(true, true));
        group001.register(FILE.VALUE, "--contig")
                .defaultTo(ChromosomeTags.DEFAULT_FILE)
                .validateWith(FILE.validateWith(true, true, false, true))
                .setDescription("Specify the corresponding contig file.");
        group001.register(FILE.VALUE, "--output", "-o")
                .addOptions(REQUEST)
                .setDescription("Set the output file.");
        group001.register(IType.NONE, "--yes", "-y")
                .setDescription("Overwrite output file without asking.");
        group001.register(STRING.ARRAY_COMMA, "--subject")
                .setDescription("Reset subject names (request that same subject number and no duplicated names) for gtb file directly. Subject names can be stored in a file with ',' delimited form, and pass in via '--subject @file'.");
        group001.register(STRING.VALUE, "--prefix")
                .defaultTo("S_")
                .setDescription("Use the format `[prefix][number][suffix]` to reset the subject names.");
        group001.register(STRING.VALUE, "--suffix")
                .setDescription("Use the format `[prefix][number][suffix]` to reset the subject names.");
        group001.register(INTEGER.VALUE, "--begin")
                .defaultTo(1)
                .setDescription("Use the format `[prefix][number][suffix]` to reset the subject names.");


        PARSER.addRule(MUTUAL_EXCLUSION, 1, "--subject", "--prefix", "--suffix", "--begin");
        PARSER.addRule(AT_LEAST, 1, "--begin", "--suffix", "--prefix", "--subject");
    }
}