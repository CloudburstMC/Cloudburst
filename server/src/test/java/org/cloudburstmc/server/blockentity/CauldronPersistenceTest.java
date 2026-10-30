package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.blockentity.BlockEntityTypes;
import org.cloudburstmc.api.blockentity.CauldronContents;
import org.cloudburstmc.api.blockentity.data.CauldronData;
import org.cloudburstmc.api.potion.PotionForm;
import org.cloudburstmc.api.potion.PotionTypes;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.network.NetworkUtils;
import org.cloudburstmc.server.registry.CloudBlockEntityRegistry;
import org.cloudburstmc.server.testutil.PersistenceFixtures;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Round-trip tests for the cauldron data path: every key today's load/saveClientData
 * handle, plus an unknown key, survive with the same NBT types.
 */
class CauldronPersistenceTest {

    private static final Vector3i POSITION = Vector3i.from(10, 64, -5);

    private static NbtMap cauldronTag() {
        return NbtMap.builder()
                .putString("id", "Cauldron")
                .putInt("x", 10)
                .putInt("y", 64)
                .putInt("z", -5)
                .putBoolean("isMovable", true)
                .putString("CustomName", "Cauldron")
                .putShort("PotionId", NetworkUtils.potionToNetwork(PotionTypes.WATER))
                .putShort("PotionType", (short) 0)
                .putList("Items", NbtType.END, List.of())
                .putInt("CustomColor", 0xFF2828FF)
                .putInt("Mystery", 7)
                .build();
    }

    @Test
    void roundTripPreservesKeysAndTypes() {
        assertEquals(CauldronData.class,
                CloudBlockEntityRegistry.get().getDataClass(BlockEntityTypes.CAULDRON));

        CauldronBlockEntity entity = (CauldronBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.CAULDRON, POSITION);
        NbtMap input = cauldronTag();
        entity.loadAdditionalData(input);

        assertEquals(new CauldronContents(PotionTypes.WATER, PotionForm.DRINKABLE, null), entity.getContents());

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
        assertEquals(Set.of("id", "PotionId", "PotionType", "Items", "CustomColor"), client.keySet());
        for (String key : Set.of("PotionId", "PotionType", "Items", "CustomColor")) {
            assertEquals(input.get(key), client.get(key), "client value of " + key);
            assertEquals(NbtType.byClass(input.get(key).getClass()),
                    NbtType.byClass(client.get(key).getClass()), "client type of " + key);
        }
    }

    @Test
    void emptyCauldronOmitsCustomColor() {
        CauldronBlockEntity entity = (CauldronBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.CAULDRON, POSITION);
        entity.loadAdditionalData(NbtMap.builder()
                .putString("id", "Cauldron")
                .putInt("x", 10)
                .putInt("y", 64)
                .putInt("z", -5)
                .build());

        assertEquals(CauldronContents.PLAIN, entity.getContents());

        NbtMap saved = entity.getServerTag();
        assertEquals(Set.of("id", "x", "y", "z", "isMovable", "PotionId", "PotionType", "Items"), saved.keySet());
        assertEquals((short) -1, saved.getShort("PotionId"));
        assertEquals((short) -1, saved.getShort("PotionType"));
        assertEquals(NbtType.SHORT, NbtType.byClass(saved.get("PotionId").getClass()));
        assertEquals(List.of(), saved.get("Items"));

        assertEquals(Set.of("id", "PotionId", "PotionType", "Items"), entity.getClientTag().keySet());
    }

    @Test
    void splashPotionKeepsForm() {
        CauldronBlockEntity entity = (CauldronBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.CAULDRON, POSITION);
        entity.loadAdditionalData(NbtMap.builder()
                .putString("id", "Cauldron")
                .putInt("x", 10)
                .putInt("y", 64)
                .putInt("z", -5)
                .putShort("PotionId", NetworkUtils.potionToNetwork(PotionTypes.NIGHT_VISION))
                .putShort("PotionType", (short) 1)
                .build());

        assertEquals(PotionForm.SPLASH, entity.getContents().form());

        NbtMap saved = entity.getServerTag();
        assertEquals(NetworkUtils.potionToNetwork(PotionTypes.NIGHT_VISION), saved.getShort("PotionId"));
        assertEquals((short) 1, saved.getShort("PotionType"));
        assertNull(saved.get("IsSplash"));
        assertEquals(NbtType.INT, NbtType.byClass(saved.get("CustomColor").getClass()));
    }

    @Test
    void cauldronBlockEntityKeepsNoDuplicateState() {
        DtoStateChecks.assertKeepsNoDuplicateState(CauldronBlockEntity.class, CauldronData.class);
    }
}
