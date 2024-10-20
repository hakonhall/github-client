package no.ion.gh.cli;

import no.ion.gh.config.Config;
import no.ion.gh.git.Git;
import no.ion.gh.github.GitHub;
import no.ion.gh.io.UnixPath;
import no.ion.gh.main.Environment;
import no.ion.gh.model.Server;

import java.io.PrintStream;
import java.util.Objects;

public class Context {
    private final Environment environment;
    private final Verbosity verbosity = new Verbosity();

    private volatile Config config = null;
    private volatile Git git = null;

    Context(Environment environment) {
        this.environment = environment;
    }

    public PrintStream out() { return environment.out(); }
    public PrintStream err() { return environment.err(); }

    public Verbosity verbosity() { return verbosity; }

    public Git git() {
        if (git == null) {
            git = Git.open(this);
        }
        return git;
    }

    public Config resolveConfig() {
        if (config == null) {
            config = Config.read(configFile());
            Objects.requireNonNull(config, "config cannot be null");
        }
        return config;
    }

    private UnixPath configDirectory() { return UnixPath.of(System.getProperty("user.home")).resolve(".gh"); }
    private UnixPath configFile() { return configDirectory().resolve("config"); }

    public GitHub gitHubClientTo(Server server) {
        return new GitHub(this, server);
    }
}
