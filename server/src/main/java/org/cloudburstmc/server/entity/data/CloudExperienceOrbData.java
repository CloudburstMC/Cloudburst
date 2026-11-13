package org.cloudburstmc.server.entity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.cloudburstmc.server.nbt.NbtTagType;
import org.cloudburstmc.server.nbt.annotation.NbtTag;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class CloudExperienceOrbData extends CloudEntityData {

    @JsonProperty("Count")
    private int count = 1;

    @JsonProperty("PickupDelay")
    @NbtTag(NbtTagType.SHORT)
    private int pickupDelay;
}
