package no.ion.gh.cli;

import no.ion.gh.git.Git;
import no.ion.gh.main.Environment;

import java.io.PrintStream;

public class Context {
    private final Environment environment;

    private ExternalCommandVerbosity externalCommandVerbosity = ExternalCommandVerbosity.NONE;

    private volatile Git git = null;

    Context(Environment environment) {
        this.environment = environment;
    }

    public PrintStream out() { return environment.out(); }
    public PrintStream err() { return environment.err(); }

    public enum ExternalCommandVerbosity {
        /** Will not inform about external commands. */
        NONE,

        /** Write a command-line equivalent of the external command that is about to be executed, so environment out. */
        COMMAND_LINE,

        /** Same as {@link #COMMAND_LINE}, but only for mutating commands (writing local files, updating remote repos, etc.). */
        COMMAND_LINE_MUTATING;

        public boolean logCommandBeforeMutableExecution() { return this != NONE; }
    }

    public void setExternalCommandInfo(ExternalCommandVerbosity verbosity) {
        this.externalCommandVerbosity = verbosity;
    }

    public ExternalCommandVerbosity externalCommandVerbosity() { return externalCommandVerbosity; }

    public Git git() {
        if (git == null) {
            git = Git.open(this);
        }
        return git;
    }
}
