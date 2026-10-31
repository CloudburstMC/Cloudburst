package org.cloudburstmc.server.blockentity;

import com.google.common.collect.Lists;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.blockentity.BrewingStand;
import org.cloudburstmc.api.blockentity.data.BlockEntityData;
import org.cloudburstmc.api.blockentity.data.BrewingStandData;
import org.cloudburstmc.api.event.inventory.BrewFinishEvent;
import org.cloudburstmc.api.event.inventory.BrewStartEvent;
import org.cloudburstmc.api.inventory.ItemContainer;
import org.cloudburstmc.api.inventory.view.SlotGroup;
import org.cloudburstmc.api.inventory.view.SlotGroupType;
import org.cloudburstmc.api.inventory.view.SlotGroupTypes;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemType;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.protocol.bedrock.packet.ContainerSetDataPacket;
import org.cloudburstmc.server.container.ContainerListener;
import org.cloudburstmc.server.crafting.CloudBrewingRecipe;
import org.cloudburstmc.server.crafting.CloudContainerRecipe;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudRecipeRegistry;

import java.util.HashSet;
import java.util.List;

/**
 * Block entity implementation for a brewing stand. Manages a 5-slot container (3 bottles, 1 ingredient,
 * 1 fuel) and performs tick-based brewing, firing {@link org.cloudburstmc.api.event.inventory.BrewStartEvent}
 * and {@link org.cloudburstmc.api.event.inventory.BrewFinishEvent} at the start and end of each cycle.
 */
public class BrewingStandBlockEntity extends ContainerBlockEntity implements BrewingStand {

    public static final List<ItemType> ingredients = Lists.newArrayList(
            ItemTypes.NETHER_WART, ItemTypes.GOLD_NUGGET, ItemTypes.GHAST_TEAR, ItemTypes.GLOWSTONE_DUST, ItemTypes.REDSTONE, ItemTypes.GUNPOWDER, ItemTypes.MAGMA_CREAM, ItemTypes.BLAZE_POWDER,
            ItemTypes.GOLDEN_CARROT, ItemTypes.SPIDER_EYE, ItemTypes.FERMENTED_SPIDER_EYE, ItemTypes.GLISTERING_MELON_SLICE, ItemTypes.SUGAR, ItemTypes.COD, ItemTypes.RABBIT_FOOT, ItemTypes.PUFFERFISH,
            ItemTypes.TURTLE_SCUTE, ItemTypes.PHANTOM_MEMBRANE, ItemTypes.DRAGON_BREATH
    );
    public static final int MAX_COOK_TIME = 400;

    private static final int SLOT_INGREDIENT = 0;
    private static final int SLOT_FUEL = 4;

    public BrewingStandBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position);
    }

    @Override
    public SlotGroupType<? extends SlotGroup> getSlotGroupType() {
        return SlotGroupTypes.BREWING_STAND;
    }

    @Override
    public BrewingStand getBlockEntity() {
        return this;
    }

    @Override
    protected Class<? extends BlockEntityData> minimumDataClass() {
        return BrewingStandData.class;
    }

    @Override
    protected BrewingStandData data() {
        return (BrewingStandData) super.data();
    }

    @Override
    protected ItemContainer items() {
        return data().getItems();
    }

    @Override
    public void close() {
        if (!closed) {
            for (ContainerListener listener : new HashSet<>(getContainer().getListeners())) {
                if (listener instanceof CloudPlayer) {
                    ((CloudPlayer) listener).closeInventory();
                }
            }
            super.close();
        }
    }

    @Override
    public void onBreak() {
        for (ItemStack content : getContainer().getContents()) {
            this.dropItemOnBreak(content);
        }
    }

    protected boolean checkIngredient(ItemStack ingredient) {
        return ingredients.contains(ingredient.getType());
    }

    public int getCookTime() {
        return data().getCookTime();
    }

    public void setCookTime(int cookTime) {
        if (cookTime < MAX_COOK_TIME) {
            this.scheduleUpdate();
        }
        data().setCookTime(cookTime);
    }

    public short getFuelTotal() {
        return data().getFuelTotal();
    }

    public void setFuelTotal(short fuelTotal) {
        data().setFuelTotal(fuelTotal);
    }

    @Override
    public boolean onUpdate() {
        if (closed) {
            return false;
        }

        boolean ret = false;

        ItemStack ingredient = this.getIngredient();
        boolean canBrew = false;

        ItemStack fuel = this.getFuel();
        if (data().getFuelAmount() <= 0 && fuel.getType() == ItemTypes.BLAZE_POWDER && fuel.getCount() > 0) {
            data().setFuelAmount(20);
            data().setFuelTotal((short) 20);

            getContainer().decrementCount(SLOT_FUEL);
            this.sendFuel();
        }

        if (data().getFuelAmount() > 0) {
            for (int i = 1; i <= 3; i++) {
                if (this.getItem(i).getType() == ItemTypes.POTION) {
                    canBrew = true;
                }
            }

            if (data().getCookTime() <= MAX_COOK_TIME && canBrew && ingredient.getCount() > 0) {
                if (!this.checkIngredient(ingredient)) {
                    canBrew = false;
                }
            } else {
                canBrew = false;
            }
        }

        if (canBrew) {
            if (data().getCookTime() == MAX_COOK_TIME) {
                this.sendBrewTime();
                BrewStartEvent e = new BrewStartEvent(this);
                this.server.getEventManager().fire(e);

                if (e.isCancelled()) {
                    return false;
                }
            }

            data().setCookTime(data().getCookTime() - 1);

            if (data().getCookTime() <= 0) { //20 seconds
                ItemStack[] results = new ItemStack[3];
                for (int i = 1; i <= 3; i++) {
                    ItemStack potion = getContainer().getItem(i);
                    ItemStack brewed = null;

                    CloudContainerRecipe containerRecipe = (CloudContainerRecipe) CloudRecipeRegistry.get().matchBrewingRecipe(ingredient, potion);
                    if (containerRecipe != null) {
                        brewed = containerRecipe.getResult();
                    } else {
                        CloudBrewingRecipe brewingRecipe = (CloudBrewingRecipe) CloudRecipeRegistry.get().matchBrewingRecipe(ingredient, potion);
                        if (brewingRecipe != null) {
                            brewed = brewingRecipe.getResult();
                        }
                    }

                    results[i - 1] = brewed != null ? brewed : potion;
                }

                BrewFinishEvent e = new BrewFinishEvent(this, results);
                this.server.getEventManager().fire(e);

                if (!e.isCancelled()) {
                    for (int i = 1; i <= 3; i++) {
                        getContainer().setItem(i, e.getPotion(i - 1));
                    }
                    this.getLevel().addLevelSoundEvent(this.getPosition(), SoundEvent.POTION_BREWED);
                    getContainer().decrementCount(SLOT_INGREDIENT);

                    data().setFuelAmount(data().getFuelAmount() - 1);
                    this.sendFuel();
                }

                data().setCookTime(MAX_COOK_TIME);
            }

            ret = true;
        } else {
            data().setCookTime(MAX_COOK_TIME);
        }

        //this.sendBrewTime();
        lastUpdate = System.currentTimeMillis();

        this.updateBlock();

        return ret;
    }

    public void sendFuel() {
        for (ContainerListener listener : getContainer().getListeners()) {
            listener.onInventoryDataChange(getContainer(), ContainerSetDataPacket.BREWING_STAND_FUEL_AMOUNT, data().getFuelAmount());
            listener.onInventoryDataChange(getContainer(), ContainerSetDataPacket.BREWING_STAND_FUEL_TOTAL, data().getFuelTotal());
        }
    }

    protected void sendBrewTime() {
        for (ContainerListener listener : getContainer().getListeners()) {
            listener.onInventoryDataChange(getContainer(), ContainerSetDataPacket.BREWING_STAND_BREW_TIME, data().getCookTime());
        }
    }

    public void updateBlock() {
        BlockState blockState = this.getBlockState();

        if (blockState.getType() != BlockTypes.BREWING_STAND) {
            return;
        }

        BlockState state = BlockStates.BREWING_STAND;

        for (int i = 1; i <= 3; ++i) {
            ItemStack potion = getContainer().getItem(i);

            ItemType id = potion.getType();
            if ((id == ItemTypes.POTION || id == ItemTypes.SPLASH_POTION || id == ItemTypes.LINGERING_POTION) && potion.getCount() > 0) {
                switch (i) {
                    case 1:
                        state = state.withTrait(BlockTraits.IS_BREWING_A, true);
                        break;
                    case 2:
                        state = state.withTrait(BlockTraits.IS_BREWING_B, true);
                        break;
                    case 3:
                        state = state.withTrait(BlockTraits.IS_BREWING_C, true);
                        break;
                }
            }
        }

        if (blockState != state) {
            this.getLevel().setBlockState(this.getPosition(), state, false, false);
        }
    }

    public int getFuelAmount() {
        return data().getFuelAmount();
    }

    public void setFuelAmount(int fuel) {
        data().setFuelAmount(fuel);
    }

    @Override
    public ItemStack getIngredient() {
        return items().getItem(SLOT_INGREDIENT);
    }

    @Override
    public void setIngredient(ItemStack ingredient) {
        items().setItem(SLOT_INGREDIENT, ingredient);
    }

    @Override
    public ItemStack getBottle(int slot) {
        if (slot < 0 || slot > 2) {
            throw new IndexOutOfBoundsException("Bottle slot must be 0-2, got: " + slot);
        }
        return items().getItem(slot + 1);
    }

    @Override
    public void setBottle(int slot, ItemStack item) {
        if (slot < 0 || slot > 2) {
            throw new IndexOutOfBoundsException("Bottle slot must be 0-2, got: " + slot);
        }
        items().setItem(slot + 1, item);
    }

    @Override
    public ItemStack getFuel() {
        return items().getItem(SLOT_FUEL);
    }

    @Override
    public void setFuel(ItemStack fuel) {
        items().setItem(SLOT_FUEL, fuel);
    }

    @Override
    public boolean isSpawnable() {
        return true;
    }

    @Override
    public int getBrewProgress() {
        return MAX_COOK_TIME - data().getCookTime();
    }

    @Override
    public void setBrewProgress(int ticks) {
        int clamped = Math.max(0, Math.min(ticks, MAX_COOK_TIME));
        this.setCookTime(MAX_COOK_TIME - clamped);
        sendBrewTime();
    }

    @Override
    public int getBrewDuration() {
        return MAX_COOK_TIME;
    }

    @Override
    public int getFuelLevel() {
        return data().getFuelAmount();
    }

    @Override
    public void setFuelLevel(int level) {
        data().setFuelAmount(Math.max(0, level));
        sendFuel();
    }

    @Override
    public int getMaxFuelLevel() {
        return 20;
    }
}
