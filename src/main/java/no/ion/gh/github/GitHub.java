package no.ion.gh.github;

import no.ion.gh.cli.Context;
import no.ion.gh.model.OrgName;
import no.ion.gh.model.Server;
import no.ion.gh.model.UserName;

import java.util.List;

public class GitHub implements AutoCloseable {
    private final Context context;
    private final Server server;
    private final GraphQLClient graphQlClient;

    public GitHub(Context context, Server server) {
        this.context = context;
        this.server = server;
        this.graphQlClient = new GraphQLClient(context, server);
    }

    public List<OrgInfo> listOrgsFor(UserName userName) {
        return graphQlClient.listOrgsFor(userName);
    }

    public List<UserRepoInfo> listReposFor(UserName user) {
        return graphQlClient.listReposFor(user);
    }

    public List<RepoInfo> listReposIn(OrgName organization) {
        return graphQlClient.listReposIn(organization);
    }

    @Override public void close() { graphQlClient.close(); }
}
