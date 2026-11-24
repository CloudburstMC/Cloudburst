package org.cloudburstmc.server.entity.passive;

import org.cloudburstmc.api.entity.Breedable;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.event.entity.EntityBreedEvent;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.server.testutil.PersistenceFixtures;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AnimalBreedingTest {

    @Test
    void sheepAndCattleShareFoodAndCooldownRules() {
        List<Breedable> animals = List.of(PersistenceFixtures.entity(EntityTypes.SHEEP),
                PersistenceFixtures.entity(EntityTypes.COW), PersistenceFixtures.entity(EntityTypes.MOOSHROOM));
        for (Breedable animal : animals) {
            assertTrue(animal.isBreedingItem(ItemStack.from(ItemTypes.WHEAT)));
            assertFalse(animal.isBreedingItem(ItemStack.from(ItemTypes.DIAMOND)));
            assertTrue(animal.canBreed());

            animal.setAge(6000);
            assertFalse(animal.canBreed());
            animal.setAge(-24000);
            assertTrue(animal.isBaby());
            assertFalse(animal.canBreed());
        }
    }

    @Test
    void feedingAcceleratesGrowthWithoutCrossingIntoCooldown() {
        assertEquals(-21_600, AnimalBreeding.ageAfterFeeding(-24_000));
        assertEquals(0, AnimalBreeding.ageAfterFeeding(-1));
    }

    @Test
    void breedEventExposesParentsAndRejectsNegativeExperience() {
        Breedable first = PersistenceFixtures.entity(EntityTypes.SHEEP);
        Breedable second = PersistenceFixtures.entity(EntityTypes.SHEEP);
        Breedable offspring = PersistenceFixtures.entity(EntityTypes.SHEEP);
        ItemStack food = ItemStack.from(ItemTypes.WHEAT);
        EntityBreedEvent event = new EntityBreedEvent(offspring, first, second, null, food, 3);

        assertSame(offspring, event.getEntity());
        assertSame(first, event.getFirstParent());
        assertSame(second, event.getSecondParent());
        assertSame(food, event.getBredWith());
        assertThrows(IllegalArgumentException.class, () -> event.setExperience(-1));
        event.setCancelled();
        assertTrue(event.isCancelled());
    }
}
