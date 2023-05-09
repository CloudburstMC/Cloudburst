package org.cloudburstmc.server.item;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.registry.RegistryException;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.api.util.Identifiers;
import org.cloudburstmc.nbt.NBTInputStream;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtUtils;
import org.cloudburstmc.protocol.bedrock.data.defintions.BlockDefinition;
import org.cloudburstmc.protocol.bedrock.data.defintions.ItemDefinition;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.protocol.bedrock.packet.CreativeContentPacket;
import org.cloudburstmc.server.Bootstrap;
import org.cloudburstmc.server.registry.CloudBlockRegistry;
import org.cloudburstmc.server.registry.CloudItemRegistry;
import org.cloudburstmc.server.registry.RegistryUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Log4j2
public class ItemPalette {
    private final static BiMap<Integer, Identifier> legacyIdMap = HashBiMap.create();
    private final static Reference2ObjectMap<Identifier, Int2ReferenceMap<Identifier>> metaMap = new Reference2ObjectOpenHashMap<>();
    private final CloudItemRegistry itemRegistry;
    private final static Reference2ReferenceMap<Identifier, CloudItemDefinition> itemEntries = new Reference2ReferenceOpenHashMap<>();
    private final static Int2ReferenceMap<CloudItemDefinition> runtimeIdMap = new Int2ReferenceOpenHashMap<>();

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

        try (InputStream in = RegistryUtils.getOrAssertResource("data/runtime_item_states.json")) {
            JsonNode json = Bootstrap.JSON_MAPPER.readTree(in);
            for (JsonNode item : json) {
                Identifier id = Identifier.fromString(item.get("name").asText());
                int runtime = item.get("id").intValue();
                CloudItemDefinition definition = new CloudItemDefinition(id, runtime, false);
                itemEntries.put(id, definition);
                runtimeIdMap.put(runtime, definition);
            }
        } catch (IOException e) {
            throw new RegistryException("Unable to load vanilla runtime mapping", e);
        }
    }

    private final AtomicInteger runtimeIdAllocator = new AtomicInteger(itemEntries.size());
    private volatile CreativeContentPacket creativeContentPacket;
    private final List<ItemData> creativeItems = new ArrayList<>();

    public ItemPalette(CloudItemRegistry registry) {
        this.itemRegistry = registry;
        runtimeIdMap.put(0, new CloudItemDefinition(Identifiers.AIR, 0, false));
    }

    public int addItem(Identifier identifier) {
        if (!itemEntries.containsKey(identifier)) {
            int runtimeId = runtimeIdAllocator.getAndIncrement();
            CloudItemDefinition definition = new CloudItemDefinition(identifier, runtimeId, false);
            runtimeIdMap.put(runtimeId, definition);

            itemEntries.put(identifier, definition);
            return runtimeId;
        }
        return -1;
    }

    public CloudItemDefinition getDefinition(int runtimeId) {
        return runtimeIdMap.get(runtimeId);
    }

    public CloudItemDefinition getDefinition(Identifier id) {
        return getDefinition(id, 0);
    }

    public CloudItemDefinition getDefinition(Identifier id, int meta) {
        if ((meta & 0x7FFF) == 0x7FFF) meta = 0;
        if (metaMap.containsKey(id)) {
            id = metaMap.get(id).get(meta);
        }
        return itemEntries.get(id);
    }

    public Identifier getIdByRuntime(int runtimeId) {
        return getIdByRuntime(runtimeId, 0);
    }

    public Identifier getIdByRuntime(int runtimeId, int meta) {
        ItemDefinition definition = runtimeIdMap.get(runtimeId);
        Identifier id = Identifier.fromString(definition.getIdentifier());
        if (metaMap.containsKey(id)) {
            id = metaMap.get(id).get(meta);
        }
        return id;
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

    public List<ItemDefinition> getItemPalette() {
        return ImmutableList.copyOf(itemEntries.values());
    }

    public ImmutableList<ItemDefinition> getItemDefinitions() {
        return ImmutableList.copyOf(runtimeIdMap.values());
    }

    public void addCreativeItem(ItemStack item) {
        int damage = 0;
        BlockDefinition blockDefinition = null;

        if (item.isBlock()) {
            blockDefinition = CloudBlockRegistry.REGISTRY.getDefinition(item.getBlockState().get());
        }

        this.creativeItems.add(ItemData.builder()
                .usingNetId(false)
                .definition(getDefinition(item.getType().getId()))
                .damage(damage)
                .blockDefinition(blockDefinition)
                .build());
    }

    public Identifier fromLegacy(int legacyId, int meta) {
        Identifier id = legacyIdMap.get(legacyId);
        if (id == null) {
            throw new RegistryException("Unknown legacy Id: " + legacyId);
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
                itemData.definition(getDefinition(Identifier.fromString(item.get("id").asText())));

                if (item.has("block_state_b64")) {
                    NbtMap blockState = decodeNbt(item.get("block_state_b64").asText());
                    NbtMapBuilder builder = blockState.toBuilder();
                    builder.remove("name_hash");
                    blockState = builder.build();

                    BlockState state = CloudBlockRegistry.REGISTRY.getBlock(blockState);
                    if (state != null) {
                        itemData.blockDefinition(CloudBlockRegistry.REGISTRY.getDefinition(state));
                    }
                }
                if (item.has("blockRuntimeId")) {
                    itemData.blockDefinition(CloudBlockRegistry.REGISTRY.getDefinition(item.get("blockRuntimeId").asInt()));
                }

                if (item.has("damage")) {
                    int meta = item.get("damage").asInt();
                    if ((meta & 0x7fff) == 0x7fff) meta = -1;
                    itemData.damage(meta);
                }

                if (item.has("nbt_b64")) {
                    itemData.tag(decodeNbt(item.get("nbt_b64").asText()));
                }

                itemData.usingNetId(false)
                        .count(1);
                creativeItems.add(itemData.build());
            }
        } catch (IOException | NumberFormatException e) {
            throw new RegistryException("Error loading Vanilla Creative Items", e);
        }
    }

    private NbtMap decodeNbt(String base64) {
        byte[] nbtBytes = Base64.getDecoder().decode(base64);
        try (NBTInputStream stream = NbtUtils.createReaderLE(new ByteArrayInputStream(nbtBytes))) {
            return (NbtMap) stream.readTag();
        } catch (Exception e) {
            throw new AssertionError("Unable to decode NBT value", e);
        }
    }

    public List<ItemData> getCreativeItems() {
        return ImmutableList.copyOf(creativeItems);
    }
}


