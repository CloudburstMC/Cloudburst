package org.cloudburstmc.api.entity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.api.inventory.ItemContainer;
import org.cloudburstmc.api.inventory.ItemContainers;
import org.cloudburstmc.api.nbt.NbtContainerSize;
import org.cloudburstmc.api.nbt.NbtSkipEmpty;
import org.cloudburstmc.api.nbt.NbtTag;
import org.cloudburstmc.api.nbt.NbtTagType;

/**
 * Persistent data of a chest boat: boat data plus its stored items. The container
 * is the live one the chest boat's inventory view wraps; a read fills it in place.
 * Only non-empty slots are written, each with its {@code Slot} byte.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class ChestBoatData extends BoatData {

    @JsonProperty("Items")
    @NbtTag(value = NbtTagType.LIST, element = NbtTagType.COMPOUND)
    @NbtContainerSize(27)
    @NbtSkipEmpty
    private ItemContainer items = ItemContainers.detached(27);

    public ChestBoatData() {
    }
}
