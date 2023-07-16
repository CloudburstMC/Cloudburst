package org.cloudburstmc.server.blockentity;

import com.google.common.collect.Lists;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.blockentity.BrewingStand;
import org.cloudburstmc.api.container.ContainerListener;
import org.cloudburstmc.api.container.ContainerViewTypes;
import org.cloudburstmc.api.event.inventory.BrewFinishEvent;
import org.cloudburstmc.api.event.inventory.BrewStartEvent;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemType;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.protocol.bedrock.packet.ContainerSetDataPacket;
import org.cloudburstmc.server.container.CloudContainer;
import org.cloudburstmc.server.container.ContainerRecipe;
import org.cloudburstmc.server.crafting.BrewingRecipe;
import org.cloudburstmc.server.item.ItemUtils;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudRecipeRegistry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class BrewingStandBlockEntity extends ContainerBlockEntity implements BrewingStand {

    private static final int SLOT_INGREDIENT = 0;
    private static final int SLOT_FUEL = 4;

    public static final short MAX_COOK_TIME = 400;
    public static final List<ItemType> ingredients = Lists.newArrayList(
            ItemTypes.NETHER_WART, ItemTypes.GOLD_NUGGET, ItemTypes.GHAST_TEAR, ItemTypes.GLOWSTONE_DUST, ItemTypes.REDSTONE, ItemTypes.GUNPOWDER, ItemTypes.MAGMA_CREAM, ItemTypes.BLAZE_POWDER,
            ItemTypes.GOLDEN_CARROT, ItemTypes.SPIDER_EYE, ItemTypes.FERMENTED_SPIDER_EYE, ItemTypes.SPECKLED_MELON, ItemTypes.SUGAR, ItemTypes.FISH, ItemTypes.RABBIT_FOOT, ItemTypes.PUFFERFISH,
            ItemTypes.TURTLE_SHELL_PIECE, ItemTypes.PHANTOM_MEMBRANE, ItemTypes.DRAGON_BREATH
    );
    public short cookTime = MAX_COOK_TIME;
    public short fuelTotal;
    public short fuelAmount;

    public BrewingStandBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position, new CloudContainer(5), ContainerViewTypes.BREWING_STAND);
    }

    @Override
    public void loadAdditionalData(NbtMap tag) {
        super.loadAdditionalData(tag);

        tag.listenForList("Items", NbtType.COMPOUND, tags -> {
            for (NbtMap itemTag : tags) {
                ItemStack item = ItemUtils.deserializeItem(itemTag);
                this.container.setItem(itemTag.getByte("Slot"), item);
            }
        });

        tag.listenForShort("CookTime", this::setCookTime);
        tag.listenForShort("FuelAmount", this::setFuelAmount);
        tag.listenForShort("FuelTotal", this::setFuelTotal);
    }

    @Override
    public void saveAdditionalData(NbtMapBuilder tag) {
        super.saveAdditionalData(tag);

        List<NbtMap> items = new ArrayList<>();
        container.forEachSlot((item, slot) -> items.add(ItemUtils.serializeItem(item, slot)));
        tag.putList("Items", NbtType.COMPOUND, items);
        tag.putShort("CookTime", this.cookTime);
    }

    @Override
    protected void saveClientData(NbtMapBuilder tag) {
        super.saveClientData(tag);

        tag.putShort("FuelAmount", this.fuelAmount);
        tag.putShort("FuelTotal", this.fuelTotal);
    }

    @Override
    public void close() {
        if (!closed) {
            for (ContainerListener listener : new HashSet<>(container.getListeners())) {
                if (listener instanceof CloudPlayer) {
                    ((CloudPlayer) listener).getInventoryManager().closeScreen();
                }
            }
            super.close();
        }
    }

    @Override
    public void onBreak() {
        for (ItemStack content : container.getContents()) {
            this.getLevel().dropItem(this.getPosition(), content);
        }
    }

    @Override
    public boolean isValid() {
        return getBlockState().getType() == BlockTypes.BREWING_STAND;
    }

    protected boolean checkIngredient(ItemStack ingredient) {
        return ingredients.contains(ingredient.getType());
    }

    public short getCookTime() {
        return cookTime;
    }

    public void setCookTime(int cookTime) {
        if (cookTime < MAX_COOK_TIME) {
            this.scheduleUpdate();
        }
        this.cookTime = (short) cookTime;
    }

    public short getFuelTotal() {
        return fuelTotal;
    }

    public void setFuelTotal(short fuelTotal) {
        this.fuelTotal = fuelTotal;
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
        if (this.fuelAmount <= 0 && fuel.getType() == ItemTypes.BLAZE_POWDER && fuel.getCount() > 0) {
            this.fuelAmount = 20;
            this.fuelTotal = 20;

            this.container.decrementCount(SLOT_FUEL);
            this.sendFuel();
        }

        if (this.fuelAmount > 0) {
            for (int i = 1; i <= 3; i++) {
                if (this.getItem(i).getType() == ItemTypes.POTION) {
                    canBrew = true;
                }
            }

            if (this.cookTime <= MAX_COOK_TIME && canBrew && ingredient.getCount() > 0) {
                if (!this.checkIngredient(ingredient)) {
                    canBrew = false;
                }
            } else {
                canBrew = false;
            }
        }

        if (canBrew) {
            if (this.cookTime == MAX_COOK_TIME) {
                this.sendBrewTime();
                BrewStartEvent e = new BrewStartEvent(this);
                this.server.getEventManager().fire(e);

                if (e.isCancelled()) {
                    return false;
                }
            }

            this.cookTime--;

            if (this.cookTime <= 0) { //20 seconds
                BrewFinishEvent e = new BrewFinishEvent(this);
                this.server.getEventManager().fire(e);

                if (!e.isCancelled()) {
                    for (int i = 1; i <= 3; i++) {
                        ItemStack potion = this.container.getItem(i);

                        ContainerRecipe containerRecipe = (ContainerRecipe) CloudRecipeRegistry.get().matchBrewingRecipe(ingredient, potion);
                        if (containerRecipe != null) {
                            ItemStack result = containerRecipe.getResult();
//                            result.setMeta(potion.getMeta()); //TODO: check
                            this.setItem(i, result);
                        } else {
                            BrewingRecipe recipe = (BrewingRecipe) CloudRecipeRegistry.get().matchBrewingRecipe(ingredient, potion);
                            if (recipe != null) {
                                this.container.setItem(i, recipe.getResult());
                            }
                        }
                    }
                    this.getLevel().addLevelSoundEvent(this.getPosition(), SoundEvent.POTION_BREWED);
                    this.container.decrementCount(SLOT_INGREDIENT);

                    this.fuelAmount--;
                    this.sendFuel();
                }

                this.cookTime = MAX_COOK_TIME;
            }

            ret = true;
        } else {
            this.cookTime = MAX_COOK_TIME;
        }

        //this.sendBrewTime();
        lastUpdate = System.currentTimeMillis();

        this.updateBlock();

        return ret;
    }

    public void sendFuel() {
        for (ContainerListener listener : this.container.getListeners()) {
            listener.onInventoryDataChange(this.container, ContainerSetDataPacket.BREWING_STAND_FUEL_AMOUNT, this.fuelAmount);
            listener.onInventoryDataChange(this.container, ContainerSetDataPacket.BREWING_STAND_FUEL_TOTAL, this.fuelTotal);
        }
    }

    protected void sendBrewTime() {
        for (ContainerListener listener : this.container.getListeners()) {
            listener.onInventoryDataChange(this.container, ContainerSetDataPacket.BREWING_STAND_BREW_TIME, this.cookTime);
        }
    }

    public void updateBlock() {
        BlockState blockState = this.getBlockState();

        if (blockState.getType() != BlockTypes.BREWING_STAND) {
            return;
        }

        BlockState state = BlockStates.BREWING_STAND;

        for (int i = 1; i <= 3; ++i) {
            ItemStack potion = this.container.getItem(i);

            var id = potion.getType();
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

    public short getFuelAmount() {
        return fuelAmount;
    }

    public void setFuelAmount(int fuel) {
        this.fuelAmount = (short) fuel;
    }

    @Override
    public ItemStack getIngredient() {
        return this.container.getItem(SLOT_INGREDIENT);
    }

    @Override
    public void setIngredient(ItemStack ingredient) {
        this.container.setItem(SLOT_INGREDIENT, ingredient);
    }

    @Override
    public ItemStack getFuel() {
        return this.container.getItem(SLOT_FUEL);
    }

    @Override
    public void setFuel(ItemStack fuel) {
        this.container.setItem(SLOT_FUEL, fuel);
    }

    @Override
    public boolean isSpawnable() {
        return true;
    }
}
