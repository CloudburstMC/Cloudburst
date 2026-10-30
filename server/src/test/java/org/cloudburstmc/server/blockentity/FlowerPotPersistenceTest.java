package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.blockentity.BlockEntityTypes;
import org.cloudburstmc.api.blockentity.data.FlowerPotData;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.block.BlockPalette;
import org.cloudburstmc.server.registry.CloudBlockEntityRegistry;
import org.cloudburstmc.server.registry.CloudBlockRegistry;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.cloudburstmc.server.testutil.PersistenceFixtures;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Round-trip tests for the flower-pot data path: every key today's load/save
 * handle, plus an unknown key, survive with the same NBT types.
 */
class FlowerPotPersistenceTest {

    private static final Vector3i POSITION = Vector3i.from(10, 64, -5);

    private static final BlockState POPPY = BlockTestRegistry.get().getBlock(BlockTypes.POPPY);

    private static NbtMap flowerPotTag() {
        return NbtMap.builder()
                .putString("id", "FlowerPot")
                .putInt("x", 10)
                .putInt("y", 64)
                .putInt("z", -5)
                .putBoolean("isMovable", true)
                .putString("CustomName", "Pot")
                .putCompound("PlantBlock", BlockPalette.INSTANCE.getSerialized(POPPY))
                .putInt("Mystery", 7)
                .build();
    }

    @Test
    void roundTripPreservesKeysAndTypes() {
        assertEquals(FlowerPotData.class,
                CloudBlockEntityRegistry.get().getDataClass(BlockEntityTypes.FLOWER_POT));

        FlowerPotBlockEntity entity = (FlowerPotBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.FLOWER_POT, POSITION);
        NbtMap input = flowerPotTag();
        entity.loadAdditionalData(input);

        assertEquals(POPPY, entity.getPlant());

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
    void airPlantIsKept() {
        BlockState air = BlockTestRegistry.get().getBlock(BlockTypes.AIR);
        NbtMap input = NbtMap.builder()
                .putString("id", "FlowerPot")
                .putInt("x", 10)
                .putInt("y", 64)
                .putInt("z", -5)
                .putCompound("PlantBlock", BlockPalette.INSTANCE.getSerialized(air))
                .build();

        FlowerPotBlockEntity entity = (FlowerPotBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.FLOWER_POT, POSITION);
        entity.loadAdditionalData(input);

        assertEquals(air, entity.getPlant());

        NbtMap saved = entity.getServerTag();
        assertEquals(input.getCompound("PlantBlock"), saved.getCompound("PlantBlock"));
        assertEquals(NbtType.COMPOUND, NbtType.byClass(saved.get("PlantBlock").getClass()));

        assertEquals(Set.of("id"), entity.getClientTag().keySet());
    }

    @Test
    void emptyPotOmitsPlantBlock() {
        FlowerPotBlockEntity entity = (FlowerPotBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.FLOWER_POT, POSITION);
        entity.loadAdditionalData(NbtMap.builder()
                .putString("id", "FlowerPot")
                .putInt("x", 10)
                .putInt("y", 64)
                .putInt("z", -5)
                .build());

        assertEquals(BlockStates.AIR, entity.getPlant());

        assertEquals(Set.of("id", "x", "y", "z", "isMovable"), entity.getServerTag().keySet());
        assertEquals(Set.of("id"), entity.getClientTag().keySet());
    }

    @Test
    void flowerPotBlockEntityKeepsNoDuplicateState() {
        DtoStateChecks.assertKeepsNoDuplicateState(FlowerPotBlockEntity.class, FlowerPotData.class);
    }
}
