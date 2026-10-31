package org.cloudburstmc.api.blockentity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * Persistent data of a skull: its type, rotation and jaw state.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class SkullData extends BlockEntityData {

    @JsonProperty("SkullType")
    private byte skullType;

    @JsonProperty("Rotation")
    private float rotation;

    @JsonProperty("MouthTickCount")
    private int mouthTickCount;

    @JsonProperty("MouthMoving")
    private boolean mouthMoving;

    public SkullData() {
    }
}
