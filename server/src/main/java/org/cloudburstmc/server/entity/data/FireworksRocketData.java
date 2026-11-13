package org.cloudburstmc.server.entity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class FireworksRocketData extends ProjectileData {

    @JsonProperty("Life")
    private int life;

    @JsonProperty("LifeTime")
    private int lifetime;

    @JsonProperty("ShotAtAngle")
    private boolean shotAtAngle;
}
