package org.cloudburstmc.api.blockentity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.api.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Persistent data shared by every loot-capable item container: an optional loot-table
 * reference. Each concrete container DTO declares its own items property with the slot
 * count of its block-entity type.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public abstract class ContainerData extends BlockEntityData implements LootTableHolder {

    @JsonProperty("LootTable")
    private String lootTable;

    @JsonProperty("LootTableSeed")
    private Integer lootTableSeed;

    protected ContainerData() {
    }

    /**
     * Copies container contents into slotted items, one entry per slot.
     *
     * <p>Empty slots are kept as air compounds so the saved list always has one
     * entry per slot, exactly as today's save code writes.
     *
     * @param contents the container's contents, one entry per slot
     * @return the slotted items
     */
    public static List<SlottedItem> slotted(ItemStack[] contents) {
        List<SlottedItem> items = new ArrayList<>(contents.length);
        for (int slot = 0; slot < contents.length; slot++) {
            items.add(new SlottedItem(slot, contents[slot]));
        }
        return items;
    }
}
