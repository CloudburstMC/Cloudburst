package org.cloudburstmc.server.registry;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.nukkitx.math.vector.Vector3i;
import org.cloudburstmc.api.registry.RegistryException;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.blockentity.BlockEntity;
import org.cloudburstmc.server.blockentity.BlockEntityFactory;
import org.cloudburstmc.server.blockentity.BlockEntityType;
import org.cloudburstmc.server.blockentity.BlockEntityTypes;
import org.cloudburstmc.server.blockentity.impl.*;
import org.cloudburstmc.server.level.chunk.Chunk;

import javax.annotation.Nonnull;
import java.util.IdentityHashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.*;

public class BlockEntityRegistry implements Registry {
    private static final BlockEntityRegistry INSTANCE = new BlockEntityRegistry();

    private final Map<BlockEntityType<?>, RegistryServiceProvider<BlockEntityFactory<?>>> providers = new IdentityHashMap<>();
    private final BiMap<BlockEntityType<?>, String> persistentMap = HashBiMap.create();
    private volatile boolean closed;

    private static RegistryServiceProvider<BlockEntityFactory<?>> UNKNOWN_PROVIDER = new RegistryServiceProvider<>(new RegistryProvider<>(UnknownBlockEntity::new, null, 1000));

    private BlockEntityRegistry() {
        this.registerVanillaEntities();
    }

    public static BlockEntityRegistry get() {
        return INSTANCE;
    }

    private <T extends BlockEntity> void registerVanilla(BlockEntityType<T> type, BlockEntityFactory<? extends T> factory, String persistentId) {
        checkNotNull(type, "type");
        checkNotNull(factory, "factory");
        checkNotNull(persistentId, "persistentId");

        this.persistentMap.put(type, persistentId);
        this.providers.put(type, new RegistryServiceProvider<>(new RegistryProvider<>(factory, null, 1000)));
    }

    public synchronized <T extends BlockEntity> void register(Object plugin, BlockEntityType<T> type,
                                                              BlockEntityFactory<T> factory, int priority) throws RegistryException {
        checkClosed();
        checkNotNull(type, "type");
        checkNotNull(factory, "factory");
        checkArgument(this.providers.containsKey(type), "Undefined BlockEntityType %", type);

        //noinspection unchecked,rawtypes
        RegistryServiceProvider<BlockEntityFactory<T>> service = (RegistryServiceProvider) this.providers.get(type);
        service.add(new RegistryProvider<>(factory, plugin, priority));

    }

    public String getPersistentId(BlockEntityType<?> type) {
        return persistentMap.get(type);
    }

    @Nonnull
    public BlockEntityType<?> getBlockEntityType(String persistentId) {
        return persistentMap.inverse().computeIfAbsent(persistentId, id -> BlockEntityType.from(id, UnknownBlockEntity.class));
    }

    public <T extends BlockEntity> T newEntity(BlockEntityType<T> type, Block block) {
        return newEntity(type, block.getChunk(), block.getPosition());
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
    public <T extends BlockEntity> T newEntity(BlockEntityType<T> type, Chunk chunk, Vector3i position) {
        checkState(closed, "Cannot create entity till registry is closed");
        checkNotNull(type, "type");
        checkNotNull(chunk, "chunk");
        checkNotNull(position, "position");
        BlockEntityFactory<T> factory = getServiceProvider(type).getProvider().getValue();
        return factory.create(type, chunk, position);
    }

    /**
     * Creates new entity of given type from specific plugin factory
     *
     * @param type     entity type
     * @param chunk    chunk of block entity
     * @param position position of block entity in world
     * @param <T>      entity class type
     * @return new entity
     */
    public <T extends BlockEntity> T newEntity(BlockEntityType<T> type, Object plugin, Chunk chunk, Vector3i position) {
        checkState(closed, "Cannot create entity till registry is closed");
        checkNotNull(type, "type");
        checkNotNull(plugin, "plugin");
        checkNotNull(chunk, "chunk");
        checkNotNull(position, "position");
        RegistryProvider<BlockEntityFactory<T>> provider = getServiceProvider(type).getProvider(plugin);
        if (provider == null) {
            throw new RegistryException("Plugin has no registered provider for " + type.getIdentifier());
        }
        return provider.getValue().create(type, chunk, position);
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

    @Override
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
        registerVanilla(BlockEntityTypes.CHEST, ChestBlockEntity::new, "Chest");
        registerVanilla(BlockEntityTypes.ENDER_CHEST, EnderChestBlockEntity::new, "EnderChest");
        registerVanilla(BlockEntityTypes.FURNACE, FurnaceBlockEntity::new, "Furnace");
        registerVanilla(BlockEntityTypes.SIGN, SignBlockEntity::new, "Sign");
//        registerVanilla(BlockEntityTypes.MOB_SPAWNER, MobSpawnerBlockEntity::new, "MobSpawner");
        registerVanilla(BlockEntityTypes.ENCHANTING_TABLE, EnchantingTableBlockEntity::new, "EnchantTable");
        registerVanilla(BlockEntityTypes.SKULL, SkullBlockEntity::new, "Skull");
        registerVanilla(BlockEntityTypes.FLOWER_POT, FlowerPotBlockEntity::new, "FlowerPot");
        registerVanilla(BlockEntityTypes.BREWING_STAND, BrewingStandBlockEntity::new, "BrewingStand");
        registerVanilla(BlockEntityTypes.DAYLIGHT_DETECTOR, DaylightDetectorBlockEntity::new, "DaylightDetector");
        registerVanilla(BlockEntityTypes.NOTEBLOCK, MusicBlockEntity::new, "Music");
        registerVanilla(BlockEntityTypes.ITEM_FRAME, ItemFrameBlockEntity::new, "ItemFrame");
        registerVanilla(BlockEntityTypes.CAULDRON, CauldronBlockEntity::new, "Cauldron");
        registerVanilla(BlockEntityTypes.BEACON, BeaconBlockEntity::new, "Beacon");
        registerVanilla(BlockEntityTypes.PISTON, PistonBlockEntity::new, "PistonArm");
        registerVanilla(BlockEntityTypes.MOVING_BLOCK, MovingBlockEntity::new, "MovingBlock");
        registerVanilla(BlockEntityTypes.COMPARATOR, ComparatorBlockEntity::new, "Comparator");
        registerVanilla(BlockEntityTypes.HOPPER, HopperBlockEntity::new, "Hopper");
        registerVanilla(BlockEntityTypes.BED, BedBlockEntity::new, "Bed");
        registerVanilla(BlockEntityTypes.JUKEBOX, JukeboxBlockEntity::new, "Jukebox");
        registerVanilla(BlockEntityTypes.SHULKER_BOX, ShulkerBoxBlockEntity::new, "ShulkerBox");
        registerVanilla(BlockEntityTypes.BANNER, BannerBlockEntity::new, "Banner");
        registerVanilla(BlockEntityTypes.CAMPFIRE, CampfireBlockEntity::new, "Campfire");
        registerVanilla(BlockEntityTypes.BLAST_FURNACE, BlastFurnaceBlockEntity::new, "BlastFurnace");
        registerVanilla(BlockEntityTypes.SMOKER, SmokerBlockEntity::new, "Smoker");
        registerVanilla(BlockEntityTypes.BARREL, BarrelBlockEntity::new, "Barrel");
    }
}