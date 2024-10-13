package no.ion.gh.main;

import java.io.PrintStream;

public record Environment(PrintStream out, PrintStream err) {
    public static Environment default_() { return new Environment(System.out, System.err); }
}
