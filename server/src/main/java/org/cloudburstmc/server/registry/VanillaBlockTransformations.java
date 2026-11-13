package org.cloudburstmc.server.registry;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.block.trait.IntegerBlockTrait;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.sound.SoundTypes;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.protocol.bedrock.data.LevelEvent;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.server.item.component.BlockTransformation;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.cloudburstmc.api.block.BlockTypes.*;

@UtilityClass
public class VanillaBlockTransformations {

    private static final String STRIPPED_PREFIX = "stripped_";
    private static final Map<BlockType, BlockType> STRIPPABLES = indexStrippables();
    private static final Set<BlockType> TILLABLE_SOILS = Set.of(DIRT, GRASS_BLOCK, GRASS_PATH);
    private static final Set<BlockType> PATH_SOILS = Set.of(COARSE_DIRT, DIRT, DIRT_WITH_ROOTS, GRASS_BLOCK, MYCELIUM, PODZOL);
    private static final Map<BlockType, IntegerBlockTrait> PRUNABLE_TIPS = Map.of(
            KELP, BlockTraits.KELP_AGE,
            CAVE_VINES, BlockTraits.CAVE_VINE_AGE,
            CAVE_VINES_HEAD_WITH_BERRIES, BlockTraits.CAVE_VINE_AGE,
            TWISTING_VINES, BlockTraits.TWISTING_VINES_AGE,
            WEEPING_VINES, BlockTraits.WEEPING_VINES_AGE
    );

    public static final BlockTransformation.Resolver AXE = (state, _, _) -> {
        BlockType type = state.getType();
        BlockType stripped = STRIPPABLES.get(type);
        if (stripped != null) {
            return replacement(state, stripped).build();
        }

        BlockType unwaxed = VanillaCopperBlocks.unwaxed(type);
        if (unwaxed != null) {
            return replacement(state, unwaxed)
                    .sound(SoundEvent.COPPER_WAX_OFF)
                    .particle(LevelEvent.PARTICLE_WAX_OFF)
                    .build();
        }

        BlockType previous = VanillaCopperBlocks.previous(type);
        return previous == null ? null : replacement(state, previous)
                .sound(SoundEvent.SCRAPE)
                .particle(LevelEvent.PARTICLE_SCRAPE)
                .build();
    };

    public static final BlockTransformation.Resolver HOE = (state, face, airAbove) -> {
        BlockType type = state.getType();
        if (type == DIRT_WITH_ROOTS) {
            return replacement(state, DIRT).drop(ItemTypes.HANGING_ROOTS).build();
        }

        if (!airAbove || face == Direction.DOWN) {
            return null;
        }

        if (type == COARSE_DIRT) {
            return replacement(state, DIRT).build();
        }

        return TILLABLE_SOILS.contains(type) ? replacement(state, FARMLAND).build() : null;
    };

    public static final BlockTransformation.Resolver SHOVEL = (state, face, airAbove) -> {
        BlockType type = state.getType();
        if (type == CAMPFIRE || type == SOUL_CAMPFIRE) {
            return state.ensureTrait(BlockTraits.IS_EXTINGUISHED) ? null : BlockTransformation.builder()
                    .state(state.withTrait(BlockTraits.IS_EXTINGUISHED, true))
                    .sound(SoundEvent.EXTINGUISH_FIRE)
                    .build();
        }

        return airAbove && face != Direction.DOWN && PATH_SOILS.contains(type) ? replacement(state, GRASS_PATH).build() : null;
    };

    public static final BlockTransformation.Resolver SHEARS = (state, _, _) -> {
        IntegerBlockTrait age = PRUNABLE_TIPS.get(state.getType());
        if (age == null) {
            return null;
        }

        int limit = age.getRange().getEnd();
        return state.ensureTrait(age) == limit ? null : BlockTransformation.builder()
                .state(state.withTrait(age, limit))
                .soundOverride(SoundTypes.MOB_SHEEP_SHEAR)
                .build();
    };

    public static final BlockTransformation.Resolver HONEYCOMB = (state, _, _) -> {
        BlockType waxed = VanillaCopperBlocks.waxed(state.getType());
        return waxed == null ? null : replacement(state, waxed)
                .sound(SoundEvent.COPPER_WAX_ON)
                .particle(LevelEvent.PARTICLE_WAX_ON)
                .consumesItem(true)
                .build();
    };

    private static Map<BlockType, BlockType> indexStrippables() {
        Map<BlockType, BlockType> replacements = new HashMap<>();
        for (BlockType stripped : BlockTypes.values()) {
            String name = stripped.getId().getName();
            if (name.startsWith(STRIPPED_PREFIX)) {
                Identifier id = Identifier.parse(name.substring(STRIPPED_PREFIX.length()));
                BlockType source = BlockTypes.get(id).orElseThrow(() -> new IllegalStateException("Missing block for " + stripped.getId()));
                replacements.put(source, stripped);
            }
        }
        return Map.copyOf(replacements);
    }

    private static BlockTransformation.BlockTransformationBuilder replacement(BlockState source, BlockType target) {
        return BlockTransformation.builder().state(target.getDefaultState().copyTraits(source));
    }
}
