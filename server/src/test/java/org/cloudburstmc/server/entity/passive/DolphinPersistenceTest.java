package org.cloudburstmc.server.entity.passive;

import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.server.testutil.PersistenceFixtures;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DolphinPersistenceTest {

    @Test
    void savesMoistnessAndRestoresTheDefaultWhenAbsent() {
        EntityDolphin dolphin = createDolphin();
        int defaultMoistness = dolphin.getMoistness();
        dolphin.setMoistness(17);

        NbtMapBuilder tag = NbtMap.builder();
        dolphin.saveAdditionalData(tag);
        EntityDolphin reloaded = createDolphin();
        reloaded.loadAdditionalData(tag.build());
        assertEquals(17, reloaded.getMoistness());

        reloaded.loadAdditionalData(NbtMap.EMPTY);
        assertEquals(defaultMoistness, reloaded.getMoistness());
    }

    @Test
    void rejectsNegativeMoistnessAndClampsInvalidSavedValues() {
        EntityDolphin dolphin = createDolphin();
        assertThrows(IllegalArgumentException.class, () -> dolphin.setMoistness(-1));

        dolphin.loadAdditionalData(NbtMap.builder().putInt("Moistness", -1).build());
        assertEquals(0, dolphin.getMoistness());
    }

    private static EntityDolphin createDolphin() {
        return assertInstanceOf(EntityDolphin.class, PersistenceFixtures.entity(EntityTypes.DOLPHIN));
    }
}
