package fr.anisekai.sanctum.interfaces.resolvers;

import fr.anisekai.sanctum.exceptions.ResolveOutOfBoundException;
import fr.anisekai.sanctum.exceptions.StorageException;

import java.nio.file.Path;

/**
 * Provides a simple API to navigate ("walk") through a directory tree, resolving directories and files relative to a root path.
 * Implementations ensure safe resolution of subpaths and enforce directory/file distinctions to avoid unexpected filesystem
 * errors.
 */
public interface StorageWalker {

    /**
     * Returns a new {@link StorageWalker} rooted in the subdirectory of the provided name, relative to the current root.
     * Implementations must ensure that the resolved path is a directory (existing or created) and prevent traversal outside the
     * original root.
     *
     * @param into
     *         The subdirectory name to walk into.
     *
     * @return A new {@link StorageWalker} rooted at the specified subdirectory.
     *
     * @throws StorageException
     *         If the resolved path is not a directory.
     * @throws ResolveOutOfBoundException
     *         If resolution attempts to move outside the root.
     */
    StorageWalker walk(String into);

    /**
     * Resolves and returns a directory path by name relative to the current root. If the directory does not exist,
     * implementations may create it.
     *
     * @param name
     *         The name of the directory to resolve.
     *
     * @return The resolved directory {@link Path}.
     *
     * @throws StorageException
     *         If the path exists but is not a directory.
     * @throws ResolveOutOfBoundException
     *         If resolution attempts to move outside the root.
     */
    Path directory(String name);

    /**
     * Resolves and returns a file path by filename relative to the current root. Non-existent files are considered valid, as they
     * may be created later.
     *
     * @param filename
     *         The filename to resolve.
     *
     * @return The resolved file {@link Path}.
     *
     * @throws StorageException
     *         If the path exists but is not a file.
     * @throws ResolveOutOfBoundException
     *         If resolution attempts to move outside the root.
     */
    Path file(String filename);

}
