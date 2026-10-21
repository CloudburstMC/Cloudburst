package org.cloudburstmc.server.registry;

import com.google.common.base.Preconditions;
import it.unimi.dsi.fastutil.objects.Object2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import lombok.Getter;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.BlockRegistrationAccess;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.entity.damage.DamageTypes;
import org.cloudburstmc.api.item.*;
import org.cloudburstmc.api.level.sound.SoundTypes;
import org.cloudburstmc.api.registry.ItemRegistry;
import org.cloudburstmc.api.registry.RegistryException;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition;
import org.cloudburstmc.protocol.bedrock.data.inventory.CreativeItemData;
import org.cloudburstmc.protocol.bedrock.packet.CreativeContentPacket;
import org.cloudburstmc.server.item.CloudItemDefinition;
import org.cloudburstmc.server.item.ItemPalette;
import org.cloudburstmc.server.item.ItemUtils;
import org.cloudburstmc.server.item.component.DefaultItemHandlers;
import org.cloudburstmc.server.item.data.serializer.*;
import org.cloudburstmc.server.item.serializer.DefaultItemSerializer;
import org.cloudburstmc.server.item.serializer.ItemSerializer;
import org.cloudburstmc.server.registry.component.CloudComponentMap;

import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Owns item types, behavior components, serializers and creative inventory entries.
 */
public class CloudItemRegistry extends CloudComponentRegistry<ItemType> implements ItemRegistry {

    private static final String ITEM_ALIAS_PREFIX = "item.";
    private static final CloudItemRegistry INSTANCE = new CloudItemRegistry();

    private final Object2ReferenceMap<Identifier, ItemType> typeMap = new Object2ReferenceOpenHashMap<>();
    private final Reference2ObjectMap<ItemType, ItemSerializer> serializers = new Reference2ObjectOpenHashMap<>();
    private final Map<ItemDataComponentType<?>, ItemDataComponentSerializer<?>> dataComponentSerializers = new LinkedHashMap<>();
    private final List<ItemType> types;
    @Getter
    private final List<ItemDataComponentType<?>> serializedDataComponents;
    private final List<ItemStack> pendingCreativeItems = new ArrayList<>();
    private final ItemPalette itemPalette = new ItemPalette();

    private volatile boolean closed;

    private CloudItemRegistry() {
        this.types = List.copyOf(ItemTypes.values());
        try {
            this.registerDefaultComponents();
            this.types.forEach(this::registerVanilla);
            VanillaItemBehaviors.configure(this);
            VanillaItemTags.freeze();
            this.registerVanillaDataComponentSerializers();
            this.serializedDataComponents = List.copyOf(this.dataComponentSerializers.keySet());
        } catch (RegistryException e) {
            throw new IllegalStateException("Unable to register vanilla items", e);
        }
    }

    public static CloudItemRegistry get() {
        return INSTANCE;
    }

    @Override
    public @NonNull CloudComponentMap getComponents(ItemType type) {
        CloudComponentMap components = this.getComponentMap(checkNotNull(type, "type"));
        if (components == null) {
            throw new RegistryException("Item type is not registered: " + type.getId());
        }
        return components;
    }

    @Override
    public synchronized CloudComponentMap configure(ItemType type) {
        this.requireOpen();
        return this.getComponents(type);
    }

    public synchronized CloudComponentMap configure(ItemType type, ItemSerializer serializer) {
        this.requireOpen();
        Objects.requireNonNull(serializer, "serializer");
        CloudComponentMap components = this.getComponents(type);
        this.serializers.put(type, serializer);
        return components;
    }

    public synchronized void registerBlockItem(ItemType itemType, BlockType placement, List<BlockType> forms) {
        this.requireOpen();
        CloudComponentMap components = this.getComponents(itemType);
        Objects.requireNonNull(placement, "placement");
        Objects.requireNonNull(forms, "forms");

        Set<BlockType> blockTypes = new HashSet<>(forms);
        blockTypes.add(placement);
        Set<BlockType> blocks = Set.copyOf(blockTypes);
        for (BlockType block : blocks) {
            BlockRegistrationAccess.linkItem(block, itemType);
            this.typeMap.putIfAbsent(block.getId(), itemType);
        }

        BlockState defaultState = placement.getDefaultState();
        components.set(ItemBehaviors.GET_BLOCK, item -> {
            BlockState state = item.get(ItemDataComponents.BLOCK_STATE);
            if (state == null) {
                return Optional.of(defaultState);
            }

            if (!blocks.contains(state.getType())) {
                return Optional.empty();
            }

            return Optional.of(state.getType() == placement ? state : defaultState.copyTraits(state));
        });
    }

    public ItemSerializer getSerializer(ItemType type) {
        this.getComponents(type);
        return this.serializers.getOrDefault(type, DefaultItemSerializer.INSTANCE);
    }

    public @Nullable ItemDataComponentSerializer<?> getDataComponentSerializer(ItemDataComponentType<?> dataType) {
        return this.dataComponentSerializers.get(checkNotNull(dataType, "dataType"));
    }

    @Override
    public Optional<ItemType> get(Identifier id) {
        return Optional.ofNullable(this.typeMap.get(checkNotNull(id, "id")));
    }

    @Override
    public Identifier getId(ItemType type) {
        checkNotNull(type, "type");
        if (this.getComponentMap(type) == null) {
            throw new IllegalArgumentException("Item type is not registered: " + type.getId());
        }
        return type.getId();
    }

    @Override
    public List<ItemType> values() {
        return this.types;
    }

    @Override
    public ItemTag getTag(ItemTagKey key) {
        return VanillaItemTags.resolve(checkNotNull(key, "key"));
    }

    @Override
    public Collection<ItemTag> getTags() {
        return VanillaItemTags.all();
    }

    public Identifier fromLegacy(int legacyId, int meta) throws RegistryException {
        return this.itemPalette.fromLegacy(legacyId, meta);
    }

    public ItemPalette getPalette() {
        return this.itemPalette;
    }

    public CloudItemDefinition getDefinition(Identifier identifier) throws RegistryException {
        return this.getDefinition(identifier, 0);
    }

    public CloudItemDefinition getDefinition(Identifier identifier, int meta) throws RegistryException {
        CloudItemDefinition definition = this.itemPalette.getDefinition(checkNotNull(identifier, "identifier"), meta);
        if (definition == null) {
            throw new RegistryException(identifier + " is not a registered item");
        }
        return definition;
    }

    public boolean isVanillaDefinition(Identifier id) {
        return this.itemPalette.isVanillaDefinition(id);
    }

    public boolean isCreativeItem(ItemType type) {
        return this.itemPalette.isCreativeItem(type.getId());
    }

    @Override
    public synchronized void registerCreativeItem(ItemStack item) {
        this.requireOpen();
        checkNotNull(item, "item");
        if (item.isEmpty()) {
            throw new RegistryException("Creative inventory entries must not be empty");
        }

        this.getComponents(item.getType());
        this.pendingCreativeItems.add(item);
    }

    public CreativeContentPacket getCreativeContent() {
        this.requireClosed();
        return this.itemPalette.getCreativeContentPacket();
    }

    public @Nullable ItemStack getCreativeItem(int creativeNetId) {
        this.requireClosed();
        CreativeItemData entry = this.itemPalette.getCreativeItem(creativeNetId);
        return entry == null ? null : ItemUtils.fromNetwork(entry.getItem());
    }

    @Override
    public synchronized void close() {
        if (this.closed) {
            return;
        }

        this.reconcileVanillaDefinitions();
        VanillaRegistryDiagnostics.flush();
        this.itemPalette.registerVanillaCreativeItems();
        this.pendingCreativeItems.forEach(this.itemPalette::addCreativeItem);

        this.freezeComponentMaps();
        this.itemPalette.freeze();
        this.pendingCreativeItems.clear();
        this.closed = true;
    }

    private void requireClosed() {
        if (!this.closed) {
            throw new RegistryException("Item registration has not closed");
        }
    }

    private void requireOpen() throws RegistryException {
        if (this.closed) {
            throw new RegistryException("Item registration is closed");
        }
    }

    private void registerVanilla(ItemType type) {
        Identifier id = type.getId();
        if (this.typeMap.containsKey(id)) {
            throw new RegistryException("Item identifier is already registered: " + id);
        }

        this.itemPalette.registerItem(id);
        this.putComponents(type, new CloudComponentMap(this));
        this.typeMap.put(id, type);
    }

    private <T> void registerDataComponentSerializer(ItemDataComponentType<T> dataType, ItemDataComponentSerializer<T> serializer) {
        Preconditions.checkNotNull(dataType, "dataType");
        Preconditions.checkNotNull(serializer, "serializer");
        this.requireOpen();
        if (this.dataComponentSerializers.containsKey(dataType)) {
            throw new RegistryException("A serializer is already registered for item data component " + dataType.getId());
        }

        this.dataComponentSerializers.put(dataType, serializer);
    }

    private void reconcileVanillaDefinitions() {
        for (ItemDefinition definition : this.itemPalette.getDefinitions()) {
            Identifier id = Identifier.parse(definition.getIdentifier());
            if (this.isRegisteredOrAliased(id)) {
                continue;
            }

            VanillaRegistryDiagnostics.missingVanillaItemDefinition(id);
            this.typeMap.put(id, ItemTypes.UNKNOWN);
        }
    }

    private boolean isRegisteredOrAliased(Identifier id) {
        if (this.typeMap.containsKey(id)) {
            return true;
        }

        if (this.registerItemPrefixAlias(id)) {
            return true;
        }

        return this.registerMappedAlias(id);
    }

    private boolean registerItemPrefixAlias(Identifier id) {
        String name = id.getName();
        if (!name.startsWith(ITEM_ALIAS_PREFIX)) {
            return false;
        }

        Identifier baseId = Identifier.from(id.getNamespace(), name.substring(ITEM_ALIAS_PREFIX.length()));
        ItemType type = this.typeMap.get(baseId);
        if (type == null) {
            return false;
        }

        this.typeMap.put(id, type);
        return true;
    }

    private boolean registerMappedAlias(Identifier id) {
        CloudItemDefinition mappedDefinition = this.itemPalette.getDefinition(id);
        if (mappedDefinition == null) {
            return false;
        }

        Identifier mappedId = Identifier.parse(mappedDefinition.getIdentifier());
        if (mappedId.equals(id)) {
            return false;
        }

        ItemType type = this.typeMap.get(mappedId);
        if (type == null) {
            return false;
        }

        this.typeMap.put(id, type);
        return true;
    }

    private void registerVanillaDataComponentSerializers() throws RegistryException {
        this.registerDataComponentSerializer(ItemDataComponents.BANNER_DATA, new BannerDataSerializer());
        this.registerDataComponentSerializer(ItemDataComponents.DYED_COLOR, new DyedColorSerializer());
        this.registerDataComponentSerializer(ItemDataComponents.DAMAGE, new PrimitiveSerializer<>("Damage", Integer.class));
        this.registerDataComponentSerializer(ItemDataComponents.ITEM_LOCK, new ItemLockModeSerializer());
        this.registerDataComponentSerializer(ItemDataComponents.KEEP_ON_DEATH, new PrimitiveSerializer<>("minecraft:keep_on_death", Boolean.class));
        this.registerDataComponentSerializer(ItemDataComponents.REPAIR_COST, new PrimitiveSerializer<>("RepairCost", Integer.class));
        this.registerDataComponentSerializer(ItemDataComponents.UNBREAKABLE, new PrimitiveSerializer<>("Unbreakable", Boolean.class));
        this.registerDataComponentSerializer(ItemDataComponents.MAP_ID, new MapSerializer());
        this.registerDataComponentSerializer(ItemDataComponents.MAP_TRACKING_POSITION, new PrimitiveSerializer<>("map_display_players", Boolean.class));
        this.registerDataComponentSerializer(ItemDataComponents.BOOK_DATA, new WrittenBookSerializer());
        this.registerDataComponentSerializer(ItemDataComponents.CHARGED_PROJECTILE, new ChargedProjectileSerializer());
        this.registerDataComponentSerializer(ItemDataComponents.SPAWN_EGG_TYPE, new EntityTypeSerializer());
        this.registerDataComponentSerializer(ItemDataComponents.ENTITY_DATA, new EntitySnapshotSerializer());
    }

    private void registerDefaultComponents() {
        this.registerComponent(ItemBehaviors.ALLOW_OFFHAND, () -> false);
        this.registerComponent(ItemBehaviors.ARMOR);
        this.registerComponent(ItemBehaviors.ATTACK_DAMAGE_TYPE, DamageTypes.PLAYER_ATTACK);
        this.registerComponent(ItemBehaviors.BLOCKS_ATTACKS);
        this.registerComponent(ItemBehaviors.CAN_BE_CHARGED, () -> false);
        this.registerComponent(ItemBehaviors.CAN_BE_DEPLETED, () -> false);
        this.registerComponent(ItemBehaviors.CAN_BE_PLACED, (item) -> false);
        this.registerComponent(ItemBehaviors.CAN_BE_PLACED_ON, DefaultItemHandlers.CAN_BE_PLACED_ON);
        this.registerComponent(ItemBehaviors.CAN_DESTROY, DefaultItemHandlers.CAN_DESTROY);
        this.registerComponent(ItemBehaviors.CAN_DESTROY_IN_CREATIVE, () -> true);
        this.registerComponent(ItemBehaviors.CAN_ENCHANT_WITH, (item, enchantment) -> false);
        this.registerComponent(ItemBehaviors.CAN_REPAIR_WITH, (item, material) -> false);
        this.registerComponent(ItemBehaviors.CAN_STORE_ENCHANTMENTS, () -> true);
        this.registerComponent(ItemBehaviors.CONSUMABLE);
        this.registerComponent(ItemBehaviors.DEATH_PROTECTION);
        this.registerComponent(ItemBehaviors.DAMAGEABLE, () -> false);
        this.registerComponent(ItemBehaviors.FINISH_USE);
        this.registerComponent(ItemBehaviors.FOOD);
        this.registerComponent(ItemBehaviors.FUEL_DURATION, () -> 0f);
        this.registerComponent(ItemBehaviors.GET_ATTACK_DAMAGE, (item) -> 1f);
        this.registerComponent(ItemBehaviors.GET_ATTACK_DURABILITY_DAMAGE, item -> 0);
        this.registerComponent(ItemBehaviors.GET_BLOCK, (item) -> Optional.empty());
        this.registerComponent(ItemBehaviors.GET_BLOCKING_DISABLE_SECONDS, item -> 0f);
        this.registerComponent(ItemBehaviors.GET_DAMAGE_CHANCE, (unbreaking) -> 0);
        this.registerComponent(ItemBehaviors.GET_EQUIPMENT_SLOT, item -> null);
        this.registerComponent(ItemBehaviors.GET_MAX_DAMAGE, (item) -> 0);
        this.registerComponent(ItemBehaviors.GET_MAX_STACK_SIZE, (item) -> 64);
        this.registerComponent(ItemBehaviors.GET_TOOL, item -> null);
        this.registerComponent(ItemBehaviors.MINE_BLOCK, (item, block, owner) -> item);
        this.registerComponent(ItemBehaviors.ON_BREAK, DefaultItemHandlers.breakEffects(SoundTypes.RANDOM_BREAK));
        this.registerComponent(ItemBehaviors.ON_DAMAGE, (item, damage, owner) -> item);
        this.registerComponent(ItemBehaviors.RELEASE_USE);
        this.registerComponent(ItemBehaviors.SPAWN_EGG);
        this.registerComponent(ItemBehaviors.STAB);
        this.registerComponent(ItemBehaviors.USE);
        this.registerComponent(ItemBehaviors.USE_DURATION_TICKS);
        this.registerComponent(ItemBehaviors.USE_ON);
        this.registerComponent(ItemBehaviors.USE_TICK);
    }


}
