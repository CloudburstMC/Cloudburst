package org.cloudburstmc.api.entity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * Persistent data of a turtle: everything shared plus age and home position.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class TurtleData extends CreatureData {

    @JsonProperty("Age")
    private int age;

    @JsonProperty("HomePosX")
    private Integer homePosX;

    @JsonProperty("HomePosY")
    private Integer homePosY;

    @JsonProperty("HomePosZ")
    private Integer homePosZ;

    public TurtleData() {
    }
}
