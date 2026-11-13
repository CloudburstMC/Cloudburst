package org.cloudburstmc.server.blockentity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Persistent data of a skull: its type, rotation and jaw state.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class SkullData extends BlockEntityData {

    @JsonProperty("SkullType")
    private byte skullType;

    @JsonProperty("Rotation")
    private float rotation;

    @JsonProperty("MouthTickCount")
    private int mouthTickCount;

    @JsonProperty("MouthMoving")
    private boolean mouthMoving;
}
