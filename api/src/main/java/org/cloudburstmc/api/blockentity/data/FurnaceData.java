package org.cloudburstmc.api.blockentity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.api.nbt.NbtTag;
import org.cloudburstmc.api.nbt.NbtTagType;

import java.util.List;

/**
 * Persistent data of a furnace: its slotted items plus the cook, burn and burn-duration counters.
 * The furnace is not loot-capable, so it declares its items separately.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class FurnaceData extends BlockEntityData {

    @JsonProperty("Items")
    @NbtTag(value = NbtTagType.LIST, element = NbtTagType.COMPOUND)
    private List<SlottedItem> items = List.of();

    @JsonProperty("CookTime")
    private short cookTime;

    @JsonProperty("BurnTime")
    private short burnTime;

    @JsonProperty("BurnDuration")
    private short burnDuration;

    public FurnaceData() {
    }
}
