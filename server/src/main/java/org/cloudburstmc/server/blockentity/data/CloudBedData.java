package org.cloudburstmc.server.blockentity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.cloudburstmc.api.util.data.DyeColor;
import org.cloudburstmc.server.nbt.annotation.NbtCodec;
import org.cloudburstmc.server.nbt.client.NbtClient;
import org.cloudburstmc.server.nbt.codec.WoolColorCodec;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class CloudBedData extends CloudBlockEntityData {

    @NbtClient
    @JsonProperty("color")
    @NbtCodec(WoolColorCodec.class)
    private DyeColor color = DyeColor.WHITE;
}
