package fr.anisekai.sanctum.interfaces.isolation;

import fr.anisekai.sanctum.AccessScope;
import fr.anisekai.sanctum.exceptions.context.ContextCommitException;
import fr.anisekai.sanctum.exceptions.context.ContextDiscardException;
import fr.anisekai.sanctum.exceptions.scope.ScopeGrantException;
import fr.anisekai.sanctum.interfaces.FileStore;
import fr.anisekai.sanctum.interfaces.StorageAware;
import fr.anisekai.sanctum.interfaces.resolvers.StorageResolver;

import java.nio.file.Path;
import java.util.Set;

/**
 * Represents a component capable of enforcing the isolation of an {@link IsolationSession}.
 */
public interface IsolationSessionAware extends StorageAware {

    /**
     * Retrieve a {@link StorageResolver} isolated within the {@link FileStore} directory of the provided
     * {@link IsolationSession}.
     *
     * @param context
     *         The {@link IsolationSession} defining the isolation context.
     * @param store
     *         The {@link FileStore} into which the {@link StorageResolver} should be bound.
     *
     * @return A {@link StorageResolver}.
     */
    StorageResolver getResolver(IsolationSession context, FileStore store);

    /**
     * Retrieve a {@link Path} pointing to a temporary file withing the provided {@link IsolationSession}.
     *
     * @param context
     *         The {@link IsolationSession} defining the isolation context.
     * @param extension
     *         The file extension for the temporary file.
     *
     * @return A {@link Path} pointing to a temporary file.
     */
    Path requestTemporaryFile(IsolationSession context, String extension);

    /**
     * Try to claim {@link AccessScope} for the provided {@link IsolationSession}.
     *
     * @param context
     *         The {@link IsolationSession} requesting the scopes
     * @param scopes
     *         The {@link AccessScope} to claim.
     *
     * @throws ScopeGrantException
     *         If one of the {@link AccessScope} could not be granted. If this exception is thrown, no {@link AccessScope} has
     *         been granted, even the valid ones.
     */
    default void requestScope(IsolationSession context, AccessScope... scopes) {

        this.requestScope(context, Set.of(scopes));
    }

    /**
     * Try to claim {@link AccessScope} for the provided {@link IsolationSession}.
     *
     * @param context
     *         The {@link IsolationSession} requesting the scopes
     * @param scopes
     *         The {@link AccessScope} to claim.
     *
     * @throws ScopeGrantException
     *         If one of the {@link AccessScope} could not be granted. If this exception is thrown, no {@link AccessScope} has
     *         been granted, even the valid ones.
     */
    void requestScope(IsolationSession context, Set<AccessScope> scopes);

    /**
     * Commit the provided {@link IsolationSession} into the main storage.
     *
     * @param context
     *         The {@link IsolationSession} to commit.
     *
     * @throws ContextCommitException
     *         If an error occurs while committing the {@link IsolationSession}.
     */
    void commit(IsolationSession context);

    /**
     * Discord the provided {@link IsolationSession} from the storage.
     *
     * @param context
     *         The {@link IsolationSession} to discard.
     *
     * @throws ContextDiscardException
     *         If an error occurs while discarding the {@link IsolationSession}.
     */
    void discard(IsolationSession context);

}
