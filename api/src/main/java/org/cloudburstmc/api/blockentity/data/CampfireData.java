package org.cloudburstmc.api.blockentity.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.nbt.NbtClient;

/**
 * Persistent data of a campfire: the item on each of its four slots plus its cook time.
 * A slot's keys are only written while the slot holds an item.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class CampfireData extends BlockEntityData {

    @JsonIgnore
    private final int[] duration = new int[4];

    @JsonIgnore
    private final boolean[] cookingPaused = new boolean[4];

    @NbtClient
    @JsonProperty("Item1")
    private ItemStack item1;

    @NbtClient
    @JsonProperty("Item2")
    private ItemStack item2;

    @NbtClient
    @JsonProperty("Item3")
    private ItemStack item3;

    @NbtClient
    @JsonProperty("Item4")
    private ItemStack item4;

    @NbtClient
    @JsonProperty("ItemTime1")
    private Integer itemTime1;

    @NbtClient
    @JsonProperty("ItemTime2")
    private Integer itemTime2;

    @NbtClient
    @JsonProperty("ItemTime3")
    private Integer itemTime3;

    @NbtClient
    @JsonProperty("ItemTime4")
    private Integer itemTime4;
}
