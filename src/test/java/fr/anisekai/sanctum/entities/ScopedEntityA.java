package fr.anisekai.sanctum.entities;

import fr.anisekai.sanctum.interfaces.ScopedEntity;

public class ScopedEntityA implements ScopedEntity {

    private final String name;

    public ScopedEntityA(String name) {

        this.name = name;
    }

    @Override
    public String getScopedName() {

        return this.name;
    }

}
