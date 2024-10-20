package no.ion.gh.text;

public record CodePoint(int value) {
    public static CodePoint fromChar(char value) { return new CodePoint(value); }
    public static CodePoint fromInt(int value) { return new CodePoint(value); }
    public static CodePoint fromString(String string, int index) { return fromInt(string.codePointAt(index)); }

    public CodePoint {
        if (!Character.isValidCodePoint(value))
            throw new IllegalArgumentException("Invalid code point value: " + value);
    }

    /** Returns the number of chars needed to represent this code point, i.e. {@code toString().length()} but faster. Either 1 or 2. */
    public int length() { return Character.charCount(value); }

    public int toInt() { return value; }

    public String toString() { return Character.toString(value); }
}
