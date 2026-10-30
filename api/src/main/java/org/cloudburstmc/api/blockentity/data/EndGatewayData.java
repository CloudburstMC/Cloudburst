package org.cloudburstmc.api.blockentity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * Persistent data of an end gateway: its age and exact-teleport flag. The exit
 * portal and the client form of the age travel outside this class.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class EndGatewayData extends BlockEntityData {

    @JsonProperty("Age")
    private long age;

    @JsonProperty("ExactTeleport")
    private boolean exactTeleport;

    public EndGatewayData() {
    }
}
