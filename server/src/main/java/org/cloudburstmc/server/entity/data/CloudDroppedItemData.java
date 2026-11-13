package org.cloudburstmc.server.entity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.nbt.NbtTagType;
import org.cloudburstmc.server.nbt.annotation.NbtTag;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class CloudDroppedItemData extends CloudEntityData {

    @JsonProperty("PickupDelay")
    @NbtTag(NbtTagType.SHORT)
    private int pickupDelay;

    @JsonProperty("Item")
    private ItemStack item = ItemStack.EMPTY;
}
