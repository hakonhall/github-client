package no.ion.gh.config;

import no.ion.gh.io.UnixPath;
import no.ion.gh.model.UserName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigParserTest {

    private static Config parse(String config) {
        var parser = new ConfigParser(UnixPath.of("/dev/null"), config);
        return parser.parse();
    }

    @Test
    void empty() {
        Config config = parse("""
                              """);
        assertEquals(0, config.servers().size());
    }

    @Test
    void minimalServer() {
        Config config = parse("""
                              [server github]
                              """);
        assertEquals(1, config.servers().size());
        assertEquals("github", config.servers().get(0).name().toString());
        assertEquals(Optional.empty(), config.servers().get(0).apiUrlIfSpecified());
        assertEquals(Optional.empty(), config.servers().get(0).usernameIfSpecified());
        assertEquals(Optional.empty(), config.servers().get(0).bearerTokenIfSpecified());
    }

    @Test
    void server() {
        Config config = parse("""
                              [server github]
                              api https://api.github.com
                              username USERNAME
                              token TOKEN_SECRET
                              """);
        assertEquals(1, config.servers().size());
        assertEquals("github", config.servers().get(0).name().toString());
        assertEquals("https://api.github.com", config.servers().get(0).apiUrl().toString());
        assertEquals("https://api.github.com/graphql", config.servers().get(0).graphQLUrl().toString());
        assertEquals(new UserName("USERNAME"), config.servers().get(0).username());
        assertEquals("TOKEN_SECRET", config.servers().get(0).bearerToken());
    }
}