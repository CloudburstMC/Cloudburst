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
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.blockentity.*;
import org.cloudburstmc.server.blockentity.data.*;
import org.cloudburstmc.server.level.chunk.CloudChunk;

import java.util.*;
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
    private final Map<BlockEntityType<?>, Class<? extends CloudBlockEntityData>> dataClasses = new IdentityHashMap<>();
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

    private <T extends BlockEntity> void registerVanilla(BlockEntityType<T> type, BlockEntityFactory<? extends T> factory, String persistentId,
                                                        Class<? extends CloudBlockEntityData> dataClass) {
        checkNotNull(type, "type");
        checkNotNull(factory, "factory");
        checkNotNull(persistentId, "persistentId");

        this.persistentMap.put(type, persistentId);
        this.providers.put(type, new RegistryServiceProvider<>(new RegistryProvider<>(factory, null, 1000)));
        this.validBlocks.put(type, VanillaBlockEntityBlocks.get(type));
        this.dataClasses.put(type, dataClass);
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
        checkArgument(!type.getId().getNamespace().equals("minecraft"),
                "Custom block entity types must use a non-minecraft namespace: %s", type.getId());
        checkArgument(!persistentId.isBlank(), "persistentId cannot be blank");
        checkArgument(!validBlocks.isEmpty(), "validBlocks cannot be empty");
        checkArgument(!this.providers.containsKey(type), "Block entity type is already registered: %s", type);
        checkArgument(!this.persistentMap.containsValue(persistentId), "Persistent ID is already registered: %s", persistentId);

        this.persistentMap.put(type, persistentId);
        this.providers.put(type, new RegistryServiceProvider<>(new RegistryProvider<>(factory, null, 0)));
        this.validBlocks.put(type, Set.copyOf(validBlocks));
    }

    public Class<? extends CloudBlockEntityData> getDataClass(BlockEntityType<?> type) {
        checkNotNull(type, "type");
        return this.dataClasses.getOrDefault(type, CloudBlockEntityData.class);
    }

    public String getPersistentId(BlockEntityType<?> type) {
        String persistentId = this.persistentMap.get(type);
        return persistentId != null ? persistentId : this.unknownPersistentIds.get(type);
    }

    @Override
    public Optional<BlockEntityType<?>> get(Identifier id) {
        checkNotNull(id, "id");
        return this.providers.keySet().stream()
                .filter(type -> type.getId().equals(id))
                .findFirst();
    }

    @Override
    public Collection<BlockEntityType<?>> values() {
        return List.copyOf(this.providers.keySet());
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
                throw new RegistryException(type.getId() + " is not a registered entity");
            }

            service = (RegistryServiceProvider) UNKNOWN_PROVIDER;
        }
        return service;
    }

    @Override
    public synchronized void close() throws RegistryException {
        checkClosed();

        // Bake registry providers
        this.providers.values().forEach(RegistryServiceProvider::bake);

        this.closed = true;
    }

    /**
     * Returns whether registration is closed. Test fixtures use this to close
     * the shared registry exactly once, regardless of test execution order.
     *
     * @return true if {@link #close()} has completed
     */
    public boolean isClosed() {
        return closed;
    }

    private void checkClosed() {
        checkState(!closed, "Registration is already closed");
    }

    private void registerVanillaEntities() {
        registerVanilla(BlockEntityTypes.BANNER, BannerBlockEntity::new, "Banner", CloudBannerData.class);
        registerVanilla(BlockEntityTypes.BARREL, BarrelBlockEntity::new, "Barrel", CloudBarrelData.class);
        registerVanilla(BlockEntityTypes.BEACON, BeaconBlockEntity::new, "Beacon", CloudBeaconData.class);
        registerVanilla(BlockEntityTypes.BED, BedBlockEntity::new, "Bed", CloudBedData.class);
        registerVanilla(BlockEntityTypes.BELL, BellBlockEntity::new, "Bell", CloudBlockEntityData.class);
        registerVanilla(BlockEntityTypes.BLAST_FURNACE, BlastFurnaceBlockEntity::new, "BlastFurnace", CloudFurnaceData.class);
        registerVanilla(BlockEntityTypes.BREWING_STAND, BrewingStandBlockEntity::new, "BrewingStand", CloudBrewingStandData.class);
        registerVanilla(BlockEntityTypes.CAMPFIRE, CampfireBlockEntity::new, "Campfire", CloudCampfireData.class);
        registerVanilla(BlockEntityTypes.CAULDRON, CauldronBlockEntity::new, "Cauldron", CloudCauldronData.class);
        registerVanilla(BlockEntityTypes.CHEST, ChestBlockEntity::new, "Chest", CloudChestData.class);
        registerVanilla(BlockEntityTypes.COMPARATOR, ComparatorBlockEntity::new, "Comparator", CloudComparatorData.class);
        registerVanilla(BlockEntityTypes.CONDUIT, ConduitBlockEntity::new, "Conduit", CloudBlockEntityData.class);
        registerVanilla(BlockEntityTypes.CRAFTER, CrafterBlockEntity::new, "Crafter", CloudCrafterData.class);
        registerVanilla(BlockEntityTypes.DAYLIGHT_DETECTOR, DaylightDetectorBlockEntity::new, "DaylightDetector", CloudBlockEntityData.class);
        registerVanilla(BlockEntityTypes.DISPENSER, DispenserBlockEntity::new, "Dispenser", CloudDispenserData.class);
        registerVanilla(BlockEntityTypes.DROPPER, DropperBlockEntity::new, "Dropper", CloudDropperData.class);
        registerVanilla(BlockEntityTypes.ENCHANTING_TABLE, EnchantingTableBlockEntity::new, "EnchantTable", CloudBlockEntityData.class);
        registerVanilla(BlockEntityTypes.END_GATEWAY, EndGatewayBlockEntity::new, "EndGateway", CloudEndGatewayData.class);
        registerVanilla(BlockEntityTypes.ENDER_CHEST, EnderChestBlockEntity::new, "EnderChest", CloudChestData.class);
        registerVanilla(BlockEntityTypes.FLOWER_POT, FlowerPotBlockEntity::new, "FlowerPot", CloudFlowerPotData.class);
        registerVanilla(BlockEntityTypes.FURNACE, FurnaceBlockEntity::new, "Furnace", CloudFurnaceData.class);
        registerVanilla(BlockEntityTypes.GLOW_ITEM_FRAME, ItemFrameBlockEntity::new, "GlowItemFrame", CloudItemFrameData.class);
        registerVanilla(BlockEntityTypes.HOPPER, HopperBlockEntity::new, "Hopper", CloudHopperData.class);
        registerVanilla(BlockEntityTypes.ITEM_FRAME, ItemFrameBlockEntity::new, "ItemFrame", CloudItemFrameData.class);
        registerVanilla(BlockEntityTypes.JUKEBOX, JukeboxBlockEntity::new, "Jukebox", CloudJukeboxData.class);
        registerVanilla(BlockEntityTypes.LECTERN, LecternBlockEntity::new, "Lectern", CloudLecternData.class);
        registerVanilla(BlockEntityTypes.MOVING_BLOCK, MovingBlockEntity::new, "MovingBlock", CloudMovingBlockData.class);
        registerVanilla(BlockEntityTypes.NOTE_BLOCK, NoteBlockEntity::new, "Music", CloudMusicData.class);
        registerVanilla(BlockEntityTypes.PISTON, PistonBlockEntity::new, "PistonArm", CloudBlockEntityData.class);
        registerVanilla(BlockEntityTypes.SHULKER_BOX, ShulkerBoxBlockEntity::new, "ShulkerBox", CloudShulkerBoxData.class);
        registerVanilla(BlockEntityTypes.SIGN, SignBlockEntity::new, "Sign", CloudSignData.class);
        registerVanilla(BlockEntityTypes.SKULL, SkullBlockEntity::new, "Skull", CloudSkullData.class);
        registerVanilla(BlockEntityTypes.SMOKER, SmokerBlockEntity::new, "Smoker", CloudFurnaceData.class);
    }
}
