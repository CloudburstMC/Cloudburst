package org.cloudburstmc.api.entity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.api.inventory.ItemContainer;
import org.cloudburstmc.api.inventory.ItemContainers;
import org.cloudburstmc.api.nbt.NbtContainerSize;
import org.cloudburstmc.api.nbt.NbtTag;
import org.cloudburstmc.api.nbt.NbtTagType;

/**
 * Persistent data of a creature: living data plus equipment. The armor and
 * offhand containers are the live ones the runtime views wrap; a read fills
 * them in place and a write stores today's {@code Armor}/{@code Offhand} list
 * shapes.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class CreatureData extends LivingData {

    @JsonProperty("Offhand")
    @NbtTag(value = NbtTagType.LIST, element = NbtTagType.COMPOUND)
    @NbtContainerSize(1)
    private ItemContainer offhand = ItemContainers.detached(1);

    @JsonProperty("Armor")
    @NbtTag(value = NbtTagType.LIST, element = NbtTagType.COMPOUND)
    @NbtContainerSize(4)
    private ItemContainer armor = ItemContainers.detached(4);

    public CreatureData() {
    }
}
