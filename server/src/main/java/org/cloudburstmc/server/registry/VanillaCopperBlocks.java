package org.cloudburstmc.server.registry;

import com.google.common.collect.ImmutableMap;
import lombok.experimental.UtilityClass;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.util.Identifier;

import java.util.List;
import java.util.Map;

import static org.cloudburstmc.api.block.BlockTypes.*;

/**
 * Indexes the vanilla copper families and their wax and weathering transitions.
 */
@UtilityClass
public class VanillaCopperBlocks {

    private static final List<String> STAGE_PREFIXES = List.of("", "exposed_", "weathered_", "oxidized_");
    private static final List<BlockType> FAMILY_BASES = List.of(
            CHISELED_COPPER, COPPER_BARS, COPPER_BLOCK, COPPER_BULB, COPPER_CHAIN,
            COPPER_CHEST, COPPER_DOOR, COPPER_GOLEM_STATUE, COPPER_GRATE, COPPER_LANTERN,
            COPPER_TRAPDOOR, CUT_COPPER, CUT_COPPER_SLAB, CUT_COPPER_STAIRS,
            DOUBLE_CUT_COPPER_SLAB, LIGHTNING_ROD
    );
    private static final ImmutableMap<BlockType, Variant> VARIANTS = indexVariants();

    public static List<BlockType> family(BlockType base) {
        return VARIANTS.entrySet().stream()
                .filter(entry -> entry.getValue().base() == base)
                .map(Map.Entry::getKey)
                .toList();
    }

    public static @Nullable BlockType waxed(BlockType type) {
        Variant variant = VARIANTS.get(type);
        return variant == null || variant.waxed() ? null : variant.counterpart();
    }

    public static @Nullable BlockType unwaxed(BlockType type) {
        Variant variant = VARIANTS.get(type);
        return variant == null || !variant.waxed() ? null : variant.counterpart();
    }

    public static @Nullable BlockType next(BlockType type) {
        Variant variant = VARIANTS.get(type);
        return variant == null ? null : variant.next();
    }

    public static @Nullable BlockType previous(BlockType type) {
        Variant variant = VARIANTS.get(type);
        return variant == null ? null : variant.previous();
    }

    public static int weatheringStage(BlockType type) {
        Variant variant = VARIANTS.get(type);
        return variant == null || variant.waxed() ? -1 : variant.stage();
    }

    private static ImmutableMap<BlockType, Variant> indexVariants() {
        ImmutableMap.Builder<BlockType, Variant> variants = ImmutableMap.builder();

        for (BlockType base : FAMILY_BASES) {
            String name = base == COPPER_BLOCK ? "copper" : base.getId().getName();
            int stageCount = STAGE_PREFIXES.size();
            List<BlockType> unwaxed = STAGE_PREFIXES.stream()
                    .map(prefix -> prefix.isEmpty() ? base : requireType(prefix + name))
                    .toList();

            for (int stage = 0; stage < stageCount; stage++) {
                BlockType source = unwaxed.get(stage);
                BlockType waxed = requireType("waxed_" + STAGE_PREFIXES.get(stage) + name);
                BlockType next = stage + 1 < stageCount ? unwaxed.get(stage + 1) : null;
                BlockType previous = stage > 0 ? unwaxed.get(stage - 1) : null;

                variants.put(source, new Variant(base, stage, false, waxed, next, previous));
                variants.put(waxed, new Variant(base, stage, true, source, null, null));
            }
        }

        return variants.buildOrThrow();
    }

    private static BlockType requireType(String name) {
        Identifier id = Identifier.parse(name);
        return BlockTypes.get(id).orElseThrow(() -> new IllegalStateException("Missing copper variant: " + id));
    }

    private record Variant(
            BlockType base,
            int stage,
            boolean waxed,
            BlockType counterpart,
            @Nullable BlockType next,
            @Nullable BlockType previous
    ) {
    }
}
