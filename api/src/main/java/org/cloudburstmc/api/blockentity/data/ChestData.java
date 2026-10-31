package org.cloudburstmc.api.blockentity.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.api.inventory.ItemContainer;
import org.cloudburstmc.api.inventory.ItemContainers;
import org.cloudburstmc.api.nbt.NbtContainerSize;
import org.cloudburstmc.api.nbt.NbtTag;
import org.cloudburstmc.api.nbt.NbtTagType;
import org.cloudburstmc.math.vector.Vector3i;

/**
 * Persistent data of a chest: its container plus whether it can pair.
 * The pair position is written by the load/save hooks, not the mapper.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class ChestData extends ContainerData {

    @JsonProperty("Items")
    @NbtTag(value = NbtTagType.LIST, element = NbtTagType.COMPOUND)
    @NbtContainerSize(27)
    private ItemContainer items = ItemContainers.detached(27);

    @JsonProperty("Findable")
    private boolean findable;

    @JsonIgnore
    private Vector3i pairPosition;

    public ChestData() {
    }
}
