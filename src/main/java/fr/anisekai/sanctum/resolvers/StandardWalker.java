package fr.anisekai.sanctum.resolvers;

import fr.anisekai.sanctum.SanctumUtils;
import fr.anisekai.sanctum.exceptions.ResolveOutOfBoundException;
import fr.anisekai.sanctum.exceptions.StorageException;
import fr.anisekai.sanctum.interfaces.resolvers.StorageWalker;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Implementation of {@link StorageWalker} allowing to navigate ("walk") through a directory tree.
 *
 * @param root
 *         The root {@link Path} into which the {@link StorageWalker} will resolve content.
 */
public record StandardWalker(Path root) implements StorageWalker {

    /**
     * Provide default sanity checks when creating a {@link StandardWalker}.
     *
     * @param root
     *         The root {@link Path} into which the {@link StorageWalker} will resolve content.
     */
    public StandardWalker {

        if (Files.exists(root) && !Files.isDirectory(root)) {
            throw new StorageException("Directory was expected (Path: " + root + ")");
        }
        SanctumUtils.Action.wrap(() -> Files.createDirectories(root), StorageException::new);
    }

    static Path walk(Path from, String toward) {

        Path walked = from.resolve(toward).toAbsolutePath().normalize();

        if (!walked.startsWith(from)) {
            throw new ResolveOutOfBoundException(String.format(
                    "Cannot resolve backward (Resolved '%s' from '%s')",
                    walked,
                    from
            ));
        }

        return walked;
    }

    @Override
    public StorageWalker walk(String into) {

        return new StandardWalker(this.directory(into));
    }

    @Override
    public Path directory(String name) {

        Path walked = walk(this.root(), name);

        if (Files.isDirectory(walked) || Files.notExists(walked)) {
            return walked;
        }

        if (Files.isRegularFile(walked)) {
            throw new StorageException("Directory was expected, got file instead (Path: " + walked + ")");
        }

        throw new StorageException("Directory was expected (Path: " + walked + ")");
    }

    @Override
    public Path file(String filename) {

        Path walked = walk(this.root(), filename);

        // Non-existant files are considered valid as they will be created on write
        if (Files.isRegularFile(walked) || Files.notExists(walked)) {
            return walked;
        }

        if (Files.isDirectory(walked)) {
            throw new StorageException("File was expected, got directory instead (Path: " + walked + ")");
        }

        throw new StorageException("File was expected (Path: " + walked + ")");
    }

}
