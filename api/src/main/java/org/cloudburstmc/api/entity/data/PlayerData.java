package org.cloudburstmc.api.entity.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.api.inventory.ItemContainer;
import org.cloudburstmc.api.nbt.NbtContainerSize;
import org.cloudburstmc.api.nbt.NbtKeepUnlisted;
import org.cloudburstmc.api.nbt.NbtSkipEmpty;
import org.cloudburstmc.api.nbt.NbtTag;
import org.cloudburstmc.api.nbt.NbtTagType;

/**
 * Persistent data of a player: human data plus experience, credits and the
 * player's live containers.
 * <p>
 * The inventory and ender chest are the player's live containers; a read fills them
 * in place without clearing unlisted slots, and a write stores only non-empty slots,
 * each with its {@code Slot} byte, exactly as players are stored today.
 * <p>
 * Food, play timestamps, the saved level and spawn, and the game type live in the
 * runtime holders (the food state and the server player-data); the player save and
 * load hooks read and write those keys directly, so they are not properties here.
 * <p>
 * A player's skin comes from the login handshake, never from the stored tag, so the
 * inherited skin is ignored here: player loads leave it alone and the player save
 * hook writes the live skin itself.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties("Skin")
public class PlayerData extends HumanData {

    @JsonProperty("EXP")
    private int exp;

    @JsonProperty("expLevel")
    private int expLevel;

    @JsonProperty("seenCredits")
    // A primitive on purpose: every player save writes the flag, including a
    // fresh player's false, exactly as players are stored today.
    private boolean seenCredits;

    // The live player containers, wired by the player itself; null until then.
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

    public PlayerData() {
    }
}
