package no.ion.gh.model;

public record BranchId(ServerName server, OrgName org, RepoName repo, BranchName branch) {
}
