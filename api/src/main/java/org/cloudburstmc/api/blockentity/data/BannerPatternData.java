package org.cloudburstmc.api.blockentity.data;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.api.nbt.NbtCodec;
import org.cloudburstmc.api.nbt.codec.BannerPatternTypeCodec;
import org.cloudburstmc.api.nbt.codec.DyeDataIntCodec;
import org.cloudburstmc.api.util.data.BannerPattern;
import org.cloudburstmc.api.util.data.DyeColor;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;

/**
 * One pattern layer on a banner: the dye color and the pattern shape. An absent
 * color reads as black, matching the zero today's load code produced.
 */
@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, isGetterVisibility = NONE, setterVisibility = NONE)
@Getter
@Setter
@EqualsAndHashCode
public class BannerPatternData {

    @JsonProperty("Color")
    @NbtCodec(DyeDataIntCodec.class)
    private DyeColor color = DyeColor.BLACK;

    @JsonProperty("Pattern")
    @NbtCodec(BannerPatternTypeCodec.class)
    private BannerPattern.Type pattern;

    public BannerPatternData() {
    }
}
