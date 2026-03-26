package org.cloudburstmc.server.item;

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
import org.cloudburstmc.nbt.NbtUtils;
import org.cloudburstmc.protocol.bedrock.data.definitions.BlockDefinition;
import org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition;
import org.cloudburstmc.protocol.bedrock.data.inventory.*;
import org.cloudburstmc.protocol.bedrock.packet.CreativeContentPacket;
import org.cloudburstmc.server.Bootstrap;
import org.cloudburstmc.server.registry.CloudBlockRegistry;
import org.cloudburstmc.server.registry.CloudItemRegistry;
import org.cloudburstmc.server.registry.RegistryUtils;
import tools.jackson.databind.JsonNode;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Log4j2
public class ItemPalette {
    private final static Reference2ObjectMap<Identifier, Int2ReferenceMap<Identifier>> metaMap = new Reference2ObjectOpenHashMap<>();
    private final static Reference2ReferenceMap<Identifier, Identifier> simpleMap = new Reference2ReferenceOpenHashMap<>();
    private final static Reference2ReferenceMap<Identifier, CloudItemDefinition> itemEntries = new Reference2ReferenceOpenHashMap<>();
    private final static Int2ReferenceMap<CloudItemDefinition> runtimeIdMap = new Int2ReferenceOpenHashMap<>();
    private final static Int2ReferenceMap<Identifier> legacyIdMap = new Int2ReferenceOpenHashMap<>();
    private final static Int2ReferenceMap<Identifier> legacyBlockIdMap = new Int2ReferenceOpenHashMap<>();

    static {
        try (InputStream in = RegistryUtils.getOrAssertResource("data/legacy_item_ids.json")) {
            JsonNode json = Bootstrap.JSON_MAPPER.readTree(in);
            for (Map.Entry<String, JsonNode> entry : json.properties()) {
                Identifier id = Identifier.parse(entry.getKey());
                int legacyId = entry.getValue().intValue();
                legacyIdMap.put(legacyId, id);
            }
        } catch (IOException e) {
            throw new RegistryException("Unable to load legacy item IDs", e);
        }

        try (InputStream in = RegistryUtils.getOrAssertResource("data/legacy_block_ids.json")) {
            JsonNode json = Bootstrap.JSON_MAPPER.readTree(in);
            for (Map.Entry<String, JsonNode> entry : json.properties()) {
                Identifier id = Identifier.parse(entry.getKey());
                int legacyId = entry.getValue().intValue();
                legacyBlockIdMap.put(legacyId, id);
            }
        } catch (IOException e) {
            throw new RegistryException("Unable to load legacy block IDs", e);
        }

        try (InputStream in = RegistryUtils.getOrAssertResource("data/item_mappings.json")) {
            JsonNode json = Bootstrap.JSON_MAPPER.readTree(in);
            if (json.has("simple")) {
                JsonNode simpleNode = json.get("simple");
                for (Map.Entry<String, JsonNode> entry : simpleNode.properties()) {
                    Identifier oldId = Identifier.parse(entry.getKey());
                    Identifier newId = Identifier.parse(entry.getValue().asString());
                    simpleMap.put(oldId, newId);
                }
            }

            if (json.has("complex")) {
                JsonNode complexNode = json.get("complex");
                for (Map.Entry<String, JsonNode> entry : complexNode.properties()) {
                    Identifier id = Identifier.parse(entry.getKey());
                    Int2ReferenceMap<Identifier> map = metaMap.computeIfAbsent(id, i -> new Int2ReferenceOpenHashMap<>());
                    for (Map.Entry<String, JsonNode> value : entry.getValue().properties()) {
                        map.put(Integer.parseInt(value.getKey()), Identifier.parse(value.getValue().asString()));
                    }
                }
            }
        } catch (IOException | NumberFormatException e) {
            throw new RegistryException("Unable to load Legacy Meta Mapping", e);
        }

        NbtMap vanillaComponents;
        try (InputStream in = RegistryUtils.getOrAssertResource("data/item_components.nbt");
             NBTInputStream nbtStream = NbtUtils.createGZIPReader(in)) {
            vanillaComponents = (NbtMap) nbtStream.readTag();
        } catch (IOException e) {
            throw new RegistryException("Unable to load item components", e);
        }

        try (InputStream in = RegistryUtils.getOrAssertResource("data/runtime_item_states.json")) {
            JsonNode json = Bootstrap.JSON_MAPPER.readTree(in);
            for (JsonNode item : json) {
                String name = item.get("name").asString();
                Identifier id = Identifier.parse(name);
                int runtime = item.get("id").intValue();
                boolean componentBased = item.has("componentBased") && item.get("componentBased").asBoolean();
                ItemVersion version = ItemVersion.from(item.has("version") ? item.get("version").intValue() : 0);

                NbtMap components = vanillaComponents.getCompound(name);
                if (components != null && components.isEmpty()) {
                    components = null;
                }

                CloudItemDefinition definition = new CloudItemDefinition(id, runtime, componentBased, version, components);
                itemEntries.put(id, definition);
                runtimeIdMap.put(runtime, definition);

                if (!legacyIdMap.containsKey(runtime) && !legacyBlockIdMap.containsKey(runtime)) {
                    legacyIdMap.put(runtime, id);
                }
            }
        } catch (IOException e) {
            throw new RegistryException("Unable to load vanilla runtime mapping", e);
        }
    }

    private final CloudItemRegistry itemRegistry;
    private final AtomicInteger runtimeIdAllocator = new AtomicInteger(itemEntries.size());
    private final List<CreativeItemData> creativeItems = new ArrayList<>();
    private final List<CreativeItemGroup> creativeGroups = new ArrayList<>();
    private volatile CreativeContentPacket creativeContentPacket;

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
        if ((meta & 0x7FFF) == 0x7FFF) {
            meta = 0;
        }

        if (simpleMap.containsKey(id)) {
            id = simpleMap.get(id);
        }

        if (metaMap.containsKey(id)) {
            Identifier mapped = metaMap.get(id).get(meta);
            if (mapped != null) {
                id = mapped;
            }
        }

        CloudItemDefinition result = itemEntries.get(id);
        if (result == null && id != null) {
            Identifier canonical = Identifier.parse(id.toString());
            result = itemEntries.get(canonical);
        }

        return result;
    }

    public Identifier getIdByRuntime(int runtimeId) {
        return getIdByRuntime(runtimeId, 0);
    }

    public Identifier getIdByRuntime(int runtimeId, int meta) {
        ItemDefinition definition = runtimeIdMap.get(runtimeId);
        Identifier id = Identifier.parse(definition.getIdentifier());
        if (metaMap.containsKey(id)) {
            id = metaMap.get(id).get(meta);
        }
        return id;
    }

    public CreativeContentPacket getCreativeContentPacket() {
        if (creativeContentPacket == null) {
            this.creativeContentPacket = new CreativeContentPacket();
            this.creativeContentPacket.getGroups().addAll(creativeGroups);
            this.creativeContentPacket.getContents().addAll(creativeItems);
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

        int netId = creativeItems.size() + 1;
        ItemData itemData = ItemData.builder()
                .definition(getDefinition(item.getType().getId()))
                .damage(damage)
                .count(1)
                .netId(netId)
                .blockDefinition(blockDefinition)
                .build();

        creativeItems.add(new CreativeItemData(itemData, netId, 0));
        this.creativeContentPacket = null;
    }

    public Identifier fromLegacy(int legacyId, int meta) {
        Identifier id = legacyIdMap.get(legacyId);
        if (id == null) {
            id = legacyBlockIdMap.get(legacyId);
        }

        if (id == null) {
            throw new RegistryException("Unknown item Id: " + legacyId);
        }

        if (metaMap.containsKey(id)) {
            Identifier metaId = metaMap.get(id).get(meta);
            if (metaId != null) {
                return metaId;
            }
        }
        return id;
    }

    public void registerVanillaCreativeItems() {
        try (InputStream in = RegistryUtils.getOrAssertResource("data/creative_items.json")) {
            JsonNode json = Bootstrap.JSON_MAPPER.readTree(in);

            AtomicInteger creativeNetId = new AtomicInteger();
            for (JsonNode item : json.get("items")) {
                String identifier = item.get("id").asString();
                ItemDefinition definition = getDefinition(Identifier.parse(identifier));
                if (definition == null) {
                    log.debug("Unknown item definition {} when loading creative items, skipping", identifier);
                    continue;
                }

                ItemData.Builder itemData = ItemData.builder();
                itemData.definition(definition);
                itemData.netId(creativeNetId.incrementAndGet());

                if (item.has("block_state_b64")) {
                    try {
                        NbtMap blockState = decodeNbt(item.get("block_state_b64").asString());
                        BlockState state = CloudBlockRegistry.REGISTRY.getBlock(blockState);
                        if (state != null) {
                            itemData.blockDefinition(CloudBlockRegistry.REGISTRY.getDefinition(state));
                        }
                    } catch (Exception e) {
                        log.warn("Failed to resolve block state for creative item {}: {}", identifier, e.getMessage());
                    }
                }

                if (item.has("damage")) {
                    int meta = item.get("damage").asInt();
                    if ((meta & 0x7fff) == 0x7fff) meta = -1;
                    itemData.damage(meta);
                }

                if (item.has("nbt_b64")) {
                    itemData.tag(decodeNbt(item.get("nbt_b64").asString()));
                }

                itemData.count(1);

                int groupId = item.has("groupId") ? item.get("groupId").asInt() : 0;
                ItemData built = itemData.build();
                creativeItems.add(new CreativeItemData(built, built.getNetId(), groupId));
            }

            for (JsonNode groupNode : json.get("groups")) {
                CreativeItemCategory category = CreativeItemCategory.valueOf(
                        groupNode.get("category").asString().toUpperCase(Locale.ROOT));
                String name = groupNode.get("name").asString();

                JsonNode iconNode = groupNode.get("icon");
                String iconId = iconNode.get("id").asString();
                ItemData icon;

                if (iconId.equals("minecraft:air")) {
                    icon = ItemData.AIR;
                } else {
                    icon = creativeItems.stream()
                            .map(CreativeItemData::getItem)
                            .filter(i -> i.getDefinition().getIdentifier().equals(iconId))
                            .findFirst()
                            .orElse(null);

                    if (icon == null) {
                        ItemData.Builder iconBuilder = ItemData.builder();
                        iconBuilder.definition(getDefinition(Identifier.parse(iconId)));
                        iconBuilder.count(1);

                        if (iconNode.has("block_state_b64")) {
                            try {
                                NbtMap blockState = decodeNbt(iconNode.get("block_state_b64").asString());
                                BlockState state = CloudBlockRegistry.REGISTRY.getBlock(blockState);
                                if (state != null) {
                                    iconBuilder.blockDefinition(CloudBlockRegistry.REGISTRY.getDefinition(state));
                                }
                            } catch (Exception e) {
                                log.warn("Failed to resolve block state for creative group icon {}: {}", iconId, e.getMessage());
                            }
                        }

                        icon = iconBuilder.build();
                    }
                }

                creativeGroups.add(new CreativeItemGroup(category, name, icon));
            }

            log.info("Loaded §a{}§r creative items in §a{}§r groups", creativeItems.size(), creativeGroups.size());
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

    public List<CreativeItemData> getCreativeItems() {
        return ImmutableList.copyOf(creativeItems);
    }
}


