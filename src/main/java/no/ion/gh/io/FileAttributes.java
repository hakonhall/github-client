package no.ion.gh.io;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

public class FileAttributes {
    private static final int S_IFMT =  0170000;
    private static final int S_IFREG = 0100000;
    private static final int S_IFDIR = 0040000;

    private final Path path;
    private final Map<String, Object> attributes;

    static Optional<FileAttributes> readIfExists(Path path, LinkOption...linkOptions) {
        final Map<String, Object> attributes;
        try {
            attributes = Files.readAttributes(path, "unix:*", linkOptions);
        } catch (NoSuchFileException e) {
            return Optional.empty();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        if (attributes == null)
            throw new IllegalStateException("failed to retrieve UNIX file attributes: not on a UNIX machine!?");

        return Optional.of(new FileAttributes(path, attributes));
    }

    public FileAttributes(Path path, Map<String, Object> attributes) {
        this.path = path;
        this.attributes = attributes;
    }

    public boolean isDirectory()   { return (mode() & S_IFMT) == S_IFDIR; }
    public boolean isRegularFile() { return (mode() & S_IFMT) == S_IFREG; }

    private int mode() {
        Object modeObject = attributes.get("mode");
        if (modeObject instanceof Integer mode)
            return mode;
        throw new IllegalStateException("failed to read the UNIX mode from " + path);
    }
}
