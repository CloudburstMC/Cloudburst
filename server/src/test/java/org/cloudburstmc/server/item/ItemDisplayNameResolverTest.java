package org.cloudburstmc.server.item;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemType;
import org.cloudburstmc.api.item.ItemTypes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class ItemDisplayNameResolverTest {

    @Test
    void resolvesSpawnEggNamesFromTheirRegisteredEntityType() {
        assertTranslationKey(ItemTypes.MOOSHROOM_SPAWN_EGG, "item.spawn_egg.entity.mooshroom.name");
        assertTranslationKey(ItemTypes.EVOKER_SPAWN_EGG, "item.spawn_egg.entity.evocation_illager.name");
        assertTranslationKey(ItemTypes.TROPICAL_FISH_SPAWN_EGG, "item.spawn_egg.entity.tropicalfish.name");
    }

    private static void assertTranslationKey(ItemType itemType, String expectedKey) {
        Component displayName = ItemDisplayNameResolver.resolve(ItemStack.from(itemType));
        TranslatableComponent translatable = assertInstanceOf(TranslatableComponent.class, displayName);
        assertEquals(expectedKey, translatable.key());
    }
}
