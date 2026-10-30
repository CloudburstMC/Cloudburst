package org.cloudburstmc.api.blockentity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.api.nbt.NbtClient;
import org.cloudburstmc.api.nbt.NbtTag;
import org.cloudburstmc.api.nbt.NbtTagType;

import java.util.List;

/**
 * Persistent data of a banner: the base color and banner type, plus the pattern
 * layers if any. The base defaults to white, like today's save code.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class BannerData extends BlockEntityData {

    @NbtClient
    @JsonProperty("Base")
    private int base = 15;

    @NbtClient
    @JsonProperty("Type")
    private int type;

    @NbtClient
    @JsonProperty("Patterns")
    @NbtTag(value = NbtTagType.LIST, element = NbtTagType.COMPOUND)
    private List<BannerPatternData> patterns;

    public BannerData() {
    }
}
