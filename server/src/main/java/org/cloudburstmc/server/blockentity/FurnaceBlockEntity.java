package org.cloudburstmc.server.blockentity;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.blockentity.Furnace;
import org.cloudburstmc.api.event.inventory.FurnaceBurnEvent;
import org.cloudburstmc.api.event.inventory.FurnaceExtractEvent;
import org.cloudburstmc.api.event.inventory.FurnaceSmeltEvent;
import org.cloudburstmc.api.event.inventory.FurnaceStartSmeltEvent;
import org.cloudburstmc.api.inventory.view.BlockFurnaceView;
import org.cloudburstmc.api.inventory.view.SlotGroupType;
import org.cloudburstmc.api.inventory.view.SlotGroupTypes;
import org.cloudburstmc.api.item.ItemBehaviors;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.protocol.bedrock.packet.ContainerSetDataPacket;
import org.cloudburstmc.server.blockentity.data.BlockEntityData;
import org.cloudburstmc.server.blockentity.data.FurnaceData;
import org.cloudburstmc.server.container.CloudContainer;
import org.cloudburstmc.server.container.ContainerListener;
import org.cloudburstmc.server.crafting.CloudCookingRecipe;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudItemRegistry;
import org.cloudburstmc.server.registry.CloudRecipeRegistry;
import org.cloudburstmc.server.registry.VanillaCookingData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FurnaceBlockEntity extends ContainerBlockEntity implements Furnace {

    public static final int INGREDIENT_SLOT = 0;
    public static final int FUEL_SLOT = 1;
    public static final int RESULT_SLOT = 2;

    private static final int AVERAGE_AMBIENT_SOUND_INTERVAL = 60;

    private final Map<Identifier, Integer> recipesUsed = new HashMap<>();
    private int cookDuration;
    private int importedExperience;
    private @Nullable Identifier activeRecipe;
    private @Nullable RecipeLookup recipeLookup;

    public FurnaceBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        this(type, chunk, position, new FurnaceStorage());
    }

    private FurnaceBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position, FurnaceStorage storage) {
        super(type, chunk, position);
        this.data().setItems(storage);
        storage.owner = this;
    }

    @Override
    protected Class<? extends BlockEntityData> minimumDataClass() {
        return FurnaceData.class;
    }

    @Override
    protected FurnaceData data() {
        return (FurnaceData) super.data();
    }

    @Override
    protected org.cloudburstmc.api.inventory.ItemContainer items() {
        return this.data().getItems();
    }

    @Override
    public SlotGroupType<BlockFurnaceView> getSlotGroupType() {
        return SlotGroupTypes.FURNACE;
    }

    @Override
    public Furnace getBlockEntity() {
        return this;
    }

    @Override
    protected void loadCustom(NbtMap tag) {


        this.data().setBurnTime(readTicks(tag, "BurnTime"));
        this.data().setBurnDuration(Math.max(this.data().getBurnTime(), readTicks(tag, "BurnDuration")));
        this.cookDuration = readTicks(tag, "CookDuration");
        this.data().setCookTime(readTicks(tag, "CookTime"));
        String recipe = tag.getString("ActiveRecipe", "");
        this.activeRecipe = recipe.isEmpty() ? null : Identifier.parse(recipe);

        this.importedExperience = readTicks(tag, "StoredXPInt");
        this.recipesUsed.clear();
        tag.getCompound("RecipesUsed").forEach((id, value) -> {
            if (value instanceof Integer count && count > 0) {
                this.recipesUsed.put(Identifier.parse(id), count);
            }
        });
    }

    @Override
    protected void saveCustom(NbtMapBuilder tag) {

        writeTicks(tag, "BurnTime", this.data().getBurnTime());
        writeTicks(tag, "BurnDuration", this.data().getBurnDuration());
        writeTicks(tag, "CookTime", this.data().getCookTime());
        tag.putInt("CookDuration", this.cookDuration);
        tag.putInt("StoredXPInt", this.importedExperience);
        NbtMapBuilder recipes = NbtMap.builder();
        this.recipesUsed.forEach((id, count) -> recipes.putInt(id.toString(), count));
        tag.putCompound("RecipesUsed", recipes.build());
        tag.remove("ActiveRecipe");
        if (this.activeRecipe != null) {
            tag.putString("ActiveRecipe", this.activeRecipe.toString());
        }

    }

    @Override
    protected void saveCustomClient(NbtMapBuilder tag) {
        super.saveCustomClient(tag);
        String name = this.getCustomName();
        if (name != null) {
            tag.putString("CustomName", name);
        }
    }

    @Override
    public boolean isSpawnable() {
        return true;
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
        this.getLevel().dropExpOrb(this.getPosition(), this.takeExperience());
    }

    @Override
    public boolean onUpdate() {
        if (this.isClosed() || !this.isValid()) {
            return false;
        }
        super.onUpdate();

        int previousBurnTime = this.data().getBurnTime();
        int previousBurnDuration = this.data().getBurnDuration();
        int previousCookTime = this.data().getCookTime();
        int previousCookDuration = this.cookDuration;

        if (this.data().getBurnTime() > 0) {
            this.data().setBurnTime(this.data().getBurnTime() - 1);
        }

        this.tickCooking();
        if (this.isClosed() || !this.isValid()) {
            return false;
        }

        this.updateLitState();
        if (this.data().getBurnTime() > 0 && this.getLevel().getRandom().nextInt(AVERAGE_AMBIENT_SOUND_INTERVAL) == 0) {
            this.getLevel().addLevelSoundEvent(this.getPosition(), this.getAmbientSound());
        }

        if (this.data().getBurnTime() != previousBurnTime || this.data().getBurnDuration() != previousBurnDuration || this.data().getCookTime() != previousCookTime || this.cookDuration != previousCookDuration) {
            this.setDirty();
            this.sendProgressChanges(previousBurnTime, previousBurnDuration, this.displayCookTime(previousCookTime, previousCookDuration));
        }

        return this.data().getBurnTime() > 0 || this.data().getCookTime() > 0;
    }

    @Override
    public ItemStack getSmelting() {
        return this.getItem(INGREDIENT_SLOT);
    }

    @Override
    public void setSmelting(ItemStack item) {
        this.setItem(INGREDIENT_SLOT, item);
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
    public ItemStack getResult() {
        return this.getItem(RESULT_SLOT);
    }

    @Override
    public void setResult(ItemStack item) {
        this.setItem(RESULT_SLOT, item);
    }

    @Override
    public int getCookProgress() {
        return this.data().getCookTime();
    }

    @Override
    public void setCookProgress(int ticks) {
        this.cookDuration = this.getCookDuration();
        this.data().setCookTime(Math.clamp(ticks, 0, this.cookDuration));
        this.setDirty();
        this.scheduleUpdate();
        this.broadcastData(ContainerSetDataPacket.FURNACE_TICK_COUNT, this.displayCookTime(this.data().getCookTime(), this.cookDuration));
    }

    @Override
    public int getCookDuration() {
        if (this.cookDuration > 0) {
            return this.cookDuration;
        }
        CloudCookingRecipe recipe = this.findRecipe(this.getSmelting());
        return recipe == null ? this.standardCookTime() : recipe.getCookingTime();
    }

    @Override
    public int getBurnProgress() {
        return this.data().getBurnTime();
    }

    @Override
    public void setBurnProgress(int ticks) {
        this.data().setBurnTime(Math.max(0, ticks));
        if (this.data().getBurnTime() > this.data().getBurnDuration()) {
            this.setBurnDuration(this.data().getBurnTime());
        }
        this.setDirty();
        this.scheduleUpdate();
        this.broadcastData(ContainerSetDataPacket.FURNACE_LIT_TIME, this.data().getBurnTime());
    }

    @Override
    public int getBurnDuration() {
        return this.data().getBurnDuration();
    }

    @Override
    public void setBurnDuration(int ticks) {
        this.data().setBurnDuration(Math.max(this.data().getBurnTime(), ticks));
        this.setDirty();
        this.broadcastData(ContainerSetDataPacket.FURNACE_LIT_DURATION, this.data().getBurnDuration());
    }

    public boolean acceptsFuel(ItemStack item) {
        if (isEmptyBucket(item)) {
            return item.getCount() == 1;
        }
        return item.isEmpty() || fuelTicks(item) > 0;
    }

    @Override
    public boolean canInsertFrom(int slot, Direction side, ItemStack item) {
        return switch (side) {
            case UP -> slot == INGREDIENT_SLOT;
            case DOWN -> false;
            default -> slot == FUEL_SLOT && acceptsFuel(item);
        };
    }

    @Override
    public boolean canExtractBelow(int slot) {
        return switch (slot) {
            case RESULT_SLOT -> true;
            case FUEL_SLOT -> {
                ItemStack fuel = this.getFuel();
                yield isEmptyBucket(fuel) || fuel.getType() == ItemTypes.WATER_BUCKET;
            }
            default -> false;
        };
    }

    public void extract(Player player, ItemStack taken) {
        FurnaceExtractEvent event = new FurnaceExtractEvent(player, this.getBlock(), taken, this.takeExperience());
        this.server.getEventManager().fire(event);
        this.getLevel().dropExpOrb(player.getPosition(), event.getExpToDrop());
    }

    public void sendProgress(ContainerListener listener) {
        listener.onInventoryDataChange(this.getContainer(), ContainerSetDataPacket.FURNACE_LIT_DURATION, this.data().getBurnDuration());
        listener.onInventoryDataChange(this.getContainer(), ContainerSetDataPacket.FURNACE_LIT_TIME, this.data().getBurnTime());
        listener.onInventoryDataChange(this.getContainer(), ContainerSetDataPacket.FURNACE_TICK_COUNT, this.displayCookTime(this.data().getCookTime(), this.cookDuration));
    }

    protected int standardCookTime() {
        return VanillaCookingData.FURNACE_COOK_TICKS;
    }

    protected SoundEvent getAmbientSound() {
        return SoundEvent.FURNACE_USE;
    }

    protected BlockType getUnlitType() {
        return BlockTypes.FURNACE;
    }

    protected BlockType getLitType() {
        return BlockTypes.LIT_FURNACE;
    }

    private void tickCooking() {
        ItemStack ingredient = this.getSmelting();
        CloudCookingRecipe recipe = this.findRecipe(ingredient);
        if (recipe == null || this.isOutputBlocked(recipe.getResult())) {
            this.resetCooking();
            return;
        }

        if (this.activeRecipe != null && !this.activeRecipe.equals(recipe.getId())) {
            this.resetCooking();
        }

        if (this.data().getBurnTime() == 0) {
            this.ignite();
            if (this.isClosed() || !this.isValid()
                    || !ingredient.equals(this.getSmelting()) || this.isOutputBlocked(recipe.getResult())) {
                return;
            }
        }

        if (this.data().getBurnTime() > 0) {
            this.cook(recipe, ingredient);
        } else {
            this.data().setCookTime(Math.max(0, this.data().getCookTime() - 2));
            if (this.data().getCookTime() == 0) {
                this.resetCooking();
            }
        }
    }

    private void ignite() {
        ItemStack fuel = this.getFuel();
        int ticks = (int) ((long) fuelTicks(fuel) * this.standardCookTime() / VanillaCookingData.FURNACE_COOK_TICKS);
        if (ticks <= 0) {
            return;
        }

        FurnaceBurnEvent event = new FurnaceBurnEvent(this.getBlock(), fuel, ticks);
        this.server.getEventManager().fire(event);
        if (event.isCancelled() || !event.isBurning() || event.getBurnTime() <= 0
                || this.isClosed() || !this.isValid()
                || !fuel.equals(this.getFuel())) {
            return;
        }

        this.data().setBurnTime(event.getBurnTime());
        this.data().setBurnDuration(this.data().getBurnTime());
        if (event.willConsumeFuel()) {
            this.setFuel(fuel.getType() == ItemTypes.LAVA_BUCKET ? ItemStack.from(ItemTypes.BUCKET) : fuel.decreaseCount());
        }
    }

    private void cook(CloudCookingRecipe recipe, ItemStack ingredient) {
        if (this.data().getCookTime() == 0) {
            FurnaceStartSmeltEvent event = new FurnaceStartSmeltEvent(this.getBlock(), ingredient.withCount(recipe.getInputCount()), recipe);
            this.server.getEventManager().fire(event);
            if (this.isClosed() || !this.isValid() || !ingredient.equals(this.getSmelting()) || this.isOutputBlocked(recipe.getResult())) {
                return;
            }
            this.cookDuration = event.getTotalCookTime();
        } else if (this.cookDuration == 0) {
            this.cookDuration = recipe.getCookingTime();
        }

        this.activeRecipe = recipe.getId();
        this.data().setCookTime(this.data().getCookTime() + 1);
        if (this.data().getCookTime() >= this.cookDuration) {
            this.finishCooking(recipe, ingredient);
            this.resetCooking();
        }
    }

    private void finishCooking(CloudCookingRecipe recipe, ItemStack ingredient) {
        FurnaceSmeltEvent event = new FurnaceSmeltEvent(this.getBlock(), ingredient.withCount(recipe.getInputCount()), recipe.getResult(), recipe);
        this.server.getEventManager().fire(event);
        if (event.isCancelled() || this.isClosed() || !this.isValid() || !ingredient.equals(this.getSmelting()) || this.isOutputBlocked(event.getResult())) {
            return;
        }

        ItemStack result = event.getResult();
        if (!result.isEmpty()) {
            ItemStack existing = this.getResult();
            this.setResult(existing.isEmpty() ? result : existing.increaseCount(result.getCount()));
        }

        if (ingredient.getType() == ItemTypes.WET_SPONGE && isEmptyBucket(this.getFuel())) {
            this.setFuel(ItemStack.from(ItemTypes.WATER_BUCKET));
        }

        this.setSmelting(ingredient.decreaseCount(recipe.getInputCount()));
        this.recipesUsed.merge(recipe.getId(), 1, Integer::sum);
        this.setDirty();
    }

    private boolean isOutputBlocked(ItemStack result) {
        if (result.isEmpty()) {
            return false;
        }

        ItemStack existing = this.getResult();
        if (!existing.isEmpty() && !existing.isStackableWith(result)) {
            return true;
        }

        int itemLimit = CloudItemRegistry.get()
                .requireComponent(result.getType(), ItemBehaviors.GET_MAX_STACK_SIZE)
                .execute(result);
        int slotLimit = Math.min(itemLimit, this.getContainer().getMaxStackSize());
        return result.getCount() > slotLimit - existing.getCount();
    }

    private void resetCooking() {
        this.data().setCookTime(0);
        this.cookDuration = 0;
        this.activeRecipe = null;
    }

    private @Nullable CloudCookingRecipe findRecipe(ItemStack ingredient) {
        if (this.recipeLookup == null || this.recipeLookup.ingredient() != ingredient) {
            this.recipeLookup = new RecipeLookup(ingredient,
                    CloudRecipeRegistry.get().matchCookingRecipe(ingredient, this.getUnlitType().getId()));
        }
        return this.recipeLookup.recipe();
    }

    private record RecipeLookup(ItemStack ingredient, @Nullable CloudCookingRecipe recipe) {
    }

    private void updateLitState() {
        BlockState current = this.getBlockState();
        BlockType target = this.data().getBurnTime() > 0 ? this.getLitType() : this.getUnlitType();
        if (current.getType() != target) {
            this.getLevel().setBlockState(this.getPosition(), target.getDefaultState().copyTraits(current), true);
        }
    }

    private int takeExperience() {
        if (this.recipesUsed.isEmpty() && this.importedExperience == 0) {
            return 0;
        }

        long experience = this.importedExperience;
        CloudRecipeRegistry registry = CloudRecipeRegistry.get();
        for (Map.Entry<Identifier, Integer> entry : this.recipesUsed.entrySet()) {
            if (registry.getRecipe(entry.getKey()) instanceof CloudCookingRecipe recipe) {
                double reward = (double) entry.getValue() * recipe.getExperience();
                int whole = (int) reward;
                experience += whole;
                if (whole < Integer.MAX_VALUE && reward > whole && this.getLevel().getRandom().nextDouble() < reward - whole) {
                    experience++;
                }
            }
        }

        this.recipesUsed.clear();
        this.importedExperience = 0;
        this.setDirty();
        return (int) Math.min(experience, Integer.MAX_VALUE);
    }

    private int displayCookTime(int progress, int duration) {
        return duration > 0 ? (int) ((long) progress * this.standardCookTime() / duration) : 0;
    }

    private void sendProgressChanges(int burnTime, int burnDuration, int cookTime) {
        if (this.data().getBurnDuration() != burnDuration) {
            this.broadcastData(ContainerSetDataPacket.FURNACE_LIT_DURATION, this.data().getBurnDuration());
        }

        if (this.data().getBurnTime() != burnTime) {
            this.broadcastData(ContainerSetDataPacket.FURNACE_LIT_TIME, this.data().getBurnTime());
        }

        int display = this.displayCookTime(this.data().getCookTime(), this.cookDuration);
        if (display != cookTime) {
            this.broadcastData(ContainerSetDataPacket.FURNACE_TICK_COUNT, display);
        }
    }

    private void broadcastData(int property, int value) {
        for (ContainerListener listener : this.getContainer().getListeners()) {
            listener.onInventoryDataChange(this.getContainer(), property, value);
        }
    }

    private static int fuelTicks(ItemStack fuel) {
        return fuel.isEmpty() ? 0 : CloudItemRegistry.get().requireComponent(fuel.getType(), ItemBehaviors.FUEL_DURATION).get();
    }

    private static boolean isEmptyBucket(ItemStack item) {
        return item.getType() == ItemTypes.BUCKET;
    }

    private static int readTicks(NbtMap tag, String name) {
        return tag.get(name) instanceof Number value ? Math.max(0, value.intValue()) : 0;
    }

    private static void writeTicks(NbtMapBuilder tag, String name, int ticks) {
        if (ticks <= Short.MAX_VALUE) {
            tag.putShort(name, (short) ticks);
        } else {
            tag.putInt(name, ticks);
        }
    }

    private static class FurnaceStorage extends CloudContainer {

        private @Nullable FurnaceBlockEntity owner;

        private FurnaceStorage() {
            super(3);
        }

        @Override
        protected void onSlotChange(int index, ItemStack old) {
            if (this.owner != null && !this.owner.isClosed()) {
                this.owner.setDirty();
                this.owner.scheduleUpdate();
            }
            super.onSlotChange(index, old);
        }
    }
}
