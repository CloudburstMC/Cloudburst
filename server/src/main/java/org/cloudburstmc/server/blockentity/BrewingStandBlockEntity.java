package org.cloudburstmc.server.blockentity;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.blockentity.BrewingStand;
import org.cloudburstmc.api.event.block.BrewingStartEvent;
import org.cloudburstmc.api.event.inventory.BrewEvent;
import org.cloudburstmc.api.event.inventory.BrewingStandFuelEvent;
import org.cloudburstmc.api.inventory.view.SlotGroup;
import org.cloudburstmc.api.inventory.view.SlotGroupType;
import org.cloudburstmc.api.inventory.view.SlotGroupTypes;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemType;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.api.level.sound.SoundTypes;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.protocol.bedrock.packet.ContainerSetDataPacket;
import org.cloudburstmc.server.container.CloudContainer;
import org.cloudburstmc.server.container.ContainerListener;
import org.cloudburstmc.server.item.ItemUtils;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudRecipeRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BrewingStandBlockEntity extends ContainerBlockEntity implements BrewingStand {

    public static final int INGREDIENT_SLOT = 0;
    public static final int FIRST_BOTTLE_SLOT = 1;
    public static final int BOTTLE_COUNT = 3;
    public static final int FUEL_SLOT = FIRST_BOTTLE_SLOT + BOTTLE_COUNT;

    private static final int BREW_TICKS = 400;
    private static final int FUEL_USES = 20;

    private int brewTime;
    private int brewDuration = BREW_TICKS;
    private int fuel;
    private int fuelTotal;
    private @Nullable ItemType brewingIngredient;
    private @Nullable Batch batch;
    private boolean inputsChanged = true;
    private boolean inventoryChanged;

    public BrewingStandBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        this(type, chunk, position, new BrewingStorage());
    }

    private BrewingStandBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position, BrewingStorage storage) {
        super(type, chunk, position, storage);
        storage.owner = this;
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
    public ItemStack getIngredient() {
        return this.getItem(INGREDIENT_SLOT);
    }

    @Override
    public void setIngredient(ItemStack ingredient) {
        this.setItem(INGREDIENT_SLOT, ingredient);
    }

    @Override
    public ItemStack getBottle(int slot) {
        return this.getItem(FIRST_BOTTLE_SLOT + Objects.checkIndex(slot, BOTTLE_COUNT));
    }

    @Override
    public void setBottle(int slot, ItemStack item) {
        this.setItem(FIRST_BOTTLE_SLOT + Objects.checkIndex(slot, BOTTLE_COUNT), item);
    }

    @Override
    public ItemStack getFuel() {
        return this.getItem(FUEL_SLOT);
    }

    @Override
    public void setFuel(ItemStack item) {
        this.setItem(FUEL_SLOT, item);
    }

    @Override
    public int getMaxStackSize(int slot) {
        return isBottleSlot(slot) ? 1 : BrewingStand.super.getMaxStackSize(slot);
    }

    @Override
    public int getBrewProgress() {
        return this.brewTime == 0 ? 0 : this.brewDuration - this.brewTime;
    }

    @Override
    public void setBrewProgress(int ticks) {
        if (this.brewingIngredient == null) {
            return;
        }

        this.brewTime = this.brewDuration - Math.clamp(ticks, 0, this.brewDuration - 1);
        this.setDirty();
        this.broadcastBrewTime();
    }

    @Override
    public int getBrewDuration() {
        return this.brewDuration;
    }

    @Override
    public int getFuelLevel() {
        return this.fuel;
    }

    @Override
    public void setFuelLevel(int level) {
        this.fuel = Math.max(0, level);
        this.fuelTotal = Math.max(this.fuelTotal, this.fuel);
        this.setDirty();
        this.scheduleUpdate();
        this.broadcastFuel();
    }

    @Override
    public int getMaxFuelLevel() {
        return FUEL_USES;
    }

    @Override
    public boolean isSpawnable() {
        return true;
    }

    @Override
    public void loadAdditionalData(NbtMap tag) {
        super.loadAdditionalData(tag);
        for (NbtMap item : tag.getList("Items", NbtType.COMPOUND)) {
            int slot = item.getByte("Slot");
            if (slot >= 0 && slot < this.size()) {
                this.setItem(slot, ItemUtils.deserializeItem(item));
            }
        }

        int savedDuration = readNumber(tag, "BrewDuration");
        this.brewDuration = savedDuration > 0 ? savedDuration : BREW_TICKS;
        this.brewTime = Math.clamp(readNumber(tag, "CookTime"), 0, this.brewDuration);
        this.fuel = readNumber(tag, "FuelAmount");
        this.fuelTotal = Math.max(this.fuel, readNumber(tag, "FuelTotal"));
        this.brewingIngredient = this.brewTime > 0 ? this.getIngredient().getType() : null;
    }

    @Override
    public void saveAdditionalData(NbtMapBuilder tag) {
        super.saveAdditionalData(tag);
        List<NbtMap> items = new ArrayList<>();
        this.container.forEachSlot((item, slot) -> items.add(ItemUtils.serializeItem(item, slot)));
        tag.putList("Items", NbtType.COMPOUND, items);
        writeNumber(tag, "CookTime", this.brewTime);
        tag.putInt("BrewDuration", this.brewDuration);
        writeNumber(tag, "FuelAmount", this.fuel);
        writeNumber(tag, "FuelTotal", this.fuelTotal);
    }

    @Override
    protected void saveClientData(NbtMapBuilder tag) {
        super.saveClientData(tag);
        writeNumber(tag, "FuelAmount", this.fuel);
        writeNumber(tag, "FuelTotal", this.fuelTotal);
    }

    @Override
    public void applyPlacementItem(ItemStack item) {
        String name = item.get(ItemDataComponents.CUSTOM_NAME);
        if (name != null && !name.isEmpty()) {
            this.setCustomName(name);
            this.setDirty();
        }
    }

    @Override
    public void close() {
        if (!this.isClosed()) {
            for (ContainerListener listener : List.copyOf(this.container.getListeners())) {
                if (listener instanceof CloudPlayer player) {
                    player.closeInventory();
                }
            }
            super.close();
        }
    }

    @Override
    public void onBreak() {
        for (ItemStack content : this.container.getContents()) {
            this.dropItemOnBreak(content);
        }
    }

    public boolean acceptsItem(int slot, ItemStack item) {
        if (item.isEmpty()) {
            return true;
        }

        return switch (slot) {
            case INGREDIENT_SLOT -> CloudRecipeRegistry.get().isBrewingIngredient(item);
            case FUEL_SLOT -> item.getType() == ItemTypes.BLAZE_POWDER;
            default -> isBottleSlot(slot) && item.getCount() == 1 && isBottle(item);
        };
    }

    @Override
    public boolean canInsertFrom(int slot, Direction side, ItemStack item) {
        return switch (side) {
            case UP -> slot == INGREDIENT_SLOT && this.acceptsItem(slot, item);
            case DOWN -> false;
            default -> slot != INGREDIENT_SLOT && this.acceptsItem(slot, item);
        };
    }

    @Override
    public boolean canExtractBelow(int slot) {
        return isBottleSlot(slot) || slot == INGREDIENT_SLOT && this.getIngredient().getType() == ItemTypes.GLASS_BOTTLE;
    }

    @Override
    public boolean onUpdate() {
        if (this.isClosed() || !this.isValid()) {
            return false;
        }
        super.onUpdate();

        if (this.inputsChanged) {
            this.batch = this.findBatch();
            this.inputsChanged = false;
            this.updateBottleState();
        }

        if (this.inventoryChanged) {
            this.inventoryChanged = false;
            this.getLevel().updateComparatorOutputLevel(this.getPosition());
        }

        if (this.fuel == 0) {
            this.refuel();
            if (this.isClosed() || this.inputsChanged) {
                return !this.isClosed();
            }
        }

        if (this.brewTime > 0) {
            if (this.batch == null || this.getIngredient().getType() != this.brewingIngredient) {
                this.resetBrew();
                return this.batch != null && this.fuel > 0;
            }

            this.brewTime--;
            this.setDirty();
            if (this.brewTime == 0) {
                this.finishBrew(this.batch);
                this.resetBrew();
                return true;
            }

            if (this.brewDuration != BREW_TICKS || this.brewTime % 40 == 0) {
                this.broadcastBrewTime();
            }

            return true;
        }

        if (this.batch != null && this.fuel > 0) {
            return this.startBrew(this.batch);
        }

        return false;
    }

    private void refuel() {
        ItemStack powder = this.getFuel();
        if (powder.isEmpty() || powder.getType() != ItemTypes.BLAZE_POWDER) {
            return;
        }

        BrewingStandFuelEvent event = new BrewingStandFuelEvent(this, powder, FUEL_USES);
        this.server.getEventManager().fire(event);
        if (event.isCancelled() || event.getFuelPower() == 0 || this.isClosed() || !powder.equals(this.getFuel()) || this.fuel != 0) {
            return;
        }

        this.fuel = event.getFuelPower();
        this.fuelTotal = this.fuel;
        if (event.isConsuming()) {
            this.setFuel(powder.decreaseCount(1));
        }
        this.setDirty();
        this.broadcastFuel();
    }

    private boolean startBrew(Batch batch) {
        BrewingStartEvent event = new BrewingStartEvent(this, batch.ingredient(), BREW_TICKS);
        this.server.getEventManager().fire(event);
        if (this.isBatchStale(batch) || this.fuel == 0) {
            return !this.isClosed() && this.inputsChanged;
        }

        this.fuel--;
        this.brewDuration = event.getBrewingTime();
        this.brewTime = this.brewDuration;
        this.brewingIngredient = batch.ingredient().getType();
        this.setDirty();
        this.broadcast(ContainerSetDataPacket.BREWING_STAND_FUEL_AMOUNT, this.fuel);
        this.broadcastBrewTime();
        return true;
    }

    private void finishBrew(Batch batch) {
        BrewEvent event = new BrewEvent(this, batch.results(), this.fuel);
        this.server.getEventManager().fire(event);
        if (event.isCancelled() || this.isBatchStale(batch)) {
            return;
        }

        List<ItemStack> results = event.getResults();
        for (int i = 0; i < BOTTLE_COUNT; i++) {
            this.setBottle(i, i < results.size() ? Objects.requireNonNullElse(results.get(i), ItemStack.EMPTY) : ItemStack.EMPTY);
        }

        ItemStack ingredient = batch.ingredient();
        ItemStack remaining = ingredient.decreaseCount(1);
        if (ingredient.getType() == ItemTypes.DRAGON_BREATH) {
            ItemStack bottle = ItemStack.from(ItemTypes.GLASS_BOTTLE);
            if (remaining.isEmpty()) {
                remaining = bottle;
            } else {
                this.dropItemOnBreak(bottle);
            }
        }

        this.setIngredient(remaining);
        this.getLevel().playSound(this.getPosition(), SoundTypes.RANDOM_POTION_BREWED);
    }

    private void resetBrew() {
        this.brewTime = 0;
        this.brewDuration = BREW_TICKS;
        this.brewingIngredient = null;
        this.setDirty();
        this.broadcastBrewTime();
    }

    private @Nullable Batch findBatch() {
        ItemStack ingredient = this.getIngredient();
        if (ingredient.isEmpty()) {
            return null;
        }

        List<ItemStack> bottles = List.of(this.getBottle(0), this.getBottle(1), this.getBottle(2));
        List<ItemStack> results = new ArrayList<>(BOTTLE_COUNT);
        CloudRecipeRegistry recipes = CloudRecipeRegistry.get();
        boolean changed = false;
        for (ItemStack bottle : bottles) {
            ItemStack result = recipes.brewPotion(ingredient, bottle);
            results.add(result);
            changed |= !result.equals(bottle);
        }
        return changed ? new Batch(ingredient, bottles, results) : null;
    }

    private boolean isBatchStale(Batch batch) {
        if (this.isClosed() || !this.isValid() || !batch.ingredient().equals(this.getIngredient())) {
            return true;
        }

        for (int i = 0; i < BOTTLE_COUNT; i++) {
            if (!batch.bottles().get(i).equals(this.getBottle(i))) {
                return true;
            }
        }

        return false;
    }

    private void updateBottleState() {
        BlockState state = this.getBlockState();
        if (state.getType() != BlockTypes.BREWING_STAND) {
            return;
        }

        BlockState updated = state
                .withTrait(BlockTraits.IS_BREWING_A, !this.getBottle(0).isEmpty())
                .withTrait(BlockTraits.IS_BREWING_B, !this.getBottle(1).isEmpty())
                .withTrait(BlockTraits.IS_BREWING_C, !this.getBottle(2).isEmpty());
        if (!updated.equals(state)) {
            this.getLevel().setBlockState(this.getPosition(), updated, false, false);
        }
    }

    public void sendProgress(ContainerListener listener) {
        listener.onInventoryDataChange(this.container, ContainerSetDataPacket.BREWING_STAND_FUEL_TOTAL, this.fuelTotal);
        listener.onInventoryDataChange(this.container, ContainerSetDataPacket.BREWING_STAND_FUEL_AMOUNT, this.fuel);
        listener.onInventoryDataChange(this.container, ContainerSetDataPacket.BREWING_STAND_BREW_TIME, this.displayBrewTime());
    }

    private void broadcastFuel() {
        this.broadcast(ContainerSetDataPacket.BREWING_STAND_FUEL_TOTAL, this.fuelTotal);
        this.broadcast(ContainerSetDataPacket.BREWING_STAND_FUEL_AMOUNT, this.fuel);
    }

    private void broadcastBrewTime() {
        this.broadcast(ContainerSetDataPacket.BREWING_STAND_BREW_TIME, this.displayBrewTime());
    }

    private int displayBrewTime() {
        return (int) Math.ceilDiv((long) this.brewTime * BREW_TICKS, this.brewDuration);
    }

    private void broadcast(int property, int value) {
        for (ContainerListener listener : this.container.getListeners()) {
            listener.onInventoryDataChange(this.container, property, value);
        }
    }

    private static boolean isBottleSlot(int slot) {
        return slot >= FIRST_BOTTLE_SLOT && slot < FUEL_SLOT;
    }

    private static boolean isBottle(ItemStack item) {
        ItemType type = item.getType();
        return type == ItemTypes.GLASS_BOTTLE || type == ItemTypes.POTION
                || type == ItemTypes.SPLASH_POTION || type == ItemTypes.LINGERING_POTION;
    }

    private static int readNumber(NbtMap tag, String key) {
        return tag.get(key) instanceof Number value ? Math.max(0, value.intValue()) : 0;
    }

    private static void writeNumber(NbtMapBuilder tag, String key, int value) {
        if (value <= Short.MAX_VALUE) {
            tag.putShort(key, (short) value);
        } else {
            tag.putInt(key, value);
        }
    }

    private record Batch(ItemStack ingredient, List<ItemStack> bottles, List<ItemStack> results) {
    }

    private static class BrewingStorage extends CloudContainer {

        private @Nullable BrewingStandBlockEntity owner;

        private BrewingStorage() {
            super(FUEL_SLOT + 1);
        }

        @Override
        protected void onSlotChange(int index, ItemStack old) {
            if (this.owner != null && !this.owner.isClosed()) {
                if (index != FUEL_SLOT) {
                    this.owner.inputsChanged = true;
                }
                this.owner.inventoryChanged = true;
                this.owner.setDirty();
                this.owner.scheduleUpdate();
            }
            super.onSlotChange(index, old);
        }
    }
}
