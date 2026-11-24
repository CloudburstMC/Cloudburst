package org.cloudburstmc.server.entity;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.Explosive;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.server.testutil.PersistenceFixtures;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExplosivePersistenceTest {

    @Test
    void defaultsPreserveExplosionBehaviorAndSaveFormat() {
        Explosive tnt = PersistenceFixtures.entity(EntityTypes.TNT);
        Explosive minecart = PersistenceFixtures.entity(EntityTypes.TNT_MINECART);
        Explosive fireball = PersistenceFixtures.entity(EntityTypes.LARGE_FIREBALL);

        assertDefaults(tnt, 4, false);
        assertDefaults(minecart, 4, false);
        assertDefaults(fireball, 1, true);
    }

    @Test
    void configuredSettingsSurviveReload() {
        for (EntityType<? extends Explosive> type : List.<EntityType<? extends Explosive>>of(
                EntityTypes.TNT, EntityTypes.TNT_MINECART, EntityTypes.LARGE_FIREBALL)) {
            Explosive entity = PersistenceFixtures.entity(type);
            boolean incendiary = !entity.isIncendiary();
            entity.setYield(2.5f);
            entity.setIsIncendiary(incendiary);

            Explosive restored = PersistenceFixtures.entity(type);
            ((CloudEntity) restored).loadAdditionalData(save(entity));

            assertEquals(2.5f, restored.getYield());
            assertEquals(incendiary, restored.isIncendiary());
        }
    }

    @Test
    void invalidRadiusDoesNotReplaceTheConfiguredYield() {
        Explosive entity = PersistenceFixtures.entity(EntityTypes.TNT);
        entity.setYield(2.5f);

        assertThrows(IllegalArgumentException.class, () -> entity.setYield(-1));
        assertThrows(IllegalArgumentException.class, () -> entity.setYield(Float.NaN));
        assertThrows(IllegalArgumentException.class, () -> entity.setYield(Float.POSITIVE_INFINITY));
        assertEquals(2.5f, entity.getYield());
    }

    private static void assertDefaults(Explosive entity, float yield, boolean incendiary) {
        assertEquals(yield, entity.getYield());
        assertEquals(incendiary, entity.isIncendiary());

        NbtMap tag = save(entity);
        assertFalse(tag.containsKey("ExplosionYield"));
        assertFalse(tag.containsKey("Incendiary"));
    }

    private static NbtMap save(Explosive entity) {
        NbtMapBuilder builder = NbtMap.builder();
        ((CloudEntity) entity).saveAdditionalData(builder);
        return builder.build();
    }
}
