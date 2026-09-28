package org.cloudburstmc.server.registry;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.protocol.bedrock.data.LevelEvent;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.server.item.component.BlockTransformation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.cloudburstmc.api.block.BlockTypes.*;

@UtilityClass
public class VanillaBlockTransformations {
    public static final Map<BlockType, BlockTransformation> AXE = createAxe();
    public static final Map<BlockType, BlockTransformation> HOE = createHoe();
    public static final Map<BlockType, BlockTransformation> SHOVEL = createShovel();
    public static final Map<BlockType, BlockTransformation> HONEYCOMB = createHoneycomb();

    private static Map<BlockType, BlockTransformation> createAxe() {
        Map<BlockType, BlockTransformation> rules = new LinkedHashMap<>();
        rules.put(ACACIA_LOG, replacement(STRIPPED_ACACIA_LOG).build());
        rules.put(ACACIA_WOOD, replacement(STRIPPED_ACACIA_WOOD).build());
        rules.put(BAMBOO_BLOCK, replacement(STRIPPED_BAMBOO_BLOCK).build());
        rules.put(BIRCH_LOG, replacement(STRIPPED_BIRCH_LOG).build());
        rules.put(BIRCH_WOOD, replacement(STRIPPED_BIRCH_WOOD).build());
        rules.put(CHERRY_LOG, replacement(STRIPPED_CHERRY_LOG).build());
        rules.put(CHERRY_WOOD, replacement(STRIPPED_CHERRY_WOOD).build());
        rules.put(CRIMSON_HYPHAE, replacement(STRIPPED_CRIMSON_HYPHAE).build());
        rules.put(CRIMSON_STEM, replacement(STRIPPED_CRIMSON_STEM).build());
        rules.put(DARK_OAK_LOG, replacement(STRIPPED_DARK_OAK_LOG).build());
        rules.put(DARK_OAK_WOOD, replacement(STRIPPED_DARK_OAK_WOOD).build());
        rules.put(JUNGLE_LOG, replacement(STRIPPED_JUNGLE_LOG).build());
        rules.put(JUNGLE_WOOD, replacement(STRIPPED_JUNGLE_WOOD).build());
        rules.put(MANGROVE_LOG, replacement(STRIPPED_MANGROVE_LOG).build());
        rules.put(MANGROVE_WOOD, replacement(STRIPPED_MANGROVE_WOOD).build());
        rules.put(OAK_LOG, replacement(STRIPPED_OAK_LOG).build());
        rules.put(OAK_WOOD, replacement(STRIPPED_OAK_WOOD).build());
        rules.put(PALE_OAK_LOG, replacement(STRIPPED_PALE_OAK_LOG).build());
        rules.put(PALE_OAK_WOOD, replacement(STRIPPED_PALE_OAK_WOOD).build());
        rules.put(POPLAR_LOG, replacement(STRIPPED_POPLAR_LOG).build());
        rules.put(POPLAR_WOOD, replacement(STRIPPED_POPLAR_WOOD).build());
        rules.put(SPRUCE_LOG, replacement(STRIPPED_SPRUCE_LOG).build());
        rules.put(SPRUCE_WOOD, replacement(STRIPPED_SPRUCE_WOOD).build());
        rules.put(WARPED_HYPHAE, replacement(STRIPPED_WARPED_HYPHAE).build());
        rules.put(WARPED_STEM, replacement(STRIPPED_WARPED_STEM).build());

        VanillaCopperBlocks.WAX_OFF.forEach((waxed, source) -> rules.put(waxed, replacement(source)
                .sound(SoundEvent.COPPER_WAX_OFF).particle(LevelEvent.PARTICLE_WAX_OFF).build()));
        VanillaCopperBlocks.PREVIOUS.forEach((source, previous) -> rules.put(source, replacement(previous)
                .sound(SoundEvent.SCRAPE).particle(LevelEvent.PARTICLE_SCRAPE).build()));
        return Map.copyOf(rules);
    }

    private static Map<BlockType, BlockTransformation> createHoe() {
        Map<BlockType, BlockTransformation> rules = new LinkedHashMap<>();
        for (BlockType source : List.of(DIRT, GRASS_BLOCK, GRASS_PATH)) {
            rules.put(source, replacement(FARMLAND).requiresAirAbove(true).disallowedFaces(Set.of(Direction.DOWN)).build());
        }

        rules.put(COARSE_DIRT, replacement(DIRT).requiresAirAbove(true).disallowedFaces(Set.of(Direction.DOWN)).build());
        rules.put(DIRT_WITH_ROOTS, replacement(DIRT).drop(ItemTypes.HANGING_ROOTS).build());
        return Map.copyOf(rules);
    }

    private static Map<BlockType, BlockTransformation> createShovel() {
        Map<BlockType, BlockTransformation> rules = new LinkedHashMap<>();
        for (BlockType source : List.of(COARSE_DIRT, DIRT, DIRT_WITH_ROOTS, GRASS_BLOCK, MYCELIUM, PODZOL)) {
            rules.put(source, replacement(GRASS_PATH).requiresAirAbove(true).disallowedFaces(Set.of(Direction.DOWN)).build());
        }

        BlockTransformation extinguish = BlockTransformation.builder()
                .transform(state -> state.withTrait(BlockTraits.IS_EXTINGUISHED, true))
                .sound(SoundEvent.EXTINGUISH_FIRE).build();
        rules.put(CAMPFIRE, extinguish);
        rules.put(SOUL_CAMPFIRE, extinguish);
        return Map.copyOf(rules);
    }

    private static Map<BlockType, BlockTransformation> createHoneycomb() {
        Map<BlockType, BlockTransformation> rules = new LinkedHashMap<>();
        VanillaCopperBlocks.WAXABLES.forEach((source, waxed) -> rules.put(source, replacement(waxed)
                .sound(SoundEvent.COPPER_WAX_ON).particle(LevelEvent.PARTICLE_WAX_ON)
                .consumesItem(true).build()));
        return Map.copyOf(rules);
    }

    private static BlockTransformation.BlockTransformationBuilder replacement(BlockType target) {
        return BlockTransformation.builder().transform(source -> target.getDefaultState().copyTraits(source));
    }
}
