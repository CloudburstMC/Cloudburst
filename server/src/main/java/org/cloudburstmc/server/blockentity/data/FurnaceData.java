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

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class FurnaceData extends BlockEntityData {

    @JsonProperty("Items")
    @NbtTag(value = NbtTagType.LIST, element = NbtTagType.COMPOUND)
    @NbtContainerSize(3)
    private ItemContainer items = new CloudContainer(3);

    @JsonProperty("CookTime")
    private int cookTime;

    @JsonProperty("BurnTime")
    private int burnTime;

    @JsonProperty("BurnDuration")
    private int burnDuration;
}
