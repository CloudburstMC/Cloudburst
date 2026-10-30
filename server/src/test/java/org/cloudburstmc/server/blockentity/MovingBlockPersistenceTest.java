package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.blockentity.BlockEntityTypes;
import org.cloudburstmc.api.blockentity.data.MovingBlockData;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.block.BlockPalette;
import org.cloudburstmc.server.registry.CloudBlockEntityRegistry;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.cloudburstmc.server.testutil.PersistenceFixtures;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Round-trip tests for the moving-block data path: every key today's load/save
 * handle, plus an unknown key, survive with the same NBT types.
 */
class MovingBlockPersistenceTest {

    private static final Vector3i POSITION = Vector3i.from(10, 64, -5);

    private static final BlockState POPPY = BlockTestRegistry.get().getBlock(BlockTypes.POPPY);
    private static final BlockState STONE = BlockTestRegistry.get().getBlock(BlockTypes.STONE);

    private static NbtMap movingBlockTag() {
        SignBlockEntity sign = (SignBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.SIGN, POSITION);
        sign.loadAdditionalData(NbtMap.builder()
                .putString("id", "Sign")
                .putInt("x", 10)
                .putInt("y", 64)
                .putInt("z", -5)
                .putString("Text", "one\ntwo\nthree\nfour")
                .putString("TextOwner", "Steve")
                .build());

        return NbtMap.builder()
                .putString("id", "MovingBlock")
                .putInt("x", 10)
                .putInt("y", 64)
                .putInt("z", -5)
                .putBoolean("isMovable", true)
                .putString("CustomName", "Moving")
                .putCompound("movingBlock", BlockPalette.INSTANCE.getSerialized(POPPY))
                .putCompound("movingBlockExtra", BlockPalette.INSTANCE.getSerialized(STONE))
                .putCompound("movingEntity", sign.getServerTag())
                .putInt("pistonPosX", 10)
                .putInt("pistonPosY", 63)
                .putInt("pistonPosZ", -5)
                .putInt("Mystery", 7)
                .build();
    }

    @Test
    void roundTripPreservesKeysAndTypes() {
        assertEquals(MovingBlockData.class,
                CloudBlockEntityRegistry.get().getDataClass(BlockEntityTypes.MOVING_BLOCK));

        MovingBlockEntity entity = (MovingBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.MOVING_BLOCK, POSITION);
        NbtMap input = movingBlockTag();
        entity.loadAdditionalData(input);

        assertEquals(POPPY, entity.getMovingBlock());
        assertEquals(STONE, entity.getExtraBlock());
        assertEquals(Vector3i.from(10, 63, -5), entity.getPiston());
        assertNotNull(entity.getBlockEntity());

        NbtMap saved = entity.getServerTag();
        Set<String> expectedKeys = new HashSet<>(input.keySet());
        expectedKeys.addAll(Set.of("id", "x", "y", "z"));
        assertEquals(expectedKeys, saved.keySet());
        for (String key : input.keySet()) {
            assertEquals(input.get(key), saved.get(key), "value of " + key);
            assertEquals(NbtType.byClass(input.get(key).getClass()),
                    NbtType.byClass(saved.get(key).getClass()), "type of " + key);
        }

        assertEquals(Set.of("id"), entity.getClientTag().keySet());
    }

    @Test
    void airExtraStateRoundTripsAsCompound() {
        BlockState air = BlockTestRegistry.get().getBlock(BlockTypes.AIR);
        NbtMap input = NbtMap.builder()
                .putString("id", "MovingBlock")
                .putInt("x", 10)
                .putInt("y", 64)
                .putInt("z", -5)
                .putCompound("movingBlock", BlockPalette.INSTANCE.getSerialized(POPPY))
                .putCompound("movingBlockExtra", BlockPalette.INSTANCE.getSerialized(air))
                .putInt("pistonPosX", 10)
                .putInt("pistonPosY", 63)
                .putInt("pistonPosZ", -5)
                .build();

        MovingBlockEntity entity = (MovingBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.MOVING_BLOCK, POSITION);
        entity.loadAdditionalData(input);

        assertEquals(air, entity.getExtraBlock());

        NbtMap saved = entity.getServerTag();
        assertEquals(input.getCompound("movingBlockExtra"), saved.getCompound("movingBlockExtra"));
        assertEquals(NbtType.COMPOUND, NbtType.byClass(saved.get("movingBlockExtra").getClass()));
    }

    @Test
    void defaultMovingBlockOmitsNestedEntity() {
        MovingBlockEntity entity = (MovingBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.MOVING_BLOCK, POSITION);
        entity.setBlock(POPPY);
        entity.setExtraBlock(STONE);
        entity.setPiston(Vector3i.from(10, 63, -5));

        assertNull(entity.getBlockEntity());

        NbtMap saved = entity.getServerTag();
        assertEquals(Set.of("id", "x", "y", "z", "isMovable", "movingBlock", "movingBlockExtra",
                "pistonPosX", "pistonPosY", "pistonPosZ"), saved.keySet());
        assertEquals(10, saved.getInt("pistonPosX"));

        assertEquals(Set.of("id"), entity.getClientTag().keySet());
    }

    @Test
    void movingBlockClearedEntityIsNotWritten() {
        MovingBlockEntity entity = (MovingBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.MOVING_BLOCK, POSITION);
        entity.loadAdditionalData(movingBlockTag());
        assertNotNull(entity.getBlockEntity());

        entity.setBlockEntity(null);
        assertNull(entity.getBlockEntity());

        NbtMap saved = entity.getServerTag();
        assertFalse(saved.containsKey("movingEntity"), "server tag still has movingEntity");

        NbtMap client = entity.getClientTag();
        assertFalse(client.containsKey("movingEntity"), "client tag still has movingEntity");
    }

    @Test
    void movingBlockBlockEntityKeepsNoDuplicateState() {
        DtoStateChecks.assertKeepsNoDuplicateState(MovingBlockEntity.class, MovingBlockData.class);
    }
}
