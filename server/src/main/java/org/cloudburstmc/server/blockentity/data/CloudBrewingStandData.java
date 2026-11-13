package org.cloudburstmc.server.blockentity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.cloudburstmc.api.inventory.ItemContainer;
import org.cloudburstmc.server.container.CloudContainer;
import org.cloudburstmc.server.nbt.NbtTagType;
import org.cloudburstmc.server.nbt.annotation.NbtContainerSize;
import org.cloudburstmc.server.nbt.annotation.NbtTag;
import org.cloudburstmc.server.nbt.client.NbtClient;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class CloudBrewingStandData extends CloudBlockEntityData {

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
