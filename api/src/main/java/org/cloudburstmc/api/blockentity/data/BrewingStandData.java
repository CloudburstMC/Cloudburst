package org.cloudburstmc.api.blockentity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.api.nbt.NbtClient;
import org.cloudburstmc.api.nbt.NbtTag;
import org.cloudburstmc.api.nbt.NbtTagType;

import java.util.List;

/**
 * Persistent data of a brewing stand: its slotted items plus the brew timer and the fuel state.
 * The fuel keys are only written by the client view, but they are always present there.
 * The brewing stand is not loot-capable, so it declares its items separately.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class BrewingStandData extends BlockEntityData {

    @JsonProperty("Items")
    @NbtTag(value = NbtTagType.LIST, element = NbtTagType.COMPOUND)
    private List<SlottedItem> items = List.of();

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
