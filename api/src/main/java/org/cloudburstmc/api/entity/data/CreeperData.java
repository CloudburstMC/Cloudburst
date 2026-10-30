package org.cloudburstmc.api.entity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * Persistent data of a creeper: everything shared plus its charged state.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class CreeperData extends CreatureData {

    @JsonProperty("powered")
    private Boolean powered;

    public CreeperData() {
    }
}
