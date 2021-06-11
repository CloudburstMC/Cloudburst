package org.cloudburstmc.server.item;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import com.nukkitx.nbt.NBTInputStream;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtUtils;
import com.nukkitx.protocol.bedrock.data.inventory.ItemData;
import com.nukkitx.protocol.bedrock.packet.CreativeContentPacket;
import com.nukkitx.protocol.bedrock.packet.StartGamePacket;
import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import org.cloudburstmc.api.registry.RegistryException;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.server.Bootstrap;
import org.cloudburstmc.server.registry.CloudBlockRegistry;
import org.cloudburstmc.server.registry.CloudItemRegistry;
import org.cloudburstmc.server.registry.RegistryUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ItemPalette {
    private final static BiMap<Integer, Identifier> legacyIdMap = HashBiMap.create();
    private final static Reference2ObjectMap<Identifier, Int2ReferenceMap<Identifier>> metaMap = new Reference2ObjectOpenHashMap<>();
    private final CloudItemRegistry itemRegistry;

    static {
        try (InputStream in = RegistryUtils.getOrAssertResource("data/legacy_item_ids.json")) {
            JsonNode json = Bootstrap.JSON_MAPPER.readTree(in);
            Iterator<Map.Entry<String, JsonNode>> it = json.fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> entry = it.next();
                legacyIdMap.put(entry.getValue().asInt(), Identifier.fromString(entry.getKey()));
            }
        } catch (IOException | NumberFormatException e) {
            throw new RegistryException("Unable to load Legacy Item IDs", e);
        }

        try (InputStream in = RegistryUtils.getOrAssertResource("data/item_mappings.json")) {
            JsonNode json = Bootstrap.JSON_MAPPER.readTree(in);
            for (Iterator<Map.Entry<String, JsonNode>> it = json.fields(); it.hasNext(); ) {
                Map.Entry<String, JsonNode> entry = it.next();
                Identifier id = Identifier.fromString(entry.getKey());
                Int2ReferenceMap<Identifier> map = metaMap.computeIfAbsent(id, i -> new Int2ReferenceOpenHashMap<>());
                for (Iterator<Map.Entry<String, JsonNode>> it2 = entry.getValue().fields(); it2.hasNext(); ) {
                    Map.Entry<String, JsonNode> value = it2.next();
                    map.put(Integer.parseInt(value.getKey()), Identifier.fromString(value.getValue().asText()));
                }
            }
        } catch (IOException | NumberFormatException e) {
            throw new RegistryException("Unable to load Legacy Meta Mapping", e);
        }
    }

    private final Int2ReferenceMap<Identifier> runtimeIdMap = new Int2ReferenceOpenHashMap<>();
    private final Reference2IntMap<Identifier> idRuntimeMap = new Reference2IntOpenHashMap<>();
    private final AtomicInteger runtimeIdAllocator = new AtomicInteger(256);
    private final List<StartGamePacket.ItemEntry> itemEntries = new ArrayList<>();
    private volatile CreativeContentPacket creativeContentPacket;
    private final List<ItemData> creativeItems = new ArrayList<>();

    public ItemPalette(CloudItemRegistry registry) {
        this.itemRegistry = registry;
    }

    public int addItem(Identifier identifier) {
        int runtimeId = runtimeIdAllocator.getAndIncrement();
        runtimeIdMap.put(runtimeId, identifier);
        idRuntimeMap.putIfAbsent(identifier, runtimeId);

        itemEntries.add(new StartGamePacket.ItemEntry(identifier.toString(), (short) runtimeId));
        return runtimeId;
    }

    public int getRuntimeId(Identifier id) {
        return getRuntimeId(id, 0);
    }

    public int getRuntimeId(Identifier id, int meta) {
        if (metaMap.containsKey(id)) {
            id = metaMap.get(id).get(meta);
        }
        return idRuntimeMap.getOrDefault(id, Integer.MAX_VALUE);
    }

    public Identifier getIdByRuntime(int runtimeId) {
        return runtimeIdMap.get(runtimeId);
    }

    public CreativeContentPacket getCreativeContentPacket() {
        if (creativeContentPacket == null) {
            this.creativeContentPacket = new CreativeContentPacket();
            ItemData[] data = creativeItems.toArray(new ItemData[0]);

            for (int i = 0; i < data.length; i++) {
                data[i].setNetId(i + 1);
            }
            creativeContentPacket.setContents(data);
        }
        return creativeContentPacket;
    }

    public List<StartGamePacket.ItemEntry> getItemPalette() {
        return itemEntries;
    }

    public ImmutableList<Identifier> getItemIds() {
        return ImmutableList.copyOf(runtimeIdMap.values());
    }

    public void addCreativeItem(CloudItemStack item) {
        int damage = 0, brid = 0;
        NbtMap tag = item.getNbt(false);

        if (!tag.isEmpty() && tag.containsKey("Damage")) {
            damage = tag.getInt("Damage");
        }

        if (item.isBlock()) {
            brid = CloudBlockRegistry.get().getRuntimeId(item.getBlockState());
        }

        this.creativeItems.add(ItemData.builder()
                .usingNetId(false)
                .id(getRuntimeId(item.getId()))
                .damage(damage)
                .blockRuntimeId(brid)
                .build());
    }

    public Identifier fromLegacy(int legacyId, int meta) {
        Identifier id = legacyIdMap.get(legacyId);
        if (id == null) {
            throw new RegistryException("Unknkown legacy Id: " + legacyId);
        }
        if (metaMap.containsKey(id)) {
            return metaMap.get(id).get(meta);
        }
        return id;
    }

    public void registerVanillaCreativeItems() {
        try (InputStream in = RegistryUtils.getOrAssertResource("data/creative_items.json")) {
            JsonNode json = Bootstrap.JSON_MAPPER.readTree(in);
            for (JsonNode item : json.get("items")) {
                ItemData.Builder itemData = ItemData.builder();
                itemData.id(getRuntimeId(Identifier.fromString(item.get("id").asText())));

                if (item.has("blockRuntimeId")) {
                    itemData.blockRuntimeId(item.get("blockRuntimeId").asInt());
                }

                if (item.has("damage")) {
                    itemData.damage(item.get("damage").asInt());
                }

                if (item.has("nbt_n64")) {
                    try (NBTInputStream stream = NbtUtils.createReaderLE(new ByteArrayInputStream(Base64.getDecoder().decode(item.get("nbt_b64").asText())))) {
                        itemData.tag((NbtMap) stream.readTag());
                    }
                }

                itemData.usingNetId(false);

                creativeItems.add(itemData.build());
            }
        } catch (IOException | NumberFormatException e) {
            throw new RegistryException("Error loading Vanilla Creative Items", e);
        }
    }

    public List<ItemData> getCreativeItems() {
        return ImmutableList.copyOf(creativeItems);
    }
}


