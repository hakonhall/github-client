package no.ion.gh.github;

import no.ion.gh.model.OrgName;
import no.ion.gh.model.Server;
import no.ion.gh.model.UserName;

public record OrgInfo(Server server,
                      UserName userName,
                      OrgName orgName) {
}
