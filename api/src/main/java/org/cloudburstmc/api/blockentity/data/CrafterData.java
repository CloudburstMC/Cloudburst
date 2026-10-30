package org.cloudburstmc.api.blockentity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.api.nbt.NbtTag;
import org.cloudburstmc.api.nbt.NbtTagType;

import java.util.List;

/**
 * Persistent data of a crafter: its slotted items plus the per-slot disable flags.
 * The crafter is not loot-capable, so it declares its items separately.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class CrafterData extends BlockEntityData {

    @JsonProperty("Items")
    @NbtTag(value = NbtTagType.LIST, element = NbtTagType.COMPOUND)
    private List<SlottedItem> items = List.of();

    @JsonProperty("DisabledSlots")
    private short disabledSlots;

    public CrafterData() {
    }
}
