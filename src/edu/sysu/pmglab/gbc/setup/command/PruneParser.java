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
import java.util.Map;

import static edu.sysu.pmglab.commandParser.CommandItem.*;
import static edu.sysu.pmglab.commandParser.CommandRule.EQUAL;

class PruneParser {
    /**
     * build by: CommandParser-1.1
     * time: 2022-06-07 03:27:48
     */
    private static final CommandParser PARSER = new CommandParser(false);


    private final CommandOptions options;
    public final CommandOption<?> help;
    public final CommandOption<File> prune;
    public final CommandOption<File> contig;
    public final CommandOption<File> output;
    public final CommandOption<?> yes;
    public final CommandOption<Map<String, int[]>> deleteNode;
    public final CommandOption<Map<String, int[]>> retainNode;
    public final CommandOption<String[]> deleteChrom;
    public final CommandOption<String[]> retainChrom;

    PruneParser(String... args) {
        this.options = PARSER.parse(args);
        this.help = new CommandOption<>("--help", this.options);
        this.prune = new CommandOption<>("prune", this.options);
        this.contig = new CommandOption<>("--contig", this.options);
        this.output = new CommandOption<>("--output", this.options);
        this.yes = new CommandOption<>("--yes", this.options);
        this.deleteNode = new CommandOption<>("--delete-node", this.options);
        this.retainNode = new CommandOption<>("--retain-node", this.options);
        this.deleteChrom = new CommandOption<>("--delete-chrom", this.options);
        this.retainChrom = new CommandOption<>("--retain-chrom", this.options);
    }

    public static PruneParser parse(String... args) {
        return new PruneParser(args);
    }

    public static PruneParser parse(File argsFile) throws IOException {
        return new PruneParser(CommandParser.readFromFile(argsFile));
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
        PARSER.setProgramName("prune <input> -o <output>");
        PARSER.offset(0);
        PARSER.debug(true);
        PARSER.usingAt(true);
        PARSER.setMaxMatchedNum(-1);
        PARSER.setAutoHelp(true);
        PARSER.setUsageStyle(new DefaultStyleUsage("Usage: ", " [options]", "", 2, 2, 100, false, "*", "^"));


        CommandGroup group001 = PARSER.addCommandGroup("Options");
        group001.register(IType.NONE, "--help", "-help", "-h")
                .addOptions(HELP, HIDDEN);
        group001.register(FILE.VALUE, "prune")
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
        group001.register(INTEGER.LABEL_ARRAY, "--delete-node")
                .arity(-1)
                .setDescription("Delete the specified GTBNodes.");
        group001.register(INTEGER.LABEL_ARRAY, "--retain-node")
                .arity(-1)
                .setDescription("Retain the specified GTBNodes.");
        group001.register(STRING.ARRAY_COMMA, "--delete-chrom")
                .setDescription("Delete the specified Chromosomes.");
        group001.register(STRING.ARRAY_COMMA, "--retain-chrom")
                .setDescription("Retain the specified Chromosomes.");


        PARSER.addRule(EQUAL, 1, "--delete-node", "--retain-node", "--delete-chrom", "--retain-chrom");
    }
}