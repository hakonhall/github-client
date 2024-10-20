package no.ion.gh.cli;

public class Verbosity {
    private boolean logMutableCommand = false;
    private boolean logHttpIO = false;

    public Verbosity() {}

    public void setVerbose(boolean verbose) {
        logMutableCommand = logHttpIO = true;
    }

    public boolean logMutableCommand() { return logMutableCommand; }
    public boolean logCommand() { return logMutableCommand; }

    public boolean logRequest() { return logHttpIO; }
    public boolean logResponse() { return logHttpIO; }
}
