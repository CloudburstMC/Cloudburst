package org.cloudburstmc.server.blockentity.impl;

import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import lombok.val;
import org.cloudburstmc.server.block.BlockIds;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.blockentity.BlockEntityType;
import org.cloudburstmc.server.blockentity.Campfire;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.ItemUtils;
import org.cloudburstmc.server.item.behavior.ItemEdibleBehavior;
import org.cloudburstmc.server.level.chunk.Chunk;

/**
 * @author Sleepybear
 */
public class CampfireBlockEntity extends BaseBlockEntity implements Campfire {

    private static final String[] ITEM_TAGS = {"Item1", "Item2", "Item3", "Item4"};
    private static final String[] TIME_TAGS = {"ItemTime1", "ItemTime2", "ItemTime3", "ItemTime4"};

    private final ItemStack[] items = new ItemStack[4];
    private final int[] itemTimes = new int[4];

    public CampfireBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position);
    }

    @Override
    public void loadAdditionalData(NbtMap tag) {
        super.loadAdditionalData(tag);

        boolean hasItems = false;
        for (int i = 0; i < 4; i++) {
            if (tag.containsKey(ITEM_TAGS[i])) {
                ItemStack item = ItemUtils.deserializeItem(tag.getCompound(ITEM_TAGS[i]));
                items[i] = item;
                hasItems = true;
            } else {
                items[i] = null;
            }

            if (tag.containsKey(TIME_TAGS[i])) {
                itemTimes[i] = tag.getInt(TIME_TAGS[i]);
            } else {
                itemTimes[i] = 0;
            }
        }
        if (hasItems) this.scheduleUpdate();
    }

    @Override
    protected void saveClientData(NbtMapBuilder tag) {
        super.saveClientData(tag);

        for (int i = 0; i < 4; i++) {
            ItemStack item = this.items[i];
            if (item != null && !item.isNull()) {
                tag.putCompound(ITEM_TAGS[i], ItemUtils.serializeItem(item));
                tag.putInt(TIME_TAGS[i], this.itemTimes[i]);
            }
        }
    }

    @Override
    public boolean isValid() {
        return getBlockState().getType() == BlockTypes.CAMPFIRE;
    }

    @Override
    public boolean onUpdate() {
        if (this.closed) {
            return false;
        }

        val state = getBlockState();

        if (state.getTrait(BlockTraits.IS_EXTINGUISHED) == null) {
            this.close();
            return false;
        }

        if (state.ensureTrait(BlockTraits.IS_EXTINGUISHED)) {
            return false;
        }

        boolean haveUpdate = false;
        boolean itemChange = false;
        for (int i = 0; i < items.length; i++) {
            if (items[i] != null) {
                if (++itemTimes[i] >= 600) {
                    ItemStack output = getLevel().getServer().getCraftingManager().matchFurnaceRecipe(items[i], BlockIds.CAMPFIRE).getResult();
                    this.getLevel().dropItem(this.getPosition(), output);
                    items[i] = null;
                    itemTimes[i] = 0;
                    itemChange = true;
                }
                haveUpdate = true;
            }
        }

        this.lastUpdate = System.currentTimeMillis();
        if (itemChange) spawnToAll();
        return haveUpdate;
    }

    @Override
    public void onBreak() {
        for (ItemStack item : items) {
            if (item != null) {
                this.getLevel().dropItem(this.getPosition(), item);
            }
        }
    }

    public ItemStack getItemInFire(int index) {
        if (index < 0 || index >= items.length) {
            return null;
        }
        return items[index];
    }

    public boolean putItemInFire(ItemStack item) {
        if (!(item instanceof ItemEdibleBehavior)) return false;

        if (this.getLevel().getServer().getCraftingManager().matchFurnaceRecipe(item, BlockIds.CAMPFIRE) != null) {
            for (int i = 0; i < items.length; i++) {
                if (items[i] == null) {
                    items[i] = item.withAmount(1);
                    itemTimes[i] = 0;
                    this.spawnToAll();
                    this.scheduleUpdate();
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean putItemInFire(ItemStack item, int index, boolean overwrite) {
        if (index < 0 || index >= items.length) return false;
        if (!(item instanceof ItemEdibleBehavior)) return false;

        item = item.withAmount(1);

        boolean addedFood = false;
        if (items[index] == null) {
            items[index] = item;
            addedFood = true;
        } else if (overwrite) {
            items[index] = item;
            itemTimes[index] = 0;
            addedFood = true;
        }
        if (addedFood) {
            spawnToAll();
            this.scheduleUpdate();
        }
        return addedFood;
    }

    /**
     * Method is used to clear all of the items cooking in the fire, without dropping any into the world.
     */
    public void clearItems() {
        for (int i = 0; i < items.length; i++) {
            items[i] = null;
            itemTimes[i] = 0;
        }
        spawnToAll();
    }

    @Override
    public boolean isSpawnable() {
        return true;
    }
}
