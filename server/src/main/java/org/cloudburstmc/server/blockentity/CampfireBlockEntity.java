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
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.server.registry.CloudRecipeRegistry;

public class CampfireBlockEntity extends BaseBlockEntity implements Campfire {

    public CampfireBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position);
    }

    @Override
    protected Class<? extends BlockEntityData> minimumDataClass() {
        return CampfireData.class;
    }

    @Override
    protected CampfireData data() {
        return (CampfireData) super.data();
    }

    @Override
    protected void loadCustom(NbtMap tag) {
        normalizeSlot(0, data().getItem1(), data().getItemTime1());
        normalizeSlot(1, data().getItem2(), data().getItemTime2());
        normalizeSlot(2, data().getItem3(), data().getItemTime3());
        normalizeSlot(3, data().getItem4(), data().getItemTime4());
    }

    /**
     * Restores the previous load shape for one slot: a time without its item is
     * dropped, and an item without its time cooks from zero, so the save writes
     * exactly the keys it used to.
     */
    private void normalizeSlot(int index, ItemStack item, Integer time) {
        if (item == null) {
            setSlot(index, null, null);
        } else if (time == null) {
            setSlot(index, item, 0);
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

        for (int i = 0; i < 4; i++) {
            ItemStack item = getItemInFire(i);
            if (item != null) {
                int time = getItemTime(i) + 1;
                if (time >= 600) {
                    ItemStack output = CloudRecipeRegistry.get().matchFurnaceRecipe(item, BlockTypes.CAMPFIRE.getId()).getResult();
                    this.getLevel().dropItem(this.getPosition(), output);
                    setSlot(i, null, null);
                    itemChange = true;
                } else {
                    setSlotTime(i, time);
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
        for (int i = 0; i < 4; i++) {
            ItemStack item = getItemInFire(i);
            if (item != null) {
                this.dropItemOnBreak(item);
            }
        }
    }

    public ItemStack getItemInFire(int index) {
        return switch (index) {
            case 0 -> data().getItem1();
            case 1 -> data().getItem2();
            case 2 -> data().getItem3();
            case 3 -> data().getItem4();
            default -> null;
        };
    }

    private int getItemTime(int index) {
        Integer time = switch (index) {
            case 0 -> data().getItemTime1();
            case 1 -> data().getItemTime2();
            case 2 -> data().getItemTime3();
            case 3 -> data().getItemTime4();
            default -> null;
        };
        return time != null ? time : 0;
    }

    private void setSlot(int index, ItemStack item, Integer time) {
        switch (index) {
            case 0 -> {
                data().setItem1(item);
                data().setItemTime1(time);
            }
            case 1 -> {
                data().setItem2(item);
                data().setItemTime2(time);
            }
            case 2 -> {
                data().setItem3(item);
                data().setItemTime3(time);
            }
            case 3 -> {
                data().setItem4(item);
                data().setItemTime4(time);
            }
            default -> {
            }
        }
    }

    private void setSlotTime(int index, int time) {
        setSlot(index, getItemInFire(index), time);
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
        if (index < 0 || index >= 4) return false;
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
        for (int i = 0; i < 4; i++) {
            setSlot(i, null, null);
        }
        spawnToAll();
    }

    @Override
    public boolean isSpawnable() {
        return true;
    }
}
