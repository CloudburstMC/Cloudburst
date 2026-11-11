package org.cloudburstmc.server.item;

import lombok.experimental.UtilityClass;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemStackBuilder;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.protocol.bedrock.data.definitions.BlockDefinition;
import org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.server.block.BlockPalette;
import org.cloudburstmc.server.network.inventory.ItemStackNetworkIds;
import org.cloudburstmc.server.registry.CloudItemRegistry;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Converts item stacks to and from item packets.
 */
@UtilityClass
public class ItemStackPacketCodec {

    public static ItemData toNetwork(ItemStack item) {
        if (item == null || item.isEmpty()) {
            return ItemData.AIR;
        }

        return toNetworkBuilder(item)
                .netId(0)
                .usingNetId(false)
                .build();
    }

    public static ItemData toNetworkNetId(ItemStack item) {
        if (item == null || item.isEmpty()) {
            return ItemData.AIR;
        }

        int netId = ItemStackNetworkIds.get(item);
        return toNetworkBuilder(item)
                .netId(netId)
                .usingNetId(true)
                .build();
    }

    private static ItemData.Builder toNetworkBuilder(ItemStack item) {
        Identifier identifier = item.getType().getId();
        int damage = CloudItemRegistry.get().getSerializer(item.getType()).getAuxValue(item);
        ItemDefinition definition = CloudItemRegistry.get().getDefinition(identifier, damage);

        BlockDefinition blockDefinition = null;
        BlockState blockState = item.getBlockState().orElse(null);
        if (blockState != null) {
            try {
                blockDefinition = BlockPalette.INSTANCE.getDefinition(blockState);
            } catch (IllegalArgumentException e) {
                // Some creative entries reference block states absent from the palette.
            }
        }

        return encode(item, definition, damage, blockDefinition);
    }

    public static ItemData.Builder encode(ItemStack item, ItemDefinition definition, int damage, @Nullable BlockDefinition blockDefinition) {
        String[] canPlace = item.getOrDefault(ItemDataComponents.CAN_PLACE_ON, List.of()).stream()
                .map(BlockType::getId)
                .map(Identifier::toString)
                .toArray(String[]::new);
        String[] canBreak = item.getOrDefault(ItemDataComponents.CAN_DESTROY, List.of()).stream()
                .map(BlockType::getId)
                .map(Identifier::toString)
                .toArray(String[]::new);

        NbtMap tag = ItemStackNbtCodec.getSerializedTag(item);
        return ItemData.builder()
                .definition(definition)
                .damage(damage)
                .count(item.getCount())
                .tag(tag.isEmpty() ? null : tag)
                .canPlace(canPlace)
                .canBreak(canBreak)
                .blockDefinition(blockDefinition);
    }

    public static ItemStack fromNetwork(ItemData data) {
        if (data.getCount() <= 0) {
            return ItemStack.EMPTY;
        }

        Identifier id = Identifier.parse(data.getDefinition().getIdentifier());
        NbtMap tag = data.getTag();
        ItemStack item = ItemStackNbtCodec.deserializeItem(id, (short) data.getDamage(), data.getCount(), tag == null ? NbtMap.EMPTY : tag);
        String[] canBreak = data.getCanBreak();
        String[] canPlace = data.getCanPlace();
        if (item.isEmpty() || (canBreak.length == 0 && canPlace.length == 0)) {
            return item;
        }

        ItemStackBuilder builder = item.toBuilder();
        if (canBreak.length != 0) {
            builder.setData(ItemDataComponents.CAN_DESTROY, decodeBlockTypes(canBreak));
        }

        if (canPlace.length != 0) {
            builder.setData(ItemDataComponents.CAN_PLACE_ON, decodeBlockTypes(canPlace));
        }

        return builder.build();
    }

    private static List<BlockType> decodeBlockTypes(String[] names) {
        Set<BlockType> types = new LinkedHashSet<>();
        for (String name : names) {
            Identifier id;
            try {
                id = Identifier.parse(name);
            } catch (IllegalArgumentException ignored) {
                continue;
            }
            ItemStackNbtCodec.findBlockType(id).ifPresent(types::add);
        }
        return new ArrayList<>(types);
    }
}
