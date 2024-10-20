package no.ion.gh.cli;

import no.ion.gh.github.GitHub;
import no.ion.gh.github.RepoInfo;
import no.ion.gh.github.UserRepoInfo;
import no.ion.gh.model.OrgName;
import no.ion.gh.model.Server;
import no.ion.gh.model.ServerName;
import no.ion.gh.model.UserName;
import no.ion.gh.util.GHException;

import java.util.List;

public class gh_listrepos implements Subcommand {
    private final Context context;

    public gh_listrepos(Context context) {
        this.context = context;
    }

    @Override
    public void main(ArgumentIterator iterator) {
        boolean user = false;
        options_loop:
        for (; iterator.isValid(); iterator.increment()) {
            String option = iterator.get();
            switch (option) {
                case "-h", "--help" -> throw GHException.ofHelp(
                        """
                        Usage: gh [GOPTION...] list-repos [-u] SERVER ORG
                        List repositories owned by ORG in SERVER defined in config.

                        Options:
                          -u, --user   Fetch user repositories for the user named ORG
                        """);
                case "-u", "--user" -> user = true;
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
        if (iterator.isDone()) throw GHException.ofInvalidUsage_missingArgument("ORG");

        UserName userName = null;
        OrgName orgName = null;
        if (user) {
            userName = new UserName(iterator.get());
        } else {
            orgName = new OrgName(iterator.get());
        }

        iterator.increment();
        if (iterator.isValid()) throw GHException.ofInvalidUsage_extraneousArguments(iterator);

        Server server = context.resolveConfig().getServer(serverName);

        try (GitHub gitHub = context.gitHubClientTo(server)) {
            if (user) {
                List<UserRepoInfo> repoInfos = gitHub.listReposFor(userName);
                for (UserRepoInfo repoInfo : repoInfos) {
                    context.out().println("%s:%s/%s#%s:\t%s\t%s".formatted(
                            repoInfo.server().value(),
                            repoInfo.userName().value(),
                            repoInfo.repo().name(),
                            repoInfo.defaultBranch(),
                            repoInfo.updatedAt(),
                            repoInfo.pushedAt()));
                }
            } else {
                List<RepoInfo> repoInfos = gitHub.listReposIn(orgName);
                for (RepoInfo repoInfo : repoInfos) {
                    context.out().println("%s:%s/%s#%s:\t%s\t%s".formatted(
                            repoInfo.server().value(),
                            repoInfo.org().name(),
                            repoInfo.repo().name(),
                            repoInfo.defaultBranch(),
                            repoInfo.updatedAt(),
                            repoInfo.pushedAt()));
                }
            }
        }
    }
}
