package org.cloudburstmc.server.item.serializer;

import com.google.common.base.Preconditions;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import org.cloudburstmc.api.item.ItemIds;
import org.cloudburstmc.api.item.ItemKeys;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemStackBuilder;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.api.util.data.TreeSpecies;

import java.util.Map;

public class TreeSpeciesSerializer extends DefaultItemSerializer {

    private static final Map<Class<?>, Object> DEFAULT_VALUES;

    public static final TreeSpeciesSerializer DOOR = new TreeSpeciesSerializer(ItemIds.WOODEN_DOOR, ItemIds.SPRUCE_DOOR, ItemIds.BIRCH_DOOR, ItemIds.JUNGLE_DOOR, ItemIds.ACACIA_DOOR, ItemIds.DARK_OAK_DOOR, ItemIds.CRIMSON_DOOR, ItemIds.WARPED_DOOR, ItemIds.MANGROVE_DOOR);
    public static final TreeSpeciesSerializer SIGN = new TreeSpeciesSerializer(ItemIds.SIGN, ItemIds.SPRUCE_SIGN, ItemIds.BIRCH_SIGN, ItemIds.JUNGLE_SIGN, ItemIds.ACACIA_SIGN, ItemIds.DARK_OAK_SIGN, ItemIds.CRIMSON_SIGN, ItemIds.WARPED_SIGN, ItemIds.MANGROVE_SIGN);
    public static final TreeSpeciesSerializer BOAT = new TreeSpeciesSerializer(ItemIds.OAK_BOAT, ItemIds.SPRUCE_BOAT, ItemIds.BIRCH_BOAT, ItemIds.JUNGLE_BOAT, ItemIds.ACACIA_BOAT, ItemIds.DARK_OAK_BOAT, ItemIds.BOAT, ItemIds.BOAT, ItemIds.MANGROVE_BOAT);

    public static final TreeSpeciesSerializer CHEST_BOAT = new TreeSpeciesSerializer(ItemIds.OAK_CHEST_BOAT, ItemIds.SPRUCE_CHEST_BOAT, ItemIds.BIRCH_CHEST_BOAT, ItemIds.JUNGLE_CHEST_BOAT, ItemIds.ACACIA_CHEST_BOAT, ItemIds.DARK_OAK_CHEST_BOAT, ItemIds.OAK_CHEST_BOAT, ItemIds.OAK_CHEST_BOAT, ItemIds.MANGROVE_CHEST_BOAT);

    private final Identifier[] identifiers;
    private final Reference2ObjectMap<Identifier, TreeSpecies> dataMap = new Reference2ObjectOpenHashMap<>();

    static {
        DEFAULT_VALUES = Map.of(TreeSpecies.class, TreeSpecies.OAK);
    }

    public TreeSpeciesSerializer(Identifier... identifiers) {
        Preconditions.checkNotNull(identifiers, "identifiers");
        Preconditions.checkArgument(identifiers.length == TreeSpecies.values().length, "Invalid amount of identifiers provided");

        this.identifiers = identifiers;
        var values = TreeSpecies.values();
        for (int i = 0; i < values.length; i++) {
            dataMap.put(identifiers[i], values[i]);
        }
    }

    @Override
    public void serialize(ItemStack item, NbtMapBuilder itemTag) {
        super.serialize(item, itemTag);
        itemTag.putString(NAME_TAG, identifiers[item.get(ItemKeys.TREE_SPECIES).ordinal()].toString());
    }

    @Override
    public void deserialize(Identifier id, short meta, ItemStackBuilder builder, NbtMap tag) {
        super.deserialize(id, meta, builder, tag);
        builder.data(ItemKeys.TREE_SPECIES, dataMap.getOrDefault(id, TreeSpecies.OAK));
    }

    @Override
    public Map<Class<?>, Object> getDefaultMetadataValues() {
        return DEFAULT_VALUES;
    }
}
