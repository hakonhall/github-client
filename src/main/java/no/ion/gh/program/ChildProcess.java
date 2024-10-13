package no.ion.gh.program;

import no.ion.gh.cli.Context;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static no.ion.gh.util.Exceptions.uncheckIO;
import static no.ion.gh.util.Exceptions.uncheckInterrupted;

public class ChildProcess {
    private final Context context;
    private final List<String> command;
    private final Process process;

    public ChildProcess(Context context, List<String> command, Process process) {
        this.context = context;
        this.command = List.copyOf(command);
        this.process = process;
    }

    public TerminatedChildProcess readStdoutAndWaitForTermination() {
        InputStream inputStream = process.getInputStream();
        byte[] bytes = uncheckIO(inputStream::readAllBytes);
        var stdout = new String(bytes, StandardCharsets.UTF_8);
        int exitCode = uncheckInterrupted(process::waitFor);
        return new TerminatedChildProcess(exitCode, stdout);
    }
}
