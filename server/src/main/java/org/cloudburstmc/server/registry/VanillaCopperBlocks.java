package org.cloudburstmc.server.registry;

import com.google.common.collect.ImmutableBiMap;
import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.BlockType;

import java.util.List;

import static org.cloudburstmc.api.block.BlockTypes.*;

@UtilityClass
public class VanillaCopperBlocks {

    private static final List<CopperBlockFamily> FAMILIES = List.of(
            new CopperBlockFamily(
                    List.of(CHISELED_COPPER, EXPOSED_CHISELED_COPPER, WEATHERED_CHISELED_COPPER, OXIDIZED_CHISELED_COPPER),
                    List.of(WAXED_CHISELED_COPPER, WAXED_EXPOSED_CHISELED_COPPER, WAXED_WEATHERED_CHISELED_COPPER, WAXED_OXIDIZED_CHISELED_COPPER)
            ),
            new CopperBlockFamily(
                    List.of(COPPER_BARS, EXPOSED_COPPER_BARS, WEATHERED_COPPER_BARS, OXIDIZED_COPPER_BARS),
                    List.of(WAXED_COPPER_BARS, WAXED_EXPOSED_COPPER_BARS, WAXED_WEATHERED_COPPER_BARS, WAXED_OXIDIZED_COPPER_BARS)
            ),
            new CopperBlockFamily(
                    List.of(COPPER_BLOCK, EXPOSED_COPPER, WEATHERED_COPPER, OXIDIZED_COPPER),
                    List.of(WAXED_COPPER, WAXED_EXPOSED_COPPER, WAXED_WEATHERED_COPPER, WAXED_OXIDIZED_COPPER)
            ),
            new CopperBlockFamily(
                    List.of(COPPER_BULB, EXPOSED_COPPER_BULB, WEATHERED_COPPER_BULB, OXIDIZED_COPPER_BULB),
                    List.of(WAXED_COPPER_BULB, WAXED_EXPOSED_COPPER_BULB, WAXED_WEATHERED_COPPER_BULB, WAXED_OXIDIZED_COPPER_BULB)
            ),
            new CopperBlockFamily(
                    List.of(COPPER_CHAIN, EXPOSED_COPPER_CHAIN, WEATHERED_COPPER_CHAIN, OXIDIZED_COPPER_CHAIN),
                    List.of(WAXED_COPPER_CHAIN, WAXED_EXPOSED_COPPER_CHAIN, WAXED_WEATHERED_COPPER_CHAIN, WAXED_OXIDIZED_COPPER_CHAIN)
            ),
            new CopperBlockFamily(
                    List.of(COPPER_CHEST, EXPOSED_COPPER_CHEST, WEATHERED_COPPER_CHEST, OXIDIZED_COPPER_CHEST),
                    List.of(WAXED_COPPER_CHEST, WAXED_EXPOSED_COPPER_CHEST, WAXED_WEATHERED_COPPER_CHEST, WAXED_OXIDIZED_COPPER_CHEST)
            ),
            new CopperBlockFamily(
                    List.of(COPPER_DOOR, EXPOSED_COPPER_DOOR, WEATHERED_COPPER_DOOR, OXIDIZED_COPPER_DOOR),
                    List.of(WAXED_COPPER_DOOR, WAXED_EXPOSED_COPPER_DOOR, WAXED_WEATHERED_COPPER_DOOR, WAXED_OXIDIZED_COPPER_DOOR)
            ),
            new CopperBlockFamily(
                    List.of(COPPER_GOLEM_STATUE, EXPOSED_COPPER_GOLEM_STATUE, WEATHERED_COPPER_GOLEM_STATUE, OXIDIZED_COPPER_GOLEM_STATUE),
                    List.of(WAXED_COPPER_GOLEM_STATUE, WAXED_EXPOSED_COPPER_GOLEM_STATUE, WAXED_WEATHERED_COPPER_GOLEM_STATUE, WAXED_OXIDIZED_COPPER_GOLEM_STATUE)
            ),
            new CopperBlockFamily(
                    List.of(COPPER_GRATE, EXPOSED_COPPER_GRATE, WEATHERED_COPPER_GRATE, OXIDIZED_COPPER_GRATE),
                    List.of(WAXED_COPPER_GRATE, WAXED_EXPOSED_COPPER_GRATE, WAXED_WEATHERED_COPPER_GRATE, WAXED_OXIDIZED_COPPER_GRATE)
            ),
            new CopperBlockFamily(
                    List.of(COPPER_LANTERN, EXPOSED_COPPER_LANTERN, WEATHERED_COPPER_LANTERN, OXIDIZED_COPPER_LANTERN),
                    List.of(WAXED_COPPER_LANTERN, WAXED_EXPOSED_COPPER_LANTERN, WAXED_WEATHERED_COPPER_LANTERN, WAXED_OXIDIZED_COPPER_LANTERN)
            ),
            new CopperBlockFamily(
                    List.of(COPPER_TRAPDOOR, EXPOSED_COPPER_TRAPDOOR, WEATHERED_COPPER_TRAPDOOR, OXIDIZED_COPPER_TRAPDOOR),
                    List.of(WAXED_COPPER_TRAPDOOR, WAXED_EXPOSED_COPPER_TRAPDOOR, WAXED_WEATHERED_COPPER_TRAPDOOR, WAXED_OXIDIZED_COPPER_TRAPDOOR)
            ),
            new CopperBlockFamily(
                    List.of(CUT_COPPER, EXPOSED_CUT_COPPER, WEATHERED_CUT_COPPER, OXIDIZED_CUT_COPPER),
                    List.of(WAXED_CUT_COPPER, WAXED_EXPOSED_CUT_COPPER, WAXED_WEATHERED_CUT_COPPER, WAXED_OXIDIZED_CUT_COPPER)
            ),
            new CopperBlockFamily(
                    List.of(CUT_COPPER_SLAB, EXPOSED_CUT_COPPER_SLAB, WEATHERED_CUT_COPPER_SLAB, OXIDIZED_CUT_COPPER_SLAB),
                    List.of(WAXED_CUT_COPPER_SLAB, WAXED_EXPOSED_CUT_COPPER_SLAB, WAXED_WEATHERED_CUT_COPPER_SLAB, WAXED_OXIDIZED_CUT_COPPER_SLAB)
            ),
            new CopperBlockFamily(
                    List.of(CUT_COPPER_STAIRS, EXPOSED_CUT_COPPER_STAIRS, WEATHERED_CUT_COPPER_STAIRS, OXIDIZED_CUT_COPPER_STAIRS),
                    List.of(WAXED_CUT_COPPER_STAIRS, WAXED_EXPOSED_CUT_COPPER_STAIRS, WAXED_WEATHERED_CUT_COPPER_STAIRS, WAXED_OXIDIZED_CUT_COPPER_STAIRS)
            ),
            new CopperBlockFamily(
                    List.of(DOUBLE_CUT_COPPER_SLAB, EXPOSED_DOUBLE_CUT_COPPER_SLAB, WEATHERED_DOUBLE_CUT_COPPER_SLAB, OXIDIZED_DOUBLE_CUT_COPPER_SLAB),
                    List.of(WAXED_DOUBLE_CUT_COPPER_SLAB, WAXED_EXPOSED_DOUBLE_CUT_COPPER_SLAB, WAXED_WEATHERED_DOUBLE_CUT_COPPER_SLAB, WAXED_OXIDIZED_DOUBLE_CUT_COPPER_SLAB)
            ),
            new CopperBlockFamily(
                    List.of(LIGHTNING_ROD, EXPOSED_LIGHTNING_ROD, WEATHERED_LIGHTNING_ROD, OXIDIZED_LIGHTNING_ROD),
                    List.of(WAXED_LIGHTNING_ROD, WAXED_EXPOSED_LIGHTNING_ROD, WAXED_WEATHERED_LIGHTNING_ROD, WAXED_OXIDIZED_LIGHTNING_ROD)
            )
    );

    public static final ImmutableBiMap<BlockType, BlockType> WAXABLES = createWaxables();
    public static final ImmutableBiMap<BlockType, BlockType> WAX_OFF = WAXABLES.inverse();
    public static final ImmutableBiMap<BlockType, BlockType> PREVIOUS = createWeathering().inverse();

    private static ImmutableBiMap<BlockType, BlockType> createWaxables() {
        ImmutableBiMap.Builder<BlockType, BlockType> pairs = ImmutableBiMap.builder();
        for (CopperBlockFamily family : FAMILIES) {
            for (int stage = 0; stage < family.unwaxed().size(); stage++) {
                pairs.put(family.unwaxed().get(stage), family.waxed().get(stage));
            }
        }

        return pairs.buildOrThrow();
    }

    private static ImmutableBiMap<BlockType, BlockType> createWeathering() {
        ImmutableBiMap.Builder<BlockType, BlockType> pairs = ImmutableBiMap.builder();
        for (CopperBlockFamily family : FAMILIES) {
            for (int stage = 1; stage < family.unwaxed().size(); stage++) {
                pairs.put(family.unwaxed().get(stage - 1), family.unwaxed().get(stage));
            }
        }

        return pairs.buildOrThrow();
    }
}
