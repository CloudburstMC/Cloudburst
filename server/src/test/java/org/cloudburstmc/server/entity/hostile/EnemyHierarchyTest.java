package org.cloudburstmc.server.entity.hostile;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.hostile.Enemy;
import org.cloudburstmc.api.entity.hostile.Monster;
import org.cloudburstmc.server.testutil.PersistenceFixtures;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EnemyHierarchyTest {

    @Test
    void monstersAreEnemies() {
        assertTrue(Enemy.class.isAssignableFrom(Monster.class));
        assertInstanceOf(Monster.class, PersistenceFixtures.entity(EntityTypes.ZOMBIE));
        assertInstanceOf(Monster.class, PersistenceFixtures.entity(EntityTypes.CAVE_SPIDER));
    }

    @Test
    void enemiesOutsideTheMonsterFamilyKeepTheirOwnTypes() {
        List<EntityType<?>> types = List.of(
                EntityTypes.ELDER_GUARDIAN_GHOST,
                EntityTypes.ENDER_DRAGON,
                EntityTypes.GHAST,
                EntityTypes.HOGLIN,
                EntityTypes.MAGMA_CUBE,
                EntityTypes.PHANTOM,
                EntityTypes.SHULKER,
                EntityTypes.SLIME);

        for (EntityType<?> type : types) {
            Class<?> entityClass = type.getEntityClass();
            assertTrue(Enemy.class.isAssignableFrom(entityClass), type.getId().toString());
            assertFalse(Monster.class.isAssignableFrom(entityClass), type.getId().toString());

            if (type != EntityTypes.ENDER_DRAGON) {
                Enemy enemy = assertInstanceOf(Enemy.class, PersistenceFixtures.entity(type), type.getId().toString());
                assertFalse(Monster.class.isInstance(enemy), type.getId().toString());
            }
        }

        assertTrue(Enemy.class.isAssignableFrom(EntityEnderDragon.class));
        assertFalse(Monster.class.isAssignableFrom(EntityEnderDragon.class));
        assertFalse(Enemy.class.isAssignableFrom(EntityTypes.SULFUR_CUBE.getEntityClass()));
        assertFalse(PersistenceFixtures.entity(EntityTypes.SULFUR_CUBE) instanceof Enemy);
    }
}
