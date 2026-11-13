package org.cloudburstmc.server.blockentity.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class MovingBlockData extends BlockEntityData {

    @JsonProperty("movingBlock")
    private BlockState movingBlock = BlockStates.AIR;

    @JsonProperty("movingBlockExtra")
    private BlockState movingBlockExtra = BlockStates.AIR;

    @JsonProperty("movingEntity")
    private NbtMap movingEntity;

    @JsonIgnore
    private Vector3i piston;
}
