package fr.anisekai.sanctum.interfaces.resolvers;

import fr.anisekai.sanctum.interfaces.ScopedEntity;

import java.util.Arrays;
import java.util.Collection;

/**
 * Interface defining a {@link StorageResolver} contract by leveraging {@link UnsupportedOperationException} on illegal resolve
 * query.
 */
public interface ResolverPolicy {

    /**
     * Create a {@link ResolverPolicy} that will execute all provided {@link ResolverPolicy} in a chain, in the order provided. In
     * order for any checks to succeed, all {@link ResolverPolicy} must resolve without any exception, otherwise the chain would
     * be broken and the resolving would be denied.
     *
     * @param policies
     *         The {@link ResolverPolicy} to chain.
     *
     * @return A {@link ResolverPolicy} chain
     */
    static ResolverPolicy chained(ResolverPolicy... policies) {

        Collection<ResolverPolicy> policyChain = Arrays.asList(policies);

        return new ResolverPolicy() {
            @Override
            public void checkResolveDirectory(String name) {

                policyChain.forEach(p -> p.checkResolveDirectory(name));
            }

            @Override
            public void checkResolveFile(String filename) {

                policyChain.forEach(p -> p.checkResolveFile(filename));
            }

            @Override
            public void checkResolveDirectory(ScopedEntity entity) {

                policyChain.forEach(p -> p.checkResolveDirectory(entity));
            }

            @Override
            public void checkResolveFile(ScopedEntity entity) {

                policyChain.forEach(p -> p.checkResolveFile(entity));
            }

            @Override
            public void checkResolveFile(ScopedEntity entity, String name) {

                policyChain.forEach(p -> p.checkResolveFile(entity, name));
            }
        };
    }

    /**
     * Check if a directory can be resolved by its name.
     *
     * @param name
     *         The name of the directory being resolved.
     */
    void checkResolveDirectory(String name);

    /**
     * Check if a file can be resolved by its name.
     *
     * @param filename
     *         The name of the file being resolved.
     */
    void checkResolveFile(String filename);

    /**
     * Check if a directory can be resolved by its matching {@link ScopedEntity}.
     *
     * @param entity
     *         The {@link ScopedEntity} used to resolve the directory.
     */
    void checkResolveDirectory(ScopedEntity entity);

    /**
     * Check if a file can be resolved by its matching {@link ScopedEntity}.
     *
     * @param entity
     *         The {@link ScopedEntity} used to resolve the file.
     */
    void checkResolveFile(ScopedEntity entity);

    /**
     * Check if a file can be resolved by its name within a {@link ScopedEntity} directory.
     *
     * @param entity
     *         The {@link ScopedEntity} used to resolve the directory.
     * @param name
     *         The nameof the file being resolved.
     */
    void checkResolveFile(ScopedEntity entity, String name);

}
