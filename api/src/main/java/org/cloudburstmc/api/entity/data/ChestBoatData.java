package org.cloudburstmc.api.entity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.api.blockentity.data.SlottedItem;
import org.cloudburstmc.api.nbt.NbtTag;
import org.cloudburstmc.api.nbt.NbtTagType;

import java.util.List;

/**
 * Persistent data of a chest boat: boat data plus its stored items.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class ChestBoatData extends BoatData {

    @JsonProperty("Items")
    @NbtTag(value = NbtTagType.LIST, element = NbtTagType.COMPOUND)
    private List<SlottedItem> items = List.of();

    public ChestBoatData() {
    }
}
