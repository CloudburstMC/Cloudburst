package org.cloudburstmc.api.blockentity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.api.inventory.ItemContainer;
import org.cloudburstmc.api.inventory.ItemContainers;
import org.cloudburstmc.api.nbt.NbtClient;
import org.cloudburstmc.api.nbt.NbtCodec;
import org.cloudburstmc.api.nbt.NbtContainerSize;
import org.cloudburstmc.api.nbt.NbtTag;
import org.cloudburstmc.api.nbt.NbtTagType;
import org.cloudburstmc.api.nbt.codec.ShulkerFacingCodec;
import org.cloudburstmc.api.util.Direction;

/**
 * Persistent data of a shulker box: its 27-slot container plus the facing the lid opens towards.
 * The facing is sent to the client so it can render the open lid.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class ShulkerBoxData extends ContainerData {

    @JsonProperty("Items")
    @NbtTag(value = NbtTagType.LIST, element = NbtTagType.COMPOUND)
    @NbtContainerSize(27)
    private ItemContainer items = ItemContainers.detached(27);

    @NbtClient
    @JsonProperty("facing")
    @NbtCodec(ShulkerFacingCodec.class)
    private Direction facing = Direction.UP;

    public ShulkerBoxData() {
    }
}
