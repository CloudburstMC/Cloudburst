package org.cloudburstmc.api.entity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * Persistent data of an experience orb: everything shared plus its value,
 * count and pickup delay. Age travels through the load/save hooks, mirroring
 * the live tick counter instead.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class ExperienceOrbData extends EntityData {

    @JsonProperty("experience value")
    private Integer experienceValue = 1;

    @JsonProperty("Count")
    private int count = 1;

    @JsonProperty("PickupDelay")
    private short pickupDelay;

    public ExperienceOrbData() {
    }
}
