package fr.anisekai.sanctum.resolvers.policies;

import fr.anisekai.sanctum.AccessScope;
import fr.anisekai.sanctum.exceptions.scope.ScopeForbiddenException;
import fr.anisekai.sanctum.interfaces.FileStore;
import fr.anisekai.sanctum.interfaces.ScopedEntity;
import fr.anisekai.sanctum.interfaces.isolation.IsolationSessionDescriptor;
import fr.anisekai.sanctum.interfaces.resolvers.ResolverPolicy;
import fr.anisekai.sanctum.interfaces.resolvers.StorageResolver;

/**
 * Implementation of {@link ResolverPolicy} allowing to limit a {@link StorageResolver} to a {@link IsolationSessionDescriptor}
 * scopes.
 *
 * @param context
 *         The {@link IsolationSessionDescriptor} from which the {@link AccessScope} should be used.
 * @param store
 *         The {@link FileStore} being accessed within a {@link StorageResolver}.
 */
public record IsolationResolverPolicy(IsolationSessionDescriptor context, FileStore store) implements ResolverPolicy {

    @Override
    public void checkResolveDirectory(String name) {

    }

    @Override
    public void checkResolveFile(String filename) {

    }

    @Override
    public void checkResolveDirectory(ScopedEntity entity) {

        AccessScope scope = new AccessScope(this.store(), entity);
        if (this.context().hasScope(scope)) return;
        throw new ScopeForbiddenException(String.format(
                "The scope '%s' is not within the allowed grants of the isolation '%s'",
                scope,
                this.context().name()
        ));
    }

    @Override
    public void checkResolveFile(ScopedEntity entity) {

        AccessScope scope = new AccessScope(this.store(), entity);
        if (this.context().hasScope(scope)) return;
        throw new ScopeForbiddenException(String.format(
                "The scope '%s' is not within the allowed grants of the isolation '%s'",
                scope,
                this.context().name()
        ));
    }

    @Override
    public void checkResolveFile(ScopedEntity entity, String name) {

        AccessScope scope = new AccessScope(this.store(), entity);
        if (this.context().hasScope(scope)) return;
        throw new ScopeForbiddenException(String.format(
                "The scope '%s' is not within the allowed grants of the isolation '%s'",
                scope,
                this.context().name()
        ));
    }

}
