package org.cloudburstmc.api.entity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.api.nbt.NbtTag;
import org.cloudburstmc.api.nbt.NbtTagType;

/**
 * Persistent data of a hanging entity: everything shared plus its attachment direction.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class HangingData extends EntityData {

    @JsonProperty("Direction")
    @NbtTag(NbtTagType.BYTE)
    private Integer direction;

    public HangingData() {
    }
}
