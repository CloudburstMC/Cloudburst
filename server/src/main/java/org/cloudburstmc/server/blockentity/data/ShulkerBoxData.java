package org.cloudburstmc.server.blockentity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.cloudburstmc.api.inventory.ItemContainer;
import org.cloudburstmc.api.inventory.ItemContainers;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.server.nbt.*;
import org.cloudburstmc.server.nbt.codec.ShulkerFacingCodec;

/**
 * Persistent data of a shulker box: its 27-slot container plus the facing the lid opens towards.
 * The facing is sent to the client so it can render the open lid.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class ShulkerBoxData extends ContainerData {

    @JsonProperty("Items")
    @NbtTag(value = NbtTagType.LIST, element = NbtTagType.COMPOUND)
    @NbtContainerSize(27)
    private ItemContainer items = ItemContainers.detached(27);

    @NbtClient
    @JsonProperty("facing")
    @NbtCodec(ShulkerFacingCodec.class)
    private Direction facing = Direction.UP;
}
