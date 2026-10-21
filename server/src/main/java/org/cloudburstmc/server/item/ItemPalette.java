package org.cloudburstmc.server.item;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import lombok.extern.log4j.Log4j2;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.registry.RegistryException;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.api.util.Identifiers;
import org.cloudburstmc.nbt.NBTInputStream;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtUtils;
import org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition;
import org.cloudburstmc.protocol.bedrock.data.inventory.CreativeItemCategory;
import org.cloudburstmc.protocol.bedrock.data.inventory.CreativeItemData;
import org.cloudburstmc.protocol.bedrock.data.inventory.CreativeItemGroup;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.protocol.bedrock.packet.CreativeContentPacket;
import org.cloudburstmc.protocol.common.DefinitionRegistry;
import org.cloudburstmc.server.Bootstrap;
import org.cloudburstmc.server.block.BlockPalette;
import org.cloudburstmc.server.block.BlockStateCodec;
import org.cloudburstmc.server.registry.RegistryUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Indexes item definitions and builds ordered creative inventory content.
 */
@Log4j2
public class ItemPalette implements DefinitionRegistry<ItemDefinition> {

    private static final VanillaItemPaletteData VANILLA = VanillaItemPaletteData.load();

    private final Map<Identifier, CloudItemDefinition> definitions = new LinkedHashMap<>(VANILLA.definitions());
    private final Int2ReferenceMap<CloudItemDefinition> runtimeDefinitions = new Int2ReferenceOpenHashMap<>();
    private final List<CreativeItemData> creativeItems = new ArrayList<>();
    private final List<CreativeItemGroup> creativeGroups = new ArrayList<>();
    private final Set<Identifier> creativeItemTypes = new HashSet<>();

    private int nextRuntimeId;
    private int customCreativeGroup = -1;
    private boolean vanillaCreativeLoaded;
    private volatile boolean frozen;

    public ItemPalette() {
        for (CloudItemDefinition definition : this.definitions.values()) {
            this.runtimeDefinitions.put(definition.getRuntimeId(), definition);
            this.nextRuntimeId = Math.max(this.nextRuntimeId, definition.getRuntimeId() + 1);
        }
    }

    @Override
    public @Nullable CloudItemDefinition getDefinition(int runtimeId) {
        return this.runtimeDefinitions.get(runtimeId);
    }

    @Override
    public @Nullable CloudItemDefinition getDefinition(String identifier) {
        return this.getDefinition(Identifier.parse(identifier));
    }

    @Override
    public boolean isRegistered(ItemDefinition definition) {
        return definition instanceof CloudItemDefinition item && this.runtimeDefinitions.get(item.getRuntimeId()) == item;
    }

    public @Nullable CloudItemDefinition getDefinition(Identifier id) {
        return this.getDefinition(id, 0);
    }

    public @Nullable CloudItemDefinition getDefinition(Identifier id, int meta) {
        return this.definitions.get(resolveIdentifier(Objects.requireNonNull(id, "id"), meta));
    }

    public List<ItemDefinition> getDefinitions() {
        return List.copyOf(this.definitions.values());
    }

    public boolean isVanillaDefinition(Identifier id) {
        return VANILLA.definitions().containsKey(Objects.requireNonNull(id, "id"));
    }

    public Identifier fromLegacy(int legacyId, int meta) {
        Identifier id = VANILLA.legacyIds().get(legacyId);
        if (id == null) {
            throw new RegistryException("Unknown legacy item ID: " + legacyId);
        }
        return resolveIdentifier(id, meta);
    }

    public void registerItem(Identifier id) {
        this.requireOpen();
        Objects.requireNonNull(id, "id");
        if (this.definitions.containsKey(id)) {
            return;
        }

        if (this.nextRuntimeId > Short.MAX_VALUE) {
            throw new RegistryException("Item runtime ID space is exhausted");
        }

        CloudItemDefinition definition = new CloudItemDefinition(id, this.nextRuntimeId++, false);
        this.definitions.put(id, definition);
        this.runtimeDefinitions.put(definition.getRuntimeId(), definition);
    }

    public @Nullable CreativeItemData getCreativeItem(int networkId) {
        return networkId > 0 && networkId <= this.creativeItems.size() ? this.creativeItems.get(networkId - 1) : null;
    }

    public boolean isCreativeItem(Identifier id) {
        return this.creativeItemTypes.contains(Objects.requireNonNull(id, "id"));
    }

    public CreativeContentPacket getCreativeContentPacket() {
        CreativeContentPacket packet = new CreativeContentPacket();
        packet.getGroups().addAll(this.creativeGroups);
        packet.getContents().addAll(this.creativeItems);
        return packet;
    }

    public void addCreativeItem(ItemStack item) {
        this.requireOpen();
        Objects.requireNonNull(item, "item");
        if (item.isEmpty() || this.getDefinition(item.getType().getId()) == null) {
            throw new RegistryException("Creative inventory entry must have a registered, nonempty item type");
        }

        int networkId = this.creativeItems.size() + 1;
        ItemData data = ItemUtils.toNetwork(item).toBuilder().netId(networkId).build();
        if (this.customCreativeGroup == -1) {
            this.customCreativeGroup = this.creativeGroups.size();
            this.creativeGroups.add(new CreativeItemGroup(CreativeItemCategory.ITEMS, "", ItemData.AIR));
        }

        this.creativeItems.add(new CreativeItemData(data, networkId, this.customCreativeGroup));
        this.creativeItemTypes.add(item.getType().getId());
    }

    public void registerVanillaCreativeItems() {
        this.requireOpen();
        if (this.vanillaCreativeLoaded) {
            return;
        }

        if (!this.creativeItems.isEmpty()) {
            throw new RegistryException("Vanilla creative content must be loaded before additional entries");
        }

        CreativeData data;
        try (InputStream input = RegistryUtils.getOrAssertResource("data/creative_items.json")) {
            data = Objects.requireNonNull(Bootstrap.JSON_MAPPER.readValue(input, CreativeData.class), "creative data");
        } catch (IOException | RuntimeException failure) {
            throw new RegistryException("Unable to load creative inventory data", failure);
        }

        List<CreativeItemGroup> groups = this.readCreativeGroups(Objects.requireNonNull(data.groups(), "creative groups"));
        List<CreativeItemData> items = this.readCreativeItems(Objects.requireNonNull(data.items(), "creative items"), groups.size());
        this.creativeGroups.addAll(groups);
        this.creativeItems.addAll(items);
        items.forEach(item -> this.creativeItemTypes.add(Identifier.parse(item.getItem().getDefinition().getIdentifier())));
        this.vanillaCreativeLoaded = true;

        log.info("Loaded {} creative items in {} groups", items.size(), groups.size());
    }

    public void freeze() {
        this.frozen = true;
    }

    private void requireOpen() {
        if (this.frozen) {
            throw new RegistryException("Item palette registration is closed");
        }
    }

    private static Identifier resolveIdentifier(Identifier id, int meta) {
        if ((meta & 0x7fff) == 0x7fff) {
            meta = 0;
        }

        Identifier resolved = VANILLA.aliases().getOrDefault(id, id);
        Map<Integer, Identifier> variants = VANILLA.variants().get(resolved);
        return variants == null ? resolved : variants.getOrDefault(meta, resolved);
    }

    private List<CreativeItemGroup> readCreativeGroups(List<CreativeGroupEntry> entries) {
        List<CreativeItemGroup> groups = new ArrayList<>(entries.size());
        for (CreativeGroupEntry entry : entries) {
            try {
                CreativeItemCategory category = CreativeItemCategory.valueOf(entry.category().toUpperCase(Locale.ROOT));
                ItemData icon = this.readCreativeStack(Objects.requireNonNull(entry.icon(), "creative group icon"));
                groups.add(new CreativeItemGroup(category, Objects.requireNonNull(entry.name(), "creative group name"), icon));
            } catch (IllegalArgumentException | NullPointerException failure) {
                throw new RegistryException("Invalid creative group: " + entry.name(), failure);
            }
        }
        return groups;
    }

    private List<CreativeItemData> readCreativeItems(List<CreativeEntry> entries, int groupCount) {
        List<CreativeItemData> items = new ArrayList<>(entries.size());
        for (CreativeEntry entry : entries) {
            int groupId = Objects.requireNonNull(entry.groupId(), "creative item group ID");
            if (groupId < 0 || groupId >= groupCount) {
                throw new RegistryException("Invalid creative group index " + groupId + " on " + entry.id());
            }

            int networkId = items.size() + 1;
            ItemData data = this.readCreativeStack(entry).toBuilder().netId(networkId).build();
            if (Identifiers.AIR.toString().equals(data.getDefinition().getIdentifier())) {
                throw new RegistryException("Creative inventory entries must not be air");
            }
            items.add(new CreativeItemData(data, networkId, groupId));
        }

        return items;
    }

    private ItemData readCreativeStack(CreativeEntry entry) {
        try {
            Identifier id = Identifier.parse(Objects.requireNonNull(entry.id(), "creative item identifier"));
            if (id.equals(Identifiers.AIR)) {
                return ItemData.AIR;
            }

            CloudItemDefinition definition = this.getDefinition(id);
            if (definition == null) {
                throw new RegistryException("Unknown creative item definition: " + id);
            }

            int damage = entry.damage() == null ? 0 : entry.damage();
            ItemData.Builder item = ItemData.builder().definition(definition).count(1).damage((damage & 0x7fff) == 0x7fff ? -1 : damage);
            if (entry.blockState() != null) {
                BlockState state = BlockStateCodec.read(decodeNbt(entry.blockState()));
                item.blockDefinition(BlockPalette.INSTANCE.getDefinition(state));
            }

            if (entry.tag() != null) {
                item.tag(decodeNbt(entry.tag()));
            }

            return item.build();
        } catch (IOException | IllegalArgumentException failure) {
            throw new RegistryException("Invalid creative item data on " + entry.id(), failure);
        }
    }

    private static NbtMap decodeNbt(String encoded) throws IOException {
        byte[] bytes = Base64.getDecoder().decode(encoded);
        try (NBTInputStream reader = NbtUtils.createReaderLE(new ByteArrayInputStream(bytes))) {
            if (!(reader.readTag() instanceof NbtMap compound)) {
                throw new IOException("Creative item data must contain an NBT compound");
            }
            return compound;
        }
    }

    private record CreativeData(List<CreativeGroupEntry> groups, List<CreativeEntry> items) {}

    private record CreativeGroupEntry(String category, String name, CreativeEntry icon) {}

    private record CreativeEntry(
            String id,
            @Nullable Integer damage,
            @Nullable Integer groupId,
            @JsonProperty("block_state_b64") @Nullable String blockState,
            @JsonProperty("nbt_b64") @Nullable String tag
    ) {}
}
