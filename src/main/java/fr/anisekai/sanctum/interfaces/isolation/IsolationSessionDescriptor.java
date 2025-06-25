package fr.anisekai.sanctum.interfaces.isolation;

import fr.anisekai.sanctum.AccessScope;

import java.util.Collection;

/**
 * Interface allowing to hold metadata about an {@link IsolationSessionDescriptor} that should not be exposed to the
 * {@link IsolationSession} itself.
 */
public interface IsolationSessionDescriptor {

    /**
     * Retrieve the name of this {@link IsolationSessionDescriptor}, which is the identifier and isolation directory name of the
     * {@link IsolationSession}.
     *
     * @return A name.
     */
    String name();

    /**
     * Retrieve the {@link IsolationSession} associated to this {@link IsolationSessionDescriptor}.
     *
     * @return An {@link IsolationSession}
     */
    IsolationSession context();

    /**
     * Retrieve all the {@link AccessScope} allowed to the associated {@link IsolationSession}.
     *
     * @return A {@link Collection} of {@link AccessScope}
     */
    Collection<AccessScope> scopes();

    /**
     * Check if the {@link IsolationSession} has claimed the provided {@link AccessScope}.
     *
     * @param scope
     *         The {@link AccessScope} to check.
     *
     * @return True if this {@link IsolationSessionDescriptor} contains the {@link AccessScope}, false otherwise.
     */
    boolean hasScope(AccessScope scope);

    /**
     * Grant the provided {@link AccessScope} to this {@link IsolationSessionDescriptor}.
     *
     * @param scope
     *         The {@link AccessScope} to grant.
     */
    void grantScope(AccessScope scope);

    /**
     * Check if the underlying {@link IsolationSession} has already been committed.
     *
     * @return True if committed, false otherwise.
     */
    boolean isCommitted();

    /**
     * Define if the underlying {@link IsolationSession} has already been committed.
     *
     * @param committed
     *         True if committed, false otherwise.
     */
    void setCommitted(boolean committed);

}
