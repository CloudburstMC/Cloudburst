package org.cloudburstmc.server.item.serializer;

import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.api.data.DataKey;
import org.cloudburstmc.api.enchantment.Enchantment;
import org.cloudburstmc.api.enchantment.EnchantmentType;
import org.cloudburstmc.api.item.ItemKeys;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemStackBuilder;
import org.cloudburstmc.api.item.data.BucketEntityData;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.item.data.serializer.ItemDataSerializer;
import org.cloudburstmc.server.registry.CloudItemRegistry;
import org.cloudburstmc.server.registry.EnchantmentRegistry;

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
        for (Map.Entry<DataKey<?, ?>, ?> entry : item.getAllMetadata().entrySet()) {
            serializeRegisteredDataValue(item, tag, entry.getKey(), entry.getValue());
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void serializeRegisteredDataValue(ItemStack item, NbtMapBuilder tag, DataKey dataKey, Object value) {
        ItemDataSerializer serializer = CloudItemRegistry.get().getDataSerializer(dataKey);
        if (serializer != null) {
            serializer.serialize(item, tag, value);
        }
    }

    private static void serializeDisplay(ItemStack item, NbtMapBuilder tag) {
        String customName = item.get(ItemKeys.CUSTOM_NAME);
        List<String> customLore = item.get(ItemKeys.CUSTOM_LORE);
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
        Map<EnchantmentType, Enchantment> enchantments = item.get(ItemKeys.ENCHANTMENTS);
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
                .putShort(TAG_ENCHANTMENT_ID, enchantment.type().id())
                .putShort(TAG_ENCHANTMENT_LEVEL, (short) enchantment.level())
                .build();
    }

    private static void serializeBucketEntityData(ItemStack item, NbtMapBuilder tag) {
        BucketEntityData bucketEntityData = item.get(ItemKeys.BUCKET_ENTITY_DATA);
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
        for (DataKey<?, ?> dataKey : CloudItemRegistry.get().getSerializedDataKeys()) {
            deserializeRegisteredDataValue(id, builder, tag, dataKey);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void deserializeRegisteredDataValue(Identifier id, ItemStackBuilder builder, NbtMap tag, DataKey dataKey) {
        ItemDataSerializer serializer = CloudItemRegistry.get().getDataSerializer(dataKey);
        if (serializer == null) {
            return;
        }

        Object value = serializer.deserialize(id, tag);
        if (value != null) {
            builder.data(dataKey, value);
        }
    }

    private static void deserializeDisplay(ItemStackBuilder builder, NbtMap tag) {
        if (!tag.containsKey(TAG_DISPLAY, NbtType.COMPOUND)) {
            return;
        }

        NbtMap display = tag.getCompound(TAG_DISPLAY);
        if (display.containsKey(TAG_DISPLAY_NAME, NbtType.STRING)) {
            builder.data(ItemKeys.CUSTOM_NAME, display.getString(TAG_DISPLAY_NAME));
        }

        if (display.containsKey(TAG_DISPLAY_LORE, NbtType.LIST)) {
            List<String> lore = display.getList(TAG_DISPLAY_LORE, NbtType.STRING, Collections.emptyList());
            if (!lore.isEmpty()) {
                builder.data(ItemKeys.CUSTOM_LORE, lore);
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
            builder.data(ItemKeys.ENCHANTMENTS, enchantments);
        }
    }

    private static Enchantment deserializeEnchantment(NbtMap enchantmentTag) {
        short enchantmentId = enchantmentTag.getShort(TAG_ENCHANTMENT_ID);
        EnchantmentType type = EnchantmentRegistry.get().getType(enchantmentId);
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
        builder.data(ItemKeys.BUCKET_ENTITY_DATA, new BucketEntityData(
                bucketEntityData.getFloat(TAG_ENTITY_HEALTH),
                bucketEntityData.getBoolean(TAG_ENTITY_INVULNERABLE, false),
                bucketEntityData.getBoolean(TAG_ENTITY_IMMOBILE, false)));
    }
}
