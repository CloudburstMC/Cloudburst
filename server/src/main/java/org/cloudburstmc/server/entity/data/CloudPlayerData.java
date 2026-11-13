package org.cloudburstmc.server.entity.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.cloudburstmc.api.inventory.ItemContainer;
import org.cloudburstmc.server.nbt.*;

/**
 * Player inventories are loaded in place without clearing unlisted slots.
 * The player's login skin is not replaced by saved data.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties("Skin")
@NoArgsConstructor
public class CloudPlayerData extends CloudHumanData {

    @JsonProperty("EXP")
    private int exp;

    @JsonProperty("expLevel")
    private int expLevel;

    @JsonProperty("enchantmentSeed")
    private int enchantmentSeed;

    @JsonProperty("seenCredits")
    private boolean seenCredits;

    @JsonProperty("Inventory")
    @NbtTag(value = NbtTagType.LIST, element = NbtTagType.COMPOUND)
    @NbtContainerSize(36)
    @NbtSkipEmpty
    @NbtKeepUnlisted
    private ItemContainer inventory;

    @JsonProperty("EnderChestInventory")
    @NbtTag(value = NbtTagType.LIST, element = NbtTagType.COMPOUND)
    @NbtContainerSize(27)
    @NbtSkipEmpty
    @NbtKeepUnlisted
    private ItemContainer enderChest;
}
