package org.cloudburstmc.server.entity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class CloudEnderDragonData extends CloudCreatureData {

    @JsonProperty("DragonDeathTime")
    private int dragonDeathTime;

    @JsonProperty("DragonDeathExperience")
    private int dragonDeathExperience;
}
