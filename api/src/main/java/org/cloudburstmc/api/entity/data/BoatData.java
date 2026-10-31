package org.cloudburstmc.api.entity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.api.entity.vehicle.BoatType;

/**
 * Persistent data of a boat: everything shared plus its hull variant.
 * The setter keeps the client metadata in sync.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class BoatData extends EntityData {

    @JsonProperty("WoodType")
    private BoatType woodType = BoatType.OAK;

    public BoatData() {
    }
}
