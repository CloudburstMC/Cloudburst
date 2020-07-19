package org.cloudburstmc.server.inventory;

import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.item.ItemIds;
import org.cloudburstmc.server.utils.Identifier;

import java.util.Map;
import java.util.TreeMap;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public abstract class Fuel {
    public static final Map<Identifier, Short> duration = new TreeMap<>();

    static {
        duration.put(ItemIds.COAL, (short) 1600);
        duration.put(BlockTypes.COAL_BLOCK, (short) 16000);
        duration.put(BlockTypes.LOG, (short) 300);
        duration.put(BlockTypes.PLANKS, (short) 300);
        duration.put(BlockTypes.SAPLING, (short) 100);
        duration.put(ItemIds.WOODEN_AXE, (short) 200);
        duration.put(ItemIds.WOODEN_PICKAXE, (short) 200);
        duration.put(ItemIds.WOODEN_SWORD, (short) 200);
        duration.put(ItemIds.WOODEN_SHOVEL, (short) 200);
        duration.put(ItemIds.WOODEN_HOE, (short) 200);
        duration.put(ItemIds.STICK, (short) 100);
        duration.put(BlockTypes.FENCE, (short) 300);
        duration.put(BlockTypes.FENCE_GATE, (short) 300);
        duration.put(BlockTypes.SPRUCE_FENCE_GATE, (short) 300);
        duration.put(BlockTypes.BIRCH_FENCE_GATE, (short) 300);
        duration.put(BlockTypes.JUNGLE_FENCE_GATE, (short) 300);
        duration.put(BlockTypes.ACACIA_FENCE_GATE, (short) 300);
        duration.put(BlockTypes.DARK_OAK_FENCE_GATE, (short) 300);
        duration.put(BlockTypes.OAK_STAIRS, (short) 300);
        duration.put(BlockTypes.SPRUCE_STAIRS, (short) 300);
        duration.put(BlockTypes.BIRCH_STAIRS, (short) 300);
        duration.put(BlockTypes.JUNGLE_STAIRS, (short) 300);
        duration.put(BlockTypes.TRAPDOOR, (short) 300);
        duration.put(BlockTypes.CRAFTING_TABLE, (short) 300);
        duration.put(BlockTypes.BOOKSHELF, (short) 300);
        duration.put(BlockTypes.CHEST, (short) 300);
        duration.put(ItemIds.BUCKET, (short) 20000);
        duration.put(BlockTypes.LADDER, (short) 300);
        duration.put(ItemIds.BOW, (short) 200);
        duration.put(ItemIds.BOWL, (short) 200);
        duration.put(BlockTypes.LOG2, (short) 300);
        duration.put(BlockTypes.DRIED_KELP_BLOCK, (short) 4000);
        duration.put(BlockTypes.WOODEN_PRESSURE_PLATE, (short) 300);
        duration.put(BlockTypes.ACACIA_STAIRS, (short) 300);
        duration.put(BlockTypes.DARK_OAK_STAIRS, (short) 300);
        duration.put(BlockTypes.TRAPPED_CHEST, (short) 300);
        duration.put(BlockTypes.DAYLIGHT_DETECTOR, (short) 300);
        duration.put(BlockTypes.DAYLIGHT_DETECTOR_INVERTED, (short) 300);
        duration.put(BlockTypes.JUKEBOX, (short) 300);
        duration.put(BlockTypes.NOTEBLOCK, (short) 300);
        duration.put(BlockTypes.WOODEN_SLAB, (short) 300);
        duration.put(BlockTypes.DOUBLE_WOODEN_SLAB, (short) 300);
        duration.put(ItemIds.BOAT, (short) 1200);
        duration.put(ItemIds.BLAZE_ROD, (short) 2400);
        duration.put(BlockTypes.BROWN_MUSHROOM_BLOCK, (short) 300);
        duration.put(BlockTypes.RED_MUSHROOM_BLOCK, (short) 300);
        duration.put(ItemIds.FISHING_ROD, (short) 300);
        duration.put(BlockTypes.WOODEN_BUTTON, (short) 100);
        duration.put(ItemIds.WOODEN_DOOR, (short) 200);
        duration.put(ItemIds.SPRUCE_DOOR, (short) 200);
        duration.put(ItemIds.BIRCH_DOOR, (short) 200);
        duration.put(ItemIds.JUNGLE_DOOR, (short) 200);
        duration.put(ItemIds.ACACIA_DOOR, (short) 200);
        duration.put(ItemIds.DARK_OAK_DOOR, (short) 200);
        duration.put(ItemIds.BANNER, (short) 300);
    }
}
