package org.cloudburstmc.server.entity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.cloudburstmc.api.inventory.ItemContainer;
import org.cloudburstmc.server.container.CloudContainer;
import org.cloudburstmc.server.nbt.NbtTagType;
import org.cloudburstmc.server.nbt.annotation.NbtContainerSize;
import org.cloudburstmc.server.nbt.annotation.NbtPositional;
import org.cloudburstmc.server.nbt.annotation.NbtTag;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class CloudCreatureData extends CloudLivingData {

    @JsonProperty("Offhand")
    @NbtTag(value = NbtTagType.LIST, element = NbtTagType.COMPOUND)
    @NbtContainerSize(1)
    @NbtPositional
    private ItemContainer offhand = new CloudContainer(1);

    @JsonProperty("Armor")
    @NbtTag(value = NbtTagType.LIST, element = NbtTagType.COMPOUND)
    @NbtContainerSize(4)
    @NbtPositional
    private ItemContainer armor = new CloudContainer(4);
}
