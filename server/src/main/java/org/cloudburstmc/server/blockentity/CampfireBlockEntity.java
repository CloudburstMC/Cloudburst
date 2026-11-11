package org.cloudburstmc.server.blockentity;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.blockentity.Campfire;
import org.cloudburstmc.api.crafting.CookingRecipe;
import org.cloudburstmc.api.event.block.BlockCookEvent;
import org.cloudburstmc.api.event.block.CampfireStartEvent;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.server.item.ItemUtils;
import org.cloudburstmc.server.registry.CloudRecipeRegistry;

import java.util.Arrays;
import java.util.Objects;

public class CampfireBlockEntity extends BaseBlockEntity implements Campfire {

    private static final int SLOT_COUNT = 4;
    private static final int AVERAGE_CRACKLE_INTERVAL = 60;

    private final ItemStack[] ingredients = new ItemStack[SLOT_COUNT];
    private final int[] progress = new int[SLOT_COUNT];
    private final int[] duration = new int[SLOT_COUNT];
    private final boolean[] cookingPaused = new boolean[SLOT_COUNT];

    public CampfireBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position);
        Arrays.fill(this.ingredients, ItemStack.EMPTY);
    }

    @Override
    public void loadAdditionalData(NbtMap tag) {
        super.loadAdditionalData(tag);

        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            ItemStack item = ItemUtils.deserializeItem(tag.getCompound(itemKey(slot)));
            this.clearSlot(slot);
            this.cookingPaused[slot] = tag.getBoolean(pausedKey(slot));
            if (item.isEmpty()) {
                continue;
            }

            CookingRecipe recipe = this.findRecipe(item);
            int remaining = Math.max(0, tag.getInt(timeKey(slot)));
            int defaultDuration = recipe == null ? Math.max(1, remaining) : recipe.getCookingTime();
            int total = Math.max(1, tag.getInt(durationKey(slot), defaultDuration));
            this.ingredients[slot] = item;
            this.duration[slot] = total;
            this.progress[slot] = total - Math.min(total, remaining);
        }
    }

    @Override
    public void saveAdditionalData(NbtMapBuilder tag) {
        super.saveAdditionalData(tag);
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            tag.putInt(durationKey(slot), this.duration[slot]);
            tag.putBoolean(pausedKey(slot), this.cookingPaused[slot]);
        }
    }

    @Override
    protected void saveClientData(NbtMapBuilder tag) {
        super.saveClientData(tag);
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            tag.putCompound(itemKey(slot), ItemUtils.serializeItem(this.ingredients[slot]));
            tag.putInt(timeKey(slot), this.duration[slot] - this.progress[slot]);
        }
    }

    @Override
    public boolean isSpawnable() {
        return true;
    }

    @Override
    public boolean onUpdate() {
        if (this.isClosed() || !this.isValid()) {
            return false;
        }
        super.onUpdate();

        boolean lit = this.isLit();
        if (lit && this.getLevel().getRandom().nextInt(AVERAGE_CRACKLE_INTERVAL) == 0) {
            this.getLevel().addLevelSoundEvent(this.getPosition(), SoundEvent.CAMPFIRE_CRACKLE);
        }

        boolean progressChanged = false;
        boolean contentsChanged = false;
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            if (this.ingredients[slot].isEmpty() || this.cookingPaused[slot]) {
                continue;
            }

            int previous = this.progress[slot];
            if (!lit) {
                this.progress[slot] = Math.max(0, previous - 2);
            } else if (previous < this.duration[slot]) {
                this.progress[slot]++;
            }
            progressChanged |= this.progress[slot] != previous;

            if (lit && this.progress[slot] == this.duration[slot]) {
                contentsChanged |= this.completeCooking(slot);
                if (this.isClosed() || !this.isValid()) {
                    return false;
                }
                if (!this.isLit()) {
                    break;
                }
            }
        }

        if (progressChanged || contentsChanged) {
            this.setDirty();
        }

        if (contentsChanged) {
            this.spawnToAll();
        }

        return lit || progressChanged;
    }

    @Override
    public void onBreak() {
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            ItemStack ingredient = this.ingredients[slot];
            this.clearSlot(slot);
            this.dropItemOnBreak(ingredient);
        }
    }

    public void ejectIngredients() {
        boolean changed = false;
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            ItemStack ingredient = this.ingredients[slot];
            if (!ingredient.isEmpty()) {
                this.clearSlot(slot);
                this.getLevel().dropItem(this.getPosition().toFloat().add(0.5f, 0.5f, 0.5f), ingredient);
                changed = true;
            }
        }

        if (changed) {
            this.setDirty();
            this.spawnToAll();
        }
    }

    @Override
    public int getSize() {
        return SLOT_COUNT;
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.ingredients[Objects.checkIndex(slot, SLOT_COUNT)];
    }

    @Override
    public void setItem(int slot, ItemStack item) {
        Objects.checkIndex(slot, SLOT_COUNT);
        if (item.isEmpty()) {
            this.clearSlot(slot);
        } else {
            CookingRecipe recipe = this.findRecipe(item);
            if (item.getCount() != 1 || recipe == null) {
                throw new IllegalArgumentException("Expected one campfire ingredient");
            }
            this.setIngredient(slot, item, recipe.getCookingTime());
        }
        this.syncSlotChange();
    }

    @Override
    public int getCookProgress(int slot) {
        return this.progress[Objects.checkIndex(slot, SLOT_COUNT)];
    }

    @Override
    public void setCookProgress(int slot, int ticks) {
        Objects.checkIndex(slot, SLOT_COUNT);
        this.progress[slot] = Math.clamp(ticks, 0, this.duration[slot]);
        this.syncSlotChange();
    }

    @Override
    public int getCookDuration(int slot) {
        return this.duration[Objects.checkIndex(slot, SLOT_COUNT)];
    }

    @Override
    public void setCookDuration(int slot, int ticks) {
        Objects.checkIndex(slot, SLOT_COUNT);
        if (ticks < 1) {
            throw new IllegalArgumentException("Cooking time must be positive");
        }

        if (this.ingredients[slot].isEmpty()) {
            throw new IllegalStateException("Cooking slot is empty");
        }

        this.duration[slot] = ticks;
        this.progress[slot] = Math.min(this.progress[slot], ticks);
        this.syncSlotChange();
    }

    @Override
    public boolean isCookingEnabled(int slot) {
        return !this.cookingPaused[Objects.checkIndex(slot, SLOT_COUNT)];
    }

    @Override
    public void setCookingEnabled(int slot, boolean enabled) {
        Objects.checkIndex(slot, SLOT_COUNT);
        boolean paused = !enabled;
        if (this.cookingPaused[slot] != paused) {
            this.cookingPaused[slot] = paused;
            this.syncSlotChange();
        }
    }

    @Override
    public boolean addItem(ItemStack item) {
        return this.addItem(item, null);
    }

    public boolean addItem(ItemStack item, @Nullable Player player) {
        if (this.isClosed() || !this.isValid() || item.isEmpty() || !this.getBlock().getLiquid().isEmpty()) {
            return false;
        }

        int slot = this.firstEmptySlot();
        if (slot < 0) {
            return false;
        }

        ItemStack ingredient = item.withCount(1);
        CookingRecipe recipe = this.findRecipe(ingredient);
        if (recipe == null) {
            return false;
        }

        CampfireStartEvent event = new CampfireStartEvent(this.getBlock(), ingredient, recipe);
        this.server.getEventManager().fire(event);
        if (this.isClosed() || !this.isValid() || !this.getBlock().getLiquid().isEmpty()
                || !this.ingredients[slot].isEmpty()
                || (player != null && !player.getInventory().getSelectedItem().equals(item))) {
            return false;
        }

        this.setIngredient(slot, ingredient, event.getTotalCookTime());
        this.syncSlotChange();
        return true;
    }

    private boolean completeCooking(int slot) {
        ItemStack ingredient = this.ingredients[slot];
        CookingRecipe recipe = this.findRecipe(ingredient);
        ItemStack result = ingredient;
        if (recipe != null) {
            BlockCookEvent event = new BlockCookEvent(this.getBlock(), ingredient, recipe.getResult(), recipe);
            this.server.getEventManager().fire(event);
            if (event.isCancelled() || this.isClosed() || !this.isValid() || !this.isLit()
                    || this.ingredients[slot] != ingredient || this.cookingPaused[slot]
                    || this.progress[slot] < this.duration[slot]) {
                return false;
            }

            result = event.getResult();
        }

        this.clearSlot(slot);
        if (!result.isEmpty()) {
            this.getLevel().dropItem(this.getPosition().toFloat().add(0.5f, 0.5f, 0.5f), result);
        }

        return true;
    }

    private int firstEmptySlot() {
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            if (this.ingredients[slot].isEmpty()) {
                return slot;
            }
        }
        return -1;
    }

    private void clearSlot(int slot) {
        this.setIngredient(slot, ItemStack.EMPTY, 0);
    }

    private void setIngredient(int slot, ItemStack item, int ticks) {
        this.ingredients[slot] = item;
        this.progress[slot] = 0;
        this.duration[slot] = ticks;
    }

    private void syncSlotChange() {
        this.setDirty();
        this.scheduleUpdate();
        this.spawnToAll();
    }

    private boolean isLit() {
        return !this.getBlockState().ensureTrait(BlockTraits.IS_EXTINGUISHED);
    }

    private @Nullable CookingRecipe findRecipe(ItemStack item) {
        return CloudRecipeRegistry.get().matchCookingRecipe(item, this.getBlockState().getType().getId());
    }

    private static String itemKey(int slot) {
        return "Item" + (slot + 1);
    }

    private static String timeKey(int slot) {
        return "ItemTime" + (slot + 1);
    }

    private static String durationKey(int slot) {
        return "ItemDuration" + (slot + 1);
    }

    private static String pausedKey(int slot) {
        return "ItemPaused" + (slot + 1);
    }
}
