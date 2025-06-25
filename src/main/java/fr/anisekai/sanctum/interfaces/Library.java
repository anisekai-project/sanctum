package fr.anisekai.sanctum.interfaces;

import fr.anisekai.sanctum.AccessScope;
import fr.anisekai.sanctum.enums.StorePolicy;
import fr.anisekai.sanctum.interfaces.isolation.IsolationSession;
import fr.anisekai.sanctum.interfaces.isolation.IsolationSessionAware;

import java.util.Collections;
import java.util.Set;

/**
 * Interface representing the main storage access point.
 */
public interface Library extends IsolationSessionAware, AutoCloseable {

    /**
     * Register the provided {@link FileStore} in this {@link Library} using the provided {@link StorePolicy}.
     *
     * @param store
     *         The {@link FileStore} to register.
     * @param policy
     *         The {@link StorePolicy} for the {@link FileStore}.
     */
    void registerStore(FileStore store, StorePolicy policy);

    /**
     * Check if this {@link Library} can use the provided {@link FileStore}.
     *
     * @param store
     *         The {@link FileStore} to check.
     *
     * @return True if the {@link FileStore} can be used with the current {@link Library}, false otherwise.
     */
    boolean hasStore(FileStore store);

    /**
     * Create an {@link IsolationSession} without any {@link AccessScope}.
     *
     * @return The newly created {@link IsolationSession}.
     */
    default IsolationSession createIsolation() {

        return this.createIsolation(Collections.emptySet());
    }

    /**
     * Create an {@link IsolationSession} with the provided {@link AccessScope} array.
     *
     * @param scopes
     *         The {@link AccessScope} to claim
     *
     * @return The newly created {@link IsolationSession}.
     */
    default IsolationSession createIsolation(AccessScope... scopes) {

        return this.createIsolation(Set.of(scopes));
    }

    /**
     * Create an {@link IsolationSession} with the provided {@link AccessScope} set.
     *
     * @param scopes
     *         The {@link AccessScope} to claim
     *
     * @return The newly created {@link IsolationSession}.
     */
    IsolationSession createIsolation(Set<AccessScope> scopes);

}
