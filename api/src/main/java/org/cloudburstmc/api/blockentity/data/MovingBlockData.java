package org.cloudburstmc.api.blockentity.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;

/**
 * Persistent data of a moving block: the moved and extra block states plus the
 * nested block-entity tag, if any. An absent moved state reads as air, like today's
 * load code. The piston position is written by the load/save hooks.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class MovingBlockData extends BlockEntityData {

    @JsonProperty("movingBlock")
    private BlockState movingBlock = BlockStates.AIR;

    @JsonProperty("movingBlockExtra")
    private BlockState movingBlockExtra = BlockStates.AIR;

    @JsonProperty("movingEntity")
    private NbtMap movingEntity;

    @JsonIgnore
    private Vector3i piston;

    public MovingBlockData() {
    }
}
