package org.cloudburstmc.server.entity.passive;

import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.data.CreatureData;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.registry.CloudEntityRegistry;
import org.cloudburstmc.server.testutil.PersistenceFixtures;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Round-trip tests for the villager data path: every key today's load/save handle,
 * plus an unknown key, survive with the same NBT types.
 */
class VillagerPersistenceTest {

    private static NbtMap save(CloudEntity entity) {
        NbtMapBuilder builder = NbtMap.builder();
        entity.saveAdditionalData(builder);
        return builder.build();
    }

    @Test
    void roundTripPreservesKeysAndTypes() {
        assertEquals(CreatureData.class, CloudEntityRegistry.get().getDataClass(EntityTypes.VILLAGER));

        EntityVillager villager = (EntityVillager) PersistenceFixtures.entity(EntityTypes.VILLAGER);
        villager.loadAdditionalData(NbtMap.builder()
                .putInt("Profession", 2)
                .putInt("Mystery", 7)
                .build());

        assertEquals(2, villager.getProfession());

        NbtMap saved = save(villager);

        assertTrue(saved.get("Profession") instanceof Integer, "Profession must be an int");
        assertEquals(2, saved.getInt("Profession"));
        assertEquals(7, saved.getInt("Mystery"));
    }

    @Test
    void defaultInstanceSavesTodayDefaults() {
        EntityVillager villager = (EntityVillager) PersistenceFixtures.entity(EntityTypes.VILLAGER);

        NbtMap saved = save(villager);

        // The constructor defaults the profession to generic (5), which today's save writes.
        assertTrue(saved.get("Profession") instanceof Integer, "Profession must be an int");
        assertEquals(EntityVillager.PROFESSION_GENERIC, saved.getInt("Profession"));
    }

    @Test
    void absentKeyLeavesStateUnchanged() {
        EntityVillager villager = (EntityVillager) PersistenceFixtures.entity(EntityTypes.VILLAGER);
        villager.setProfession(3);

        villager.loadAdditionalData(NbtMap.EMPTY);

        assertEquals(3, villager.getProfession());
    }

    @Test
    void villagerKeepsNoDuplicateState() {
        org.cloudburstmc.server.blockentity.DtoStateChecks.assertEntityKeepsNoDuplicateState(
                EntityVillager.class, CreatureData.class,
                java.util.Map.of(
                        "armor", "an armor view over the live container in data().getArmor()",
                        "offhand", "an offhand view over the live container in data().getOffhand()"));
    }
}
