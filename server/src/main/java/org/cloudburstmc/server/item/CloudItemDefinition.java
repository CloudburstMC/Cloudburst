package org.cloudburstmc.server.item;

import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemVersion;

public class CloudItemDefinition implements ItemDefinition {

    private final Identifier identifier;
    private final int runtimeId;
    private final boolean componentBased;
    private final ItemVersion version;
    private final NbtMap componentData;

    public CloudItemDefinition(Identifier identifier, int runtimeId, boolean componentBased) {
        this(identifier, runtimeId, componentBased, ItemVersion.LEGACY, NbtMap.EMPTY);
    }

    public CloudItemDefinition(Identifier identifier, int runtimeId, boolean componentBased, ItemVersion version, NbtMap componentData) {
        this.identifier = identifier;
        this.runtimeId = runtimeId;
        this.componentBased = componentBased;
        this.version = version;
        this.componentData = componentData != null ? componentData : NbtMap.EMPTY;
    }

    public Identifier getCloudIdentifier() {
        return identifier;
    }

    @Override
    public int getRuntimeId() {
        return runtimeId;
    }

    @Override
    public String getIdentifier() {
        return identifier.toString();
    }

    @Override
    public boolean isComponentBased() {
        return componentBased;
    }

    @Override
    public ItemVersion getVersion() {
        return version;
    }

    @Override
    public NbtMap getComponentData() {
        return componentData;
    }
}
