package org.cloudburstmc.server.metadata;

import org.cloudburstmc.server.entity.BaseEntity;

import javax.inject.Singleton;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
@Singleton
public class EntityMetadataStore extends MetadataStore {

    @Override
    protected String disambiguate(Metadatable entity, String metadataKey) {
        if (!(entity instanceof BaseEntity)) {
            throw new IllegalArgumentException("Argument must be an Entity instance");
        }
        return ((BaseEntity) entity).getUniqueId() + ":" + metadataKey;
    }
}
