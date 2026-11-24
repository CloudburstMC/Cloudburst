package org.cloudburstmc.server.entity.misc;

import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.server.testutil.PersistenceFixtures;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OminousItemSpawnerPersistenceTest {

    @Test
    void savesThePayloadAndRemainingDelay() {
        EntityOminousItemSpawner spawner = createSpawner();
        ItemStack item = ItemStack.from(ItemTypes.DIAMOND, 2);
        spawner.setItem(item);
        spawner.setTicksUntilSpawn(17);

        NbtMapBuilder tag = NbtMap.builder();
        spawner.saveAdditionalData(tag);
        EntityOminousItemSpawner reloaded = createSpawner();
        reloaded.loadAdditionalData(tag.build());

        assertEquals(item, reloaded.getItem());
        assertEquals(17, reloaded.getTicksUntilSpawn());
    }

    @Test
    void rejectsInvalidDelayButAllowsImmediateRelease() {
        EntityOminousItemSpawner spawner = createSpawner();
        assertThrows(IllegalArgumentException.class, () -> spawner.setTicksUntilSpawn(-1));
        spawner.setTicksUntilSpawn(0);
        assertEquals(0, spawner.getTicksUntilSpawn());
    }

    private static EntityOminousItemSpawner createSpawner() {
        return assertInstanceOf(EntityOminousItemSpawner.class,
                PersistenceFixtures.entity(EntityTypes.OMINOUS_ITEM_SPAWNER));
    }
}
