package org.cloudburstmc.server.blockentity.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.cloudburstmc.api.util.data.BannerPattern;
import org.cloudburstmc.api.util.data.DyeColor;
import org.cloudburstmc.server.nbt.NbtClient;
import org.cloudburstmc.server.nbt.NbtCodec;
import org.cloudburstmc.server.nbt.NbtTag;
import org.cloudburstmc.server.nbt.NbtTagType;
import org.cloudburstmc.server.nbt.codec.BannerPatternsCodec;
import org.cloudburstmc.server.nbt.codec.DyeDataIntCodec;

import java.util.List;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class CloudBannerData extends CloudBlockEntityData {

    @NbtClient
    @JsonProperty("Base")
    @NbtCodec(DyeDataIntCodec.class)
    private DyeColor base = DyeColor.WHITE;

    @NbtClient
    @JsonProperty("Type")
    private int type;

    @NbtClient
    @JsonProperty("Patterns")
    @NbtCodec(BannerPatternsCodec.class)
    @NbtTag(value = NbtTagType.LIST, element = NbtTagType.COMPOUND)
    private List<BannerPattern> patterns;

    @JsonIgnore
    private boolean patternsPresent;
}
