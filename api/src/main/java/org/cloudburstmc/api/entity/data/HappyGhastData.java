package org.cloudburstmc.api.entity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * Persistent data of a happy ghast: everything shared plus age.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class HappyGhastData extends CreatureData {

    @JsonProperty("Age")
    private int age;

    public HappyGhastData() {
    }
}
