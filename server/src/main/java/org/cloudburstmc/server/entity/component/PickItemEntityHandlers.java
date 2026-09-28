package org.cloudburstmc.server.entity.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.entity.component.PickItemEntityHandler;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.api.util.data.TreeSpecies;
import org.cloudburstmc.server.entity.vehicle.EntityBoat;

import java.util.Locale;

@UtilityClass
public class PickItemEntityHandlers {
    public static final PickItemEntityHandler BOAT = (entity, includeData) -> boatItem((EntityBoat) entity, "_boat");

    public static final PickItemEntityHandler CHEST_BOAT = (entity, includeData) -> boatItem((EntityBoat) entity, "_chest_boat");

    private static ItemStack boatItem(EntityBoat boat, String suffix) {
        TreeSpecies[] species = TreeSpecies.values();
        int woodType = boat.getWoodType();
        if (woodType < 0 || woodType >= species.length) {
            return ItemStack.EMPTY;
        }

        Identifier id = Identifier.parse(species[woodType].name().toLowerCase(Locale.ROOT) + suffix);
        return ItemTypes.get(id).map(ItemStack::from).orElse(ItemStack.EMPTY);
    }
}
