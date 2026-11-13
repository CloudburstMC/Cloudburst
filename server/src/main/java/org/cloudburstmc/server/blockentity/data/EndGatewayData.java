package org.cloudburstmc.server.blockentity.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.cloudburstmc.math.vector.Vector3i;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class EndGatewayData extends BlockEntityData {

    @JsonProperty("Age")
    private long age;

    @JsonProperty("ExactTeleport")
    private boolean exactTeleport;

    @JsonIgnore
    private Vector3i exitPortal;
}
