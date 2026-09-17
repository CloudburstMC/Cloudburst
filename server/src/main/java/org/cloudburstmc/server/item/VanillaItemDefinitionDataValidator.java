package org.cloudburstmc.server.item;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.item.ItemDefinitionComponents;
import org.cloudburstmc.api.registry.RegistryException;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.nbt.NbtMap;

import java.util.Set;

/**
 * Validates the structure and component names of vanilla item-definition data.
 */
@UtilityClass
public class VanillaItemDefinitionDataValidator {

    private static final String COMPONENTS_KEY = "components";

    // Present in vanilla network definitions but absent from the behavior-pack component schema.
    private static final Set<String> NETWORK_ONLY_COMPONENTS = Set.of(
            "item_properties",
            "item_tags",
            "minecraft:block",
            "minecraft:camera",
            "minecraft:foil",
            "minecraft:publisher_on_use_on",
            "minecraft:seed",
            "minecraft:use_duration"
    );

    public static void validate(Identifier itemId, NbtMap componentData) {
        if (componentData.isEmpty()) {
            return;
        }

        if (componentData.size() != 1 || !componentData.containsKey(COMPONENTS_KEY)) {
            throw new RegistryException("Invalid item-definition data envelope on " + itemId);
        }

        Object value = componentData.get(COMPONENTS_KEY);
        if (!(value instanceof NbtMap components)) {
            throw new RegistryException("Item-definition components must be a compound on " + itemId);
        }

        for (String name : components.keySet()) {
            if (NETWORK_ONLY_COMPONENTS.contains(name)) {
                continue;
            }

            Identifier componentId;
            try {
                componentId = Identifier.parse(name);
            } catch (IllegalArgumentException exception) {
                throw new RegistryException("Invalid item-definition component '" + name + "' on " + itemId, exception);
            }

            if (ItemDefinitionComponents.get(componentId).isEmpty()) {
                throw new RegistryException("Unknown item-definition component '" + name + "' on " + itemId);
            }
        }
    }
}
