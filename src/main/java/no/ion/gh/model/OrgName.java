package no.ion.gh.model;

/** GitHub organization name. */
public record OrgName(String name) {
    @Override
    public String toString() { return name; }
}
