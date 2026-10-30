package org.cloudburstmc.api.entity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.nbt.NbtMap;

/**
 * Persistent data of an ender crystal: everything shared plus its base visibility
 * and beam target, if any.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class EnderCrystalData extends EntityData {

    @JsonProperty("ShowBottom")
    private Boolean showBottom;

    @JsonProperty("BeamTarget")
    private NbtMap beamTarget;

    public EnderCrystalData() {
    }
}
