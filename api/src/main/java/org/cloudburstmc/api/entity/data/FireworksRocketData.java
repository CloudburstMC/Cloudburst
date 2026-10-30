package org.cloudburstmc.api.entity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * Persistent data of a fireworks rocket: projectile data plus its life,
 * lifetime and launch angle.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class FireworksRocketData extends ProjectileData {

    @JsonProperty("Life")
    private Integer life;

    @JsonProperty("LifeTime")
    private Integer lifetime;

    @JsonProperty("ShotAtAngle")
    private Boolean shotAtAngle;

    public FireworksRocketData() {
    }
}
