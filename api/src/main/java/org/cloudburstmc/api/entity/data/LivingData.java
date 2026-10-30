package org.cloudburstmc.api.entity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * Persistent data of a living entity: everything shared plus health.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class LivingData extends EntityData {

    @JsonProperty("Health")
    private float health = 20;

    public LivingData() {
    }
}
