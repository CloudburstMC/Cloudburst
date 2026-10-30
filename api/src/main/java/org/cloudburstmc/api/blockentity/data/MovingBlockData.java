package org.cloudburstmc.api.blockentity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.nbt.NbtMap;

/**
 * Persistent data of a moving block: the moved and extra block states, the piston
 * position and the nested block-entity tag, if any.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class MovingBlockData extends BlockEntityData {

    @JsonProperty("movingBlock")
    private BlockState movingBlock;

    @JsonProperty("movingBlockExtra")
    private BlockState movingBlockExtra = BlockStates.AIR;

    @JsonProperty("movingEntity")
    private NbtMap movingEntity;

    @JsonProperty("pistonPosX")
    private int pistonPosX;

    @JsonProperty("pistonPosY")
    private int pistonPosY;

    @JsonProperty("pistonPosZ")
    private int pistonPosZ;

    public MovingBlockData() {
    }
}
