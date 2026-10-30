package org.cloudburstmc.api.entity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.api.blockentity.data.SlottedItem;
import org.cloudburstmc.api.nbt.NbtTag;
import org.cloudburstmc.api.nbt.NbtTagType;

import java.util.List;

/**
 * Persistent data of a player: human data plus experience, food, credits,
 * inventory, ender chest and the player-data keys.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
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
    private List<SlottedItem> inventory;

    @JsonProperty("EnderChestInventory")
    @NbtTag(value = NbtTagType.LIST, element = NbtTagType.COMPOUND)
    private List<SlottedItem> enderChest;

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
