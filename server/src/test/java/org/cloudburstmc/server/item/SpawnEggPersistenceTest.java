package org.cloudburstmc.server.item;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemType;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.server.testutil.PersistenceFixtures;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class SpawnEggPersistenceTest {

    @Test
    void specializedSpiderEggKeepsItsIdentity() {
        assertEggName(ItemTypes.CAVE_SPIDER_SPAWN_EGG, EntityTypes.CAVE_SPIDER);
        assertEggName(ItemTypes.SPIDER_SPAWN_EGG, EntityTypes.SPIDER);
    }

    @Test
    void elderGuardianEggKeepsItsIdentity() {
        assertEggName(ItemTypes.ELDER_GUARDIAN_SPAWN_EGG, EntityTypes.ELDER_GUARDIAN);
        assertEggName(ItemTypes.GUARDIAN_SPAWN_EGG, EntityTypes.GUARDIAN);
    }

    @Test
    void zombieVariantsKeepTheirEggIdentity() {
        assertEggName(ItemTypes.DROWNED_SPAWN_EGG, EntityTypes.DROWNED);
        assertEggName(ItemTypes.HUSK_SPAWN_EGG, EntityTypes.HUSK);
        assertEggName(ItemTypes.ZOMBIE_VILLAGER_SPAWN_EGG, EntityTypes.ZOMBIE_VILLAGER);
        assertEggName(ItemTypes.ZOMBIE_SPAWN_EGG, EntityTypes.ZOMBIE);
    }

    @Test
    void passiveVariantsKeepTheirEggIdentity() {
        assertEggName(ItemTypes.CAMEL_HUSK_SPAWN_EGG, EntityTypes.CAMEL_HUSK);
        assertEggName(ItemTypes.GLOW_SQUID_SPAWN_EGG, EntityTypes.GLOW_SQUID);
        assertEggName(ItemTypes.TRADER_LLAMA_SPAWN_EGG, EntityTypes.TRADER_LLAMA);
    }

    @Test
    void cowEggDoesNotBecomeABatEgg() {
        assertEggName(ItemTypes.COW_SPAWN_EGG, EntityTypes.COW);
        assertEggName(ItemTypes.BAT_SPAWN_EGG, EntityTypes.BAT);
    }

    private static void assertEggName(ItemType eggType, EntityType<?> entityType) {
        PersistenceFixtures.ensureInitialized();
        ItemStack item = ItemStack.builder(eggType)
                .setData(ItemDataComponents.SPAWN_EGG_TYPE, entityType)
                .build();
        NbtMap tag = ItemStackNbtCodec.serializeItem(item);
        assertEquals(eggType.getId().toString(), tag.getString("Name"));

        ItemStack restored = ItemStackNbtCodec.deserializeItem(tag);
        assertSame(eggType, restored.getType());
        assertSame(entityType, restored.get(ItemDataComponents.SPAWN_EGG_TYPE));
    }
}
