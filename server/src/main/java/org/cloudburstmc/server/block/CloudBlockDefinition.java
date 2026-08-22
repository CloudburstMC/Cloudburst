package org.cloudburstmc.server.block;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.protocol.bedrock.data.definitions.BlockDefinition;

@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class CloudBlockDefinition implements BlockDefinition {
    private final BlockState cloudState;
    private final NbtMap state;
    private final int runtimeId;
    private final long blockStateHash;
}
