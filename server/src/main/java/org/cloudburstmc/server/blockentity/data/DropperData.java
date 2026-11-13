package org.cloudburstmc.server.blockentity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.cloudburstmc.api.inventory.ItemContainer;
import org.cloudburstmc.server.container.CloudContainer;
import org.cloudburstmc.server.nbt.NbtContainerSize;
import org.cloudburstmc.server.nbt.NbtTag;
import org.cloudburstmc.server.nbt.NbtTagType;

/**
 * Persistent data of a dropper: exactly its 9-slot container.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class DropperData extends ContainerData {

    @JsonProperty("Items")
    @NbtTag(value = NbtTagType.LIST, element = NbtTagType.COMPOUND)
    @NbtContainerSize(9)
    private ItemContainer items = new CloudContainer(9);
}
