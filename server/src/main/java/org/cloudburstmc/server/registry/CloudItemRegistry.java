package org.cloudburstmc.server.registry;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.data.BehaviorKey;
import org.cloudburstmc.api.data.DataKey;
import org.cloudburstmc.api.item.*;
import org.cloudburstmc.api.item.behavior.ItemBehavior;
import org.cloudburstmc.api.registry.GlobalRegistry;
import org.cloudburstmc.api.registry.ItemRegistry;
import org.cloudburstmc.api.registry.RegistryException;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.api.util.behavior.BehaviorCollection;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition;
import org.cloudburstmc.protocol.bedrock.packet.CreativeContentPacket;
import org.cloudburstmc.protocol.common.DefinitionRegistry;
import org.cloudburstmc.server.block.BlockPalette;
import org.cloudburstmc.server.item.CloudItemDefinition;
import org.cloudburstmc.server.item.ItemPalette;
import org.cloudburstmc.server.item.ItemUtils;
import org.cloudburstmc.server.item.data.serializer.*;
import org.cloudburstmc.server.item.serializer.*;
import org.cloudburstmc.server.registry.behavior.CloudBehaviorCollection;
import org.cloudburstmc.server.registry.behavior.proxy.BehaviorProxies;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Log4j2
public class CloudItemRegistry extends CloudBehaviorRegistry<ItemType> implements ItemRegistry, DefinitionRegistry<CloudItemDefinition> {
    private static final CloudItemRegistry INSTANCE = new CloudItemRegistry(); // Needs to be initialized afterwards

    private final Reference2ReferenceMap<ItemType, BehaviorCollection> behaviorMap = new Reference2ReferenceOpenHashMap<>();
    private final Reference2ReferenceMap<Identifier, ItemType> typeMap = new Reference2ReferenceOpenHashMap<>();
    private final Reference2ObjectMap<ItemType, ItemSerializer> serializers = new Reference2ObjectOpenHashMap<>();
    private final Reference2ObjectMap<DataKey<?, ?>, ItemDataSerializer<?>> dataSerializers = new Reference2ObjectOpenHashMap<>();
    private int hardcodedBlockingId;
    private final ItemPalette itemPalette = new ItemPalette(this);

    private volatile boolean closed;

    private CloudItemRegistry() {
        try {
            this.registerVanillaBehaviors();
            this.registerVanillaItems();
            this.registerVanillaIdentifiers();
            this.registerVanillaDataSerializers();

//            for (ItemDefinition definition : itemPalette.getItemDefinitions()) {
//                if (itemPalette.getDefinition(definition) == Integer.MAX_VALUE) {
//                    System.out.println("Unimplemented item found: " + definition.getName());
//                    registerType(ItemTypes.UNKNOWN, definition);
//                }
//            }
        } catch (RegistryException e) {
            throw new IllegalStateException("Unable to register vanilla items", e);
        }
    }

    public static CloudItemRegistry get() {
        return INSTANCE;
    }

    public synchronized <T> void registerDataSerializer(DataKey<T, T> dataKey, ItemDataSerializer<T> serializer) {
        Preconditions.checkNotNull(dataKey, "dataKey");
        Preconditions.checkNotNull(serializer, "serializer");
        this.dataSerializers.put(dataKey, serializer);
    }

    @Override
    public <F> void registerBehavior(BehaviorKey<F, F> key, F defaultBehavior) {
        this.registerBehaviorInternal(key, defaultBehavior, (context, behavior) -> behavior);
    }


    @Override
    public <F, E> void registerContextBehavior(BehaviorKey<F, E> key, F defaultBehavior) {
        try {
            this.registerBehaviorInternal(key, defaultBehavior, BehaviorProxies.createExecutorProxy(key.getType(), key.getExecutorType()));
        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException |
                 InstantiationException e) {
            throw new IllegalArgumentException("Unable to create behavior proxy for " + key, e);
        }
    }

    @Override
    public BehaviorCollection getBehaviors(ItemType type) {
        return behaviorMap.get(type);
    }

    @Override
    public GlobalRegistry global() {
        //TODO Implementation
        return null;
    }

    public int getHardcodedBlockingId() {
        return this.hardcodedBlockingId;
    }

    public synchronized void register(ItemType type, ItemSerializer serializer, ItemBehavior behavior, Identifier... identifiers) throws RegistryException {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(behavior, "behavior");
        checkClosed();

        if (identifiers == null || identifiers.length == 0) {
            identifiers = new Identifier[]{type.getId()};
        }

        if (this.typeMap.containsValue(type)) {
            throw new RegistryException(type + " has already been registered");
        }

        for (Identifier identifier : identifiers) {
            if (this.typeMap.containsKey(identifier)) {
                throw new RegistryException(identifier + " has already been registered");
            }
        }

        if (serializer != null) {
            this.serializers.put(type, serializer);
        }

        for (Identifier identifier : identifiers) {
            this.typeMap.put(identifier, type);
            int runtimeId = itemPalette.addItem(identifier);
            if (type == ItemTypes.SHIELD) {
                this.hardcodedBlockingId = runtimeId;
            }
        }
    }

    protected synchronized void registerVanilla(ItemType type) throws RegistryException {
        registerVanilla(type, null);
    }

    private synchronized void registerVanilla(ItemType type, ItemSerializer serializer) throws RegistryException {
        Objects.requireNonNull(type, "type");
        checkClosed();

        if (serializer != null) {
            this.serializers.put(type, serializer);
        }

        CloudBehaviorCollection collection = new CloudBehaviorCollection(this);
//        collection.apply(DefaultBlockBehaviours.BLOCK_BEHAVIOR_BASE);

        collection.bake();

        synchronized (this.behaviorMap) {
            if (this.behaviorMap.putIfAbsent(type, collection) != null) {
                throw new RegistryException(type + " is already registered");
            }
        }

        this.registerType(type, type.getId());
    }

    protected void registerBlock(BlockType type) {
        CloudBehaviorCollection collection = new CloudBehaviorCollection(this);
//        collection.apply(DefaultBlockBehaviours.BLOCK_BEHAVIOR_BASE);

        BlockState defaultState = type.getDefaultState();
        collection.overwrite(ItemBehaviors.GET_BLOCK, (behavior, item) -> {
            BlockState state = item.get(ItemKeys.BLOCK_STATE);
            return Optional.of(state != null ? state : defaultState);
        });

        collection.bake();

        synchronized (this.behaviorMap) {
            if (this.behaviorMap.putIfAbsent(type, collection) != null) {
                throw new RegistryException(type + " is already registered");
            }
        }
    }

    public ItemSerializer getSerializer(ItemType type) {
        return serializers.getOrDefault(type, DefaultItemSerializer.INSTANCE);
    }

    public <T> ItemDataSerializer<T> getSerializer(DataKey<T, T> dataKey) {
        //noinspection unchecked
        return (ItemDataSerializer<T>) dataSerializers.get(dataKey);
    }

    public ItemType getType(Identifier id) {
        return this.typeMap.get(id);
    }

    public ItemType getType(int legacyId) {
        return getType(getIdentifier(legacyId));
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

    @Override
    public void register(ItemType itemType, ItemBehavior itemBehavior, Identifier... identifiers) throws RegistryException {
        this.register(itemType,DefaultItemSerializer.INSTANCE,itemBehavior,identifiers);
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
            return blockType;
        }

        return typeMap.getOrDefault(runtimeId, BlockTypes.AIR);
    }

    @Override
    public ItemType getType(int runtimeId, int data) {
        return null;
    }

    @Override
    public CloudItemDefinition getDefinition(int runtimeId) {
        return itemPalette.getDefinition(runtimeId);
    }

    @Override
    public boolean isRegistered(CloudItemDefinition definition) {
        return itemPalette.getDefinition(definition.getRuntimeId()) == definition;
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
        registerVanilla(ItemTypes.ALLAY_SPAWN_EGG);
        registerVanilla(ItemTypes.AMETHYST_SHARD);
        registerVanilla(ItemTypes.ANGLER_POTTERY_SHERD);
        registerVanilla(ItemTypes.APPLE);
        registerVanilla(ItemTypes.ARCHER_POTTERY_SHERD);
        registerVanilla(ItemTypes.ARMADILLO_SCUTE);
        registerVanilla(ItemTypes.ARMADILLO_SPAWN_EGG);
        registerVanilla(ItemTypes.ARMOR_STAND);
        registerVanilla(ItemTypes.ARMS_UP_POTTERY_SHERD);
        registerVanilla(ItemTypes.ARROW);
        registerVanilla(ItemTypes.AXOLOTL_BUCKET);
        registerVanilla(ItemTypes.AXOLOTL_SPAWN_EGG);
        registerVanilla(ItemTypes.BAKED_POTATO);
        registerVanilla(ItemTypes.BAMBOO_CHEST_RAFT);
        registerVanilla(ItemTypes.BAMBOO_RAFT);
        registerVanilla(ItemTypes.BAMBOO_SIGN);
        registerVanilla(ItemTypes.BANNER, new BannerSerializer());
        registerVanilla(ItemTypes.BAT_SPAWN_EGG);
        registerVanilla(ItemTypes.BEE_SPAWN_EGG);
        registerVanilla(ItemTypes.BEEF);
        registerVanilla(ItemTypes.BEETROOT_SEEDS);
        registerVanilla(ItemTypes.BEETROOT_SOUP);
        registerVanilla(ItemTypes.BIRCH_BOAT);
        registerVanilla(ItemTypes.BIRCH_CHEST_BOAT);
        registerVanilla(ItemTypes.BIRCH_SIGN);
        registerVanilla(ItemTypes.BLACK_BUNDLE);
        registerVanilla(ItemTypes.BLACK_DYE);
        registerVanilla(ItemTypes.BLACK_HARNESS);
        registerVanilla(ItemTypes.BLADE_POTTERY_SHERD);
        registerVanilla(ItemTypes.BLAZE_POWDER);
        registerVanilla(ItemTypes.BLAZE_ROD);
        registerVanilla(ItemTypes.BLAZE_SPAWN_EGG);
        registerVanilla(ItemTypes.BLUE_BUNDLE);
        registerVanilla(ItemTypes.BLUE_DYE);
        registerVanilla(ItemTypes.BLUE_EGG);
        registerVanilla(ItemTypes.BLUE_HARNESS);
        registerVanilla(ItemTypes.BOGGED_SPAWN_EGG);
        registerVanilla(ItemTypes.BOLT_ARMOR_TRIM_SMITHING_TEMPLATE);
        registerVanilla(ItemTypes.BONE);
        registerVanilla(ItemTypes.BONE_MEAL);
        registerVanilla(ItemTypes.BOOK);
        registerVanilla(ItemTypes.BORDURE_INDENTED_BANNER_PATTERN);
        registerVanilla(ItemTypes.BOW);
        registerVanilla(ItemTypes.BOWL);
        registerVanilla(ItemTypes.BREAD);
        registerVanilla(ItemTypes.BREEZE_ROD);
        registerVanilla(ItemTypes.BREEZE_SPAWN_EGG);
        registerVanilla(ItemTypes.BREWER_POTTERY_SHERD);
        registerVanilla(ItemTypes.BRICK);
        registerVanilla(ItemTypes.BROWN_BUNDLE);
        registerVanilla(ItemTypes.BROWN_DYE);
        registerVanilla(ItemTypes.BROWN_EGG);
        registerVanilla(ItemTypes.BROWN_HARNESS);
        registerVanilla(ItemTypes.BRUSH);
        registerVanilla(ItemTypes.BUCKET);
        registerVanilla(ItemTypes.BUNDLE);
        registerVanilla(ItemTypes.BURN_POTTERY_SHERD);
        registerVanilla(ItemTypes.CAMEL_HUSK_SPAWN_EGG);
        registerVanilla(ItemTypes.CAMEL_SPAWN_EGG);
        registerVanilla(ItemTypes.CARROT);
        registerVanilla(ItemTypes.CARROT_ON_A_STICK);
        registerVanilla(ItemTypes.CAT_SPAWN_EGG);
        registerVanilla(ItemTypes.CAVE_SPIDER_SPAWN_EGG);
        registerVanilla(ItemTypes.CHAINMAIL_BOOTS);
        registerVanilla(ItemTypes.CHAINMAIL_CHESTPLATE);
        registerVanilla(ItemTypes.CHAINMAIL_HELMET);
        registerVanilla(ItemTypes.CHAINMAIL_LEGGINGS);
        registerVanilla(ItemTypes.CHARCOAL);
        registerVanilla(ItemTypes.CHERRY_BOAT);
        registerVanilla(ItemTypes.CHERRY_CHEST_BOAT);
        registerVanilla(ItemTypes.CHERRY_SIGN);
        registerVanilla(ItemTypes.CHEST_MINECART);
        registerVanilla(ItemTypes.CHICKEN);
        registerVanilla(ItemTypes.CHICKEN_SPAWN_EGG);
        registerVanilla(ItemTypes.CHORUS_FRUIT);
        registerVanilla(ItemTypes.CLAY_BALL);
        registerVanilla(ItemTypes.CLOCK);
        registerVanilla(ItemTypes.COAL);
        registerVanilla(ItemTypes.COAST_ARMOR_TRIM_SMITHING_TEMPLATE);
        registerVanilla(ItemTypes.COCOA_BEANS);
        registerVanilla(ItemTypes.COD);
        registerVanilla(ItemTypes.COD_BUCKET);
        registerVanilla(ItemTypes.COD_SPAWN_EGG);
        registerVanilla(ItemTypes.COMPARATOR);
        registerVanilla(ItemTypes.COMPASS);
        registerVanilla(ItemTypes.COOKED_BEEF);
        registerVanilla(ItemTypes.COOKED_CHICKEN);
        registerVanilla(ItemTypes.COOKED_COD);
        registerVanilla(ItemTypes.COOKED_MUTTON);
        registerVanilla(ItemTypes.COOKED_PORKCHOP);
        registerVanilla(ItemTypes.COOKED_RABBIT);
        registerVanilla(ItemTypes.COOKED_SALMON);
        registerVanilla(ItemTypes.COOKIE);
        registerVanilla(ItemTypes.COPPER_AXE);
        registerVanilla(ItemTypes.COPPER_BOOTS);
        registerVanilla(ItemTypes.COPPER_CHESTPLATE);
        registerVanilla(ItemTypes.COPPER_GOLEM_SPAWN_EGG);
        registerVanilla(ItemTypes.COPPER_HELMET);
        registerVanilla(ItemTypes.COPPER_HOE);
        registerVanilla(ItemTypes.COPPER_HORSE_ARMOR);
        registerVanilla(ItemTypes.COPPER_INGOT);
        registerVanilla(ItemTypes.COPPER_LEGGINGS);
        registerVanilla(ItemTypes.COPPER_NAUTILUS_ARMOR);
        registerVanilla(ItemTypes.COPPER_NUGGET);
        registerVanilla(ItemTypes.COPPER_PICKAXE);
        registerVanilla(ItemTypes.COPPER_SHOVEL);
        registerVanilla(ItemTypes.COPPER_SPEAR);
        registerVanilla(ItemTypes.COPPER_SWORD);
        registerVanilla(ItemTypes.COW_SPAWN_EGG);
        registerVanilla(ItemTypes.CREAKING_SPAWN_EGG);
        registerVanilla(ItemTypes.CREEPER_BANNER_PATTERN);
        registerVanilla(ItemTypes.CREEPER_SPAWN_EGG);
        registerVanilla(ItemTypes.CRIMSON_SIGN);
        registerVanilla(ItemTypes.CROSSBOW);
        registerVanilla(ItemTypes.CYAN_BUNDLE);
        registerVanilla(ItemTypes.CYAN_DYE);
        registerVanilla(ItemTypes.CYAN_HARNESS);
        registerVanilla(ItemTypes.DANGER_POTTERY_SHERD);
        registerVanilla(ItemTypes.DARK_OAK_BOAT);
        registerVanilla(ItemTypes.DARK_OAK_CHEST_BOAT);
        registerVanilla(ItemTypes.DARK_OAK_SIGN);
        registerVanilla(ItemTypes.DIAMOND);
        registerVanilla(ItemTypes.DIAMOND_AXE);
        registerVanilla(ItemTypes.DIAMOND_BOOTS);
        registerVanilla(ItemTypes.DIAMOND_CHESTPLATE);
        registerVanilla(ItemTypes.DIAMOND_HELMET);
        registerVanilla(ItemTypes.DIAMOND_HOE);
        registerVanilla(ItemTypes.DIAMOND_HORSE_ARMOR);
        registerVanilla(ItemTypes.DIAMOND_LEGGINGS);
        registerVanilla(ItemTypes.DIAMOND_NAUTILUS_ARMOR);
        registerVanilla(ItemTypes.DIAMOND_PICKAXE);
        registerVanilla(ItemTypes.DIAMOND_SHOVEL);
        registerVanilla(ItemTypes.DIAMOND_SPEAR);
        registerVanilla(ItemTypes.DIAMOND_SWORD);
        registerVanilla(ItemTypes.DISC_FRAGMENT_5);
        registerVanilla(ItemTypes.DOLPHIN_SPAWN_EGG);
        registerVanilla(ItemTypes.DONKEY_SPAWN_EGG);
        registerVanilla(ItemTypes.DRAGON_BREATH);
        registerVanilla(ItemTypes.DRIED_KELP);
        registerVanilla(ItemTypes.DROWNED_SPAWN_EGG);
        registerVanilla(ItemTypes.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE);
        registerVanilla(ItemTypes.ECHO_SHARD);
        registerVanilla(ItemTypes.EGG);
        registerVanilla(ItemTypes.ELDER_GUARDIAN_SPAWN_EGG);
        registerVanilla(ItemTypes.ELYTRA);
        registerVanilla(ItemTypes.EMERALD);
        registerVanilla(ItemTypes.EMPTY_MAP);
        registerVanilla(ItemTypes.ENCHANTED_BOOK);
        registerVanilla(ItemTypes.ENCHANTED_GOLDEN_APPLE);
        registerVanilla(ItemTypes.END_CRYSTAL);
        registerVanilla(ItemTypes.ENDER_EYE);
        registerVanilla(ItemTypes.ENDER_PEARL);
        registerVanilla(ItemTypes.ENDERMAN_SPAWN_EGG);
        registerVanilla(ItemTypes.ENDERMITE_SPAWN_EGG);
        registerVanilla(ItemTypes.EVOKER_SPAWN_EGG);
        registerVanilla(ItemTypes.EXPERIENCE_BOTTLE);
        registerVanilla(ItemTypes.EXPLORER_POTTERY_SHERD);
        registerVanilla(ItemTypes.EYE_ARMOR_TRIM_SMITHING_TEMPLATE);
        registerVanilla(ItemTypes.FEATHER);
        registerVanilla(ItemTypes.FENCE_GATE);
        registerVanilla(ItemTypes.FERMENTED_SPIDER_EYE);
        registerVanilla(ItemTypes.FIELD_MASONED_BANNER_PATTERN);
        registerVanilla(ItemTypes.FILLED_MAP);
        registerVanilla(ItemTypes.FIRE_CHARGE);
        registerVanilla(ItemTypes.FIREWORK_ROCKET);
        registerVanilla(ItemTypes.FIREWORK_STAR);
        registerVanilla(ItemTypes.FISHING_ROD);
        registerVanilla(ItemTypes.FLINT);
        registerVanilla(ItemTypes.FLINT_AND_STEEL);
        registerVanilla(ItemTypes.FLOW_ARMOR_TRIM_SMITHING_TEMPLATE);
        registerVanilla(ItemTypes.FLOW_BANNER_PATTERN);
        registerVanilla(ItemTypes.FLOW_POTTERY_SHERD);
        registerVanilla(ItemTypes.FLOWER_BANNER_PATTERN);
        registerVanilla(ItemTypes.FOX_SPAWN_EGG);
        registerVanilla(ItemTypes.FRIEND_POTTERY_SHERD);
        registerVanilla(ItemTypes.FROG_SPAWN_EGG);
        registerVanilla(ItemTypes.GHAST_SPAWN_EGG);
        registerVanilla(ItemTypes.GHAST_TEAR);
        registerVanilla(ItemTypes.GLASS_BOTTLE);
        registerVanilla(ItemTypes.GLISTERING_MELON_SLICE);
        registerVanilla(ItemTypes.GLOBE_BANNER_PATTERN);
        registerVanilla(ItemTypes.GLOW_BERRIES);
        registerVanilla(ItemTypes.GLOW_INK_SAC);
        registerVanilla(ItemTypes.GLOW_SQUID_SPAWN_EGG);
        registerVanilla(ItemTypes.GLOWSTONE_DUST);
        registerVanilla(ItemTypes.GOAT_HORN);
        registerVanilla(ItemTypes.GOAT_SPAWN_EGG);
        registerVanilla(ItemTypes.GOLD_INGOT);
        registerVanilla(ItemTypes.GOLD_NUGGET);
        registerVanilla(ItemTypes.GOLDEN_APPLE);
        registerVanilla(ItemTypes.GOLDEN_AXE);
        registerVanilla(ItemTypes.GOLDEN_BOOTS);
        registerVanilla(ItemTypes.GOLDEN_CARROT);
        registerVanilla(ItemTypes.GOLDEN_CHESTPLATE);
        registerVanilla(ItemTypes.GOLDEN_HELMET);
        registerVanilla(ItemTypes.GOLDEN_HOE);
        registerVanilla(ItemTypes.GOLDEN_HORSE_ARMOR);
        registerVanilla(ItemTypes.GOLDEN_LEGGINGS);
        registerVanilla(ItemTypes.GOLDEN_NAUTILUS_ARMOR);
        registerVanilla(ItemTypes.GOLDEN_PICKAXE);
        registerVanilla(ItemTypes.GOLDEN_SHOVEL);
        registerVanilla(ItemTypes.GOLDEN_SPEAR);
        registerVanilla(ItemTypes.GOLDEN_SWORD);
        registerVanilla(ItemTypes.GRAY_BUNDLE);
        registerVanilla(ItemTypes.GRAY_DYE);
        registerVanilla(ItemTypes.GRAY_HARNESS);
        registerVanilla(ItemTypes.GREEN_BUNDLE);
        registerVanilla(ItemTypes.GREEN_DYE);
        registerVanilla(ItemTypes.GREEN_HARNESS);
        registerVanilla(ItemTypes.GUARDIAN_SPAWN_EGG);
        registerVanilla(ItemTypes.GUNPOWDER);
        registerVanilla(ItemTypes.GUSTER_BANNER_PATTERN);
        registerVanilla(ItemTypes.GUSTER_POTTERY_SHERD);
        registerVanilla(ItemTypes.HAPPY_GHAST_SPAWN_EGG);
        registerVanilla(ItemTypes.HEART_OF_THE_SEA);
        registerVanilla(ItemTypes.HEART_POTTERY_SHERD);
        registerVanilla(ItemTypes.HEARTBREAK_POTTERY_SHERD);
        registerVanilla(ItemTypes.HOGLIN_SPAWN_EGG);
        registerVanilla(ItemTypes.HONEY_BOTTLE);
        registerVanilla(ItemTypes.HONEYCOMB);
        registerVanilla(ItemTypes.HOPPER_MINECART);
        registerVanilla(ItemTypes.HORSE_SPAWN_EGG);
        registerVanilla(ItemTypes.HOST_ARMOR_TRIM_SMITHING_TEMPLATE);
        registerVanilla(ItemTypes.HOWL_POTTERY_SHERD);
        registerVanilla(ItemTypes.HUSK_SPAWN_EGG);
        registerVanilla(ItemTypes.INK_SAC);
        registerVanilla(ItemTypes.IRON_AXE);
        registerVanilla(ItemTypes.IRON_BOOTS);
        registerVanilla(ItemTypes.IRON_CHESTPLATE);
        registerVanilla(ItemTypes.IRON_GOLEM_SPAWN_EGG);
        registerVanilla(ItemTypes.IRON_HELMET);
        registerVanilla(ItemTypes.IRON_HOE);
        registerVanilla(ItemTypes.IRON_HORSE_ARMOR);
        registerVanilla(ItemTypes.IRON_INGOT);
        registerVanilla(ItemTypes.IRON_LEGGINGS);
        registerVanilla(ItemTypes.IRON_NAUTILUS_ARMOR);
        registerVanilla(ItemTypes.IRON_NUGGET);
        registerVanilla(ItemTypes.IRON_PICKAXE);
        registerVanilla(ItemTypes.IRON_SHOVEL);
        registerVanilla(ItemTypes.IRON_SPEAR);
        registerVanilla(ItemTypes.IRON_SWORD);
        registerVanilla(ItemTypes.JUNGLE_BOAT);
        registerVanilla(ItemTypes.JUNGLE_CHEST_BOAT);
        registerVanilla(ItemTypes.JUNGLE_SIGN);
        registerVanilla(ItemTypes.LAPIS_LAZULI);
        registerVanilla(ItemTypes.LAVA_BUCKET);
        registerVanilla(ItemTypes.LEAD);
        registerVanilla(ItemTypes.LEATHER);
        registerVanilla(ItemTypes.LEATHER_BOOTS);
        registerVanilla(ItemTypes.LEATHER_CHESTPLATE);
        registerVanilla(ItemTypes.LEATHER_HELMET);
        registerVanilla(ItemTypes.LEATHER_HORSE_ARMOR);
        registerVanilla(ItemTypes.LEATHER_LEGGINGS);
        registerVanilla(ItemTypes.LIGHT_BLUE_BUNDLE);
        registerVanilla(ItemTypes.LIGHT_BLUE_DYE);
        registerVanilla(ItemTypes.LIGHT_BLUE_HARNESS);
        registerVanilla(ItemTypes.LIGHT_GRAY_BUNDLE);
        registerVanilla(ItemTypes.LIGHT_GRAY_DYE);
        registerVanilla(ItemTypes.LIGHT_GRAY_HARNESS);
        registerVanilla(ItemTypes.LIME_BUNDLE);
        registerVanilla(ItemTypes.LIME_DYE);
        registerVanilla(ItemTypes.LIME_HARNESS);
        registerVanilla(ItemTypes.LINGERING_POTION);
        registerVanilla(ItemTypes.LLAMA_SPAWN_EGG);
        registerVanilla(ItemTypes.MACE);
        registerVanilla(ItemTypes.MAGENTA_BUNDLE);
        registerVanilla(ItemTypes.MAGENTA_DYE);
        registerVanilla(ItemTypes.MAGENTA_HARNESS);
        registerVanilla(ItemTypes.MAGMA_CREAM);
        registerVanilla(ItemTypes.MAGMA_CUBE_SPAWN_EGG);
        registerVanilla(ItemTypes.MANGROVE_BOAT);
        registerVanilla(ItemTypes.MANGROVE_CHEST_BOAT);
        registerVanilla(ItemTypes.MANGROVE_SIGN);
        registerVanilla(ItemTypes.MELON_SEEDS);
        registerVanilla(ItemTypes.MELON_SLICE);
        registerVanilla(ItemTypes.MILK_BUCKET);
        registerVanilla(ItemTypes.MINECART);
        registerVanilla(ItemTypes.MINER_POTTERY_SHERD);
        registerVanilla(ItemTypes.MOJANG_BANNER_PATTERN);
        registerVanilla(ItemTypes.MOOSHROOM_SPAWN_EGG);
        registerVanilla(ItemTypes.MOURNER_POTTERY_SHERD);
        registerVanilla(ItemTypes.MULE_SPAWN_EGG);
        registerVanilla(ItemTypes.MUSHROOM_STEW);
        registerVanilla(ItemTypes.MUSIC_DISC_11);
        registerVanilla(ItemTypes.MUSIC_DISC_13);
        registerVanilla(ItemTypes.MUSIC_DISC_5);
        registerVanilla(ItemTypes.MUSIC_DISC_BLOCKS);
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
        registerVanilla(ItemTypes.NAUTILUS_SPAWN_EGG);
        registerVanilla(ItemTypes.NETHER_STAR);
        registerVanilla(ItemTypes.NETHERBRICK);
        registerVanilla(ItemTypes.NETHERITE_AXE);
        registerVanilla(ItemTypes.NETHERITE_BOOTS);
        registerVanilla(ItemTypes.NETHERITE_CHESTPLATE);
        registerVanilla(ItemTypes.NETHERITE_HELMET);
        registerVanilla(ItemTypes.NETHERITE_HOE);
        registerVanilla(ItemTypes.NETHERITE_HORSE_ARMOR);
        registerVanilla(ItemTypes.NETHERITE_INGOT);
        registerVanilla(ItemTypes.NETHERITE_LEGGINGS);
        registerVanilla(ItemTypes.NETHERITE_NAUTILUS_ARMOR);
        registerVanilla(ItemTypes.NETHERITE_PICKAXE);
        registerVanilla(ItemTypes.NETHERITE_SCRAP);
        registerVanilla(ItemTypes.NETHERITE_SHOVEL);
        registerVanilla(ItemTypes.NETHERITE_SPEAR);
        registerVanilla(ItemTypes.NETHERITE_SWORD);
        registerVanilla(ItemTypes.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
        registerVanilla(ItemTypes.NOTEBLOCK);
        registerVanilla(ItemTypes.OAK_BOAT);
        registerVanilla(ItemTypes.OAK_CHEST_BOAT);
        registerVanilla(ItemTypes.OAK_SIGN);
        registerVanilla(ItemTypes.OCELOT_SPAWN_EGG);
        registerVanilla(ItemTypes.OMINOUS_BOTTLE);
        registerVanilla(ItemTypes.OMINOUS_TRIAL_KEY);
        registerVanilla(ItemTypes.ORANGE_BUNDLE);
        registerVanilla(ItemTypes.ORANGE_DYE);
        registerVanilla(ItemTypes.ORANGE_HARNESS);
        registerVanilla(ItemTypes.PAINTING);
        registerVanilla(ItemTypes.PALE_OAK_BOAT);
        registerVanilla(ItemTypes.PALE_OAK_CHEST_BOAT);
        registerVanilla(ItemTypes.PALE_OAK_SIGN);
        registerVanilla(ItemTypes.PANDA_SPAWN_EGG);
        registerVanilla(ItemTypes.PAPER);
        registerVanilla(ItemTypes.PARCHED_SPAWN_EGG);
        registerVanilla(ItemTypes.PARROT_SPAWN_EGG);
        registerVanilla(ItemTypes.PHANTOM_MEMBRANE);
        registerVanilla(ItemTypes.PHANTOM_SPAWN_EGG);
        registerVanilla(ItemTypes.PIG_SPAWN_EGG);
        registerVanilla(ItemTypes.PIGLIN_BANNER_PATTERN);
        registerVanilla(ItemTypes.PIGLIN_BRUTE_SPAWN_EGG);
        registerVanilla(ItemTypes.PIGLIN_SPAWN_EGG);
        registerVanilla(ItemTypes.PILLAGER_SPAWN_EGG);
        registerVanilla(ItemTypes.PINK_BUNDLE);
        registerVanilla(ItemTypes.PINK_DYE);
        registerVanilla(ItemTypes.PINK_HARNESS);
        registerVanilla(ItemTypes.PITCHER_POD);
        registerVanilla(ItemTypes.PLENTY_POTTERY_SHERD);
        registerVanilla(ItemTypes.POISONOUS_POTATO);
        registerVanilla(ItemTypes.POLAR_BEAR_SPAWN_EGG);
        registerVanilla(ItemTypes.POPPED_CHORUS_FRUIT);
        registerVanilla(ItemTypes.PORKCHOP);
        registerVanilla(ItemTypes.POTATO);
        registerVanilla(ItemTypes.POTION);
        registerVanilla(ItemTypes.POWDER_SNOW_BUCKET);
        registerVanilla(ItemTypes.PRISMARINE_CRYSTALS);
        registerVanilla(ItemTypes.PRISMARINE_SHARD);
        registerVanilla(ItemTypes.PRIZE_POTTERY_SHERD);
        registerVanilla(ItemTypes.PUFFERFISH);
        registerVanilla(ItemTypes.PUFFERFISH_BUCKET);
        registerVanilla(ItemTypes.PUFFERFISH_SPAWN_EGG);
        registerVanilla(ItemTypes.PUMPKIN_PIE);
        registerVanilla(ItemTypes.PUMPKIN_SEEDS);
        registerVanilla(ItemTypes.PURPLE_BUNDLE);
        registerVanilla(ItemTypes.PURPLE_DYE);
        registerVanilla(ItemTypes.PURPLE_HARNESS);
        registerVanilla(ItemTypes.QUARTZ);
        registerVanilla(ItemTypes.RABBIT);
        registerVanilla(ItemTypes.RABBIT_FOOT);
        registerVanilla(ItemTypes.RABBIT_HIDE);
        registerVanilla(ItemTypes.RABBIT_SPAWN_EGG);
        registerVanilla(ItemTypes.RABBIT_STEW);
        registerVanilla(ItemTypes.RAISER_ARMOR_TRIM_SMITHING_TEMPLATE);
        registerVanilla(ItemTypes.RAVAGER_SPAWN_EGG);
        registerVanilla(ItemTypes.RAW_COPPER);
        registerVanilla(ItemTypes.RAW_GOLD);
        registerVanilla(ItemTypes.RAW_IRON);
        registerVanilla(ItemTypes.RECOVERY_COMPASS);
        registerVanilla(ItemTypes.RED_BUNDLE);
        registerVanilla(ItemTypes.RED_DYE);
        registerVanilla(ItemTypes.RED_HARNESS);
        registerVanilla(ItemTypes.REDSTONE);
        registerVanilla(ItemTypes.REPEATER);
        registerVanilla(ItemTypes.RESIN_BRICK);
        registerVanilla(ItemTypes.RIB_ARMOR_TRIM_SMITHING_TEMPLATE);
        registerVanilla(ItemTypes.ROTTEN_FLESH);
        registerVanilla(ItemTypes.SADDLE);
        registerVanilla(ItemTypes.SALMON);
        registerVanilla(ItemTypes.SALMON_BUCKET);
        registerVanilla(ItemTypes.SALMON_SPAWN_EGG);
        registerVanilla(ItemTypes.SCRAPE_POTTERY_SHERD);
        registerVanilla(ItemTypes.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE);
        registerVanilla(ItemTypes.SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE);
        registerVanilla(ItemTypes.SHEAF_POTTERY_SHERD);
        registerVanilla(ItemTypes.SHEARS);
        registerVanilla(ItemTypes.SHEEP_SPAWN_EGG);
        registerVanilla(ItemTypes.SHELTER_POTTERY_SHERD);
        registerVanilla(ItemTypes.SHIELD);
        registerVanilla(ItemTypes.SHULKER_SHELL);
        registerVanilla(ItemTypes.SHULKER_SPAWN_EGG);
        registerVanilla(ItemTypes.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE);
        registerVanilla(ItemTypes.SILVER_GLAZED_TERRACOTTA);
        registerVanilla(ItemTypes.SILVERFISH_SPAWN_EGG);
        registerVanilla(ItemTypes.SKELETON_HORSE_SPAWN_EGG);
        registerVanilla(ItemTypes.SKELETON_SPAWN_EGG);
        registerVanilla(ItemTypes.SKULL_BANNER_PATTERN);
        registerVanilla(ItemTypes.SKULL_POTTERY_SHERD);
        registerVanilla(ItemTypes.SLIME_BALL);
        registerVanilla(ItemTypes.SLIME_SPAWN_EGG);
        registerVanilla(ItemTypes.SNIFFER_SPAWN_EGG);
        registerVanilla(ItemTypes.SNORT_POTTERY_SHERD);
        registerVanilla(ItemTypes.SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE);
        registerVanilla(ItemTypes.SNOW_GOLEM_SPAWN_EGG);
        registerVanilla(ItemTypes.SNOWBALL);
        registerVanilla(ItemTypes.SPIDER_EYE);
        registerVanilla(ItemTypes.SPIDER_SPAWN_EGG);
        registerVanilla(ItemTypes.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE);
        registerVanilla(ItemTypes.SPLASH_POTION);
        registerVanilla(ItemTypes.SPRUCE_BOAT);
        registerVanilla(ItemTypes.SPRUCE_CHEST_BOAT);
        registerVanilla(ItemTypes.SPRUCE_SIGN);
        registerVanilla(ItemTypes.SPYGLASS);
        registerVanilla(ItemTypes.SQUID_SPAWN_EGG);
        registerVanilla(ItemTypes.STICK);
        registerVanilla(ItemTypes.STONE_AXE);
        registerVanilla(ItemTypes.STONE_HOE);
        registerVanilla(ItemTypes.STONE_PICKAXE);
        registerVanilla(ItemTypes.STONE_SHOVEL);
        registerVanilla(ItemTypes.STONE_SPEAR);
        registerVanilla(ItemTypes.STONE_SWORD);
        registerVanilla(ItemTypes.STRAY_SPAWN_EGG);
        registerVanilla(ItemTypes.STRIDER_SPAWN_EGG);
        registerVanilla(ItemTypes.STRING);
        registerVanilla(ItemTypes.SUGAR);
        registerVanilla(ItemTypes.SUGAR_CANE);
        registerVanilla(ItemTypes.SUSPICIOUS_STEW);
        registerVanilla(ItemTypes.SWEET_BERRIES);
        registerVanilla(ItemTypes.TADPOLE_BUCKET);
        registerVanilla(ItemTypes.TADPOLE_SPAWN_EGG);
        registerVanilla(ItemTypes.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE);
        registerVanilla(ItemTypes.TNT_MINECART);
        registerVanilla(ItemTypes.TORCHFLOWER_SEEDS);
        registerVanilla(ItemTypes.TOTEM_OF_UNDYING);
        registerVanilla(ItemTypes.TRADER_LLAMA_SPAWN_EGG);
        registerVanilla(ItemTypes.TRAPDOOR);
        registerVanilla(ItemTypes.TRIAL_KEY);
        registerVanilla(ItemTypes.TRIDENT);
        registerVanilla(ItemTypes.TROPICAL_FISH);
        registerVanilla(ItemTypes.TROPICAL_FISH_BUCKET);
        registerVanilla(ItemTypes.TROPICAL_FISH_SPAWN_EGG);
        registerVanilla(ItemTypes.TURTLE_HELMET);
        registerVanilla(ItemTypes.TURTLE_SCUTE);
        registerVanilla(ItemTypes.TURTLE_SPAWN_EGG);
        registerVanilla(ItemTypes.VEX_ARMOR_TRIM_SMITHING_TEMPLATE);
        registerVanilla(ItemTypes.VEX_SPAWN_EGG);
        registerVanilla(ItemTypes.VILLAGER_SPAWN_EGG);
        registerVanilla(ItemTypes.VINDICATOR_SPAWN_EGG);
        registerVanilla(ItemTypes.WANDERING_TRADER_SPAWN_EGG);
        registerVanilla(ItemTypes.WARD_ARMOR_TRIM_SMITHING_TEMPLATE);
        registerVanilla(ItemTypes.WARDEN_SPAWN_EGG);
        registerVanilla(ItemTypes.WARPED_FUNGUS_ON_A_STICK);
        registerVanilla(ItemTypes.WARPED_SIGN);
        registerVanilla(ItemTypes.WATER_BUCKET);
        registerVanilla(ItemTypes.WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE);
        registerVanilla(ItemTypes.WHEAT_SEEDS);
        registerVanilla(ItemTypes.WHITE_BUNDLE);
        registerVanilla(ItemTypes.WHITE_DYE);
        registerVanilla(ItemTypes.WHITE_HARNESS);
        registerVanilla(ItemTypes.WILD_ARMOR_TRIM_SMITHING_TEMPLATE);
        registerVanilla(ItemTypes.WIND_CHARGE);
        registerVanilla(ItemTypes.WITCH_SPAWN_EGG);
        registerVanilla(ItemTypes.WITHER_SKELETON_SPAWN_EGG);
        registerVanilla(ItemTypes.WOLF_ARMOR);
        registerVanilla(ItemTypes.WOLF_SPAWN_EGG);
        registerVanilla(ItemTypes.WOODEN_AXE);
        registerVanilla(ItemTypes.WOODEN_BUTTON);
        registerVanilla(ItemTypes.WOODEN_DOOR);
        registerVanilla(ItemTypes.WOODEN_HOE);
        registerVanilla(ItemTypes.WOODEN_PICKAXE);
        registerVanilla(ItemTypes.WOODEN_PRESSURE_PLATE);
        registerVanilla(ItemTypes.WOODEN_SHOVEL);
        registerVanilla(ItemTypes.WOODEN_SPEAR);
        registerVanilla(ItemTypes.WOODEN_SWORD);
        registerVanilla(ItemTypes.WRITABLE_BOOK);
        registerVanilla(ItemTypes.YELLOW_BUNDLE);
        registerVanilla(ItemTypes.YELLOW_DYE);
        registerVanilla(ItemTypes.YELLOW_HARNESS);
        registerVanilla(ItemTypes.ZOGLIN_SPAWN_EGG);
        registerVanilla(ItemTypes.ZOMBIE_HORSE_SPAWN_EGG);
        registerVanilla(ItemTypes.ZOMBIE_NAUTILUS_SPAWN_EGG);
        registerVanilla(ItemTypes.ZOMBIE_PIGMAN_SPAWN_EGG);
        registerVanilla(ItemTypes.ZOMBIE_SPAWN_EGG);
        registerVanilla(ItemTypes.ZOMBIE_VILLAGER_SPAWN_EGG);

        registerVanilla(ItemTypes.UNKNOWN);
    }

    private void registerType(ItemType type, Identifier id) {
        this.typeMap.put(id, type);
        int runtime = itemPalette.addItem(id);
        if (type == ItemTypes.SHIELD) {
            this.hardcodedBlockingId = runtime;
        }
    }

    private void registerType(ItemType type, Identifier... identifiers) {
        for (Identifier id : identifiers) {
            this.typeMap.put(id, type);
        }
    }

    private void registerVanillaIdentifiers() {
    }

    private void registerVanillaDataSerializers() {
        this.registerDataSerializer(ItemKeys.BANNER_DATA, new BannerDataSerializer());
        this.registerDataSerializer(ItemKeys.DAMAGE, new PrimitiveSerializer<>("Damage", Integer.class));
        this.registerDataSerializer(ItemKeys.UNBREAKABLE, new PrimitiveSerializer<>("Unbreakable", Boolean.class));
        this.registerDataSerializer(ItemKeys.FIREWORK_DATA, new FireworkSerializer());
        this.registerDataSerializer(ItemKeys.MAP_DATA, new MapSerializer());
        this.registerDataSerializer(ItemKeys.BOOK_DATA, new WrittenBookSerializer());
//        this.registerDataSerializer(EntityType.class, new EntityTypeSerializer());
    }

    private void registerVanillaBehaviors() {
        this.registerContextBehavior(ItemBehaviors.GET_MAX_STACK_SIZE, (behavior) -> 64);
        this.registerContextBehavior(ItemBehaviors.GET_MAX_DAMAGE, (behavior) -> 0);
        this.registerContextBehavior(ItemBehaviors.MINE_BLOCK, (behavior, item, block, owner) -> item);
        this.registerContextBehavior(ItemBehaviors.ON_DAMAGE, (behavior, item, damage, owner) -> item);
        this.registerContextBehavior(ItemBehaviors.CAN_DESTROY, (behavior, block) -> true);
        this.registerContextBehavior(ItemBehaviors.GET_DESTROY_SPEED, (behavior, item, block) -> 1);
        this.registerContextBehavior(ItemBehaviors.GET_DESTROY_SPEED_BONUS, (behavior, item) -> 0);
        this.registerBehavior(ItemBehaviors.CAN_DESTROY_IN_CREATIVE, false);
        this.registerContextBehavior(ItemBehaviors.GET_DAMAGE_CHANCE, (behavior, unbreaking) -> 0);
        this.registerContextBehavior(ItemBehaviors.CAN_BE_USED, (behavior, item) -> false);
        this.registerContextBehavior(ItemBehaviors.USE_ON, (behavior, item, entity, blockPos, face, clickPos) -> item);
        this.registerBehavior(ItemBehaviors.GET_FUEL_DURATION, 0f);
        this.registerContextBehavior(ItemBehaviors.GET_ATTACH_DAMAGE, (behavior) -> 0f);
        this.registerContextBehavior(ItemBehaviors.IS_TOOL, (behavior, item) -> false);
        this.registerContextBehavior(ItemBehaviors.CAN_BE_PLACED, (behavior, item) -> false);
        this.registerContextBehavior(ItemBehaviors.CAN_BE_PLACED_ON, (behavior, item, block) -> true);
        this.registerContextBehavior(ItemBehaviors.GET_BLOCK, (behavior, item) -> Optional.empty());
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