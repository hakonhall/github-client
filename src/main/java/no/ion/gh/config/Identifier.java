package no.ion.gh.config;

import no.ion.gh.text.Char;

public record Identifier(String value) {
    public boolean isValid(String value) {
        if (value == null) return false;
        if (value.isEmpty()) return false;
        if (!new Char(value.charAt(0)).isJavaIdentifierStart()) return false;
        for (int i = 1; i < value.length(); ++i) {
            var c = new Char(value.charAt(i));
            if (!c.isJavaIdentifierPart())
                return false;
        }
        return true;
    }
    public Identifier {
        if (!isValid(value))
            throw new IllegalArgumentException("Invalid identifier: " + value);
    }
}
