package fr.anisekai.sanctum;

import fr.anisekai.sanctum.interfaces.FileStore;
import fr.anisekai.sanctum.interfaces.isolation.IsolationSession;
import fr.anisekai.sanctum.interfaces.isolation.IsolationSessionAware;
import fr.anisekai.sanctum.interfaces.resolvers.StorageResolver;

import java.nio.file.Path;

/**
 * Default implementation of {@link IsolationSession}
 *
 * @param owner
 *         The {@link IsolationSessionAware} that created this {@link IsolationSession}
 * @param root
 *         The {@link Path} into which this {@link IsolationSession} is located.
 * @param name
 *         The name of this {@link IsolationSession}.
 */
public record IsolationSessionImpl(IsolationSessionAware owner, Path root, String name) implements IsolationSession {

    @Override
    public String getScopedName() {

        return this.name();
    }

    @Override
    public StorageResolver getResolver(FileStore store) {

        return this.owner.getResolver(this, store);
    }

}
