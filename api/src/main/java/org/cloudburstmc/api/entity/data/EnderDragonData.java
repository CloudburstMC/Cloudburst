package org.cloudburstmc.api.entity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * Persistent data of an ender dragon: everything shared plus death progress.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class EnderDragonData extends CreatureData {

    @JsonProperty("DragonDeathTime")
    private int dragonDeathTime;

    @JsonProperty("DragonDeathExperience")
    private int dragonDeathExperience;

    public EnderDragonData() {
    }
}
