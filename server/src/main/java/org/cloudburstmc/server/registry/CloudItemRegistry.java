package org.cloudburstmc.server.registry;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.objects.Object2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.BlockRegistrationAccess;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.enchantment.EnchantmentTarget;
import org.cloudburstmc.api.entity.damage.DamageTypes;
import org.cloudburstmc.api.item.*;
import org.cloudburstmc.api.item.component.CanEnchantWithHandler;
import org.cloudburstmc.api.item.component.CanRepairWithHandler;
import org.cloudburstmc.api.registry.ItemRegistry;
import org.cloudburstmc.api.registry.RegistryException;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.api.util.component.ComponentBuilder;
import org.cloudburstmc.api.util.component.ComponentMap;
import org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition;
import org.cloudburstmc.protocol.bedrock.packet.CreativeContentPacket;
import org.cloudburstmc.protocol.common.DefinitionRegistry;
import org.cloudburstmc.server.item.CloudItemDefinition;
import org.cloudburstmc.server.item.ItemPalette;
import org.cloudburstmc.server.item.ItemUtils;
import org.cloudburstmc.server.item.component.DefaultItemHandlers;
import org.cloudburstmc.server.item.data.serializer.*;
import org.cloudburstmc.server.item.serializer.DefaultItemSerializer;
import org.cloudburstmc.server.item.serializer.ItemSerializer;
import org.cloudburstmc.server.registry.component.CloudComponentMap;

import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

public class CloudItemRegistry extends CloudComponentRegistry<ItemType> implements ItemRegistry, DefinitionRegistry<ItemDefinition> {
    private static final String ITEM_ALIAS_PREFIX = "item.";
    private static final CloudItemRegistry INSTANCE = new CloudItemRegistry();

    private final Object2ReferenceMap<Identifier, ItemType> typeMap = new Object2ReferenceOpenHashMap<>();
    private final Reference2ObjectMap<ItemType, ItemSerializer> serializers = new Reference2ObjectOpenHashMap<>();
    private final Map<ItemDataComponentType<?>, ItemDataComponentSerializer<?>> dataComponentSerializers = new LinkedHashMap<>();
    private final ItemPalette itemPalette = new ItemPalette(this);
    private int hardcodedBlockingId;
    private volatile boolean closed;

    private CloudItemRegistry() {
        try {
            this.registerVanillaBehaviors();
            ItemTypes.values().forEach(this::registerVanilla);
            VanillaItemBehaviors.configure(this);
            VanillaItemTags.freeze();
            this.registerVanillaDataComponentSerializers();
        } catch (RegistryException e) {
            throw new IllegalStateException("Unable to register vanilla items", e);
        }
    }

    public static CloudItemRegistry get() {
        return INSTANCE;
    }

    private synchronized <T> void registerDataComponentSerializer(ItemDataComponentType<T> dataType, ItemDataComponentSerializer<T> serializer) throws RegistryException {
        Preconditions.checkNotNull(dataType, "dataType");
        Preconditions.checkNotNull(serializer, "serializer");
        checkClosed();
        if (this.dataComponentSerializers.containsKey(dataType)) {
            throw new RegistryException("A serializer is already registered for item data component " + dataType.getId());
        }
        this.dataComponentSerializers.put(dataType, serializer);
    }

    public List<ItemDataComponentType<?>> getSerializedDataComponents() {
        return List.copyOf(this.dataComponentSerializers.keySet());
    }

    @Override
    public ComponentMap getComponents(ItemType type) {
        return super.getComponents(type);
    }

    public int getHardcodedBlockingId() {
        return this.hardcodedBlockingId;
    }

    protected synchronized CloudComponentMap registerVanilla(ItemType type) throws RegistryException {
        return registerVanilla(type, null);
    }

    @Override
    public ComponentBuilder configure(ItemType type) {
        checkClosed();
        CloudComponentMap components = getComponentMap(checkNotNull(type, "type"));
        if (components == null) {
            throw new RegistryException(type.getId() + " is not registered");
        }
        return components;
    }

    protected static CanEnchantWithHandler enchantableWith(EnchantmentTarget... targets) {
        Preconditions.checkNotNull(targets, "targets");
        for (EnchantmentTarget target : targets) {
            Preconditions.checkNotNull(target, "target");
        }

        Set<EnchantmentTarget> supportedTargets = targets.length == 0 ? Set.of() : EnumSet.copyOf(List.of(targets));
        return (item, enchantment) -> enchantment != null && supportedTargets.contains(enchantment.target());
    }

    protected static CanEnchantWithHandler toolEnchantableWith(EnchantmentTarget... additionalTargets) {
        Preconditions.checkNotNull(additionalTargets, "additionalTargets");
        for (EnchantmentTarget target : additionalTargets) {
            Preconditions.checkNotNull(target, "additionalTarget");
        }

        EnumSet<EnchantmentTarget> targets =
                EnumSet.of(EnchantmentTarget.TOOL, EnchantmentTarget.BREAKABLE, EnchantmentTarget.VANISHABLE);
        targets.addAll(List.of(additionalTargets));
        return enchantableWith(targets.toArray(EnchantmentTarget[]::new));
    }

    protected static CanRepairWithHandler repairWith(Identifier... materials) {
        Preconditions.checkNotNull(materials, "materials");
        for (Identifier repairMaterial : materials) {
            Preconditions.checkNotNull(repairMaterial, "repairMaterial");
        }

        return (item, material) -> {
            if (item.isEmpty() || material.isEmpty()) {
                return false;
            }

            Identifier materialId = material.getType().getId();
            for (Identifier repairMaterial : materials) {
                if (repairMaterial.equals(materialId)) {
                    return true;
                }
            }

            return false;
        };
    }

    protected static CanRepairWithHandler repairWith(ItemTagKey tag) {
        Preconditions.checkNotNull(tag, "tag");
        return (item, material) -> !item.isEmpty()
                && !material.isEmpty()
                && VanillaItemTags.resolve(tag).isTagged(material.getType());
    }

    protected ComponentBuilder configure(ItemType type, ItemSerializer serializer) {
        Objects.requireNonNull(type, "type");
        checkClosed();

        CloudComponentMap components = getComponentMap(type);
        if (components == null) {
            throw new RegistryException(type.getId() + " is not registered");
        }

        if (serializer != null) {
            this.serializers.put(type, serializer);
        }

        return components;
    }

    private synchronized CloudComponentMap registerVanilla(ItemType type, ItemSerializer serializer)
            throws RegistryException {
        Objects.requireNonNull(type, "type");
        checkClosed();

        if (getComponentMap(type) != null) {
            throw new RegistryException(type.getId() + " is already registered");
        }

        CloudComponentMap collection = new CloudComponentMap(this);
        //        collection.apply(DefaultBlockBehaviours.BLOCK_BEHAVIOR_BASE);

        putComponents(type, collection);

        if (serializer != null) {
            this.serializers.put(type, serializer);
        }

        this.registerItemType(type, type.getId());
        return collection;
    }

    protected synchronized void registerBlock(BlockType type) {
        ItemType itemType = this.typeMap.get(type.getId());
        if (itemType == null) {
            itemType = ItemTypes.get(type.getId()).orElseGet(() -> ItemType.of(type.getId()));
            this.registerItemType(itemType, type.getId());
        }

        BlockRegistrationAccess.linkItem(type, itemType);

        CloudComponentMap components = getComponentMap(itemType);

        if (components == null) {
            components = new CloudComponentMap(this);
            putComponents(itemType, components);
        }

        BlockState defaultState = type.getDefaultState();
        components.set(ItemBehaviors.GET_BLOCK, item -> {
            BlockState state = item.get(ItemDataComponents.BLOCK_STATE);
            return Optional.of(state != null ? state : defaultState);
        });
    }

    public ItemSerializer getSerializer(ItemType type) {
        return serializers.getOrDefault(type, DefaultItemSerializer.INSTANCE);
    }

    public ItemDataComponentSerializer<?> getDataComponentSerializer(ItemDataComponentType<?> dataType) {
        return dataComponentSerializers.get(dataType);
    }

    @Override
    public Optional<ItemType> get(Identifier id) {
        return Optional.ofNullable(this.typeMap.get(id));
    }

    @Override
    public Collection<ItemType> values() {
        LinkedHashSet<ItemType> values = new LinkedHashSet<>();
        for (Identifier id : this.getItems()) {
            this.get(id).ifPresent(values::add);
        }
        return ImmutableList.copyOf(values);
    }

    @Override
    public ItemTag getTag(ItemTagKey key) {
        return VanillaItemTags.resolve(key);
    }

    @Override
    public Collection<ItemTag> getTags() {
        return VanillaItemTags.all();
    }

    public Identifier fromLegacy(int legacyId, int meta) throws RegistryException {
        return itemPalette.fromLegacy(legacyId, meta);
    }

    @Override
    public CloudItemDefinition getDefinition(int runtimeId) {
        return itemPalette.getDefinition(runtimeId);
    }

    @Nullable
    @Override
    public CloudItemDefinition getDefinition(String identifier) {
        return itemPalette.getDefinition(Identifier.parse(identifier));
    }

    @Override
    public boolean isRegistered(ItemDefinition definition) {
        return definition instanceof CloudItemDefinition cloudDefinition
                && itemPalette.getDefinition(cloudDefinition.getRuntimeId()) == cloudDefinition;
    }

    public CloudItemDefinition getDefinition(Identifier identifier) throws RegistryException {
        return getDefinition(identifier, 0);
    }

    public CloudItemDefinition getDefinition(Identifier identifier, int meta) throws RegistryException {
        CloudItemDefinition definition = itemPalette.getDefinition(identifier, meta);
        if (definition == null) {
            throw new RegistryException(identifier + " is not a registered item");
        }
        return definition;
    }

    public ImmutableList<Identifier> getItems() {
        return ImmutableList.copyOf(itemPalette.getItemDefinitions().stream()
                .map(itemDefinition -> Identifier.parse(itemDefinition.getIdentifier()))
                .collect(Collectors.toList()));
    }

    public boolean isVanillaDefinition(Identifier id) {
        return this.itemPalette.isVanillaDefinition(id);
    }

    public boolean isCreativeItem(ItemType type) {
        return this.itemPalette.isCreativeItem(type.getId());
    }

    @Override
    public synchronized void close() throws RegistryException {
        checkClosed();
        reconcileVanillaDefinitions();
        VanillaRegistryDiagnostics.flush();
        this.freezeComponentMaps();
        this.closed = true;

        itemPalette.registerVanillaCreativeItems();
    }

    private void checkClosed() throws RegistryException {
        if (this.closed) {
            throw new RegistryException("Registration is closed");
        }
    }

    public List<ItemDefinition> getItemEntries() {
        return itemPalette.getItemPalette();
    }

    private void registerItemType(ItemType type, Identifier id) {
        this.typeMap.put(id, type);
        int runtime = itemPalette.addItem(id);
        if (type == ItemTypes.SHIELD) {
            this.hardcodedBlockingId = runtime;
        }
    }

    private void reconcileVanillaDefinitions() {
        for (ItemDefinition definition : this.itemPalette.getItemDefinitions()) {
            Identifier id = Identifier.parse(definition.getIdentifier());
            if (isRegisteredOrAliased(id)) {
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

        if (registerItemPrefixAlias(id)) {
            return true;
        }

        return registerMappedAlias(id);
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
        this.registerDataComponentSerializer(ItemDataComponents.DAMAGE, new PrimitiveSerializer<>("Damage", Integer.class));
        this.registerDataComponentSerializer(ItemDataComponents.ITEM_LOCK, new ItemLockModeSerializer());
        this.registerDataComponentSerializer(ItemDataComponents.KEEP_ON_DEATH, new PrimitiveSerializer<>("minecraft:keep_on_death", Boolean.class));
        this.registerDataComponentSerializer(ItemDataComponents.REPAIR_COST, new PrimitiveSerializer<>("RepairCost", Integer.class));
        this.registerDataComponentSerializer(ItemDataComponents.UNBREAKABLE, new PrimitiveSerializer<>("Unbreakable", Boolean.class));
        this.registerDataComponentSerializer(ItemDataComponents.MAP_DATA, new MapSerializer());
        this.registerDataComponentSerializer(ItemDataComponents.BOOK_DATA, new WrittenBookSerializer());
        this.registerDataComponentSerializer(ItemDataComponents.SPAWN_EGG_TYPE, new EntityTypeSerializer());
    }

    private void registerVanillaBehaviors() {
        this.registerComponent(ItemBehaviors.ALLOW_OFFHAND, () -> false);
        this.registerComponent(ItemBehaviors.ARMOR);
        this.registerComponent(ItemBehaviors.ATTACK_DAMAGE_TYPE, DamageTypes.PLAYER_ATTACK);
        this.registerComponent(ItemBehaviors.CAN_BE_CHARGED, () -> false);
        this.registerComponent(ItemBehaviors.CAN_BE_DEPLETED, () -> false);
        this.registerComponent(ItemBehaviors.CAN_BE_PLACED, (item) -> false);
        this.registerComponent(ItemBehaviors.CAN_BE_PLACED_ON, DefaultItemHandlers.CAN_BE_PLACED_ON);
        this.registerComponent(ItemBehaviors.CAN_DESTROY, DefaultItemHandlers.CAN_DESTROY);
        this.registerComponent(ItemBehaviors.CAN_DESTROY_IN_CREATIVE, () -> true);
        this.registerComponent(ItemBehaviors.CAN_ENCHANT_WITH, (item, enchantment) -> false);
        this.registerComponent(ItemBehaviors.CAN_REPAIR_WITH, (item, material) -> false);
        this.registerComponent(ItemBehaviors.CAN_STORE_ENCHANTMENTS, () -> true);
        this.registerComponent(ItemBehaviors.DAMAGEABLE, () -> false);
        this.registerComponent(ItemBehaviors.FINISH_USE);
        this.registerComponent(ItemBehaviors.FUEL_DURATION, () -> 0f);
        this.registerComponent(ItemBehaviors.GET_ATTACK_DAMAGE, (item) -> 1f);
        this.registerComponent(ItemBehaviors.GET_ATTACK_DURABILITY_DAMAGE, item -> 0);
        this.registerComponent(ItemBehaviors.GET_BLOCK, (item) -> Optional.empty());
        this.registerComponent(ItemBehaviors.GET_DAMAGE_CHANCE, (unbreaking) -> 0);
        this.registerComponent(ItemBehaviors.GET_EQUIPMENT_SLOT, item -> null);
        this.registerComponent(ItemBehaviors.GET_MAX_DAMAGE, (item) -> 0);
        this.registerComponent(ItemBehaviors.GET_MAX_STACK_SIZE, (item) -> 64);
        this.registerComponent(ItemBehaviors.GET_TOOL, item -> null);
        this.registerComponent(ItemBehaviors.MINE_BLOCK, (item, block, owner) -> item);
        this.registerComponent(ItemBehaviors.ON_DAMAGE, (item, damage, owner) -> item);
        this.registerComponent(ItemBehaviors.SPAWN_EGG);
        this.registerComponent(ItemBehaviors.USE);
        this.registerComponent(ItemBehaviors.USE_DURATION_TICKS);
        this.registerComponent(ItemBehaviors.USE_ON);
    }

    public void registerCreativeItem(ItemStack item) {
        Preconditions.checkNotNull(item, "item");
        itemPalette.addCreativeItem(item);
    }

    public ItemStack getCreativeItemByIndex(int index) {
        return ItemUtils.fromNetwork(itemPalette.getCreativeItems().get(index).getItem());
    }

    public CreativeContentPacket getCreativeContent() {
        return itemPalette.getCreativeContentPacket();
    }

    public int getCreativeItemIndex(ItemStack item) {
        int rid = itemPalette.getDefinition(item.getType().getId()).getRuntimeId();

        for (int i = 0; i < itemPalette.getCreativeItems().size(); i++) {
            if (rid
                    == itemPalette
                            .getCreativeItems()
                            .get(i)
                            .getItem()
                            .getDefinition()
                            .getRuntimeId()) {
                return i;
            }
        }

        return -1;
    }

    public ItemStack getCreativeItem(int creativeNetId) {
        for (var creativeItem : itemPalette.getCreativeItems()) {
            if (creativeItem.getNetId() == creativeNetId) {
                return ItemUtils.fromNetwork(creativeItem.getItem());
            }
        }
        return null;
    }
}
