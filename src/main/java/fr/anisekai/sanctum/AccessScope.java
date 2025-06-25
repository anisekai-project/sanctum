package fr.anisekai.sanctum;

import fr.anisekai.sanctum.exceptions.scope.ScopeDefinitionException;
import fr.anisekai.sanctum.interfaces.FileStore;
import fr.anisekai.sanctum.interfaces.ScopedEntity;
import fr.anisekai.sanctum.interfaces.isolation.IsolationSessionDescriptor;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Represent an access scope within a {@link FileStore} granted for a {@link IsolationSessionDescriptor}.
 *
 * @param store
 *         The {@link FileStore} targeted by this {@link AccessScope}.
 * @param claim
 *         The {@link ScopedEntity} targeted by this {@link AccessScope}.
 */
public record AccessScope(FileStore store, ScopedEntity claim) {

    /**
     * Provide default sanity checks when creating an {@link AccessScope}.
     *
     * @param store
     *         The {@link FileStore} targeted by this {@link AccessScope}.
     * @param claim
     *         The {@link ScopedEntity} targeted by this {@link AccessScope}.
     */
    public AccessScope {

        if (store == null) {
            throw new ScopeDefinitionException("The file store cannot be null");
        }

        if (claim == null) {
            throw new ScopeDefinitionException("The claim cannot be null");
        }

        if (claim.getScopedName() == null) {
            throw new ScopeDefinitionException("The scope name cannot be null");
        }

        if (!store.type().isScoped()) {
            throw new ScopeDefinitionException("Cannot create an access scope targeting a non-scoped store.");
        }

        if (!store.scope().equals(claim.getClass())) {
            throw new ScopeDefinitionException(String.format(
                    "Cannot create an access scope using a non compatible scoped entity type on '%s' store.",
                    store.name()
            ));
        }
    }

    @Override
    public boolean equals(Object o) {

        if (!(o instanceof AccessScope(FileStore otherStore, ScopedEntity otherClaim))) return false;
        return Objects.equals(this.store(), otherStore) &&
                Objects.equals(this.claim().getClass(), otherClaim.getClass()) &&
                Objects.equals(this.claim().getScopedName(), otherClaim.getScopedName());
    }

    @Override
    public int hashCode() {

        return Objects.hash(this.store(), this.claim().getClass(), this.claim().getScopedName());
    }

    @Override
    public @NotNull String toString() {

        return String.format(
                "AccessScope{store='%s', claim='%s'}",
                this.store().name(),
                this.claim.getScopedName()
        );
    }

}
