package org.cloudburstmc.api.entity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.nbt.NbtTag;
import org.cloudburstmc.api.nbt.NbtTagType;

/**
 * Persistent data of a dropped item: everything shared plus its pickup delay
 * and the item itself. Health, age and owner travel through the load/save hooks:
 * they mirror the live combat, tick and metadata state instead.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class DroppedItemData extends EntityData {

    @JsonProperty("PickupDelay")
    @NbtTag(NbtTagType.SHORT)
    private int pickupDelay;

    @JsonProperty("Item")
    private ItemStack item = ItemStack.EMPTY;

    public DroppedItemData() {
    }
}
