package org.cloudburstmc.server.item.serializer;

import com.google.common.base.Preconditions;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.item.ItemKeys;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemStackBuilder;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.api.util.data.TreeSpecies;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;

import java.util.Map;

public class TreeSpeciesSerializer extends DefaultItemSerializer {

    private static final Map<Class<?>, Object> DEFAULT_VALUES;

    public static final TreeSpeciesSerializer DOOR = new TreeSpeciesSerializer(ItemTypes.WOODEN_DOOR.getId(), ItemTypes.SPRUCE_DOOR.getId(), ItemTypes.BIRCH_DOOR.getId(), ItemTypes.JUNGLE_DOOR.getId(), ItemTypes.ACACIA_DOOR.getId(), ItemTypes.DARK_OAK_DOOR.getId(), ItemTypes.CRIMSON_DOOR.getId(), ItemTypes.WARPED_DOOR.getId(), ItemTypes.MANGROVE_DOOR.getId());
    public static final TreeSpeciesSerializer SIGN = new TreeSpeciesSerializer(ItemTypes.OAK_SIGN.getId(), ItemTypes.SPRUCE_SIGN.getId(), ItemTypes.BIRCH_SIGN.getId(), ItemTypes.JUNGLE_SIGN.getId(), ItemTypes.ACACIA_SIGN.getId(), ItemTypes.DARK_OAK_SIGN.getId(), ItemTypes.CRIMSON_SIGN.getId(), ItemTypes.WARPED_SIGN.getId(), ItemTypes.MANGROVE_SIGN.getId());
    public static final TreeSpeciesSerializer BOAT = new TreeSpeciesSerializer(ItemTypes.OAK_BOAT.getId(), ItemTypes.SPRUCE_BOAT.getId(), ItemTypes.BIRCH_BOAT.getId(), ItemTypes.JUNGLE_BOAT.getId(), ItemTypes.ACACIA_BOAT.getId(), ItemTypes.DARK_OAK_BOAT.getId(), ItemTypes.OAK_BOAT.getId(), ItemTypes.OAK_BOAT.getId(), ItemTypes.MANGROVE_BOAT.getId());

    public static final TreeSpeciesSerializer CHEST_BOAT = new TreeSpeciesSerializer(ItemTypes.OAK_CHEST_BOAT.getId(), ItemTypes.SPRUCE_CHEST_BOAT.getId(), ItemTypes.BIRCH_CHEST_BOAT.getId(), ItemTypes.JUNGLE_CHEST_BOAT.getId(), ItemTypes.ACACIA_CHEST_BOAT.getId(), ItemTypes.DARK_OAK_CHEST_BOAT.getId(), ItemTypes.OAK_CHEST_BOAT.getId(), ItemTypes.OAK_CHEST_BOAT.getId(), ItemTypes.MANGROVE_CHEST_BOAT.getId());

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
