package org.cloudburstmc.api.blockentity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.api.nbt.NbtClient;

/**
 * Persistent data of a brewing stand: its container plus the brew timer and the fuel state.
 * The fuel keys are only written by the client view, but they are always present there.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class BrewingStandData extends ContainerData {

    @JsonProperty("CookTime")
    private int cookTime = 400;

    @NbtClient
    @JsonProperty("FuelAmount")
    private int fuelAmount;

    @NbtClient
    @JsonProperty("FuelTotal")
    private short fuelTotal;

    public BrewingStandData() {
    }
}
