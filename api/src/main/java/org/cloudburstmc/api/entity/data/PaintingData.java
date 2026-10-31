package org.cloudburstmc.api.entity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.api.entity.misc.Painting;
import org.cloudburstmc.api.nbt.NbtCodec;
import org.cloudburstmc.api.nbt.codec.MotiveCodec;

/**
 * Persistent data of a painting: hanging data plus its motive.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class PaintingData extends HangingData {

    @JsonProperty("Motive")
    @NbtCodec(MotiveCodec.class)
    private Painting.Motive motive = Painting.Motive.KEBAB;

    public PaintingData() {
    }
}
