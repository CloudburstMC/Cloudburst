package org.cloudburstmc.api.entity.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.api.entity.projectile.ArrowPickupStatus;
import org.cloudburstmc.api.nbt.NbtCodec;
import org.cloudburstmc.api.nbt.NbtTag;
import org.cloudburstmc.api.nbt.NbtTagType;
import org.cloudburstmc.api.nbt.codec.ArrowPickupStatusCodec;
import org.cloudburstmc.math.vector.Vector3i;

/**
 * Persistent data of an abstract arrow: projectile data plus its stuck state,
 * pierce level and pickup rule. The embedded block position is conditional: it
 * is only written while the arrow is stuck, so it travels through the load/save
 * hooks.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class AbstractArrowData extends ProjectileData {

    @JsonProperty("inGround")
    private boolean inGround;

    @JsonProperty("PierceLevel")
    @NbtTag(NbtTagType.BYTE)
    private int pierceLevel;

    @JsonIgnore
    private Vector3i embeddedPosition;

    @JsonProperty("pickup")
    @NbtCodec(ArrowPickupStatusCodec.class)
    private ArrowPickupStatus pickup = ArrowPickupStatus.ALLOWED;

    public AbstractArrowData() {
    }
}
