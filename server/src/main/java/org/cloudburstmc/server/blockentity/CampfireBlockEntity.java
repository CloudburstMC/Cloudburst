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
import org.cloudburstmc.server.blockentity.data.CloudCampfireData;
import org.cloudburstmc.server.item.ItemStackNbtCodec;
import org.cloudburstmc.server.registry.CloudRecipeRegistry;

import java.util.Objects;

public class CampfireBlockEntity extends BaseBlockEntity implements Campfire {

    private static final int SLOT_COUNT = 4;
    private static final int AVERAGE_CRACKLE_INTERVAL = 60;

    private final CloudCampfireData persistenceState;

    public CampfireBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        CloudCampfireData state = new CloudCampfireData();
        super(type, chunk, position, state);
        this.persistenceState = state;
    }

    @Override
    protected CloudCampfireData data() {
        return this.persistenceState;
    }

    @Override
    protected void loadCustom(NbtMap tag) {

        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            ItemStack item = this.ingredient(slot);
            int remaining = this.remaining(slot);
            this.clearSlot(slot);
            this.data().getCookingPaused()[slot] = tag.getBoolean(pausedKey(slot));
            if (item.isEmpty()) {
                continue;
            }

            CookingRecipe recipe = this.getLevel() == null ? null : this.findRecipe(item);
            int defaultDuration = recipe == null ? Math.max(1, remaining) : recipe.getCookingTime();
            int total = Math.max(1, tag.getInt(durationKey(slot), defaultDuration));
            this.storeIngredient(slot, item);
            this.data().getDuration()[slot] = total;
            this.setProgress(slot, total - Math.min(total, remaining));
        }
    }

    @Override
    protected void saveCustom(NbtMapBuilder tag) {
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            if (!this.ingredient(slot).isEmpty()) {
                tag.putInt(durationKey(slot), this.data().getDuration()[slot]);
                if (this.data().getCookingPaused()[slot]) {
                    tag.putBoolean(pausedKey(slot), true);
                }
                tag.putCompound(itemKey(slot), ItemStackNbtCodec.serializeItem(this.ingredient(slot)));
                tag.putInt(timeKey(slot), this.remaining(slot));
            }
        }
    }

    @Override
    protected void saveCustomClient(NbtMapBuilder tag) {
        super.saveCustomClient(tag);
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            if (!this.ingredient(slot).isEmpty()) {
                tag.putCompound(itemKey(slot), ItemStackNbtCodec.serializeItem(this.ingredient(slot)));
                tag.putInt(timeKey(slot), this.remaining(slot));
            }
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
            if (this.ingredient(slot).isEmpty() || this.data().getCookingPaused()[slot]) {
                continue;
            }

            int previous = this.progress(slot);
            if (!lit) {
                this.setProgress(slot, Math.max(0, previous - 2));
            } else if (previous < this.data().getDuration()[slot]) {
                this.setProgress(slot, this.progress(slot) + 1);
            }
            progressChanged |= this.progress(slot) != previous;

            if (lit && this.progress(slot) == this.data().getDuration()[slot]) {
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
            ItemStack ingredient = this.ingredient(slot);
            this.clearSlot(slot);
            this.dropItemOnBreak(ingredient);
        }
    }

    public void ejectIngredients() {
        boolean changed = false;
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            ItemStack ingredient = this.ingredient(slot);
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
        return this.ingredient(Objects.checkIndex(slot, SLOT_COUNT));
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
        return this.progress(Objects.checkIndex(slot, SLOT_COUNT));
    }

    @Override
    public void setCookProgress(int slot, int ticks) {
        Objects.checkIndex(slot, SLOT_COUNT);
        this.setProgress(slot, Math.clamp(ticks, 0, this.data().getDuration()[slot]));
        this.syncSlotChange();
    }

    @Override
    public int getCookDuration(int slot) {
        return this.data().getDuration()[Objects.checkIndex(slot, SLOT_COUNT)];
    }

    @Override
    public void setCookDuration(int slot, int ticks) {
        Objects.checkIndex(slot, SLOT_COUNT);
        if (ticks < 1) {
            throw new IllegalArgumentException("Cooking time must be positive");
        }

        if (this.ingredient(slot).isEmpty()) {
            throw new IllegalStateException("Cooking slot is empty");
        }

        int progress = this.progress(slot);
        this.data().getDuration()[slot] = ticks;
        this.setProgress(slot, Math.min(progress, ticks));
        this.syncSlotChange();
    }

    @Override
    public boolean isCookingEnabled(int slot) {
        return !this.data().getCookingPaused()[Objects.checkIndex(slot, SLOT_COUNT)];
    }

    @Override
    public void setCookingEnabled(int slot, boolean enabled) {
        Objects.checkIndex(slot, SLOT_COUNT);
        boolean paused = !enabled;
        if (this.data().getCookingPaused()[slot] != paused) {
            this.data().getCookingPaused()[slot] = paused;
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
                || !this.ingredient(slot).isEmpty()
                || (player != null && !player.getInventory().getSelectedItem().equals(item))) {
            return false;
        }

        this.setIngredient(slot, ingredient, event.getTotalCookTime());
        this.syncSlotChange();
        return true;
    }

    private boolean completeCooking(int slot) {
        ItemStack ingredient = this.ingredient(slot);
        CookingRecipe recipe = this.findRecipe(ingredient);
        ItemStack result = ingredient;
        if (recipe != null) {
            BlockCookEvent event = new BlockCookEvent(this.getBlock(), ingredient, recipe.getResult(), recipe);
            this.server.getEventManager().fire(event);
            if (event.isCancelled() || this.isClosed() || !this.isValid() || !this.isLit()
                    || this.ingredient(slot) != ingredient || this.data().getCookingPaused()[slot]
                    || this.progress(slot) < this.data().getDuration()[slot]) {
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
            if (this.ingredient(slot).isEmpty()) {
                return slot;
            }
        }
        return -1;
    }

    private ItemStack ingredient(int slot) {
        ItemStack item = switch (slot) {
            case 0 -> this.data().getItem1();
            case 1 -> this.data().getItem2();
            case 2 -> this.data().getItem3();
            case 3 -> this.data().getItem4();
            default -> throw new IndexOutOfBoundsException(slot);
        };
        return Objects.requireNonNullElse(item, ItemStack.EMPTY);
    }

    private void storeIngredient(int slot, ItemStack item) {
        ItemStack stored = item.isEmpty() ? null : item;
        switch (slot) {
            case 0 -> this.data().setItem1(stored);
            case 1 -> this.data().setItem2(stored);
            case 2 -> this.data().setItem3(stored);
            case 3 -> this.data().setItem4(stored);
            default -> throw new IndexOutOfBoundsException(slot);
        }
    }

    private int remaining(int slot) {
        Integer remaining = switch (slot) {
            case 0 -> this.data().getItemTime1();
            case 1 -> this.data().getItemTime2();
            case 2 -> this.data().getItemTime3();
            case 3 -> this.data().getItemTime4();
            default -> throw new IndexOutOfBoundsException(slot);
        };
        return Math.max(0, Objects.requireNonNullElse(remaining, 0));
    }

    private int progress(int slot) {
        return this.data().getDuration()[slot] - this.remaining(slot);
    }

    private void setProgress(int slot, int progress) {
        Integer remaining = this.ingredient(slot).isEmpty() ? null : this.data().getDuration()[slot] - progress;
        switch (slot) {
            case 0 -> this.data().setItemTime1(remaining);
            case 1 -> this.data().setItemTime2(remaining);
            case 2 -> this.data().setItemTime3(remaining);
            case 3 -> this.data().setItemTime4(remaining);
            default -> throw new IndexOutOfBoundsException(slot);
        }
    }

    private void clearSlot(int slot) {
        this.setIngredient(slot, ItemStack.EMPTY, 0);
    }

    private void setIngredient(int slot, ItemStack item, int ticks) {
        this.storeIngredient(slot, item);
        this.data().getDuration()[slot] = ticks;
        this.setProgress(slot, 0);
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
