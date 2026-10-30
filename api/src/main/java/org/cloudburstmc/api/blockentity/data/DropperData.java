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
 * Persistent data of a dropper: exactly its 9-slot container.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class DropperData extends ContainerData {

    @JsonProperty("Items")
    @NbtTag(value = NbtTagType.LIST, element = NbtTagType.COMPOUND)
    @NbtContainerSize(9)
    private ItemContainer items = ItemContainers.detached(9);

    public DropperData() {
    }
}
