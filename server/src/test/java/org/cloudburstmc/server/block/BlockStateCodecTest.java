package org.cloudburstmc.server.block;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.server.registry.CloudBlockRegistry;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BlockStateCodecTest {

    private static final CloudBlockRegistry REGISTRY = BlockTestRegistry.get();

    @Test
    void everyRuntimeStateSurvivesPersistence() {
        for (CloudBlockDefinition definition : BlockPalette.INSTANCE.getRuntimeMap().values()) {
            BlockState state = definition.getCloudState();

            assertSame(state, BlockStateCodec.read(BlockStateCodec.write(state)), state.toString());
        }
    }

    @Test
    void missingPropertiesUseTheBlocksDefaults() {
        BlockState state = REGISTRY.getBlock(BlockTypes.OAK_STAIRS);
        NbtMapBuilder incomplete = BlockStateCodec.write(state).toBuilder();
        incomplete.remove("states");

        assertSame(state, BlockStateCodec.read(incomplete.build()));
    }

    @Test
    void unknownNamesAndMalformedPropertiesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> BlockStateCodec.read(NbtMap.EMPTY));
        assertThrows(IllegalArgumentException.class, () -> BlockStateCodec.read(NbtMap.builder()
                .putString("name", "cloudburst:missing_block")
                .build()));
        assertThrows(IllegalArgumentException.class, () -> BlockStateCodec.read(NbtMap.builder()
                .putString("name", "minecraft:stone")
                .putString("states", "not a compound")
                .build()));

        NbtMap invalid = BlockStateCodec.write(REGISTRY.getBlock(BlockTypes.OAK_STAIRS)).toBuilder()
                .putCompound("states", NbtMap.builder().putString("cloudburst:unknown_property", "invalid").build())
                .build();

        assertThrows(IllegalArgumentException.class, () -> BlockStateCodec.read(invalid));
    }

    @Test
    void unregisteredNamesUseTheExplicitFallbackWithoutWeakeningStateValidation() {
        BlockState unknown = REGISTRY.getBlock(BlockTypes.UNKNOWN);
        NbtMap custom = NbtMap.builder().putString("name", "test:unregistered_block")
                .putCompound("states", NbtMap.EMPTY).build();

        assertSame(unknown, BlockStateCodec.read(custom, id -> {
            assertEquals(Identifier.parse("test:unregistered_block"), id);
            return unknown;
        }));

        NbtMap invalid = BlockStateCodec.write(REGISTRY.getBlock(BlockTypes.OAK_STAIRS)).toBuilder()
                .putCompound("states", NbtMap.builder().putString("cloudburst:unknown_property", "invalid").build())
                .build();
        for (NbtMap malformed : new NbtMap[]{NbtMap.EMPTY, invalid,
                custom.toBuilder().putString("states", "not a compound").build()}) {
            assertThrows(IllegalArgumentException.class, () -> BlockStateCodec.read(malformed, id -> {
                throw new AssertionError("Malformed states must not use the fallback");
            }));
        }
    }

    @Test
    void numericMetadataRequiresMigration() {
        NbtMap named = BlockStateCodec.write(REGISTRY.getBlock(BlockTypes.STONE));

        for (short metadata : new short[]{0, 1, 15}) {
            NbtMap numeric = named.toBuilder().putShort("val", metadata).build();

            assertThrows(UnsupportedOperationException.class, () -> BlockStateCodec.read(numeric));
        }
    }
}
