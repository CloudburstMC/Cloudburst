package org.cloudburstmc.server.entity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.checkerframework.checker.nullness.qual.Nullable;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class CloudPrimedTntData extends CloudEntityData {

    @JsonProperty("Fuse")
    private int fuseTicks = 80;

    @JsonProperty("ExplosionYield")
    private @Nullable Float explosionYield;

    @JsonProperty("Incendiary")
    private @Nullable Boolean incendiary;
}
