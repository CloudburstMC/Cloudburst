package org.cloudburstmc.api.entity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * Persistent data of a boat: everything shared plus its wood variant.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class BoatData extends EntityData {

    @JsonProperty("WoodType")
    private int woodType;

    public BoatData() {
    }
}
