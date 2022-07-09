package org.cloudburstmc.server.item.serializer;

import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import com.nukkitx.nbt.NbtType;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.api.enchantment.Enchantment;
import org.cloudburstmc.api.enchantment.EnchantmentType;
import org.cloudburstmc.api.item.ItemKeys;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemStackBuilder;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.server.registry.EnchantmentRegistry;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Log4j2
public class DefaultItemSerializer implements ItemSerializer {

    public static final DefaultItemSerializer INSTANCE = new DefaultItemSerializer();

    @Override
    public void serialize(ItemStack item, NbtMapBuilder tag) {
//        TODO: Item Serializers
//        item.getAllMetadata().forEach((dataKey, value) -> {
//            ItemDataSerializer serializer = CloudItemRegistry.get().getSerializer(dataKey);
//            if (serializer == null) {
//                if (!(value instanceof NonSerializable)) {
//                    log.debug("Unregistered item metadata class {}", dataKey);
//                }
//                return;
//            }
//
//            serializer.serialize(item, dataTag, value);
//        });

        if (item.get(ItemKeys.CUSTOM_NAME) != null || (item.get(ItemKeys.CUSTOM_LORE) != null && !item.get(ItemKeys.CUSTOM_LORE).isEmpty())) {
            NbtMapBuilder display = NbtMap.builder();
            if (item.get(ItemKeys.CUSTOM_NAME) != null) {
                display.putString("Name", item.get(ItemKeys.CUSTOM_NAME));
            }

            if (item.get(ItemKeys.CUSTOM_LORE) != null && !item.get(ItemKeys.CUSTOM_LORE).isEmpty()) {
                display.putList("Lore", NbtType.STRING, item.get(ItemKeys.CUSTOM_LORE));
            }

            tag.putCompound("display", display.build());
        }

        if (item.get(ItemKeys.ENCHANTMENTS) != null && !item.get(ItemKeys.ENCHANTMENTS).isEmpty()) {
            List<NbtMap> enchantments = item.get(ItemKeys.ENCHANTMENTS).values().stream().map(enchantment ->
                    NbtMap.builder()
                            .putShort("id", enchantment.type().getId())
                            .putShort("lvl", (short) enchantment.level())
                            .build()
            ).toList();

            tag.putList("ench", NbtType.COMPOUND, enchantments);
        }
    }

    @Override
    public void deserialize(Identifier id, short meta, ItemStackBuilder builder, NbtMap tag) {
        if (tag.isEmpty()) {
            return;
        }

        if (tag.containsKey("display", NbtType.COMPOUND)) {
            NbtMap display = tag.getCompound("display");
            if (display.containsKey("Name", NbtType.STRING)) {
                builder.data(ItemKeys.CUSTOM_NAME, display.getString("Name"));
            }

            if(display.containsKey("Lore", NbtType.LIST)) {
                List<String> lore = display.getList("Lore", NbtType.STRING, Collections.emptyList());
                if(!lore.isEmpty()) {
                    builder.data(ItemKeys.CUSTOM_LORE, lore);
                }
            }
        }

        List<NbtMap> enchantmentNbt = tag.getList("ench", NbtType.COMPOUND, Collections.emptyList());
        if(!enchantmentNbt.isEmpty()) {
            //TODO something else than a HashMap?
            Map<EnchantmentType, Enchantment> enchantments = new HashMap<>();
            enchantmentNbt.forEach(enchantment -> {
                EnchantmentType type = EnchantmentRegistry.get().getType(enchantment.getShort("id"));

                if(type == null) {
                    log.debug("Unknown enchantment id: {}", enchantment.getShort("id"));
                    return;
                }

                enchantments.put(type, new Enchantment(type, enchantment.getShort("lvl", (short) 1)));
            });

            builder.data(ItemKeys.ENCHANTMENTS, enchantments);
        }
    }
}
