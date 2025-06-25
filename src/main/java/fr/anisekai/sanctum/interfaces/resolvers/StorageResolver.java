package fr.anisekai.sanctum.interfaces.resolvers;

import fr.anisekai.sanctum.interfaces.ScopedEntity;

import java.nio.file.Path;

/**
 * This interface provides methods to retrieve {@link Path} instances representing directories and files, based on either simple
 * names or scoped entities. Resolved {@link Path} are not guaranteed to exist.
 */
public interface StorageResolver {

    /**
     * Retrieve the root {@link Path} of this {@link StorageResolver}.
     *
     * @return A {@link Path}.
     */
    Path directory();

    /**
     * Try to resolve a {@link Path} pointing to a directory with the provided name.
     *
     * @param name
     *         The directory name to resolve.
     *
     * @return The {@link Path} pointing to the directory.
     */
    Path directory(String name);

    /**
     * Try to resolve a {@link Path} pointing to a file with the provided name.
     *
     * @param filename
     *         The file name to resolve.
     *
     * @return The {@link Path} pointing to the file.
     */
    Path file(String filename);

    /**
     * Try to resolve a {@link Path} pointing to the directory of the provided {@link ScopedEntity}.
     *
     * @param entity
     *         The {@link ScopedEntity} for which the directory should be resolved.
     *
     * @return The {@link Path} pointing to the directory.
     */
    Path directory(ScopedEntity entity);

    /**
     * Try to resolve a {@link Path} pointing to the file of the provided {@link ScopedEntity}.
     *
     * @param entity
     *         The {@link ScopedEntity} for which the file should be resolved.
     *
     * @return The {@link Path} pointing to the file.
     */
    Path file(ScopedEntity entity);

    /**
     * Try to resolve a {@link Path} pointing to a file with the provided name within the directory of the provided
     * {@link ScopedEntity}.
     *
     * @param entity
     *         The {@link ScopedEntity} for which the directory should be resolved.
     * @param filename
     *         The file name to resolve.
     *
     * @return The {@link Path} pointing to the file.
     */
    Path file(ScopedEntity entity, String filename);

}
