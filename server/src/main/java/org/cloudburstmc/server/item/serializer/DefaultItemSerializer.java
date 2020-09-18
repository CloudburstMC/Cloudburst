package org.cloudburstmc.server.item.serializer;

import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import com.nukkitx.nbt.NbtType;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.cloudburstmc.server.enchantment.CloudEnchantmentInstance;
import org.cloudburstmc.server.enchantment.EnchantmentInstance;
import org.cloudburstmc.server.enchantment.EnchantmentTypes.CloudEnchantmentType;
import org.cloudburstmc.server.item.CloudItemStack;
import org.cloudburstmc.server.item.CloudItemStackBuilder;
import org.cloudburstmc.server.item.ItemType;
import org.cloudburstmc.server.item.data.serializer.ItemDataSerializer;
import org.cloudburstmc.server.registry.CloudItemRegistry;
import org.cloudburstmc.server.registry.EnchantmentRegistry;
import org.cloudburstmc.server.utils.Identifier;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"unchecked", "rawtypes"})
@Log4j2
public class DefaultItemSerializer implements ItemSerializer {

    public static final DefaultItemSerializer INSTANCE = new DefaultItemSerializer();

    protected static final CloudItemRegistry registry = CloudItemRegistry.get();

    @Override
    public void serialize(CloudItemStack item, NbtMapBuilder itemTag) {
        itemTag.putString("Name", item.getType().getId().toString())
                .putByte("Count", (byte) item.getAmount())
                .putShort("Damage", (short) 0);

        if (!item.getData().isEmpty()) {
            NbtMapBuilder dataTag = NbtMap.builder();

            item.getData().forEach((clazz, value) -> {
                ItemDataSerializer serializer = registry.getSerializer(clazz);
                if (serializer == null) {
                    log.debug("Unregistered item metadata class {}", clazz);
                    return;
                }

                serializer.serialize(item, dataTag, value);
            });

            if (!dataTag.isEmpty()) {
                itemTag.putCompound("tag", dataTag.build());
            }
        }

        NbtMapBuilder dataTag = NbtMap.builder();

        if (item.getName() != null || !item.getLore().isEmpty()) {
            NbtMapBuilder display = NbtMap.builder();
            if (item.getName() != null) {
                display.putString("Name", item.getName());
            }

            if (!item.getLore().isEmpty()) {
                display.putList("Lore", NbtType.STRING, item.getLore());
            }

            dataTag.putCompound("display", display.build());
        }

        if (!item.getEnchantments().isEmpty()) {
            List<NbtMap> enchantments = new ArrayList<>(item.getEnchantments().size());
            for (EnchantmentInstance enchantment : item.getEnchantments().values()) {
                enchantments.add(NbtMap.builder()
                        .putShort("id", ((CloudEnchantmentType) enchantment.getType()).getId())
                        .putShort("lvl", (short) enchantment.getLevel())
                        .build()
                );
            }

            dataTag.putList("ench", NbtType.COMPOUND, enchantments);
        }

        if (!item.getCanDestroy().isEmpty()) {
            List<String> canDestroy = new ArrayList<>(item.getCanDestroy().size());
            for (Identifier identifier : item.getCanDestroy()) {
                canDestroy.add(identifier.toString());
            }

            dataTag.putList("CanDestroy", NbtType.STRING, canDestroy);
        }

        if (!item.getCanPlaceOn().isEmpty()) {
            List<String> canPlaceOn = new ArrayList<>(item.getCanPlaceOn().size());
            for (Identifier identifier : item.getCanPlaceOn()) {
                canPlaceOn.add(identifier.toString());
            }

            dataTag.putList("CanPlaceOn", NbtType.STRING, canPlaceOn);
        }

        if (!dataTag.isEmpty()) {
            itemTag.putCompound("tag", dataTag.build());
        }
    }

    @Override
    public void deserialize(Identifier id, short meta, int amount, CloudItemStackBuilder builder, NbtMap tag) {
        ItemType type = registry.getType(id);
        builder.itemType(type);
        builder.amount(amount);

        if (tag.isEmpty()) {
            return;
        }

        val display = tag.getCompound("display");
        if (display != null && !display.isEmpty()) {
            builder.name(display.getString("Name"));
            builder.lore(display.getList("Lore", NbtType.STRING, null));
        }

        val ench = tag.getList("ench", NbtType.COMPOUND, null);
        if (ench != null && !ench.isEmpty()) {
            for (NbtMap entry : ench) {
                val enchantmentType = EnchantmentRegistry.get().getType(entry.getShort("id"));

                if (enchantmentType == null) {
                    log.debug("Unknown enchantment id: {}", entry.getShort("id"));
                    continue;
                }

                builder.addEnchantment(new CloudEnchantmentInstance(enchantmentType, entry.getShort("lvl", (short) 1)));
            }
        }

        val canPlaceOn = tag.getList("CanPlaceOn", NbtType.STRING, null);
        if (canPlaceOn != null && !canPlaceOn.isEmpty()) {
            for (String s : canPlaceOn) {
                builder.addCanPlaceOn(Identifier.fromString(s));
            }
        }

        val canDestroy = tag.getList("CanDestroy", NbtType.STRING, null);
        if (canDestroy != null && !canDestroy.isEmpty()) {
            for (String s : canDestroy) {
                builder.addCanDestroy(Identifier.fromString(s));
            }
        }
    }
}
