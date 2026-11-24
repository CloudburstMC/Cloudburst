package org.cloudburstmc.server.entity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.item.ItemStack;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class CloudFireworkRocketData extends CloudProjectileData {

    @JsonProperty("Life")
    private int ticksFlown;

    @JsonProperty("LifeTime")
    private int ticksToDetonate;

    @JsonProperty("ShotAtAngle")
    private boolean shotAtAngle;

    @JsonProperty("FireworksItem")
    private @Nullable ItemStack firework;
}
