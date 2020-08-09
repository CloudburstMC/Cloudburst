package org.cloudburstmc.server.metadata;

import org.cloudburstmc.server.entity.impl.BaseEntity;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class EntityMetadataStore extends MetadataStore {

    @Override
    protected String disambiguate(Metadatable entity, String metadataKey) {
        if (!(entity instanceof BaseEntity)) {
            throw new IllegalArgumentException("Argument must be an Entity instance");
        }
        return ((BaseEntity) entity).getUniqueId() + ":" + metadataKey;
    }
}
