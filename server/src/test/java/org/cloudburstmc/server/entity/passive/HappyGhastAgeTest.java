package org.cloudburstmc.server.entity.passive;

import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HappyGhastAgeTest {

    @Test
    void juvenileStateControlsScaleBoundsEyesAndUnderwaterBreathing() {
        EntityHappyGhast ghast = ghast();

        ghast.setAge(-24000);

        assertTrue(ghast.isBaby());
        assertEquals(-24000, ghast.getAge());
        assertEquals(0.2375f, ghast.getScale());
        assertEquals(0.95f, ghast.getBoundingBox().getMaxX() - ghast.getBoundingBox().getMinX(), 0.00001f);
        assertEquals(0.95f, ghast.getBoundingBox().getMaxY() - ghast.getBoundingBox().getMinY(), 0.00001f);
        assertEquals(0.46875f, ghast.getEyeHeight());
        assertTrue(ghast.canBreatheUnderwater());

        ghast.setAge(0);

        assertFalse(ghast.isBaby());
        assertEquals(1.0f, ghast.getScale());
        assertEquals(4.0f, ghast.getBoundingBox().getMaxX() - ghast.getBoundingBox().getMinX());
        assertEquals(2.6f, ghast.getEyeHeight());
        assertFalse(ghast.canBreatheUnderwater());
    }

    @Test
    void ageAndBabyStateSurviveSavingAndLoading() {
        EntityHappyGhast original = ghast();
        original.setAge(-12345);
        NbtMapBuilder saved = NbtMap.builder();
        original.saveAdditionalData(saved);

        EntityHappyGhast restored = ghast();
        restored.loadAdditionalData(saved.build());

        assertEquals(-12345, restored.getAge());
        assertTrue(restored.isBaby());
        assertEquals(0.95f, restored.getBoundingBox().getMaxX() - restored.getBoundingBox().getMinX(), 0.00001f);
    }

    private static EntityHappyGhast ghast() {
        Level level = InterfaceProxy.create(Level.class, Map.of());
        return new EntityHappyGhast(EntityTypes.HAPPY_GHAST, Location.from(level));
    }
}
