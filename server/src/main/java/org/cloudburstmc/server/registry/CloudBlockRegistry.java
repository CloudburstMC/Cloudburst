package org.cloudburstmc.server.registry;

import com.google.common.collect.HashBiMap;
import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.registry.BlockRegistry;
import org.cloudburstmc.api.registry.RegistryException;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.api.util.VoxelShape;
import org.cloudburstmc.api.util.component.ComponentBuilder;
import org.cloudburstmc.blockstateupdater.BlockStateUpdaters;
import org.cloudburstmc.nbt.NbtList;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.Bootstrap;
import org.cloudburstmc.server.block.BlockPalette;
import org.cloudburstmc.server.block.CloudBlockDefinition;
import org.cloudburstmc.server.block.component.*;
import org.cloudburstmc.server.block.serializer.BlockSerializer;
import org.cloudburstmc.server.block.serializer.DefaultBlockSerializer;
import org.cloudburstmc.server.block.trait.BlockTraitSerializers;
import org.cloudburstmc.server.block.util.BlockStateMetaMappings;
import org.cloudburstmc.server.block.util.BlockSupport;
import org.cloudburstmc.server.registry.component.CloudComponentMap;
import tools.jackson.core.type.TypeReference;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Singleton {@link BlockRegistry} that loads the block state palette from the embedded JSON resource,
 * maintains a bidirectional {@link org.cloudburstmc.api.block.BlockState} ↔ network runtime-ID mapping,
 * and registers all block behaviors.
 */
public class CloudBlockRegistry extends CloudComponentRegistry<BlockType> implements BlockRegistry {
    private static final HashBiMap<Identifier, Integer> VANILLA_LEGACY_IDS = HashBiMap.create();
    public static CloudBlockRegistry REGISTRY;

    static {
        InputStream stream = RegistryUtils.getOrAssertResource("data/legacy_block_ids.json");

        try {
            VANILLA_LEGACY_IDS.putAll(Bootstrap.JSON_MAPPER.readValue(stream, new TypeReference<Map<Identifier, Integer>>() {
            }));
        } catch (Exception e) {
            throw new AssertionError("Unable to load legacy IDs", e);
        }
    }

    private final BlockPalette palette = BlockPalette.INSTANCE;
    private final CloudItemRegistry itemRegistry;
    private NbtMap propertiesTag;
    private volatile boolean closed;
    private transient NbtList<NbtMap> serializedPalette;

    public CloudBlockRegistry(CloudItemRegistry itemRegistry) {
        this.itemRegistry = itemRegistry;
        BlockTraitSerializers.init();
        this.registerVanillaBehaviors();
        BlockTypeDefaultInitializer.init(this);
        BlockTypeInitializer.init(this);
        VanillaBlockTags.freeze();
        REGISTRY = this; // TODO: Remove at some point
    }

    CloudComponentMap registerVanilla(BlockType type) throws RegistryException {
        return this.registerVanilla(type, DefaultBlockSerializer.INSTANCE);
    }

    synchronized CloudComponentMap registerVanilla(BlockType type, BlockSerializer serializer) throws RegistryException {
        checkNotNull(type, "type");
        checkNotNull(serializer, "serializer");
        checkClosed();

        if (getComponentMap(type) != null) {
            throw new RegistryException(type.getId() + " is already registered");
        }

        VanillaBlockTags.bind(type);

        this.itemRegistry.registerBlock(type);

        CloudComponentMap collection = new CloudComponentMap(this);

        putComponents(type, collection);

        this.palette.addBlock(type, serializer);

        return collection;
    }

    @Override
    public boolean isBlock(Identifier id) {
        return VANILLA_LEGACY_IDS.containsKey(id);
    }

    @Override
    public int getRuntimeId(BlockState state) {
        return getDefinition(state).getRuntimeId();
    }

    @Override
    public VoxelShape getBlockSupportShape(BlockState state) {
        return BlockSupport.getBlockSupportShape(this, state);
    }

    @Override
    public boolean isFaceSturdy(BlockState state, Direction face, SupportType supportType) {
        return BlockSupport.isFaceSturdy(this, state, face, supportType);
    }

    @Override
    public BlockTag getTag(BlockTagKey key) {
        return VanillaBlockTags.resolve(key);
    }

    @Override
    public ComponentBuilder configure(BlockType type) {
        checkClosed();
        CloudComponentMap components = getComponentMap(checkNotNull(type, "type"));
        if (components == null) {
            throw new RegistryException(type.getId() + " is not registered");
        }
        return components;
    }

    public CloudBlockDefinition getDefinition(int runtimeId) {
        return this.palette.getDefinition(runtimeId);
    }

    public CloudBlockDefinition getDefinition(BlockState blockState) {
        return this.palette.getDefinition(blockState);
    }

    @Override
    public int getRuntimeId(Identifier id, int meta) {
        return getDefinition(id, meta).getRuntimeId();
    }

    public CloudBlockDefinition getDefinition(Identifier identifier, int meta) {
        NbtMap tag = NbtMap.builder()
                .putString("name", identifier.toString())
                .putShort("val", (short) meta)
                .putInt("version", 0)
                .build();

        tag = BlockStateUpdaters.updateBlockState(tag, 0);

        return palette.getDefinition(palette.getBlockState(tag));
    }

    public CloudBlockDefinition getDefinition(int id, int meta) {
        return getDefinition(VANILLA_LEGACY_IDS.inverse().get(id), meta);
    }

    public BlockState getBlock(BlockType type) {
        return palette.getDefaultState(type);
    }

    public BlockState getBlock(ItemStack item) {
        return BlockStateMetaMappings.getStateFromMeta(item.getType().getId(), 0);
    }

    public BlockState getBlock(Identifier identifier) {
        return palette.getState(identifier);
    }

    // TODO: Blocks are flattened
    public BlockState getBlock(Identifier identifier, int meta) {
        return getDefinition(identifier, meta).getCloudState();
    }

    // TODO: Blocks are flattened
    public BlockState getBlock(int id, int meta) {
        return getDefinition(id, meta).getCloudState();
    }

    public BlockState getBlock(int runtimeId) {
        BlockState blockBehavior = this.palette.getBlockState(runtimeId);
        if (blockBehavior == null) {
            throw new RegistryException("No block for runtime ID " + runtimeId + " registered");
        }
        return blockBehavior;
    }

    public BlockState getBlock(NbtMap tag) {
        BlockState state;

        if (!tag.containsKey("states", NbtType.COMPOUND)) {
            tag = tag.toBuilder().putCompound("states", NbtMap.EMPTY).build();
        }

        state = palette.getBlockState(tag);

        if (state == null) {
            tag = BlockStateUpdaters.updateBlockState(tag, tag.getInt("version"));
            state = palette.getBlockState(tag);
        }

        if (state == null/* && tag.containsKey("states", NbtType.COMPOUND)*/) { //TODO: fix unknown states
            var defaultState = getBlock(Identifier.parse(tag.getString("name")));
            var serialized = palette.getSerialized(defaultState);

            if (serialized.containsKey("states", NbtType.COMPOUND)) {
                var builder = tag.toBuilder();

                var statesBuilder = ((NbtMap) builder.get("states")).toBuilder();
                serialized.getCompound("states").forEach(statesBuilder::putIfAbsent);
                builder.putCompound("states", statesBuilder.build());
                state = palette.getBlockState(builder.build());
            }
        }

        if (state == null) throw new IllegalStateException("Invalid block state\n" + tag);

        return state;
    }

    public int getLegacyId(String name) {
        return getLegacyId(Identifier.parse(name));
    }

    public int getLegacyId(Identifier identifier) {
        int legacyId = VANILLA_LEGACY_IDS.getOrDefault(identifier, -1);
        if (legacyId == -1) {
            throw new RegistryException("No legacy ID found for " + identifier);
        }
        return legacyId;
    }

    public Identifier getNameFromLegacyId(int id) {
        Identifier identifier = VANILLA_LEGACY_IDS.inverse().get(id);
        if (identifier == null) {
            throw new RegistryException("No block found for ID " + id);
        }
        return identifier;
    }

    @Override
    public synchronized void close() throws RegistryException {
        checkClosed();
        this.freezeComponentMaps();
        this.closed = true;
        this.palette.generateRuntimeIds();
        // generate cache

        this.propertiesTag = NbtMap.EMPTY;
    }

    private void checkClosed() throws RegistryException {
        if (this.closed) {
            throw new RegistryException("Registration has been closed");
        }
    }

    public NbtList<NbtMap> getPaletteTag() {
        if (this.serializedPalette != null) {
            return serializedPalette;
        }
        Map<NbtMap, BlockState> palette = this.palette.getSerializedPalette();
        List<NbtMap> serialized = new ArrayList<>(palette.size());
        palette.forEach((serializedState, state) -> {
            serialized.add(NbtMap.builder()
                    .putCompound("block", serializedState)
                    .putShort("id", (short) this.getLegacyId(state.getType().getId()))
                    .build());
        });

        this.serializedPalette = new NbtList<>(NbtType.COMPOUND, serialized);
        return this.serializedPalette;
    }

    public NbtMap getPropertiesTag() {
        return propertiesTag;
    }

    public List<BlockState> getBlockStates() {
        return List.copyOf(palette.getSerializedPalette().values());
    }

    private void registerVanillaBehaviors() {
        this.registerComponent(BlockComponents.BUTTON_PRESS_DURATION_TICKS, ButtonBlockHandlers.WOODEN_PRESS_TICKS);
        this.registerComponent(BlockComponents.BUCKET_PICKUP, (block, player) -> ItemStack.EMPTY);
        this.registerComponent(BlockComponents.CAN_RANDOM_TICK, false);
        this.registerComponent(BlockComponents.GET_DESCRIPTION_ID, (state) -> state.getType().getId().toString());
        this.registerComponent(BlockComponents.GET_BLOCK_SUPPORT_SHAPE, DefaultBlockHandlers.GET_BLOCK_SUPPORT_SHAPE);
        this.registerComponent(BlockComponents.GET_COLLISION_SHAPE, new GetCollisionShapeHandler());
        this.registerComponent(BlockComponents.GET_ENTITY_INSIDE_COLLISION_SHAPE, DefaultBlockHandlers.GET_ENTITY_INSIDE_COLLISION_SHAPE);
        this.registerComponent(BlockComponents.GET_OUTLINE_SHAPE, new GetOutlineShapeHandler());
        this.registerComponent(BlockComponents.SUFFOCATING, DefaultBlockHandlers.SUFFOCATING);
        this.registerComponent(BlockComponents.CAN_BE_REPLACED, (block, replacement, player, face, click) -> block.getState().isReplaceable() && block.getState().getType() != replacement.getType());
        this.registerComponent(BlockComponents.CAN_BE_USED_IN_COMMANDS, DefaultBlockHandlers.CAN_BE_USED_IN_COMMANDS);
        this.registerComponent(BlockComponents.CAN_SPAWN_ON, DefaultBlockHandlers.CAN_SPAWN_ON);
        this.registerComponent(BlockComponents.CAN_BE_USED, (block, player) -> false);
        this.registerComponent(BlockComponents.GET_GRAVITY, (block) -> 0.02f);
        this.registerComponent(BlockComponents.GET_EXPERIENCE, DefaultBlockHandlers.GET_EXPERIENCE);
        this.registerComponent(BlockComponents.GET_BLOCK_ENTITY, (block) -> Optional.empty());
        this.registerComponent(BlockComponents.MAY_PICK, (block) -> true);
        this.registerComponent(BlockComponents.MAY_PLACE, (block, direction) -> true);
        this.registerComponent(BlockComponents.MAY_PLACE_ON, (block) -> true);
        this.registerComponent(BlockComponents.ON_DESTROY, DefaultBlockHandlers.ON_DESTROY);
        this.registerComponent(BlockComponents.ON_NEIGHBOUR_CHANGED, (block, neighbor) -> {});
        this.registerComponent(BlockComponents.ON_FALL_ON, DefaultBlockHandlers.ON_FALL_ON);
        this.registerComponent(BlockComponents.ON_FALLING_LAND, (entity, target, fallDistance) -> {});
        this.registerComponent(BlockComponents.ON_LIGHTNING_HIT, DefaultBlockHandlers.ON_LIGHTNING_HIT);
        this.registerComponent(BlockComponents.ON_PLACE, new DefaultBlockPlaceHandler(this));
        this.registerComponent(BlockComponents.RESOLVE_PLACEMENT_STATE, (state, block, player, face, clickPosition) -> state);
        this.registerComponent(BlockComponents.ON_PROJECTILE_HIT, DefaultBlockHandlers.ON_PROJECTILE_HIT);
        this.registerComponent(BlockComponents.ON_REDSTONE_UPDATE, DefaultBlockHandlers.ON_REDSTONE_UPDATE);
        this.registerComponent(BlockComponents.ON_REMOVE, DefaultBlockHandlers.ON_REMOVE);
        this.registerComponent(BlockComponents.ON_RANDOM_TICK, (block, randomGenerator) -> {});
        this.registerComponent(BlockComponents.ON_TICK, (block, randomGenerator) -> {});
        this.registerComponent(BlockComponents.USE, (block, player, direction, item) -> false);
        this.registerComponent(BlockComponents.ON_STAND_ON, DefaultBlockHandlers.ON_STAND_ON);
        this.registerComponent(BlockComponents.ON_STEP_ON, DefaultBlockHandlers.ON_STEP_ON);
        this.registerComponent(BlockComponents.ON_STEP_OFF, DefaultBlockHandlers.ON_STEP_OFF);
        this.registerComponent(BlockComponents.GET_LOOT, DefaultBlockHandlers.GET_LOOT);
        this.registerComponent(BlockComponents.GET_PICK_BLOCK, DefaultBlockHandlers.GET_PICK_BLOCK);
        this.registerComponent(BlockComponents.GET_COLOR, (block) -> null);
        this.registerComponent(BlockComponents.CAN_SURVIVE, (block) -> true);
        this.registerComponent(BlockComponents.CHECK_ALIVE, (block) -> {});
        this.registerComponent(BlockComponents.CAN_SLIDE, (block) -> false);
        this.registerComponent(BlockComponents.IS_FREE_TO_FALL, (block) -> false);
        this.registerComponent(BlockComponents.START_FALLING, (block) -> {});
        this.registerComponent(BlockComponents.ON_ENTITY_COLLIDE, (block, entity) -> {});
        this.registerComponent(BlockComponents.ON_ENTITY_INSIDE, DefaultBlockHandlers.ON_ENTITY_INSIDE);
        this.registerComponent(BlockComponents.GET_MAP_COLOR, (block) -> block.getState().getMapColor());
        this.registerComponent(BlockComponents.IS_BREAKABLE, (block, item) -> true);
    }
}
