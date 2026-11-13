package org.cloudburstmc.server.blockentity.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.cloudburstmc.api.inventory.ItemContainer;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.container.CloudContainer;
import org.cloudburstmc.server.nbt.NbtContainerSize;
import org.cloudburstmc.server.nbt.NbtTag;
import org.cloudburstmc.server.nbt.NbtTagType;

/**
 * Persistent data of a chest: its container plus whether it can pair.
 * The pair position is written by the load/save hooks, not the mapper.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class ChestData extends ContainerData {

    @JsonProperty("Items")
    @NbtTag(value = NbtTagType.LIST, element = NbtTagType.COMPOUND)
    @NbtContainerSize(27)
    private ItemContainer items = new CloudContainer(27);

    @JsonProperty("Findable")
    private boolean findable;

    @JsonIgnore
    private Vector3i pairPosition;
}
