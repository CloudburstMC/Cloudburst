package org.cloudburstmc.server.block;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.protocol.bedrock.data.defintions.BlockDefinition;

@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class CloudBlockDefinition implements BlockDefinition {
    BlockState cloudState;
    NbtMap state;
    int runtimeId;
}
