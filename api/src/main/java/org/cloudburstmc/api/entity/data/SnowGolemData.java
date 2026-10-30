package org.cloudburstmc.api.entity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * Persistent data of a snow golem: everything shared plus whether it wears a pumpkin.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class SnowGolemData extends CreatureData {

    @JsonProperty("Pumpkin")
    private Boolean pumpkin;

    public SnowGolemData() {
    }
}
