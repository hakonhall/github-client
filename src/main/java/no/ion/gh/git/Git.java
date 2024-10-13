package no.ion.gh.git;

import no.ion.gh.cli.Context;
import no.ion.gh.io.UnixPath;
import no.ion.gh.util.GHException;

public class Git {
    private final Context context;
    private final String program;

    public static Git open(Context context) { return open(context, "git"); }

    private static Git open(Context context, String gitProgram) {
        if (!UnixPath.of(gitProgram).isProgram())
            throw GHException.ofUserError("Program not found: " + gitProgram);
        return new Git(context, gitProgram);
    }

    private Git(Context context, String program) {
        this.context = context;
        this.program = program;
    }

    Context context() { return context; }

    String program() { return program; }

    public GitInvocation newInvocation() { return new GitInvocation(this); }
}
