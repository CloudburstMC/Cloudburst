package org.cloudburstmc.server.blockentity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class CloudSkullData extends CloudBlockEntityData {

    @JsonProperty("SkullType")
    private byte skullType;

    @JsonProperty("Rotation")
    private float rotation;

    @JsonProperty("MouthTickCount")
    private int mouthTickCount;

    @JsonProperty("MouthMoving")
    private boolean mouthMoving;
}
