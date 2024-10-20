package no.ion.gh.cli;

import no.ion.gh.github.GitHub;
import no.ion.gh.github.OrgInfo;
import no.ion.gh.model.Server;
import no.ion.gh.model.ServerName;
import no.ion.gh.util.GHException;

import java.util.List;

public class gh_listorgs implements Subcommand {
    private final Context context;

    public gh_listorgs(Context context) {
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
                        Usage: gh [GOPTION...] list-orgs SERVER
                        List organizations owned by USER defined in config for SERVER.
                        """);
                case "-v", "--verbose" -> context.verbosity().setVerbose(true);
                default -> {
                    if (option.startsWith("-"))
                        throw GHException.ofInvalidUsage_unknownOption(option);
                    break options_loop;
                }
            }
        }

        if (iterator.isDone()) throw GHException.ofInvalidUsage_missingArgument("SERVER");
        ServerName serverName = new ServerName(iterator.get());
        iterator.increment();
        if (iterator.isValid()) throw GHException.ofInvalidUsage_extraneousArguments(iterator);

        Server server = context.resolveConfig().getServer(serverName);

        try (GitHub client = context.gitHubClientTo(server)) {
            List<OrgInfo> orgInfos = client.listOrgsFor(server.username());

            for (OrgInfo orgInfo : orgInfos) {
                context.out().println(orgInfo.orgName().name());
            }
        }
    }
}
