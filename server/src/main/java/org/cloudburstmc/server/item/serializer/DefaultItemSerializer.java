package org.cloudburstmc.server.item.serializer;

import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.api.enchantment.Enchantment;
import org.cloudburstmc.api.enchantment.EnchantmentType;
import org.cloudburstmc.api.item.ItemDataComponentType;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemStackBuilder;
import org.cloudburstmc.api.item.data.BucketEntityData;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.item.data.serializer.ItemDataComponentSerializer;
import org.cloudburstmc.server.registry.CloudEnchantmentRegistry;
import org.cloudburstmc.server.registry.CloudItemRegistry;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Log4j2
public class DefaultItemSerializer implements ItemSerializer {

    public static final DefaultItemSerializer INSTANCE = new DefaultItemSerializer();

    private static final String TAG_BUCKET_ENTITY_DATA = "BucketEntityData";
    private static final String TAG_DISPLAY = "display";
    private static final String TAG_DISPLAY_LORE = "Lore";
    private static final String TAG_DISPLAY_NAME = "Name";
    private static final String TAG_ENCHANTMENTS = "ench";
    private static final String TAG_ENCHANTMENT_ID = "id";
    private static final String TAG_ENCHANTMENT_LEVEL = "lvl";
    private static final String TAG_ENTITY_HEALTH = "Health";
    private static final String TAG_ENTITY_IMMOBILE = "NoAI";
    private static final String TAG_ENTITY_INVULNERABLE = "Invulnerable";

    @Override
    public void serialize(ItemStack item, NbtMapBuilder tag) {
        serializeRegisteredData(item, tag);
        serializeDisplay(item, tag);
        serializeEnchantments(item, tag);
        serializeBucketEntityData(item, tag);
    }

    private static void serializeRegisteredData(ItemStack item, NbtMapBuilder tag) {
        for (Map.Entry<ItemDataComponentType<?>, ?> entry : item.getDataComponents().entrySet()) {
            serializeRegisteredDataValue(item, tag, entry.getKey(), entry.getValue());
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void serializeRegisteredDataValue(ItemStack item, NbtMapBuilder tag, ItemDataComponentType dataType, Object value) {
        ItemDataComponentSerializer serializer = CloudItemRegistry.get().getDataComponentSerializer(dataType);
        if (serializer != null) {
            serializer.serialize(item, tag, value);
        }
    }

    private static void serializeDisplay(ItemStack item, NbtMapBuilder tag) {
        String customName = item.get(ItemDataComponents.CUSTOM_NAME);
        List<String> customLore = item.getOrDefault(ItemDataComponents.CUSTOM_LORE, List.of());
        if (customName == null && customLore.isEmpty()) {
            return;
        }

        NbtMapBuilder display = NbtMap.builder();
        if (customName != null) {
            display.putString(TAG_DISPLAY_NAME, customName);
        }

        if (!customLore.isEmpty()) {
            display.putList(TAG_DISPLAY_LORE, NbtType.STRING, customLore);
        }

        tag.putCompound(TAG_DISPLAY, display.build());
    }

    private static void serializeEnchantments(ItemStack item, NbtMapBuilder tag) {
        Map<EnchantmentType, Enchantment> enchantments = item.getOrDefault(ItemDataComponents.ENCHANTMENTS, Map.of());
        if (enchantments.isEmpty()) {
            return;
        }

        List<NbtMap> enchantmentTags = enchantments.values().stream()
                .map(DefaultItemSerializer::serializeEnchantment)
                .toList();
        tag.putList(TAG_ENCHANTMENTS, NbtType.COMPOUND, enchantmentTags);
    }

    private static NbtMap serializeEnchantment(Enchantment enchantment) {
        return NbtMap.builder()
                .putShort(TAG_ENCHANTMENT_ID, CloudEnchantmentRegistry.get().getSerializedId(enchantment.type()))
                .putShort(TAG_ENCHANTMENT_LEVEL, (short) enchantment.level())
                .build();
    }

    private static void serializeBucketEntityData(ItemStack item, NbtMapBuilder tag) {
        BucketEntityData bucketEntityData = item.get(ItemDataComponents.BUCKET_ENTITY_DATA);
        if (bucketEntityData == null) {
            return;
        }

        tag.putCompound(TAG_BUCKET_ENTITY_DATA, NbtMap.builder()
                .putFloat(TAG_ENTITY_HEALTH, bucketEntityData.health())
                .putBoolean(TAG_ENTITY_INVULNERABLE, bucketEntityData.invulnerable())
                .putBoolean(TAG_ENTITY_IMMOBILE, bucketEntityData.immobile())
                .build());
    }

    @Override
    public void deserialize(Identifier id, short meta, ItemStackBuilder builder, NbtMap tag) {
        if (tag.isEmpty()) {
            return;
        }

        deserializeRegisteredData(id, builder, tag);
        deserializeDisplay(builder, tag);
        deserializeEnchantments(builder, tag);
        deserializeBucketEntityData(builder, tag);
    }

    private static void deserializeRegisteredData(Identifier id, ItemStackBuilder builder, NbtMap tag) {
        for (ItemDataComponentType<?> dataType : CloudItemRegistry.get().getSerializedDataComponents()) {
            deserializeRegisteredDataValue(id, builder, tag, dataType);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void deserializeRegisteredDataValue(Identifier id, ItemStackBuilder builder, NbtMap tag, ItemDataComponentType dataType) {
        ItemDataComponentSerializer serializer = CloudItemRegistry.get().getDataComponentSerializer(dataType);
        if (serializer == null) {
            return;
        }

        Object value = serializer.deserialize(id, tag);
        if (value != null) {
            builder.setData(dataType, value);
        }
    }

    private static void deserializeDisplay(ItemStackBuilder builder, NbtMap tag) {
        if (!tag.containsKey(TAG_DISPLAY, NbtType.COMPOUND)) {
            return;
        }

        NbtMap display = tag.getCompound(TAG_DISPLAY);
        if (display.containsKey(TAG_DISPLAY_NAME, NbtType.STRING)) {
            builder.setData(ItemDataComponents.CUSTOM_NAME, display.getString(TAG_DISPLAY_NAME));
        }

        if (display.containsKey(TAG_DISPLAY_LORE, NbtType.LIST)) {
            List<String> lore = display.getList(TAG_DISPLAY_LORE, NbtType.STRING, Collections.emptyList());
            if (!lore.isEmpty()) {
                builder.setData(ItemDataComponents.CUSTOM_LORE, lore);
            }
        }
    }

    private static void deserializeEnchantments(ItemStackBuilder builder, NbtMap tag) {
        List<NbtMap> enchantmentTags = tag.getList(TAG_ENCHANTMENTS, NbtType.COMPOUND, Collections.emptyList());
        if (enchantmentTags.isEmpty()) {
            return;
        }

        Map<EnchantmentType, Enchantment> enchantments = new LinkedHashMap<>();
        for (NbtMap enchantmentTag : enchantmentTags) {
            Enchantment enchantment = deserializeEnchantment(enchantmentTag);
            if (enchantment != null) {
                enchantments.put(enchantment.type(), enchantment);
            }
        }

        if (!enchantments.isEmpty()) {
            builder.setData(ItemDataComponents.ENCHANTMENTS, enchantments);
        }
    }

    private static Enchantment deserializeEnchantment(NbtMap enchantmentTag) {
        short enchantmentId = enchantmentTag.getShort(TAG_ENCHANTMENT_ID);
        EnchantmentType type = CloudEnchantmentRegistry.get().getType(enchantmentId);
        if (type == null) {
            log.debug("Unknown enchantment id: {}", enchantmentId);
            return null;
        }

        short level = enchantmentTag.getShort(TAG_ENCHANTMENT_LEVEL, (short) 1);
        if (level <= 0) {
            log.debug("Ignoring enchantment {} with invalid level {}", enchantmentId, level);
            return null;
        }

        return new Enchantment(type, level);
    }

    private static void deserializeBucketEntityData(ItemStackBuilder builder, NbtMap tag) {
        if (!tag.containsKey(TAG_BUCKET_ENTITY_DATA, NbtType.COMPOUND)) {
            return;
        }

        NbtMap bucketEntityData = tag.getCompound(TAG_BUCKET_ENTITY_DATA);
        builder.setData(ItemDataComponents.BUCKET_ENTITY_DATA, new BucketEntityData(
                bucketEntityData.getFloat(TAG_ENTITY_HEALTH),
                bucketEntityData.getBoolean(TAG_ENTITY_INVULNERABLE, false),
                bucketEntityData.getBoolean(TAG_ENTITY_IMMOBILE, false)));
    }
}
