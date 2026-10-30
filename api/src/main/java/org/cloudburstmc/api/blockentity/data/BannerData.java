package org.cloudburstmc.api.blockentity.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.api.nbt.NbtClient;
import org.cloudburstmc.api.nbt.NbtCodec;
import org.cloudburstmc.api.nbt.NbtTag;
import org.cloudburstmc.api.nbt.NbtTagType;
import org.cloudburstmc.api.nbt.codec.BannerPatternsCodec;
import org.cloudburstmc.api.nbt.codec.DyeDataIntCodec;
import org.cloudburstmc.api.util.data.BannerPattern;
import org.cloudburstmc.api.util.data.DyeColor;

import java.util.List;

/**
 * Persistent data of a banner: the base color and banner type, plus the pattern
 * layers if any. The base defaults to white, like today's save code. Whether an
 * empty pattern list was explicitly loaded is tracked outside the tag, so a fresh
 * banner and a loaded empty list save their client tags as today.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class BannerData extends BlockEntityData {

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

    public BannerData() {
    }
}
