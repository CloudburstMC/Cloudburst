package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.blockentity.BlockEntityTypes;
import org.cloudburstmc.api.blockentity.data.BannerData;
import org.cloudburstmc.api.util.data.BannerPattern;
import org.cloudburstmc.api.util.data.DyeColor;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.registry.CloudBlockEntityRegistry;
import org.cloudburstmc.server.testutil.PersistenceFixtures;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Round-trip tests for the banner data path: every key today's load/saveClientData
 * handle, plus an unknown key, survive with the same NBT types.
 */
class BannerPersistenceTest {

    private static final Vector3i POSITION = Vector3i.from(10, 64, -5);

    private static NbtMap bannerTag() {
        return NbtMap.builder()
                .putString("id", "Banner")
                .putInt("x", 10)
                .putInt("y", 64)
                .putInt("z", -5)
                .putBoolean("isMovable", true)
                .putString("CustomName", "Banner")
                .putInt("Base", 1)
                .putInt("Type", 0)
                .putList("Patterns", NbtType.COMPOUND, List.of(
                        NbtMap.builder().putInt("Color", 11).putString("Pattern", "cs").build(),
                        NbtMap.builder().putInt("Color", 0).putString("Pattern", "bo").build()))
                .putInt("Mystery", 7)
                .build();
    }

    @Test
    void roundTripPreservesKeysAndTypes() {
        assertEquals(BannerData.class,
                CloudBlockEntityRegistry.get().getDataClass(BlockEntityTypes.BANNER));

        BannerBlockEntity entity = (BannerBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.BANNER, POSITION);
        NbtMap input = bannerTag();
        entity.loadAdditionalData(input);

        assertEquals(DyeColor.RED, entity.getBase());
        assertEquals(0, entity.getBannerType());
        assertEquals(List.of(
                new BannerPattern(BannerPattern.Type.getByName("cs"), DyeColor.YELLOW),
                new BannerPattern(BannerPattern.Type.getByName("bo"), DyeColor.BLACK)),
                entity.getPatterns());

        NbtMap saved = entity.getServerTag();
        Set<String> expectedKeys = new HashSet<>(input.keySet());
        expectedKeys.addAll(Set.of("id", "x", "y", "z"));
        assertEquals(expectedKeys, saved.keySet());
        for (String key : input.keySet()) {
            assertEquals(input.get(key), saved.get(key), "value of " + key);
            assertEquals(NbtType.byClass(input.get(key).getClass()),
                    NbtType.byClass(saved.get(key).getClass()), "type of " + key);
        }

        NbtMap client = entity.getClientTag();
        assertEquals(Set.of("id", "Base", "Type", "Patterns"), client.keySet());
        for (String key : Set.of("Base", "Type", "Patterns")) {
            assertEquals(input.get(key), client.get(key), "client value of " + key);
            assertEquals(NbtType.byClass(input.get(key).getClass()),
                    NbtType.byClass(saved.get(key).getClass()), "client type of " + key);
        }
    }

    @Test
    void emptyBannerWritesBaseAndTypeWithoutPatterns() {
        BannerBlockEntity entity = (BannerBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.BANNER, POSITION);
        entity.loadAdditionalData(NbtMap.builder()
                .putString("id", "Banner")
                .putInt("x", 10)
                .putInt("y", 64)
                .putInt("z", -5)
                .build());

        assertEquals(DyeColor.WHITE, entity.getBase());
        assertTrue(entity.getPatterns().isEmpty());

        NbtMap saved = entity.getServerTag();
        assertEquals(Set.of("id", "x", "y", "z", "isMovable", "Base", "Type"), saved.keySet());
        assertEquals(DyeColor.WHITE.getDyeData(), saved.getInt("Base"));
        assertEquals(NbtType.INT, NbtType.byClass(saved.get("Base").getClass()));

        NbtMap client = entity.getClientTag();
        assertEquals(Set.of("id", "Base", "Type"), client.keySet());
    }
}
