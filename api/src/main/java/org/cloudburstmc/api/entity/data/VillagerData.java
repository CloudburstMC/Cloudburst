package org.cloudburstmc.api.entity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * Persistent data of a villager: everything shared plus profession.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class VillagerData extends CreatureData {

    @JsonProperty("Profession")
    private Integer profession;

    public VillagerData() {
    }
}
