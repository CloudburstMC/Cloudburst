package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.blockentity.Campfire;
import org.cloudburstmc.api.blockentity.data.BlockEntityData;
import org.cloudburstmc.api.blockentity.data.CampfireData;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.registry.CloudRecipeRegistry;

public class CampfireBlockEntity extends BaseBlockEntity implements Campfire {

    private final ItemStack[] items = new ItemStack[4];
    private final int[] itemTimes = new int[4];

    public CampfireBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position);
    }

    @Override
    protected void applyData(BlockEntityData data) {
        super.applyData(data);
        CampfireData campfire = (CampfireData) data;
        ItemStack[] loaded = {campfire.getItem1(), campfire.getItem2(), campfire.getItem3(), campfire.getItem4()};
        Integer[] times = {campfire.getItemTime1(), campfire.getItemTime2(), campfire.getItemTime3(), campfire.getItemTime4()};

        boolean hasItems = false;
        for (int i = 0; i < 4; i++) {
            if (loaded[i] != null) {
                this.items[i] = loaded[i];
                hasItems = true;
            } else {
                this.items[i] = null;
            }
            this.itemTimes[i] = times[i] != null ? times[i] : 0;
        }
        if (hasItems) this.scheduleUpdate();
    }

    @Override
    protected void fillData(BlockEntityData data) {
        super.fillData(data);
        CampfireData campfire = (CampfireData) data;
        if (this.items[0] != null) {
            campfire.setItem1(this.items[0]);
            campfire.setItemTime1(this.itemTimes[0]);
        }
        if (this.items[1] != null) {
            campfire.setItem2(this.items[1]);
            campfire.setItemTime2(this.itemTimes[1]);
        }
        if (this.items[2] != null) {
            campfire.setItem3(this.items[2]);
            campfire.setItemTime3(this.itemTimes[2]);
        }
        if (this.items[3] != null) {
            campfire.setItem4(this.items[3]);
            campfire.setItemTime4(this.itemTimes[3]);
        }
    }

    @Override
    public boolean onUpdate() {
        if (this.closed) {
            return false;
        }

        BlockState state = getBlockState();

/*
        if (state.getTrait(BlockTraits.IS_EXTINGUISHED) == null) {
            this.close();
            return false;
        }
*/

        if (state.ensureTrait(BlockTraits.IS_EXTINGUISHED)) {
            return false;
        }

        boolean haveUpdate = false;
        boolean itemChange = false;

        for (int i = 0; i < items.length; i++) {
            if (items[i] != null) {
                if (++itemTimes[i] >= 600) {
                    ItemStack output = CloudRecipeRegistry.get().matchFurnaceRecipe(items[i], BlockTypes.CAMPFIRE.getId()).getResult();
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
                this.dropItemOnBreak(item);
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
//        if (!(item.getBehavior() instanceof ItemEdibleBehavior)) return false; //TODO: edible items
//
//        if (CloudRecipeRegistry.get().matchFurnaceRecipe(item, BlockTypes.CAMPFIRE.getId()) != null) {
//            for (int i = 0; i < items.length; i++) {
//                if (items[i] == null) {
//                    items[i] = item.withAmount(1);
//                    itemTimes[i] = 0;
//                    this.spawnToAll();
//                    this.scheduleUpdate();
//                    return true;
//                }
//            }
//        }
        return false;
    }

    @Override
    public boolean putItemInFire(ItemStack item, int index, boolean overwrite) {
        if (index < 0 || index >= items.length) return false;
//        if (!(item.getBehavior() instanceof ItemEdibleBehavior)) return false; //TODO: edible items
//
//        item = item.withAmount(1);
//
//        boolean addedFood = false;
//        if (items[index] == null) {
//            items[index] = item;
//            addedFood = true;
//        } else if (overwrite) {
//            items[index] = item;
//            itemTimes[index] = 0;
//            addedFood = true;
//        }
//        if (addedFood) {
//            spawnToAll();
//            this.scheduleUpdate();
//        }
//        return addedFood;
        return false;
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
