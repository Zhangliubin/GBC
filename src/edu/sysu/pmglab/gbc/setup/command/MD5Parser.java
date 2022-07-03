package edu.sysu.pmglab.gbc.setup.command;

import edu.sysu.pmglab.commandParser.CommandGroup;
import edu.sysu.pmglab.commandParser.CommandOption;
import edu.sysu.pmglab.commandParser.CommandOptions;
import edu.sysu.pmglab.commandParser.CommandParser;
import edu.sysu.pmglab.commandParser.types.FILE;
import edu.sysu.pmglab.commandParser.types.IType;
import edu.sysu.pmglab.commandParser.usage.DefaultStyleUsage;
import edu.sysu.pmglab.container.File;

import java.io.IOException;

import static edu.sysu.pmglab.commandParser.CommandItem.*;

class MD5Parser {
    /**
     * build by: CommandParser-1.1
     * time: 2022-05-11 22:33:46
     */
    private static final CommandParser PARSER = new CommandParser(false);


    private final CommandOptions options;
    public final CommandOption<?> help;
    public final CommandOption<File[]> md5;
    public final CommandOption<?> oMd5;
    public final CommandOption<?> yes;

    public MD5Parser(String... args) {
        this.options = PARSER.parse(args);
        this.help = new CommandOption<>("--help", this.options);
        this.md5 = new CommandOption<>("md5", this.options);
        this.oMd5 = new CommandOption<>("--o-md5", this.options);
        this.yes = new CommandOption<>("--yes", this.options);
    }

    public MD5Parser(File argsFile) throws IOException {
        this(CommandParser.readFromFile(argsFile));
    }

    public static MD5Parser parse(String... args) {
        return new MD5Parser(args);
    }

    public static MD5Parser parse(File argsFile) throws IOException {
        return new MD5Parser(argsFile);
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

    @Override
    public String toString() {
        return this.options.toString();
    }

    static {
        PARSER.setProgramName("md5 <input(s)>");
        PARSER.offset(0);
        PARSER.debug(false);
        PARSER.usingAt(true);
        PARSER.setMaxMatchedNum(-1);
        PARSER.setAutoHelp(true);
        PARSER.setUsageStyle(DefaultStyleUsage.UNIX_TYPE_1);


        CommandGroup group001 = PARSER.addCommandGroup("Options");
        group001.register(IType.NONE, "--help", "-help", "-h")
                .addOptions(HELP, HIDDEN);
        group001.register(FILE.ARRAY, "md5")
                .arity(-1)
                .addOptions(REQUEST, HIDDEN)
                .validateWith(FILE.validateWith(true, true));
        group001.register(IType.NONE, "--o-md5")
                .setDescription("Generate *.md5 file(s) in-place.");
        group001.register(IType.NONE, "--yes", "-y")
                .setDescription("Overwrite output file without asking.");
    }
}