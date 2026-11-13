package org.cloudburstmc.server.blockentity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.cloudburstmc.api.util.data.DyeColor;
import org.cloudburstmc.server.nbt.NbtClient;
import org.cloudburstmc.server.nbt.NbtCodec;
import org.cloudburstmc.server.nbt.codec.WoolColorCodec;

/**
 * Persistent data of a bed: its color as wool data. White by default, like today's save code.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class BedData extends BlockEntityData {

    @NbtClient
    @JsonProperty("color")
    @NbtCodec(WoolColorCodec.class)
    private DyeColor color = DyeColor.WHITE;
}
