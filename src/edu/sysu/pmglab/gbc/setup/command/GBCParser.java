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

import java.io.IOException;

import static edu.sysu.pmglab.commandParser.CommandItem.HELP;
import static edu.sysu.pmglab.commandParser.CommandItem.HIDDEN;
import static edu.sysu.pmglab.commandParser.CommandRule.EQUAL;

class GBCParser {
    /**
     * build by: CommandParser-1.1
     * time: 2022-06-06 15:58:06
     */
    private static final CommandParser PARSER = new CommandParser(false);


    private final CommandOptions options;
    public final CommandOption<?> help;
    public final CommandOption<String[]> build;
    public final CommandOption<String[]> extract;
    public final CommandOption<String[]> show;
    public final CommandOption<String[]> sort;
    public final CommandOption<String[]> concat;
    public final CommandOption<String[]> merge;
    public final CommandOption<String[]> resetSubject;
    public final CommandOption<String[]> prune;
    public final CommandOption<String[]> alleleCheck;
    public final CommandOption<String[]> split;
    public final CommandOption<String[]> ld;
    public final CommandOption<String[]> index;
    public final CommandOption<String[]> bgzip;
    public final CommandOption<String[]> md5;
    public final CommandOption<String[]> download;
    public final CommandOption<File[]> update;

    GBCParser(String... args) {
        this.options = PARSER.parse(args);
        this.help = new CommandOption<>("--help", this.options);
        this.build = new CommandOption<>("build", this.options);
        this.extract = new CommandOption<>("extract", this.options);
        this.show = new CommandOption<>("show", this.options);
        this.sort = new CommandOption<>("sort", this.options);
        this.concat = new CommandOption<>("concat", this.options);
        this.merge = new CommandOption<>("merge", this.options);
        this.resetSubject = new CommandOption<>("reset-subject", this.options);
        this.prune = new CommandOption<>("prune", this.options);
        this.alleleCheck = new CommandOption<>("allele-check", this.options);
        this.split = new CommandOption<>("split", this.options);
        this.ld = new CommandOption<>("ld", this.options);
        this.index = new CommandOption<>("index", this.options);
        this.bgzip = new CommandOption<>("bgzip", this.options);
        this.md5 = new CommandOption<>("md5", this.options);
        this.download = new CommandOption<>("download", this.options);
        this.update = new CommandOption<>("update", this.options);
    }

    public static GBCParser parse(String... args) {
        return new GBCParser(args);
    }

    public static GBCParser parse(File argsFile) throws IOException {
        return new GBCParser(CommandParser.readFromFile(argsFile));
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
        PARSER.setProgramName("java -jar gbc.jar [mode/tool]");
        PARSER.offset(0);
        PARSER.debug(false);
        PARSER.usingAt(true);
        PARSER.setMaxMatchedNum(1);
        PARSER.setAutoHelp(true);
        PARSER.setUsageStyle(new DefaultStyleUsage("Usage: ", " [options]", "Version: GBC-1.2 (last edited on 2022.06.20, http://pmglab.top/gbc)", 2, 2, 80, false, "*", "^"));


        CommandGroup group001 = PARSER.addCommandGroup("Options");
        group001.register(IType.NONE, "--help", "-help", "-h")
                .addOptions(HELP, HIDDEN);


        CommandGroup group002 = PARSER.addCommandGroup("Mode");
        group002.register(STRING.ARRAY, "build")
                .arity(-1)
                .setFormat("build <input(s)> -o <output> [options]")
                .setDescription("Compress and build *.gtb for vcf/vcf.gz files.");
        group002.register(STRING.ARRAY, "extract")
                .arity(-1)
                .setFormat("extract <input> -o <output> [options]")
                .setDescription("Retrieve variants from *.gtb file, and export them to (compressed) VCF format or GTB format.");
        group002.register(STRING.ARRAY, "show")
                .arity(-1)
                .setFormat("show <input> [options]")
                .setDescription("Display summary of the GTB File.");
        group002.register(STRING.ARRAY, "sort")
                .arity(-1)
                .setFormat("sort <input> -o <output> [options]")
                .setDescription("Sort variants in GTB by coordinates (chromosome and position).");
        group002.register(STRING.ARRAY, "concat")
                .arity(-1)
                .setFormat("concat <input(s)> -o <output> [options]")
                .setDescription("Concatenate multiple VCF files. All source files must have the same subjects columns appearing in the same order with entirely different sites, and all files must have to be the same in parameters of the status.");
        group002.register(STRING.ARRAY, "merge")
                .arity(-1)
                .setFormat("merge <input(s)> -o <output> [options]")
                .setDescription("Merge multiple GTB files (with non-overlapping subject sets) into a single GTB file.");
        group002.register(STRING.ARRAY, "reset-subject")
                .arity(-1)
                .setFormat("reset-subject <input> -o <output> [options]")
                .setDescription("Reset subject names (request that same subject number and no duplicated names) for gtb file directly. Subject names can be stored in a file with ',' delimited form, and pass in via '--reset-subject @file'.");
        group002.register(STRING.ARRAY, "prune")
                .arity(-1)
                .setFormat("prune <input> -o <output> [options]")
                .setDescription("Prune GTB files by node-level or chromosome-level.");
        group002.register(STRING.ARRAY, "allele-check")
                .arity(-1)
                .setFormat("allele-check <template_input> <input> -o <output> [options]")
                .setDescription("Correct for potential complementary strand errors based on allele labels (A and C, T and G; only biallelic variants are supported).");
        group002.register(STRING.ARRAY, "split")
                .arity(-1)
                .setFormat("split <input> -o <output> [options]")
                .setDescription("Split a single GTB file into multiple subfiles (e.g. split by chromosome).");
        group002.register(STRING.ARRAY, "ld")
                .arity(-1)
                .setFormat("ld <input> -o <output> [options]")
                .setDescription("Calculate pairwise the linkage disequilibrium or genotypic correlation.");
        group002.register(STRING.ARRAY, "index")
                .arity(-1)
                .setFormat("index <input (VCF or GTB)> -o <output> [options]")
                .setDescription("Index contig file for specified VCF file or reset contig file for specified GTB file.");


        CommandGroup group003 = PARSER.addCommandGroup("Tool");
        group003.register(STRING.ARRAY, "bgzip")
                .arity(-1)
                .setDescription("Use parallel bgzip to compress a single file.");
        group003.register(STRING.ARRAY, "md5")
                .arity(-1)
                .setDescription("Calculate a message-digest fingerprint (checksum) for file.");
        group003.register(STRING.ARRAY, "download")
                .arity(-1)
                .setFormat("download <string> <string> ...")
                .setDescription("Download resources from an URL address.");
        group003.register(FILE.ARRAY, "update")
                .defaultTo("./gbc.jar")
                .setFormat("update <gbc.jar>")
                .setDescription("Update GBC software packages.");


        PARSER.addRule(EQUAL, 1, "build", "extract", "show", "sort", "concat", "merge", "reset-subject", "index", "prune", "allele-check", "split", "ld", "bgzip", "md5", "download", "update");
    }
}