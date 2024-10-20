package no.ion.gh.io;

import no.ion.gh.util.GHException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public record UnixPath(Path path) {

    private static volatile List<UnixPath> PATH = null;

    /** Returns an unmodifiable list of the non-empty paths of the :-separated PATH environment variable. */
    private static List<UnixPath> getPATH() {
        if (PATH == null) {
            String path = System.getenv("PATH");
            if (path == null) {
                PATH = List.of(UnixPath.of("/bin"), UnixPath.of("/usr/bin"));
            } else {
                String[] parts = path.split(":", -1);
                PATH = Stream.of(parts)
                             .filter(part -> !part.isEmpty())
                             .map(UnixPath::of)
                             .toList();
            }
        }

        return PATH;
    }

    public static UnixPath of(String path) { return of(FileSystems.getDefault().getPath(path)); }
    public static UnixPath of(Path path) { return new UnixPath(path); }

    public UnixPath {
        Objects.requireNonNull(path, "path must be non-null");
    }

    public UnixPath resolve(UnixPath other) { return of(other.path); }
    public UnixPath resolve(Path other) { return of(path.resolve(other)); }
    public UnixPath resolve(String other) { return of(path.resolve(other)); }

    public boolean contains(char c) { return path.toString().indexOf(c) != -1; }

    public Optional<FileAttributes> readFileAttributesIfExists(LinkOption... linkOptions) {
        return FileAttributes.readIfExists(path, linkOptions);
    }

    public boolean exists() { return readFileAttributesIfExists().isPresent(); }

    public boolean isDirectory() {
        return readFileAttributesIfExists().map(FileAttributes::isDirectory).orElse(false);
    }

    public boolean isExecutable() { return Files.isExecutable(path); }

    public boolean isEmptyDirectory() {
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(path)) {
            return !directoryStream.iterator().hasNext();
        } catch (NotDirectoryException | NoSuchFileException e) {
            return false;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Whether this path is an executable file.  If this path does not contain any '/', the file is looked for
     * in the directories given by the :-separated PATH environment variable.  This matches the algorithm used by
     * {@link ProcessBuilder#start()} to look for the program in command[0].
     */
    public boolean isProgram() {
        // The following assumes the system property jdk.lang.Process.launchMechanism is left at its
        // default, in Java >= 13, meaning posix_spawn mode.
        //
        // See JDK_execvpe() in src/java.base/unix/native/libjava/childproc.c.

        String pathString = path.toString();
        if (pathString.indexOf('/') != -1)
            return isExecutable();

        for (UnixPath path : getPATH()) {
            if (path.resolve(pathString).isExecutable())
                return true;
        }

        return false;
    }

    @Override
    public String toString() { return path.toString(); }

    public Optional<String> readUtf8FileIfExists() {
        try {
            return Optional.of(Files.readString(path));
        } catch (NoSuchFileException e) {
            return Optional.empty();
        } catch (IOException e) {
            throw GHException.ofUserError("Failed to read config at " + path + ": " + e.getMessage());
        }
    }
}
