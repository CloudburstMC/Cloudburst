package org.cloudburstmc.server.item;

import org.cloudburstmc.api.item.ItemBehaviors;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.server.registry.CloudItemRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ItemUseBehaviorRegistrationTest {

    @Test
    public void distinguishesOptionalUseCallbacksFromTimedCompletion() {
        CloudItemRegistry items = CloudItemRegistry.get();

        assertNull(items.getComponent(ItemTypes.BOW, ItemBehaviors.USE_TICK));
        assertNull(items.getComponent(ItemTypes.TRIDENT, ItemBehaviors.USE_TICK));
        assertNotNull(items.getComponent(ItemTypes.CROSSBOW, ItemBehaviors.USE_TICK));

        assertNull(items.getComponent(ItemTypes.POTION, ItemBehaviors.RELEASE_USE));
        assertNull(items.getComponent(ItemTypes.CHORUS_FRUIT, ItemBehaviors.RELEASE_USE));
        assertNotNull(items.getComponent(ItemTypes.BOW, ItemBehaviors.RELEASE_USE));

        assertNotNull(items.requireComponent(ItemTypes.POTION, ItemBehaviors.FINISH_USE));
        assertNull(items.getComponent(ItemTypes.CROSSBOW, ItemBehaviors.FINISH_USE));
        assertNull(items.getComponent(ItemTypes.CROSSBOW, ItemBehaviors.RELEASE_USE));
    }

    @Test
    public void registersStabOnlyForPiercingWeapons() {
        CloudItemRegistry items = CloudItemRegistry.get();

        assertNotNull(items.requireComponent(ItemTypes.WOODEN_SPEAR, ItemBehaviors.STAB));
        assertNotNull(items.requireComponent(ItemTypes.NETHERITE_SPEAR, ItemBehaviors.STAB));
        assertNotNull(items.requireComponent(ItemTypes.WOODEN_SPEAR, ItemBehaviors.USE));
        assertNotNull(items.requireComponent(ItemTypes.WOODEN_SPEAR, ItemBehaviors.USE_TICK));
        assertNotNull(items.requireComponent(ItemTypes.WOODEN_SPEAR, ItemBehaviors.RELEASE_USE));
        assertNull(items.getComponent(ItemTypes.TRIDENT, ItemBehaviors.STAB));
    }
}
