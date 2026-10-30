package org.cloudburstmc.api.entity.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.api.nbt.NbtTag;
import org.cloudburstmc.api.nbt.NbtTagType;
import org.cloudburstmc.api.potion.PotionEffect;
import org.cloudburstmc.api.potion.PotionType;

import java.util.ArrayList;
import java.util.List;

/**
 * Persistent data of an area effect cloud: everything shared plus its timers,
 * radius behavior, potion id, custom effects and duration scale. Radius,
 * wait time and particle color are metadata-backed, so they travel through
 * the load/save hooks instead. The effect list is the live one the runtime
 * mutates; a read replaces it, so the runtime keeps no second copy. The
 * duration scale is conditional: a scale of one is not written, so it travels
 * through the load/save hooks.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
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

    public AreaEffectCloudData() {
    }
}
