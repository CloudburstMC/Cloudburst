package org.cloudburstmc.server.block;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.protocol.bedrock.data.defintions.BlockDefinition;

@Value
@EqualsAndHashCode(callSuper = false)
public class CloudBlockDefinition extends BlockDefinition {
    BlockState cloudState;
    NbtMap state;
    int runtimeId;

    @Override
    public String getIdentifier() {
        return cloudState.getType().getId().toString();
    }
}
