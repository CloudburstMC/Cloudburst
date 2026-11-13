package org.cloudburstmc.server.blockentity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.cloudburstmc.api.inventory.ItemContainer;
import org.cloudburstmc.server.container.CloudContainer;
import org.cloudburstmc.server.nbt.NbtClient;
import org.cloudburstmc.server.nbt.NbtContainerSize;
import org.cloudburstmc.server.nbt.NbtTag;
import org.cloudburstmc.server.nbt.NbtTagType;

/**
 * Persistent data of a brewing stand: its slotted items plus the brew timer and the fuel state.
 * The fuel keys are only written by the client view, but they are always present there.
 * The brewing stand is not loot-capable, so it declares its items separately.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class BrewingStandData extends BlockEntityData {

    @JsonProperty("Items")
    @NbtTag(value = NbtTagType.LIST, element = NbtTagType.COMPOUND)
    @NbtContainerSize(5)
    private ItemContainer items = new CloudContainer(5);

    @JsonProperty("CookTime")
    private int cookTime = 400;

    @NbtClient
    @JsonProperty("FuelAmount")
    private int fuelAmount;

    @NbtClient
    @JsonProperty("FuelTotal")
    private int fuelTotal;
}
