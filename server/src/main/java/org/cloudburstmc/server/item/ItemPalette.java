package org.cloudburstmc.server.item;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.*;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.api.block.BlockIds;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.registry.RegistryException;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.nbt.NBTInputStream;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtUtils;
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
    private final static Reference2ReferenceMap<Identifier, ItemDefinition> itemEntries = new Reference2ReferenceOpenHashMap<>();
    private final static Reference2IntMap<Identifier> idRuntimeMap = new Reference2IntOpenHashMap<>();
    private final static Int2ReferenceMap<Identifier> runtimeIdMap = new Int2ReferenceOpenHashMap<>();

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
                itemEntries.put(id, new ItemDefinition(id.toString(), (short) runtime, false));
                runtimeIdMap.put(runtime, id);
                idRuntimeMap.put(id, runtime);
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
        idRuntimeMap.put(BlockIds.AIR, 0);
        runtimeIdMap.put(0, BlockIds.AIR);
    }

    public int addItem(Identifier identifier) {
        if (!itemEntries.containsKey(identifier)) {
            int runtimeId = runtimeIdAllocator.getAndIncrement();
            runtimeIdMap.put(runtimeId, identifier);
            idRuntimeMap.putIfAbsent(identifier, runtimeId);

            itemEntries.put(identifier, new ItemDefinition(identifier.toString(), runtimeId, false));
            return runtimeId;
        }
        return -1;
    }

    public int getRuntimeId(Identifier id) {
        return getRuntimeId(id, 0);
    }

    public int getRuntimeId(Identifier id, int meta) {
        if ((meta & 0x7FFF) == 0x7FFF) meta = 0;
        if (metaMap.containsKey(id)) {
            id = metaMap.get(id).get(meta);
        }
        int rid = idRuntimeMap.getOrDefault(id, Integer.MAX_VALUE);
        return rid;
    }

    public Identifier getIdByRuntime(int runtimeId) {
        return getIdByRuntime(runtimeId, 0);
    }

    public Identifier getIdByRuntime(int runtimeId, int meta) {
        Identifier id = runtimeIdMap.get(runtimeId);
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

    public ImmutableList<Identifier> getItemIds() {
        return ImmutableList.copyOf(runtimeIdMap.values());
    }

    public void addCreativeItem(ItemStack item) {
        int damage = 0, brid = 0;

        if (item.isBlock()) {
//            brid = CloudBlockRegistry.get().getRuntimeId(item.getBlockState());
        }

        this.creativeItems.add(ItemData.builder()
                .usingNetId(false)
                .definition(getRuntimeId(item.getType().getId()))
                .damage(damage)
                .blockRuntimeId(brid)
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
                itemData.definition(getRuntimeId(Identifier.fromString(item.get("id").asText())));

                if (item.has("block_state_b64")) {
                    NbtMap blockState = decodeNbt(item.get("block_state_b64").asText());
                    NbtMapBuilder builder = blockState.toBuilder();
                    builder.remove("name_hash");
                    blockState = builder.build();

                    BlockState state = CloudBlockRegistry.REGISTRY.getBlock(blockState);
                    if (state != null) {
                        itemData.blockDefinition(CloudBlockRegistry.REGISTRY.getRuntimeId(state));
                    }
                }
                if (item.has("blockRuntimeId")) {
                    itemData.blockDefinition(item.get("blockRuntimeId").asInt());
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


