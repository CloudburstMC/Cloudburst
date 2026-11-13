package org.cloudburstmc.server.entity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.cloudburstmc.api.potion.PotionType;
import org.cloudburstmc.server.nbt.NbtTagType;
import org.cloudburstmc.server.nbt.annotation.NbtTag;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class CloudSplashPotionData extends CloudProjectileData {

    @JsonProperty("PotionId")
    @NbtTag(NbtTagType.SHORT)
    private PotionType potionId;
}
