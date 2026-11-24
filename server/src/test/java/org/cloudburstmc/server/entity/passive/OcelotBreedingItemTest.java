package org.cloudburstmc.server.entity.passive;

import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemType;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.server.testutil.PersistenceFixtures;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OcelotBreedingItemTest {

    @Test
    void acceptsRawCodAndSalmon() {
        Animal ocelot = (EntityOcelot) PersistenceFixtures.entity(EntityTypes.OCELOT);

        assertTrue(ocelot.isBreedingItem(ItemStack.from(ItemTypes.COD)));
        assertTrue(ocelot.isBreedingItem(ItemStack.from(ItemTypes.SALMON)));
    }

    @Test
    void rejectsCookedFishOtherFishAndDefaultAnimalFood() {
        Animal ocelot = (EntityOcelot) PersistenceFixtures.entity(EntityTypes.OCELOT);

        for (ItemType itemType : new ItemType[]{ItemTypes.COOKED_COD, ItemTypes.COOKED_SALMON,
                ItemTypes.PUFFERFISH, ItemTypes.TROPICAL_FISH, ItemTypes.WHEAT}) {
            assertFalse(ocelot.isBreedingItem(ItemStack.from(itemType)),
                    () -> "Ocelots must reject " + itemType.getId());
        }
    }
}
