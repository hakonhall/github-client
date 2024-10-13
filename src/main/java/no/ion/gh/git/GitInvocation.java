package no.ion.gh.git;

import no.ion.gh.cli.Context;
import no.ion.gh.program.ChildProcess;
import no.ion.gh.program.TerminatedChildProcess;
import no.ion.gh.program.Shell;
import no.ion.gh.util.GHException;

import java.util.ArrayList;
import java.util.List;

import static no.ion.gh.util.Exceptions.uncheckIO;

public class GitInvocation {
    private final Git git;
    private final List<String> command = new ArrayList<>();

    GitInvocation(Git git) {
        this.git = git;
        command.add(git.program());
    }

    public void addArguments(String... arguments) {
        for (var argument : arguments)
            command.add(argument);
    }

    /**
     * Spawn a git command that may potentially mutate the local file system, this git repository,
     * staging, a remote repository, or similar.  The git command's stdin is closed, stderr redirected
     * to environment err, and stdout to be returned by this method.
     *
     * @throws GHException if git returned a non-0 exit code.
     */
    public String executeMutating() throws GHException {
        Context.ExternalCommandVerbosity verbosity = git.context().externalCommandVerbosity();

        if (verbosity.logCommandBeforeMutableExecution()) {
            logCommand();
        }

        var processBuilder = new ProcessBuilder()
                .command(command)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectErrorStream(true);
        Process process = uncheckIO(processBuilder::start);
        var child = new ChildProcess(git.context(), command, process);
        TerminatedChildProcess completion = child.readStdoutAndWaitForTermination();
        int exitCode = completion.exitCode();
        if (exitCode != 0) {
            if (!verbosity.logCommandBeforeMutableExecution()) {
                logCommand();
            }

            String stdout = completion.stdout();
            int length = stdout.length();
            if (length > 0) {
                if (stdout.charAt(length - 1) == '\n') {
                    git.context().err().print(stdout);
                } else {
                    git.context().err().println(stdout);
                }
            }

            git.context().err().println("Terminated with exit code " + exitCode);

            throw GHException.ofExternalCommandError(1);
        }

        return completion.stdout();
    }

    private void logCommand() {
        String shellCommand = Shell.processBuilderCommand2CommandLine(command);
        git.context().out().println(shellCommand);
    }
}
