package org.cloudburstmc.api.entity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.api.nbt.NbtCodec;
import org.cloudburstmc.api.nbt.codec.DirectionCodec;
import org.cloudburstmc.api.util.Direction;

/**
 * Persistent data of a hanging entity: everything shared plus its attachment direction.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class HangingData extends EntityData {

    @JsonProperty("Direction")
    @NbtCodec(DirectionCodec.class)
    private Direction direction = Direction.DOWN;

    public HangingData() {
    }
}
