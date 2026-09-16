package org.cloudburstmc.server.item;

import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.Component;
import org.cloudburstmc.api.item.ItemComponents;
import org.cloudburstmc.api.item.ItemKeys;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.component.SpawnEggComponent;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.server.registry.CloudBlockRegistry;
import org.cloudburstmc.server.registry.CloudItemRegistry;

import static java.util.Objects.requireNonNull;

/**
 * Resolves item names for translatable messages.
 */
@UtilityClass
public class ItemDisplayNameResolver {

    /**
     * Returns the effective display name of an item stack.
     *
     * @param item item stack to name
     * @return custom or translated item name
     */
    public static Component resolve(ItemStack item) {
        requireNonNull(item, "item");

        String customName = item.get(ItemKeys.CUSTOM_NAME);
        if (customName != null) {
            return Component.text(customName);
        }

        if (item.isBlock()) {
            String translationKey = CloudBlockRegistry.REGISTRY
                    .getDefinition(item.requireBlockState())
                    .getTranslationKey();
            return Component.translatable(translationKey);
        }

        SpawnEggComponent spawnEgg = CloudItemRegistry.get().getComponent(item.getType(), ItemComponents.SPAWN_EGG);
        if (spawnEgg != null) {
            return Component.translatable("item.spawn_egg.entity." + spawnEgg.entityType().getId().getName() + ".name");
        }

        Identifier itemId = item.getType().getId();
        if ("minecraft".equals(itemId.getNamespace())) {
            return Component.translatable("item." + itemId.getName() + ".name");
        }

        return Component.text(itemId.toString());
    }
}
