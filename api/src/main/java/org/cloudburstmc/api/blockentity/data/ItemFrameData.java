package org.cloudburstmc.api.blockentity.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.nbt.NbtClient;

/**
 * Persistent data of an item frame: the displayed item, if any, plus whether it is fixed.
 * The rotation and drop chance travel outside this class; they are always written,
 * to saved and client tags alike.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class ItemFrameData extends BlockEntityData {

    @NbtClient
    @JsonProperty("Item")
    private ItemStack item;

    @JsonProperty("Fixed")
    private boolean fixed;

    @JsonIgnore
    private int itemRotation;

    @JsonIgnore
    private float itemDropChance = 1.0f;

    public ItemFrameData() {
    }
}
