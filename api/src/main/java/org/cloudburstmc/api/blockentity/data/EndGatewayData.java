package org.cloudburstmc.api.blockentity.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.math.vector.Vector3i;

/**
 * Persistent data of an end gateway: its age and exact-teleport flag. The exit
 * portal and the client form of the age are written by the load/save hooks.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class EndGatewayData extends BlockEntityData {

    @JsonProperty("Age")
    private long age;

    @JsonProperty("ExactTeleport")
    private boolean exactTeleport;

    @JsonIgnore
    private Vector3i exitPortal;

    public EndGatewayData() {
    }
}
