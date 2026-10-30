package org.cloudburstmc.api.entity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.api.nbt.NbtTag;
import org.cloudburstmc.api.nbt.NbtTagType;

/**
 * Persistent data of a sheep: everything shared plus wool color and sheared state.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class SheepData extends CreatureData {

    @JsonProperty("Color")
    @NbtTag(NbtTagType.BYTE)
    private Integer color;

    @JsonProperty("Sheared")
    private Boolean sheared;

    public SheepData() {
    }
}
