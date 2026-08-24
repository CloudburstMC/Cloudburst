package org.cloudburstmc.server.registry;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.objects.*;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.data.DataKey;
import org.cloudburstmc.api.enchantment.EnchantmentTarget;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.item.*;
import org.cloudburstmc.api.item.component.CanEnchantWithHandler;
import org.cloudburstmc.api.item.component.CanRepairWithHandler;
import org.cloudburstmc.api.registry.ItemRegistry;
import org.cloudburstmc.api.registry.RegistryException;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.api.util.component.ComponentMap;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition;
import org.cloudburstmc.protocol.bedrock.packet.CreativeContentPacket;
import org.cloudburstmc.protocol.common.DefinitionRegistry;
import org.cloudburstmc.server.block.BlockPalette;
import org.cloudburstmc.server.item.CloudItemDefinition;
import org.cloudburstmc.server.item.ItemPalette;
import org.cloudburstmc.server.item.ItemUtils;
import org.cloudburstmc.server.item.VanillaTools;
import org.cloudburstmc.server.item.component.*;
import org.cloudburstmc.server.item.data.serializer.*;
import org.cloudburstmc.server.item.serializer.*;
import org.cloudburstmc.server.level.Sound;
import org.cloudburstmc.server.registry.component.CloudComponentMap;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class CloudItemRegistry extends CloudComponentRegistry<ItemType> implements ItemRegistry, DefinitionRegistry<ItemDefinition> {
    private static final String ITEM_ALIAS_PREFIX = "item.";
    private static final CloudItemRegistry INSTANCE = new CloudItemRegistry();

    private final Object2ReferenceMap<Identifier, ItemType> typeMap = new Object2ReferenceOpenHashMap<>();
    private final Reference2ObjectMap<ItemType, ItemSerializer> serializers = new Reference2ObjectOpenHashMap<>();
    private final Reference2ObjectMap<DataKey<?, ?>, ItemDataSerializer<?>> dataSerializers = new Reference2ObjectLinkedOpenHashMap<>();
    private final ItemPalette itemPalette = new ItemPalette(this);
    private int hardcodedBlockingId;
    private volatile boolean closed;

    private CloudItemRegistry() {
        try {
            this.registerVanillaBehaviors();
            this.registerVanillaItems();
            VanillaItemTags.freeze();
            this.registerVanillaDataSerializers();
        } catch (RegistryException e) {
            throw new IllegalStateException("Unable to register vanilla items", e);
        }
    }

    public static CloudItemRegistry get() {
        return INSTANCE;
    }

    public synchronized <T> void registerDataSerializer(DataKey<T, T> dataKey, ItemDataSerializer<T> serializer) throws RegistryException {
        Preconditions.checkNotNull(dataKey, "dataKey");
        Preconditions.checkNotNull(serializer, "serializer");
        checkClosed();
        this.dataSerializers.put(dataKey, serializer);
    }

    public List<DataKey<?, ?>> getSerializedDataKeys() {
        return List.copyOf(this.dataSerializers.keySet());
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

    private void registerArmorHelmet(ItemType type, int materialDurability, CanRepairWithHandler repairWith, Sound equipSound) throws RegistryException {
        this.registerDamageableVanilla(type, 11 * materialDurability, repairWith)
                .set(ItemComponents.GET_EQUIPMENT_SLOT, item -> EquipmentSlot.HEAD)
                .set(ItemComponents.CAN_ENCHANT_WITH, enchantableWith(
                        EnchantmentTarget.ARMOR,
                        EnchantmentTarget.ARMOR_HEAD,
                        EnchantmentTarget.BREAKABLE,
                        EnchantmentTarget.VANISHABLE,
                        EnchantmentTarget.WEARABLE))
                .set(ItemComponents.USE, ArmorItemHandlers.equip(EquipmentSlot.HEAD, equipSound));
    }

    private void registerArmorChestplate(ItemType type, int materialDurability, CanRepairWithHandler repairWith, Sound equipSound) throws RegistryException {
        this.registerDamageableVanilla(type, 16 * materialDurability, repairWith)
                .set(ItemComponents.GET_EQUIPMENT_SLOT, item -> EquipmentSlot.CHEST)
                .set(ItemComponents.CAN_ENCHANT_WITH, enchantableWith(
                        EnchantmentTarget.ARMOR,
                        EnchantmentTarget.ARMOR_CHEST,
                        EnchantmentTarget.BREAKABLE,
                        EnchantmentTarget.VANISHABLE,
                        EnchantmentTarget.WEARABLE))
                .set(ItemComponents.USE, ArmorItemHandlers.equip(EquipmentSlot.CHEST, equipSound));
    }

    private void registerArmorLeggings(ItemType type, int materialDurability, CanRepairWithHandler repairWith, Sound equipSound) throws RegistryException {
        this.registerDamageableVanilla(type, 15 * materialDurability, repairWith)
                .set(ItemComponents.GET_EQUIPMENT_SLOT, item -> EquipmentSlot.LEGS)
                .set(ItemComponents.CAN_ENCHANT_WITH, enchantableWith(
                        EnchantmentTarget.ARMOR,
                        EnchantmentTarget.ARMOR_LEGS,
                        EnchantmentTarget.BREAKABLE,
                        EnchantmentTarget.VANISHABLE,
                        EnchantmentTarget.WEARABLE))
                .set(ItemComponents.USE, ArmorItemHandlers.equip(EquipmentSlot.LEGS, equipSound));
    }

    private void registerArmorBoots(ItemType type, int materialDurability, CanRepairWithHandler repairWith, Sound equipSound) throws RegistryException {
        this.registerDamageableVanilla(type, 13 * materialDurability, repairWith)
                .set(ItemComponents.GET_EQUIPMENT_SLOT, item -> EquipmentSlot.FEET)
                .set(ItemComponents.CAN_ENCHANT_WITH, enchantableWith(
                        EnchantmentTarget.ARMOR,
                        EnchantmentTarget.ARMOR_FEET,
                        EnchantmentTarget.BREAKABLE,
                        EnchantmentTarget.VANISHABLE,
                        EnchantmentTarget.WEARABLE))
                .set(ItemComponents.USE, ArmorItemHandlers.equip(EquipmentSlot.FEET, equipSound));
    }

    private void registerElytra(ItemType type, CanRepairWithHandler repairWith) throws RegistryException {
        this.registerDamageableVanilla(type, 432, repairWith)
                .set(ItemComponents.GET_EQUIPMENT_SLOT, item -> EquipmentSlot.CHEST)
                .set(ItemComponents.CAN_ENCHANT_WITH, enchantableWith(
                        EnchantmentTarget.BREAKABLE,
                        EnchantmentTarget.VANISHABLE,
                        EnchantmentTarget.WEARABLE))
                .set(ItemComponents.USE, ArmorItemHandlers.equip(EquipmentSlot.CHEST, Sound.ARMOR_EQUIP_ELYTRA));
    }

    private void registerTool(ItemType type, Tool tool, int maxDamage, CanRepairWithHandler repairWith, EnchantmentTarget... additionalEnchantmentTargets) throws RegistryException {
        this.registerDamageableVanilla(type, maxDamage, repairWith)
                .set(ItemComponents.CAN_ENCHANT_WITH, toolEnchantableWith(additionalEnchantmentTargets))
                .set(ItemComponents.GET_TOOL, item -> tool)
                .set(ItemComponents.MINE_BLOCK, DefaultItemHandlers.MINE_BLOCK);
    }

    private void registerAxe(ItemType type, ToolMaterial material, CanRepairWithHandler repairWith) throws RegistryException {
        registerTool(type, VanillaTools.axe(material), material.getDurability(), repairWith,
                EnchantmentTarget.SHARP_WEAPON,
                EnchantmentTarget.WEAPON);
    }

    private void registerPickaxe(ItemType type, ToolMaterial material, CanRepairWithHandler repairWith) throws RegistryException {
        registerTool(type, VanillaTools.pickaxe(material), material.getDurability(), repairWith);
    }

    private void registerShovel(ItemType type, ToolMaterial material, CanRepairWithHandler repairWith) throws RegistryException {
        registerTool(type, VanillaTools.shovel(material.getSpeed()), material.getDurability(), repairWith);
    }

    private void registerHoe(ItemType type, ToolMaterial material, CanRepairWithHandler repairWith) throws RegistryException {
        registerTool(type, VanillaTools.hoe(material), material.getDurability(), repairWith);
    }

    private void registerSword(ItemType type, ToolMaterial material, CanRepairWithHandler repairWith) throws RegistryException {
        this.registerDamageableVanilla(type, material.getDurability(), repairWith)
                .set(ItemComponents.CAN_ENCHANT_WITH, enchantableWith(
                        EnchantmentTarget.FIRE_ASPECT,
                        EnchantmentTarget.MELEE_WEAPON,
                        EnchantmentTarget.SHARP_WEAPON,
                        EnchantmentTarget.WEAPON,
                        EnchantmentTarget.BREAKABLE,
                        EnchantmentTarget.VANISHABLE))
                .set(ItemComponents.GET_TOOL, item -> VanillaTools.sword())
                .set(ItemComponents.MINE_BLOCK, DefaultItemHandlers.MINE_BLOCK);
    }

    private void registerSpear(ItemType type, ToolMaterial material, CanRepairWithHandler repairWith) throws RegistryException {
        this.registerDamageableVanilla(type, material.getDurability(), repairWith)
                .set(ItemComponents.CAN_ENCHANT_WITH, enchantableWith(
                        EnchantmentTarget.FIRE_ASPECT,
                        EnchantmentTarget.MELEE_WEAPON,
                        EnchantmentTarget.SHARP_WEAPON,
                        EnchantmentTarget.SPEAR,
                        EnchantmentTarget.WEAPON,
                        EnchantmentTarget.BREAKABLE,
                        EnchantmentTarget.VANISHABLE));
    }

    private CloudComponentMap registerDamageableEnchantable(ItemType type, int maxDamage, CanRepairWithHandler repairWith,
                                                            EnchantmentTarget... enchantmentTargets) throws RegistryException {
        CloudComponentMap components = this.registerDamageableVanilla(type, maxDamage, repairWith);
        components.set(ItemComponents.CAN_ENCHANT_WITH, enchantableWith(enchantmentTargets));
        return components;
    }

    private void registerDamageableEnchantableTool(ItemType type, Tool tool, int maxDamage,
                                                   CanRepairWithHandler repairWith,
                                                   EnchantmentTarget... enchantmentTargets) throws RegistryException {
        CloudComponentMap components = this.registerDamageableEnchantable(type, maxDamage, repairWith, enchantmentTargets);
        components.set(ItemComponents.GET_TOOL, item -> tool);
        components.set(ItemComponents.MINE_BLOCK, DefaultItemHandlers.MINE_BLOCK);
    }

    private CloudComponentMap registerDamageableVanilla(ItemType type, int maxDamage, CanRepairWithHandler repairWith) throws RegistryException {
        CloudComponentMap components = this.registerVanilla(type);
        components.set(ItemComponents.DAMAGEABLE, () -> true);
        components.set(ItemComponents.GET_DAMAGE_CHANCE, DefaultItemHandlers.GET_DAMAGE_CHANCE);
        components.set(ItemComponents.GET_MAX_DAMAGE, item -> maxDamage);
        components.set(ItemComponents.GET_MAX_STACK_SIZE, item -> 1);
        components.set(ItemComponents.ON_DAMAGE, DefaultItemHandlers.ON_DAMAGE);
        components.set(ItemComponents.CAN_REPAIR_WITH, repairWith);
        return components;
    }

    private static CanEnchantWithHandler enchantableWith(EnchantmentTarget... targets) {
        Preconditions.checkNotNull(targets, "targets");
        for (EnchantmentTarget target : targets) {
            Preconditions.checkNotNull(target, "target");
        }

        Set<EnchantmentTarget> supportedTargets = targets.length == 0 ? Set.of() : EnumSet.copyOf(List.of(targets));
        return (item, enchantment) -> enchantment != null && supportedTargets.contains(enchantment.target());
    }

    private static CanEnchantWithHandler toolEnchantableWith(EnchantmentTarget... additionalTargets) {
        Preconditions.checkNotNull(additionalTargets, "additionalTargets");
        for (EnchantmentTarget target : additionalTargets) {
            Preconditions.checkNotNull(target, "additionalTarget");
        }

        EnumSet<EnchantmentTarget> targets = EnumSet.of(
                EnchantmentTarget.TOOL,
                EnchantmentTarget.BREAKABLE,
                EnchantmentTarget.VANISHABLE);
        targets.addAll(List.of(additionalTargets));
        return enchantableWith(targets.toArray(EnchantmentTarget[]::new));
    }

    private static CanRepairWithHandler repairWith(Identifier... materials) {
        Preconditions.checkNotNull(materials, "materials");
        for (Identifier repairMaterial : materials) {
            Preconditions.checkNotNull(repairMaterial, "repairMaterial");
        }

        return (item, material) -> {
            if (item.isEmpty() || material.isEmpty() || material.getType() == null) {
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

    private static CanRepairWithHandler repairWith(ItemTagKey tag) {
        Preconditions.checkNotNull(tag, "tag");
        return (item, material) -> !item.isEmpty()
                && !material.isEmpty()
                && material.getType() != null
                && VanillaItemTags.isTagged(material.getType(), tag);
    }

    private synchronized CloudComponentMap registerVanilla(ItemType type, ItemSerializer serializer) throws RegistryException {
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
        }
        BlockRegistrationAccess.linkItem(type, itemType);
        this.typeMap.put(type.getId(), itemType);

        if (getComponentMap(itemType) != null) {
            if (itemType != ItemTypes.UNKNOWN) {
                VanillaRegistryDiagnostics.duplicateBlockItem(itemType.getId());
            }
            return;
        }

        CloudComponentMap collection = new CloudComponentMap(this);
//        collection.apply(DefaultBlockBehaviours.BLOCK_BEHAVIOR_BASE);

        BlockState defaultState = type.getDefaultState();
        collection.set(ItemComponents.GET_BLOCK, (item) -> {
            BlockState state = item.get(ItemKeys.BLOCK_STATE);
            return Optional.of(state != null ? state : defaultState);
        });

        putComponents(itemType, collection);
    }

    public ItemSerializer getSerializer(ItemType type) {
        return serializers.getOrDefault(type, DefaultItemSerializer.INSTANCE);
    }

    public ItemDataSerializer<?> getDataSerializer(DataKey<?, ?> dataKey) {
        return dataSerializers.get(dataKey);
    }

    public ItemType getType(Identifier id) {
        return this.typeMap.get(id);
    }

    public ItemType getType(int legacyId) {
        return getType(getIdentifier(legacyId));
    }

    @Override
    public boolean isTagged(ItemType type, ItemTagKey key) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(key, "key");
        return VanillaItemTags.isTagged(type, key);
    }

    @Override
    public boolean isTagged(ItemStack item, ItemTagKey key) {
        Objects.requireNonNull(item, "item");
        Objects.requireNonNull(key, "key");
        return !item.isEmpty()
                && item.getType() != null
                && VanillaItemTags.isTagged(item.getType(), key);
    }

    @Override
    public ItemTag getTag(ItemTagKey key) {
        return VanillaItemTags.resolve(key);
    }

    @Override
    public Collection<ItemTag> getTags() {
        return VanillaItemTags.all();
    }

    @Deprecated
    public ItemStack getItemLegacy(int legacyId) {
        return ItemStack.from(getType(legacyId), 1);
    }

    @Deprecated
    public ItemStack getItemLegacy(int legacyId, short damage) {
        return getItemLegacy(legacyId, damage, 1);
    }

    @Deprecated
    public ItemStack getItemLegacy(int legacyId, short damage, int amount) {
        return ItemUtils.deserializeItem(getIdentifier(legacyId), damage, amount, NbtMap.EMPTY);
    }

    public Collection<Identifier> getIdentifiers(ItemType type) {
        return this.typeMap.entrySet().stream().filter((e) -> e.getValue() == type).map(Entry::getKey).collect(Collectors.toSet());
    }

    public Identifier fromLegacy(int legacyId, int meta) throws RegistryException {
        return itemPalette.fromLegacy(legacyId, meta);
    }

    public Identifier fromLegacy(int legacyId) throws RegistryException {
        return itemPalette.fromLegacy(legacyId, 0);
    }

    @Override
    public Identifier getIdentifier(int runtimeId) throws RegistryException {
        Identifier identifier = itemPalette.getIdByRuntime(runtimeId);

        if (identifier == null) {
            throw new RegistryException("Runtime ID " + runtimeId + " does not exist");
        }
        return identifier;
    }

    @Override
    public ItemType getType(Identifier runtimeId, int data) {
        var blockType = BlockPalette.INSTANCE.getType(runtimeId);

        if (blockType != null) {
            return blockType.asItem().orElse(ItemTypes.UNKNOWN);
        }

        return typeMap.getOrDefault(runtimeId, ItemTypes.UNKNOWN);
    }

    @Override
    public ItemType getType(int runtimeId, int data) {
        return null;
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

    @Override
    public ImmutableList<Identifier> getItems() {
        return ImmutableList.copyOf(itemPalette.getItemDefinitions().stream()
                .map(itemDefinition -> Identifier.parse(itemDefinition.getIdentifier()))
                .collect(Collectors.toList()));
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

    private void registerVanillaItems() throws RegistryException {
        registerVanilla(ItemTypes.ACACIA_BOAT);
        registerVanilla(ItemTypes.ACACIA_CHEST_BOAT);
        registerVanilla(ItemTypes.ACACIA_SIGN);
        registerVanilla(ItemTypes.AGENT_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.AGENT));
        registerVanilla(ItemTypes.ALLAY_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.ALLAY));
        registerVanilla(ItemTypes.AMETHYST_SHARD);
        registerVanilla(ItemTypes.ANGLER_POTTERY_SHERD);
        registerVanilla(ItemTypes.APPLE);
        registerVanilla(ItemTypes.ARCHER_POTTERY_SHERD);
        registerVanilla(ItemTypes.ARMADILLO_SCUTE);
        registerVanilla(ItemTypes.ARMADILLO_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.ARMADILLO));
        registerVanilla(ItemTypes.ARMOR_STAND);
        registerVanilla(ItemTypes.ARMS_UP_POTTERY_SHERD);
        registerVanilla(ItemTypes.ARROW);
        registerVanilla(ItemTypes.AXOLOTL_BUCKET)
                .set(ItemComponents.USE_ON, BucketItemHandlers.placeEntity(BlockStates.WATER, EntityTypes.AXOLOTL));
        registerVanilla(ItemTypes.AXOLOTL_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.AXOLOTL));
        registerVanilla(ItemTypes.BAKED_POTATO);
        registerVanilla(ItemTypes.BALLOON);
        registerVanilla(ItemTypes.BAMBOO_CHEST_RAFT);
        registerVanilla(ItemTypes.BAMBOO_RAFT);
        registerVanilla(ItemTypes.BAMBOO_SIGN);
        registerVanilla(ItemTypes.BANNER, new BannerSerializer());
        registerVanilla(ItemTypes.BAT_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.BAT));
        registerVanilla(ItemTypes.BEE_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.BEE));
        registerVanilla(ItemTypes.BEEF);
        registerVanilla(ItemTypes.BEETROOT_SEEDS);
        registerVanilla(ItemTypes.BEETROOT_SOUP);
        registerVanilla(ItemTypes.BIRCH_BOAT);
        registerVanilla(ItemTypes.BIRCH_CHEST_BOAT);
        registerVanilla(ItemTypes.BIRCH_SIGN);
        registerVanilla(ItemTypes.BLACK_BUNDLE);
        registerVanilla(ItemTypes.BLACK_CUSHION);
        registerVanilla(ItemTypes.BLACK_DYE);
        registerVanilla(ItemTypes.BLACK_HARNESS);
        registerVanilla(ItemTypes.BLADE_POTTERY_SHERD);
        registerVanilla(ItemTypes.BLAZE_POWDER);
        registerVanilla(ItemTypes.BLAZE_ROD);
        registerVanilla(ItemTypes.BLAZE_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.BLAZE));
        registerVanilla(ItemTypes.BLEACH);
        registerVanilla(ItemTypes.BLUE_BUNDLE);
        registerVanilla(ItemTypes.BLUE_CUSHION);
        registerVanilla(ItemTypes.BLUE_DYE);
        registerVanilla(ItemTypes.BLUE_EGG);
        registerVanilla(ItemTypes.BLUE_HARNESS);
        registerVanilla(ItemTypes.BOARD);
        registerVanilla(ItemTypes.BOGGED_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.BOGGED));
        registerVanilla(ItemTypes.BOLT_ARMOR_TRIM_SMITHING_TEMPLATE);
        registerVanilla(ItemTypes.BONE);
        registerVanilla(ItemTypes.BONE_MEAL);
        registerVanilla(ItemTypes.BOOK);
        registerVanilla(ItemTypes.BORDURE_INDENTED_BANNER_PATTERN);
        registerDamageableEnchantable(ItemTypes.BOW, 384, repairWith(),
                EnchantmentTarget.BOW,
                EnchantmentTarget.BREAKABLE,
                EnchantmentTarget.VANISHABLE);
        registerVanilla(ItemTypes.BOWL);
        registerVanilla(ItemTypes.BREAD);
        registerVanilla(ItemTypes.BREEZE_ROD);
        registerVanilla(ItemTypes.BREEZE_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.BREEZE));
        registerVanilla(ItemTypes.BREWER_POTTERY_SHERD);
        registerVanilla(ItemTypes.BRICK);
        registerVanilla(ItemTypes.BROWN_BUNDLE);
        registerVanilla(ItemTypes.BROWN_CUSHION);
        registerVanilla(ItemTypes.BROWN_DYE);
        registerVanilla(ItemTypes.BROWN_EGG);
        registerVanilla(ItemTypes.BROWN_HARNESS);
        registerDamageableEnchantable(ItemTypes.BRUSH, 64, repairWith(),
                EnchantmentTarget.BREAKABLE,
                EnchantmentTarget.VANISHABLE);
        registerVanilla(ItemTypes.BUCKET)
                .set(ItemComponents.USE_ON, BucketItemHandlers.PICK_UP);
        registerVanilla(ItemTypes.BUNDLE);
        registerVanilla(ItemTypes.BURN_POTTERY_SHERD);
        registerVanilla(ItemTypes.CAMEL_HUSK_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.CAMEL_HUSK));
        registerVanilla(ItemTypes.CAMEL_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.CAMEL));
        registerVanilla(ItemTypes.CARROT);
        registerDamageableEnchantable(ItemTypes.CARROT_ON_A_STICK, 25, repairWith(),
                EnchantmentTarget.BREAKABLE,
                EnchantmentTarget.VANISHABLE);
        registerVanilla(ItemTypes.CAT_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.CAT));
        registerVanilla(ItemTypes.CAVE_SPIDER_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.CAVE_SPIDER));
        registerArmorBoots(ItemTypes.CHAINMAIL_BOOTS, 15, repairWith(ItemTags.REPAIRS_CHAIN_ARMOR), Sound.ARMOR_EQUIP_CHAIN);
        registerArmorChestplate(ItemTypes.CHAINMAIL_CHESTPLATE, 15, repairWith(ItemTags.REPAIRS_CHAIN_ARMOR), Sound.ARMOR_EQUIP_CHAIN);
        registerArmorHelmet(ItemTypes.CHAINMAIL_HELMET, 15, repairWith(ItemTags.REPAIRS_CHAIN_ARMOR), Sound.ARMOR_EQUIP_CHAIN);
        registerArmorLeggings(ItemTypes.CHAINMAIL_LEGGINGS, 15, repairWith(ItemTags.REPAIRS_CHAIN_ARMOR), Sound.ARMOR_EQUIP_CHAIN);
        registerVanilla(ItemTypes.CHARCOAL);
        registerVanilla(ItemTypes.CHERRY_BOAT);
        registerVanilla(ItemTypes.CHERRY_CHEST_BOAT);
        registerVanilla(ItemTypes.CHERRY_SIGN);
        registerVanilla(ItemTypes.CHEST_MINECART)
                .set(ItemComponents.USE_ON, MinecartItemHandlers.useOn(EntityTypes.CHEST_MINECART));
        registerVanilla(ItemTypes.CHICKEN);
        registerVanilla(ItemTypes.CHICKEN_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.CHICKEN));
        registerVanilla(ItemTypes.CHORUS_FRUIT);
        registerVanilla(ItemTypes.CLAY_BALL);
        registerVanilla(ItemTypes.CLOCK);
        registerVanilla(ItemTypes.COAL);
        registerVanilla(ItemTypes.COAST_ARMOR_TRIM_SMITHING_TEMPLATE);
        registerVanilla(ItemTypes.COCOA_BEANS);
        registerVanilla(ItemTypes.COD);
        registerVanilla(ItemTypes.COD_BUCKET)
                .set(ItemComponents.USE_ON, BucketItemHandlers.placeEntity(BlockStates.WATER, EntityTypes.COD));
        registerVanilla(ItemTypes.COD_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.COD));
        registerVanilla(ItemTypes.COMMAND_BLOCK_MINECART)
                .set(ItemComponents.USE_ON, MinecartItemHandlers.useOn(EntityTypes.COMMAND_BLOCK_MINECART));
        registerVanilla(ItemTypes.COMPARATOR);
        registerVanilla(ItemTypes.COMPASS);
        registerVanilla(ItemTypes.COMPOUND);
        registerVanilla(ItemTypes.COOKED_BEEF);
        registerVanilla(ItemTypes.COOKED_CHICKEN);
        registerVanilla(ItemTypes.COOKED_COD);
        registerVanilla(ItemTypes.COOKED_MUTTON);
        registerVanilla(ItemTypes.COOKED_PORKCHOP);
        registerVanilla(ItemTypes.COOKED_RABBIT);
        registerVanilla(ItemTypes.COOKED_SALMON);
        registerVanilla(ItemTypes.COOKIE);
        registerAxe(ItemTypes.COPPER_AXE, ToolMaterials.COPPER, repairWith(ItemTags.COPPER_TOOL_MATERIALS));
        registerArmorBoots(ItemTypes.COPPER_BOOTS, 11, repairWith(ItemTags.REPAIRS_COPPER_ARMOR), Sound.ARMOR_EQUIP_COPPER);
        registerArmorChestplate(ItemTypes.COPPER_CHESTPLATE, 11, repairWith(ItemTags.REPAIRS_COPPER_ARMOR), Sound.ARMOR_EQUIP_COPPER);
        registerVanilla(ItemTypes.COPPER_GOLEM_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.COPPER_GOLEM));
        registerArmorHelmet(ItemTypes.COPPER_HELMET, 11, repairWith(ItemTags.REPAIRS_COPPER_ARMOR), Sound.ARMOR_EQUIP_COPPER);
        registerHoe(ItemTypes.COPPER_HOE, ToolMaterials.COPPER, repairWith(ItemTags.COPPER_TOOL_MATERIALS));
        registerVanilla(ItemTypes.COPPER_HORSE_ARMOR);
        registerVanilla(ItemTypes.COPPER_INGOT);
        registerArmorLeggings(ItemTypes.COPPER_LEGGINGS, 11, repairWith(ItemTags.REPAIRS_COPPER_ARMOR), Sound.ARMOR_EQUIP_COPPER);
        registerVanilla(ItemTypes.COPPER_NAUTILUS_ARMOR);
        registerVanilla(ItemTypes.COPPER_NUGGET);
        registerPickaxe(ItemTypes.COPPER_PICKAXE, ToolMaterials.COPPER, repairWith(ItemTags.COPPER_TOOL_MATERIALS));
        registerShovel(ItemTypes.COPPER_SHOVEL, ToolMaterials.COPPER, repairWith(ItemTags.COPPER_TOOL_MATERIALS));
        registerSpear(ItemTypes.COPPER_SPEAR, ToolMaterials.COPPER, repairWith(ItemTags.COPPER_TOOL_MATERIALS));
        registerSword(ItemTypes.COPPER_SWORD, ToolMaterials.COPPER, repairWith(ItemTags.COPPER_TOOL_MATERIALS));
        registerVanilla(ItemTypes.COW_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.COW));
        registerVanilla(ItemTypes.CREAKING_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.CREAKING));
        registerVanilla(ItemTypes.CREEPER_BANNER_PATTERN);
        registerVanilla(ItemTypes.CREEPER_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.CREEPER));
        registerVanilla(ItemTypes.CRIMSON_SIGN);
        registerDamageableEnchantable(ItemTypes.CROSSBOW, 465, repairWith(),
                EnchantmentTarget.CROSSBOW,
                EnchantmentTarget.BREAKABLE,
                EnchantmentTarget.VANISHABLE);
        registerVanilla(ItemTypes.CYAN_BUNDLE);
        registerVanilla(ItemTypes.CYAN_CUSHION);
        registerVanilla(ItemTypes.CYAN_DYE);
        registerVanilla(ItemTypes.CYAN_HARNESS);
        registerVanilla(ItemTypes.DANGER_POTTERY_SHERD);
        registerVanilla(ItemTypes.DARK_OAK_BOAT);
        registerVanilla(ItemTypes.DARK_OAK_CHEST_BOAT);
        registerVanilla(ItemTypes.DARK_OAK_SIGN);
        registerVanilla(ItemTypes.DIAMOND);
        registerAxe(ItemTypes.DIAMOND_AXE, ToolMaterials.DIAMOND, repairWith(ItemTags.DIAMOND_TOOL_MATERIALS));
        registerArmorBoots(ItemTypes.DIAMOND_BOOTS, 33, repairWith(ItemTags.REPAIRS_DIAMOND_ARMOR), Sound.ARMOR_EQUIP_DIAMOND);
        registerArmorChestplate(ItemTypes.DIAMOND_CHESTPLATE, 33, repairWith(ItemTags.REPAIRS_DIAMOND_ARMOR), Sound.ARMOR_EQUIP_DIAMOND);
        registerArmorHelmet(ItemTypes.DIAMOND_HELMET, 33, repairWith(ItemTags.REPAIRS_DIAMOND_ARMOR), Sound.ARMOR_EQUIP_DIAMOND);
        registerHoe(ItemTypes.DIAMOND_HOE, ToolMaterials.DIAMOND, repairWith(ItemTags.DIAMOND_TOOL_MATERIALS));
        registerVanilla(ItemTypes.DIAMOND_HORSE_ARMOR);
        registerArmorLeggings(ItemTypes.DIAMOND_LEGGINGS, 33, repairWith(ItemTags.REPAIRS_DIAMOND_ARMOR), Sound.ARMOR_EQUIP_DIAMOND);
        registerVanilla(ItemTypes.DIAMOND_NAUTILUS_ARMOR);
        registerPickaxe(ItemTypes.DIAMOND_PICKAXE, ToolMaterials.DIAMOND, repairWith(ItemTags.DIAMOND_TOOL_MATERIALS));
        registerShovel(ItemTypes.DIAMOND_SHOVEL, ToolMaterials.DIAMOND, repairWith(ItemTags.DIAMOND_TOOL_MATERIALS));
        registerSpear(ItemTypes.DIAMOND_SPEAR, ToolMaterials.DIAMOND, repairWith(ItemTags.DIAMOND_TOOL_MATERIALS));
        registerSword(ItemTypes.DIAMOND_SWORD, ToolMaterials.DIAMOND, repairWith(ItemTags.DIAMOND_TOOL_MATERIALS));
        registerVanilla(ItemTypes.DISC_FRAGMENT_5);
        registerVanilla(ItemTypes.DOLPHIN_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.DOLPHIN));
        registerVanilla(ItemTypes.DONKEY_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.DONKEY));
        registerVanilla(ItemTypes.DRAGON_BREATH);
        registerVanilla(ItemTypes.DRIED_KELP);
        registerVanilla(ItemTypes.DROWNED_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.DROWNED));
        registerVanilla(ItemTypes.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE);
        registerVanilla(ItemTypes.ECHO_SHARD);
        registerVanilla(ItemTypes.EGG);
        registerVanilla(ItemTypes.ELDER_GUARDIAN_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.ELDER_GUARDIAN));
        registerElytra(ItemTypes.ELYTRA, repairWith(ItemTypes.PHANTOM_MEMBRANE.getId()));
        registerVanilla(ItemTypes.EMERALD);
        registerVanilla(ItemTypes.EMPTY_MAP);
        registerVanilla(ItemTypes.ENCHANTED_BOOK)
                .set(ItemComponents.CAN_ENCHANT_WITH, (item, enchantment) -> true);
        registerVanilla(ItemTypes.ENCHANTED_GOLDEN_APPLE);
        registerVanilla(ItemTypes.END_CRYSTAL);
        registerVanilla(ItemTypes.ENDER_DRAGON_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.ENDER_DRAGON));
        registerVanilla(ItemTypes.ENDER_EYE);
        registerVanilla(ItemTypes.ENDER_PEARL);
        registerVanilla(ItemTypes.ENDERMAN_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.ENDERMAN));
        registerVanilla(ItemTypes.ENDERMITE_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.ENDERMITE));
        registerVanilla(ItemTypes.EVOKER_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.EVOCATION_ILLAGER));
        registerVanilla(ItemTypes.EXPERIENCE_BOTTLE);
        registerVanilla(ItemTypes.EXPLORER_POTTERY_SHERD);
        registerVanilla(ItemTypes.EYE_ARMOR_TRIM_SMITHING_TEMPLATE);
        registerVanilla(ItemTypes.FEATHER);
        registerVanilla(ItemTypes.FERMENTED_SPIDER_EYE);
        registerVanilla(ItemTypes.FIELD_MASONED_BANNER_PATTERN);
        registerVanilla(ItemTypes.FILLED_MAP);
        registerVanilla(ItemTypes.FIRE_CHARGE)
                .set(ItemComponents.USE_ON, FireChargeItemHandlers.USE_ON);
        registerVanilla(ItemTypes.FIREWORK_ROCKET, new FireworkRocketSerializer())
                .set(ItemComponents.USE, FireworkRocketItemHandlers.USE)
                .set(ItemComponents.USE_ON, FireworkRocketItemHandlers.USE_ON);
        registerVanilla(ItemTypes.FIREWORK_STAR, new FireworkStarSerializer());
        registerDamageableEnchantable(ItemTypes.FISHING_ROD, 64, repairWith(),
                EnchantmentTarget.FISHING_ROD,
                EnchantmentTarget.BREAKABLE,
                EnchantmentTarget.VANISHABLE)
                .set(ItemComponents.USE, FishingRodItemHandlers.USE);
        registerVanilla(ItemTypes.FLINT);
        registerVanilla(ItemTypes.FLINT_AND_STEEL)
                .set(ItemComponents.USE_ON, FlintAndSteelItemHandlers.USE_ON);
        registerVanilla(ItemTypes.FLOW_ARMOR_TRIM_SMITHING_TEMPLATE);
        registerVanilla(ItemTypes.FLOW_BANNER_PATTERN);
        registerVanilla(ItemTypes.FLOW_POTTERY_SHERD);
        registerVanilla(ItemTypes.FLOWER_BANNER_PATTERN);
        registerVanilla(ItemTypes.FOX_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.FOX));
        registerVanilla(ItemTypes.FRIEND_POTTERY_SHERD);
        registerVanilla(ItemTypes.FROG_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.FROG));
        registerVanilla(ItemTypes.GHAST_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.GHAST));
        registerVanilla(ItemTypes.GHAST_TEAR);
        registerVanilla(ItemTypes.GLASS_BOTTLE);
        registerVanilla(ItemTypes.GLISTERING_MELON_SLICE);
        registerVanilla(ItemTypes.GLOBE_BANNER_PATTERN);
        registerVanilla(ItemTypes.GLOW_BERRIES);
        registerVanilla(ItemTypes.GLOW_INK_SAC);
        registerVanilla(ItemTypes.GLOW_SQUID_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.GLOW_SQUID));
        registerVanilla(ItemTypes.GLOW_STICK);
        registerVanilla(ItemTypes.GLOWSTONE_DUST);
        registerVanilla(ItemTypes.GOAT_HORN);
        registerVanilla(ItemTypes.GOAT_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.GOAT));
        registerVanilla(ItemTypes.GOLD_INGOT);
        registerVanilla(ItemTypes.GOLD_NUGGET);
        registerVanilla(ItemTypes.GOLDEN_APPLE);
        registerAxe(ItemTypes.GOLDEN_AXE, ToolMaterials.GOLD, repairWith(ItemTags.GOLD_TOOL_MATERIALS));
        registerArmorBoots(ItemTypes.GOLDEN_BOOTS, 7, repairWith(ItemTags.REPAIRS_GOLD_ARMOR), Sound.ARMOR_EQUIP_GOLD);
        registerVanilla(ItemTypes.GOLDEN_CARROT);
        registerArmorChestplate(ItemTypes.GOLDEN_CHESTPLATE, 7, repairWith(ItemTags.REPAIRS_GOLD_ARMOR), Sound.ARMOR_EQUIP_GOLD);
        registerArmorHelmet(ItemTypes.GOLDEN_HELMET, 7, repairWith(ItemTags.REPAIRS_GOLD_ARMOR), Sound.ARMOR_EQUIP_GOLD);
        registerHoe(ItemTypes.GOLDEN_HOE, ToolMaterials.GOLD, repairWith(ItemTags.GOLD_TOOL_MATERIALS));
        registerVanilla(ItemTypes.GOLDEN_HORSE_ARMOR);
        registerArmorLeggings(ItemTypes.GOLDEN_LEGGINGS, 7, repairWith(ItemTags.REPAIRS_GOLD_ARMOR), Sound.ARMOR_EQUIP_GOLD);
        registerVanilla(ItemTypes.GOLDEN_NAUTILUS_ARMOR);
        registerPickaxe(ItemTypes.GOLDEN_PICKAXE, ToolMaterials.GOLD, repairWith(ItemTags.GOLD_TOOL_MATERIALS));
        registerShovel(ItemTypes.GOLDEN_SHOVEL, ToolMaterials.GOLD, repairWith(ItemTags.GOLD_TOOL_MATERIALS));
        registerSpear(ItemTypes.GOLDEN_SPEAR, ToolMaterials.GOLD, repairWith(ItemTags.GOLD_TOOL_MATERIALS));
        registerSword(ItemTypes.GOLDEN_SWORD, ToolMaterials.GOLD, repairWith(ItemTags.GOLD_TOOL_MATERIALS));
        registerVanilla(ItemTypes.GRAY_BUNDLE);
        registerVanilla(ItemTypes.GRAY_CUSHION);
        registerVanilla(ItemTypes.GRAY_DYE);
        registerVanilla(ItemTypes.GRAY_HARNESS);
        registerVanilla(ItemTypes.GREEN_BUNDLE);
        registerVanilla(ItemTypes.GREEN_CUSHION);
        registerVanilla(ItemTypes.GREEN_DYE);
        registerVanilla(ItemTypes.GREEN_HARNESS);
        registerVanilla(ItemTypes.GUARDIAN_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.GUARDIAN));
        registerVanilla(ItemTypes.GUNPOWDER);
        registerVanilla(ItemTypes.GUSTER_BANNER_PATTERN);
        registerVanilla(ItemTypes.GUSTER_POTTERY_SHERD);
        registerVanilla(ItemTypes.HAPPY_GHAST_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.HAPPY_GHAST));
        registerVanilla(ItemTypes.HEART_OF_THE_SEA);
        registerVanilla(ItemTypes.HEART_POTTERY_SHERD);
        registerVanilla(ItemTypes.HEARTBREAK_POTTERY_SHERD);
        registerVanilla(ItemTypes.HOGLIN_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.HOGLIN));
        registerVanilla(ItemTypes.HONEY_BOTTLE);
        registerVanilla(ItemTypes.HONEYCOMB);
        registerVanilla(ItemTypes.HOPPER_MINECART)
                .set(ItemComponents.USE_ON, MinecartItemHandlers.useOn(EntityTypes.HOPPER_MINECART));
        registerVanilla(ItemTypes.HORSE_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.HORSE));
        registerVanilla(ItemTypes.HOST_ARMOR_TRIM_SMITHING_TEMPLATE);
        registerVanilla(ItemTypes.HOWL_POTTERY_SHERD);
        registerVanilla(ItemTypes.HUSK_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.HUSK));
        registerVanilla(ItemTypes.ICE_BOMB);
        registerVanilla(ItemTypes.INK_SAC);
        registerAxe(ItemTypes.IRON_AXE, ToolMaterials.IRON, repairWith(ItemTags.IRON_TOOL_MATERIALS));
        registerArmorBoots(ItemTypes.IRON_BOOTS, 15, repairWith(ItemTags.REPAIRS_IRON_ARMOR), Sound.ARMOR_EQUIP_IRON);
        registerArmorChestplate(ItemTypes.IRON_CHESTPLATE, 15, repairWith(ItemTags.REPAIRS_IRON_ARMOR), Sound.ARMOR_EQUIP_IRON);
        registerVanilla(ItemTypes.IRON_GOLEM_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.IRON_GOLEM));
        registerArmorHelmet(ItemTypes.IRON_HELMET, 15, repairWith(ItemTags.REPAIRS_IRON_ARMOR), Sound.ARMOR_EQUIP_IRON);
        registerHoe(ItemTypes.IRON_HOE, ToolMaterials.IRON, repairWith(ItemTags.IRON_TOOL_MATERIALS));
        registerVanilla(ItemTypes.IRON_HORSE_ARMOR);
        registerVanilla(ItemTypes.IRON_INGOT);
        registerArmorLeggings(ItemTypes.IRON_LEGGINGS, 15, repairWith(ItemTags.REPAIRS_IRON_ARMOR), Sound.ARMOR_EQUIP_IRON);
        registerVanilla(ItemTypes.IRON_NAUTILUS_ARMOR);
        registerVanilla(ItemTypes.IRON_NUGGET);
        registerPickaxe(ItemTypes.IRON_PICKAXE, ToolMaterials.IRON, repairWith(ItemTags.IRON_TOOL_MATERIALS));
        registerShovel(ItemTypes.IRON_SHOVEL, ToolMaterials.IRON, repairWith(ItemTags.IRON_TOOL_MATERIALS));
        registerSpear(ItemTypes.IRON_SPEAR, ToolMaterials.IRON, repairWith(ItemTags.IRON_TOOL_MATERIALS));
        registerSword(ItemTypes.IRON_SWORD, ToolMaterials.IRON, repairWith(ItemTags.IRON_TOOL_MATERIALS));
        registerVanilla(ItemTypes.JUNGLE_BOAT);
        registerVanilla(ItemTypes.JUNGLE_CHEST_BOAT);
        registerVanilla(ItemTypes.JUNGLE_SIGN);
        registerVanilla(ItemTypes.LAPIS_LAZULI);
        registerVanilla(ItemTypes.LAVA_BUCKET)
                .set(ItemComponents.USE_ON, BucketItemHandlers.place(BlockStates.LAVA));
        registerVanilla(ItemTypes.LEAD);
        registerVanilla(ItemTypes.LEATHER);
        registerArmorBoots(ItemTypes.LEATHER_BOOTS, 5, repairWith(ItemTags.REPAIRS_LEATHER_ARMOR), Sound.ARMOR_EQUIP_LEATHER);
        registerArmorChestplate(ItemTypes.LEATHER_CHESTPLATE, 5, repairWith(ItemTags.REPAIRS_LEATHER_ARMOR), Sound.ARMOR_EQUIP_LEATHER);
        registerArmorHelmet(ItemTypes.LEATHER_HELMET, 5, repairWith(ItemTags.REPAIRS_LEATHER_ARMOR), Sound.ARMOR_EQUIP_LEATHER);
        registerVanilla(ItemTypes.LEATHER_HORSE_ARMOR);
        registerArmorLeggings(ItemTypes.LEATHER_LEGGINGS, 5, repairWith(ItemTags.REPAIRS_LEATHER_ARMOR), Sound.ARMOR_EQUIP_LEATHER);
        registerVanilla(ItemTypes.LIGHT_BLUE_BUNDLE);
        registerVanilla(ItemTypes.LIGHT_BLUE_CUSHION);
        registerVanilla(ItemTypes.LIGHT_BLUE_DYE);
        registerVanilla(ItemTypes.LIGHT_BLUE_HARNESS);
        registerVanilla(ItemTypes.LIGHT_GRAY_BUNDLE);
        registerVanilla(ItemTypes.LIGHT_GRAY_CUSHION);
        registerVanilla(ItemTypes.LIGHT_GRAY_DYE);
        registerVanilla(ItemTypes.LIGHT_GRAY_HARNESS);
        registerVanilla(ItemTypes.LIME_BUNDLE);
        registerVanilla(ItemTypes.LIME_CUSHION);
        registerVanilla(ItemTypes.LIME_DYE);
        registerVanilla(ItemTypes.LIME_HARNESS);
        registerVanilla(ItemTypes.LINGERING_POTION);
        registerVanilla(ItemTypes.LLAMA_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.LLAMA));
        registerVanilla(ItemTypes.LODESTONE_COMPASS);
        registerDamageableEnchantableTool(ItemTypes.MACE, VanillaTools.weapon(), 500, repairWith(ItemTypes.BREEZE_ROD.getId()),
                EnchantmentTarget.FIRE_ASPECT,
                EnchantmentTarget.MACE,
                EnchantmentTarget.WEAPON,
                EnchantmentTarget.BREAKABLE,
                EnchantmentTarget.VANISHABLE);
        registerVanilla(ItemTypes.MAGENTA_BUNDLE);
        registerVanilla(ItemTypes.MAGENTA_CUSHION);
        registerVanilla(ItemTypes.MAGENTA_DYE);
        registerVanilla(ItemTypes.MAGENTA_HARNESS);
        registerVanilla(ItemTypes.MAGMA_CREAM);
        registerVanilla(ItemTypes.MAGMA_CUBE_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.MAGMA_CUBE));
        registerVanilla(ItemTypes.MANGROVE_BOAT);
        registerVanilla(ItemTypes.MANGROVE_CHEST_BOAT);
        registerVanilla(ItemTypes.MANGROVE_SIGN);
        registerVanilla(ItemTypes.MEDICINE);
        registerVanilla(ItemTypes.MELON_SEEDS);
        registerVanilla(ItemTypes.MELON_SLICE);
        registerVanilla(ItemTypes.MILK_BUCKET);
        registerVanilla(ItemTypes.MINECART)
                .set(ItemComponents.USE_ON, MinecartItemHandlers.useOn(EntityTypes.MINECART));
        registerVanilla(ItemTypes.MINER_POTTERY_SHERD);
        registerVanilla(ItemTypes.MOJANG_BANNER_PATTERN);
        registerVanilla(ItemTypes.MOOSHROOM_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.MOOSHROOM));
        registerVanilla(ItemTypes.MOURNER_POTTERY_SHERD);
        registerVanilla(ItemTypes.MULE_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.MULE));
        registerVanilla(ItemTypes.MUSHROOM_STEW);
        registerVanilla(ItemTypes.MUSIC_DISC_11);
        registerVanilla(ItemTypes.MUSIC_DISC_13);
        registerVanilla(ItemTypes.MUSIC_DISC_5);
        registerVanilla(ItemTypes.MUSIC_DISC_BLOCKS);
        registerVanilla(ItemTypes.MUSIC_DISC_BOUNCE);
        registerVanilla(ItemTypes.MUSIC_DISC_CAT);
        registerVanilla(ItemTypes.MUSIC_DISC_CHIRP);
        registerVanilla(ItemTypes.MUSIC_DISC_CREATOR);
        registerVanilla(ItemTypes.MUSIC_DISC_CREATOR_MUSIC_BOX);
        registerVanilla(ItemTypes.MUSIC_DISC_FAR);
        registerVanilla(ItemTypes.MUSIC_DISC_LAVA_CHICKEN);
        registerVanilla(ItemTypes.MUSIC_DISC_MALL);
        registerVanilla(ItemTypes.MUSIC_DISC_MELLOHI);
        registerVanilla(ItemTypes.MUSIC_DISC_OTHERSIDE);
        registerVanilla(ItemTypes.MUSIC_DISC_PIGSTEP);
        registerVanilla(ItemTypes.MUSIC_DISC_PRECIPICE);
        registerVanilla(ItemTypes.MUSIC_DISC_RELIC);
        registerVanilla(ItemTypes.MUSIC_DISC_STAL);
        registerVanilla(ItemTypes.MUSIC_DISC_STRAD);
        registerVanilla(ItemTypes.MUSIC_DISC_TEARS);
        registerVanilla(ItemTypes.MUSIC_DISC_WAIT);
        registerVanilla(ItemTypes.MUSIC_DISC_WARD);
        registerVanilla(ItemTypes.MUTTON);
        registerVanilla(ItemTypes.NAME_TAG);
        registerVanilla(ItemTypes.NAUTILUS_SHELL);
        registerVanilla(ItemTypes.NAUTILUS_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.NAUTILUS));
        registerVanilla(ItemTypes.NETHER_STAR);
        registerVanilla(ItemTypes.NETHERBRICK);
        registerAxe(ItemTypes.NETHERITE_AXE, ToolMaterials.NETHERITE, repairWith(ItemTags.NETHERITE_TOOL_MATERIALS));
        registerArmorBoots(ItemTypes.NETHERITE_BOOTS, 37, repairWith(ItemTags.REPAIRS_NETHERITE_ARMOR), Sound.ARMOR_EQUIP_NETHERITE);
        registerArmorChestplate(ItemTypes.NETHERITE_CHESTPLATE, 37, repairWith(ItemTags.REPAIRS_NETHERITE_ARMOR), Sound.ARMOR_EQUIP_NETHERITE);
        registerArmorHelmet(ItemTypes.NETHERITE_HELMET, 37, repairWith(ItemTags.REPAIRS_NETHERITE_ARMOR), Sound.ARMOR_EQUIP_NETHERITE);
        registerHoe(ItemTypes.NETHERITE_HOE, ToolMaterials.NETHERITE, repairWith(ItemTags.NETHERITE_TOOL_MATERIALS));
        registerVanilla(ItemTypes.NETHERITE_HORSE_ARMOR);
        registerVanilla(ItemTypes.NETHERITE_INGOT);
        registerArmorLeggings(ItemTypes.NETHERITE_LEGGINGS, 37, repairWith(ItemTags.REPAIRS_NETHERITE_ARMOR), Sound.ARMOR_EQUIP_NETHERITE);
        registerVanilla(ItemTypes.NETHERITE_NAUTILUS_ARMOR);
        registerPickaxe(ItemTypes.NETHERITE_PICKAXE, ToolMaterials.NETHERITE, repairWith(ItemTags.NETHERITE_TOOL_MATERIALS));
        registerVanilla(ItemTypes.NETHERITE_SCRAP);
        registerShovel(ItemTypes.NETHERITE_SHOVEL, ToolMaterials.NETHERITE, repairWith(ItemTags.NETHERITE_TOOL_MATERIALS));
        registerSpear(ItemTypes.NETHERITE_SPEAR, ToolMaterials.NETHERITE, repairWith(ItemTags.NETHERITE_TOOL_MATERIALS));
        registerSword(ItemTypes.NETHERITE_SWORD, ToolMaterials.NETHERITE, repairWith(ItemTags.NETHERITE_TOOL_MATERIALS));
        registerVanilla(ItemTypes.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
        registerVanilla(ItemTypes.NPC_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.NPC));
        registerVanilla(ItemTypes.OAK_BOAT);
        registerVanilla(ItemTypes.OAK_CHEST_BOAT);
        registerVanilla(ItemTypes.OAK_SIGN);
        registerVanilla(ItemTypes.OCELOT_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.OCELOT));
        registerVanilla(ItemTypes.OMINOUS_BOTTLE);
        registerVanilla(ItemTypes.OMINOUS_TRIAL_KEY);
        registerVanilla(ItemTypes.ORANGE_BUNDLE);
        registerVanilla(ItemTypes.ORANGE_CUSHION);
        registerVanilla(ItemTypes.ORANGE_DYE);
        registerVanilla(ItemTypes.ORANGE_HARNESS);
        registerVanilla(ItemTypes.PAINTING);
        registerVanilla(ItemTypes.PALE_OAK_BOAT);
        registerVanilla(ItemTypes.PALE_OAK_CHEST_BOAT);
        registerVanilla(ItemTypes.PALE_OAK_SIGN);
        registerVanilla(ItemTypes.PANDA_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.PANDA));
        registerVanilla(ItemTypes.PAPER);
        registerVanilla(ItemTypes.PARCHED_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.PARCHED));
        registerVanilla(ItemTypes.PARROT_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.PARROT));
        registerVanilla(ItemTypes.PHANTOM_MEMBRANE);
        registerVanilla(ItemTypes.PHANTOM_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.PHANTOM));
        registerVanilla(ItemTypes.PIG_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.PIG));
        registerVanilla(ItemTypes.PIGLIN_BANNER_PATTERN);
        registerVanilla(ItemTypes.PIGLIN_BRUTE_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.PIGLIN_BRUTE));
        registerVanilla(ItemTypes.PIGLIN_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.PIGLIN));
        registerVanilla(ItemTypes.PILLAGER_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.PILLAGER));
        registerVanilla(ItemTypes.PINK_BUNDLE);
        registerVanilla(ItemTypes.PINK_CUSHION);
        registerVanilla(ItemTypes.PINK_DYE);
        registerVanilla(ItemTypes.PINK_HARNESS);
        registerVanilla(ItemTypes.PITCHER_POD);
        registerVanilla(ItemTypes.PLENTY_POTTERY_SHERD);
        registerVanilla(ItemTypes.POISONOUS_POTATO);
        registerVanilla(ItemTypes.POLAR_BEAR_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.POLAR_BEAR));
        registerVanilla(ItemTypes.POPPED_CHORUS_FRUIT);
        registerVanilla(ItemTypes.POPLAR_BOAT);
        registerVanilla(ItemTypes.POPLAR_CHEST_BOAT);
        registerVanilla(ItemTypes.POPLAR_SIGN);
        registerVanilla(ItemTypes.PORKCHOP);
        registerVanilla(ItemTypes.POTATO);
        registerVanilla(ItemTypes.POTION);
        registerVanilla(ItemTypes.POWDER_SNOW_BUCKET)
                .set(ItemComponents.GET_BLOCK, item -> Optional.of(BlockTypes.POWDER_SNOW.getDefaultState()))
                .set(ItemComponents.USE_ON,
                        BucketItemHandlers.placePowderSnow(BlockTypes.POWDER_SNOW.getDefaultState()));
        registerVanilla(ItemTypes.PRISMARINE_CRYSTALS);
        registerVanilla(ItemTypes.PRISMARINE_SHARD);
        registerVanilla(ItemTypes.PRIZE_POTTERY_SHERD);
        registerVanilla(ItemTypes.PUFFERFISH);
        registerVanilla(ItemTypes.PUFFERFISH_BUCKET)
                .set(ItemComponents.USE_ON, BucketItemHandlers.placeEntity(BlockStates.WATER, EntityTypes.PUFFERFISH));
        registerVanilla(ItemTypes.PUFFERFISH_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.PUFFERFISH));
        registerVanilla(ItemTypes.PUMPKIN_PIE);
        registerVanilla(ItemTypes.PUMPKIN_SEEDS);
        registerVanilla(ItemTypes.PURPLE_BUNDLE);
        registerVanilla(ItemTypes.PURPLE_CUSHION);
        registerVanilla(ItemTypes.PURPLE_DYE);
        registerVanilla(ItemTypes.PURPLE_HARNESS);
        registerVanilla(ItemTypes.QUARTZ);
        registerVanilla(ItemTypes.RABBIT);
        registerVanilla(ItemTypes.RABBIT_FOOT);
        registerVanilla(ItemTypes.RABBIT_HIDE);
        registerVanilla(ItemTypes.RABBIT_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.RABBIT));
        registerVanilla(ItemTypes.RABBIT_STEW);
        registerVanilla(ItemTypes.RAISER_ARMOR_TRIM_SMITHING_TEMPLATE);
        registerVanilla(ItemTypes.RAPID_FERTILIZER);
        registerVanilla(ItemTypes.RAVAGER_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.RAVAGER));
        registerVanilla(ItemTypes.RAW_COPPER);
        registerVanilla(ItemTypes.RAW_GOLD);
        registerVanilla(ItemTypes.RAW_IRON);
        registerVanilla(ItemTypes.RECOVERY_COMPASS);
        registerVanilla(ItemTypes.RED_BUNDLE);
        registerVanilla(ItemTypes.RED_CUSHION);
        registerVanilla(ItemTypes.RED_DYE);
        registerVanilla(ItemTypes.RED_HARNESS);
        registerVanilla(ItemTypes.REDSTONE);
        registerVanilla(ItemTypes.REPEATER);
        registerVanilla(ItemTypes.RESIN_BRICK);
        registerVanilla(ItemTypes.RIB_ARMOR_TRIM_SMITHING_TEMPLATE);
        registerVanilla(ItemTypes.ROTTEN_FLESH);
        registerVanilla(ItemTypes.SADDLE);
        registerVanilla(ItemTypes.SALMON);
        registerVanilla(ItemTypes.SALMON_BUCKET)
                .set(ItemComponents.USE_ON, BucketItemHandlers.placeEntity(BlockStates.WATER, EntityTypes.SALMON));
        registerVanilla(ItemTypes.SALMON_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.SALMON));
        registerVanilla(ItemTypes.SCRAPE_POTTERY_SHERD);
        registerVanilla(ItemTypes.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE);
        registerVanilla(ItemTypes.SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE);
        registerVanilla(ItemTypes.SHEAF_POTTERY_SHERD);
        registerTool(ItemTypes.SHEARS, VanillaTools.shears(), 238, repairWith());
        registerVanilla(ItemTypes.SHEEP_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.SHEEP));
        registerVanilla(ItemTypes.SHELTER_POTTERY_SHERD);
        registerVanilla(ItemTypes.SHIELD);
        registerVanilla(ItemTypes.SHULKER_SHELL);
        registerVanilla(ItemTypes.SHULKER_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.SHULKER));
        registerVanilla(ItemTypes.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE);
        registerVanilla(ItemTypes.SILVERFISH_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.SILVERFISH));
        registerVanilla(ItemTypes.SKELETON_HORSE_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.SKELETON_HORSE));
        registerVanilla(ItemTypes.SKELETON_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.SKELETON));
        registerVanilla(ItemTypes.SKULL_BANNER_PATTERN);
        registerVanilla(ItemTypes.SKULL_POTTERY_SHERD);
        registerVanilla(ItemTypes.SLIME_BALL);
        registerVanilla(ItemTypes.SLIME_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.SLIME));
        registerVanilla(ItemTypes.SNIFFER_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.SNIFFER));
        registerVanilla(ItemTypes.SNORT_POTTERY_SHERD);
        registerVanilla(ItemTypes.SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE);
        registerVanilla(ItemTypes.SNOW_GOLEM_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.SNOW_GOLEM));
        registerVanilla(ItemTypes.SNOWBALL);
        registerVanilla(ItemTypes.SPARKLER);
        registerVanilla(ItemTypes.SPAWN_EGG);
        registerVanilla(ItemTypes.SPIDER_EYE);
        registerVanilla(ItemTypes.SPIDER_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.SPIDER));
        registerVanilla(ItemTypes.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE);
        registerVanilla(ItemTypes.SPLASH_POTION);
        registerVanilla(ItemTypes.SPRUCE_BOAT);
        registerVanilla(ItemTypes.SPRUCE_CHEST_BOAT);
        registerVanilla(ItemTypes.SPRUCE_SIGN);
        registerVanilla(ItemTypes.SPYGLASS);
        registerVanilla(ItemTypes.SQUID_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.SQUID));
        registerVanilla(ItemTypes.STICK);
        registerAxe(ItemTypes.STONE_AXE, ToolMaterials.STONE, repairWith(ItemTags.STONE_TOOL_MATERIALS));
        registerHoe(ItemTypes.STONE_HOE, ToolMaterials.STONE, repairWith(ItemTags.STONE_TOOL_MATERIALS));
        registerPickaxe(ItemTypes.STONE_PICKAXE, ToolMaterials.STONE, repairWith(ItemTags.STONE_TOOL_MATERIALS));
        registerShovel(ItemTypes.STONE_SHOVEL, ToolMaterials.STONE, repairWith(ItemTags.STONE_TOOL_MATERIALS));
        registerSpear(ItemTypes.STONE_SPEAR, ToolMaterials.STONE, repairWith(ItemTags.STONE_TOOL_MATERIALS));
        registerSword(ItemTypes.STONE_SWORD, ToolMaterials.STONE, repairWith(ItemTags.STONE_TOOL_MATERIALS));
        registerVanilla(ItemTypes.STRAY_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.STRAY));
        registerVanilla(ItemTypes.STRIDER_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.STRIDER));
        registerVanilla(ItemTypes.STRING)
                .set(ItemComponents.GET_BLOCK, item -> Optional.of(BlockTypes.TRIP_WIRE.getDefaultState()));
        registerVanilla(ItemTypes.SUGAR);
        registerVanilla(ItemTypes.SUGAR_CANE);
        registerVanilla(ItemTypes.SULFUR_CUBE_BUCKET)
                .set(ItemComponents.USE_ON, BucketItemHandlers.placeEntity(EntityTypes.SULFUR_CUBE));
        registerVanilla(ItemTypes.SULFUR_CUBE_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.SULFUR_CUBE));
        registerVanilla(ItemTypes.SUSPICIOUS_STEW);
        registerVanilla(ItemTypes.SWEET_BERRIES);
        registerVanilla(ItemTypes.TADPOLE_BUCKET)
                .set(ItemComponents.USE_ON, BucketItemHandlers.placeEntity(BlockStates.WATER, EntityTypes.TADPOLE));
        registerVanilla(ItemTypes.TADPOLE_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.TADPOLE));
        registerVanilla(ItemTypes.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE);
        registerVanilla(ItemTypes.TNT_MINECART)
                .set(ItemComponents.USE_ON, MinecartItemHandlers.useOn(EntityTypes.TNT_MINECART));
        registerVanilla(ItemTypes.TORCHFLOWER_SEEDS);
        registerVanilla(ItemTypes.TOTEM_OF_UNDYING);
        registerVanilla(ItemTypes.TRADER_LLAMA_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.TRADER_LLAMA));
        registerVanilla(ItemTypes.TRIAL_KEY);
        registerDamageableEnchantableTool(ItemTypes.TRIDENT, VanillaTools.weapon(), 250, repairWith(),
                EnchantmentTarget.TRIDENT,
                EnchantmentTarget.WEAPON,
                EnchantmentTarget.BREAKABLE,
                EnchantmentTarget.VANISHABLE);
        registerVanilla(ItemTypes.TROPICAL_FISH);
        registerVanilla(ItemTypes.TROPICAL_FISH_BUCKET)
                .set(ItemComponents.USE_ON,
                        BucketItemHandlers.placeEntity(BlockStates.WATER, EntityTypes.TROPICAL_FISH));
        registerVanilla(ItemTypes.TROPICAL_FISH_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.TROPICAL_FISH));
        registerArmorHelmet(ItemTypes.TURTLE_HELMET, 25, repairWith(ItemTags.REPAIRS_TURTLE_HELMET), Sound.ARMOR_EQUIP_GENERIC);
        registerVanilla(ItemTypes.TURTLE_SCUTE);
        registerVanilla(ItemTypes.TURTLE_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.TURTLE));
        registerVanilla(ItemTypes.VEX_ARMOR_TRIM_SMITHING_TEMPLATE);
        registerVanilla(ItemTypes.VEX_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.VEX));
        registerVanilla(ItemTypes.VILLAGER_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.VILLAGER));
        registerVanilla(ItemTypes.VINDICATOR_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.VINDICATOR));
        registerVanilla(ItemTypes.WANDERING_TRADER_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.WANDERING_TRADER));
        registerVanilla(ItemTypes.WARD_ARMOR_TRIM_SMITHING_TEMPLATE);
        registerVanilla(ItemTypes.WARDEN_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.WARDEN));
        registerDamageableEnchantable(ItemTypes.WARPED_FUNGUS_ON_A_STICK, 100, repairWith(),
                EnchantmentTarget.BREAKABLE,
                EnchantmentTarget.VANISHABLE);
        registerVanilla(ItemTypes.WARPED_SIGN);
        registerVanilla(ItemTypes.WATER_BUCKET)
                .set(ItemComponents.USE_ON, BucketItemHandlers.place(BlockStates.WATER));
        registerVanilla(ItemTypes.WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE);
        registerVanilla(ItemTypes.WHEAT_SEEDS);
        registerVanilla(ItemTypes.WHITE_BUNDLE);
        registerVanilla(ItemTypes.WHITE_CUSHION);
        registerVanilla(ItemTypes.WHITE_DYE);
        registerVanilla(ItemTypes.WHITE_HARNESS);
        registerVanilla(ItemTypes.WILD_ARMOR_TRIM_SMITHING_TEMPLATE);
        registerVanilla(ItemTypes.WIND_CHARGE);
        registerVanilla(ItemTypes.WITCH_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.WITCH));
        registerVanilla(ItemTypes.WITHER_SKELETON_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.WITHER_SKELETON));
        registerVanilla(ItemTypes.WITHER_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.WITHER));
        registerVanilla(ItemTypes.WOLF_ARMOR);
        registerVanilla(ItemTypes.WOLF_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.WOLF));
        registerAxe(ItemTypes.WOODEN_AXE, ToolMaterials.WOOD, repairWith(ItemTags.WOODEN_TOOL_MATERIALS));
        registerHoe(ItemTypes.WOODEN_HOE, ToolMaterials.WOOD, repairWith(ItemTags.WOODEN_TOOL_MATERIALS));
        registerPickaxe(ItemTypes.WOODEN_PICKAXE, ToolMaterials.WOOD, repairWith(ItemTags.WOODEN_TOOL_MATERIALS));
        registerShovel(ItemTypes.WOODEN_SHOVEL, ToolMaterials.WOOD, repairWith(ItemTags.WOODEN_TOOL_MATERIALS));
        registerSpear(ItemTypes.WOODEN_SPEAR, ToolMaterials.WOOD, repairWith(ItemTags.WOODEN_TOOL_MATERIALS));
        registerSword(ItemTypes.WOODEN_SWORD, ToolMaterials.WOOD, repairWith(ItemTags.WOODEN_TOOL_MATERIALS));
        registerVanilla(ItemTypes.WRITABLE_BOOK);
        registerVanilla(ItemTypes.WRITTEN_BOOK);
        registerVanilla(ItemTypes.YELLOW_BUNDLE);
        registerVanilla(ItemTypes.YELLOW_CUSHION);
        registerVanilla(ItemTypes.YELLOW_DYE);
        registerVanilla(ItemTypes.YELLOW_HARNESS);
        registerVanilla(ItemTypes.ZOGLIN_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.ZOGLIN));
        registerVanilla(ItemTypes.ZOMBIE_HORSE_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.ZOMBIE_HORSE));
        registerVanilla(ItemTypes.ZOMBIE_NAUTILUS_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.ZOMBIE_NAUTILUS));
        registerVanilla(ItemTypes.ZOMBIE_PIGMAN_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.ZOMBIE_PIGMAN));
        registerVanilla(ItemTypes.ZOMBIE_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.ZOMBIE));
        registerVanilla(ItemTypes.ZOMBIE_VILLAGER_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.ZOMBIE_VILLAGER));

        registerVanilla(ItemTypes.UNKNOWN);
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

    private void registerVanillaDataSerializers() throws RegistryException {
        this.registerDataSerializer(ItemKeys.BANNER_DATA, new BannerDataSerializer());
        this.registerDataSerializer(ItemKeys.DAMAGE, new PrimitiveSerializer<>("Damage", Integer.class));
        this.registerDataSerializer(ItemKeys.REPAIR_COST, new PrimitiveSerializer<>("RepairCost", Integer.class));
        this.registerDataSerializer(ItemKeys.UNBREAKABLE, new PrimitiveSerializer<>("Unbreakable", Boolean.class));
        this.registerDataSerializer(ItemKeys.MAP_DATA, new MapSerializer());
        this.registerDataSerializer(ItemKeys.BOOK_DATA, new WrittenBookSerializer());
        this.registerDataSerializer(ItemKeys.SPAWN_EGG_TYPE, new EntityTypeSerializer());
    }

    private void registerVanillaBehaviors() {
        this.registerComponent(ItemComponents.ALLOW_OFFHAND, () -> false);
        this.registerComponent(ItemComponents.CAN_BE_CHARGED, () -> false);
        this.registerComponent(ItemComponents.CAN_BE_DEPLETED, () -> false);
        this.registerComponent(ItemComponents.CAN_BE_PLACED, (item) -> false);
        this.registerComponent(ItemComponents.CAN_BE_PLACED_ON, DefaultItemHandlers.CAN_BE_PLACED_ON);
        this.registerComponent(ItemComponents.CAN_DESTROY, DefaultItemHandlers.CAN_DESTROY);
        this.registerComponent(ItemComponents.CAN_DESTROY_IN_CREATIVE, () -> true);
        this.registerComponent(ItemComponents.CAN_ENCHANT_WITH, (item, enchantment) -> false);
        this.registerComponent(ItemComponents.CAN_REPAIR_WITH, (item, material) -> false);
        this.registerComponent(ItemComponents.CAN_STORE_ENCHANTMENTS, () -> true);
        this.registerComponent(ItemComponents.DAMAGEABLE, () -> false);
        this.registerComponent(ItemComponents.FUEL_DURATION, () -> 0f);
        this.registerComponent(ItemComponents.GET_ATTACK_DAMAGE_BONUS, (item) -> 0f);
        this.registerComponent(ItemComponents.GET_BLOCK, (item) -> Optional.empty());
        this.registerComponent(ItemComponents.GET_DAMAGE_CHANCE, (unbreaking) -> 0);
        this.registerComponent(ItemComponents.GET_EQUIPMENT_SLOT, item -> null);
        this.registerComponent(ItemComponents.GET_MAX_DAMAGE, (item) -> 0);
        this.registerComponent(ItemComponents.GET_MAX_STACK_SIZE, (item) -> 64);
        this.registerComponent(ItemComponents.GET_TOOL, item -> null);
        this.registerComponent(ItemComponents.MINE_BLOCK, (item, block, owner) -> item);
        this.registerComponent(ItemComponents.ON_DAMAGE, (item, damage, owner) -> item);
        this.registerComponent(ItemComponents.USE);
        this.registerComponent(ItemComponents.USE_ON);
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
            if (rid == itemPalette.getCreativeItems().get(i).getItem().getDefinition().getRuntimeId()) {
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
