package fr.anisekai.sanctum.interfaces.isolation;

import fr.anisekai.sanctum.AccessScope;
import fr.anisekai.sanctum.exceptions.context.ContextCommitException;
import fr.anisekai.sanctum.exceptions.context.ContextDiscardException;
import fr.anisekai.sanctum.exceptions.scope.ScopeGrantException;
import fr.anisekai.sanctum.interfaces.FileStore;
import fr.anisekai.sanctum.interfaces.Library;
import fr.anisekai.sanctum.interfaces.ScopedEntity;
import fr.anisekai.sanctum.interfaces.StorageAware;
import fr.anisekai.sanctum.interfaces.resolvers.StorageResolver;

import java.nio.file.Path;
import java.util.UUID;

/**
 * Interface representing the access to an isolated space that mirrors {@link FileStore} from the main {@link Library}.
 */
public interface IsolationSession extends StorageAware, ScopedEntity, AutoCloseable {

    /**
     * Retrieve the uuid of this {@link IsolationSession}.
     *
     * @return The uuid
     */
    UUID uuid();

    /**
     * Retrieve the {@link IsolationSessionAware} instance that created this {@link IsolationSession}.
     *
     * @return An {@link IsolationSessionAware}.
     */
    IsolationSessionAware owner();

    /**
     * Retrieve a {@link Path} pointing to a temporary file within this {@link IsolationSession}.
     *
     * @param extension
     *         The file extension of the temporary file.
     *
     * @return A {@link Path} pointing to a temporary file
     */
    default Path requestTemporaryFile(String extension) {

        return this.owner().requestTemporaryFile(this, extension);
    }

    /**
     * Request one or more {@link AccessScope} to claim for this {@link IsolationSession}.
     *
     * @param scopes
     *         Array of {@link AccessScope} to claim.
     *
     * @throws ScopeGrantException
     *         If one of the {@link AccessScope} could not be granted. If this exception is thrown, no {@link AccessScope} has
     *         been granted, even the valid ones.
     */
    default void requestScope(AccessScope... scopes) {

        this.owner().requestScope(this, scopes);
    }

    /**
     * Commit this {@link IsolationSession} to the main storage.
     *
     * @throws ContextCommitException
     *         If an error occurs while committing this {@link IsolationSession}
     */
    default void commit() {

        this.owner().commit(this);
    }

    /**
     * Discard this {@link IsolationSession}.
     *
     * @throws ContextDiscardException
     *         If an error occurs while discarding this {@link IsolationSession}
     */
    @Override
    default void close() {

        this.owner().discard(this);
    }

    /**
     * Retrieve a {@link StorageResolver} associated to this {@link IsolationSession}.
     *
     * @param store
     *         The {@link FileStore} to which the {@link StorageResolver} should be associated.
     *
     * @return A {@link StorageResolver}.
     */
    @Override
    default StorageResolver getResolver(FileStore store) {

        return this.owner().getResolver(this, store);
    }

}
