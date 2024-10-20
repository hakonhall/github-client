package no.ion.gh.github;

import no.ion.gh.cli.Context;
import no.ion.gh.json.Json;
import no.ion.gh.model.OrgName;
import no.ion.gh.model.RepoName;
import no.ion.gh.model.Server;
import no.ion.gh.model.UserName;
import no.ion.gh.util.GHException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static no.ion.gh.util.Exceptions.uncheckIO;

public class GraphQLClient implements AutoCloseable {
    private final Context context;
    private final Server server;
    private final HttpClient client;

    public GraphQLClient(Context context, Server server) {
        this.context = context;
        this.server = server;
        this.client = HttpClient.newHttpClient();
    }

    public List<OrgInfo> listOrgsFor(UserName userName) {
        return getAllPages((cursor, entries) -> listOrgsFor(userName, cursor, entries));
    }

    private Json listOrgsFor(UserName userName, String cursor, List<OrgInfo> orgInfos) {
        String query = """
                       query {
                         user(login: "%s") {
                           organizations(first: 100%s) {
                             pageInfo {
                               hasNextPage
                               endCursor
                             }
                             nodes {
                               login
                             }
                           }
                         }
                       }
                       """.formatted(userName.value(), afterPart(cursor));

        Json json = post(query);

        Json organizations = json.to("data.user.organizations");

        organizations.of("nodes").forEachArrayElement(node -> {
            OrgName orgName = new OrgName(node.of("login").asString());
            OrgInfo orgInfo = new OrgInfo(server, userName, orgName);
            orgInfos.add(orgInfo);
        });

        return organizations.of("pageInfo");
    }

    public List<UserRepoInfo> listReposFor(UserName user) {
        return getAllPages((cursor, list) -> listReposFor(user, cursor, list));
    }

    private Json listReposFor(UserName userName, String cursor, List<UserRepoInfo> repoInfos) {
        String query = """
                       query {
                         user(login: "%s") {
                           repositories(first: 100%s) {
                             pageInfo {
                               hasNextPage
                               endCursor
                             }
                             nodes {
                               name
                               updatedAt
                               pushedAt
                               defaultBranchRef {
                                 name
                               }
                             }
                           }
                         }
                       }
                       """.formatted(userName.value(), afterPart(cursor));

        Json json = post(query);

        Json repositories = json.to("data.user.repositories");

        repositories.of("nodes").forEachArrayElement(repoJson -> {
            var repoInfo = new UserRepoInfo(server.name(),
                                            userName,
                                            new RepoName(repoJson.of("name").asString()),
                                            Instant.parse(repoJson.of("updatedAt").asString()),
                                            Instant.parse(repoJson.of("pushedAt").asString()),
                                            repoJson.of("defaultBranchRef").of("name").asString());
            repoInfos.add(repoInfo);
        });

        return repositories.of("pageInfo");
    }

    public List<RepoInfo> listReposIn(OrgName organization) {
        return getAllPages((cursor, list) -> listReposIn(organization, cursor, list));
    }

    private Json listReposIn(OrgName organization, String cursor, List<RepoInfo> repoInfos) {
        String query = """
                       query {
                         organization(login: "%s") {
                           repositories(first: 100%s) {
                             pageInfo {
                               hasNextPage
                               endCursor
                             }
                             nodes {
                               name
                               updatedAt
                               pushedAt
                               defaultBranchRef {
                                 name
                               }
                             }
                           }
                         }
                       }
                       """.formatted(organization, afterPart(cursor));

        Json json = post(query);

        Json repositories = json.to("data.organization.repositories");

        repositories.of("nodes").forEachArrayElement(repoJson -> {
            var repoInfo = new RepoInfo(server.name(),
                                        organization,
                                        new RepoName(repoJson.of("name").asString()),
                                        Instant.parse(repoJson.of("updatedAt").asString()),
                                        Instant.parse(repoJson.of("pushedAt").asString()),
                                        repoJson.of("defaultBranchRef").of("name").asString());
            repoInfos.add(repoInfo);
        });

        return repositories.of("pageInfo");
    }

    @FunctionalInterface
    private interface PagedRequest<T> {
        /** @return PageInfo Json. */
        Json getPage(String cursor, List<T> entries);
    }

    private <T> List<T> getAllPages(PagedRequest<T> pagedRequest) {
        var repoInfos = new ArrayList<T>();
        String cursor = null;
        do {
            Json pageInfoJson = pagedRequest.getPage(cursor, repoInfos);
            if (!pageInfoJson.of("hasNextPage").asBoolean()) return repoInfos;
            cursor = pageInfoJson.of("endCursor").asString();
        } while (true);
    }

    private static PageInfo pageInfoFromJson(Json pageInfo) {
        return new PageInfo(pageInfo.of("hasNextPage").asBoolean(), pageInfo.of("endCursor").asString());
    }

    private static String afterPart(String afterCursor) {
        return afterCursor == null ? "" : ", after: \"" + afterCursor + "\"";
    }

    private Json post(String query) {
        if (context.verbosity().logRequest())
            context.out().print(query);

        String requestJson = toJson(query);

        HttpRequest.Builder builder = HttpRequest.newBuilder();
        server.bearerTokenIfSpecified().ifPresent(token -> builder.header("Authorization", "bearer " + token));
        URI url = server.graphQLUrl();
        HttpRequest request = builder.header("Content-Type", "application/json")
                                     .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                                     .timeout(Duration.ofSeconds(60))
                                     .uri(url)
                                     .build();

        final HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        int statusCode = response.statusCode();
        String body = response.body();

        // GraphQL failure returns 200 and e.g.
        //     {"data":{"organization":null},"errors":[{"type":"NOT_FOUND","path":["organization"],"locations":[{"line":2,"column":3}],"message":"Could not resolve to an Organization with the login of 'hakonhall'."}]}
        Optional<Json> json = Json.tryFrom(body);
        if (statusCode != 200 || json.isEmpty() || json.get().of("errors").isValid()) {
            throw GHException.ofHttpFailure(request.method(), url, statusCode, body);
        }

        if (context.verbosity().logResponse()) {
            context.out().printf("status code %d\n%s\n", statusCode, body);
        }

        return json.get();
    }

    private static String toJson(String query) {
        return """
               {"query":"%s"}
               """.formatted(no.ion.gh.json.Json.escapeString(query));
    }

    @Override
    public void close() { uncheckIO(client::close); }
}
