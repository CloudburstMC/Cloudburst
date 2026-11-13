package org.cloudburstmc.server.entity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Persistent data of an ender dragon: everything shared plus death progress.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class EnderDragonData extends CreatureData {

    @JsonProperty("DragonDeathTime")
    private int dragonDeathTime;

    @JsonProperty("DragonDeathExperience")
    private int dragonDeathExperience;
}
