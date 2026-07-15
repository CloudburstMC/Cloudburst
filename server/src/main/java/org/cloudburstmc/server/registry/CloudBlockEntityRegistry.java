package org.cloudburstmc.server.registry;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.blockentity.BlockEntity;
import org.cloudburstmc.api.blockentity.BlockEntityFactory;
import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.blockentity.BlockEntityTypes;
import org.cloudburstmc.api.registry.BlockEntityRegistry;
import org.cloudburstmc.api.registry.RegistryException;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.blockentity.*;
import org.cloudburstmc.server.level.chunk.CloudChunk;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static com.google.common.base.Preconditions.*;

/**
 * Singleton registry that maps {@link BlockEntityType} tokens to their
 * {@link BlockEntityFactory} implementations. Supports registration of custom factories by plugins
 * and is used to create block entity instances by type or from persisted NBT data.
 */
public final class CloudBlockEntityRegistry implements BlockEntityRegistry {
    private static final CloudBlockEntityRegistry INSTANCE = new CloudBlockEntityRegistry();
    private static final RegistryServiceProvider<BlockEntityFactory<?>> UNKNOWN_PROVIDER = new RegistryServiceProvider<>(new RegistryProvider<>(UnknownBlockEntity::new, null, 1000));

    private final Map<BlockEntityType<?>, RegistryServiceProvider<BlockEntityFactory<?>>> providers = new IdentityHashMap<>();
    private final BiMap<BlockEntityType<?>, String> persistentMap = HashBiMap.create();
    private final Map<BlockEntityType<?>, Set<BlockType>> validBlocks = new IdentityHashMap<>();
    private final Map<String, BlockEntityType<?>> unknownTypes = new java.util.HashMap<>();
    private final Map<BlockEntityType<?>, String> unknownPersistentIds = new IdentityHashMap<>();
    private final AtomicLong unknownTypeIds = new AtomicLong();

    private volatile boolean closed;

    private CloudBlockEntityRegistry() {
        this.registerVanillaEntities();
    }

    public static CloudBlockEntityRegistry get() {
        return INSTANCE;
    }

    private <T extends BlockEntity> void registerVanilla(BlockEntityType<T> type, BlockEntityFactory<? extends T> factory, String persistentId) {
        checkNotNull(type, "type");
        checkNotNull(factory, "factory");
        checkNotNull(persistentId, "persistentId");

        this.persistentMap.put(type, persistentId);
        this.providers.put(type, new RegistryServiceProvider<>(new RegistryProvider<>(factory, null, 1000)));
        this.validBlocks.put(type, VanillaBlockEntityBlocks.get(type));
    }

    @Override
    public synchronized <T extends BlockEntity> void register(BlockEntityType<T> type,
                                                              BlockEntityFactory<T> factory,
                                                              String persistentId,
                                                              Set<BlockType> validBlocks) throws RegistryException {
        checkClosed();
        checkNotNull(type, "type");
        checkNotNull(factory, "factory");
        checkNotNull(persistentId, "persistentId");
        checkNotNull(validBlocks, "validBlocks");
        checkArgument(!type.getIdentifier().getNamespace().equals("minecraft"),
                "Custom block entity types must use a non-minecraft namespace: %s", type.getIdentifier());
        checkArgument(!persistentId.isBlank(), "persistentId cannot be blank");
        checkArgument(!validBlocks.isEmpty(), "validBlocks cannot be empty");
        checkArgument(!this.providers.containsKey(type), "Block entity type is already registered: %s", type);
        checkArgument(!this.persistentMap.containsValue(persistentId), "Persistent ID is already registered: %s", persistentId);

        this.persistentMap.put(type, persistentId);
        this.providers.put(type, new RegistryServiceProvider<>(new RegistryProvider<>(factory, null, 0)));
        this.validBlocks.put(type, Set.copyOf(validBlocks));
    }

    public String getPersistentId(BlockEntityType<?> type) {
        String persistentId = this.persistentMap.get(type);
        return persistentId != null ? persistentId : this.unknownPersistentIds.get(type);
    }

    @NonNull
    public synchronized BlockEntityType<?> getBlockEntityType(String persistentId) {
        BlockEntityType<?> registered = this.persistentMap.inverse().get(persistentId);
        if (registered != null) {
            return registered;
        }
        return this.unknownTypes.computeIfAbsent(persistentId, id -> {
            BlockEntityType<UnknownBlockEntity> unknown = BlockEntityType.from(
                    org.cloudburstmc.api.util.Identifier.from("cloudburst", "unknown_block_entity_" + this.unknownTypeIds.getAndIncrement()),
                    UnknownBlockEntity.class);
            this.unknownPersistentIds.put(unknown, id);
            return unknown;
        });
    }

    @Override
    public boolean isValid(BlockEntityType<?> type, BlockState state) {
        Set<BlockType> blocks = this.validBlocks.get(type);
        return blocks != null
                ? blocks.contains(state.getType())
                : type.getBlockEntityClass() == UnknownBlockEntity.class;
    }

    public <T extends BlockEntity> T newEntity(BlockEntityType<T> type, Block block) {
        return newEntity(type, (CloudChunk) block.getChunk(), block.getPosition());
    }

    /**
     * Creates new entity of given type
     *
     * @param type     entity type
     * @param chunk    chunk of block entity
     * @param position position of block entity in world
     * @param <T>      entity class type
     * @return new entity
     */
    public <T extends BlockEntity> T newEntity(BlockEntityType<T> type, CloudChunk chunk, Vector3i position) {
        checkState(closed, "Cannot create entity till registry is closed");
        checkNotNull(type, "type");
        checkNotNull(chunk, "chunk");
        checkNotNull(position, "position");
        BlockEntityFactory<T> factory = getServiceProvider(type).getProvider().getValue();
        return factory.create(type, chunk, position);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T extends BlockEntity> RegistryServiceProvider<BlockEntityFactory<T>> getServiceProvider(BlockEntityType<T> type) {
        RegistryServiceProvider<BlockEntityFactory<T>> service = (RegistryServiceProvider) this.providers.get(type);
        if (service == null) {
            if (type.getBlockEntityClass() != UnknownBlockEntity.class) {
                throw new RegistryException(type.getIdentifier() + " is not a registered entity");
            }

            service = (RegistryServiceProvider) UNKNOWN_PROVIDER;
        }
        return service;
    }

    public synchronized void close() throws RegistryException {
        checkClosed();

        // Bake registry providers
        this.providers.values().forEach(RegistryServiceProvider::bake);

        this.closed = true;
    }

    private void checkClosed() {
        checkState(!closed, "Registration is already closed");
    }

    private void registerVanillaEntities() {
        registerVanilla(BlockEntityTypes.BANNER, BannerBlockEntity::new, "Banner");
        registerVanilla(BlockEntityTypes.BARREL, BarrelBlockEntity::new, "Barrel");
        registerVanilla(BlockEntityTypes.BEACON, BeaconBlockEntity::new, "Beacon");
        registerVanilla(BlockEntityTypes.BED, BedBlockEntity::new, "Bed");
        registerVanilla(BlockEntityTypes.BLAST_FURNACE, BlastFurnaceBlockEntity::new, "BlastFurnace");
        registerVanilla(BlockEntityTypes.BREWING_STAND, BrewingStandBlockEntity::new, "BrewingStand");
        registerVanilla(BlockEntityTypes.CAMPFIRE, CampfireBlockEntity::new, "Campfire");
        registerVanilla(BlockEntityTypes.CAULDRON, CauldronBlockEntity::new, "Cauldron");
        registerVanilla(BlockEntityTypes.CHEST, ChestBlockEntity::new, "Chest");
        registerVanilla(BlockEntityTypes.COMPARATOR, ComparatorBlockEntity::new, "Comparator");
        registerVanilla(BlockEntityTypes.CRAFTER, CrafterBlockEntity::new, "Crafter");
        registerVanilla(BlockEntityTypes.DAYLIGHT_DETECTOR, DaylightDetectorBlockEntity::new, "DaylightDetector");
        registerVanilla(BlockEntityTypes.DISPENSER, DispenserBlockEntity::new, "Dispenser");
        registerVanilla(BlockEntityTypes.DROPPER, DropperBlockEntity::new, "Dropper");
        registerVanilla(BlockEntityTypes.ENCHANTING_TABLE, EnchantingTableBlockEntity::new, "EnchantTable");
        registerVanilla(BlockEntityTypes.ENDER_CHEST, EnderChestBlockEntity::new, "EnderChest");
        registerVanilla(BlockEntityTypes.FLOWER_POT, FlowerPotBlockEntity::new, "FlowerPot");
        registerVanilla(BlockEntityTypes.FURNACE, FurnaceBlockEntity::new, "Furnace");
        registerVanilla(BlockEntityTypes.HOPPER, HopperBlockEntity::new, "Hopper");
        registerVanilla(BlockEntityTypes.ITEM_FRAME, ItemFrameBlockEntity::new, "ItemFrame");
        registerVanilla(BlockEntityTypes.JUKEBOX, JukeboxBlockEntity::new, "Jukebox");
        registerVanilla(BlockEntityTypes.LECTERN, LecternBlockEntity::new, "Lectern");
        registerVanilla(BlockEntityTypes.MOVING_BLOCK, MovingBlockEntity::new, "MovingBlock");
        registerVanilla(BlockEntityTypes.NOTEBLOCK, MusicBlockEntity::new, "Music");
        registerVanilla(BlockEntityTypes.PISTON, PistonBlockEntity::new, "PistonArm");
        registerVanilla(BlockEntityTypes.SHULKER_BOX, ShulkerBoxBlockEntity::new, "ShulkerBox");
        registerVanilla(BlockEntityTypes.SIGN, SignBlockEntity::new, "Sign");
        registerVanilla(BlockEntityTypes.SKULL, SkullBlockEntity::new, "Skull");
        registerVanilla(BlockEntityTypes.SMOKER, SmokerBlockEntity::new, "Smoker");
    }
}
