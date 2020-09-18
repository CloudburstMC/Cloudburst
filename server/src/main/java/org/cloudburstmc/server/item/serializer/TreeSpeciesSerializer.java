package org.cloudburstmc.server.item.serializer;

import com.google.common.base.Preconditions;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import lombok.val;
import org.cloudburstmc.server.item.CloudItemStack;
import org.cloudburstmc.server.item.CloudItemStackBuilder;
import org.cloudburstmc.server.item.ItemIds;
import org.cloudburstmc.server.utils.Identifier;
import org.cloudburstmc.server.utils.data.TreeSpecies;

public class TreeSpeciesSerializer extends DefaultItemSerializer {

    public static final TreeSpeciesSerializer DOOR = new TreeSpeciesSerializer(ItemIds.WOODEN_DOOR, ItemIds.SPRUCE_DOOR, ItemIds.BIRCH_DOOR, ItemIds.JUNGLE_DOOR, ItemIds.ACACIA_DOOR, ItemIds.DARK_OAK_DOOR, ItemIds.CRIMSON_DOOR, ItemIds.WARPED_DOOR);
    public static final TreeSpeciesSerializer SIGN = new TreeSpeciesSerializer(ItemIds.SIGN, ItemIds.SPRUCE_SIGN, ItemIds.BIRCH_SIGN, ItemIds.JUNGLE_SIGN, ItemIds.ACACIA_SIGN, ItemIds.DARK_OAK_SIGN, ItemIds.CRIMSON_SIGN, ItemIds.WARPED_SIGN);

    private final Identifier[] identifiers;
    private final Reference2ObjectMap<Identifier, TreeSpecies> dataMap = new Reference2ObjectOpenHashMap<>();

    public TreeSpeciesSerializer(Identifier... identifiers) {
        Preconditions.checkNotNull(identifiers, "identifiers");
        Preconditions.checkArgument(identifiers.length == TreeSpecies.values().length, "Invalid amount of identifiers provided");

        this.identifiers = identifiers;
        val values = TreeSpecies.values();
        for (int i = 0; i < values.length; i++) {
            dataMap.put(identifiers[i], values[i]);
        }
    }

    @Override
    public void serialize(CloudItemStack item, NbtMapBuilder itemTag) {
        super.serialize(item, itemTag);
        itemTag.putString(NAME_TAG, identifiers[item.getMetadata(TreeSpecies.class, TreeSpecies.OAK).ordinal()].toString());
    }

    @Override
    public void deserialize(Identifier id, short meta, int amount, CloudItemStackBuilder builder, NbtMap tag) {
        super.deserialize(id, meta, amount, builder, tag);
        builder.itemData(dataMap.getOrDefault(id, TreeSpecies.OAK));
    }
}
