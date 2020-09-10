package org.cloudburstmc.server.item.mapper;

import lombok.val;
import org.cloudburstmc.server.item.ItemIds;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.utils.Identifier;
import org.cloudburstmc.server.utils.data.TreeSpecies;

public class TreeSpiecesMapper implements ItemTypeMapper {

    public static TreeSpiecesMapper SIGN_CONVERTER = new TreeSpiecesMapper(
            ItemIds.SIGN,
            ItemIds.SPRUCE_SIGN,
            ItemIds.BIRCH_SIGN,
            ItemIds.JUNGLE_SIGN,
            ItemIds.ACACIA_SIGN,
            ItemIds.DARK_OAK_SIGN,
            ItemIds.CRIMSON_SIGN,
            ItemIds.WARPED_SIGN
    );

    public static TreeSpiecesMapper DOOR_CONVERTER = new TreeSpiecesMapper(
            ItemIds.WOODEN_DOOR,
            ItemIds.SPRUCE_DOOR,
            ItemIds.BIRCH_DOOR,
            ItemIds.JUNGLE_DOOR,
            ItemIds.ACACIA_DOOR,
            ItemIds.DARK_OAK_DOOR,
            ItemIds.CRIMSON_DOOR,
            ItemIds.WARPED_DOOR
    );

    private final Identifier[] identifiers;

    public TreeSpiecesMapper(Identifier... identifiers) {
        this.identifiers = identifiers;
    }

    @Override
    public Identifier convert(ItemStack item) {
        Identifier type = identifiers[0];

        val meta = item.getMetadata(TreeSpecies.class);
        if (meta.isPresent()) {
            type = identifiers[meta.get().ordinal()];
        }

        return type;
    }
}
