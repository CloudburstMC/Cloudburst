package org.cloudburstmc.api.entity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * Persistent data of a splash potion: projectile data plus its potion id.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class SplashPotionData extends ProjectileData {

    @JsonProperty("PotionId")
    private Short potionId;

    public SplashPotionData() {
    }
}
