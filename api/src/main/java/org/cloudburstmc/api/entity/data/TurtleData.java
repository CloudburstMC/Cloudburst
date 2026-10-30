package org.cloudburstmc.api.entity.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.math.vector.Vector3i;

/**
 * Persistent data of a turtle: everything shared plus age and home position.
 * The home position spans three NBT keys and travels through the load/save
 * hooks; the DTO holds it once as a runtime value.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class TurtleData extends CreatureData {

    @JsonProperty("Age")
    private int age;

    @JsonIgnore
    private Vector3i homePosition = Vector3i.ZERO;

    public TurtleData() {
    }
}
