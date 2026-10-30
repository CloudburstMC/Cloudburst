package org.cloudburstmc.api.blockentity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * Persistent data of a chest: its container plus whether it can pair.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class ChestData extends ContainerData {

    @JsonProperty("Findable")
    private boolean findable;

    public ChestData() {
    }
}
