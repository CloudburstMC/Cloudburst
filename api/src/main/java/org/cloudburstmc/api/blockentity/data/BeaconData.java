package org.cloudburstmc.api.blockentity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.api.nbt.NbtClient;

/**
 * Persistent data of a beacon: the network ids of its primary and secondary effects.
 * Either is absent while the beacon has no effect set, which today's save code cannot write.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class BeaconData extends BlockEntityData {

    @JsonProperty("primary")
    private Integer primary;

    @JsonProperty("secondary")
    private Integer secondary;

    public BeaconData() {
    }
}
