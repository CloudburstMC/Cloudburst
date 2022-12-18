package org.cloudburstmc.server.item;

import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.protocol.bedrock.data.defintions.ItemDefinition;

public class CloudItemDefinition extends ItemDefinition {

    private final Identifier identifier;

    public CloudItemDefinition(Identifier identifier, int runtimeId, boolean componentBased) {
        super(identifier.toString(), runtimeId, componentBased);
        this.identifier = identifier;
    }

    public Identifier getCloudIdentifier() {
        return identifier;
    }
}
