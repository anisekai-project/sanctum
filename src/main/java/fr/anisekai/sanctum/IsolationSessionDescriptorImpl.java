package fr.anisekai.sanctum;

import fr.anisekai.sanctum.interfaces.isolation.IsolationSession;
import fr.anisekai.sanctum.interfaces.isolation.IsolationSessionDescriptor;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of {@link IsolationSessionDescriptor}.
 */
public class IsolationSessionDescriptorImpl implements IsolationSessionDescriptor {

    private final UUID             uuid;
    private final Set<AccessScope> scopes;
    private final IsolationSession context;
    private volatile boolean       committed = false;

    /**
     * Create a new {@link IsolationSessionDescriptorImpl} instance.
     *
     * @param uuid
     *         The uuid of this {@link IsolationSessionDescriptor}.
     * @param context
     *         The {@link IsolationSession} associated to this {@link IsolationSessionDescriptor}.
     */
    public IsolationSessionDescriptorImpl(UUID uuid, IsolationSession context) {

        this.uuid    = uuid;
        this.scopes  = ConcurrentHashMap.newKeySet();
        this.context = context;
    }

    @Override
    public UUID uuid() {

        return this.uuid;
    }

    @Override
    public IsolationSession context() {

        return this.context;
    }

    @Override
    public Collection<AccessScope> scopes() {

        return List.copyOf(this.scopes);
    }

    @Override
    public boolean hasScope(AccessScope scope) {

        return this.scopes.contains(scope);
    }

    @Override
    public void grantScope(AccessScope scope) {

        this.scopes.add(scope);
    }

    @Override
    public boolean isCommitted() {

        return this.committed;
    }

    @Override
    public void setCommitted(boolean committed) {

        this.committed = committed;
    }

}
