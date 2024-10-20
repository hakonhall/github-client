package no.ion.gh.model;

import no.ion.gh.util.GHException;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;

public class Server {
    public static final String CONFIG_API_KEY = "api";
    public static final String CONFIG_USERNAME_KEY = "username";
    public static final String CONFIG_TOKEN_KEY = "token";

    private final ServerName name;
    private final Optional<URI> apiUrl;
    private final Optional<UserName> username;
    private final Optional<String> bearerToken;

    public static Server of(ServerName name, URI apiUrl, UserName username, String bearerToken) {
        return new Server(Objects.requireNonNull(name, "name cannot be null"),
                          Optional.ofNullable(apiUrl),
                          Optional.ofNullable(username),
                          Optional.ofNullable(bearerToken));
    }

    private Server(ServerName name, Optional<URI> apiUrl, Optional<UserName> username, Optional<String> bearerToken) {
        this.name = name;
        this.apiUrl = apiUrl;
        this.username = username;
        this.bearerToken = bearerToken;
    }

    public ServerName name() { return name; }

    public URI graphQLUrl() { return apiUrl().resolve("/graphql"); }
    public URI apiUrl() { return apiUrl.orElseThrow(() -> GHException.ofMissingServerSetting(name, CONFIG_API_KEY)); }
    public Optional<URI> apiUrlIfSpecified() { return apiUrl; }

    public UserName username() { return username.orElseThrow(() -> GHException.ofMissingServerSetting(name, CONFIG_USERNAME_KEY)); }
    public Optional<UserName> usernameIfSpecified() { return username; }

    public String bearerToken() { return bearerToken.orElseThrow(() -> GHException.ofMissingServerSetting(name, CONFIG_TOKEN_KEY)); }
    public Optional<String> bearerTokenIfSpecified() { return bearerToken; }
}
