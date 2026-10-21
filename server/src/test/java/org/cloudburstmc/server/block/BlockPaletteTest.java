package org.cloudburstmc.server.block;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.block.trait.IntegerBlockTrait;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.server.block.serializer.BlockSerializer;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BlockPaletteTest {

    private static final IntegerBlockTrait STAGE = IntegerBlockTrait.from("palette_stage", 0, 3, 2);
    private static final Identifier ID = Identifier.parse("cloudburst:palette_test");
    private static final BlockSerializer SERIALIZER = (builder, type, traits) ->
            builder.putCompound("states", NbtMap.builder().putInt("stage", (Integer) traits.get(STAGE)).build());

    @Test
    void identifierLookupUsesTheDeclaredDefaultInsteadOfTheFirstPermutation() {
        BlockPalette palette = new BlockPalette();
        BlockType type = BlockType.of(ID, STAGE);

        palette.addBlock(type, SERIALIZER);

        assertSame(type.getDefaultState(), palette.getState(ID));
        assertSame(type, palette.getType(ID));
        assertEquals(2, palette.getDefaultProperties(ID).getInt("stage"));
        assertSame(type.getDefaultState().withTrait(STAGE, 0), palette.getState(ID, Map.of("stage", 0)));
    }

    @Test
    void stateIdentityIgnoresEnvelopeMetadataButPreservesProperties() {
        BlockPalette palette = new BlockPalette();
        BlockType type = BlockType.of(ID, STAGE);
        palette.addBlock(type, SERIALIZER);
        BlockState state = type.getDefaultState().withTrait(STAGE, 1);
        NbtMap serialized = palette.getSerialized(state);
        NbtMap decorated = serialized.toBuilder()
                .putInt("version", 123)
                .putInt("name_hash", 456)
                .putInt("network_id", 789)
                .putInt("block_id", 12)
                .build();

        assertSame(state, palette.getBlockState(decorated));
        assertSame(ID, palette.getIdentifier(state));
        assertNull(palette.getState(ID, Map.of("stage", 10)));
    }

    @Test
    void duplicateSerializationDoesNotPublishAPartialRegistration() {
        BlockPalette palette = new BlockPalette();
        BlockType type = BlockType.of(ID, STAGE);
        BlockSerializer duplicate = (builder, blockType, traits) -> builder.putCompound("states", NbtMap.EMPTY);

        assertThrows(IllegalArgumentException.class, () -> palette.addBlock(type, duplicate));

        assertNull(palette.getType(ID));
        assertTrue(palette.getSerializedPalette().isEmpty());
        assertTrue(palette.getTypeIdentifiers(type).isEmpty());
        assertDoesNotThrow(() -> palette.addBlock(type, SERIALIZER));
        assertThrows(IllegalArgumentException.class, () -> palette.addBlock(type, SERIALIZER));
    }

    @Test
    void malformedSerializationDoesNotPublishARegistration() {
        BlockPalette palette = new BlockPalette();
        BlockType type = BlockType.of(ID, STAGE);
        BlockSerializer invalidName = (builder, blockType, traits) -> {
            builder.putInt("name", 1);
            builder.putCompound("states", NbtMap.EMPTY);
        };
        BlockSerializer blankName = (builder, blockType, traits) -> {
            builder.putString("name", " ");
            builder.putCompound("states", NbtMap.EMPTY);
        };
        BlockSerializer invalidProperties = (builder, blockType, traits) -> builder.putString("states", "invalid");
        BlockSerializer missingProperties = (builder, blockType, traits) -> { };

        assertThrows(IllegalArgumentException.class, () -> palette.addBlock(type, invalidName));
        assertThrows(IllegalArgumentException.class, () -> palette.addBlock(type, blankName));
        assertThrows(IllegalArgumentException.class, () -> palette.addBlock(type, invalidProperties));
        assertThrows(IllegalArgumentException.class, () -> palette.addBlock(type, missingProperties));

        assertNull(palette.getType(ID));
        assertTrue(palette.getSerializedPalette().isEmpty());
        assertDoesNotThrow(() -> palette.addBlock(type, SERIALIZER));
    }

    @Test
    void exposedIndexesCannotBeModified() {
        BlockPalette palette = new BlockPalette();
        BlockType type = BlockType.of(ID, STAGE);
        palette.addBlock(type, SERIALIZER);

        assertThrows(UnsupportedOperationException.class, () -> palette.getSerializedPalette().clear());
        assertThrows(UnsupportedOperationException.class, () -> palette.getStates(ID).clear());
        assertThrows(UnsupportedOperationException.class, () -> palette.getTypeIdentifiers(type).clear());
        assertThrows(UnsupportedOperationException.class, () -> palette.getRuntimeMap().put(0, null));
    }
}
