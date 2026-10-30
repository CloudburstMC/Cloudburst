package org.cloudburstmc.api.entity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.api.nbt.NbtCodec;
import org.cloudburstmc.api.nbt.codec.Vector3iCompoundCodec;
import org.cloudburstmc.math.vector.Vector3i;

/**
 * Persistent data of an ender crystal: everything shared plus its base visibility
 * and beam target, if any. The setters keep the client metadata in sync.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class EnderCrystalData extends EntityData {

    @JsonProperty("ShowBottom")
    private boolean showBottom = true;

    @JsonProperty("BeamTarget")
    @NbtCodec(Vector3iCompoundCodec.class)
    private Vector3i beamTarget;

    public EnderCrystalData() {
    }
}
