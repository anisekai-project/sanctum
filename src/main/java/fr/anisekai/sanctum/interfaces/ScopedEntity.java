package fr.anisekai.sanctum.interfaces;

/**
 * Interface defining an object that can be used as scope target within a scoped {@link FileStore}.
 */
public interface ScopedEntity {

    /**
     * Retrieve the scoped name of this {@link ScopedEntity}. In most cases, this will be the identifier of the parent entity.
     *
     * @return The name.
     */
    String getScopedName();

}
