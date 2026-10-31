package org.cloudburstmc.api.blockentity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.api.potion.EffectType;

/**
 * Persistent data of a beacon: its primary and secondary effects.
 * Either is absent while the beacon has no effect set.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class BeaconData extends BlockEntityData {

    @JsonProperty("primary")
    private EffectType primary;

    @JsonProperty("secondary")
    private EffectType secondary;

    public BeaconData() {
    }
}
