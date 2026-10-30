package org.cloudburstmc.api.entity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.api.nbt.NbtTag;
import org.cloudburstmc.api.nbt.NbtTagType;

/**
 * Persistent data of an abstract arrow: projectile data plus its stuck state,
 * pierce level, embedded block position and pickup rule.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class AbstractArrowData extends ProjectileData {

    @JsonProperty("inGround")
    private Boolean inGround;

    @JsonProperty("PierceLevel")
    @NbtTag(NbtTagType.BYTE)
    private Integer pierceLevel;

    @JsonProperty("inBlockX")
    private Integer inBlockX;

    @JsonProperty("inBlockY")
    private Integer inBlockY;

    @JsonProperty("inBlockZ")
    private Integer inBlockZ;

    @JsonProperty("pickup")
    @NbtTag(NbtTagType.BYTE)
    private int pickup = 1;

    public AbstractArrowData() {
    }
}
