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
 * Persistent data of a crafter: its 9-slot container plus the per-slot disable flags.
 * The crafter is not loot-capable, so it declares its items separately.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class CrafterData extends BlockEntityData {

    @JsonProperty("Items")
    @NbtTag(value = NbtTagType.LIST, element = NbtTagType.COMPOUND)
    @NbtContainerSize(9)
    private ItemContainer items = ItemContainers.detached(9);

    @JsonProperty("DisabledSlots")
    private short disabledSlots;

    public CrafterData() {
    }
}
