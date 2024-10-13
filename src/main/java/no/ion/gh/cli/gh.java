package no.ion.gh.cli;

import no.ion.gh.main.Environment;
import no.ion.gh.util.GHException;

public class gh {
    private final Context context;

    public void help() {
        throw GHException.ofHelp(
                """
                Usage: gh [GOPTION...] COMMAND [ARG...]
                Execute gh command.
                
                GLOBAL OPTIONS
                
                COMMANDS
                  clone
                """);
    }

    public gh(Environment environment) {
        this.context = new Context(environment);
    }

    public void main(String... args) throws GHException {
        var iterator = new ArgumentIterator(args);

        global_options_loop:
        for (; iterator.isValid(); iterator.increment()) {
            String argument = iterator.get();
            switch (argument) {
                case "-h", "--help" -> help();
                default -> {
                    if (argument.startsWith("-"))
                        throw GHException.ofInvalidUsage_unknownOption(argument);
                    break global_options_loop;
                }
            }
        }

        if (iterator.isDone()) help();

        var subcommandName = iterator.getAndIncrement();

        Subcommand subcommand = switch (subcommandName) {
            case "clone" -> new gh_clone(context);
            default -> throw GHException.ofInvalidUsage_unknownSubcommand(subcommandName);
        };

        subcommand.main(iterator);
    }
}
