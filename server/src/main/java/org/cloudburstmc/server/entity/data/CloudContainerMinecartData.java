package org.cloudburstmc.server.entity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.api.inventory.ItemContainer;
import org.cloudburstmc.server.nbt.NbtTagType;
import org.cloudburstmc.server.nbt.annotation.NbtSkipEmpty;
import org.cloudburstmc.server.nbt.annotation.NbtTag;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class CloudContainerMinecartData extends CloudAbstractMinecartData {

    @JsonProperty("Items")
    @NbtTag(value = NbtTagType.LIST, element = NbtTagType.COMPOUND)
    @NbtSkipEmpty
    private ItemContainer items;
}
