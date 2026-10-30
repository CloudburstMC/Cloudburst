package org.cloudburstmc.api.entity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * Persistent data of an experience orb: everything shared plus its value,
 * age, count and pickup delay.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class ExperienceOrbData extends EntityData {

    @JsonProperty("experience value")
    private Integer experienceValue;

    @JsonProperty("Age")
    private Short age;

    @JsonProperty("Count")
    private Integer count;

    @JsonProperty("PickupDelay")
    private Short pickupDelay;

    public ExperienceOrbData() {
    }
}
