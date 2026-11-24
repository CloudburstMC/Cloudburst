package org.cloudburstmc.server.entity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class CloudHopperMinecartData extends CloudContainerMinecartData {

    @JsonProperty("Enabled")
    private Boolean enabled;
}
