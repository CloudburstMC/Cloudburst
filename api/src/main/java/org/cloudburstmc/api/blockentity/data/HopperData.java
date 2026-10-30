package org.cloudburstmc.api.blockentity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * Persistent data of a hopper: its container plus the pull/push cooldown.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class HopperData extends ContainerData {

    @JsonProperty("TransferCooldown")
    private int transferCooldown = 8;

    public HopperData() {
    }
}
