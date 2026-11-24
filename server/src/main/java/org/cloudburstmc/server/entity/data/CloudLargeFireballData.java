package org.cloudburstmc.server.entity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.checkerframework.checker.nullness.qual.Nullable;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class CloudLargeFireballData extends CloudProjectileData {

    @JsonProperty("ExplosionYield")
    private @Nullable Float explosionYield;

    @JsonProperty("Incendiary")
    private @Nullable Boolean incendiary;
}
