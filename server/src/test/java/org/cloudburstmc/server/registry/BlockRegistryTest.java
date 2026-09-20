package org.cloudburstmc.server.registry;

import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.data.ComponentType;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.component.ComponentMap;
import org.cloudburstmc.nbt.*;
import org.cloudburstmc.server.block.BlockLayerRules;
import org.cloudburstmc.server.block.BlockLayers;
import org.cloudburstmc.server.block.BlockPalette;
import org.cloudburstmc.server.block.component.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BlockRegistryTest {
    private static final CloudBlockRegistry REGISTRY = createRegistry();

    @Test
    void serializedPaletteMatchesVanillaPalette() throws IOException {
        LinkedList<NbtMap> vanillaPalette;
        InputStream stream = Objects.requireNonNull(
                BlockRegistryTest.class.getClassLoader().getResourceAsStream("data/block_palette.nbt"),
                "Missing vanilla block palette"
        );

        try (NBTInputStream nbtStream = NbtUtils.createGZIPReader(stream)) {
            NbtMap tag = (NbtMap) nbtStream.readTag();
            vanillaPalette = tag.getList("blocks", NbtType.COMPOUND).stream()
                    .map(BlockRegistryTest::stripRuntimeOnlyTags)
                    .collect(LinkedList::new, LinkedList::add, LinkedList::addAll);
        }

        Set<NbtMap> serializedStates = BlockPalette.INSTANCE.getSerializedPalette().keySet();
        List<NbtMap> missingStates = vanillaPalette.stream()
                .filter(state -> !serializedStates.contains(state))
                .toList();

        assertAll(
                () -> assertEquals(vanillaPalette.size(), BlockPalette.INSTANCE.getRuntimeMap().size(), "Every vanilla state must have one runtime definition"),
                () -> assertTrue(missingStates.isEmpty(), () -> missingStates.size() + " vanilla states are absent from the serialized palette: " + missingStates.stream().limit(5).toList())
        );
    }

    @Test
    void configuresSpecializedVanillaBlockBehaviors() {
        assertAll(
                () -> assertSame(AnvilBlockHandlers.RESOLVE_PLACEMENT_STATE, component(BlockTypes.ANVIL, BlockComponents.RESOLVE_PLACEMENT_STATE)),
                () -> assertSlabPlaceHandler(BlockTypes.BAMBOO_MOSAIC_SLAB),
                () -> assertSame(DefaultBlockHandlers.CAN_BE_USED, component(BlockTypes.ENCHANTING_TABLE, BlockComponents.CAN_BE_USED)),
                () -> assertSame(ContainerBlockHandlers.ENCHANTING_TABLE, component(BlockTypes.ENCHANTING_TABLE, BlockComponents.USE)),
                () -> assertSame(DefaultBlockHandlers.CAN_BE_USED, component(BlockTypes.ENDER_CHEST, BlockComponents.CAN_BE_USED)),
                () -> assertSame(ContainerBlockHandlers.ENDER_CHEST, component(BlockTypes.ENDER_CHEST, BlockComponents.USE)),
                () -> assertSlabPlaceHandler(BlockTypes.GRANITE_SLAB),
                () -> assertSlabPlaceHandler(BlockTypes.MOSSY_STONE_BRICK_SLAB)
        );
    }

    @Test
    void stairTagMatchesTheStairShapeTrait() {
        Set<BlockType> taggedStairs = REGISTRY.getTag(BlockTags.STAIRS).getValues();
        Set<BlockType> stairTypes = Set.copyOf(BlockTypes.values().stream()
                .filter(type -> type.getTraits().contains(BlockTraits.STAIR_SHAPE))
                .toList());

        assertEquals(stairTypes, taggedStairs);
    }

    @Test
    void fenceAndWallTagsDriveTheirBehaviors() {
        Set<BlockType> fenceTypes = Set.copyOf(BlockTypes.values().stream()
                .filter(type -> type.getId().getName().endsWith("_fence"))
                .toList());
        Set<BlockType> fenceGateTypes = Set.copyOf(BlockTypes.values().stream()
                .filter(type -> type.getTraits().contains(BlockTraits.IS_IN_WALL))
                .toList());
        Set<BlockType> wallTypes = Set.copyOf(BlockTypes.values().stream()
                .filter(type -> type != BlockTypes.BORDER_BLOCK)
                .filter(type -> type.getTraits().contains(BlockTraits.HAS_POST))
                .toList());
        Set<BlockType> woodenFenceTypes = Set.copyOf(fenceTypes.stream()
                .filter(type -> type != BlockTypes.NETHER_BRICK_FENCE)
                .toList());

        assertAll(
                () -> assertEquals(fenceTypes, REGISTRY.getTag(BlockTags.FENCE).getValues()),
                () -> assertEquals(fenceGateTypes, REGISTRY.getTag(BlockTags.FENCE_GATE).getValues()),
                () -> assertEquals(wallTypes, REGISTRY.getTag(BlockTags.WALLS).getValues()),
                () -> assertEquals(woodenFenceTypes, REGISTRY.getTag(BlockTags.WOODEN_FENCE).getValues()),
                () -> assertTrue(REGISTRY.getTag(BlockTags.WALL_POST_OVERRIDE).isTagged(BlockTypes.OAK_STANDING_SIGN)),
                () -> assertTrue(REGISTRY.getTag(BlockTags.WALL_POST_OVERRIDE).isTagged(BlockTypes.OAK_WALL_SIGN)),
                () -> assertFalse(REGISTRY.getTag(BlockTags.WALL_POST_OVERRIDE).isTagged(BlockTypes.OAK_HANGING_SIGN))
        );

        for (BlockType type : REGISTRY.getTag(BlockTags.FENCE).getValues()) {
            assertAll(type.toString(),
                    () -> assertSame(FenceBlockHandlers.RESOLVE_PLACEMENT_STATE, component(type, BlockComponents.RESOLVE_PLACEMENT_STATE)),
                    () -> assertSame(FenceBlockHandlers.ON_NEIGHBOUR_CHANGED, component(type, BlockComponents.ON_NEIGHBOUR_CHANGED))
            );
        }

        for (BlockType type : REGISTRY.getTag(BlockTags.FENCE_GATE).getValues()) {
            assertSame(FenceGateBlockHandlers.RESOLVE_PLACEMENT_STATE, component(type, BlockComponents.RESOLVE_PLACEMENT_STATE), type.toString());
        }

        for (BlockType type : REGISTRY.getTag(BlockTags.STAIRS).getValues()) {
            assertAll(type.toString(),
                    () -> assertSame(StairBlockHandlers.RESOLVE_PLACEMENT_STATE, component(type, BlockComponents.RESOLVE_PLACEMENT_STATE)),
                    () -> assertSame(StairBlockHandlers.ON_NEIGHBOUR_CHANGED, component(type, BlockComponents.ON_NEIGHBOUR_CHANGED))
            );
        }

        for (BlockType type : REGISTRY.getTag(BlockTags.WALLS).getValues()) {
            assertAll(type.toString(),
                    () -> assertSame(WallBlockHandlers.RESOLVE_PLACEMENT_STATE, component(type, BlockComponents.RESOLVE_PLACEMENT_STATE)),
                    () -> assertSame(WallBlockHandlers.ON_NEIGHBOUR_CHANGED, component(type, BlockComponents.ON_NEIGHBOUR_CHANGED))
            );
        }
    }

    @Test
    void fencesOnlyConnectWithinTheirMaterialFamily() {
        BlockState oak = BlockTypes.OAK_FENCE.getDefaultState();
        BlockState spruce = BlockTypes.SPRUCE_FENCE.getDefaultState();
        BlockState netherBrick = BlockTypes.NETHER_BRICK_FENCE.getDefaultState();

        assertAll(
                () -> assertTrue(FenceBlockHandlers.connectsTo(oak, spruce, false, Direction.NORTH)),
                () -> assertFalse(FenceBlockHandlers.connectsTo(oak, netherBrick, false, Direction.NORTH)),
                () -> assertFalse(FenceBlockHandlers.connectsTo(netherBrick, oak, false, Direction.NORTH)),
                () -> assertTrue(FenceBlockHandlers.connectsTo(netherBrick, netherBrick, false, Direction.NORTH))
        );
    }

    @Test
    void fencesAndWallsRespectGateAlignment() {
        BlockState alignedGate = BlockTypes.OAK_FENCE_GATE.getDefaultState().withTrait(BlockTraits.CARDINAL_DIRECTION, Direction.EAST.getCardinalDirection());
        BlockState crossingGate = BlockTypes.OAK_FENCE_GATE.getDefaultState().withTrait(BlockTraits.CARDINAL_DIRECTION, Direction.NORTH.getCardinalDirection());
        BlockState fence = BlockTypes.OAK_FENCE.getDefaultState();

        assertAll(
                () -> assertTrue(FenceBlockHandlers.connectsTo(fence, alignedGate, false, Direction.NORTH)),
                () -> assertFalse(FenceBlockHandlers.connectsTo(fence, crossingGate, false, Direction.NORTH)),
                () -> assertTrue(WallBlockHandlers.connectsTo(alignedGate, false, Direction.NORTH)),
                () -> assertFalse(WallBlockHandlers.connectsTo(crossingGate, false, Direction.NORTH))
        );
    }

    @Test
    void sturdyConnectionExceptionsRemainDisconnected() {
        BlockState fence = BlockTypes.OAK_FENCE.getDefaultState();
        BlockState leaves = BlockTypes.OAK_LEAVES.getDefaultState();

        assertAll(
                () -> assertFalse(FenceBlockHandlers.connectsTo(fence, leaves, true, Direction.NORTH)),
                () -> assertFalse(WallBlockHandlers.connectsTo(leaves, true, Direction.NORTH)),
                () -> assertTrue(WallBlockHandlers.connectsTo(BlockTypes.IRON_BARS.getDefaultState(), false, Direction.NORTH)),
                () -> assertTrue(WallBlockHandlers.connectsTo(BlockTypes.COPPER_BARS.getDefaultState(), false, Direction.NORTH)),
                () -> assertTrue(WallBlockHandlers.connectsTo(BlockTypes.GLASS_PANE.getDefaultState(), false, Direction.NORTH)),
                () -> assertFalse(WallBlockHandlers.connectsTo(BlockTypes.TRIP_WIRE.getDefaultState(), false, Direction.NORTH))
        );
    }

    @Test
    void coloredBuildingBlockFamiliesInheritStructuralAndMiningTags() {
        for (VanillaSlabAndStairFamily family : VanillaBlockFamilies.COLORED_BUILDING_BLOCKS) {
            assertAll(family.base().toString(),
                    () -> assertTrue(family.slab().is(BlockTags.SLAB)),
                    () -> assertTrue(family.doubleSlab().is(BlockTags.DOUBLE_SLAB)),
                    () -> assertTrue(family.stairs().is(BlockTags.STAIRS)),
                    () -> assertEquals(family.base().is(BlockTags.MINEABLE_WITH_PICKAXE), family.slab().is(BlockTags.MINEABLE_WITH_PICKAXE)),
                    () -> assertEquals(family.base().is(BlockTags.MINEABLE_WITH_PICKAXE), family.doubleSlab().is(BlockTags.MINEABLE_WITH_PICKAXE)),
                    () -> assertEquals(family.base().is(BlockTags.MINEABLE_WITH_PICKAXE), family.stairs().is(BlockTags.MINEABLE_WITH_PICKAXE)),
                    () -> assertEquals(family.base().is(BlockTags.WOOL), family.slab().is(BlockTags.WOOL)),
                    () -> assertEquals(family.base().is(BlockTags.WOOL), family.doubleSlab().is(BlockTags.WOOL)),
                    () -> assertEquals(family.base().is(BlockTags.WOOL), family.stairs().is(BlockTags.WOOL))
            );
        }
    }

    @Test
    void snowloggingPreservesVegetationOnlyWhenSnowIsPlaced() {
        BlockState snow = BlockStates.SNOW_LAYER.withTrait(BlockTraits.IS_COVERED, true);

        BlockLayers snowPlacedOnGrass = Objects.requireNonNull(BlockLayerRules.resolveReplacement(
                new BlockLayers(BlockStates.SHORT_GRASS, BlockStates.AIR), BlockLayer.PRIMARY, snow));
        BlockLayers grassPlacedInSnow = Objects.requireNonNull(BlockLayerRules.resolveReplacement(
                new BlockLayers(snow, BlockStates.AIR), BlockLayer.PRIMARY, BlockStates.SHORT_GRASS));
        BlockLayers snowRemoved = Objects.requireNonNull(
                BlockLayerRules.resolveReplacement(snowPlacedOnGrass, BlockLayer.PRIMARY, BlockStates.AIR));
        BlockLayers snowOnGrassBlock = BlockLayerRules.normalizeSnowCover(
                new BlockLayers(BlockStates.SNOW_LAYER, BlockStates.AIR), BlockStates.GRASS_BLOCK);

        assertAll(
                () -> assertSame(BlockTypes.SNOW_LAYER, snowPlacedOnGrass.primary().getType()),
                () -> assertFalse(snowPlacedOnGrass.primary().ensureTrait(BlockTraits.IS_COVERED)),
                () -> assertSame(BlockStates.SHORT_GRASS, snowPlacedOnGrass.secondary()),
                () -> assertEquals(new BlockLayers(BlockStates.SHORT_GRASS, BlockStates.AIR), grassPlacedInSnow),
                () -> assertEquals(new BlockLayers(BlockStates.SHORT_GRASS, BlockStates.AIR), snowRemoved),
                () -> assertTrue(snowOnGrassBlock.primary().ensureTrait(BlockTraits.IS_COVERED)),
                () -> assertNull(BlockLayerRules.resolveReplacement(new BlockLayers(BlockStates.STONE, BlockStates.AIR), BlockLayer.SECONDARY, BlockStates.SHORT_GRASS)),
                () -> assertTrue(BlockTypes.SHORT_GRASS.is(BlockTags.SNOWLOGGABLE)),
                () -> assertFalse(BlockTypes.GRASS_BLOCK.is(BlockTags.SNOWLOGGABLE))
        );
    }

    private static void assertSlabPlaceHandler(BlockType blockType) {
        assertInstanceOf(SlabPlaceHandler.class, component(blockType, BlockComponents.ON_PLACE));
    }

    private static Object component(BlockType blockType, ComponentType<?> componentType) {
        ComponentMap components = Objects.requireNonNull(REGISTRY.getComponents(blockType), () -> blockType.getId() + " has no component map");
        return Objects.requireNonNull(components.get(componentType), () -> blockType.getId() + " has no " + componentType.getId() + " component");
    }

    private static CloudBlockRegistry createRegistry() {
        CloudBlockRegistry registry = new CloudBlockRegistry(CloudItemRegistry.get());
        registry.close();
        return registry;
    }

    private static NbtMap stripRuntimeOnlyTags(NbtMap state) {
        NbtMapBuilder builder = state.toBuilder();
        builder.remove("version");
        builder.remove("name_hash");
        builder.remove("network_id");
        builder.remove("block_id");
        return builder.build();
    }
}
