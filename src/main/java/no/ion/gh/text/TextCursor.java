package no.ion.gh.text;

import java.util.Objects;

public class TextCursor {
    private final String text;

    private int index, lineIndex, columnIndex;

    public TextCursor(String text) {
        this.text = text;
        resetToFirstChar();
    }

    public TextCursor(TextCursor that) {
        this.text = that.text;
        this.index = that.index;
        this.lineIndex = that.lineIndex;
        this.columnIndex = that.columnIndex;
    }

    public TextCursor copy() { return new TextCursor(this); }

    public void resetToFirstChar() {
        index = lineIndex = columnIndex = 0;
    }

    public String text() { return text; }
    public int index() { return index; }
    public int lineIndex() { return lineIndex; }
    public int columnIndex() { return columnIndex; }

    public String line() {
        int startIndex = index - columnIndex;
        int endIndex = indexPast(text, startIndex, '\n');
        return text.substring(startIndex, endIndex);
    }

    public boolean eot() { return index == text.length(); }
    public boolean isValid() { return index < text.length(); }

    public char getRawChar() {
        assertNotEot();
        return text.charAt(index);
    }

    public Char getChar() { return new Char(getRawChar()); }

    public boolean isNewline() { return is('\n'); }
    public boolean is(char c) {
        assertNotEot();
        return text.charAt(index) == c;
    }

    public void advanceOneChar() {
        assertNotEot();

        if (is('\n')) {
            lineIndex++;
            columnIndex = 0;
        } else {
            columnIndex++;
        }
        index++;
    }

    /**
     * If the cursor does not point to '\n', advance one char. Repeat until
     * '\n' is found (and return true), or until EOT (and return false).
     */
    public boolean advanceToNewline() {
        int i = text.indexOf('\n', index);
        if (i == -1) {
            columnIndex += text.length() - index;
            index = text.length();
            return false;
        } else {
            columnIndex += i - index;
            index = i;
            return true;
        }
    }

    public boolean skip(char c) {
        if (eot()) return false;
        if (getRawChar() != c) return false;
        advanceOneChar();
        return true;
    }

    /**
     * Return the index of the first occurrence of char `needle` in `haystack`,
     * that occurs after index fromIndexExclusive. If no such char is found,
     * return `haystack.length()`.
     */
    private static int indexPast(String haystack, int fromIndexExclusive, char needle) {
        if (fromIndexExclusive == haystack.length())
            return fromIndexExclusive;
        if (fromIndexExclusive > haystack.length())
            throw new IndexOutOfBoundsException(fromIndexExclusive);
        int endIndex = haystack.indexOf(needle, fromIndexExclusive + 1);
        return endIndex != -1 ? endIndex : haystack.length();
    }

    private void assertNotEot() {
        if (eot())
            throw new IllegalStateException("At EOT");
    }

    @Override
    public String toString() {
        return text.substring(index);
    }

    /** WARNING: The text is compared with == and not equals, for performance. */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TextCursor that)) return false;
        return index == that.index && lineIndex == that.lineIndex && columnIndex == that.columnIndex && text == that.text;
    }

    @Override
    public int hashCode() {
        return Objects.hash(text, index, lineIndex, columnIndex);
    }
}
