package org.cloudburstmc.api.blockentity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.api.nbt.NbtClient;

/**
 * Persistent data of a cauldron: the network potion and form ids plus the custom
 * color, if any. The item list and the stale splash flag travel outside this class.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class CauldronData extends BlockEntityData {

    @NbtClient
    @JsonProperty("PotionId")
    private short potionId = -1;

    @NbtClient
    @JsonProperty("PotionType")
    private short potionType = -1;

    @NbtClient
    @JsonProperty("CustomColor")
    private Integer customColor;

    public CauldronData() {
    }
}
