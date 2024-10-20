package no.ion.gh.text;

public record Char(char value) {
    public boolean is(char c) { return value == c; }
    public boolean isOneOf(String haystack) { return haystack.indexOf(value) != -1; }
    public boolean isWhitespace() { return Character.isWhitespace(value); }
    public boolean isStartOfLineComment() { return value == '#'; }
    public boolean isJavaIdentifierStart() { return Character.isJavaIdentifierStart(value); }
    public boolean isJavaIdentifierPart() { return Character.isJavaIdentifierPart(value); }
}
