package org.cloudburstmc.api.entity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.api.nbt.NbtTag;
import org.cloudburstmc.api.nbt.NbtTagType;
import org.cloudburstmc.api.potion.PotionType;

/**
 * Persistent data of a splash potion: projectile data plus its potion type.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class SplashPotionData extends ProjectileData {

    @JsonProperty("PotionId")
    @NbtTag(NbtTagType.SHORT)
    private PotionType potionId;

    public SplashPotionData() {
    }
}
