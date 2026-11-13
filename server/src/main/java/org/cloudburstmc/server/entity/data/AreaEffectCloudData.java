package org.cloudburstmc.server.entity.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.cloudburstmc.api.potion.PotionEffect;
import org.cloudburstmc.api.potion.PotionType;
import org.cloudburstmc.server.nbt.NbtTag;
import org.cloudburstmc.server.nbt.NbtTagType;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class AreaEffectCloudData extends EntityData {

    @JsonProperty("mobEffects")
    @NbtTag(value = NbtTagType.LIST, element = NbtTagType.COMPOUND)
    private List<PotionEffect> mobEffects = new ArrayList<>();

    @JsonProperty("PotionId")
    @NbtTag(NbtTagType.SHORT)
    private PotionType potionId;

    @JsonProperty("Duration")
    private int duration;

    @JsonProperty("DurationOnUse")
    private int durationOnUse;

    @JsonProperty("ReapplicationDelay")
    private int reapplicationDelay;

    @JsonProperty("RadiusOnUse")
    private float radiusOnUse;

    @JsonProperty("RadiusPerTick")
    private float radiusPerTick;

    @JsonIgnore
    private Float potionDurationScale;
}
