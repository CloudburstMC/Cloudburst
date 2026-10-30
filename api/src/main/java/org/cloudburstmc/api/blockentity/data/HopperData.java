package org.cloudburstmc.api.blockentity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.api.inventory.ItemContainer;
import org.cloudburstmc.api.inventory.ItemContainers;
import org.cloudburstmc.api.nbt.NbtContainerSize;
import org.cloudburstmc.api.nbt.NbtTag;
import org.cloudburstmc.api.nbt.NbtTagType;

/**
 * Persistent data of a hopper: its 5-slot container plus the pull/push cooldown.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class HopperData extends ContainerData {

    @JsonProperty("Items")
    @NbtTag(value = NbtTagType.LIST, element = NbtTagType.COMPOUND)
    @NbtContainerSize(5)
    private ItemContainer items = ItemContainers.detached(5);

    @JsonProperty("TransferCooldown")
    private int transferCooldown = 8;

    public HopperData() {
    }
}
