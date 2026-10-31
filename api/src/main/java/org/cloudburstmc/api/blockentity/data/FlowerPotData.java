package org.cloudburstmc.api.blockentity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.api.block.BlockState;

/**
 * Persistent data of a flower pot: the planted block, if any.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class FlowerPotData extends BlockEntityData {

    @JsonProperty("PlantBlock")
    private BlockState plant;

    public FlowerPotData() {
    }
}
