package org.cloudburstmc.server.entity.vehicle;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.entity.vehicle.BoatType;
import org.cloudburstmc.api.item.ItemType;
import org.cloudburstmc.api.item.ItemTypes;

import java.util.List;

@UtilityClass
public class VanillaBoats {

    public static final List<Definition> DEFINITIONS = List.of(
            new Definition(BoatType.OAK, 0, ItemTypes.OAK_BOAT, ItemTypes.OAK_CHEST_BOAT),
            new Definition(BoatType.SPRUCE, 1, ItemTypes.SPRUCE_BOAT, ItemTypes.SPRUCE_CHEST_BOAT),
            new Definition(BoatType.BIRCH, 2, ItemTypes.BIRCH_BOAT, ItemTypes.BIRCH_CHEST_BOAT),
            new Definition(BoatType.JUNGLE, 3, ItemTypes.JUNGLE_BOAT, ItemTypes.JUNGLE_CHEST_BOAT),
            new Definition(BoatType.ACACIA, 4, ItemTypes.ACACIA_BOAT, ItemTypes.ACACIA_CHEST_BOAT),
            new Definition(BoatType.DARK_OAK, 5, ItemTypes.DARK_OAK_BOAT, ItemTypes.DARK_OAK_CHEST_BOAT),
            new Definition(BoatType.MANGROVE, 6, ItemTypes.MANGROVE_BOAT, ItemTypes.MANGROVE_CHEST_BOAT),
            new Definition(BoatType.BAMBOO, 7, ItemTypes.BAMBOO_RAFT, ItemTypes.BAMBOO_CHEST_RAFT),
            new Definition(BoatType.CHERRY, 8, ItemTypes.CHERRY_BOAT, ItemTypes.CHERRY_CHEST_BOAT),
            new Definition(BoatType.PALE_OAK, 9, ItemTypes.PALE_OAK_BOAT, ItemTypes.PALE_OAK_CHEST_BOAT),
            new Definition(BoatType.POPLAR, 10, ItemTypes.POPLAR_BOAT, ItemTypes.POPLAR_CHEST_BOAT)
    );

    public static Definition definition(BoatType type) {
        return DEFINITIONS.stream().filter(definition -> definition.type() == type).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown boat type " + type));
    }

    public static Definition fromVariant(int variant) {
        return DEFINITIONS.stream().filter(definition -> definition.variant() == variant).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown boat variant " + variant));
    }

    public record Definition(BoatType type, int variant, ItemType boatItem, ItemType chestItem) {
    }
}
