package org.cloudburstmc.server.registry;

import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.api.block.BlockComponents;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.block.component.StringTypeHandler;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.registry.BlockRegistry;
import org.cloudburstmc.api.registry.RegistryException;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.blockstateupdater.BlockStateUpdaters;
import org.cloudburstmc.nbt.NbtList;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.Bootstrap;
import org.cloudburstmc.server.block.BlockPalette;
import org.cloudburstmc.server.block.BlockTypeData;
import org.cloudburstmc.server.block.CloudBlockDefinition;
import org.cloudburstmc.server.block.component.*;
import org.cloudburstmc.server.block.serializer.BlockSerializer;
import org.cloudburstmc.server.block.serializer.DefaultBlockSerializer;
import org.cloudburstmc.server.block.serializer.FluidBlockSerializer;
import org.cloudburstmc.server.block.trait.BlockTraitSerializers;
import org.cloudburstmc.server.block.util.BlockStateMetaMappings;
import org.cloudburstmc.server.registry.component.CloudComponentMap;
import tools.jackson.core.type.TypeReference;

import java.awt.*;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.cloudburstmc.api.block.BlockTypes.*;

/**
 * Singleton {@link BlockRegistry} that loads the block state palette from the embedded JSON resource,
 * maintains a bidirectional {@link org.cloudburstmc.api.block.BlockState} ↔ network runtime-ID mapping,
 * and registers all block behaviors.
 */
@Log4j2
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

    private final AtomicInteger customIdAllocator = new AtomicInteger(1000);
    private final BlockPalette palette = BlockPalette.INSTANCE;
    private final CloudItemRegistry itemRegistry;
    private NbtMap propertiesTag;
    private volatile boolean closed;
    private transient NbtList<NbtMap> serializedPalette;

    public CloudBlockRegistry(CloudItemRegistry itemRegistry) {
        this.itemRegistry = itemRegistry;
        BlockTraitSerializers.init();
        this.registerVanillaBehaviors();
        this.registerVanillaBlocks();
        REGISTRY = this; // TODO: Remove at some point
    }

    @Override
    public synchronized CloudComponentMap register(BlockType type) throws RegistryException {
        checkNotNull(type, "type");
        CloudComponentMap behaviors = registerVanilla(type);

        // generate legacy ID (Not sure why we need to but it's a requirement)
        int legacyId = this.customIdAllocator.getAndIncrement();
        VANILLA_LEGACY_IDS.put(type.getId(), legacyId);
        return behaviors;
    }

    private CloudComponentMap registerVanilla(BlockType type) throws RegistryException {
        return this.registerVanilla(type, DefaultBlockSerializer.INSTANCE);
    }

    private synchronized CloudComponentMap registerVanilla(BlockType type, BlockSerializer serializer) throws RegistryException {
        checkNotNull(type, "type");
        checkNotNull(serializer, "serializer");
        checkClosed();

        this.itemRegistry.registerBlock(type);

        CloudComponentMap collection = new CloudComponentMap(this);

        collection.bake();

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

    public ImmutableList<BlockState> getBlockStates() {
        return ImmutableList.copyOf(palette.getSerializedPalette().values());
    }

    private void registerVanillaBlocks() {
        this.registerVanilla(ACACIA_BUTTON);
        this.registerDoor(ACACIA_DOOR);
        this.registerVanilla(ACACIA_DOUBLE_SLAB);
        this.registerVanilla(ACACIA_FENCE);
        this.registerVanilla(ACACIA_FENCE_GATE);
        this.registerVanilla(ACACIA_HANGING_SIGN);
        this.registerVanilla(ACACIA_LEAVES);
        this.registerVanilla(ACACIA_LOG);
        this.registerVanilla(ACACIA_PLANKS);
        this.registerVanilla(ACACIA_PRESSURE_PLATE);
        this.registerVanilla(ACACIA_SAPLING);
        this.registerVanilla(ACACIA_SHELF);
        this.registerVanilla(ACACIA_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(ACACIA_DOUBLE_SLAB));
        this.registerVanilla(ACACIA_STAIRS);
        this.registerVanilla(ACACIA_STANDING_SIGN);
        this.registerTrapdoor(ACACIA_TRAPDOOR);
        this.registerVanilla(ACACIA_WALL_SIGN);
        this.registerVanilla(ACACIA_WOOD);
        this.registerVanilla(ACTIVATOR_RAIL);
        this.registerVanilla(AIR)
                .set(BlockComponents.REPLACEABLE, () -> true)
                .set(BlockComponents.GET_RESOURCE_COUNT, (block, random, bonusLevel) -> 0);
        this.registerVanilla(ALLIUM);
        this.registerVanilla(ALLOW);
        this.registerVanilla(AMETHYST_BLOCK);
        this.registerVanilla(AMETHYST_CLUSTER);
        this.registerVanilla(ANCIENT_DEBRIS);
        this.registerVanilla(ANDESITE);
        this.registerVanilla(ANDESITE_DOUBLE_SLAB);
        this.registerVanilla(ANDESITE_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(ANDESITE_DOUBLE_SLAB));
        this.registerVanilla(ANDESITE_STAIRS);
        this.registerVanilla(ANDESITE_WALL);
        this.registerVanilla(ANVIL)
                .set(BlockComponents.ON_PLACE, new AnvilPlaceHandler(this))
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, ContainerBlockHandlers.ANVIL);
        this.registerVanilla(AZALEA);
        this.registerVanilla(AZALEA_LEAVES);
        this.registerVanilla(AZALEA_LEAVES_FLOWERED);
        this.registerVanilla(AZURE_BLUET);
        this.registerVanilla(BAMBOO);
        this.registerVanilla(BAMBOO_BLOCK);
        this.registerVanilla(BAMBOO_BUTTON);
        this.registerDoor(BAMBOO_DOOR);
        this.registerVanilla(BAMBOO_DOUBLE_SLAB);
        this.registerVanilla(BAMBOO_FENCE);
        this.registerVanilla(BAMBOO_FENCE_GATE);
        this.registerVanilla(BAMBOO_HANGING_SIGN);
        this.registerVanilla(BAMBOO_MOSAIC);
        this.registerVanilla(BAMBOO_MOSAIC_DOUBLE_SLAB);
        this.registerVanilla(BAMBOO_MOSAIC_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(BAMBOO_MOSAIC_DOUBLE_SLAB));
        this.registerVanilla(BAMBOO_MOSAIC_STAIRS);
        this.registerVanilla(BAMBOO_PLANKS);
        this.registerVanilla(BAMBOO_PRESSURE_PLATE);
        this.registerVanilla(BAMBOO_SAPLING);
        this.registerVanilla(BAMBOO_SHELF);
        this.registerVanilla(BAMBOO_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(BAMBOO_DOUBLE_SLAB));
        this.registerVanilla(BAMBOO_STAIRS);
        this.registerVanilla(BAMBOO_STANDING_SIGN);
        this.registerTrapdoor(BAMBOO_TRAPDOOR);
        this.registerVanilla(BAMBOO_WALL_SIGN);
        this.registerVanilla(BARREL)
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, ContainerBlockHandlers.BARREL);
        this.registerVanilla(BARRIER);
        this.registerVanilla(BASALT);
        this.registerVanilla(BEACON)
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, ContainerBlockHandlers.BEACON);
        this.registerVanilla(BED)
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, BedBlockHandlers.BED)
                .set(BlockComponents.ON_PLACE, BedBlockHandlers.PLACE)
                .set(BlockComponents.ON_DESTROY, BedBlockHandlers.ON_DESTROY)
                .set(BlockComponents.GET_RESOURCE, BedBlockHandlers.GET_RESOURCE)
                .set(BlockComponents.GET_PICK_BLOCK, BedBlockHandlers.GET_PICK_BLOCK);
        this.registerVanilla(BEDROCK);
        this.registerVanilla(BEEHIVE);
        this.registerVanilla(BEETROOT);
        this.registerVanilla(BEE_NEST);
        this.registerVanilla(BELL);
        this.registerVanilla(BIG_DRIPLEAF);
        this.registerVanilla(BIRCH_BUTTON);
        this.registerDoor(BIRCH_DOOR);
        this.registerVanilla(BIRCH_DOUBLE_SLAB);
        this.registerVanilla(BIRCH_FENCE);
        this.registerVanilla(BIRCH_FENCE_GATE);
        this.registerVanilla(BIRCH_HANGING_SIGN);
        this.registerVanilla(BIRCH_LEAVES);
        this.registerVanilla(BIRCH_LOG);
        this.registerVanilla(BIRCH_PLANKS);
        this.registerVanilla(BIRCH_PRESSURE_PLATE);
        this.registerVanilla(BIRCH_SAPLING);
        this.registerVanilla(BIRCH_SHELF);
        this.registerVanilla(BIRCH_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(BIRCH_DOUBLE_SLAB));
        this.registerVanilla(BIRCH_STAIRS);
        this.registerVanilla(BIRCH_STANDING_SIGN);
        this.registerTrapdoor(BIRCH_TRAPDOOR);
        this.registerVanilla(BIRCH_WALL_SIGN);
        this.registerVanilla(BIRCH_WOOD);
        this.registerVanilla(BLACKSTONE);
        this.registerVanilla(BLACKSTONE_DOUBLE_SLAB);
        this.registerVanilla(BLACKSTONE_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(BLACKSTONE_DOUBLE_SLAB));
        this.registerVanilla(BLACKSTONE_STAIRS);
        this.registerVanilla(BLACKSTONE_WALL);
        this.registerVanilla(BLACK_CANDLE);
        this.registerVanilla(BLACK_CANDLE_CAKE);
        this.registerVanilla(BLACK_CARPET);
        this.registerVanilla(BLACK_CONCRETE);
        this.registerVanilla(BLACK_CONCRETE_POWDER);
        this.registerVanilla(BLACK_GLAZED_TERRACOTTA);
        this.registerVanilla(BLACK_SHULKER_BOX)
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, ContainerBlockHandlers.SHULKER_BOX);
        this.registerVanilla(BLACK_STAINED_GLASS);
        this.registerVanilla(BLACK_STAINED_GLASS_PANE);
        this.registerVanilla(BLACK_TERRACOTTA);
        this.registerVanilla(BLACK_WOOL);
        this.registerVanilla(BLAST_FURNACE)
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, ContainerBlockHandlers.BLAST_FURNACE);
        this.registerVanilla(BLUE_CANDLE);
        this.registerVanilla(BLUE_CANDLE_CAKE);
        this.registerVanilla(BLUE_CARPET);
        this.registerVanilla(BLUE_CONCRETE);
        this.registerVanilla(BLUE_CONCRETE_POWDER);
        this.registerVanilla(BLUE_GLAZED_TERRACOTTA);
        this.registerVanilla(BLUE_ICE);
        this.registerVanilla(BLUE_ORCHID);
        this.registerVanilla(BLUE_SHULKER_BOX)
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, ContainerBlockHandlers.SHULKER_BOX);
        this.registerVanilla(BLUE_STAINED_GLASS);
        this.registerVanilla(BLUE_STAINED_GLASS_PANE);
        this.registerVanilla(BLUE_TERRACOTTA);
        this.registerVanilla(BLUE_WOOL);
        this.registerVanilla(BONE_BLOCK);
        this.registerVanilla(BOOKSHELF);
        this.registerVanilla(BORDER_BLOCK);
        this.registerVanilla(BRAIN_CORAL);
        this.registerVanilla(BRAIN_CORAL_BLOCK);
        this.registerVanilla(BRAIN_CORAL_FAN);
        this.registerVanilla(BRAIN_CORAL_WALL_FAN);
        this.registerVanilla(BREWING_STAND)
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, ContainerBlockHandlers.BREWING_STAND);
        this.registerVanilla(BRICK_BLOCK);
        this.registerVanilla(BRICK_DOUBLE_SLAB);
        this.registerVanilla(BRICK_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(BRICK_DOUBLE_SLAB));
        this.registerVanilla(BRICK_STAIRS);
        this.registerVanilla(BRICK_WALL);
        this.registerVanilla(BROWN_CANDLE);
        this.registerVanilla(BROWN_CANDLE_CAKE);
        this.registerVanilla(BROWN_CARPET);
        this.registerVanilla(BROWN_CONCRETE);
        this.registerVanilla(BROWN_CONCRETE_POWDER);
        this.registerVanilla(BROWN_GLAZED_TERRACOTTA);
        this.registerVanilla(BROWN_MUSHROOM);
        this.registerVanilla(BROWN_MUSHROOM_BLOCK);
        this.registerVanilla(BROWN_SHULKER_BOX)
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, ContainerBlockHandlers.SHULKER_BOX);
        this.registerVanilla(BROWN_STAINED_GLASS);
        this.registerVanilla(BROWN_STAINED_GLASS_PANE);
        this.registerVanilla(BROWN_TERRACOTTA);
        this.registerVanilla(BROWN_WOOL);
        this.registerVanilla(BUBBLE_COLUMN);
        this.registerVanilla(BUBBLE_CORAL);
        this.registerVanilla(BUBBLE_CORAL_BLOCK);
        this.registerVanilla(BUBBLE_CORAL_FAN);
        this.registerVanilla(BUBBLE_CORAL_WALL_FAN);
        this.registerVanilla(BUDDING_AMETHYST);
        this.registerVanilla(BUSH);
        this.registerVanilla(CACTUS);
        this.registerVanilla(CACTUS_FLOWER);
        this.registerVanilla(CAKE);
        this.registerVanilla(CALCITE);
        this.registerVanilla(CALIBRATED_SCULK_SENSOR);
        this.registerVanilla(CAMERA);
        this.registerVanilla(CAMPFIRE);
        this.registerVanilla(CANDLE);
        this.registerVanilla(CANDLE_CAKE);
        this.registerVanilla(CARROTS);
        this.registerVanilla(CARTOGRAPHY_TABLE)
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, ContainerBlockHandlers.CARTOGRAPHY_TABLE);
        this.registerVanilla(CARVED_PUMPKIN);
        this.registerVanilla(CAULDRON);
        this.registerVanilla(CAVE_VINES);
        this.registerVanilla(CAVE_VINES_BODY_WITH_BERRIES);
        this.registerVanilla(CAVE_VINES_HEAD_WITH_BERRIES);
        this.registerVanilla(CHAIN_COMMAND_BLOCK);
        this.registerVanilla(CHALKBOARD);
        this.registerVanilla(CHEMICAL_HEAT);
        this.registerVanilla(CHERRY_BUTTON);
        this.registerDoor(CHERRY_DOOR);
        this.registerVanilla(CHERRY_DOUBLE_SLAB);
        this.registerVanilla(CHERRY_FENCE);
        this.registerVanilla(CHERRY_FENCE_GATE);
        this.registerVanilla(CHERRY_HANGING_SIGN);
        this.registerVanilla(CHERRY_LEAVES);
        this.registerVanilla(CHERRY_LOG);
        this.registerVanilla(CHERRY_PLANKS);
        this.registerVanilla(CHERRY_PRESSURE_PLATE);
        this.registerVanilla(CHERRY_SAPLING);
        this.registerVanilla(CHERRY_SHELF);
        this.registerVanilla(CHERRY_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(CHERRY_DOUBLE_SLAB));
        this.registerVanilla(CHERRY_STAIRS);
        this.registerVanilla(CHERRY_STANDING_SIGN);
        this.registerTrapdoor(CHERRY_TRAPDOOR);
        this.registerVanilla(CHERRY_WALL_SIGN);
        this.registerVanilla(CHERRY_WOOD);
        this.registerVanilla(CHEST)
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, ContainerBlockHandlers.CHEST);
        this.registerVanilla(CHIPPED_ANVIL)
                .set(BlockComponents.ON_PLACE, new AnvilPlaceHandler(this))
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, ContainerBlockHandlers.ANVIL);
        this.registerVanilla(CHISELED_BOOKSHELF);
        this.registerVanilla(CHISELED_CINNABAR);
        this.registerVanilla(CHISELED_COPPER);
        this.registerVanilla(CHISELED_DEEPSLATE);
        this.registerVanilla(CHISELED_NETHER_BRICKS);
        this.registerVanilla(CHISELED_POLISHED_BLACKSTONE);
        this.registerVanilla(CHISELED_QUARTZ_BLOCK);
        this.registerVanilla(CHISELED_RED_SANDSTONE);
        this.registerVanilla(CHISELED_RESIN_BRICKS);
        this.registerVanilla(CHISELED_SANDSTONE);
        this.registerVanilla(CHISELED_STONE_BRICKS);
        this.registerVanilla(CHISELED_SULFUR);
        this.registerVanilla(CHISELED_TUFF);
        this.registerVanilla(CHISELED_TUFF_BRICKS);
        this.registerVanilla(CHORUS_FLOWER);
        this.registerVanilla(CHORUS_PLANT);
        this.registerVanilla(CINNABAR);
        this.registerVanilla(CINNABAR_BRICKS);
        this.registerVanilla(CINNABAR_BRICK_DOUBLE_SLAB);
        this.registerVanilla(CINNABAR_BRICK_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(CINNABAR_BRICK_DOUBLE_SLAB));
        this.registerVanilla(CINNABAR_BRICK_STAIRS);
        this.registerVanilla(CINNABAR_BRICK_WALL);
        this.registerVanilla(CINNABAR_DOUBLE_SLAB);
        this.registerVanilla(CINNABAR_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(CINNABAR_DOUBLE_SLAB));
        this.registerVanilla(CINNABAR_STAIRS);
        this.registerVanilla(CINNABAR_WALL);
        this.registerVanilla(CLAY);
        this.registerVanilla(CLIENT_REQUEST_PLACEHOLDER_BLOCK);
        this.registerVanilla(CLOSED_EYEBLOSSOM);
        this.registerVanilla(COAL_BLOCK);
        this.registerVanilla(COAL_ORE);
        this.registerVanilla(COARSE_DIRT);
        this.registerVanilla(COBBLED_DEEPSLATE);
        this.registerVanilla(COBBLED_DEEPSLATE_DOUBLE_SLAB);
        this.registerVanilla(COBBLED_DEEPSLATE_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(COBBLED_DEEPSLATE_DOUBLE_SLAB));
        this.registerVanilla(COBBLED_DEEPSLATE_STAIRS);
        this.registerVanilla(COBBLED_DEEPSLATE_WALL);
        this.registerVanilla(COBBLESTONE);
        this.registerVanilla(COBBLESTONE_DOUBLE_SLAB);
        this.registerVanilla(COBBLESTONE_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(COBBLESTONE_DOUBLE_SLAB));
        this.registerVanilla(COBBLESTONE_STAIRS);
        this.registerVanilla(COBBLESTONE_WALL);
        this.registerVanilla(COCOA);
        this.registerVanilla(COLORED_TORCH_BLUE);
        this.registerVanilla(COLORED_TORCH_GREEN);
        this.registerVanilla(COLORED_TORCH_PURPLE);
        this.registerVanilla(COLORED_TORCH_RED);
        this.registerVanilla(COMMAND_BLOCK);
        this.registerVanilla(COMPOSTER);
        this.registerVanilla(COMPOUND_CREATOR);
        this.registerVanilla(CONDUIT);
        this.registerVanilla(COPPER_BARS);
        this.registerVanilla(COPPER_BLOCK);
        this.registerVanilla(COPPER_BULB);
        this.registerVanilla(COPPER_CHAIN);
        this.registerVanilla(COPPER_CHEST);
        this.registerDoor(COPPER_DOOR);
        this.registerVanilla(COPPER_GOLEM_STATUE);
        this.registerVanilla(COPPER_GRATE);
        this.registerVanilla(COPPER_LANTERN);
        this.registerVanilla(COPPER_ORE);
        this.registerVanilla(COPPER_TORCH);
        this.registerTrapdoor(COPPER_TRAPDOOR);
        this.registerVanilla(CORNFLOWER);
        this.registerVanilla(CRACKED_DEEPSLATE_BRICKS);
        this.registerVanilla(CRACKED_DEEPSLATE_TILES);
        this.registerVanilla(CRACKED_NETHER_BRICKS);
        this.registerVanilla(CRACKED_POLISHED_BLACKSTONE_BRICKS);
        this.registerVanilla(CRACKED_STONE_BRICKS);
        this.registerVanilla(CRAFTER)
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, ContainerBlockHandlers.CRAFTER);
        this.registerVanilla(CRAFTING_TABLE)
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, ContainerBlockHandlers.CRAFTING_TABLE);
        this.registerVanilla(CREAKING_HEART);
        this.registerVanilla(CREEPER_HEAD);
        this.registerVanilla(CRIMSON_BUTTON);
        this.registerDoor(CRIMSON_DOOR);
        this.registerVanilla(CRIMSON_DOUBLE_SLAB);
        this.registerVanilla(CRIMSON_FENCE);
        this.registerVanilla(CRIMSON_FENCE_GATE);
        this.registerVanilla(CRIMSON_FUNGUS);
        this.registerVanilla(CRIMSON_HANGING_SIGN);
        this.registerVanilla(CRIMSON_HYPHAE);
        this.registerVanilla(CRIMSON_NYLIUM);
        this.registerVanilla(CRIMSON_PLANKS);
        this.registerVanilla(CRIMSON_PRESSURE_PLATE);
        this.registerVanilla(CRIMSON_ROOTS);
        this.registerVanilla(CRIMSON_SHELF);
        this.registerVanilla(CRIMSON_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(CRIMSON_DOUBLE_SLAB));
        this.registerVanilla(CRIMSON_STAIRS);
        this.registerVanilla(CRIMSON_STANDING_SIGN);
        this.registerVanilla(CRIMSON_STEM);
        this.registerTrapdoor(CRIMSON_TRAPDOOR);
        this.registerVanilla(CRIMSON_WALL_SIGN);
        this.registerVanilla(CRYING_OBSIDIAN);
        this.registerVanilla(CUT_COPPER);
        this.registerVanilla(CUT_COPPER_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(DOUBLE_CUT_COPPER_SLAB));
        this.registerVanilla(CUT_COPPER_STAIRS);
        this.registerVanilla(CUT_RED_SANDSTONE);
        this.registerVanilla(CUT_RED_SANDSTONE_DOUBLE_SLAB);
        this.registerVanilla(CUT_RED_SANDSTONE_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(CUT_RED_SANDSTONE_DOUBLE_SLAB));
        this.registerVanilla(CUT_SANDSTONE);
        this.registerVanilla(CUT_SANDSTONE_DOUBLE_SLAB);
        this.registerVanilla(CUT_SANDSTONE_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(CUT_SANDSTONE_DOUBLE_SLAB));
        this.registerVanilla(CYAN_CANDLE);
        this.registerVanilla(CYAN_CANDLE_CAKE);
        this.registerVanilla(CYAN_CARPET);
        this.registerVanilla(CYAN_CONCRETE);
        this.registerVanilla(CYAN_CONCRETE_POWDER);
        this.registerVanilla(CYAN_GLAZED_TERRACOTTA);
        this.registerVanilla(CYAN_SHULKER_BOX)
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, ContainerBlockHandlers.SHULKER_BOX);
        this.registerVanilla(CYAN_STAINED_GLASS);
        this.registerVanilla(CYAN_STAINED_GLASS_PANE);
        this.registerVanilla(CYAN_TERRACOTTA);
        this.registerVanilla(CYAN_WOOL);
        this.registerVanilla(DAMAGED_ANVIL)
                .set(BlockComponents.ON_PLACE, new AnvilPlaceHandler(this))
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, ContainerBlockHandlers.ANVIL);
        this.registerVanilla(DANDELION);
        this.registerVanilla(DARKOAK_STANDING_SIGN);
        this.registerVanilla(DARKOAK_WALL_SIGN);
        this.registerVanilla(DARK_OAK_BUTTON);
        this.registerDoor(DARK_OAK_DOOR);
        this.registerVanilla(DARK_OAK_DOUBLE_SLAB);
        this.registerVanilla(DARK_OAK_FENCE);
        this.registerVanilla(DARK_OAK_FENCE_GATE);
        this.registerVanilla(DARK_OAK_HANGING_SIGN);
        this.registerVanilla(DARK_OAK_LEAVES);
        this.registerVanilla(DARK_OAK_LOG);
        this.registerVanilla(DARK_OAK_PLANKS);
        this.registerVanilla(DARK_OAK_PRESSURE_PLATE);
        this.registerVanilla(DARK_OAK_SAPLING);
        this.registerVanilla(DARK_OAK_SHELF);
        this.registerVanilla(DARK_OAK_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(DARK_OAK_DOUBLE_SLAB));
        this.registerVanilla(DARK_OAK_STAIRS);
        this.registerTrapdoor(DARK_OAK_TRAPDOOR);
        this.registerVanilla(DARK_OAK_WOOD);
        this.registerVanilla(DARK_PRISMARINE);
        this.registerVanilla(DARK_PRISMARINE_DOUBLE_SLAB);
        this.registerVanilla(DARK_PRISMARINE_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(DARK_PRISMARINE_DOUBLE_SLAB));
        this.registerVanilla(DARK_PRISMARINE_STAIRS);
        this.registerVanilla(DAYLIGHT_DETECTOR);
        this.registerVanilla(DAYLIGHT_DETECTOR_INVERTED);
        this.registerVanilla(DEADBUSH);
        this.registerVanilla(DEAD_BRAIN_CORAL);
        this.registerVanilla(DEAD_BRAIN_CORAL_BLOCK);
        this.registerVanilla(DEAD_BRAIN_CORAL_FAN);
        this.registerVanilla(DEAD_BRAIN_CORAL_WALL_FAN);
        this.registerVanilla(DEAD_BUBBLE_CORAL);
        this.registerVanilla(DEAD_BUBBLE_CORAL_BLOCK);
        this.registerVanilla(DEAD_BUBBLE_CORAL_FAN);
        this.registerVanilla(DEAD_BUBBLE_CORAL_WALL_FAN);
        this.registerVanilla(DEAD_FIRE_CORAL);
        this.registerVanilla(DEAD_FIRE_CORAL_BLOCK);
        this.registerVanilla(DEAD_FIRE_CORAL_FAN);
        this.registerVanilla(DEAD_FIRE_CORAL_WALL_FAN);
        this.registerVanilla(DEAD_HORN_CORAL);
        this.registerVanilla(DEAD_HORN_CORAL_BLOCK);
        this.registerVanilla(DEAD_HORN_CORAL_FAN);
        this.registerVanilla(DEAD_HORN_CORAL_WALL_FAN);
        this.registerVanilla(DEAD_TUBE_CORAL);
        this.registerVanilla(DEAD_TUBE_CORAL_BLOCK);
        this.registerVanilla(DEAD_TUBE_CORAL_FAN);
        this.registerVanilla(DEAD_TUBE_CORAL_WALL_FAN);
        this.registerVanilla(DECORATED_POT);
        this.registerVanilla(DEEPSLATE);
        this.registerVanilla(DEEPSLATE_BRICKS);
        this.registerVanilla(DEEPSLATE_BRICK_DOUBLE_SLAB);
        this.registerVanilla(DEEPSLATE_BRICK_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(DEEPSLATE_BRICK_DOUBLE_SLAB));
        this.registerVanilla(DEEPSLATE_BRICK_STAIRS);
        this.registerVanilla(DEEPSLATE_BRICK_WALL);
        this.registerVanilla(DEEPSLATE_COAL_ORE);
        this.registerVanilla(DEEPSLATE_COPPER_ORE);
        this.registerVanilla(DEEPSLATE_DIAMOND_ORE);
        this.registerVanilla(DEEPSLATE_EMERALD_ORE);
        this.registerVanilla(DEEPSLATE_GOLD_ORE);
        this.registerVanilla(DEEPSLATE_IRON_ORE);
        this.registerVanilla(DEEPSLATE_LAPIS_ORE);
        this.registerVanilla(DEEPSLATE_REDSTONE_ORE);
        this.registerVanilla(DEEPSLATE_TILES);
        this.registerVanilla(DEEPSLATE_TILE_DOUBLE_SLAB);
        this.registerVanilla(DEEPSLATE_TILE_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(DEEPSLATE_TILE_DOUBLE_SLAB));
        this.registerVanilla(DEEPSLATE_TILE_STAIRS);
        this.registerVanilla(DEEPSLATE_TILE_WALL);
        this.registerVanilla(DENY);
        this.registerVanilla(DEPRECATED_ANVIL);
        this.registerVanilla(DEPRECATED_PURPUR_BLOCK_1);
        this.registerVanilla(DEPRECATED_PURPUR_BLOCK_2);
        this.registerVanilla(DETECTOR_RAIL);
        this.registerVanilla(DIAMOND_BLOCK);
        this.registerVanilla(DIAMOND_ORE);
        this.registerVanilla(DIORITE);
        this.registerVanilla(DIORITE_DOUBLE_SLAB);
        this.registerVanilla(DIORITE_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(DIORITE_DOUBLE_SLAB));
        this.registerVanilla(DIORITE_STAIRS);
        this.registerVanilla(DIORITE_WALL);
        this.registerVanilla(DIRT);
        this.registerVanilla(DIRT_WITH_ROOTS);
        this.registerVanilla(DISPENSER)
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, ContainerBlockHandlers.DISPENSER);
        this.registerVanilla(DOUBLE_CUT_COPPER_SLAB);
        this.registerVanilla(DRAGON_EGG);
        this.registerVanilla(DRAGON_HEAD);
        this.registerVanilla(DRIED_GHAST);
        this.registerVanilla(DRIED_KELP_BLOCK);
        this.registerVanilla(DRIPSTONE_BLOCK);
        this.registerVanilla(DROPPER)
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, ContainerBlockHandlers.DROPPER);
        this.registerVanilla(ELEMENT_0);
        this.registerVanilla(ELEMENT_1);
        this.registerVanilla(ELEMENT_2);
        this.registerVanilla(ELEMENT_3);
        this.registerVanilla(ELEMENT_4);
        this.registerVanilla(ELEMENT_5);
        this.registerVanilla(ELEMENT_6);
        this.registerVanilla(ELEMENT_7);
        this.registerVanilla(ELEMENT_8);
        this.registerVanilla(ELEMENT_9);
        this.registerVanilla(ELEMENT_10);
        this.registerVanilla(ELEMENT_11);
        this.registerVanilla(ELEMENT_12);
        this.registerVanilla(ELEMENT_13);
        this.registerVanilla(ELEMENT_14);
        this.registerVanilla(ELEMENT_15);
        this.registerVanilla(ELEMENT_16);
        this.registerVanilla(ELEMENT_17);
        this.registerVanilla(ELEMENT_18);
        this.registerVanilla(ELEMENT_19);
        this.registerVanilla(ELEMENT_20);
        this.registerVanilla(ELEMENT_21);
        this.registerVanilla(ELEMENT_22);
        this.registerVanilla(ELEMENT_23);
        this.registerVanilla(ELEMENT_24);
        this.registerVanilla(ELEMENT_25);
        this.registerVanilla(ELEMENT_26);
        this.registerVanilla(ELEMENT_27);
        this.registerVanilla(ELEMENT_28);
        this.registerVanilla(ELEMENT_29);
        this.registerVanilla(ELEMENT_30);
        this.registerVanilla(ELEMENT_31);
        this.registerVanilla(ELEMENT_32);
        this.registerVanilla(ELEMENT_33);
        this.registerVanilla(ELEMENT_34);
        this.registerVanilla(ELEMENT_35);
        this.registerVanilla(ELEMENT_36);
        this.registerVanilla(ELEMENT_37);
        this.registerVanilla(ELEMENT_38);
        this.registerVanilla(ELEMENT_39);
        this.registerVanilla(ELEMENT_40);
        this.registerVanilla(ELEMENT_41);
        this.registerVanilla(ELEMENT_42);
        this.registerVanilla(ELEMENT_43);
        this.registerVanilla(ELEMENT_44);
        this.registerVanilla(ELEMENT_45);
        this.registerVanilla(ELEMENT_46);
        this.registerVanilla(ELEMENT_47);
        this.registerVanilla(ELEMENT_48);
        this.registerVanilla(ELEMENT_49);
        this.registerVanilla(ELEMENT_50);
        this.registerVanilla(ELEMENT_51);
        this.registerVanilla(ELEMENT_52);
        this.registerVanilla(ELEMENT_53);
        this.registerVanilla(ELEMENT_54);
        this.registerVanilla(ELEMENT_55);
        this.registerVanilla(ELEMENT_56);
        this.registerVanilla(ELEMENT_57);
        this.registerVanilla(ELEMENT_58);
        this.registerVanilla(ELEMENT_59);
        this.registerVanilla(ELEMENT_60);
        this.registerVanilla(ELEMENT_61);
        this.registerVanilla(ELEMENT_62);
        this.registerVanilla(ELEMENT_63);
        this.registerVanilla(ELEMENT_64);
        this.registerVanilla(ELEMENT_65);
        this.registerVanilla(ELEMENT_66);
        this.registerVanilla(ELEMENT_67);
        this.registerVanilla(ELEMENT_68);
        this.registerVanilla(ELEMENT_69);
        this.registerVanilla(ELEMENT_70);
        this.registerVanilla(ELEMENT_71);
        this.registerVanilla(ELEMENT_72);
        this.registerVanilla(ELEMENT_73);
        this.registerVanilla(ELEMENT_74);
        this.registerVanilla(ELEMENT_75);
        this.registerVanilla(ELEMENT_76);
        this.registerVanilla(ELEMENT_77);
        this.registerVanilla(ELEMENT_78);
        this.registerVanilla(ELEMENT_79);
        this.registerVanilla(ELEMENT_80);
        this.registerVanilla(ELEMENT_81);
        this.registerVanilla(ELEMENT_82);
        this.registerVanilla(ELEMENT_83);
        this.registerVanilla(ELEMENT_84);
        this.registerVanilla(ELEMENT_85);
        this.registerVanilla(ELEMENT_86);
        this.registerVanilla(ELEMENT_87);
        this.registerVanilla(ELEMENT_88);
        this.registerVanilla(ELEMENT_89);
        this.registerVanilla(ELEMENT_90);
        this.registerVanilla(ELEMENT_91);
        this.registerVanilla(ELEMENT_92);
        this.registerVanilla(ELEMENT_93);
        this.registerVanilla(ELEMENT_94);
        this.registerVanilla(ELEMENT_95);
        this.registerVanilla(ELEMENT_96);
        this.registerVanilla(ELEMENT_97);
        this.registerVanilla(ELEMENT_98);
        this.registerVanilla(ELEMENT_99);
        this.registerVanilla(ELEMENT_100);
        this.registerVanilla(ELEMENT_101);
        this.registerVanilla(ELEMENT_102);
        this.registerVanilla(ELEMENT_103);
        this.registerVanilla(ELEMENT_104);
        this.registerVanilla(ELEMENT_105);
        this.registerVanilla(ELEMENT_106);
        this.registerVanilla(ELEMENT_107);
        this.registerVanilla(ELEMENT_108);
        this.registerVanilla(ELEMENT_109);
        this.registerVanilla(ELEMENT_110);
        this.registerVanilla(ELEMENT_111);
        this.registerVanilla(ELEMENT_112);
        this.registerVanilla(ELEMENT_113);
        this.registerVanilla(ELEMENT_114);
        this.registerVanilla(ELEMENT_115);
        this.registerVanilla(ELEMENT_116);
        this.registerVanilla(ELEMENT_117);
        this.registerVanilla(ELEMENT_118);
        this.registerVanilla(ELEMENT_CONSTRUCTOR);
        this.registerVanilla(EMERALD_BLOCK);
        this.registerVanilla(EMERALD_ORE);
        this.registerVanilla(ENCHANTING_TABLE)
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, ContainerBlockHandlers.ENCHANTING_TABLE);
        this.registerVanilla(ENDER_CHEST)
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, ContainerBlockHandlers.ENDER_CHEST);
        this.registerVanilla(END_BRICKS);
        this.registerVanilla(END_BRICK_STAIRS);
        this.registerVanilla(END_GATEWAY);
        this.registerVanilla(END_PORTAL);
        this.registerVanilla(END_PORTAL_FRAME);
        this.registerVanilla(END_ROD);
        this.registerVanilla(END_STONE);
        this.registerVanilla(END_STONE_BRICK_DOUBLE_SLAB);
        this.registerVanilla(END_STONE_BRICK_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(END_STONE_BRICK_DOUBLE_SLAB));
        this.registerVanilla(END_STONE_BRICK_WALL);
        this.registerVanilla(EXPOSED_CHISELED_COPPER);
        this.registerVanilla(EXPOSED_COPPER);
        this.registerVanilla(EXPOSED_COPPER_BARS);
        this.registerVanilla(EXPOSED_COPPER_BULB);
        this.registerVanilla(EXPOSED_COPPER_CHAIN);
        this.registerVanilla(EXPOSED_COPPER_CHEST);
        this.registerDoor(EXPOSED_COPPER_DOOR);
        this.registerVanilla(EXPOSED_COPPER_GOLEM_STATUE);
        this.registerVanilla(EXPOSED_COPPER_GRATE);
        this.registerVanilla(EXPOSED_COPPER_LANTERN);
        this.registerTrapdoor(EXPOSED_COPPER_TRAPDOOR);
        this.registerVanilla(EXPOSED_CUT_COPPER);
        this.registerVanilla(EXPOSED_CUT_COPPER_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(EXPOSED_DOUBLE_CUT_COPPER_SLAB));
        this.registerVanilla(EXPOSED_CUT_COPPER_STAIRS);
        this.registerVanilla(EXPOSED_DOUBLE_CUT_COPPER_SLAB);
        this.registerVanilla(EXPOSED_LIGHTNING_ROD);
        this.registerVanilla(FARMLAND);
        this.registerVanilla(FERN);
        this.registerVanilla(FIRE);
        this.registerVanilla(FIREFLY_BUSH);
        this.registerVanilla(FIRE_CORAL);
        this.registerVanilla(FIRE_CORAL_BLOCK);
        this.registerVanilla(FIRE_CORAL_FAN);
        this.registerVanilla(FIRE_CORAL_WALL_FAN);
        this.registerVanilla(FLETCHING_TABLE);
        this.registerVanilla(FLOWERING_AZALEA);
        this.registerVanilla(FLOWER_POT);
        this.registerVanilla(FLOWING_LAVA, FluidBlockSerializer.INSTANCE)
                .set(BlockComponents.REPLACEABLE, () -> true)
                .set(BlockComponents.LIQUID, () -> true);
        this.registerVanilla(FLOWING_WATER, FluidBlockSerializer.INSTANCE)
                .set(BlockComponents.REPLACEABLE, () -> true)
                .set(BlockComponents.LIQUID, () -> true);
        this.registerVanilla(FRAME);
        this.registerVanilla(FROG_SPAWN);
        this.registerVanilla(FROSTED_ICE);
        this.registerVanilla(FURNACE)
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, ContainerBlockHandlers.FURNACE);
        this.registerVanilla(GILDED_BLACKSTONE);
        this.registerVanilla(GLASS);
        this.registerVanilla(GLASS_PANE);
        this.registerVanilla(GLOWING_OBSIDIAN);
        this.registerVanilla(GLOWSTONE);
        this.registerVanilla(GLOW_FRAME);
        this.registerVanilla(GLOW_LICHEN);
        this.registerVanilla(GOLDEN_DANDELION);
        this.registerVanilla(GOLDEN_RAIL);
        this.registerVanilla(GOLD_BLOCK);
        this.registerVanilla(GOLD_ORE);
        this.registerVanilla(GRANITE);
        this.registerVanilla(GRANITE_DOUBLE_SLAB);
        this.registerVanilla(GRANITE_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(GRANITE_DOUBLE_SLAB));
        this.registerVanilla(GRANITE_STAIRS);
        this.registerVanilla(GRANITE_WALL);
        this.registerVanilla(GRASS_BLOCK);
        this.registerVanilla(GRASS_PATH);
        this.registerVanilla(GRAVEL);
        this.registerVanilla(GRAY_CANDLE);
        this.registerVanilla(GRAY_CANDLE_CAKE);
        this.registerVanilla(GRAY_CARPET);
        this.registerVanilla(GRAY_CONCRETE);
        this.registerVanilla(GRAY_CONCRETE_POWDER);
        this.registerVanilla(GRAY_GLAZED_TERRACOTTA);
        this.registerVanilla(GRAY_SHULKER_BOX)
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, ContainerBlockHandlers.SHULKER_BOX);
        this.registerVanilla(GRAY_STAINED_GLASS);
        this.registerVanilla(GRAY_STAINED_GLASS_PANE);
        this.registerVanilla(GRAY_TERRACOTTA);
        this.registerVanilla(GRAY_WOOL);
        this.registerVanilla(GREEN_CANDLE);
        this.registerVanilla(GREEN_CANDLE_CAKE);
        this.registerVanilla(GREEN_CARPET);
        this.registerVanilla(GREEN_CONCRETE);
        this.registerVanilla(GREEN_CONCRETE_POWDER);
        this.registerVanilla(GREEN_GLAZED_TERRACOTTA);
        this.registerVanilla(GREEN_SHULKER_BOX)
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, ContainerBlockHandlers.SHULKER_BOX);
        this.registerVanilla(GREEN_STAINED_GLASS);
        this.registerVanilla(GREEN_STAINED_GLASS_PANE);
        this.registerVanilla(GREEN_TERRACOTTA);
        this.registerVanilla(GREEN_WOOL);
        this.registerVanilla(GRINDSTONE)
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, ContainerBlockHandlers.GRINDSTONE);
        this.registerVanilla(HANGING_ROOTS);
        this.registerVanilla(HARDENED_CLAY);
        this.registerVanilla(HARD_BLACK_STAINED_GLASS);
        this.registerVanilla(HARD_BLACK_STAINED_GLASS_PANE);
        this.registerVanilla(HARD_BLUE_STAINED_GLASS);
        this.registerVanilla(HARD_BLUE_STAINED_GLASS_PANE);
        this.registerVanilla(HARD_BROWN_STAINED_GLASS);
        this.registerVanilla(HARD_BROWN_STAINED_GLASS_PANE);
        this.registerVanilla(HARD_CYAN_STAINED_GLASS);
        this.registerVanilla(HARD_CYAN_STAINED_GLASS_PANE);
        this.registerVanilla(HARD_GLASS);
        this.registerVanilla(HARD_GLASS_PANE);
        this.registerVanilla(HARD_GRAY_STAINED_GLASS);
        this.registerVanilla(HARD_GRAY_STAINED_GLASS_PANE);
        this.registerVanilla(HARD_GREEN_STAINED_GLASS);
        this.registerVanilla(HARD_GREEN_STAINED_GLASS_PANE);
        this.registerVanilla(HARD_LIGHT_BLUE_STAINED_GLASS);
        this.registerVanilla(HARD_LIGHT_BLUE_STAINED_GLASS_PANE);
        this.registerVanilla(HARD_LIGHT_GRAY_STAINED_GLASS);
        this.registerVanilla(HARD_LIGHT_GRAY_STAINED_GLASS_PANE);
        this.registerVanilla(HARD_LIME_STAINED_GLASS);
        this.registerVanilla(HARD_LIME_STAINED_GLASS_PANE);
        this.registerVanilla(HARD_MAGENTA_STAINED_GLASS);
        this.registerVanilla(HARD_MAGENTA_STAINED_GLASS_PANE);
        this.registerVanilla(HARD_ORANGE_STAINED_GLASS);
        this.registerVanilla(HARD_ORANGE_STAINED_GLASS_PANE);
        this.registerVanilla(HARD_PINK_STAINED_GLASS);
        this.registerVanilla(HARD_PINK_STAINED_GLASS_PANE);
        this.registerVanilla(HARD_PURPLE_STAINED_GLASS);
        this.registerVanilla(HARD_PURPLE_STAINED_GLASS_PANE);
        this.registerVanilla(HARD_RED_STAINED_GLASS);
        this.registerVanilla(HARD_RED_STAINED_GLASS_PANE);
        this.registerVanilla(HARD_WHITE_STAINED_GLASS);
        this.registerVanilla(HARD_WHITE_STAINED_GLASS_PANE);
        this.registerVanilla(HARD_YELLOW_STAINED_GLASS);
        this.registerVanilla(HARD_YELLOW_STAINED_GLASS_PANE);
        this.registerVanilla(HAY_BLOCK);
        this.registerVanilla(HEAVY_CORE);
        this.registerVanilla(HEAVY_WEIGHTED_PRESSURE_PLATE);
        this.registerVanilla(HONEYCOMB_BLOCK);
        this.registerVanilla(HONEY_BLOCK);
        this.registerVanilla(HOPPER)
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, ContainerBlockHandlers.HOPPER);
        this.registerVanilla(HORN_CORAL);
        this.registerVanilla(HORN_CORAL_BLOCK);
        this.registerVanilla(HORN_CORAL_FAN);
        this.registerVanilla(HORN_CORAL_WALL_FAN);
        this.registerVanilla(ICE);
        this.registerVanilla(INFESTED_CHISELED_STONE_BRICKS);
        this.registerVanilla(INFESTED_COBBLESTONE);
        this.registerVanilla(INFESTED_CRACKED_STONE_BRICKS);
        this.registerVanilla(INFESTED_DEEPSLATE);
        this.registerVanilla(INFESTED_MOSSY_STONE_BRICKS);
        this.registerVanilla(INFESTED_STONE);
        this.registerVanilla(INFESTED_STONE_BRICKS);
        this.registerVanilla(INFO_UPDATE);
        this.registerVanilla(INFO_UPDATE2);
        this.registerVanilla(INVISIBLE_BEDROCK);
        this.registerVanilla(IRON_BARS);
        this.registerVanilla(IRON_BLOCK);
        this.registerVanilla(IRON_CHAIN);
        this.registerDoor(IRON_DOOR);
        this.registerVanilla(IRON_ORE);
        this.registerTrapdoor(IRON_TRAPDOOR);
        this.registerVanilla(JIGSAW);
        this.registerVanilla(JUKEBOX);
        this.registerVanilla(JUNGLE_BUTTON);
        this.registerDoor(JUNGLE_DOOR);
        this.registerVanilla(JUNGLE_DOUBLE_SLAB);
        this.registerVanilla(JUNGLE_FENCE);
        this.registerVanilla(JUNGLE_FENCE_GATE);
        this.registerVanilla(JUNGLE_HANGING_SIGN);
        this.registerVanilla(JUNGLE_LEAVES);
        this.registerVanilla(JUNGLE_LOG);
        this.registerVanilla(JUNGLE_PLANKS);
        this.registerVanilla(JUNGLE_PRESSURE_PLATE);
        this.registerVanilla(JUNGLE_SAPLING);
        this.registerVanilla(JUNGLE_SHELF);
        this.registerVanilla(JUNGLE_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(JUNGLE_DOUBLE_SLAB));
        this.registerVanilla(JUNGLE_STAIRS);
        this.registerVanilla(JUNGLE_STANDING_SIGN);
        this.registerTrapdoor(JUNGLE_TRAPDOOR);
        this.registerVanilla(JUNGLE_WALL_SIGN);
        this.registerVanilla(JUNGLE_WOOD);
        this.registerVanilla(KELP);
        this.registerVanilla(LAB_TABLE);
        this.registerVanilla(LADDER);
        this.registerVanilla(LANTERN);
        this.registerVanilla(LAPIS_BLOCK);
        this.registerVanilla(LAPIS_ORE);
        this.registerVanilla(LARGE_AMETHYST_BUD);
        this.registerVanilla(LARGE_FERN);
        this.registerVanilla(LAVA, FluidBlockSerializer.INSTANCE)
                .set(BlockComponents.REPLACEABLE, () -> true)
                .set(BlockComponents.LIQUID, () -> true);
        this.registerVanilla(LEAF_LITTER);
        this.registerVanilla(LECTERN)
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, ContainerBlockHandlers.LECTERN);
        this.registerVanilla(LEVER);
        this.registerVanilla(LIGHTNING_ROD);
        this.registerVanilla(LIGHT_BLOCK_0);
        this.registerVanilla(LIGHT_BLOCK_1);
        this.registerVanilla(LIGHT_BLOCK_2);
        this.registerVanilla(LIGHT_BLOCK_3);
        this.registerVanilla(LIGHT_BLOCK_4);
        this.registerVanilla(LIGHT_BLOCK_5);
        this.registerVanilla(LIGHT_BLOCK_6);
        this.registerVanilla(LIGHT_BLOCK_7);
        this.registerVanilla(LIGHT_BLOCK_8);
        this.registerVanilla(LIGHT_BLOCK_9);
        this.registerVanilla(LIGHT_BLOCK_10);
        this.registerVanilla(LIGHT_BLOCK_11);
        this.registerVanilla(LIGHT_BLOCK_12);
        this.registerVanilla(LIGHT_BLOCK_13);
        this.registerVanilla(LIGHT_BLOCK_14);
        this.registerVanilla(LIGHT_BLOCK_15);
        this.registerVanilla(LIGHT_BLUE_CANDLE);
        this.registerVanilla(LIGHT_BLUE_CANDLE_CAKE);
        this.registerVanilla(LIGHT_BLUE_CARPET);
        this.registerVanilla(LIGHT_BLUE_CONCRETE);
        this.registerVanilla(LIGHT_BLUE_CONCRETE_POWDER);
        this.registerVanilla(LIGHT_BLUE_GLAZED_TERRACOTTA);
        this.registerVanilla(LIGHT_BLUE_SHULKER_BOX)
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, ContainerBlockHandlers.SHULKER_BOX);
        this.registerVanilla(LIGHT_BLUE_STAINED_GLASS);
        this.registerVanilla(LIGHT_BLUE_STAINED_GLASS_PANE);
        this.registerVanilla(LIGHT_BLUE_TERRACOTTA);
        this.registerVanilla(LIGHT_BLUE_WOOL);
        this.registerVanilla(LIGHT_GRAY_CANDLE);
        this.registerVanilla(LIGHT_GRAY_CANDLE_CAKE);
        this.registerVanilla(LIGHT_GRAY_CARPET);
        this.registerVanilla(LIGHT_GRAY_CONCRETE);
        this.registerVanilla(LIGHT_GRAY_CONCRETE_POWDER);
        this.registerVanilla(LIGHT_GRAY_GLAZED_TERRACOTTA);
        this.registerVanilla(LIGHT_GRAY_SHULKER_BOX)
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, ContainerBlockHandlers.SHULKER_BOX);
        this.registerVanilla(LIGHT_GRAY_STAINED_GLASS);
        this.registerVanilla(LIGHT_GRAY_STAINED_GLASS_PANE);
        this.registerVanilla(LIGHT_GRAY_TERRACOTTA);
        this.registerVanilla(LIGHT_GRAY_WOOL);
        this.registerVanilla(LIGHT_WEIGHTED_PRESSURE_PLATE);
        this.registerVanilla(LILAC);
        this.registerVanilla(LILY_OF_THE_VALLEY);
        this.registerVanilla(LIME_CANDLE);
        this.registerVanilla(LIME_CANDLE_CAKE);
        this.registerVanilla(LIME_CARPET);
        this.registerVanilla(LIME_CONCRETE);
        this.registerVanilla(LIME_CONCRETE_POWDER);
        this.registerVanilla(LIME_GLAZED_TERRACOTTA);
        this.registerVanilla(LIME_SHULKER_BOX)
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, ContainerBlockHandlers.SHULKER_BOX);
        this.registerVanilla(LIME_STAINED_GLASS);
        this.registerVanilla(LIME_STAINED_GLASS_PANE);
        this.registerVanilla(LIME_TERRACOTTA);
        this.registerVanilla(LIME_WOOL);
        this.registerVanilla(LIT_BLAST_FURNACE)
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, ContainerBlockHandlers.BLAST_FURNACE);
        this.registerVanilla(LIT_DEEPSLATE_REDSTONE_ORE);
        this.registerVanilla(LIT_FURNACE)
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, ContainerBlockHandlers.FURNACE);
        this.registerVanilla(LIT_PUMPKIN);
        this.registerVanilla(LIT_REDSTONE_LAMP);
        this.registerVanilla(LIT_REDSTONE_ORE);
        this.registerVanilla(LIT_SMOKER)
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, ContainerBlockHandlers.SMOKER);
        this.registerVanilla(LODESTONE);
        this.registerVanilla(LOOM)
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, ContainerBlockHandlers.LOOM);
        this.registerVanilla(MAGENTA_CANDLE);
        this.registerVanilla(MAGENTA_CANDLE_CAKE);
        this.registerVanilla(MAGENTA_CARPET);
        this.registerVanilla(MAGENTA_CONCRETE);
        this.registerVanilla(MAGENTA_CONCRETE_POWDER);
        this.registerVanilla(MAGENTA_GLAZED_TERRACOTTA);
        this.registerVanilla(MAGENTA_SHULKER_BOX)
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, ContainerBlockHandlers.SHULKER_BOX);
        this.registerVanilla(MAGENTA_STAINED_GLASS);
        this.registerVanilla(MAGENTA_STAINED_GLASS_PANE);
        this.registerVanilla(MAGENTA_TERRACOTTA);
        this.registerVanilla(MAGENTA_WOOL);
        this.registerVanilla(MAGMA);
        this.registerVanilla(MANGROVE_BUTTON);
        this.registerDoor(MANGROVE_DOOR);
        this.registerVanilla(MANGROVE_DOUBLE_SLAB);
        this.registerVanilla(MANGROVE_FENCE);
        this.registerVanilla(MANGROVE_FENCE_GATE);
        this.registerVanilla(MANGROVE_HANGING_SIGN);
        this.registerVanilla(MANGROVE_LEAVES);
        this.registerVanilla(MANGROVE_LOG);
        this.registerVanilla(MANGROVE_PLANKS);
        this.registerVanilla(MANGROVE_PRESSURE_PLATE);
        this.registerVanilla(MANGROVE_PROPAGULE);
        this.registerVanilla(MANGROVE_ROOTS);
        this.registerVanilla(MANGROVE_SHELF);
        this.registerVanilla(MANGROVE_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(MANGROVE_DOUBLE_SLAB));
        this.registerVanilla(MANGROVE_STAIRS);
        this.registerVanilla(MANGROVE_STANDING_SIGN);
        this.registerTrapdoor(MANGROVE_TRAPDOOR);
        this.registerVanilla(MANGROVE_WALL_SIGN);
        this.registerVanilla(MANGROVE_WOOD);
        this.registerVanilla(MATERIAL_REDUCER);
        this.registerVanilla(MEDIUM_AMETHYST_BUD);
        this.registerVanilla(MELON_BLOCK);
        this.registerVanilla(MELON_STEM);
        this.registerVanilla(MOB_SPAWNER);
        this.registerVanilla(MOSSY_COBBLESTONE);
        this.registerVanilla(MOSSY_COBBLESTONE_DOUBLE_SLAB);
        this.registerVanilla(MOSSY_COBBLESTONE_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(MOSSY_COBBLESTONE_DOUBLE_SLAB));
        this.registerVanilla(MOSSY_COBBLESTONE_STAIRS);
        this.registerVanilla(MOSSY_COBBLESTONE_WALL);
        this.registerVanilla(MOSSY_STONE_BRICKS);
        this.registerVanilla(MOSSY_STONE_BRICK_DOUBLE_SLAB);
        this.registerVanilla(MOSSY_STONE_BRICK_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(MOSSY_STONE_BRICK_DOUBLE_SLAB));
        this.registerVanilla(MOSSY_STONE_BRICK_STAIRS);
        this.registerVanilla(MOSSY_STONE_BRICK_WALL);
        this.registerVanilla(MOSS_BLOCK);
        this.registerVanilla(MOSS_CARPET);
        this.registerVanilla(MOVING_BLOCK);
        this.registerVanilla(MUD);
        this.registerVanilla(MUDDY_MANGROVE_ROOTS);
        this.registerVanilla(MUD_BRICKS);
        this.registerVanilla(MUD_BRICK_DOUBLE_SLAB);
        this.registerVanilla(MUD_BRICK_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(MUD_BRICK_DOUBLE_SLAB));
        this.registerVanilla(MUD_BRICK_STAIRS);
        this.registerVanilla(MUD_BRICK_WALL);
        this.registerVanilla(MUSHROOM_STEM);
        this.registerVanilla(MYCELIUM);
        this.registerVanilla(NETHERITE_BLOCK);
        this.registerVanilla(NETHERRACK);
        this.registerVanilla(NETHERREACTOR);
        this.registerVanilla(NETHER_BRICK);
        this.registerVanilla(NETHER_BRICK_DOUBLE_SLAB);
        this.registerVanilla(NETHER_BRICK_FENCE);
        this.registerVanilla(NETHER_BRICK_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(NETHER_BRICK_DOUBLE_SLAB));
        this.registerVanilla(NETHER_BRICK_STAIRS);
        this.registerVanilla(NETHER_BRICK_WALL);
        this.registerVanilla(NETHER_GOLD_ORE);
        this.registerVanilla(NETHER_SPROUTS);
        this.registerVanilla(NETHER_WART);
        this.registerVanilla(NETHER_WART_BLOCK);
        this.registerVanilla(NOTE_BLOCK);
        this.registerVanilla(OAK_BUTTON);
        this.registerDoor(OAK_DOOR);
        this.registerVanilla(OAK_DOUBLE_SLAB);
        this.registerVanilla(OAK_FENCE);
        this.registerVanilla(OAK_FENCE_GATE);
        this.registerVanilla(OAK_HANGING_SIGN);
        this.registerVanilla(OAK_LEAVES);
        this.registerVanilla(OAK_LOG);
        this.registerVanilla(OAK_PLANKS);
        this.registerVanilla(OAK_PRESSURE_PLATE);
        this.registerVanilla(OAK_SAPLING);
        this.registerVanilla(OAK_SHELF);
        this.registerVanilla(OAK_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(OAK_DOUBLE_SLAB));
        this.registerVanilla(OAK_STAIRS);
        this.registerVanilla(OAK_STANDING_SIGN);
        this.registerTrapdoor(OAK_TRAPDOOR);
        this.registerVanilla(OAK_WALL_SIGN);
        this.registerVanilla(OAK_WOOD);
        this.registerVanilla(OBSERVER);
        this.registerVanilla(OBSIDIAN);
        this.registerVanilla(OCHRE_FROGLIGHT);
        this.registerVanilla(OPEN_EYEBLOSSOM);
        this.registerVanilla(ORANGE_CANDLE);
        this.registerVanilla(ORANGE_CANDLE_CAKE);
        this.registerVanilla(ORANGE_CARPET);
        this.registerVanilla(ORANGE_CONCRETE);
        this.registerVanilla(ORANGE_CONCRETE_POWDER);
        this.registerVanilla(ORANGE_GLAZED_TERRACOTTA);
        this.registerVanilla(ORANGE_SHULKER_BOX)
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, ContainerBlockHandlers.SHULKER_BOX);
        this.registerVanilla(ORANGE_STAINED_GLASS);
        this.registerVanilla(ORANGE_STAINED_GLASS_PANE);
        this.registerVanilla(ORANGE_TERRACOTTA);
        this.registerVanilla(ORANGE_TULIP);
        this.registerVanilla(ORANGE_WOOL);
        this.registerVanilla(OXEYE_DAISY);
        this.registerVanilla(OXIDIZED_CHISELED_COPPER);
        this.registerVanilla(OXIDIZED_COPPER);
        this.registerVanilla(OXIDIZED_COPPER_BARS);
        this.registerVanilla(OXIDIZED_COPPER_BULB);
        this.registerVanilla(OXIDIZED_COPPER_CHAIN);
        this.registerVanilla(OXIDIZED_COPPER_CHEST);
        this.registerDoor(OXIDIZED_COPPER_DOOR);
        this.registerVanilla(OXIDIZED_COPPER_GOLEM_STATUE);
        this.registerVanilla(OXIDIZED_COPPER_GRATE);
        this.registerVanilla(OXIDIZED_COPPER_LANTERN);
        this.registerTrapdoor(OXIDIZED_COPPER_TRAPDOOR);
        this.registerVanilla(OXIDIZED_CUT_COPPER);
        this.registerVanilla(OXIDIZED_CUT_COPPER_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(OXIDIZED_DOUBLE_CUT_COPPER_SLAB));
        this.registerVanilla(OXIDIZED_CUT_COPPER_STAIRS);
        this.registerVanilla(OXIDIZED_DOUBLE_CUT_COPPER_SLAB);
        this.registerVanilla(OXIDIZED_LIGHTNING_ROD);
        this.registerVanilla(PACKED_ICE);
        this.registerVanilla(PACKED_MUD);
        this.registerVanilla(PALE_HANGING_MOSS);
        this.registerVanilla(PALE_MOSS_BLOCK);
        this.registerVanilla(PALE_MOSS_CARPET);
        this.registerVanilla(PALE_OAK_BUTTON);
        this.registerDoor(PALE_OAK_DOOR);
        this.registerVanilla(PALE_OAK_DOUBLE_SLAB);
        this.registerVanilla(PALE_OAK_FENCE);
        this.registerVanilla(PALE_OAK_FENCE_GATE);
        this.registerVanilla(PALE_OAK_HANGING_SIGN);
        this.registerVanilla(PALE_OAK_LEAVES);
        this.registerVanilla(PALE_OAK_LOG);
        this.registerVanilla(PALE_OAK_PLANKS);
        this.registerVanilla(PALE_OAK_PRESSURE_PLATE);
        this.registerVanilla(PALE_OAK_SAPLING);
        this.registerVanilla(PALE_OAK_SHELF);
        this.registerVanilla(PALE_OAK_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(PALE_OAK_DOUBLE_SLAB));
        this.registerVanilla(PALE_OAK_STAIRS);
        this.registerVanilla(PALE_OAK_STANDING_SIGN);
        this.registerTrapdoor(PALE_OAK_TRAPDOOR);
        this.registerVanilla(PALE_OAK_WALL_SIGN);
        this.registerVanilla(PALE_OAK_WOOD);
        this.registerVanilla(PEARLESCENT_FROGLIGHT);
        this.registerVanilla(PEONY);
        this.registerVanilla(PETRIFIED_OAK_DOUBLE_SLAB);
        this.registerVanilla(PETRIFIED_OAK_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(PETRIFIED_OAK_DOUBLE_SLAB));
        this.registerVanilla(PIGLIN_HEAD);
        this.registerVanilla(PINK_CANDLE);
        this.registerVanilla(PINK_CANDLE_CAKE);
        this.registerVanilla(PINK_CARPET);
        this.registerVanilla(PINK_CONCRETE);
        this.registerVanilla(PINK_CONCRETE_POWDER);
        this.registerVanilla(PINK_GLAZED_TERRACOTTA);
        this.registerVanilla(PINK_PETALS);
        this.registerVanilla(PINK_SHULKER_BOX)
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, ContainerBlockHandlers.SHULKER_BOX);
        this.registerVanilla(PINK_STAINED_GLASS);
        this.registerVanilla(PINK_STAINED_GLASS_PANE);
        this.registerVanilla(PINK_TERRACOTTA);
        this.registerVanilla(PINK_TULIP);
        this.registerVanilla(PINK_WOOL);
        this.registerVanilla(PISTON);
        this.registerVanilla(PISTON_ARM_COLLISION);
        this.registerVanilla(PITCHER_CROP);
        this.registerVanilla(PITCHER_PLANT);
        this.registerVanilla(PLAYER_HEAD);
        this.registerVanilla(PODZOL);
        this.registerVanilla(POINTED_DRIPSTONE);
        this.registerVanilla(POLISHED_ANDESITE);
        this.registerVanilla(POLISHED_ANDESITE_DOUBLE_SLAB);
        this.registerVanilla(POLISHED_ANDESITE_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(POLISHED_ANDESITE_DOUBLE_SLAB));
        this.registerVanilla(POLISHED_ANDESITE_STAIRS);
        this.registerVanilla(POLISHED_BASALT);
        this.registerVanilla(POLISHED_BLACKSTONE);
        this.registerVanilla(POLISHED_BLACKSTONE_BRICKS);
        this.registerVanilla(POLISHED_BLACKSTONE_BRICK_DOUBLE_SLAB);
        this.registerVanilla(POLISHED_BLACKSTONE_BRICK_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(POLISHED_BLACKSTONE_BRICK_DOUBLE_SLAB));
        this.registerVanilla(POLISHED_BLACKSTONE_BRICK_STAIRS);
        this.registerVanilla(POLISHED_BLACKSTONE_BRICK_WALL);
        this.registerVanilla(POLISHED_BLACKSTONE_BUTTON);
        this.registerVanilla(POLISHED_BLACKSTONE_DOUBLE_SLAB);
        this.registerVanilla(POLISHED_BLACKSTONE_PRESSURE_PLATE);
        this.registerVanilla(POLISHED_BLACKSTONE_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(POLISHED_BLACKSTONE_DOUBLE_SLAB));
        this.registerVanilla(POLISHED_BLACKSTONE_STAIRS);
        this.registerVanilla(POLISHED_BLACKSTONE_WALL);
        this.registerVanilla(POLISHED_CINNABAR);
        this.registerVanilla(POLISHED_CINNABAR_DOUBLE_SLAB);
        this.registerVanilla(POLISHED_CINNABAR_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(POLISHED_CINNABAR_DOUBLE_SLAB));
        this.registerVanilla(POLISHED_CINNABAR_STAIRS);
        this.registerVanilla(POLISHED_CINNABAR_WALL);
        this.registerVanilla(POLISHED_DEEPSLATE);
        this.registerVanilla(POLISHED_DEEPSLATE_DOUBLE_SLAB);
        this.registerVanilla(POLISHED_DEEPSLATE_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(POLISHED_DEEPSLATE_DOUBLE_SLAB));
        this.registerVanilla(POLISHED_DEEPSLATE_STAIRS);
        this.registerVanilla(POLISHED_DEEPSLATE_WALL);
        this.registerVanilla(POLISHED_DIORITE);
        this.registerVanilla(POLISHED_DIORITE_DOUBLE_SLAB);
        this.registerVanilla(POLISHED_DIORITE_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(POLISHED_DIORITE_DOUBLE_SLAB));
        this.registerVanilla(POLISHED_DIORITE_STAIRS);
        this.registerVanilla(POLISHED_GRANITE);
        this.registerVanilla(POLISHED_GRANITE_DOUBLE_SLAB);
        this.registerVanilla(POLISHED_GRANITE_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(POLISHED_GRANITE_DOUBLE_SLAB));
        this.registerVanilla(POLISHED_GRANITE_STAIRS);
        this.registerVanilla(POLISHED_SULFUR);
        this.registerVanilla(POLISHED_SULFUR_DOUBLE_SLAB);
        this.registerVanilla(POLISHED_SULFUR_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(POLISHED_SULFUR_DOUBLE_SLAB));
        this.registerVanilla(POLISHED_SULFUR_STAIRS);
        this.registerVanilla(POLISHED_SULFUR_WALL);
        this.registerVanilla(POLISHED_TUFF);
        this.registerVanilla(POLISHED_TUFF_DOUBLE_SLAB);
        this.registerVanilla(POLISHED_TUFF_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(POLISHED_TUFF_DOUBLE_SLAB));
        this.registerVanilla(POLISHED_TUFF_STAIRS);
        this.registerVanilla(POLISHED_TUFF_WALL);
        this.registerVanilla(POPPY);
        this.registerVanilla(PORTAL)
                .set(BlockComponents.ON_NEIGHBOUR_CHANGED, PortalBlockHandlers.ON_NEIGHBOUR_CHANGED)
                .set(BlockComponents.CAN_RANDOM_TICK, () -> true)
                .set(BlockComponents.ON_RANDOM_TICK, PortalBlockHandlers.ON_RANDOM_TICK);
        this.registerVanilla(POTATOES);
        this.registerVanilla(POTENT_SULFUR);
        this.registerVanilla(POWDER_SNOW);
        this.registerVanilla(POWERED_COMPARATOR);
        this.registerVanilla(POWERED_REPEATER);
        this.registerVanilla(PRISMARINE);
        this.registerVanilla(PRISMARINE_BRICKS);
        this.registerVanilla(PRISMARINE_BRICKS_STAIRS);
        this.registerVanilla(PRISMARINE_BRICK_DOUBLE_SLAB);
        this.registerVanilla(PRISMARINE_BRICK_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(PRISMARINE_BRICK_DOUBLE_SLAB));
        this.registerVanilla(PRISMARINE_DOUBLE_SLAB);
        this.registerVanilla(PRISMARINE_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(PRISMARINE_DOUBLE_SLAB));
        this.registerVanilla(PRISMARINE_STAIRS);
        this.registerVanilla(PRISMARINE_WALL);
        this.registerVanilla(PUMPKIN);
        this.registerVanilla(PUMPKIN_STEM);
        this.registerVanilla(PURPLE_CANDLE);
        this.registerVanilla(PURPLE_CANDLE_CAKE);
        this.registerVanilla(PURPLE_CARPET);
        this.registerVanilla(PURPLE_CONCRETE);
        this.registerVanilla(PURPLE_CONCRETE_POWDER);
        this.registerVanilla(PURPLE_GLAZED_TERRACOTTA);
        this.registerVanilla(PURPLE_SHULKER_BOX)
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, ContainerBlockHandlers.SHULKER_BOX);
        this.registerVanilla(PURPLE_STAINED_GLASS);
        this.registerVanilla(PURPLE_STAINED_GLASS_PANE);
        this.registerVanilla(PURPLE_TERRACOTTA);
        this.registerVanilla(PURPLE_WOOL);
        this.registerVanilla(PURPUR_BLOCK);
        this.registerVanilla(PURPUR_DOUBLE_SLAB);
        this.registerVanilla(PURPUR_PILLAR);
        this.registerVanilla(PURPUR_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(PURPUR_DOUBLE_SLAB));
        this.registerVanilla(PURPUR_STAIRS);
        this.registerVanilla(QUARTZ_BLOCK);
        this.registerVanilla(QUARTZ_BRICKS);
        this.registerVanilla(QUARTZ_DOUBLE_SLAB);
        this.registerVanilla(QUARTZ_ORE);
        this.registerVanilla(QUARTZ_PILLAR);
        this.registerVanilla(QUARTZ_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(QUARTZ_DOUBLE_SLAB));
        this.registerVanilla(QUARTZ_STAIRS);
        this.registerVanilla(RAIL);
        this.registerVanilla(RAW_COPPER_BLOCK);
        this.registerVanilla(RAW_GOLD_BLOCK);
        this.registerVanilla(RAW_IRON_BLOCK);
        this.registerVanilla(REDSTONE_BLOCK);
        this.registerVanilla(REDSTONE_LAMP);
        this.registerVanilla(REDSTONE_ORE);
        this.registerVanilla(REDSTONE_TORCH);
        this.registerVanilla(REDSTONE_WIRE);
        this.registerVanilla(RED_CANDLE);
        this.registerVanilla(RED_CANDLE_CAKE);
        this.registerVanilla(RED_CARPET);
        this.registerVanilla(RED_CONCRETE);
        this.registerVanilla(RED_CONCRETE_POWDER);
        this.registerVanilla(RED_GLAZED_TERRACOTTA);
        this.registerVanilla(RED_MUSHROOM);
        this.registerVanilla(RED_MUSHROOM_BLOCK);
        this.registerVanilla(RED_NETHER_BRICK);
        this.registerVanilla(RED_NETHER_BRICK_DOUBLE_SLAB);
        this.registerVanilla(RED_NETHER_BRICK_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(RED_NETHER_BRICK_DOUBLE_SLAB));
        this.registerVanilla(RED_NETHER_BRICK_STAIRS);
        this.registerVanilla(RED_NETHER_BRICK_WALL);
        this.registerVanilla(RED_SAND);
        this.registerVanilla(RED_SANDSTONE);
        this.registerVanilla(RED_SANDSTONE_DOUBLE_SLAB);
        this.registerVanilla(RED_SANDSTONE_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(RED_SANDSTONE_DOUBLE_SLAB));
        this.registerVanilla(RED_SANDSTONE_STAIRS);
        this.registerVanilla(RED_SANDSTONE_WALL);
        this.registerVanilla(RED_SHULKER_BOX)
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, ContainerBlockHandlers.SHULKER_BOX);
        this.registerVanilla(RED_STAINED_GLASS);
        this.registerVanilla(RED_STAINED_GLASS_PANE);
        this.registerVanilla(RED_TERRACOTTA);
        this.registerVanilla(RED_TULIP);
        this.registerVanilla(RED_WOOL);
        this.registerVanilla(REEDS);
        this.registerVanilla(REINFORCED_DEEPSLATE);
        this.registerVanilla(REPEATING_COMMAND_BLOCK);
        this.registerVanilla(RESERVED6);
        this.registerVanilla(RESIN_BLOCK);
        this.registerVanilla(RESIN_BRICKS);
        this.registerVanilla(RESIN_BRICK_DOUBLE_SLAB);
        this.registerVanilla(RESIN_BRICK_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(RESIN_BRICK_DOUBLE_SLAB));
        this.registerVanilla(RESIN_BRICK_STAIRS);
        this.registerVanilla(RESIN_BRICK_WALL);
        this.registerVanilla(RESIN_CLUMP);
        this.registerVanilla(RESPAWN_ANCHOR)
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, RespawnAnchorBlockHandlers.RESPAWN_ANCHOR);
        this.registerVanilla(ROSE_BUSH);
        this.registerVanilla(SAND);
        this.registerVanilla(SANDSTONE);
        this.registerVanilla(SANDSTONE_DOUBLE_SLAB);
        this.registerVanilla(SANDSTONE_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(SANDSTONE_DOUBLE_SLAB));
        this.registerVanilla(SANDSTONE_STAIRS);
        this.registerVanilla(SANDSTONE_WALL);
        this.registerVanilla(SCAFFOLDING);
        this.registerVanilla(SCULK);
        this.registerVanilla(SCULK_CATALYST);
        this.registerVanilla(SCULK_SENSOR);
        this.registerVanilla(SCULK_SHRIEKER);
        this.registerVanilla(SCULK_VEIN);
        this.registerVanilla(SEAGRASS);
        this.registerVanilla(SEA_LANTERN);
        this.registerVanilla(SEA_PICKLE);
        this.registerVanilla(SHORT_DRY_GRASS);
        this.registerVanilla(SHORT_GRASS);
        this.registerVanilla(SHROOMLIGHT);
        this.registerVanilla(SKELETON_SKULL);
        this.registerVanilla(SLIME);
        this.registerVanilla(SMALL_AMETHYST_BUD);
        this.registerVanilla(SMALL_DRIPLEAF_BLOCK);
        this.registerVanilla(SMITHING_TABLE)
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, ContainerBlockHandlers.SMITHING_TABLE);
        this.registerVanilla(SMOKER)
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, ContainerBlockHandlers.SMOKER);
        this.registerVanilla(SMOOTH_BASALT);
        this.registerVanilla(SMOOTH_QUARTZ);
        this.registerVanilla(SMOOTH_QUARTZ_DOUBLE_SLAB);
        this.registerVanilla(SMOOTH_QUARTZ_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(SMOOTH_QUARTZ_DOUBLE_SLAB));
        this.registerVanilla(SMOOTH_QUARTZ_STAIRS);
        this.registerVanilla(SMOOTH_RED_SANDSTONE);
        this.registerVanilla(SMOOTH_RED_SANDSTONE_DOUBLE_SLAB);
        this.registerVanilla(SMOOTH_RED_SANDSTONE_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(SMOOTH_RED_SANDSTONE_DOUBLE_SLAB));
        this.registerVanilla(SMOOTH_RED_SANDSTONE_STAIRS);
        this.registerVanilla(SMOOTH_SANDSTONE);
        this.registerVanilla(SMOOTH_SANDSTONE_DOUBLE_SLAB);
        this.registerVanilla(SMOOTH_SANDSTONE_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(SMOOTH_SANDSTONE_DOUBLE_SLAB));
        this.registerVanilla(SMOOTH_SANDSTONE_STAIRS);
        this.registerVanilla(SMOOTH_STONE);
        this.registerVanilla(SMOOTH_STONE_DOUBLE_SLAB);
        this.registerVanilla(SMOOTH_STONE_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(SMOOTH_STONE_DOUBLE_SLAB));
        this.registerVanilla(SNIFFER_EGG);
        this.registerVanilla(SNOW);
        this.registerVanilla(SNOW_LAYER);
        this.registerVanilla(SOUL_CAMPFIRE);
        this.registerVanilla(SOUL_FIRE);
        this.registerVanilla(SOUL_LANTERN);
        this.registerVanilla(SOUL_SAND);
        this.registerVanilla(SOUL_SOIL);
        this.registerVanilla(SOUL_TORCH);
        this.registerVanilla(SPONGE);
        this.registerVanilla(SPORE_BLOSSOM);
        this.registerVanilla(SPRUCE_BUTTON);
        this.registerDoor(SPRUCE_DOOR);
        this.registerVanilla(SPRUCE_DOUBLE_SLAB);
        this.registerVanilla(SPRUCE_FENCE);
        this.registerVanilla(SPRUCE_FENCE_GATE);
        this.registerVanilla(SPRUCE_HANGING_SIGN);
        this.registerVanilla(SPRUCE_LEAVES);
        this.registerVanilla(SPRUCE_LOG);
        this.registerVanilla(SPRUCE_PLANKS);
        this.registerVanilla(SPRUCE_PRESSURE_PLATE);
        this.registerVanilla(SPRUCE_SAPLING);
        this.registerVanilla(SPRUCE_SHELF);
        this.registerVanilla(SPRUCE_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(SPRUCE_DOUBLE_SLAB));
        this.registerVanilla(SPRUCE_STAIRS);
        this.registerVanilla(SPRUCE_STANDING_SIGN);
        this.registerTrapdoor(SPRUCE_TRAPDOOR);
        this.registerVanilla(SPRUCE_WALL_SIGN);
        this.registerVanilla(SPRUCE_WOOD);
        this.registerVanilla(STANDING_BANNER);
        this.registerVanilla(STICKY_PISTON);
        this.registerVanilla(STICKY_PISTON_ARM_COLLISION);
        this.registerVanilla(STONE);
        this.registerVanilla(STONECUTTER)
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, ContainerBlockHandlers.STONECUTTER);
        this.registerVanilla(STONECUTTER_BLOCK)
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, ContainerBlockHandlers.STONECUTTER);
        this.registerVanilla(STONE_BRICKS);
        this.registerVanilla(STONE_BRICK_DOUBLE_SLAB);
        this.registerVanilla(STONE_BRICK_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(STONE_BRICK_DOUBLE_SLAB));
        this.registerVanilla(STONE_BRICK_STAIRS);
        this.registerVanilla(STONE_BRICK_WALL);
        this.registerVanilla(STONE_BUTTON);
        this.registerVanilla(STONE_DOUBLE_SLAB);
        this.registerVanilla(STONE_PRESSURE_PLATE);
        this.registerVanilla(STONE_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(STONE_DOUBLE_SLAB));
        this.registerVanilla(STONE_STAIRS);
        this.registerVanilla(STRIPPED_ACACIA_LOG);
        this.registerVanilla(STRIPPED_ACACIA_WOOD);
        this.registerVanilla(STRIPPED_BAMBOO_BLOCK);
        this.registerVanilla(STRIPPED_BIRCH_LOG);
        this.registerVanilla(STRIPPED_BIRCH_WOOD);
        this.registerVanilla(STRIPPED_CHERRY_LOG);
        this.registerVanilla(STRIPPED_CHERRY_WOOD);
        this.registerVanilla(STRIPPED_CRIMSON_HYPHAE);
        this.registerVanilla(STRIPPED_CRIMSON_STEM);
        this.registerVanilla(STRIPPED_DARK_OAK_LOG);
        this.registerVanilla(STRIPPED_DARK_OAK_WOOD);
        this.registerVanilla(STRIPPED_JUNGLE_LOG);
        this.registerVanilla(STRIPPED_JUNGLE_WOOD);
        this.registerVanilla(STRIPPED_MANGROVE_LOG);
        this.registerVanilla(STRIPPED_MANGROVE_WOOD);
        this.registerVanilla(STRIPPED_OAK_LOG);
        this.registerVanilla(STRIPPED_OAK_WOOD);
        this.registerVanilla(STRIPPED_PALE_OAK_LOG);
        this.registerVanilla(STRIPPED_PALE_OAK_WOOD);
        this.registerVanilla(STRIPPED_SPRUCE_LOG);
        this.registerVanilla(STRIPPED_SPRUCE_WOOD);
        this.registerVanilla(STRIPPED_WARPED_HYPHAE);
        this.registerVanilla(STRIPPED_WARPED_STEM);
        this.registerVanilla(STRUCTURE_BLOCK);
        this.registerVanilla(STRUCTURE_VOID);
        this.registerVanilla(SULFUR);
        this.registerVanilla(SULFUR_BRICKS);
        this.registerVanilla(SULFUR_BRICK_DOUBLE_SLAB);
        this.registerVanilla(SULFUR_BRICK_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(SULFUR_BRICK_DOUBLE_SLAB));
        this.registerVanilla(SULFUR_BRICK_STAIRS);
        this.registerVanilla(SULFUR_BRICK_WALL);
        this.registerVanilla(SULFUR_DOUBLE_SLAB);
        this.registerVanilla(SULFUR_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(SULFUR_DOUBLE_SLAB));
        this.registerVanilla(SULFUR_STAIRS);
        this.registerVanilla(SULFUR_WALL);
        this.registerVanilla(SUNFLOWER);
        this.registerVanilla(SUSPICIOUS_GRAVEL);
        this.registerVanilla(SUSPICIOUS_SAND);
        this.registerVanilla(SWEET_BERRY_BUSH);
        this.registerVanilla(TALL_DRY_GRASS);
        this.registerVanilla(TALL_GRASS);
        this.registerVanilla(TARGET);
        this.registerVanilla(TINTED_GLASS);
        this.registerVanilla(TNT);
        this.registerVanilla(TORCH);
        this.registerVanilla(TORCHFLOWER);
        this.registerVanilla(TORCHFLOWER_CROP);
        this.registerVanilla(TRAPPED_CHEST)
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, ContainerBlockHandlers.TRAPPED_CHEST);
        this.registerVanilla(TRIAL_SPAWNER);
        this.registerVanilla(TRIPWIRE_HOOK);
        this.registerVanilla(TRIP_WIRE);
        this.registerVanilla(TUBE_CORAL);
        this.registerVanilla(TUBE_CORAL_BLOCK);
        this.registerVanilla(TUBE_CORAL_FAN);
        this.registerVanilla(TUBE_CORAL_WALL_FAN);
        this.registerVanilla(TUFF);
        this.registerVanilla(TUFF_BRICKS);
        this.registerVanilla(TUFF_BRICK_DOUBLE_SLAB);
        this.registerVanilla(TUFF_BRICK_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(TUFF_BRICK_DOUBLE_SLAB));
        this.registerVanilla(TUFF_BRICK_STAIRS);
        this.registerVanilla(TUFF_BRICK_WALL);
        this.registerVanilla(TUFF_DOUBLE_SLAB);
        this.registerVanilla(TUFF_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(TUFF_DOUBLE_SLAB));
        this.registerVanilla(TUFF_STAIRS);
        this.registerVanilla(TUFF_WALL);
        this.registerVanilla(TURTLE_EGG);
        this.registerVanilla(TWISTING_VINES);
        this.registerVanilla(UNDERWATER_TNT);
        this.registerVanilla(UNDERWATER_TORCH);
        this.registerVanilla(UNDYED_SHULKER_BOX)
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, ContainerBlockHandlers.SHULKER_BOX);
        this.registerVanilla(UNKNOWN);
        this.registerVanilla(UNLIT_REDSTONE_TORCH);
        this.registerVanilla(UNPOWERED_COMPARATOR);
        this.registerVanilla(UNPOWERED_REPEATER);
        this.registerVanilla(VAULT);
        this.registerVanilla(VERDANT_FROGLIGHT);
        this.registerVanilla(VINE);
        this.registerVanilla(WALL_BANNER);
        this.registerVanilla(WARPED_BUTTON);
        this.registerDoor(WARPED_DOOR);
        this.registerVanilla(WARPED_DOUBLE_SLAB);
        this.registerVanilla(WARPED_FENCE);
        this.registerVanilla(WARPED_FENCE_GATE);
        this.registerVanilla(WARPED_FUNGUS);
        this.registerVanilla(WARPED_HANGING_SIGN);
        this.registerVanilla(WARPED_HYPHAE);
        this.registerVanilla(WARPED_NYLIUM);
        this.registerVanilla(WARPED_PLANKS);
        this.registerVanilla(WARPED_PRESSURE_PLATE);
        this.registerVanilla(WARPED_ROOTS);
        this.registerVanilla(WARPED_SHELF);
        this.registerVanilla(WARPED_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(WARPED_DOUBLE_SLAB));
        this.registerVanilla(WARPED_STAIRS);
        this.registerVanilla(WARPED_STANDING_SIGN);
        this.registerVanilla(WARPED_STEM);
        this.registerTrapdoor(WARPED_TRAPDOOR);
        this.registerVanilla(WARPED_WALL_SIGN);
        this.registerVanilla(WARPED_WART_BLOCK);
        this.registerVanilla(WATER, FluidBlockSerializer.INSTANCE)
                .set(BlockComponents.REPLACEABLE, () -> true)
                .set(BlockComponents.LIQUID, () -> true);
        this.registerVanilla(WATERLILY);
        this.registerVanilla(WAXED_CHISELED_COPPER);
        this.registerVanilla(WAXED_COPPER);
        this.registerVanilla(WAXED_COPPER_BARS);
        this.registerVanilla(WAXED_COPPER_BULB);
        this.registerVanilla(WAXED_COPPER_CHAIN);
        this.registerVanilla(WAXED_COPPER_CHEST);
        this.registerDoor(WAXED_COPPER_DOOR);
        this.registerVanilla(WAXED_COPPER_GOLEM_STATUE);
        this.registerVanilla(WAXED_COPPER_GRATE);
        this.registerVanilla(WAXED_COPPER_LANTERN);
        this.registerTrapdoor(WAXED_COPPER_TRAPDOOR);
        this.registerVanilla(WAXED_CUT_COPPER);
        this.registerVanilla(WAXED_CUT_COPPER_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(WAXED_DOUBLE_CUT_COPPER_SLAB));
        this.registerVanilla(WAXED_CUT_COPPER_STAIRS);
        this.registerVanilla(WAXED_DOUBLE_CUT_COPPER_SLAB);
        this.registerVanilla(WAXED_EXPOSED_CHISELED_COPPER);
        this.registerVanilla(WAXED_EXPOSED_COPPER);
        this.registerVanilla(WAXED_EXPOSED_COPPER_BARS);
        this.registerVanilla(WAXED_EXPOSED_COPPER_BULB);
        this.registerVanilla(WAXED_EXPOSED_COPPER_CHAIN);
        this.registerVanilla(WAXED_EXPOSED_COPPER_CHEST);
        this.registerDoor(WAXED_EXPOSED_COPPER_DOOR);
        this.registerVanilla(WAXED_EXPOSED_COPPER_GOLEM_STATUE);
        this.registerVanilla(WAXED_EXPOSED_COPPER_GRATE);
        this.registerVanilla(WAXED_EXPOSED_COPPER_LANTERN);
        this.registerTrapdoor(WAXED_EXPOSED_COPPER_TRAPDOOR);
        this.registerVanilla(WAXED_EXPOSED_CUT_COPPER);
        this.registerVanilla(WAXED_EXPOSED_CUT_COPPER_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(WAXED_EXPOSED_DOUBLE_CUT_COPPER_SLAB));
        this.registerVanilla(WAXED_EXPOSED_CUT_COPPER_STAIRS);
        this.registerVanilla(WAXED_EXPOSED_DOUBLE_CUT_COPPER_SLAB);
        this.registerVanilla(WAXED_EXPOSED_LIGHTNING_ROD);
        this.registerVanilla(WAXED_LIGHTNING_ROD);
        this.registerVanilla(WAXED_OXIDIZED_CHISELED_COPPER);
        this.registerVanilla(WAXED_OXIDIZED_COPPER);
        this.registerVanilla(WAXED_OXIDIZED_COPPER_BARS);
        this.registerVanilla(WAXED_OXIDIZED_COPPER_BULB);
        this.registerVanilla(WAXED_OXIDIZED_COPPER_CHAIN);
        this.registerVanilla(WAXED_OXIDIZED_COPPER_CHEST);
        this.registerDoor(WAXED_OXIDIZED_COPPER_DOOR);
        this.registerVanilla(WAXED_OXIDIZED_COPPER_GOLEM_STATUE);
        this.registerVanilla(WAXED_OXIDIZED_COPPER_GRATE);
        this.registerVanilla(WAXED_OXIDIZED_COPPER_LANTERN);
        this.registerTrapdoor(WAXED_OXIDIZED_COPPER_TRAPDOOR);
        this.registerVanilla(WAXED_OXIDIZED_CUT_COPPER);
        this.registerVanilla(WAXED_OXIDIZED_CUT_COPPER_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(WAXED_OXIDIZED_DOUBLE_CUT_COPPER_SLAB));
        this.registerVanilla(WAXED_OXIDIZED_CUT_COPPER_STAIRS);
        this.registerVanilla(WAXED_OXIDIZED_DOUBLE_CUT_COPPER_SLAB);
        this.registerVanilla(WAXED_OXIDIZED_LIGHTNING_ROD);
        this.registerVanilla(WAXED_WEATHERED_CHISELED_COPPER);
        this.registerVanilla(WAXED_WEATHERED_COPPER);
        this.registerVanilla(WAXED_WEATHERED_COPPER_BARS);
        this.registerVanilla(WAXED_WEATHERED_COPPER_BULB);
        this.registerVanilla(WAXED_WEATHERED_COPPER_CHAIN);
        this.registerVanilla(WAXED_WEATHERED_COPPER_CHEST);
        this.registerDoor(WAXED_WEATHERED_COPPER_DOOR);
        this.registerVanilla(WAXED_WEATHERED_COPPER_GOLEM_STATUE);
        this.registerVanilla(WAXED_WEATHERED_COPPER_GRATE);
        this.registerVanilla(WAXED_WEATHERED_COPPER_LANTERN);
        this.registerTrapdoor(WAXED_WEATHERED_COPPER_TRAPDOOR);
        this.registerVanilla(WAXED_WEATHERED_CUT_COPPER);
        this.registerVanilla(WAXED_WEATHERED_CUT_COPPER_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(WAXED_WEATHERED_DOUBLE_CUT_COPPER_SLAB));
        this.registerVanilla(WAXED_WEATHERED_CUT_COPPER_STAIRS);
        this.registerVanilla(WAXED_WEATHERED_DOUBLE_CUT_COPPER_SLAB);
        this.registerVanilla(WAXED_WEATHERED_LIGHTNING_ROD);
        this.registerVanilla(WEATHERED_CHISELED_COPPER);
        this.registerVanilla(WEATHERED_COPPER);
        this.registerVanilla(WEATHERED_COPPER_BARS);
        this.registerVanilla(WEATHERED_COPPER_BULB);
        this.registerVanilla(WEATHERED_COPPER_CHAIN);
        this.registerVanilla(WEATHERED_COPPER_CHEST);
        this.registerDoor(WEATHERED_COPPER_DOOR);
        this.registerVanilla(WEATHERED_COPPER_GOLEM_STATUE);
        this.registerVanilla(WEATHERED_COPPER_GRATE);
        this.registerVanilla(WEATHERED_COPPER_LANTERN);
        this.registerTrapdoor(WEATHERED_COPPER_TRAPDOOR);
        this.registerVanilla(WEATHERED_CUT_COPPER);
        this.registerVanilla(WEATHERED_CUT_COPPER_SLAB).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(WEATHERED_DOUBLE_CUT_COPPER_SLAB));
        this.registerVanilla(WEATHERED_CUT_COPPER_STAIRS);
        this.registerVanilla(WEATHERED_DOUBLE_CUT_COPPER_SLAB);
        this.registerVanilla(WEATHERED_LIGHTNING_ROD);
        this.registerVanilla(WEB);
        this.registerVanilla(WEEPING_VINES);
        this.registerVanilla(WET_SPONGE);
        this.registerVanilla(WHEAT);
        this.registerVanilla(WHITE_CANDLE);
        this.registerVanilla(WHITE_CANDLE_CAKE);
        this.registerVanilla(WHITE_CARPET);
        this.registerVanilla(WHITE_CONCRETE);
        this.registerVanilla(WHITE_CONCRETE_POWDER);
        this.registerVanilla(WHITE_GLAZED_TERRACOTTA);
        this.registerVanilla(WHITE_SHULKER_BOX)
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, ContainerBlockHandlers.SHULKER_BOX);
        this.registerVanilla(WHITE_STAINED_GLASS);
        this.registerVanilla(WHITE_STAINED_GLASS_PANE);
        this.registerVanilla(WHITE_TERRACOTTA);
        this.registerVanilla(WHITE_TULIP);
        this.registerVanilla(WHITE_WOOL);
        this.registerVanilla(WILDFLOWERS);
        this.registerVanilla(WITHER_ROSE);
        this.registerVanilla(WITHER_SKELETON_SKULL);
        this.registerVanilla(YELLOW_CANDLE);
        this.registerVanilla(YELLOW_CANDLE_CAKE);
        this.registerVanilla(YELLOW_CARPET);
        this.registerVanilla(YELLOW_CONCRETE);
        this.registerVanilla(YELLOW_CONCRETE_POWDER);
        this.registerVanilla(YELLOW_GLAZED_TERRACOTTA);
        this.registerVanilla(YELLOW_SHULKER_BOX)
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, ContainerBlockHandlers.SHULKER_BOX);
        this.registerVanilla(YELLOW_STAINED_GLASS);
        this.registerVanilla(YELLOW_STAINED_GLASS_PANE);
        this.registerVanilla(YELLOW_TERRACOTTA);
        this.registerVanilla(YELLOW_WOOL);
        this.registerVanilla(ZOMBIE_HEAD);
        BlockTypeData.applyAll(this);
    }

    private void registerDoor(BlockType type) {
        this.registerVanilla(type)
                .set(BlockComponents.ON_PLACE, new DoorPlaceHandler(this))
                .set(BlockComponents.CAN_BE_USED, DoorBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, DoorBlockHandlers.USE)
                .set(BlockComponents.ON_NEIGHBOUR_CHANGED, DoorBlockHandlers.ON_NEIGHBOUR_CHANGED)
                .set(BlockComponents.ON_DESTROY, DoorBlockHandlers.ON_DESTROY);
    }

    private void registerTrapdoor(BlockType type) {
        this.registerVanilla(type)
                .set(BlockComponents.ON_PLACE, new TrapdoorPlaceHandler(this))
                .set(BlockComponents.CAN_BE_USED, TrapdoorBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, TrapdoorBlockHandlers.USE);
    }

    private void registerVanillaBehaviors() {
        this.registerComponent(BlockComponents.HARDNESS, () -> 0f);
        this.registerComponent(BlockComponents.RESISTANCE, () -> 0f);
        this.registerComponent(BlockComponents.FRICTION, () -> 0.6f);
        this.registerComponent(BlockComponents.TRANSLUCENCY, () -> 0f);
        this.registerComponent(BlockComponents.THICKNESS, () -> 0f);
        this.registerComponent(BlockComponents.BURN_ODDS, () -> 0);
        this.registerComponent(BlockComponents.FLAME_ODDS, () -> 0);
        this.registerComponent(BlockComponents.LIGHT_DAMPENING, () -> 0);
        this.registerComponent(BlockComponents.LIGHT_EMISSION, () -> 0);
        this.registerComponent(BlockComponents.SOLID, () -> false);
        this.registerComponent(BlockComponents.REQUIRES_CORRECT_TOOL, () -> false);
        this.registerComponent(BlockComponents.REPLACEABLE, () -> false);
        this.registerComponent(BlockComponents.MAP_COLOR, () -> "#00000000");
        this.registerComponent(BlockComponents.LIQUID, () -> false);
        this.registerComponent(BlockComponents.TOP_SOLID, () -> true);
        this.registerComponent(BlockComponents.STAIRS, () -> false);
        this.registerComponent(BlockComponents.SLAB, () -> false);
        this.registerComponent(BlockComponents.SUPER_HOT, () -> false);
        this.registerComponent(BlockComponents.FLAMMABLE, () -> false);
        this.registerComponent(BlockComponents.FLOODABLE, () -> false);
        this.registerComponent(BlockComponents.CAN_INSTATICK, () -> false);
        this.registerComponent(BlockComponents.CAN_RANDOM_TICK, () -> false);
        this.registerComponent(BlockComponents.TICK_DELAY, () -> 0);
        this.registerComponent(BlockComponents.CAN_DAMAGE_ITEM, () -> false);
        this.registerComponent(BlockComponents.USES_WATERLOGGING, () -> false);
        this.registerComponent(BlockComponents.ALWAYS_DESTROYABLE, () -> true);
        this.registerComponent(BlockComponents.GET_DESCRIPTION_ID, (state) -> state.getType().getId().toString());
        this.registerComponent(BlockComponents.CAN_PASS_THROUGH, DefaultBlockHandlers.CAN_PASS_THROUGH);
        this.registerComponent(BlockComponents.GET_BOUNDING_BOX, new GetBoundingBoxHandler());
        this.registerComponent(BlockComponents.CAN_BE_SILK_TOUCHED, DefaultBlockHandlers.CAN_BE_SILK_TOUCHED);
        this.registerComponent(BlockComponents.CAN_BE_USED_IN_COMMANDS, DefaultBlockHandlers.CAN_BE_USED_IN_COMMANDS);
        this.registerComponent(BlockComponents.CAN_CONTAIN_LIQUID, DefaultBlockHandlers.CAN_CONTAIN_LIQUID);
        this.registerComponent(BlockComponents.CAN_SPAWN_ON, DefaultBlockHandlers.CAN_SPAWN_ON);
        this.registerComponent(BlockComponents.CAN_BE_USED, (block, player) -> false);
        this.registerComponent(BlockComponents.GET_GRAVITY, (block) -> 0.02f);
        this.registerComponent(BlockComponents.GET_DESTROY_SPEED, (block) -> 0);
        this.registerComponent(BlockComponents.GET_EXPERIENCE_DROP, (block, randomGenerator) -> 0);
        this.registerComponent(BlockComponents.GET_BLOCK_ENTITY, (block) -> Optional.empty());
        this.registerComponent(BlockComponents.MAY_PICK, (block) -> true);
        this.registerComponent(BlockComponents.MAY_PLACE, (block, direction) -> true);
        this.registerComponent(BlockComponents.MAY_PLACE_ON, (block) -> true);
        this.registerComponent(BlockComponents.ON_DESTROY, DefaultBlockHandlers.ON_DESTROY);
        this.registerComponent(BlockComponents.POST_DESTROY, DefaultBlockHandlers.POST_DESTROY);
        this.registerComponent(BlockComponents.ON_NEIGHBOUR_CHANGED, (block, neighbor) -> {
        });
        this.registerComponent(BlockComponents.ON_FALL_ON, (block, entity) -> {
        });
        this.registerComponent(BlockComponents.ON_LIGHTNING_HIT, DefaultBlockHandlers.ON_LIGHTNING_HIT);
        this.registerComponent(BlockComponents.ON_PLACE, new DefaultBlockPlaceHandler(this));
        this.registerComponent(BlockComponents.ON_PROJECTILE_HIT, DefaultBlockHandlers.ON_PROJECTILE_HIT);
        this.registerComponent(BlockComponents.ON_REDSTONE_UPDATE, DefaultBlockHandlers.ON_REDSTONE_UPDATE);
        this.registerComponent(BlockComponents.ON_REMOVE, DefaultBlockHandlers.ON_REMOVE);
        this.registerComponent(BlockComponents.ON_RANDOM_TICK, (block, randomGenerator) -> {
        });
        this.registerComponent(BlockComponents.ON_TICK, (block, randomGenerator) -> {
        });
        this.registerComponent(BlockComponents.USE, (block, player, direction, item) -> false);
        this.registerComponent(BlockComponents.ON_STAND_ON, DefaultBlockHandlers.ON_STAND_ON);
        this.registerComponent(BlockComponents.ON_STEP_ON, DefaultBlockHandlers.ON_STEP_ON);
        this.registerComponent(BlockComponents.ON_STEP_OFF, DefaultBlockHandlers.ON_STEP_OFF);
        this.registerComponent(BlockComponents.GET_SILK_TOUCH_RESOURCE, (block, randomGenerator, bonusLevel) -> null);
        this.registerComponent(BlockComponents.DROP_RESOURCE, DefaultBlockHandlers.DROP_RESOURCE);
        this.registerComponent(BlockComponents.SPAWN_RESOURCES, DefaultBlockHandlers.SPAWN_RESOURCES);
        this.registerComponent(BlockComponents.GET_RESOURCE, DefaultBlockHandlers.GET_RESOURCE);
        this.registerComponent(BlockComponents.GET_RESOURCE_COUNT, DefaultBlockHandlers.GET_RESOURCE_COUNT);
        this.registerComponent(BlockComponents.GET_PICK_BLOCK, (block) -> ItemStack.from(block.getState()));
        this.registerComponent(BlockComponents.GET_COLOR, (block) -> null);
        this.registerComponent(BlockComponents.CAN_SURVIVE, (block) -> true);
        this.registerComponent(BlockComponents.CHECK_ALIVE, (block) -> {
        });
        this.registerComponent(BlockComponents.CAN_SLIDE, (block) -> false);
        this.registerComponent(BlockComponents.IS_FREE_TO_FALL, (block) -> false);
        this.registerComponent(BlockComponents.START_FALLING, (block) -> {
        });
        this.registerComponent(BlockComponents.GET_LIQUID_HEIGHT, (block) -> 0);
        this.registerComponent(BlockComponents.ON_ENTITY_COLLIDE, (block, entity) -> {
        });
        this.registerComponent(BlockComponents.GET_MAP_COLOR, (block) -> {
            StringTypeHandler mapColorHandler = getComponent(block.getState().getType(), BlockComponents.MAP_COLOR);
            String hex = mapColorHandler != null ? mapColorHandler.get() : null;
            if (hex == null || hex.length() < 9) return new Color(0, 0, 0, 0);
            int r = Integer.parseInt(hex.substring(1, 3), 16);
            int g = Integer.parseInt(hex.substring(3, 5), 16);
            int b = Integer.parseInt(hex.substring(5, 7), 16);
            int a = Integer.parseInt(hex.substring(7, 9), 16);
            return new Color(r, g, b, a);
        });
        this.registerComponent(BlockComponents.IS_BREAKABLE, (block, item) -> true);
    }
}
