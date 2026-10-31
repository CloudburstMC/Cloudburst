package org.cloudburstmc.api.blockentity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.api.nbt.NbtClient;
import org.cloudburstmc.api.nbt.NbtCodec;
import org.cloudburstmc.api.nbt.codec.WoolColorCodec;
import org.cloudburstmc.api.util.data.DyeColor;

/**
 * Persistent data of a bed: its color as wool data. White by default, like today's save code.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class BedData extends BlockEntityData {

    @NbtClient
    @JsonProperty("color")
    @NbtCodec(WoolColorCodec.class)
    private DyeColor color = DyeColor.WHITE;

    public BedData() {
    }
}
