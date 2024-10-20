package no.ion.gh.github;

import no.ion.gh.model.RepoName;
import no.ion.gh.model.ServerName;
import no.ion.gh.model.UserName;

import java.time.Instant;

public record UserRepoInfo(ServerName server,
                           UserName userName,
                           RepoName repo,
                           Instant updatedAt,
                           Instant pushedAt,
                           String defaultBranch) {
    public Instant mostRecentActivity() { return updatedAt.isAfter(pushedAt) ? updatedAt : pushedAt; }
}
