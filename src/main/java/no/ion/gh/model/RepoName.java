package no.ion.gh.model;

public record RepoName(String name) {
    @Override
    public String toString() { return name; }
}
