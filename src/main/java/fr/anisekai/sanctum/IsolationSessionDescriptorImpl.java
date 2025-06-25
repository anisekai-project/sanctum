package fr.anisekai.sanctum;

import fr.anisekai.sanctum.interfaces.isolation.IsolationSession;
import fr.anisekai.sanctum.interfaces.isolation.IsolationSessionDescriptor;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Default implementation of {@link IsolationSessionDescriptor}.
 */
public class IsolationSessionDescriptorImpl implements IsolationSessionDescriptor {

    private final String           name;
    private final Set<AccessScope> scopes;
    private final IsolationSession context;
    private       boolean          committed = false;

    /**
     * Create a new {@link IsolationSessionDescriptorImpl} instance.
     *
     * @param name
     *         The name of this {@link IsolationSessionDescriptor}.
     * @param context
     *         The {@link IsolationSession} associated to this {@link IsolationSessionDescriptor}.
     */
    public IsolationSessionDescriptorImpl(String name, IsolationSession context) {

        this.name    = name;
        this.scopes  = new HashSet<>();
        this.context = context;
    }

    @Override
    public String name() {

        return this.name;
    }

    @Override
    public IsolationSession context() {

        return this.context;
    }

    @Override
    public Collection<AccessScope> scopes() {

        return Collections.unmodifiableCollection(this.scopes);
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
