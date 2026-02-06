package org.cloudburstmc.server.container;

import org.cloudburstmc.api.block.BlockIds;
import org.cloudburstmc.api.item.ItemIds;
import org.cloudburstmc.api.util.Identifier;

import java.util.Map;
import java.util.TreeMap;

public abstract class Fuel {
    public static final Map<Identifier, Short> duration = new TreeMap<>();

    static {
        duration.put(ItemIds.COAL, (short) 1600);
        duration.put(BlockIds.COAL_BLOCK, (short) 16000);

        // Logs
        duration.put(BlockIds.ACACIA_LOG, (short) 300);
        duration.put(BlockIds.BIRCH_LOG, (short) 300);
        duration.put(BlockIds.CHERRY_LOG, (short) 300);
        duration.put(BlockIds.CRIMSON_STEM, (short) 300);
        duration.put(BlockIds.DARK_OAK_LOG, (short) 300);
        duration.put(BlockIds.JUNGLE_LOG, (short) 300);
        duration.put(BlockIds.MANGROVE_LOG, (short) 300);
        duration.put(BlockIds.OAK_LOG, (short) 300);
        duration.put(BlockIds.SPRUCE_LOG, (short) 300);
        duration.put(BlockIds.WARPED_STEM, (short) 300);

        // Planks
        duration.put(BlockIds.ACACIA_PLANKS, (short) 300);
        duration.put(BlockIds.BAMBOO_PLANKS, (short) 300);
        duration.put(BlockIds.BIRCH_PLANKS, (short) 300);
        duration.put(BlockIds.CHERRY_PLANKS, (short) 300);
        duration.put(BlockIds.CRIMSON_PLANKS, (short) 300);
        duration.put(BlockIds.DARK_OAK_PLANKS, (short) 300);
        duration.put(BlockIds.JUNGLE_PLANKS, (short) 300);
        duration.put(BlockIds.MANGROVE_PLANKS, (short) 300);
        duration.put(BlockIds.OAK_PLANKS, (short) 300);
        duration.put(BlockIds.SPRUCE_PLANKS, (short) 300);
        duration.put(BlockIds.WARPED_PLANKS, (short) 300);

        // Saplings
        duration.put(BlockIds.ACACIA_SAPLING, (short) 100);
        duration.put(BlockIds.BIRCH_SAPLING, (short) 100);
        duration.put(BlockIds.CHERRY_SAPLING, (short) 100);
        duration.put(BlockIds.DARK_OAK_SAPLING, (short) 100);
        duration.put(BlockIds.JUNGLE_SAPLING, (short) 100);
        duration.put(BlockIds.OAK_SAPLING, (short) 100);
        duration.put(BlockIds.SPRUCE_SAPLING, (short) 100);

        // Wooden tools
        duration.put(ItemIds.STICK, (short) 100);
        duration.put(ItemIds.WOODEN_AXE, (short) 200);
        duration.put(ItemIds.WOODEN_HOE, (short) 200);
        duration.put(ItemIds.WOODEN_PICKAXE, (short) 200);
        duration.put(ItemIds.WOODEN_SHOVEL, (short) 200);
        duration.put(ItemIds.WOODEN_SWORD, (short) 200);

        // Fences
        duration.put(BlockIds.ACACIA_FENCE, (short) 300);
        duration.put(BlockIds.BAMBOO_FENCE, (short) 300);
        duration.put(BlockIds.BIRCH_FENCE, (short) 300);
        duration.put(BlockIds.CHERRY_FENCE, (short) 300);
        duration.put(BlockIds.CRIMSON_FENCE, (short) 300);
        duration.put(BlockIds.DARK_OAK_FENCE, (short) 300);
        duration.put(BlockIds.JUNGLE_FENCE, (short) 300);
        duration.put(BlockIds.MANGROVE_FENCE, (short) 300);
        duration.put(BlockIds.OAK_FENCE, (short) 300);
        duration.put(BlockIds.SPRUCE_FENCE, (short) 300);
        duration.put(BlockIds.WARPED_FENCE, (short) 300);

        // Fence gates
        duration.put(BlockIds.ACACIA_FENCE_GATE, (short) 300);
        duration.put(BlockIds.BAMBOO_FENCE_GATE, (short) 300);
        duration.put(BlockIds.BIRCH_FENCE_GATE, (short) 300);
        duration.put(BlockIds.CHERRY_FENCE_GATE, (short) 300);
        duration.put(BlockIds.CRIMSON_FENCE_GATE, (short) 300);
        duration.put(BlockIds.DARK_OAK_FENCE_GATE, (short) 300);
        duration.put(BlockIds.JUNGLE_FENCE_GATE, (short) 300);
        duration.put(BlockIds.MANGROVE_FENCE_GATE, (short) 300);
        duration.put(BlockIds.OAK_FENCE_GATE, (short) 300);
        duration.put(BlockIds.SPRUCE_FENCE_GATE, (short) 300);
        duration.put(BlockIds.WARPED_FENCE_GATE, (short) 300);

        // Stairs
        duration.put(BlockIds.ACACIA_STAIRS, (short) 300);
        duration.put(BlockIds.BAMBOO_MOSAIC_STAIRS, (short) 300);
        duration.put(BlockIds.BAMBOO_STAIRS, (short) 300);
        duration.put(BlockIds.BIRCH_STAIRS, (short) 300);
        duration.put(BlockIds.CHERRY_STAIRS, (short) 300);
        duration.put(BlockIds.CRIMSON_STAIRS, (short) 300);
        duration.put(BlockIds.DARK_OAK_STAIRS, (short) 300);
        duration.put(BlockIds.JUNGLE_STAIRS, (short) 300);
        duration.put(BlockIds.MANGROVE_STAIRS, (short) 300);
        duration.put(BlockIds.OAK_STAIRS, (short) 300);
        duration.put(BlockIds.SPRUCE_STAIRS, (short) 300);
        duration.put(BlockIds.WARPED_STAIRS, (short) 300);

        // Trapdoors
        duration.put(BlockIds.ACACIA_TRAPDOOR, (short) 300);
        duration.put(BlockIds.BAMBOO_TRAPDOOR, (short) 300);
        duration.put(BlockIds.BIRCH_TRAPDOOR, (short) 300);
        duration.put(BlockIds.CHERRY_TRAPDOOR, (short) 300);
        duration.put(BlockIds.CRIMSON_TRAPDOOR, (short) 300);
        duration.put(BlockIds.DARK_OAK_TRAPDOOR, (short) 300);
        duration.put(BlockIds.JUNGLE_TRAPDOOR, (short) 300);
        duration.put(BlockIds.MANGROVE_TRAPDOOR, (short) 300);
        duration.put(BlockIds.OAK_TRAPDOOR, (short) 300);
        duration.put(BlockIds.SPRUCE_TRAPDOOR, (short) 300);
        duration.put(BlockIds.WARPED_TRAPDOOR, (short) 300);

        // Pressure plates
        duration.put(BlockIds.ACACIA_PRESSURE_PLATE, (short) 300);
        duration.put(BlockIds.BAMBOO_PRESSURE_PLATE, (short) 300);
        duration.put(BlockIds.BIRCH_PRESSURE_PLATE, (short) 300);
        duration.put(BlockIds.CHERRY_PRESSURE_PLATE, (short) 300);
        duration.put(BlockIds.CRIMSON_PRESSURE_PLATE, (short) 300);
        duration.put(BlockIds.DARK_OAK_PRESSURE_PLATE, (short) 300);
        duration.put(BlockIds.JUNGLE_PRESSURE_PLATE, (short) 300);
        duration.put(BlockIds.MANGROVE_PRESSURE_PLATE, (short) 300);
        duration.put(BlockIds.OAK_PRESSURE_PLATE, (short) 300);
        duration.put(BlockIds.SPRUCE_PRESSURE_PLATE, (short) 300);
        duration.put(BlockIds.WARPED_PRESSURE_PLATE, (short) 300);

        // Buttons
        duration.put(BlockIds.ACACIA_BUTTON, (short) 100);
        duration.put(BlockIds.BAMBOO_BUTTON, (short) 100);
        duration.put(BlockIds.BIRCH_BUTTON, (short) 100);
        duration.put(BlockIds.CHERRY_BUTTON, (short) 100);
        duration.put(BlockIds.CRIMSON_BUTTON, (short) 100);
        duration.put(BlockIds.DARK_OAK_BUTTON, (short) 100);
        duration.put(BlockIds.JUNGLE_BUTTON, (short) 100);
        duration.put(BlockIds.MANGROVE_BUTTON, (short) 100);
        duration.put(BlockIds.OAK_BUTTON, (short) 100);
        duration.put(BlockIds.SPRUCE_BUTTON, (short) 100);
        duration.put(BlockIds.WARPED_BUTTON, (short) 100);

        // Slabs
        duration.put(BlockIds.ACACIA_SLAB, (short) 300);
        duration.put(BlockIds.BAMBOO_MOSAIC_SLAB, (short) 300);
        duration.put(BlockIds.BAMBOO_SLAB, (short) 300);
        duration.put(BlockIds.BIRCH_SLAB, (short) 300);
        duration.put(BlockIds.CHERRY_SLAB, (short) 300);
        duration.put(BlockIds.CRIMSON_SLAB, (short) 300);
        duration.put(BlockIds.DARK_OAK_SLAB, (short) 300);
        duration.put(BlockIds.JUNGLE_SLAB, (short) 300);
        duration.put(BlockIds.MANGROVE_SLAB, (short) 300);
        duration.put(BlockIds.OAK_SLAB, (short) 300);
        duration.put(BlockIds.SPRUCE_SLAB, (short) 300);
        duration.put(BlockIds.WARPED_SLAB, (short) 300);

        // Double slabs
        duration.put(BlockIds.ACACIA_DOUBLE_SLAB, (short) 300);
        duration.put(BlockIds.BAMBOO_DOUBLE_SLAB, (short) 300);
        duration.put(BlockIds.BAMBOO_MOSAIC_DOUBLE_SLAB, (short) 300);
        duration.put(BlockIds.BIRCH_DOUBLE_SLAB, (short) 300);
        duration.put(BlockIds.CHERRY_DOUBLE_SLAB, (short) 300);
        duration.put(BlockIds.CRIMSON_DOUBLE_SLAB, (short) 300);
        duration.put(BlockIds.DARK_OAK_DOUBLE_SLAB, (short) 300);
        duration.put(BlockIds.JUNGLE_DOUBLE_SLAB, (short) 300);
        duration.put(BlockIds.MANGROVE_DOUBLE_SLAB, (short) 300);
        duration.put(BlockIds.OAK_DOUBLE_SLAB, (short) 300);
        duration.put(BlockIds.SPRUCE_DOUBLE_SLAB, (short) 300);
        duration.put(BlockIds.WARPED_DOUBLE_SLAB, (short) 300);

        // Other wooden items/blocks
        duration.put(BlockIds.BOOKSHELF, (short) 300);
        duration.put(BlockIds.BROWN_MUSHROOM_BLOCK, (short) 300);
        duration.put(BlockIds.CHEST, (short) 300);
        duration.put(BlockIds.CRAFTING_TABLE, (short) 300);
        duration.put(BlockIds.DAYLIGHT_DETECTOR, (short) 300);
        duration.put(BlockIds.DAYLIGHT_DETECTOR_INVERTED, (short) 300);
        duration.put(BlockIds.DRIED_KELP_BLOCK, (short) 4000);
        duration.put(BlockIds.JUKEBOX, (short) 300);
        duration.put(BlockIds.LADDER, (short) 300);
        duration.put(BlockIds.NOTE_BLOCK, (short) 300);
        duration.put(BlockIds.RED_MUSHROOM_BLOCK, (short) 300);
        duration.put(BlockIds.TRAPPED_CHEST, (short) 300);
        duration.put(ItemIds.ACACIA_DOOR, (short) 200);
        duration.put(ItemIds.BANNER, (short) 300);
        duration.put(ItemIds.BIRCH_DOOR, (short) 200);
        duration.put(ItemIds.BLAZE_ROD, (short) 2400);
        duration.put(ItemIds.BOAT, (short) 1200);
        duration.put(ItemIds.BOW, (short) 200);
        duration.put(ItemIds.BOWL, (short) 200);
        duration.put(ItemIds.BUCKET, (short) 20000);
        duration.put(ItemIds.DARK_OAK_DOOR, (short) 200);
        duration.put(ItemIds.FISHING_ROD, (short) 300);
        duration.put(ItemIds.JUNGLE_DOOR, (short) 200);
        duration.put(ItemIds.SPRUCE_DOOR, (short) 200);
        duration.put(ItemIds.WOODEN_DOOR, (short) 200);
    }
}
