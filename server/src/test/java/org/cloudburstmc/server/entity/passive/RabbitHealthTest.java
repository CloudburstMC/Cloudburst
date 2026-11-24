package org.cloudburstmc.server.entity.passive;

import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.server.testutil.PersistenceFixtures;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RabbitHealthTest {

    @Test
    void initializesWithThreeHealth() {
        EntityRabbit rabbit = rabbit();

        assertEquals(3, rabbit.getMaxHealth());
        assertEquals(3f, rabbit.getHealth());
    }

    @Test
    void loadingOldHealthCapsItAtTheRabbitMaximum() {
        EntityRabbit rabbit = rabbit();

        rabbit.loadAdditionalData(NbtMap.builder().putFloat("Health", 10f).build());

        assertEquals(3, rabbit.getMaxHealth());
        assertEquals(3f, rabbit.getHealth());
    }

    @Test
    void injuredHealthSurvivesRoundTrip() {
        EntityRabbit rabbit = rabbit();
        rabbit.setHealth(1.5f);
        NbtMapBuilder saved = NbtMap.builder();
        rabbit.saveAdditionalData(saved);
        EntityRabbit restored = rabbit();

        restored.loadAdditionalData(saved.build());

        assertEquals(3, restored.getMaxHealth());
        assertEquals(1.5f, restored.getHealth());
    }

    private static EntityRabbit rabbit() {
        return (EntityRabbit) PersistenceFixtures.entity(EntityTypes.RABBIT);
    }
}
