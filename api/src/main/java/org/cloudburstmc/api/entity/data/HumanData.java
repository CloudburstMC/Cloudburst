package org.cloudburstmc.api.entity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.nbt.NbtMap;

/**
 * Persistent data of a human: everything shared plus the skin compound.
 * <p>
 * The inventory and legacy name tag alias are load-only keys preserved through
 * the raw-tag merge; they are not properties here.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class HumanData extends CreatureData {

    @JsonProperty("Skin")
    private NbtMap skin;

    public HumanData() {
    }
}
