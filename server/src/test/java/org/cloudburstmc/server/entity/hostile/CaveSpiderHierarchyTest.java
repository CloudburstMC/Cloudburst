package org.cloudburstmc.server.entity.hostile;

import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.hostile.Spider;
import org.cloudburstmc.server.testutil.PersistenceFixtures;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class CaveSpiderHierarchyTest {

    @Test
    void caveSpiderRetainsItsSpecializedDimensionsAndHealth() {
        Spider spider = PersistenceFixtures.entity(EntityTypes.CAVE_SPIDER);
        EntityCaveSpider caveSpider = assertInstanceOf(EntityCaveSpider.class, spider);

        assertInstanceOf(EntitySpider.class, caveSpider);
        assertEquals(0.7f, caveSpider.getWidth());
        assertEquals(0.5f, caveSpider.getHeight());
        assertEquals(12, caveSpider.getMaxHealth());
    }
}
