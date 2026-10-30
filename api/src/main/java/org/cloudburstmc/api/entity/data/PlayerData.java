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
 * Persistent data of a player: human data plus experience, food, credits,
 * inventory, ender chest and the player-data keys.
 * <p>
 * The inventory and ender chest are the player's live containers; a read fills them
 * in place without clearing unlisted slots, and a write stores only non-empty slots,
 * each with its {@code Slot} byte, exactly as players are stored today.
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

    @JsonProperty("foodLevel")
    private int foodLevel = 20;

    @JsonProperty("foodSaturationLevel")
    private float foodSaturation = 5.0f;

    @JsonProperty("foodExhaustionLevel")
    private float foodExhaustion;

    @JsonProperty("foodTickTimer")
    private int foodTickTimer;

    @JsonProperty("seenCredits")
    private Boolean seenCredits;

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

    @JsonProperty("firstPlayed")
    private Long firstPlayed;

    @JsonProperty("lastPlayed")
    private Long lastPlayed;

    @JsonProperty("Level")
    private String level;

    @JsonProperty("SpawnLevel")
    private String spawnLevel;

    @JsonProperty("SpawnX")
    private Integer spawnX;

    @JsonProperty("SpawnY")
    private Integer spawnY;

    @JsonProperty("SpawnZ")
    private Integer spawnZ;

    @JsonProperty("SpawnYaw")
    private Float spawnYaw;

    @JsonProperty("playerGameType")
    private Integer playerGameType;

    public PlayerData() {
    }
}
