package org.cloudburstmc.server.blockentity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.cloudburstmc.api.potion.EffectType;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class BeaconData extends BlockEntityData {

    @JsonProperty("primary")
    private EffectType primary;

    @JsonProperty("secondary")
    private EffectType secondary;
}
