package org.cloudburstmc.api.blockentity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.api.nbt.NbtClient;

/**
 * Persistent data of a bed: its color as wool data. White by default, like today's save code.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class BedData extends BlockEntityData {

    @NbtClient
    @JsonProperty("color")
    private byte color;

    public BedData() {
    }
}
