package no.ion.gh.util;

import no.ion.gh.cli.ArgumentIterator;

public class GHException extends RuntimeException {

    private final int exitCode;
    private final String multiLineDetail;

    public static GHException ofInvalidUsage(String message) {
        return new GHException(message, 1, "error: " + message + "\n" +
                                           "See --help for usage\n");
    }

    public static GHException ofInvalidUsage_unknownOption(String option) {
        return ofInvalidUsage("Unknown option: " + option);
    }

    public static GHException ofInvalidUsage_extraneousArguments(ArgumentIterator iterator) {
        return ofInvalidUsage("Extraneous arguments starting with: " + iterator.get());
    }

    public static GHException ofInvalidUsage_unknownSubcommand(String subcommand) {
        return ofInvalidUsage("Unknown subcommand: " + subcommand);
    }

    public static GHException ofInvalidUsage_missingArgument(String argument) {
        return ofInvalidUsage("Missing required argument: " + argument);
    }

    public static GHException ofHelp(String multiLineHelpText) {
        return new GHException("User asked for help", 0, multiLineHelpText);
    }

    public static GHException ofUserError(String message) {
        return new GHException(message, 1, "error: " + message + '\n');
    }

    public static GHException ofExternalCommandError(int exitCode) {
        return new GHException("", exitCode, "");
    }

    private GHException(String message, int exitCode, String multiLineDetail) {
        super(message);
        this.exitCode = exitCode;
        this.multiLineDetail = multiLineDetail;
    }

    public int exitCode() { return exitCode; }
    public String multiLineDetail() { return multiLineDetail; }
}
