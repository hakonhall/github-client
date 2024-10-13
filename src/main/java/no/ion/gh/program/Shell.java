package no.ion.gh.program;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class Shell {
    /**
     * Return a string that could be pasted to a terminal command-line, and that would execute
     * the ProcessBuilder command given by the argument.
     */
    public static String processBuilderCommand2CommandLine(List<String> command) {
        if (command.isEmpty()) return "";

        return command.stream()
                      .map(Shell::escape)
                      .collect(Collectors.joining(" "));
    }

    /**
     * Returns a string that has been escaped, such that when the shell parses it, it will be unescaped to
     * reconstruct the raw string.
     */
    private static String escape(String raw) {
        // If there are only safe chars and \, do not quote (but escape \).
        // If there are newlines or tabs, use $'' and escape ', \, newline, and tab.
        // If there are no ', use ''.
        // Use "" and escape ", $, `, \, and !.

        char[] chars = raw.toCharArray();
        Quotation quotation = decideQuotation(chars);

        return switch (quotation) {
            case NONE -> raw;
            case NONE_WITH_ESCAPE -> escapeBackslashes(raw);
            case SINGLE -> "'" + raw + "'";
            case DOUBLE -> escape(chars, "\"", "\"", (builder, c) -> {
                switch (c) {
                    case '"', '$', '`', '\\', '!' -> builder.append('\\').append(c);
                    default -> builder.append(c);
                }
            });
            case DOLLAR_SINGLE -> escape(chars, "$'", "'", (builder, c) -> {
                switch (c) {
                    case '\'', '\\' -> builder.append('\\').append(c);
                    case '\n' -> builder.append("\\n");
                    case '\t' -> builder.append("\\t");
                    default -> builder.append(c);
                }
            });
        };
    }

    /** Does not need to be escaped (or quoted). */
    private static boolean safe(char c) {
        if ('a' <= c && c <= 'z') return true;
        if ('A' <= c && c <= 'Z') return true;
        if ('0' <= c && c <= '9') return true;
        if ("%+,-./:=@^_".indexOf(c) != -1) return true;
        return false;
    }

    enum Quotation { NONE, NONE_WITH_ESCAPE, DOUBLE, SINGLE, DOLLAR_SINGLE }

    private static Quotation decideQuotation(char[] chars) {
        boolean hasBackslash = false;
        boolean hasSingleQuote = false;
        boolean other = false;

        for (char c : chars) {
            if (safe(c)) continue;
            switch (c) {
                case '\\' -> hasBackslash = true;
                case '\n', '\t' -> { return Quotation.DOLLAR_SINGLE; }
                case '\'' -> hasSingleQuote = true;
                default -> other = true;
            }
        }

        if (hasSingleQuote) return Quotation.DOUBLE;
        if (other) return Quotation.SINGLE;
        if (hasBackslash) return Quotation.NONE_WITH_ESCAPE;
        return Quotation.NONE;
    }

    private static String escapeBackslashes(String haystack) {
        var builder = new StringBuilder();
        int startIndex = 0;
        do {
            int endIndex = haystack.indexOf('\\', startIndex);
            if (endIndex == -1) {
                builder.append(haystack, startIndex, haystack.length());
                return builder.toString();
            }

            builder.append(haystack, startIndex, endIndex).append('\\');
            startIndex = endIndex; // Include \ at endIndex in next segment.
        } while (true);
    }

    private static String escape(char[] chars, String prefix, String suffix, BiConsumer<StringBuilder, Character> mapper) {
        StringBuilder builder = new StringBuilder();
        builder.append(prefix);

        for (char c : chars) {
            mapper.accept(builder, c);
        }

        builder.append(suffix);
        return builder.toString();
    }
}
