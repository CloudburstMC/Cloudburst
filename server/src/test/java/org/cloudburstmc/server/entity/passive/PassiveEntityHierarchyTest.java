package org.cloudburstmc.server.entity.passive;

import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.passive.AbstractCow;
import org.cloudburstmc.api.entity.passive.AbstractHorse;
import org.cloudburstmc.api.entity.passive.Fish;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.server.testutil.PersistenceFixtures;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PassiveEntityHierarchyTest {

    @Test
    void horseFamilySharesAgeWithoutLosingSpeciesDimensions() {
        AbstractHorse horse = PersistenceFixtures.entity(EntityTypes.HORSE);
        AbstractHorse donkey = PersistenceFixtures.entity(EntityTypes.DONKEY);
        AbstractHorse camel = PersistenceFixtures.entity(EntityTypes.CAMEL);

        horse.setAge(-100);
        donkey.setAge(-100);

        assertInstanceOf(EntityAbstractHorse.class, camel);
        assertEquals(horse.getWidth(), donkey.getWidth());
        assertEquals(horse.getHeight(), donkey.getHeight());
        assertTrue(horse.isBaby());
        assertEquals(32, camel.getMaxHealth());
    }

    @Test
    void nautilusVariantsShareAdultDimensionsAndHealth() {
        EntityAbstractNautilus nautilus = assertInstanceOf(EntityAbstractNautilus.class,
                PersistenceFixtures.entity(EntityTypes.NAUTILUS));
        EntityAbstractNautilus zombie = assertInstanceOf(EntityAbstractNautilus.class,
                PersistenceFixtures.entity(EntityTypes.ZOMBIE_NAUTILUS));

        assertEquals(nautilus.getWidth(), zombie.getWidth());
        assertEquals(nautilus.getHeight(), zombie.getHeight());
        assertEquals(nautilus.getMaxHealth(), zombie.getMaxHealth());
    }

    @Test
    void cowsAndMooshroomsUseTheSameFamilyBase() {
        AbstractCow cow = PersistenceFixtures.entity(EntityTypes.COW);
        AbstractCow mooshroom = PersistenceFixtures.entity(EntityTypes.MOOSHROOM);

        assertInstanceOf(EntityAbstractCow.class, cow);
        assertInstanceOf(EntityAbstractCow.class, mooshroom);
        assertEquals(cow.getWidth(), mooshroom.getWidth());
        assertEquals(cow.getHeight(), mooshroom.getHeight());
        assertEquals(cow.getMaxHealth(), mooshroom.getMaxHealth());
    }

    @Test
    void tadpolesUseTheFishContract() {
        Fish tadpole = PersistenceFixtures.entity(EntityTypes.TADPOLE);

        assertSame(ItemTypes.TADPOLE_BUCKET, tadpole.getBaseBucketItem().getType());
    }
}
