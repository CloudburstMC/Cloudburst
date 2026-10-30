package org.cloudburstmc.api.entity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.api.nbt.NbtTag;
import org.cloudburstmc.api.nbt.NbtTagType;

/**
 * Persistent data of an experience orb: everything shared plus its
 * count and pickup delay. Age travels through the load/save hooks, mirroring
 * the live tick counter instead, and the value is metadata-backed so it
 * travels through the hooks too.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class ExperienceOrbData extends EntityData {

    @JsonProperty("Count")
    private int count = 1;

    @JsonProperty("PickupDelay")
    @NbtTag(NbtTagType.SHORT)
    private int pickupDelay;

    public ExperienceOrbData() {
    }
}
