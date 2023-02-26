package org.cloudburstmc.server.item;

import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.protocol.bedrock.data.defintions.ItemDefinition;

public class CloudItemDefinition implements ItemDefinition {

    private final Identifier identifier;
    private final int runtimeId;
    private final boolean componentBased;

    public CloudItemDefinition(Identifier identifier, int runtimeId, boolean componentBased) {
        this.identifier = identifier;
        this.runtimeId = runtimeId;
        this.componentBased = componentBased;
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
}
