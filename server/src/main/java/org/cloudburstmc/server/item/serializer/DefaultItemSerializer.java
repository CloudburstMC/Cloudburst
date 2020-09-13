package org.cloudburstmc.server.item.serializer;

import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import com.nukkitx.nbt.NbtType;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.cloudburstmc.server.enchantment.CloudEnchantmentInstance;
import org.cloudburstmc.server.enchantment.EnchantmentInstance;
import org.cloudburstmc.server.enchantment.EnchantmentType;
import org.cloudburstmc.server.enchantment.EnchantmentTypes.CloudEnchantmentType;
import org.cloudburstmc.server.item.CloudItemStack;
import org.cloudburstmc.server.item.ItemType;
import org.cloudburstmc.server.item.data.serializer.ItemDataSerializer;
import org.cloudburstmc.server.registry.CloudItemRegistry;
import org.cloudburstmc.server.registry.EnchantmentRegistry;
import org.cloudburstmc.server.utils.Identifier;

import java.util.*;

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

        item.getData().forEach((clazz, value) -> {
            ItemDataSerializer serializer = registry.getSerializer(clazz);
            if (serializer == null) {
                log.debug("Unregistered item metadata class {}", clazz);
                return;
            }

            serializer.serialize(item, itemTag, value);
        });

        if (item.getName() != null || !item.getLore().isEmpty()) {
            NbtMapBuilder display = NbtMap.builder();
            if (item.getName() != null) {
                display.putString("Name", item.getName());
            }

            if (!item.getLore().isEmpty()) {
                display.putList("Lore", NbtType.STRING, item.getLore());
            }

            itemTag.putCompound("display", display.build());
        }

        if (!item.getEnchantments().isEmpty()) {
            List<NbtMap> enchantments = new ArrayList<>(item.getEnchantments().size());
            for (EnchantmentInstance enchantment : item.getEnchantments()) {
                enchantments.add(NbtMap.builder()
                        .putShort("id", (short) ((CloudEnchantmentType) enchantment.getType()).getId())
                        .putShort("lvl", (short) enchantment.getLevel())
                        .build()
                );
            }

            itemTag.putList("ench", NbtType.COMPOUND, enchantments);
        }

        if (!item.getCanDestroy().isEmpty()) {
            List<String> canDestroy = new ArrayList<>(item.getCanDestroy().size());
            for (Identifier identifier : item.getCanDestroy()) {
                canDestroy.add(identifier.toString());
            }

            itemTag.putList("CanDestroy", NbtType.STRING, canDestroy);
        }

        if (!item.getCanPlaceOn().isEmpty()) {
            List<String> canPlaceOn = new ArrayList<>(item.getCanPlaceOn().size());
            for (Identifier identifier : item.getCanPlaceOn()) {
                canPlaceOn.add(identifier.toString());
            }

            itemTag.putList("CanPlaceOn", NbtType.STRING, canPlaceOn);
        }
    }

    @Override
    public CloudItemStack deserialize(Identifier id, Integer meta, NbtMap tag) {
        ItemType type = registry.getType(id);
        if (type == null) {
            throw new IllegalArgumentException("No ItemType found for identifier: " + id);
        }
        String name = null;
        List<String> lore = null;
        Map<EnchantmentType, EnchantmentInstance> enchantments = null;
        Set<Identifier> placeOn = null;
        Set<Identifier> destroy = null;

        val display = tag.getCompound("display");
        if (display != null && !display.isEmpty()) {
            name = display.getString("Name");
            lore = display.getList("Lore", NbtType.STRING, null);
        }

        val ench = tag.getList("ench", NbtType.COMPOUND, null);
        if (ench != null && !ench.isEmpty()) {
            enchantments = new Reference2ObjectOpenHashMap<>();
            for (NbtMap entry : ench) {
                val enchantmentType = EnchantmentRegistry.get().getType(entry.getShort("id"));

                if (enchantmentType == null) {
                    log.debug("Unknown enchantment id: {}", entry.getShort("id"));
                    continue;
                }

                enchantments.put(enchantmentType, new CloudEnchantmentInstance(enchantmentType, entry.getShort("lvl", (short) 1)));
            }
        }

        val canPlaceOn = tag.getList("CanPlaceOn", NbtType.STRING, null);
        if (canPlaceOn != null && !canPlaceOn.isEmpty()) {
            placeOn = new HashSet<>(canPlaceOn.size());
            for (String s : canPlaceOn) {
                placeOn.add(Identifier.fromString(s));
            }
        }

        val canDestroy = tag.getList("CanDestroy", NbtType.STRING, null);
        if (canDestroy != null && !canDestroy.isEmpty()) {
            destroy = new HashSet<>(canDestroy.size());
            for (String s : canDestroy) {
                destroy.add(Identifier.fromString(s));
            }
        }

        return new CloudItemStack(
                id,
                type,
                tag.getByte("Count"),
                name,
                lore,
                enchantments,
                destroy,
                placeOn,
                null,
                tag,
                null
        );
    }
}
