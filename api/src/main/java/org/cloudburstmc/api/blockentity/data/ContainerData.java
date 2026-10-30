package org.cloudburstmc.api.blockentity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.nbt.NbtTag;
import org.cloudburstmc.api.nbt.NbtTagType;

import java.util.ArrayList;
import java.util.List;

/**
 * Persistent data shared by every item container: its slotted items plus an optional
 * loot-table reference. Concrete container DTOs declare their own keys on top; the
 * crafter is not loot-capable and declares its items separately.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public abstract class ContainerData extends BlockEntityData implements LootTableHolder {

    @JsonProperty("Items")
    @NbtTag(value = NbtTagType.LIST, element = NbtTagType.COMPOUND)
    private List<SlottedItem> items = List.of();

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
