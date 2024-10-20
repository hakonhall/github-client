package no.ion.gh.config;

import no.ion.gh.io.UnixPath;
import no.ion.gh.model.Server;
import no.ion.gh.model.ServerName;
import no.ion.gh.model.UserName;
import no.ion.gh.text.Char;
import no.ion.gh.text.TextCursor;
import no.ion.gh.util.GHException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

public class ConfigParser {
    private final UnixPath path;
    private final String config;
    private final TextCursor cursor;

    private final List<Server> servers = new ArrayList<>();

    public ConfigParser(UnixPath path, String config) {
        this.path = path;
        this.config = config;
        this.cursor = new TextCursor(config);
    }

    private void reset() {
        cursor.resetToFirstChar();
        servers.clear();
    }

    public Config parse() {
        reset();

        for (skip(); !eof(); skip()) {
            String token = readStartToken();
            if (token.equals("[")) {
                readSection();
            } else {
                readGlobalField(token);
            }
        }

        return new Config(servers);
    }

    private void readGlobalField(String fieldName) {
        throw invalidConfig("Unknown global field: " + fieldName);
    }

    private void readSection() {
        skipSpaces();
        TextCursor nameStart = cursor.copy();
        String name = readIdentifier("section name");
        switch (name) {
            case "server" -> readServerSection();
            default -> throw invalidConfig("Invalid section name: " + name, nameStart);
        }
    }

    private void readServerSection() {
        skipSpaces();
        TextCursor serverNameStart = cursor.copy();
        String serverName = readIdentifier("server name");
        verifyServerNameIsUnique(serverNameStart, serverName);
        skipSpaces();
        readSectionHeaderClose();
        Server server = readServerBody(serverName);
        servers.add(server);
    }

    private void verifyServerNameIsUnique(TextCursor serverNameStart, String serverName) {
        if (servers.stream().anyMatch(server -> server.name().toString().equals(serverName)))
            throw invalidConfig("Server " + serverName + " has already been defined", serverNameStart);
    }

    private void readSectionHeaderClose() {
        if (eof() || !cursor.skip(']')) throw invalidConfig("Missing section header close ']'");
        skipToNextLine();
    }

    /** @return '[', or a Java identifier. */
    private String readStartToken() {
        assertNotEof("Expected start token");
        Char c = cursor.getChar();
        if (c.is('[')) {
            cursor.advanceOneChar();
            return "[";
        }
        if (c.is(']')) throw invalidConfig("Expected start token");

        return readIdentifier("field name");
    }

    private String readIdentifier(String idDescription) {
        assertNotEof("Expected " + idDescription);
        TextCursor start = cursor.copy();
        if (!cursor.getChar().isJavaIdentifierStart())
            throw invalidConfig("Expected " + idDescription + " but found an invalid identifier");
        cursor.advanceOneChar();
        for (; cursor.isValid() && cursor.getChar().isJavaIdentifierPart(); cursor.advanceOneChar()) {
            // do nothing
        }
        if (!isValidEndOfIdentifier())
            throw invalidConfig("Invalid identifier", start);
        return cursor.text().substring(start.index(), cursor.index());
    }

    private boolean isValidEndOfIdentifier() {
        if (cursor.eot()) return true;
        Char c = cursor.getChar();
        if (c.isWhitespace()) return true;
        if (c.isOneOf("[]#")) return true;
        return false;
    }

    private Server readServerBody(String serverName) {
        String api = null, token = null;
        UserName userName = null;

        fields_loop:
        for (skip(); !eof(); skip()) {
            String fieldName = readStartToken();
            switch (fieldName) {
                case "[" -> {
                    // new section => close this section and return
                    break fields_loop;
                }
                case Server.CONFIG_API_KEY -> {
                    if (api != null) throw invalidConfig(fieldName + " has already been set");
                    skipSpaces();
                    api = readValue(fieldName);
                }
                case Server.CONFIG_USERNAME_KEY -> {
                    if (userName != null) throw invalidConfig(fieldName + " has already been set");
                    skipSpaces();
                    userName = new UserName(readValue(fieldName));
                }
                case Server.CONFIG_TOKEN_KEY -> {
                    if (token != null) throw invalidConfig(fieldName + " has already been set");
                    skipSpaces();
                    token = readValue(fieldName);
                }
                default -> throw invalidConfig("Invalid server field: " + fieldName);
            }
            skipToNextLine();
        }

        var missing = new HashSet<String>();
        // if or when some fields of the server section become required, fill `missing` here.
        if (!missing.isEmpty()) {
            String missingList = missing.stream().sorted().collect(Collectors.joining(", "));
            throw invalidConfig("Missing fields for server " + serverName + ": " + missingList);
        }

        URI apiUrl = null;
        if (api != null) {
            try {
                apiUrl = new URI(api);
            } catch (URISyntaxException e) {
                throw invalidConfig("Invalid " + Server.CONFIG_API_KEY + " URL: " + e.getMessage());
            }
        }

        return Server.of(new ServerName(serverName), apiUrl, userName, token);
    }

    private String readValue(String name) {
        assertNotEof("Missing value of " + name);
        if (cursor.is('"')) return readDoubleQuotedStringValue();
        int startIndex = cursor.index();
        skipToWhitespace();
        return cursor.text().substring(startIndex, cursor.index());
    }

    private String readDoubleQuotedStringValue() {
        if (cursor.eot() || !cursor.is('"'))
            throw invalidConfig("Invalid start of double-quoted string");

        cursor.advanceOneChar();

        var builder = new StringBuilder();

        string_body_loop:
        for (; cursor.isValid(); cursor.advanceOneChar()) {
            switch (cursor.getRawChar()) {
                case '"' -> { break string_body_loop; }
                case '\\' -> {
                    cursor.advanceOneChar();
                    assertNotEof("Single backslash at EOF");
                    switch (cursor.getRawChar()) {
                        case '"', '\\' -> builder.append(cursor.getRawChar());
                        default -> throw invalidConfig("Invalid escape sequence");
                    }
                }
                default -> builder.append(cursor.getRawChar());
            }
        }

        if (!isSeparator())
            throw invalidConfig("Invalid end of double-quoted string, or missing escape of double quote");

        return builder.toString();
    }

    private void assertNotEof(String problem) {
        if(cursor.eot())
            throw invalidConfig("Unexpected early end of file: " + problem);
    }

    private boolean eof() { return cursor.eot(); }

    private boolean isSeparator() {
        if (cursor.eot()) return true;
        Char c = cursor.getChar();
        if (c.isWhitespace()) return true;
        if (c.isStartOfLineComment()) return true;
        return false;
    }

    private void skipToNextLine() {
        for (; cursor.isValid() && !cursor.isNewline(); cursor.advanceOneChar()) {
            Char c = cursor.getChar();
            if (c.isWhitespace()) continue;
            if (c.isStartOfLineComment()) {
                cursor.advanceToNewline();
                break;
            }
            throw invalidConfig("Expected newline");
        }
    }

    private void skipToWhitespace() {
        for (; cursor.isValid() && !cursor.getChar().isWhitespace(); cursor.advanceOneChar()) {
            // do nothing
        }
    }

    private void skipSpaces() {
        for (; !eof() && !cursor.isNewline() && cursor.getChar().isWhitespace(); cursor.advanceOneChar()) {
            // do nothing
        }
    }

    private void skip() {
        for (; cursor.isValid(); cursor.advanceOneChar()) {
            Char c = cursor.getChar();
            if (c.isWhitespace()) continue;
            if (c.isStartOfLineComment()) {
                cursor.advanceToNewline();
                continue;
            }
            return;
        }
    }

    private GHException invalidConfig(String problem) {
        return GHException.ofInvalidConfig(path, cursor, problem);
    }

    private GHException invalidConfig(String problem, TextCursor actualLocation) {
        return GHException.ofInvalidConfig(path, actualLocation, problem);
    }
}
