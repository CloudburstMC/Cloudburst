package org.cloudburstmc.server.entity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.cloudburstmc.server.nbt.NbtTag;
import org.cloudburstmc.server.nbt.NbtTagType;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class ExperienceOrbData extends EntityData {

    @JsonProperty("Count")
    private int count = 1;

    @JsonProperty("PickupDelay")
    @NbtTag(NbtTagType.SHORT)
    private int pickupDelay;
}
