package org.cloudburstmc.server.item;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.item.*;
import org.cloudburstmc.api.registry.BlockRegistry;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.block.BlockPalette;
import org.cloudburstmc.server.block.util.BlockStateMetaMappings;
import org.cloudburstmc.server.registry.CloudItemRegistry;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;


/**
 * Reads and writes saved item stacks and their component tags.
 */
@UtilityClass
public class ItemStackNbtCodec {

    private static final Cache<ItemStack, NbtMap> SERIALIZED_TAGS =
            CacheBuilder.newBuilder().weakKeys().softValues().build();

    public static NbtMap serializeItem(ItemStack item) {
        if (item == null || item.isEmpty()) {
            return NbtMap.builder()
                    .putString("Name", "minecraft:air")
                    .putByte("Count", (byte) 0)
                    .putShort("Damage", (short) 0)
                    .build();
        }

        NbtMapBuilder nbtTag = NbtMap.builder();
        nbtTag.putString("Name", item.getType().getId().toString())
                .putByte("Count", (byte) item.getCount())
                .putShort("Damage", (short) CloudItemRegistry.get().getSerializer(item.getType()).getAuxValue(item));

        BlockState blockState = item.getBlockState().orElse(null);
        if (blockState != null) {
            NbtMapBuilder blockTag = NbtMap.builder();
            blockTag.putString("Name", BlockPalette.INSTANCE.getIdentifier(blockState).toString());
            blockTag.putShort("Damage", (short) 0);
            nbtTag.put("Block", blockTag.build());
        }

        if (item.has(ItemDataComponents.CAN_DESTROY)) {
            List<String> blocks = item.getOrDefault(ItemDataComponents.CAN_DESTROY, List.of())
                    .stream()
                    .map(BlockPalette.INSTANCE::getTypeIdentifiers)
                    .flatMap(Collection::stream)
                    .map(Identifier::toString)
                    .toList();
            nbtTag.putList("CanDestroy", NbtType.STRING, blocks);
        }

        if (item.has(ItemDataComponents.CAN_PLACE_ON)) {
            List<String> blocks = item.getOrDefault(ItemDataComponents.CAN_PLACE_ON, List.of()).stream().map(blockType -> blockType.getId().toString()).toList();
            nbtTag.putList("CanPlaceOn", NbtType.STRING, blocks);
        }

        NbtMap tag = getSerializedTag(item);
        if (!tag.isEmpty()) {
            nbtTag.put("tag", tag);
        }

        return nbtTag.build();
    }

    public static NbtMap serializeItem(ItemStack item, int slot) {
        NbtMap map = serializeItem(item);
        if (slot >= 0) {
            return map.toBuilder().putByte("Slot", (byte) slot).build();
        }
        return map;
    }

    public static NbtMap getSerializedTag(ItemStack item) {
        if (!CloudItemRegistry.get().getSerializer(item.getType()).isCacheable()) {
            return serializeTag(item);
        }

        try {
            return SERIALIZED_TAGS.get(item, () -> serializeTag(item));
        } catch (ExecutionException e) {
            throw new IllegalStateException("Invalid state while serializing item " + item, e);
        }
    }

    private static NbtMap serializeTag(ItemStack item) {
        NbtMapBuilder tag = NbtMap.builder();
        CloudItemRegistry.get().getSerializer(item.getType()).serialize(item, tag);
        return tag.build();
    }

    public static ItemStack deserializeItem(NbtMap tag) {
        if (!tag.containsKey("Name", NbtType.STRING) || !tag.containsKey("Count", NbtType.BYTE)) {
            return ItemStack.EMPTY;
        }

        ItemStack base = deserializeItem(
                Identifier.parse(tag.getString("Name")),
                tag.getShort("Damage", (short) 0),
                tag.getByte("Count"),
                tag.getCompound("tag", NbtMap.EMPTY));

        if (base.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStackBuilder builder = base.toBuilder();
        if (tag.containsKey("CanPlaceOn", NbtType.LIST)) {
            builder.setData(ItemDataComponents.CAN_PLACE_ON, readBlockTypes(tag, "CanPlaceOn"));
        }

        if (tag.containsKey("CanDestroy", NbtType.LIST)) {
            builder.setData(ItemDataComponents.CAN_DESTROY, readBlockTypes(tag, "CanDestroy"));
        }

        return builder.build();
    }

    public static ItemStack deserializeItem(Identifier id, short damage, int amount, NbtMap tag) {
        ItemStackBuilder builder = ItemStack.builder();
        if (amount > 0) {
            ItemType type = CloudItemRegistry.get().get(id).orElse(ItemTypes.UNKNOWN);
            builder.itemType(type);
            builder.amount(amount);

            if (BlockPalette.INSTANCE.getType(id) != null) {
                BlockState blockState = BlockStateMetaMappings.getStateFromMeta(id, damage);
                if (blockState != null) {
                    builder.setData(ItemDataComponents.BLOCK_STATE, blockState);
                }

                if (damage != 0) {
                    builder.setData(ItemDataComponents.DAMAGE, (int) damage);
                }
            }

            CloudItemRegistry.get().getSerializer(type).deserialize(id, damage, builder, tag);
        } else {
            return ItemStack.EMPTY;
        }

        return builder.build();
    }

    private static List<BlockType> readBlockTypes(NbtMap tag, String key) {
        return tag.getList(key, NbtType.STRING, List.of()).stream()
                .map(Identifier::parse)
                .map(id -> findBlockType(id).orElseThrow(() -> new IllegalArgumentException("Unknown block type in " + key + ": " + id)))
                .distinct()
                .toList();
    }

    public static Optional<BlockType> findBlockType(Identifier id) {
        BlockType serialized = BlockPalette.INSTANCE.getType(id);
        return serialized != null ? Optional.of(serialized) : BlockRegistry.get().get(id);
    }
}
