package no.ion.gh.util;

import no.ion.gh.cli.ArgumentIterator;
import no.ion.gh.io.UnixPath;
import no.ion.gh.model.ServerName;
import no.ion.gh.text.TextCursor;

import java.net.URI;

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

    public static GHException ofInvalidUsage_badArgument(String argument, String problem) {
        return ofInvalidUsage("Bad argument: " + argument + ": " + problem);
    }

    public static GHException ofHelp(String multiLineHelpText) {
        return new GHException("User asked for help", 0, multiLineHelpText);
    }

    public static GHException ofUserError(String message) {
        return new GHException(message, 1, "error: " + message + '\n');
    }

    public static GHException ofInvalidConfig(UnixPath path, TextCursor cursor, String problem) {
        String message = "invalid config file: " + problem + ", at\n" +
                         path + ':' + (cursor.lineIndex() + 1) + ':' + cursor.columnIndex() + ":\n" +
                         cursor.line() + "\n" +
                         " ".repeat(cursor.columnIndex()) + "^\n";
        return ofUserError(message);
    }

    public static GHException ofMissingServerSetting(ServerName server, String settingName) {
        return ofUserError("Server " + server.value() + " is missing a setting for " + settingName);
    }

    public static GHException ofHttpFailure(String method, URI url, int statusCode, String body) {
        return new GHException("", 1, method + " against " + url + " failed with status " + statusCode + ": " + body + '\n');
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
