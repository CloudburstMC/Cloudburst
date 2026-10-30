package org.cloudburstmc.api.blockentity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * Persistent data of a furnace: its container plus the cook, burn and burn-duration counters.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class FurnaceData extends ContainerData {

    @JsonProperty("CookTime")
    private short cookTime;

    @JsonProperty("BurnTime")
    private short burnTime;

    @JsonProperty("BurnDuration")
    private short burnDuration;

    public FurnaceData() {
    }
}
