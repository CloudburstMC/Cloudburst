package org.cloudburstmc.api.entity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.api.nbt.NbtTag;
import org.cloudburstmc.api.nbt.NbtTagType;
import org.cloudburstmc.nbt.NbtMap;

import java.util.List;

/**
 * Persistent data of a creature: living data plus equipment.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class CreatureData extends LivingData {

    @JsonProperty("Offhand")
    @NbtTag(value = NbtTagType.LIST, element = NbtTagType.COMPOUND)
    private List<NbtMap> offhand = List.of();

    @JsonProperty("Armor")
    @NbtTag(value = NbtTagType.LIST, element = NbtTagType.COMPOUND)
    private List<NbtMap> armor = List.of();

    public CreatureData() {
    }
}
