package org.cloudburstmc.server.entity;

import org.cloudburstmc.api.plugin.PluginContainer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EntityProvider<T extends Entity> {
    private final List<EntityFactory<T>> priority = new ArrayList<>();

    public EntityProvider(Map<PluginContainer, EntityFactory<T>> factoryMap) {
    }
}
