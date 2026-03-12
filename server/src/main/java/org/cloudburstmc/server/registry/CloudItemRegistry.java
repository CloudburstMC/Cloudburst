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
import org.cloudburstmc.api.data.DataKey;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.item.*;
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
import org.cloudburstmc.server.item.component.FireChargeItemHandlers;
import org.cloudburstmc.server.item.component.FlintAndSteelItemHandlers;
import org.cloudburstmc.server.item.component.SpawnEggItemHandlers;
import org.cloudburstmc.server.item.data.serializer.*;
import org.cloudburstmc.server.item.serializer.BannerSerializer;
import org.cloudburstmc.server.item.serializer.DefaultItemSerializer;
import org.cloudburstmc.server.item.serializer.ItemSerializer;
import org.cloudburstmc.server.registry.component.CloudComponentMap;

import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Log4j2
public class CloudItemRegistry extends CloudComponentRegistry<ItemType> implements ItemRegistry, DefinitionRegistry<CloudItemDefinition> {
    private static final CloudItemRegistry INSTANCE = new CloudItemRegistry(); // Needs to be initialized afterwards

    private final Reference2ReferenceMap<Identifier, ItemType> typeMap = new Reference2ReferenceOpenHashMap<>();
    private final Reference2ObjectMap<ItemType, ItemSerializer> serializers = new Reference2ObjectOpenHashMap<>();
    private final Reference2ObjectMap<DataKey<?, ?>, ItemDataSerializer<?>> dataSerializers = new Reference2ObjectOpenHashMap<>();
    private final ItemPalette itemPalette = new ItemPalette(this);
    private int hardcodedBlockingId;
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
    public ComponentMap getComponents(ItemType type) {
        return super.getComponents(type);
    }

    public int getHardcodedBlockingId() {
        return this.hardcodedBlockingId;
    }

    protected synchronized CloudComponentMap registerVanilla(ItemType type) throws RegistryException {
        return registerVanilla(type, null);
    }

    private synchronized CloudComponentMap registerVanilla(ItemType type, ItemSerializer serializer) throws RegistryException {
        Objects.requireNonNull(type, "type");
        checkClosed();

        if (serializer != null) {
            this.serializers.put(type, serializer);
        }

        CloudComponentMap collection = new CloudComponentMap(this);
//        collection.apply(DefaultBlockBehaviours.BLOCK_BEHAVIOR_BASE);

        collection.bake();
        putComponents(type, collection);
        this.registerType(type, type.getId());
        return collection;
    }

    protected void registerBlock(BlockType type) {
        CloudComponentMap collection = new CloudComponentMap(this);
//        collection.apply(DefaultBlockBehaviours.BLOCK_BEHAVIOR_BASE);

        BlockState defaultState = type.getDefaultState();
        collection.set(ItemComponents.GET_BLOCK, (item) -> {
            BlockState state = item.get(ItemKeys.BLOCK_STATE);
            return Optional.of(state != null ? state : defaultState);
        });

        collection.bake();

        ItemType itemType = ItemType.of(type.getId());
        type.linkItemType(itemType);
        this.typeMap.put(type.getId(), itemType);

        putComponents(itemType, collection);
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
        registerVanilla(ItemTypes.AXOLOTL_BUCKET);
        registerVanilla(ItemTypes.AXOLOTL_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.AXOLOTL));
        registerVanilla(ItemTypes.BAKED_POTATO);
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
        registerVanilla(ItemTypes.BLACK_DYE);
        registerVanilla(ItemTypes.BLACK_HARNESS);
        registerVanilla(ItemTypes.BLADE_POTTERY_SHERD);
        registerVanilla(ItemTypes.BLAZE_POWDER);
        registerVanilla(ItemTypes.BLAZE_ROD);
        registerVanilla(ItemTypes.BLAZE_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.BLAZE));
        registerVanilla(ItemTypes.BLUE_BUNDLE);
        registerVanilla(ItemTypes.BLUE_DYE);
        registerVanilla(ItemTypes.BLUE_EGG);
        registerVanilla(ItemTypes.BLUE_HARNESS);
        registerVanilla(ItemTypes.BOGGED_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.BOGGED));
        registerVanilla(ItemTypes.BOLT_ARMOR_TRIM_SMITHING_TEMPLATE);
        registerVanilla(ItemTypes.BONE);
        registerVanilla(ItemTypes.BONE_MEAL);
        registerVanilla(ItemTypes.BOOK);
        registerVanilla(ItemTypes.BORDURE_INDENTED_BANNER_PATTERN);
        registerVanilla(ItemTypes.BOW);
        registerVanilla(ItemTypes.BOWL);
        registerVanilla(ItemTypes.BREAD);
        registerVanilla(ItemTypes.BREEZE_ROD);
        registerVanilla(ItemTypes.BREEZE_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.BREEZE));
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
        registerVanilla(ItemTypes.CAMEL_HUSK_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.CAMEL_HUSK));
        registerVanilla(ItemTypes.CAMEL_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.CAMEL));
        registerVanilla(ItemTypes.CARROT);
        registerVanilla(ItemTypes.CARROT_ON_A_STICK);
        registerVanilla(ItemTypes.CAT_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.CAT));
        registerVanilla(ItemTypes.CAVE_SPIDER_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.CAVE_SPIDER));
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
        registerVanilla(ItemTypes.CHICKEN_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.CHICKEN));
        registerVanilla(ItemTypes.CHORUS_FRUIT);
        registerVanilla(ItemTypes.CLAY_BALL);
        registerVanilla(ItemTypes.CLOCK);
        registerVanilla(ItemTypes.COAL);
        registerVanilla(ItemTypes.COAST_ARMOR_TRIM_SMITHING_TEMPLATE);
        registerVanilla(ItemTypes.COCOA_BEANS);
        registerVanilla(ItemTypes.COD);
        registerVanilla(ItemTypes.COD_BUCKET);
        registerVanilla(ItemTypes.COD_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.COD));
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
        registerVanilla(ItemTypes.COPPER_GOLEM_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.COPPER_GOLEM));
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
        registerVanilla(ItemTypes.COW_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.COW));
        registerVanilla(ItemTypes.CREAKING_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.CREAKING));
        registerVanilla(ItemTypes.CREEPER_BANNER_PATTERN);
        registerVanilla(ItemTypes.CREEPER_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.CREEPER));
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
        registerVanilla(ItemTypes.ELYTRA);
        registerVanilla(ItemTypes.EMERALD);
        registerVanilla(ItemTypes.EMPTY_MAP);
        registerVanilla(ItemTypes.ENCHANTED_BOOK);
        registerVanilla(ItemTypes.ENCHANTED_GOLDEN_APPLE);
        registerVanilla(ItemTypes.END_CRYSTAL);
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
        registerVanilla(ItemTypes.FENCE_GATE);
        registerVanilla(ItemTypes.FERMENTED_SPIDER_EYE);
        registerVanilla(ItemTypes.FIELD_MASONED_BANNER_PATTERN);
        registerVanilla(ItemTypes.FILLED_MAP);
        registerVanilla(ItemTypes.FIRE_CHARGE)
                .set(ItemComponents.USE_ON, FireChargeItemHandlers.USE_ON);
        registerVanilla(ItemTypes.FIREWORK_ROCKET);
        registerVanilla(ItemTypes.FIREWORK_STAR);
        registerVanilla(ItemTypes.FISHING_ROD);
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
        registerVanilla(ItemTypes.GLOWSTONE_DUST);
        registerVanilla(ItemTypes.GOAT_HORN);
        registerVanilla(ItemTypes.GOAT_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.GOAT));
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
        registerVanilla(ItemTypes.HOPPER_MINECART);
        registerVanilla(ItemTypes.HORSE_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.HORSE));
        registerVanilla(ItemTypes.HOST_ARMOR_TRIM_SMITHING_TEMPLATE);
        registerVanilla(ItemTypes.HOWL_POTTERY_SHERD);
        registerVanilla(ItemTypes.HUSK_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.HUSK));
        registerVanilla(ItemTypes.INK_SAC);
        registerVanilla(ItemTypes.IRON_AXE);
        registerVanilla(ItemTypes.IRON_BOOTS);
        registerVanilla(ItemTypes.IRON_CHESTPLATE);
        registerVanilla(ItemTypes.IRON_GOLEM_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.IRON_GOLEM));
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
        registerVanilla(ItemTypes.LLAMA_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.LLAMA));
        registerVanilla(ItemTypes.MACE);
        registerVanilla(ItemTypes.MAGENTA_BUNDLE);
        registerVanilla(ItemTypes.MAGENTA_DYE);
        registerVanilla(ItemTypes.MAGENTA_HARNESS);
        registerVanilla(ItemTypes.MAGMA_CREAM);
        registerVanilla(ItemTypes.MAGMA_CUBE_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.MAGMA_CUBE));
        registerVanilla(ItemTypes.MANGROVE_BOAT);
        registerVanilla(ItemTypes.MANGROVE_CHEST_BOAT);
        registerVanilla(ItemTypes.MANGROVE_SIGN);
        registerVanilla(ItemTypes.MELON_SEEDS);
        registerVanilla(ItemTypes.MELON_SLICE);
        registerVanilla(ItemTypes.MILK_BUCKET);
        registerVanilla(ItemTypes.MINECART);
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
        registerVanilla(ItemTypes.OCELOT_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.OCELOT));
        registerVanilla(ItemTypes.OMINOUS_BOTTLE);
        registerVanilla(ItemTypes.OMINOUS_TRIAL_KEY);
        registerVanilla(ItemTypes.ORANGE_BUNDLE);
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
        registerVanilla(ItemTypes.PINK_DYE);
        registerVanilla(ItemTypes.PINK_HARNESS);
        registerVanilla(ItemTypes.PITCHER_POD);
        registerVanilla(ItemTypes.PLENTY_POTTERY_SHERD);
        registerVanilla(ItemTypes.POISONOUS_POTATO);
        registerVanilla(ItemTypes.POLAR_BEAR_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.POLAR_BEAR));
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
        registerVanilla(ItemTypes.PUFFERFISH_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.PUFFERFISH));
        registerVanilla(ItemTypes.PUMPKIN_PIE);
        registerVanilla(ItemTypes.PUMPKIN_SEEDS);
        registerVanilla(ItemTypes.PURPLE_BUNDLE);
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
        registerVanilla(ItemTypes.RAVAGER_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.RAVAGER));
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
        registerVanilla(ItemTypes.SALMON_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.SALMON));
        registerVanilla(ItemTypes.SCRAPE_POTTERY_SHERD);
        registerVanilla(ItemTypes.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE);
        registerVanilla(ItemTypes.SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE);
        registerVanilla(ItemTypes.SHEAF_POTTERY_SHERD);
        registerVanilla(ItemTypes.SHEARS);
        registerVanilla(ItemTypes.SHEEP_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.SHEEP));
        registerVanilla(ItemTypes.SHELTER_POTTERY_SHERD);
        registerVanilla(ItemTypes.SHIELD);
        registerVanilla(ItemTypes.SHULKER_SHELL);
        registerVanilla(ItemTypes.SHULKER_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.SHULKER));
        registerVanilla(ItemTypes.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE);
        registerVanilla(ItemTypes.SILVER_GLAZED_TERRACOTTA);
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
        registerVanilla(ItemTypes.STONE_AXE);
        registerVanilla(ItemTypes.STONE_HOE);
        registerVanilla(ItemTypes.STONE_PICKAXE);
        registerVanilla(ItemTypes.STONE_SHOVEL);
        registerVanilla(ItemTypes.STONE_SPEAR);
        registerVanilla(ItemTypes.STONE_SWORD);
        registerVanilla(ItemTypes.STRAY_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.STRAY));
        registerVanilla(ItemTypes.STRIDER_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.STRIDER));
        registerVanilla(ItemTypes.STRING);
        registerVanilla(ItemTypes.SUGAR);
        registerVanilla(ItemTypes.SUGAR_CANE);
        registerVanilla(ItemTypes.SUSPICIOUS_STEW);
        registerVanilla(ItemTypes.SWEET_BERRIES);
        registerVanilla(ItemTypes.TADPOLE_BUCKET);
        registerVanilla(ItemTypes.TADPOLE_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.TADPOLE));
        registerVanilla(ItemTypes.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE);
        registerVanilla(ItemTypes.TNT_MINECART);
        registerVanilla(ItemTypes.TORCHFLOWER_SEEDS);
        registerVanilla(ItemTypes.TOTEM_OF_UNDYING);
        registerVanilla(ItemTypes.TRADER_LLAMA_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.TRADER_LLAMA));
        registerVanilla(ItemTypes.TRAPDOOR);
        registerVanilla(ItemTypes.TRIAL_KEY);
        registerVanilla(ItemTypes.TRIDENT);
        registerVanilla(ItemTypes.TROPICAL_FISH);
        registerVanilla(ItemTypes.TROPICAL_FISH_BUCKET);
        registerVanilla(ItemTypes.TROPICAL_FISH_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.TROPICAL_FISH));
        registerVanilla(ItemTypes.TURTLE_HELMET);
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
        registerVanilla(ItemTypes.WITCH_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.WITCH));
        registerVanilla(ItemTypes.WITHER_SKELETON_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.WITHER_SKELETON));
        registerVanilla(ItemTypes.WOLF_ARMOR);
        registerVanilla(ItemTypes.WOLF_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.WOLF));
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
        this.registerDataSerializer(ItemKeys.SPAWN_EGG_TYPE, new EntityTypeSerializer());
    }

    private void registerVanillaBehaviors() {
        this.registerComponent(ItemComponents.ALLOW_OFFHAND, () -> false);
        this.registerComponent(ItemComponents.CAN_BE_CHARGED, () -> false);
        this.registerComponent(ItemComponents.CAN_BE_DEPLETED, () -> false);
        this.registerComponent(ItemComponents.CAN_BE_PLACED, (item) -> false);
        this.registerComponent(ItemComponents.CAN_BE_PLACED_ON, (item, block) -> true);
        this.registerComponent(ItemComponents.CAN_DESTROY, (item, block) -> true);
        this.registerComponent(ItemComponents.CAN_DESTROY_IN_CREATIVE, () -> true);
        this.registerComponent(ItemComponents.DAMAGEABLE, () -> false);
        this.registerComponent(ItemComponents.FUEL_DURATION, () -> 0f);
        this.registerComponent(ItemComponents.GET_ATTACK_DAMAGE_BONUS, (item) -> 0f);
        this.registerComponent(ItemComponents.GET_BLOCK, (item) -> Optional.empty());
        this.registerComponent(ItemComponents.GET_DAMAGE_CHANCE, (unbreaking) -> 0);
        this.registerComponent(ItemComponents.GET_DESTROY_SPEED, (item, block) -> 1);
        this.registerComponent(ItemComponents.GET_DESTROY_SPEED_BONUS, (item) -> 0);
        this.registerComponent(ItemComponents.GET_MAX_DAMAGE, (item) -> 0);
        this.registerComponent(ItemComponents.GET_MAX_STACK_SIZE, (item) -> 64);
        this.registerComponent(ItemComponents.IS_TOOL, (item) -> false);
        this.registerComponent(ItemComponents.MINE_BLOCK, (item, block, owner) -> item);
        this.registerComponent(ItemComponents.ON_DAMAGE, (item, damage, owner) -> item);
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
