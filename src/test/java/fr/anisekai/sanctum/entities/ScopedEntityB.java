package fr.anisekai.sanctum.entities;

import fr.anisekai.sanctum.interfaces.ScopedEntity;

public class ScopedEntityB implements ScopedEntity {

    private final String name;

    public ScopedEntityB(String name) {

        this.name = name;
    }

    @Override
    public String getScopedName() {

        return this.name;
    }

}
