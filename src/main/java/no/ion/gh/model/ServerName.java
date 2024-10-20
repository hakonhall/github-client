package no.ion.gh.model;

public record ServerName(String value) {
    @Override
    public String toString() { return value; }
}
