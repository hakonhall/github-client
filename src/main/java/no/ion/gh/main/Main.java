package no.ion.gh.main;

import no.ion.gh.cli.gh;
import no.ion.gh.util.GHException;

import java.io.PrintStream;

public class Main {
    public static void main(String... args) {
        System.exit(main(Environment.default_(), args));
    }

    public static int main(Environment environment, String... args) {
        var gh = new gh(environment);
        try {
            gh.main(args);
        } catch (GHException e) {
            int exitCode = e.exitCode();
            PrintStream printStream = exitCode == 0 ? environment.out() : environment.err();
            printStream.print(e.multiLineDetail());
            return exitCode;
        } catch (RuntimeException e) {
            e.printStackTrace(environment.err());
            return 1;
        }

        return 0;
    }
}
