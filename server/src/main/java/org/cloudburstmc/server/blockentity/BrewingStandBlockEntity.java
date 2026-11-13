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
import org.cloudburstmc.api.inventory.ItemContainer;
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
import org.cloudburstmc.protocol.bedrock.packet.ContainerSetDataPacket;
import org.cloudburstmc.server.blockentity.data.CloudBrewingStandData;
import org.cloudburstmc.server.container.CloudContainer;
import org.cloudburstmc.server.container.ContainerListener;
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

    private int brewDuration = BREW_TICKS;
    private @Nullable ItemType brewingIngredient;
    private @Nullable Batch batch;
    private boolean inputsChanged = true;
    private boolean inventoryChanged;

    private final CloudBrewingStandData persistenceState;

    public BrewingStandBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        this(type, chunk, position, new BrewingStorage());
    }

    private BrewingStandBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position, BrewingStorage storage) {
        CloudBrewingStandData state = new CloudBrewingStandData();
        super(type, chunk, position, state);
        this.persistenceState = state;
        this.data().setItems(storage);
        storage.owner = this;
    }

    @Override
    protected CloudBrewingStandData data() {
        return this.persistenceState;
    }

    @Override
    protected ItemContainer items() {
        return this.data().getItems();
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
        return this.data().getCookTime() == 0 ? 0 : this.brewDuration - this.data().getCookTime();
    }

    @Override
    public void setBrewProgress(int ticks) {
        if (this.brewingIngredient == null) {
            return;
        }

        this.data().setCookTime(this.brewDuration - Math.clamp(ticks, 0, this.brewDuration - 1));
        this.setDirty();
        this.broadcastBrewTime();
    }

    @Override
    public int getBrewDuration() {
        return this.brewDuration;
    }

    @Override
    public int getFuelLevel() {
        return this.data().getFuelAmount();
    }

    @Override
    public void setFuelLevel(int level) {
        this.data().setFuelAmount(Math.max(0, level));
        this.data().setFuelTotal(Math.max(this.data().getFuelTotal(), this.data().getFuelAmount()));
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
    protected void loadCustom(NbtMap tag) {

        int savedDuration = readNumber(tag, "BrewDuration");
        this.brewDuration = savedDuration > 0 ? savedDuration : BREW_TICKS;
        this.data().setCookTime(Math.clamp(this.data().getCookTime(), 0, this.brewDuration));
        this.data().setFuelAmount(readNumber(tag, "FuelAmount"));
        this.data().setFuelTotal(Math.max(this.data().getFuelAmount(), readNumber(tag, "FuelTotal")));
        this.brewingIngredient = this.data().getCookTime() > 0 ? this.getIngredient().getType() : null;
    }

    @Override
    protected void saveCustom(NbtMapBuilder tag) {
        writeNumber(tag, "CookTime", this.data().getCookTime());
        tag.putInt("BrewDuration", this.brewDuration);
        writeNumber(tag, "FuelAmount", this.data().getFuelAmount());
        writeNumber(tag, "FuelTotal", this.data().getFuelTotal());
    }

    @Override
    protected void saveCustomClient(NbtMapBuilder tag) {
        super.saveCustomClient(tag);
        writeNumber(tag, "FuelAmount", this.data().getFuelAmount());
        writeNumber(tag, "FuelTotal", this.data().getFuelTotal());
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
            for (ContainerListener listener : List.copyOf(this.getContainer().getListeners())) {
                if (listener instanceof CloudPlayer player) {
                    player.closeInventory();
                }
            }
            super.close();
        }
    }

    @Override
    public void onBreak() {
        for (ItemStack content : this.getContainer().getContents()) {
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
    public boolean blocksInsertionFrom(int slot, Direction side, ItemStack item) {
        return switch (side) {
            case UP -> slot != INGREDIENT_SLOT || !this.acceptsItem(slot, item);
            case DOWN -> true;
            default -> slot == INGREDIENT_SLOT || !this.acceptsItem(slot, item);
        };
    }

    @Override
    public boolean blocksExtractionBelow(int slot) {
        return !isBottleSlot(slot) && (slot != INGREDIENT_SLOT || this.getIngredient().getType() != ItemTypes.GLASS_BOTTLE);
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

        if (this.data().getFuelAmount() == 0) {
            this.refuel();
            if (this.isClosed() || this.inputsChanged) {
                return !this.isClosed();
            }
        }

        if (this.data().getCookTime() > 0) {
            if (this.batch == null || this.getIngredient().getType() != this.brewingIngredient) {
                this.resetBrew();
                return this.batch != null && this.data().getFuelAmount() > 0;
            }

            this.data().setCookTime(this.data().getCookTime() - 1);
            this.setDirty();
            if (this.data().getCookTime() == 0) {
                this.finishBrew(this.batch);
                this.resetBrew();
                return true;
            }

            if (this.brewDuration != BREW_TICKS || this.data().getCookTime() % 40 == 0) {
                this.broadcastBrewTime();
            }

            return true;
        }

        if (this.batch != null && this.data().getFuelAmount() > 0) {
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
        if (event.isCancelled() || event.getFuelPower() == 0 || this.isClosed() || !powder.equals(this.getFuel()) || this.data().getFuelAmount() != 0) {
            return;
        }

        this.data().setFuelAmount(event.getFuelPower());
        this.data().setFuelTotal(this.data().getFuelAmount());
        if (event.isConsuming()) {
            this.setFuel(powder.decreaseCount(1));
        }
        this.setDirty();
        this.broadcastFuel();
    }

    private boolean startBrew(Batch batch) {
        BrewingStartEvent event = new BrewingStartEvent(this, batch.ingredient(), BREW_TICKS);
        this.server.getEventManager().fire(event);
        if (this.isBatchStale(batch) || this.data().getFuelAmount() == 0) {
            return !this.isClosed() && this.inputsChanged;
        }

        this.data().setFuelAmount(this.data().getFuelAmount() - 1);
        this.brewDuration = event.getBrewingTime();
        this.data().setCookTime(this.brewDuration);
        this.brewingIngredient = batch.ingredient().getType();
        this.setDirty();
        this.broadcast(ContainerSetDataPacket.BREWING_STAND_FUEL_AMOUNT, this.data().getFuelAmount());
        this.broadcastBrewTime();
        return true;
    }

    private void finishBrew(Batch batch) {
        BrewEvent event = new BrewEvent(this, batch.results(), this.data().getFuelAmount());
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
        this.data().setCookTime(0);
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
        listener.onInventoryDataChange(this.getContainer(), ContainerSetDataPacket.BREWING_STAND_FUEL_TOTAL, this.data().getFuelTotal());
        listener.onInventoryDataChange(this.getContainer(), ContainerSetDataPacket.BREWING_STAND_FUEL_AMOUNT, this.data().getFuelAmount());
        listener.onInventoryDataChange(this.getContainer(), ContainerSetDataPacket.BREWING_STAND_BREW_TIME, this.displayBrewTime());
    }

    private void broadcastFuel() {
        this.broadcast(ContainerSetDataPacket.BREWING_STAND_FUEL_TOTAL, this.data().getFuelTotal());
        this.broadcast(ContainerSetDataPacket.BREWING_STAND_FUEL_AMOUNT, this.data().getFuelAmount());
    }

    private void broadcastBrewTime() {
        this.broadcast(ContainerSetDataPacket.BREWING_STAND_BREW_TIME, this.displayBrewTime());
    }

    private int displayBrewTime() {
        return (int) Math.ceilDiv((long) this.data().getCookTime() * BREW_TICKS, this.brewDuration);
    }

    private void broadcast(int property, int value) {
        for (ContainerListener listener : this.getContainer().getListeners()) {
            listener.onInventoryDataChange(this.getContainer(), property, value);
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
