package org.cloudburstmc.server.registry;

import com.google.common.collect.ImmutableMap;
import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.blockentity.BlockEntityTypes;
import org.cloudburstmc.api.level.sound.SoundType;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.cloudburstmc.api.block.BlockTypes.*;
import static org.cloudburstmc.api.level.sound.SoundTypes.*;

@UtilityClass
public class VanillaChestBlocks {

    private static final Definition WOODEN = new Definition(Family.WOODEN, RANDOM_CHESTOPEN, RANDOM_CHESTCLOSED);
    private static final Definition TRAPPED = new Definition(Family.TRAPPED, RANDOM_CHESTOPEN, RANDOM_CHESTCLOSED);
    private static final Definition ENDER = new Definition(Family.ENDER, RANDOM_ENDERCHESTOPEN, RANDOM_ENDERCHESTCLOSED);
    private static final Definition COPPER = new Definition(Family.COPPER, BLOCK_COPPER_CHEST_OPEN, BLOCK_COPPER_CHEST_CLOSED);
    private static final Definition WEATHERED = new Definition(Family.COPPER, BLOCK_COPPER_CHEST_WEATHERED_OPEN, BLOCK_COPPER_CHEST_WEATHERED_CLOSED);
    private static final Definition OXIDIZED = new Definition(Family.COPPER, BLOCK_COPPER_CHEST_OXIDIZED_OPEN, BLOCK_COPPER_CHEST_OXIDIZED_CLOSED);
    private static final Map<BlockType, Definition> DEFINITIONS = indexDefinitions();

    public static Map<BlockType, Definition> definitions() {
        return DEFINITIONS;
    }

    public static Set<BlockType> blocksFor(BlockEntityType<?> type) {
        return DEFINITIONS.entrySet().stream()
                .filter(entry -> entry.getValue().family().blockEntityType() == type)
                .map(Map.Entry::getKey)
                .collect(Collectors.toUnmodifiableSet());
    }

    public static boolean isCopper(BlockType type) {
        Definition definition = DEFINITIONS.get(type);
        return definition != null && definition.family() == Family.COPPER;
    }

    public static boolean usesPlacementOrder(BlockType type) {
        return DEFINITIONS.getOrDefault(type, WOODEN).family() != Family.TRAPPED;
    }

    public static boolean emitsSignal(BlockType type) {
        Definition definition = DEFINITIONS.get(type);
        return definition != null && definition.family().emitsSignal();
    }

    public static SoundType lidSound(BlockType type, boolean open) {
        Definition definition = DEFINITIONS.getOrDefault(type, WOODEN);
        return open ? definition.openSound() : definition.closeSound();
    }

    private static Map<BlockType, Definition> indexDefinitions() {
        ImmutableMap.Builder<BlockType, Definition> definitions = ImmutableMap.builder();
        definitions.put(CHEST, WOODEN).put(TRAPPED_CHEST, TRAPPED).put(ENDER_CHEST, ENDER);
        for (BlockType variant : VanillaCopperBlocks.family(COPPER_CHEST)) {
            BlockType unwaxed = Objects.requireNonNullElse(VanillaCopperBlocks.unwaxed(variant), variant);
            Definition definition = unwaxed == WEATHERED_COPPER_CHEST ? WEATHERED : unwaxed == OXIDIZED_COPPER_CHEST ? OXIDIZED : COPPER;
            definitions.put(variant, definition);
        }
        return definitions.buildOrThrow();
    }

    public record Definition(Family family, SoundType openSound, SoundType closeSound) {
    }

    public enum Family {
        WOODEN,
        TRAPPED,
        COPPER,
        ENDER;

        public BlockEntityType<?> blockEntityType() {
            return this == ENDER ? BlockEntityTypes.ENDER_CHEST : BlockEntityTypes.CHEST;
        }

        public boolean emitsSignal() {
            return this == TRAPPED;
        }
    }
}
