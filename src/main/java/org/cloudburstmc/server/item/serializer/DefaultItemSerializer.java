package org.cloudburstmc.server.item.serializer;

import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import com.nukkitx.nbt.NbtType;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.enchantment.EnchantmentInstance;
import org.cloudburstmc.api.item.ItemKeys;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemStackBuilder;
import org.cloudburstmc.api.item.ItemType;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.api.util.NonSerializable;
import org.cloudburstmc.server.block.BlockPalette;
import org.cloudburstmc.server.block.util.BlockStateMetaMappings;
import org.cloudburstmc.server.enchantment.CloudEnchantmentInstance;
import org.cloudburstmc.server.item.SerializedItem;
import org.cloudburstmc.server.item.data.serializer.ItemDataSerializer;
import org.cloudburstmc.server.registry.CloudItemRegistry;
import org.cloudburstmc.server.registry.EnchantmentRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Log4j2
public class DefaultItemSerializer implements ItemSerializer {

    public static final DefaultItemSerializer INSTANCE = new DefaultItemSerializer();

    @Override
    public void serialize(ItemStack item, NbtMapBuilder itemTag) {
        itemTag.putString("Name", item.getType().getId().toString())
                .putByte("Count", (byte) item.getCount());

        NbtMapBuilder dataTag = NbtMap.builder();

//        TODO: Item Serializers
        item.getAllMetadata().forEach((dataKey, value) -> {
            ItemDataSerializer serializer = CloudItemRegistry.get().getSerializer(dataKey);
            if (serializer == null) {
                if (!(value instanceof NonSerializable)) {
                    log.debug("Unregistered item metadata class {}", dataKey);
                }
                return;
            }

            serializer.serialize(item, dataTag, value);
        });

        if (item.isBlock()) {
            BlockState blockState = item.get(ItemKeys.BLOCK_STATE);

            itemTag.putString("Name", BlockPalette.INSTANCE.getIdentifier(blockState).toString());
            itemTag.putShort("Damage", (short) 0);
        }

        if (item.get(ItemKeys.CUSTOM_NAME) != null || (item.get(ItemKeys.CUSTOM_LORE) != null && !item.get(ItemKeys.CUSTOM_LORE).isEmpty())) {
            NbtMapBuilder display = NbtMap.builder();
            if (item.get(ItemKeys.CUSTOM_NAME) != null) {
                display.putString("Name", item.get(ItemKeys.CUSTOM_NAME));
            }

            if (item.get(ItemKeys.CUSTOM_LORE) != null && !item.get(ItemKeys.CUSTOM_LORE).isEmpty()) {
                display.putList("Lore", NbtType.STRING, item.get(ItemKeys.CUSTOM_LORE));
            }

            dataTag.putCompound("display", display.build());
        }

//        TODO Enchantments implementation
//        if (!item.getEnchantments().isEmpty()) {
//            List<NbtMap> enchantments = new ArrayList<>(item.getEnchantments().size());
//            for (EnchantmentInstance enchantment : item.getEnchantments().values()) {
//                enchantments.add(NbtMap.builder()
//                        .putShort("id", (enchantment.getType()).getId())
//                        .putShort("lvl", (short) enchantment.getLevel())
//                        .build()
//                );
//            }
//
//            dataTag.putList("ench", NbtType.COMPOUND, enchantments);
//        }

        //TODO: This should get it's own Serializer, but for now this should work
        // FIXME: Blocks can be mapped to multiple identifiers.
        if(item.get(ItemKeys.CAN_DESTROY) != null) {
            List<String> blocks = item.get(ItemKeys.CAN_DESTROY).stream().map(blockType -> blockType.getId().toString()).toList();
            dataTag.putList("CanDestroy", NbtType.STRING, blocks);
        }

        if(item.get(ItemKeys.CAN_PLACE_ON) != null) {
            List<String> blocks = item.get(ItemKeys.CAN_PLACE_ON).stream().map(blockType -> blockType.getId().toString()).toList();
            dataTag.putList("CanPlaceOn", NbtType.STRING, blocks);
        }

        if (!dataTag.isEmpty()) {
            itemTag.putCompound("tag", dataTag.build());
        }
    }

    //TODO Keep the old Damage value tags for the conversion of old world formats
    @Override
    public void deserialize(Identifier id, short meta, int amount, ItemStackBuilder builder, NbtMap tag) {
        if (amount <= 0) {
            builder.itemType(BlockTypes.AIR);
            return;
        }

        //TODO: Somewhere here it should create a SerializedItem instance including the required tags and storing it in the cache (?)

        ItemType type = CloudItemRegistry.get().getType(id, meta);
        builder.itemType(type);
        builder.amount(amount);

        NbtMapBuilder tagBuilder = NbtMap.builder();
        tagBuilder.putString("Name", id.toString());
        tagBuilder.putShort("Damage", meta);
        tagBuilder.putByte("Count", (byte) amount);
        if (!tag.isEmpty()) {
            //TODO DataTags
            builder.dataTag(tag);
            tagBuilder.putCompound("tag", tag);
        }
        builder.nbt(tagBuilder.build());

        if (type instanceof BlockType) {
            var blockState = BlockStateMetaMappings.getStateFromMeta(id, meta);

            if (blockState != null) {
                builder.data(ItemKeys.BLOCK_STATE, blockState);
            }
        }

        if (tag.isEmpty()) {
            return;
        }

        var display = tag.getCompound("display");
        if (display != null && !display.isEmpty()) {
            builder.data(ItemKeys.CUSTOM_NAME, display.getString("Name"));
            builder.data(ItemKeys.CUSTOM_LORE, display.getList("Lore", NbtType.STRING, null));
        }

//        TODO: Implement Enchantments
//        var ench = tag.getList("ench", NbtType.COMPOUND, null);
//        if (ench != null && !ench.isEmpty()) {
//            for (NbtMap entry : ench) {
//                var enchantmentType = EnchantmentRegistry.get().getType(entry.getShort("id"));
//
//                if (enchantmentType == null) {
//                    log.debug("Unknown enchantment id: {}", entry.getShort("id"));
//                    continue;
//                }
//
//                builder.addEnchantment(new CloudEnchantmentInstance(enchantmentType, entry.getShort("lvl", (short) 1)));
//            }
//        }

        if(tag.containsKey("CanPlaceOn", NbtType.LIST)) {
            List<BlockType> list = tag.getList("CanPlaceOn", NbtType.STRING, null).stream().map(Identifier::fromString).map(BlockType::of).toList();
            builder.data(ItemKeys.CAN_PLACE_ON, list);
        }

        if(tag.containsKey("CanDestroy", NbtType.LIST)) {
            List<BlockType> list = tag.getList("CanDestroy", NbtType.STRING, null).stream().map(Identifier::fromString).map(BlockType::of).toList();
            builder.data(ItemKeys.CAN_DESTROY, list);
        }
    }
}
