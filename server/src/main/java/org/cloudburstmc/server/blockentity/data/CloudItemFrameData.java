package org.cloudburstmc.server.blockentity.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.nbt.NbtClient;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class CloudItemFrameData extends CloudBlockEntityData {

    @NbtClient
    @JsonProperty("Item")
    private ItemStack item;

    @JsonProperty("Fixed")
    private boolean fixed;

    @JsonIgnore
    private int itemRotation;

    @JsonIgnore
    private float itemDropChance = 1.0f;
}
