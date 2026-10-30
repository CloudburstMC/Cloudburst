package org.cloudburstmc.api.entity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.api.nbt.NbtTag;
import org.cloudburstmc.api.nbt.NbtTagType;
import org.cloudburstmc.nbt.NbtMap;

import java.util.List;

/**
 * Persistent data of an area effect cloud: everything shared plus its timers,
 * radius behavior, potion id, custom effects, color and duration scale.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class AreaEffectCloudData extends EntityData {

    @JsonProperty("mobEffects")
    @NbtTag(value = NbtTagType.LIST, element = NbtTagType.COMPOUND)
    private List<NbtMap> mobEffects;

    @JsonProperty("PotionId")
    private Short potionId;

    @JsonProperty("ParticleColor")
    private Integer particleColor;

    @JsonProperty("Duration")
    private Integer duration;

    @JsonProperty("DurationOnUse")
    private Integer durationOnUse;

    @JsonProperty("ReapplicationDelay")
    private Integer reapplicationDelay;

    @JsonProperty("Radius")
    private Float radius;

    @JsonProperty("RadiusOnUse")
    private Float radiusOnUse;

    @JsonProperty("RadiusPerTick")
    private Float radiusPerTick;

    @JsonProperty("WaitTime")
    private Integer waitTime;

    @JsonProperty("PotionDurationScale")
    private Float potionDurationScale;

    public AreaEffectCloudData() {
    }
}
