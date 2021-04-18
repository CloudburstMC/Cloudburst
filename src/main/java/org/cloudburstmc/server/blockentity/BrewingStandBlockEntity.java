package org.cloudburstmc.server.blockentity;

import com.google.common.collect.Lists;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import com.nukkitx.nbt.NbtType;
import com.nukkitx.protocol.bedrock.data.SoundEvent;
import com.nukkitx.protocol.bedrock.packet.ContainerSetDataPacket;
import lombok.val;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.blockentity.BrewingStand;
import org.cloudburstmc.api.event.inventory.BrewFinishEvent;
import org.cloudburstmc.api.event.inventory.BrewStartEvent;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemType;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.server.crafting.BrewingRecipe;
import org.cloudburstmc.server.inventory.CloudBrewingInventory;
import org.cloudburstmc.server.inventory.ContainerRecipe;
import org.cloudburstmc.server.item.ItemUtils;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudRecipeRegistry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class BrewingStandBlockEntity extends BaseBlockEntity implements BrewingStand {

    public static final short MAX_COOK_TIME = 400;
    public static final List<ItemType> ingredients = Lists.newArrayList(
            ItemTypes.NETHER_WART, ItemTypes.GOLD_NUGGET, ItemTypes.GHAST_TEAR, ItemTypes.GLOWSTONE_DUST, ItemTypes.REDSTONE, ItemTypes.GUNPOWDER, ItemTypes.MAGMA_CREAM, ItemTypes.BLAZE_POWDER,
            ItemTypes.GOLDEN_CARROT, ItemTypes.SPIDER_EYE, ItemTypes.FERMENTED_SPIDER_EYE, ItemTypes.SPECKLED_MELON, ItemTypes.SUGAR, ItemTypes.FISH, ItemTypes.RABBIT_FOOT, ItemTypes.PUFFERFISH,
            ItemTypes.TURTLE_SHELL_PIECE, ItemTypes.PHANTOM_MEMBRANE, ItemTypes.DRAGON_BREATH
    );
    protected final CloudBrewingInventory inventory = new CloudBrewingInventory(this);
    public short cookTime = MAX_COOK_TIME;
    public short fuelTotal;
    public short fuelAmount;

    public BrewingStandBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position);
    }

    @Override
    public void loadAdditionalData(NbtMap tag) {
        super.loadAdditionalData(tag);

        tag.listenForList("Items", NbtType.COMPOUND, tags -> {
            for (NbtMap itemTag : tags) {
                ItemStack item = ItemUtils.deserializeItem(itemTag);
                this.inventory.setItem(itemTag.getByte("Slot"), item);
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
        for (Map.Entry<Integer, ItemStack> entry : this.inventory.getContents().entrySet()) {
            items.add(ItemUtils.serializeItem(entry.getValue(), entry.getKey()));
        }
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
            for (CloudPlayer player : new HashSet<>(getInventory().getViewers())) {
                player.removeWindow(getInventory());
            }
            super.close();
        }
    }

    @Override
    public void onBreak() {
        for (ItemStack content : inventory.getContents().values()) {
            this.getLevel().dropItem(this.getPosition(), content);
        }
    }

    @Override
    public boolean isValid() {
        return getBlockState().getType() == BlockTypes.BREWING_STAND;
    }

    @Override
    public CloudBrewingInventory getInventory() {
        return inventory;
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

        ItemStack ingredient = this.inventory.getIngredient();
        boolean canBrew = false;

        ItemStack fuel = this.getInventory().getFuel();
        if (this.fuelAmount <= 0 && fuel.getType() == ItemTypes.BLAZE_POWDER && fuel.getAmount() > 0) {
            this.fuelAmount = 20;
            this.fuelTotal = 20;

            this.inventory.decrementCount(CloudBrewingInventory.SLOT_FUEL);
            this.sendFuel();
        }

        if (this.fuelAmount > 0) {
            for (int i = 1; i <= 3; i++) {
                if (this.inventory.getItem(i).getType() == ItemTypes.POTION) {
                    canBrew = true;
                }
            }

            if (this.cookTime <= MAX_COOK_TIME && canBrew && ingredient.getAmount() > 0) {
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
                        ItemStack potion = this.inventory.getItem(i);

                        ContainerRecipe containerRecipe = (ContainerRecipe) CloudRecipeRegistry.get().matchBrewingRecipe(ingredient, potion);
                        if (containerRecipe != null) {
                            ItemStack result = containerRecipe.getResult();
//                            result.setMeta(potion.getMeta()); //TODO: check
                            this.inventory.setItem(i, result);
                        } else {
                            BrewingRecipe recipe = (BrewingRecipe) CloudRecipeRegistry.get().matchBrewingRecipe(ingredient, potion);
                            if (recipe != null) {
                                this.inventory.setItem(i, recipe.getResult());
                            }
                        }
                    }
                    this.getLevel().addLevelSoundEvent(this.getPosition(), SoundEvent.POTION_BREWED);
                    this.inventory.decrementCount(CloudBrewingInventory.SLOT_INGREDIENT);

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
        for (CloudPlayer p : this.inventory.getViewers()) {
            int windowId = p.getWindowId(this.inventory);
            if (windowId > 0) {
                ContainerSetDataPacket fuelAmountPacket = new ContainerSetDataPacket();
                fuelAmountPacket.setWindowId((byte) windowId);

                fuelAmountPacket.setProperty(ContainerSetDataPacket.BREWING_STAND_FUEL_AMOUNT);
                fuelAmountPacket.setValue(this.fuelAmount);
                p.sendPacket(fuelAmountPacket);

                ContainerSetDataPacket totalFuelPacket = new ContainerSetDataPacket();

                totalFuelPacket.setProperty(ContainerSetDataPacket.BREWING_STAND_FUEL_TOTAL);
                totalFuelPacket.setValue(this.fuelTotal);
                p.sendPacket(totalFuelPacket);
            }
        }
    }

    protected void sendBrewTime() {
        for (CloudPlayer p : this.inventory.getViewers()) {
            int windowId = p.getWindowId(this.inventory);
            if (windowId > 0) {
                ContainerSetDataPacket packet = new ContainerSetDataPacket();
                packet.setProperty(ContainerSetDataPacket.BREWING_STAND_BREW_TIME);
                packet.setWindowId((byte) windowId);
                packet.setValue(this.cookTime);

                p.sendPacket(packet);
            }
        }
    }

    public void updateBlock() {
        BlockState blockState = this.getBlockState();

        if (blockState.getType() != BlockTypes.BREWING_STAND) {
            return;
        }

        BlockState state = BlockStates.BREWING_STAND;

        for (int i = 1; i <= 3; ++i) {
            ItemStack potion = this.inventory.getItem(i);

            val id = potion.getType();
            if ((id == ItemTypes.POTION || id == ItemTypes.SPLASH_POTION || id == ItemTypes.LINGERING_POTION) && potion.getAmount() > 0) {
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
    public int[] getHopperPushSlots(Direction direction, ItemStack item) {
        val id = item.getType();

        if (direction.getAxis().isHorizontal()) {
            if (id == ItemTypes.BLAZE_POWDER) {
                return new int[]{CloudBrewingInventory.SLOT_FUEL};
            }
        } else {
            if (id == ItemTypes.NETHER_WART || id == ItemTypes.REDSTONE || id == ItemTypes.GLOWSTONE_DUST || id == ItemTypes.FERMENTED_SPIDER_EYE || id == ItemTypes.GUNPOWDER || id == ItemTypes.DRAGON_BREATH) {
                return new int[]{CloudBrewingInventory.SLOT_INGREDIENT};
            }
        }

        return null;
    }

    @Override
    public int[] getHopperPullSlots() {
        return new int[]{1, 2, 3};
    }

    @Override
    public boolean isSpawnable() {
        return true;
    }
}
