package no.ion.gh.text;

import java.util.List;
import java.util.function.Function;

public class Text {
    private final String string;
    private final List<CodePoint> codePoints;

    public static String replace(String haystack, char needle, String replacement) {
        var builder = new StringBuilder();
        int startIndex = 0;
        for (;;) {
            int endIndex = haystack.indexOf(needle, startIndex);
            if (endIndex == -1) {
                builder.append(haystack, startIndex, haystack.length());
                return builder.toString();
            }
            builder.append(haystack, startIndex, endIndex).append(replacement);
            startIndex = endIndex + 1;
        }
    }

    public static String replace(String string, Function<Character, String> mapper) {
        var builder = new StringBuilder();
        int startIndex = 0;
        for (int index = 0; index < string.length(); ++index) {
            String replacement = mapper.apply(string.charAt(index));
            if (replacement != null) {
                builder.append(string, startIndex, index); // may append 0 chars
                builder.append(replacement);
                startIndex = index + 1;
            }
        }
        builder.append(string, startIndex, string.length()); // may append 0 chars
        return builder.toString();
    }

    public Text(String string) {
        this.string = string;
        this.codePoints = string.codePoints().mapToObj(CodePoint::fromInt).toList();
    }

    public List<CodePoint> codePoints() { return codePoints; }
}
