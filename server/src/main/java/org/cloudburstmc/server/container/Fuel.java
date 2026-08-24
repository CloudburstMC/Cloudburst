package org.cloudburstmc.server.container;

import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.util.Identifier;

import java.util.Map;
import java.util.TreeMap;

public abstract class Fuel {
    public static final Map<Identifier, Short> duration = new TreeMap<>();

    static {
        duration.put(ItemTypes.COAL.getId(), (short) 1600);
        duration.put(BlockTypes.COAL_BLOCK.getId(), (short) 16000);

        // Logs
        duration.put(BlockTypes.ACACIA_LOG.getId(), (short) 300);
        duration.put(BlockTypes.BIRCH_LOG.getId(), (short) 300);
        duration.put(BlockTypes.CHERRY_LOG.getId(), (short) 300);
        duration.put(BlockTypes.CRIMSON_STEM.getId(), (short) 300);
        duration.put(BlockTypes.DARK_OAK_LOG.getId(), (short) 300);
        duration.put(BlockTypes.JUNGLE_LOG.getId(), (short) 300);
        duration.put(BlockTypes.MANGROVE_LOG.getId(), (short) 300);
        duration.put(BlockTypes.OAK_LOG.getId(), (short) 300);
        duration.put(BlockTypes.SPRUCE_LOG.getId(), (short) 300);
        duration.put(BlockTypes.WARPED_STEM.getId(), (short) 300);

        // Planks
        duration.put(BlockTypes.ACACIA_PLANKS.getId(), (short) 300);
        duration.put(BlockTypes.BAMBOO_PLANKS.getId(), (short) 300);
        duration.put(BlockTypes.BIRCH_PLANKS.getId(), (short) 300);
        duration.put(BlockTypes.CHERRY_PLANKS.getId(), (short) 300);
        duration.put(BlockTypes.CRIMSON_PLANKS.getId(), (short) 300);
        duration.put(BlockTypes.DARK_OAK_PLANKS.getId(), (short) 300);
        duration.put(BlockTypes.JUNGLE_PLANKS.getId(), (short) 300);
        duration.put(BlockTypes.MANGROVE_PLANKS.getId(), (short) 300);
        duration.put(BlockTypes.OAK_PLANKS.getId(), (short) 300);
        duration.put(BlockTypes.SPRUCE_PLANKS.getId(), (short) 300);
        duration.put(BlockTypes.WARPED_PLANKS.getId(), (short) 300);

        // Saplings
        duration.put(BlockTypes.ACACIA_SAPLING.getId(), (short) 100);
        duration.put(BlockTypes.BIRCH_SAPLING.getId(), (short) 100);
        duration.put(BlockTypes.CHERRY_SAPLING.getId(), (short) 100);
        duration.put(BlockTypes.DARK_OAK_SAPLING.getId(), (short) 100);
        duration.put(BlockTypes.JUNGLE_SAPLING.getId(), (short) 100);
        duration.put(BlockTypes.OAK_SAPLING.getId(), (short) 100);
        duration.put(BlockTypes.SPRUCE_SAPLING.getId(), (short) 100);

        // Wooden tools
        duration.put(ItemTypes.STICK.getId(), (short) 100);
        duration.put(ItemTypes.WOODEN_AXE.getId(), (short) 200);
        duration.put(ItemTypes.WOODEN_HOE.getId(), (short) 200);
        duration.put(ItemTypes.WOODEN_PICKAXE.getId(), (short) 200);
        duration.put(ItemTypes.WOODEN_SHOVEL.getId(), (short) 200);
        duration.put(ItemTypes.WOODEN_SWORD.getId(), (short) 200);

        // Fences
        duration.put(BlockTypes.ACACIA_FENCE.getId(), (short) 300);
        duration.put(BlockTypes.BAMBOO_FENCE.getId(), (short) 300);
        duration.put(BlockTypes.BIRCH_FENCE.getId(), (short) 300);
        duration.put(BlockTypes.CHERRY_FENCE.getId(), (short) 300);
        duration.put(BlockTypes.CRIMSON_FENCE.getId(), (short) 300);
        duration.put(BlockTypes.DARK_OAK_FENCE.getId(), (short) 300);
        duration.put(BlockTypes.JUNGLE_FENCE.getId(), (short) 300);
        duration.put(BlockTypes.MANGROVE_FENCE.getId(), (short) 300);
        duration.put(BlockTypes.OAK_FENCE.getId(), (short) 300);
        duration.put(BlockTypes.SPRUCE_FENCE.getId(), (short) 300);
        duration.put(BlockTypes.WARPED_FENCE.getId(), (short) 300);

        // Fence gates
        duration.put(BlockTypes.ACACIA_FENCE_GATE.getId(), (short) 300);
        duration.put(BlockTypes.BAMBOO_FENCE_GATE.getId(), (short) 300);
        duration.put(BlockTypes.BIRCH_FENCE_GATE.getId(), (short) 300);
        duration.put(BlockTypes.CHERRY_FENCE_GATE.getId(), (short) 300);
        duration.put(BlockTypes.CRIMSON_FENCE_GATE.getId(), (short) 300);
        duration.put(BlockTypes.DARK_OAK_FENCE_GATE.getId(), (short) 300);
        duration.put(BlockTypes.JUNGLE_FENCE_GATE.getId(), (short) 300);
        duration.put(BlockTypes.MANGROVE_FENCE_GATE.getId(), (short) 300);
        duration.put(BlockTypes.OAK_FENCE_GATE.getId(), (short) 300);
        duration.put(BlockTypes.SPRUCE_FENCE_GATE.getId(), (short) 300);
        duration.put(BlockTypes.WARPED_FENCE_GATE.getId(), (short) 300);

        // Stairs
        duration.put(BlockTypes.ACACIA_STAIRS.getId(), (short) 300);
        duration.put(BlockTypes.BAMBOO_MOSAIC_STAIRS.getId(), (short) 300);
        duration.put(BlockTypes.BAMBOO_STAIRS.getId(), (short) 300);
        duration.put(BlockTypes.BIRCH_STAIRS.getId(), (short) 300);
        duration.put(BlockTypes.CHERRY_STAIRS.getId(), (short) 300);
        duration.put(BlockTypes.CRIMSON_STAIRS.getId(), (short) 300);
        duration.put(BlockTypes.DARK_OAK_STAIRS.getId(), (short) 300);
        duration.put(BlockTypes.JUNGLE_STAIRS.getId(), (short) 300);
        duration.put(BlockTypes.MANGROVE_STAIRS.getId(), (short) 300);
        duration.put(BlockTypes.OAK_STAIRS.getId(), (short) 300);
        duration.put(BlockTypes.SPRUCE_STAIRS.getId(), (short) 300);
        duration.put(BlockTypes.WARPED_STAIRS.getId(), (short) 300);

        // Trapdoors
        duration.put(BlockTypes.ACACIA_TRAPDOOR.getId(), (short) 300);
        duration.put(BlockTypes.BAMBOO_TRAPDOOR.getId(), (short) 300);
        duration.put(BlockTypes.BIRCH_TRAPDOOR.getId(), (short) 300);
        duration.put(BlockTypes.CHERRY_TRAPDOOR.getId(), (short) 300);
        duration.put(BlockTypes.CRIMSON_TRAPDOOR.getId(), (short) 300);
        duration.put(BlockTypes.DARK_OAK_TRAPDOOR.getId(), (short) 300);
        duration.put(BlockTypes.JUNGLE_TRAPDOOR.getId(), (short) 300);
        duration.put(BlockTypes.MANGROVE_TRAPDOOR.getId(), (short) 300);
        duration.put(BlockTypes.OAK_TRAPDOOR.getId(), (short) 300);
        duration.put(BlockTypes.SPRUCE_TRAPDOOR.getId(), (short) 300);
        duration.put(BlockTypes.WARPED_TRAPDOOR.getId(), (short) 300);

        // Pressure plates
        duration.put(BlockTypes.ACACIA_PRESSURE_PLATE.getId(), (short) 300);
        duration.put(BlockTypes.BAMBOO_PRESSURE_PLATE.getId(), (short) 300);
        duration.put(BlockTypes.BIRCH_PRESSURE_PLATE.getId(), (short) 300);
        duration.put(BlockTypes.CHERRY_PRESSURE_PLATE.getId(), (short) 300);
        duration.put(BlockTypes.CRIMSON_PRESSURE_PLATE.getId(), (short) 300);
        duration.put(BlockTypes.DARK_OAK_PRESSURE_PLATE.getId(), (short) 300);
        duration.put(BlockTypes.JUNGLE_PRESSURE_PLATE.getId(), (short) 300);
        duration.put(BlockTypes.MANGROVE_PRESSURE_PLATE.getId(), (short) 300);
        duration.put(BlockTypes.OAK_PRESSURE_PLATE.getId(), (short) 300);
        duration.put(BlockTypes.SPRUCE_PRESSURE_PLATE.getId(), (short) 300);
        duration.put(BlockTypes.WARPED_PRESSURE_PLATE.getId(), (short) 300);

        // Buttons
        duration.put(BlockTypes.ACACIA_BUTTON.getId(), (short) 100);
        duration.put(BlockTypes.BAMBOO_BUTTON.getId(), (short) 100);
        duration.put(BlockTypes.BIRCH_BUTTON.getId(), (short) 100);
        duration.put(BlockTypes.CHERRY_BUTTON.getId(), (short) 100);
        duration.put(BlockTypes.CRIMSON_BUTTON.getId(), (short) 100);
        duration.put(BlockTypes.DARK_OAK_BUTTON.getId(), (short) 100);
        duration.put(BlockTypes.JUNGLE_BUTTON.getId(), (short) 100);
        duration.put(BlockTypes.MANGROVE_BUTTON.getId(), (short) 100);
        duration.put(BlockTypes.OAK_BUTTON.getId(), (short) 100);
        duration.put(BlockTypes.SPRUCE_BUTTON.getId(), (short) 100);
        duration.put(BlockTypes.WARPED_BUTTON.getId(), (short) 100);

        // Slabs
        duration.put(BlockTypes.ACACIA_SLAB.getId(), (short) 300);
        duration.put(BlockTypes.BAMBOO_MOSAIC_SLAB.getId(), (short) 300);
        duration.put(BlockTypes.BAMBOO_SLAB.getId(), (short) 300);
        duration.put(BlockTypes.BIRCH_SLAB.getId(), (short) 300);
        duration.put(BlockTypes.CHERRY_SLAB.getId(), (short) 300);
        duration.put(BlockTypes.CRIMSON_SLAB.getId(), (short) 300);
        duration.put(BlockTypes.DARK_OAK_SLAB.getId(), (short) 300);
        duration.put(BlockTypes.JUNGLE_SLAB.getId(), (short) 300);
        duration.put(BlockTypes.MANGROVE_SLAB.getId(), (short) 300);
        duration.put(BlockTypes.OAK_SLAB.getId(), (short) 300);
        duration.put(BlockTypes.SPRUCE_SLAB.getId(), (short) 300);
        duration.put(BlockTypes.WARPED_SLAB.getId(), (short) 300);

        // Double slabs
        duration.put(BlockTypes.ACACIA_DOUBLE_SLAB.getId(), (short) 300);
        duration.put(BlockTypes.BAMBOO_DOUBLE_SLAB.getId(), (short) 300);
        duration.put(BlockTypes.BAMBOO_MOSAIC_DOUBLE_SLAB.getId(), (short) 300);
        duration.put(BlockTypes.BIRCH_DOUBLE_SLAB.getId(), (short) 300);
        duration.put(BlockTypes.CHERRY_DOUBLE_SLAB.getId(), (short) 300);
        duration.put(BlockTypes.CRIMSON_DOUBLE_SLAB.getId(), (short) 300);
        duration.put(BlockTypes.DARK_OAK_DOUBLE_SLAB.getId(), (short) 300);
        duration.put(BlockTypes.JUNGLE_DOUBLE_SLAB.getId(), (short) 300);
        duration.put(BlockTypes.MANGROVE_DOUBLE_SLAB.getId(), (short) 300);
        duration.put(BlockTypes.OAK_DOUBLE_SLAB.getId(), (short) 300);
        duration.put(BlockTypes.SPRUCE_DOUBLE_SLAB.getId(), (short) 300);
        duration.put(BlockTypes.WARPED_DOUBLE_SLAB.getId(), (short) 300);

        // Other wooden items/blocks
        duration.put(BlockTypes.BOOKSHELF.getId(), (short) 300);
        duration.put(BlockTypes.BROWN_MUSHROOM_BLOCK.getId(), (short) 300);
        duration.put(BlockTypes.CHEST.getId(), (short) 300);
        duration.put(BlockTypes.CRAFTING_TABLE.getId(), (short) 300);
        duration.put(BlockTypes.DAYLIGHT_DETECTOR.getId(), (short) 300);
        duration.put(BlockTypes.DAYLIGHT_DETECTOR_INVERTED.getId(), (short) 300);
        duration.put(BlockTypes.DRIED_KELP_BLOCK.getId(), (short) 4000);
        duration.put(BlockTypes.JUKEBOX.getId(), (short) 300);
        duration.put(BlockTypes.LADDER.getId(), (short) 300);
        duration.put(BlockTypes.NOTE_BLOCK.getId(), (short) 300);
        duration.put(BlockTypes.RED_MUSHROOM_BLOCK.getId(), (short) 300);
        duration.put(BlockTypes.TRAPPED_CHEST.getId(), (short) 300);
        duration.put(ItemTypes.ACACIA_DOOR.getId(), (short) 200);
        duration.put(ItemTypes.BANNER.getId(), (short) 300);
        duration.put(ItemTypes.BIRCH_DOOR.getId(), (short) 200);
        duration.put(ItemTypes.BLAZE_ROD.getId(), (short) 2400);
        duration.put(ItemTypes.OAK_BOAT.getId(), (short) 1200);
        duration.put(ItemTypes.SPRUCE_BOAT.getId(), (short) 1200);
        duration.put(ItemTypes.BIRCH_BOAT.getId(), (short) 1200);
        duration.put(ItemTypes.JUNGLE_BOAT.getId(), (short) 1200);
        duration.put(ItemTypes.ACACIA_BOAT.getId(), (short) 1200);
        duration.put(ItemTypes.DARK_OAK_BOAT.getId(), (short) 1200);
        duration.put(ItemTypes.MANGROVE_BOAT.getId(), (short) 1200);
        duration.put(ItemTypes.BOW.getId(), (short) 200);
        duration.put(ItemTypes.BOWL.getId(), (short) 200);
        duration.put(ItemTypes.BUCKET.getId(), (short) 20000);
        duration.put(ItemTypes.DARK_OAK_DOOR.getId(), (short) 200);
        duration.put(ItemTypes.FISHING_ROD.getId(), (short) 300);
        duration.put(ItemTypes.JUNGLE_DOOR.getId(), (short) 200);
        duration.put(ItemTypes.SPRUCE_DOOR.getId(), (short) 200);
        duration.put(ItemTypes.WOODEN_DOOR.getId(), (short) 200);
    }
}
