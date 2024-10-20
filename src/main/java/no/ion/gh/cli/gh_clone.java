package no.ion.gh.cli;

import no.ion.gh.git.GitInvocation;
import no.ion.gh.io.UnixPath;
import no.ion.gh.util.GHException;

import java.util.Optional;

public class gh_clone implements Subcommand {
    private final Context context;

    public gh_clone(Context context) {
        this.context = context;
    }

    @Override
    public void main(ArgumentIterator iterator) {
        options_loop:
        for (; iterator.isValid(); iterator.increment()) {
            String option = iterator.get();
            switch (option) {
                case "-h", "--help" -> throw GHException.ofHelp(
                        """
                        Usage: gh [GOPTION...] clone URL [DEST]
                        Clone git repository to DEST.
                        
                        DEST must not exist, if present.
                        """);
                case "-v", "--verbose" -> context.verbosity().setVerbose(true);
                default -> {
                    if (option.startsWith("-"))
                        throw GHException.ofInvalidUsage_unknownOption(option);
                    break options_loop;
                }
            }
        }

        if (iterator.isDone()) throw GHException.ofInvalidUsage_missingArgument("URL");

        String url = iterator.get();

        Optional<String> dest = Optional.empty();
        iterator.increment();
        if (iterator.isValid()) {
            dest = Optional.of(iterator.get());
            UnixPath destDirectory = UnixPath.of(dest.get());

            // Either DEST must not exist, or it must be an empty directory.
            if (destDirectory.exists()) {
                if (destDirectory.isDirectory()) {
                    if (!destDirectory.isEmptyDirectory())
                        throw GHException.ofUserError("The DEST directory " + destDirectory + " is not empty");
                } else {
                    throw GHException.ofUserError("The DEST path is not a directory: " + destDirectory);
                }
            }

            iterator.increment();
            if (iterator.isValid())
                throw GHException.ofInvalidUsage_extraneousArguments(iterator);
        }

        GitInvocation invocation = context.git().newInvocation();
        invocation.addArguments("clone", url);
        dest.ifPresent(invocation::addArguments);
        String stdout = invocation.executeMutating();
    }
}
