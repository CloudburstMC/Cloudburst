package org.cloudburstmc.server.item.serializer;

import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import com.nukkitx.nbt.NbtType;
import lombok.extern.log4j.Log4j2;
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
import org.cloudburstmc.server.item.data.serializer.ItemDataSerializer;
import org.cloudburstmc.server.registry.CloudItemRegistry;
import org.cloudburstmc.server.registry.EnchantmentRegistry;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"unchecked", "rawtypes"})
@Log4j2
public class DefaultItemSerializer implements ItemSerializer {

    public static final DefaultItemSerializer INSTANCE = new DefaultItemSerializer();

    @Override
    public void serialize(ItemStack item, NbtMapBuilder itemTag) {
        if(SerializationCache.ITEM_CACHE.getIfPresent(item) != null) {
            //TODO handle cache
        }

        itemTag.putString("Name", item.getType().getId().toString())
                .putByte("Count", (byte) item.getCount());

        NbtMapBuilder dataTag = NbtMap.builder();

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
            itemTag.putString("Name", BlockPalette.INSTANCE.getIdentifier(item.get(ItemKeys.BLOCK_STATE)).toString());
            itemTag.putShort("Damage", (short) BlockStateMetaMappings.getMetaFromState(item.get(ItemKeys.BLOCK_STATE)));
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
        serializeCanInteract(dataTag, item.get(ItemKeys.CAN_DESTROY), "CanDestroy");
        serializeCanInteract(dataTag, item.get(ItemKeys.CAN_PLACE_ON), "CanPlaceOn");

        if (!dataTag.isEmpty()) {
            itemTag.putCompound("tag", dataTag.build());
        }
    }

    private static void serializeCanInteract(NbtMapBuilder tag, List<BlockType> blockTypes, String name) {
        if (blockTypes != null && !blockTypes.isEmpty()) {
            List<String> tagList = new ArrayList<>(blockTypes.size());
            for (BlockType type : blockTypes) {
                tagList.add(type.getId().toString()); // FIXME: Blocks can be mapped to multiple identifiers.
            }

            tag.putList(name, NbtType.STRING, tagList);
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

        var canPlaceOn = tag.getList("CanPlaceOn", NbtType.STRING, null);
        if (canPlaceOn != null && !canPlaceOn.isEmpty()) {
            for (String s : canPlaceOn) {
                builder.addCanPlaceOn(Identifier.fromString(s));
            }
        }

        var canDestroy = tag.getList("CanDestroy", NbtType.STRING, null);
        if (canDestroy != null && !canDestroy.isEmpty()) {
            for (String s : canDestroy) {
                builder.addCanDestroy(Identifier.fromString(s));
            }
        }
    }
}
