package fr.anisekai.sanctum;

import fr.anisekai.sanctum.exceptions.scope.ScopeDefinitionException;
import fr.anisekai.sanctum.interfaces.FileStore;
import fr.anisekai.sanctum.interfaces.ScopedEntity;
import fr.anisekai.sanctum.interfaces.isolation.IsolationSessionDescriptor;
import org.jetbrains.annotations.NotNull;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;

/**
 * Represent an access scope within a {@link FileStore} granted for a {@link IsolationSessionDescriptor}.
 *
 * @param store
 *         The {@link FileStore} targeted by this {@link AccessScope}.
 * @param claim
 *         The root file or directory name targeted by this {@link AccessScope}.
 */
public record AccessScope(FileStore store, String claim) {

    /**
     * Provide default sanity checks when creating an {@link AccessScope}.
     *
     * @param store
     *         The {@link FileStore} targeted by this {@link AccessScope}.
     * @param claim
     *         The root file or directory name targeted by this {@link AccessScope}.
     */
    public AccessScope {

        if (store == null) {
            throw new ScopeDefinitionException("The file store cannot be null");
        }

        if (claim == null) {
            throw new ScopeDefinitionException("The claim cannot be null");
        }

        if (!store.type().isScoped()) {
            throw new ScopeDefinitionException("Cannot create an access scope targeting a non-scoped store.");
        }

        if (claim.isBlank()) {
            throw new ScopeDefinitionException("The scope name cannot be blank");
        }

        try {
            Path claimPath = Path.of(claim);
            if (claimPath.isAbsolute() || claimPath.getNameCount() != 1 || claim.equals(".") || claim.equals("..") ||
                    claim.contains("/") || claim.contains("\\")) {
                throw new ScopeDefinitionException("The scope name must identify a direct child of the store");
            }
        } catch (InvalidPathException e) {
            throw new ScopeDefinitionException("The scope name is not a valid file name: " + e.getMessage());
        }
    }

    /**
     * Create an access scope from a legacy scoped entity.
     *
     * @param store
     *         The {@link FileStore} targeted by this {@link AccessScope}.
     * @param claim
     *         The {@link ScopedEntity} whose scoped name identifies the target.
     *
     * @deprecated Pass the scoped name directly using {@link #AccessScope(FileStore, String)}.
     */
    @Deprecated(forRemoval = true)
    public AccessScope(FileStore store, ScopedEntity claim) {

        this(store, claim == null ? null : claim.getScopedName());
    }

    @Override
    public @NotNull String toString() {

        return String.format(
                "AccessScope{store='%s', claim='%s'}",
                this.store().name(),
                this.claim()
        );
    }

}
