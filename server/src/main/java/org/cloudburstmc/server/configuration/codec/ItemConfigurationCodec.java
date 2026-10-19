package org.cloudburstmc.server.configuration.codec;

import org.cloudburstmc.api.configuration.ConfigurationCodec;
import org.cloudburstmc.api.configuration.ConfigurationLoader;
import org.cloudburstmc.api.item.ItemDataComponentType;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemStackBuilder;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.nbt.NBTInputStream;
import org.cloudburstmc.nbt.NBTOutputStream;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtUtils;
import org.cloudburstmc.server.item.ItemUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Uses item persistence for vanilla data and explicit codecs for additional component values.
 */
public class ItemConfigurationCodec implements ConfigurationCodec<ItemStack> {
    private final ConfigurationLoader loader;

    public ItemConfigurationCodec(ConfigurationLoader loader) {
        this.loader = Objects.requireNonNull(loader, "loader");
    }

    @Override
    public Map<String, Object> encode(ItemStack value) {
        NbtMap stored = ItemUtils.serializeItem(value.isEmpty() ? value : value.withCount(1));
        ItemStack restored = ItemUtils.deserializeItem(stored);

        if (!restored.getType().equals(value.getType())) {
            throw new IllegalArgumentException("Item persistence cannot restore type " + value.getType().getId());
        }

        Map<String, Object> components = new LinkedHashMap<>();
        value.getDataComponents().forEach((type, component) -> {
            Object persisted = restored.getDataComponents().get(type);

            components.put(type.getId().toString(), Objects.equals(component, persisted)
                    ? Map.of("persisted", true)
                    : Map.of("value", loader.encode(component)));
        });

        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
            try (NBTOutputStream writer = NbtUtils.createWriterLE(bytes)) {
                writer.writeTag(stored);
            }

            return Map.of("version", 1, "count", value.getCount(),
                    "item", Base64.getEncoder().encodeToString(bytes.toByteArray()), "components", components);
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to encode item configuration", e);
        }
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public ItemStack decode(Map<String, Object> values) {
        int version = ConfigurationNumbers.integer(values.get("version"), "version");
        int count = ConfigurationNumbers.integer(values.get("count"), "count");

        if (version != 1 || !(values.get("item") instanceof String encoded)
                || !(values.get("components") instanceof Map<?, ?> components)) {
            throw new IllegalArgumentException("Invalid item configuration");
        }

        try (NBTInputStream reader = NbtUtils.createReaderLE(new ByteArrayInputStream(Base64.getDecoder().decode(encoded)))) {
            if (!(reader.readTag() instanceof NbtMap stored)) {
                throw new IllegalArgumentException("Item data must be a compound");
            }

            ItemStack restored = ItemUtils.deserializeItem(stored);
            if (!restored.getType().getId().toString().equals(stored.getString("Name"))) {
                throw new IllegalArgumentException("Unknown stored item type: " + stored.getString("Name"));
            }

            if (restored.isEmpty()) {
                if (count != 0 || !components.isEmpty()) {
                    throw new IllegalArgumentException("Invalid empty item configuration");
                }

                return ItemStack.EMPTY;
            }

            ItemStackBuilder builder = restored.toBuilder().amount(count).clearData();

            for (Map.Entry<?, ?> entry : components.entrySet()) {
                if (!(entry.getKey() instanceof String id) || !(entry.getValue() instanceof Map<?, ?> component)) {
                    throw new IllegalArgumentException("Invalid item component entry");
                }

                ItemDataComponentType type = ItemDataComponentType.get(Identifier.parse(id))
                        .orElseThrow(() -> new IllegalArgumentException("Unknown item component: " + id));

                Object data;
                if (Boolean.TRUE.equals(component.get("persisted"))) {
                    data = restored.getDataComponents().get(type);
                } else if (component.get("value") instanceof Map<?, ?> custom) {
                    Map<String, Object> envelope = (Map<String, Object>) custom;
                    data = loader.decode(envelope, type.getValueType());
                } else {
                    throw new IllegalArgumentException("Missing item component value: " + id);
                }

                builder.setData(type, Objects.requireNonNull(data, "Missing persisted component " + id));
            }

            return builder.build();
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to decode item configuration", e);
        }
    }
}
