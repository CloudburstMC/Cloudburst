package org.cloudburstmc.server.registry;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import lombok.extern.log4j.Log4j2;
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
import org.cloudburstmc.protocol.bedrock.data.defintions.ItemDefinition;
import org.cloudburstmc.protocol.bedrock.packet.CreativeContentPacket;
import org.cloudburstmc.server.block.BlockPalette;
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
public class CloudItemRegistry extends CloudBehaviorRegistry<ItemType> implements ItemRegistry, Registry {
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

            for (Identifier id : itemPalette.getItemIds()) {
                if (itemPalette.getRuntimeId(id) == Integer.MAX_VALUE) {
                    System.out.println("Unimplemented item found: " + id.getName());
                    registerType(ItemTypes.UNKNOWN, id);
                }
            }
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

    public int getRuntimeId(Identifier identifier) throws RegistryException {
        return getRuntimeId(identifier, 0);
    }

    public int getRuntimeId(Identifier identifier, int meta) throws RegistryException {
        int runtimeId = itemPalette.getRuntimeId(identifier, meta);
        if (runtimeId == Integer.MAX_VALUE) {
            throw new RegistryException(identifier + " is not a registered item");
        }
        return runtimeId;
    }

    @Override
    public ImmutableList<Identifier> getItems() {
        return itemPalette.getItemIds();
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
        registerVanilla(ItemTypes.IRON_SHOVEL);
        registerVanilla(ItemTypes.IRON_PICKAXE);
        registerVanilla(ItemTypes.IRON_AXE);
        registerVanilla(ItemTypes.FLINT_AND_STEEL);
        registerVanilla(ItemTypes.APPLE);
        registerVanilla(ItemTypes.BOW);
        registerVanilla(ItemTypes.ARROW);
        registerVanilla(ItemTypes.COAL);
        registerVanilla(ItemTypes.DIAMOND);
        registerVanilla(ItemTypes.IRON_INGOT);
        registerVanilla(ItemTypes.GOLD_INGOT);
        registerVanilla(ItemTypes.IRON_SWORD);
        registerVanilla(ItemTypes.WOODEN_SWORD);
        registerVanilla(ItemTypes.WOODEN_SHOVEL);
        registerVanilla(ItemTypes.WOODEN_PICKAXE);
        registerVanilla(ItemTypes.WOODEN_AXE);
        registerVanilla(ItemTypes.STONE_SWORD);
        registerVanilla(ItemTypes.STONE_SHOVEL);
        registerVanilla(ItemTypes.STONE_PICKAXE);
        registerVanilla(ItemTypes.STONE_AXE);
        registerVanilla(ItemTypes.DIAMOND_SWORD);
        registerVanilla(ItemTypes.DIAMOND_SHOVEL);
        registerVanilla(ItemTypes.DIAMOND_PICKAXE);
        registerVanilla(ItemTypes.DIAMOND_AXE);
        registerVanilla(ItemTypes.STICK);
        registerVanilla(ItemTypes.BOWL);
        registerVanilla(ItemTypes.MUSHROOM_STEW);
        registerVanilla(ItemTypes.GOLDEN_SWORD);
        registerVanilla(ItemTypes.GOLDEN_SHOVEL);
        registerVanilla(ItemTypes.GOLDEN_PICKAXE);
        registerVanilla(ItemTypes.GOLDEN_AXE);
        registerVanilla(ItemTypes.STRING);
        registerVanilla(ItemTypes.FEATHER);
        registerVanilla(ItemTypes.GUNPOWDER);
        registerVanilla(ItemTypes.WOODEN_HOE);
        registerVanilla(ItemTypes.STONE_HOE);
        registerVanilla(ItemTypes.IRON_HOE);
        registerVanilla(ItemTypes.DIAMOND_HOE);
        registerVanilla(ItemTypes.GOLDEN_HOE);
        registerVanilla(ItemTypes.WHEAT_SEEDS);
        registerVanilla(ItemTypes.WHEAT);
        registerVanilla(ItemTypes.BREAD);
        registerVanilla(ItemTypes.LEATHER_HELMET);
        registerVanilla(ItemTypes.LEATHER_CHESTPLATE);
        registerVanilla(ItemTypes.LEATHER_LEGGINGS);
        registerVanilla(ItemTypes.LEATHER_BOOTS);
        registerVanilla(ItemTypes.CHAINMAIL_HELMET);
        registerVanilla(ItemTypes.CHAINMAIL_CHESTPLATE);
        registerVanilla(ItemTypes.CHAINMAIL_LEGGINGS);
        registerVanilla(ItemTypes.CHAINMAIL_BOOTS);
        registerVanilla(ItemTypes.IRON_HELMET);
        registerVanilla(ItemTypes.IRON_CHESTPLATE);
        registerVanilla(ItemTypes.IRON_LEGGINGS);
        registerVanilla(ItemTypes.IRON_BOOTS);
        registerVanilla(ItemTypes.DIAMOND_HELMET);
        registerVanilla(ItemTypes.DIAMOND_CHESTPLATE);
        registerVanilla(ItemTypes.DIAMOND_LEGGINGS);
        registerVanilla(ItemTypes.DIAMOND_BOOTS);
        registerVanilla(ItemTypes.GOLDEN_HELMET);
        registerVanilla(ItemTypes.GOLDEN_CHESTPLATE);
        registerVanilla(ItemTypes.GOLDEN_LEGGINGS);
        registerVanilla(ItemTypes.GOLDEN_BOOTS);
        registerVanilla(ItemTypes.FLINT);
        registerVanilla(ItemTypes.PORKCHOP);
        registerVanilla(ItemTypes.COOKED_PORKCHOP);
        registerVanilla(ItemTypes.PAINTING);
        registerVanilla(ItemTypes.GOLDEN_APPLE);
        registerVanilla(ItemTypes.SIGN, TreeSpeciesSerializer.SIGN);
        registerVanilla(ItemTypes.WOODEN_DOOR, TreeSpeciesSerializer.DOOR);
        registerVanilla(ItemTypes.BUCKET);

        registerVanilla(ItemTypes.MINECART);
        registerVanilla(ItemTypes.SADDLE);
        registerVanilla(ItemTypes.IRON_DOOR);
        registerVanilla(ItemTypes.REDSTONE);
        registerVanilla(ItemTypes.SNOWBALL);
        registerVanilla(ItemTypes.BOAT, TreeSpeciesSerializer.BOAT);
        registerVanilla(ItemTypes.LEATHER);
        registerVanilla(ItemTypes.KELP);
        registerVanilla(ItemTypes.BRICK);
        registerVanilla(ItemTypes.CLAY_BALL);
        registerVanilla(ItemTypes.REEDS);
        registerVanilla(ItemTypes.PAPER);
        registerVanilla(ItemTypes.BOOK);
        registerVanilla(ItemTypes.SLIME_BALL);
        registerVanilla(ItemTypes.CHEST_MINECART);

        registerVanilla(ItemTypes.EGG);
        registerVanilla(ItemTypes.COMPASS);
        registerVanilla(ItemTypes.FISHING_ROD);
        registerVanilla(ItemTypes.CLOCK);
        registerVanilla(ItemTypes.GLOWSTONE_DUST);
        registerVanilla(ItemTypes.FISH);
        registerVanilla(ItemTypes.COOKED_FISH);
        registerVanilla(ItemTypes.DYE, new DyeSerializer());
        registerVanilla(ItemTypes.BONE);
        registerVanilla(ItemTypes.SUGAR);
        registerVanilla(ItemTypes.CAKE);
        registerVanilla(ItemTypes.BED, EnumDamageSerializer.DYE_COLOR);
        registerVanilla(ItemTypes.REPEATER);
        registerVanilla(ItemTypes.COOKIE);
        registerVanilla(ItemTypes.MAP);
        registerVanilla(ItemTypes.SHEARS);
        registerVanilla(ItemTypes.MELON);
        registerVanilla(ItemTypes.PUMPKIN_SEEDS);
        registerVanilla(ItemTypes.MELON_SEEDS);
        registerVanilla(ItemTypes.BEEF);
        registerVanilla(ItemTypes.COOKED_BEEF);
        registerVanilla(ItemTypes.CHICKEN);
        registerVanilla(ItemTypes.COOKED_CHICKEN);
        registerVanilla(ItemTypes.ROTTEN_FLESH);
        registerVanilla(ItemTypes.ENDER_PEARL);
        registerVanilla(ItemTypes.BLAZE_ROD);
        registerVanilla(ItemTypes.GHAST_TEAR);
        registerVanilla(ItemTypes.GOLD_NUGGET);
        registerVanilla(ItemTypes.NETHER_WART);
        registerVanilla(ItemTypes.POTION);
        registerVanilla(ItemTypes.GLASS_BOTTLE);
        registerVanilla(ItemTypes.SPIDER_EYE);
        registerVanilla(ItemTypes.FERMENTED_SPIDER_EYE);
        registerVanilla(ItemTypes.BLAZE_POWDER);
        registerVanilla(ItemTypes.MAGMA_CREAM);
        registerVanilla(ItemTypes.BREWING_STAND);
        registerVanilla(ItemTypes.CAULDRON);
        registerVanilla(ItemTypes.ENDER_EYE);
        registerVanilla(ItemTypes.SPECKLED_MELON);
        registerVanilla(ItemTypes.SPAWN_EGG);
        registerVanilla(ItemTypes.EXPERIENCE_BOTTLE);
        registerVanilla(ItemTypes.FIREBALL);
        registerVanilla(ItemTypes.WRITABLE_BOOK);
        registerVanilla(ItemTypes.WRITTEN_BOOK);
        registerVanilla(ItemTypes.EMERALD);
        registerVanilla(ItemTypes.FRAME);
        registerVanilla(ItemTypes.FLOWER_POT);
        registerVanilla(ItemTypes.CARROT);
        registerVanilla(ItemTypes.POTATO);
        registerVanilla(ItemTypes.BAKED_POTATO);
        registerVanilla(ItemTypes.POISONOUS_POTATO);
        registerVanilla(ItemTypes.EMPTY_MAP);
        registerVanilla(ItemTypes.GOLDEN_CARROT);
        registerVanilla(ItemTypes.SKULL);
        registerVanilla(ItemTypes.CARROT_ON_A_STICK);
        registerVanilla(ItemTypes.NETHER_STAR);
        registerVanilla(ItemTypes.PUMPKIN_PIE);
        registerVanilla(ItemTypes.FIREWORKS);

        registerVanilla(ItemTypes.ENCHANTED_BOOK);
        registerVanilla(ItemTypes.COMPARATOR);
        registerVanilla(ItemTypes.NETHERBRICK);
        registerVanilla(ItemTypes.QUARTZ);
        registerVanilla(ItemTypes.TNT_MINECART);
        registerVanilla(ItemTypes.HOPPER_MINECART);
        registerVanilla(ItemTypes.PRISMARINE_SHARD);
        registerVanilla(ItemTypes.HOPPER);
        registerVanilla(ItemTypes.RABBIT);
        registerVanilla(ItemTypes.COOKED_RABBIT);
        registerVanilla(ItemTypes.RABBIT_STEW);
        registerVanilla(ItemTypes.RABBIT_FOOT);
        registerVanilla(ItemTypes.RABBIT_HIDE);
        registerVanilla(ItemTypes.HORSE_ARMOR_LEATHER);
        registerVanilla(ItemTypes.HORSE_ARMOR_IRON);
        registerVanilla(ItemTypes.HORSE_ARMOR_GOLD);
        registerVanilla(ItemTypes.HORSE_ARMOR_DIAMOND);
        registerVanilla(ItemTypes.LEAD);
        registerVanilla(ItemTypes.NAME_TAG);
        registerVanilla(ItemTypes.PRISMARINE_CRYSTALS);
        registerVanilla(ItemTypes.MUTTON_RAW);
        registerVanilla(ItemTypes.MUTTON_COOKED);

        registerVanilla(ItemTypes.ARMOR_STAND);
        registerVanilla(ItemTypes.END_CRYSTAL);
        registerVanilla(ItemTypes.CHORUS_FRUIT);
        registerVanilla(ItemTypes.CHORUS_FRUIT_POPPED);

        registerVanilla(ItemTypes.DRAGON_BREATH);
        registerVanilla(ItemTypes.SPLASH_POTION);

        registerVanilla(ItemTypes.LINGERING_POTION);

        registerVanilla(ItemTypes.COMMAND_BLOCK_MINECART);
        registerVanilla(ItemTypes.ELYTRA);

        registerVanilla(ItemTypes.SHULKER_SHELL);
        registerVanilla(ItemTypes.BANNER, new BannerSerializer());
        registerVanilla(ItemTypes.BANNER_PATTERN);

        registerVanilla(ItemTypes.IRON_NUGGET);
        registerVanilla(ItemTypes.NAUTILUS_SHELL);
        registerVanilla(ItemTypes.TRIDENT);

        registerVanilla(ItemTypes.BEETROOT);
        registerVanilla(ItemTypes.BEETROOT_SEEDS);
        registerVanilla(ItemTypes.BEETROOT_SOUP);
        registerVanilla(ItemTypes.SALMON);
        registerVanilla(ItemTypes.CLOWNFISH);
        registerVanilla(ItemTypes.PUFFERFISH);
        registerVanilla(ItemTypes.COOKED_SALMON);
        registerVanilla(ItemTypes.DRIED_KELP);

        registerVanilla(ItemTypes.APPLE_ENCHANTED);

        registerVanilla(ItemTypes.TURTLE_HELMET);
        registerVanilla(ItemTypes.SWEET_BERRIES);

        registerVanilla(ItemTypes.RECORD, new RecordSerializer());

        registerVanilla(ItemTypes.SHIELD);

        registerVanilla(ItemTypes.CAMPFIRE);

        registerVanilla(ItemTypes.HONEYCOMB);
        registerVanilla(ItemTypes.HONEY_BOTTLE);

        registerVanilla(ItemTypes.NETHERITE_SWORD);
        registerVanilla(ItemTypes.NETHERITE_SHOVEL);
        registerVanilla(ItemTypes.NETHERITE_PICKAXE);
        registerVanilla(ItemTypes.NETHERITE_AXE);
        registerVanilla(ItemTypes.NETHERITE_HOE);
        registerVanilla(ItemTypes.NETHERITE_HELMET);
        registerVanilla(ItemTypes.NETHERITE_CHESTPLATE);
        registerVanilla(ItemTypes.NETHERITE_LEGGINGS);
        registerVanilla(ItemTypes.NETHERITE_BOOTS);
        registerVanilla(ItemTypes.NETHERITE_INGOT);
        registerVanilla(ItemTypes.NETHERITE_SCRAP);

        registerVanilla(ItemTypes.CONCRETE_POWDER);

        registerVanilla(ItemTypes.UNKNOWN);

        registerVanilla(ItemTypes.AMETHYST_SHARD);
        registerVanilla(ItemTypes.GLOW_BERRIES);
        registerVanilla(ItemTypes.GLOW_INK_SAC);
        registerVanilla(ItemTypes.GOAT_HORN);
        registerVanilla(ItemTypes.GLOW_FRAME);
        registerVanilla(ItemTypes.COPPER_INGOT);
        registerVanilla(ItemTypes.RAW_COPPER);
        registerVanilla(ItemTypes.RAW_GOLD);
        registerVanilla(ItemTypes.RAW_IRON);
        registerVanilla(ItemTypes.SPYGLASS);
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

    /**
     * register additional vanilla identifiers not used as a separate ItemType
     */
    private void registerVanillaIdentifiers() {
        registerType(ItemTypes.SIGN, ItemIds.SPRUCE_SIGN);
        registerType(ItemTypes.SIGN, ItemIds.BIRCH_SIGN);
        registerType(ItemTypes.SIGN, ItemIds.JUNGLE_SIGN);
        registerType(ItemTypes.SIGN, ItemIds.ACACIA_SIGN);
        registerType(ItemTypes.SIGN, ItemIds.DARK_OAK_SIGN);
        registerType(ItemTypes.SIGN, ItemIds.CRIMSON_SIGN);
        registerType(ItemTypes.SIGN, ItemIds.WARPED_SIGN);

        registerType(ItemTypes.WOODEN_DOOR, ItemIds.SPRUCE_DOOR);
        registerType(ItemTypes.WOODEN_DOOR, ItemIds.BIRCH_DOOR);
        registerType(ItemTypes.WOODEN_DOOR, ItemIds.JUNGLE_DOOR);
        registerType(ItemTypes.WOODEN_DOOR, ItemIds.ACACIA_DOOR);
        registerType(ItemTypes.WOODEN_DOOR, ItemIds.DARK_OAK_DOOR);
        registerType(ItemTypes.WOODEN_DOOR, ItemIds.CRIMSON_DOOR);
        registerType(ItemTypes.WOODEN_DOOR, ItemIds.WARPED_DOOR);

        registerType(ItemTypes.RECORD, ItemIds.RECORD_BLOCKS);
        registerType(ItemTypes.RECORD, ItemIds.RECORD_CHIRP);
        registerType(ItemTypes.RECORD, ItemIds.RECORD_FAR);
        registerType(ItemTypes.RECORD, ItemIds.RECORD_MALL);
        registerType(ItemTypes.RECORD, ItemIds.RECORD_MELLOHI);
        registerType(ItemTypes.RECORD, ItemIds.RECORD_STAL);
        registerType(ItemTypes.RECORD, ItemIds.RECORD_STRAD);
        registerType(ItemTypes.RECORD, ItemIds.RECORD_WARD);
        registerType(ItemTypes.RECORD, ItemIds.RECORD_11);
        registerType(ItemTypes.RECORD, ItemIds.RECORD_WAIT);
        registerType(ItemTypes.RECORD, ItemIds.RECORD_5);

        registerType(ItemTypes.COAL, ItemIds.CHARCOAL);

        registerType(ItemTypes.DYE, ItemIds.RED_DYE);
        registerType(ItemTypes.DYE, ItemIds.GREEN_DYE);
        registerType(ItemTypes.DYE, ItemIds.COCOA_BEANS);
        registerType(ItemTypes.DYE, ItemIds.LAPIS_LAZULI);
        registerType(ItemTypes.DYE, ItemIds.PURPLE_DYE);
        registerType(ItemTypes.DYE, ItemIds.CYAN_DYE);
        registerType(ItemTypes.DYE, ItemIds.LIGHT_GRAY_DYE);
        registerType(ItemTypes.DYE, ItemIds.GRAY_DYE);
        registerType(ItemTypes.DYE, ItemIds.PINK_DYE);
        registerType(ItemTypes.DYE, ItemIds.LIME_DYE);
        registerType(ItemTypes.DYE, ItemIds.YELLOW_DYE);
        registerType(ItemTypes.DYE, ItemIds.LIGHT_BLUE_DYE);
        registerType(ItemTypes.DYE, ItemIds.MAGENTA_DYE);
        registerType(ItemTypes.DYE, ItemIds.ORANGE_DYE);
        registerType(ItemTypes.DYE, ItemIds.BONE_MEAL);
        registerType(ItemTypes.DYE, ItemIds.BLACK_DYE);
        registerType(ItemTypes.DYE, ItemIds.BROWN_DYE);
        registerType(ItemTypes.DYE, ItemIds.WHITE_DYE);
        registerType(ItemTypes.DYE, ItemIds.BLUE_DYE);

        registerType(ItemTypes.BOAT, ItemIds.OAK_BOAT);
        registerType(ItemTypes.BOAT, ItemIds.SPRUCE_BOAT);
        registerType(ItemTypes.BOAT, ItemIds.BIRCH_BOAT);
        registerType(ItemTypes.BOAT, ItemIds.JUNGLE_BOAT);
        registerType(ItemTypes.BOAT, ItemIds.ACACIA_BOAT);
        registerType(ItemTypes.BOAT, ItemIds.DARK_OAK_BOAT);

        registerType(ItemTypes.BANNER_PATTERN, ItemIds.CREEPER_BANNER_PATTERN);
        registerType(ItemTypes.BANNER_PATTERN, ItemIds.SKULL_BANNER_PATTERN);
        registerType(ItemTypes.BANNER_PATTERN, ItemIds.FLOWER_BANNER_PATTERN);
        registerType(ItemTypes.BANNER_PATTERN, ItemIds.MOJANG_BANNER_PATTERN);
        registerType(ItemTypes.BANNER_PATTERN, ItemIds.FIELD_MASONED_BANNER_PATTERN);
        registerType(ItemTypes.BANNER_PATTERN, ItemIds.BORDURE_INDENTED_BANNER_PATTERN);
        registerType(ItemTypes.BANNER_PATTERN, ItemIds.PIGLIN_BANNER_PATTERN);

        registerType(ItemTypes.SPAWN_EGG, ItemIds.CHICKEN_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.BEE_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.COW_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.PIG_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.SHEEP_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.WOLF_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.POLAR_BEAR_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.OCELOT_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.CAT_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.MOOSHROOM_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.BAT_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.PARROT_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.RABBIT_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.LLAMA_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.HORSE_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.DONKEY_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.MULE_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.SKELETON_HORSE_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.ZOMBIE_HORSE_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.TROPICAL_FISH_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.COD_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.PUFFERFISH_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.SALMON_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.DOLPHIN_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.TURTLE_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.PANDA_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.FOX_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.CREEPER_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.ENDERMAN_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.SILVERFISH_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.SKELETON_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.WITHER_SKELETON_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.STRAY_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.SLIME_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.SPIDER_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.ZOMBIE_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.ZOMBIE_PIGMAN_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.HUSK_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.DROWNED_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.SQUID_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.CAVE_SPIDER_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.WITCH_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.GUARDIAN_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.ELDER_GUARDIAN_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.ENDERMITE_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.MAGMA_CUBE_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.STRIDER_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.HOGLIN_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.PIGLIN_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.ZOGLIN_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.PIGLIN_BRUTE_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.GHAST_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.BLAZE_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.SHULKER_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.VINDICATOR_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.EVOKER_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.VEX_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.VILLAGER_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.WANDERING_TRADER_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.ZOMBIE_VILLAGER_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.PHANTOM_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.PILLAGER_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.RAVAGER_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.AXOLOTL_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.GLOW_SQUID_SPAWN_EGG);
        registerType(ItemTypes.SPAWN_EGG, ItemIds.GOAT_SPAWN_EGG);

        registerType(ItemTypes.BUCKET, ItemIds.MILK_BUCKET);
        registerType(ItemTypes.BUCKET, ItemIds.COD_BUCKET);
        registerType(ItemTypes.BUCKET, ItemIds.SALMON_BUCKET);
        registerType(ItemTypes.BUCKET, ItemIds.TROPICAL_FISH_BUCKET);
        registerType(ItemTypes.BUCKET, ItemIds.PUFFERFISH_BUCKET);
        registerType(ItemTypes.BUCKET, ItemIds.WATER_BUCKET);
        registerType(ItemTypes.BUCKET, ItemIds.LAVA_BUCKET);
        registerType(ItemTypes.BUCKET, ItemIds.AXOLOTL_BUCKET);
        registerType(ItemTypes.BUCKET, ItemIds.POWDER_SNOW_BUCKET);

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
        return ItemUtils.fromNetwork(itemPalette.getCreativeItems().get(index));
    }

    public CreativeContentPacket getCreativeContent() {
        return itemPalette.getCreativeContentPacket();
    }

    public int getCreativeItemIndex(ItemStack item) {
        int rid = itemPalette.getRuntimeId(item.getType().getId());

        for (int i = 0; i < itemPalette.getCreativeItems().size(); i++) {
            if (rid == itemPalette.getCreativeItems().get(i).getDefinition().getRuntimeId()) {
                return i;
            }
        }

        return -1;
    }
}
