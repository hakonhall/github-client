package no.ion.gh.config;

import no.ion.gh.io.UnixPath;
import no.ion.gh.model.Server;
import no.ion.gh.model.ServerName;
import no.ion.gh.util.GHException;

import java.util.List;
import java.util.Objects;

public record Config(List<Server> servers) {
    public Config(List<Server> servers) {
        this.servers = List.copyOf(Objects.requireNonNull(servers));
    }

    public Server getServer(ServerName serverName) {
        return servers.stream()
                      .filter(server -> server.name().equals(serverName)).findFirst()
                      .orElseThrow(() -> GHException.ofInvalidUsage_badArgument(serverName.value(), "Config defines no such server"));
    }

    public static Config read(UnixPath configPath) {
        String configString = configPath.readUtf8FileIfExists()
                                        .orElseThrow(() -> GHException.ofUserError("Config file not found: " + configPath));
        ConfigParser parser = new ConfigParser(configPath, configString);
        return parser.parse();
    }
}
