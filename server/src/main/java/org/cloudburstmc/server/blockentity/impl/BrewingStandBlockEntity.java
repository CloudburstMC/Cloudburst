package org.cloudburstmc.server.blockentity.impl;

import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import com.nukkitx.nbt.NbtType;
import com.nukkitx.protocol.bedrock.data.SoundEvent;
import com.nukkitx.protocol.bedrock.packet.ContainerSetDataPacket;
import org.cloudburstmc.server.Server;
import org.cloudburstmc.server.block.BlockIds;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.block.behavior.BlockBehaviorBrewingStand;
import org.cloudburstmc.server.blockentity.BlockEntityType;
import org.cloudburstmc.server.blockentity.BrewingStand;
import org.cloudburstmc.server.event.inventory.BrewEvent;
import org.cloudburstmc.server.event.inventory.StartBrewEvent;
import org.cloudburstmc.server.inventory.BrewingInventory;
import org.cloudburstmc.server.inventory.BrewingRecipe;
import org.cloudburstmc.server.inventory.ContainerRecipe;
import org.cloudburstmc.server.item.ItemIds;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.ItemUtils;
import org.cloudburstmc.server.level.chunk.Chunk;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.Identifier;

import java.util.*;

import static org.cloudburstmc.server.block.BlockIds.REDSTONE_WIRE;

public class BrewingStandBlockEntity extends BaseBlockEntity implements BrewingStand {

    public static final short MAX_COOK_TIME = 400;
    public static final List<Identifier> ingredients = new ArrayList<>(Arrays.asList(
            ItemIds.NETHER_WART, ItemIds.GOLD_NUGGET, ItemIds.GHAST_TEAR, ItemIds.GLOWSTONE_DUST, REDSTONE_WIRE, ItemIds.GUNPOWDER, ItemIds.MAGMA_CREAM, ItemIds.BLAZE_POWDER,
            ItemIds.GOLDEN_CARROT, ItemIds.SPIDER_EYE, ItemIds.FERMENTED_SPIDER_EYE, ItemIds.SPECKLED_MELON, ItemIds.SUGAR, ItemIds.FISH, ItemIds.RABBIT_FOOT, ItemIds.PUFFERFISH,
            ItemIds.TURTLE_SHELL_PIECE, ItemIds.PHANTOM_MEMBRANE, ItemIds.DRAGON_BREATH
    ));
    protected final BrewingInventory inventory = new BrewingInventory(this);
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
            for (Player player : new HashSet<>(getInventory().getViewers())) {
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
        return getBlockState().getType() == BlockIds.BREWING_STAND;
    }

    @Override
    public BrewingInventory getInventory() {
        return inventory;
    }

    protected boolean checkIngredient(ItemStack ingredient) {
        return ingredients.contains(ingredient.getId());
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
        if (this.fuelAmount <= 0 && fuel.getId() == ItemIds.BLAZE_POWDER && fuel.getCount() > 0) {
            fuel.decrementCount();
            this.fuelAmount = 20;
            this.fuelTotal = 20;

            this.inventory.setFuel(fuel);
            this.sendFuel();
        }

        if (this.fuelAmount > 0) {
            for (int i = 1; i <= 3; i++) {
                if (this.inventory.getItem(i).getId() == ItemIds.POTION) {
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
                StartBrewEvent e = new StartBrewEvent(this);
                this.server.getEventManager().fire(e);

                if (e.isCancelled()) {
                    return false;
                }
            }

            this.cookTime--;

            if (this.cookTime <= 0) { //20 seconds
                BrewEvent e = new BrewEvent(this);
                this.server.getEventManager().fire(e);

                if (!e.isCancelled()) {
                    for (int i = 1; i <= 3; i++) {
                        ItemStack potion = this.inventory.getItem(i);

                        ContainerRecipe containerRecipe = Server.getInstance().getCraftingManager().matchContainerRecipe(ingredient, potion);
                        if (containerRecipe != null) {
                            ItemStack result = containerRecipe.getResult();
                            result.setMeta(potion.getMeta());
                            this.inventory.setItem(i, result);
                        } else {
                            BrewingRecipe recipe = Server.getInstance().getCraftingManager().matchBrewingRecipe(ingredient, potion);
                            if (recipe != null) {
                                this.inventory.setItem(i, recipe.getResult());
                            }
                        }
                    }
                    this.getLevel().addLevelSoundEvent(this.getPosition(), SoundEvent.POTION_BREWED);

                    ingredient.decrementCount();
                    this.inventory.setIngredient(ingredient);

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
        for (Player p : this.inventory.getViewers()) {
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
        for (Player p : this.inventory.getViewers()) {
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

        if (!(blockState instanceof BlockBehaviorBrewingStand)) {
            return;
        }

        BlockState state = BlockState.get(BlockIds.BREWING_STAND);

        for (int i = 1; i <= 3; ++i) {
            ItemStack potion = this.inventory.getItem(i);

            Identifier id = potion.getId();
            if ((id == ItemIds.POTION || id == ItemIds.SPLASH_POTION || id == ItemIds.LINGERING_POTION) && potion.getCount() > 0) {
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
            this.getLevel().setBlock(this.getPosition(), state, false, false);
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
        Identifier id = item.getId();

        if (direction.getAxis().isHorizontal()) {
            if (id == ItemIds.BLAZE_POWDER) {
                return new int[]{BrewingInventory.SLOT_FUEL};
            }
        } else {
            if (id == ItemIds.NETHER_WART || id == ItemIds.REDSTONE || id == ItemIds.GLOWSTONE_DUST || id == ItemIds.FERMENTED_SPIDER_EYE || id == ItemIds.GUNPOWDER || id == ItemIds.DRAGON_BREATH) {
                return new int[]{BrewingInventory.SLOT_INGREDIENT};
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
