package no.ion.gh.github;

import no.ion.gh.model.OrgName;
import no.ion.gh.model.RepoName;
import no.ion.gh.model.ServerName;

import java.time.Instant;

/**
 *
 * @param server
 * @param org
 * @param repo
 * @param updatedAt     time of the last update to the GitHub repository?  From sampling: the time
 *                      of the last merge equals updatedAt, while pushedAt can be much later, but
 *                      the reason is unknown.
 *                      Update: Checked a personal repo, and there pushedAt matched last merge,
 *                      while updatedAt was older.
 * @param pushedAt      time of the last push to any of the branches of the GitHub repository?
 * @param defaultBranch
 */
public record RepoInfo(ServerName server,
                       OrgName org,
                       RepoName repo,
                       Instant updatedAt,
                       Instant pushedAt,
                       String defaultBranch) {
    public Instant mostRecentActivity() { return updatedAt.isAfter(pushedAt) ? updatedAt : pushedAt; }
}
