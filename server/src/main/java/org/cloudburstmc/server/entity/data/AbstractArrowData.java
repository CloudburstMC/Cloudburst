package org.cloudburstmc.server.entity.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.cloudburstmc.api.entity.projectile.ArrowPickupStatus;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.nbt.NbtCodec;
import org.cloudburstmc.server.nbt.NbtTag;
import org.cloudburstmc.server.nbt.NbtTagType;
import org.cloudburstmc.server.nbt.codec.ArrowPickupStatusCodec;

/**
 * Persistent data of an abstract arrow: projectile data plus its stuck state,
 * pierce level and pickup rule. The embedded block position is conditional: it
 * is only written while the arrow is stuck, so it travels through the load/save
 * hooks.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
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
}
