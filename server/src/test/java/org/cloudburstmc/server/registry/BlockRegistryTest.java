package org.cloudburstmc.server.registry;

import org.cloudburstmc.api.Server;
import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.data.ComponentType;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.util.CollisionContext;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.VoxelShape;
import org.cloudburstmc.api.util.component.ComponentMap;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.*;
import org.cloudburstmc.server.block.BlockLayerRules;
import org.cloudburstmc.server.block.BlockLayers;
import org.cloudburstmc.server.block.BlockPalette;
import org.cloudburstmc.server.block.component.*;
import org.cloudburstmc.server.block.util.BlockSupport;
import org.cloudburstmc.server.level.collision.CloudVoxelShapes;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

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
                () -> assertSame(FarmlandBlockHandlers.FALL_ON, component(BlockTypes.FARMLAND, BlockComponents.ON_FALL_ON)),
                () -> assertSame(DefaultBlockHandlers.NO_LOOT, component(BlockTypes.FIRE, BlockComponents.GET_LOOT)),
                () -> assertSame(DefaultBlockHandlers.NO_LOOT, component(BlockTypes.SOUL_FIRE, BlockComponents.GET_LOOT)),
                () -> assertSame(DefaultBlockHandlers.NO_LOOT, component(BlockTypes.PORTAL, BlockComponents.GET_LOOT)),
                () -> assertSame(DefaultBlockHandlers.NO_LOOT, component(BlockTypes.END_PORTAL, BlockComponents.GET_LOOT)),
                () -> assertSame(DefaultBlockHandlers.NO_LOOT, component(BlockTypes.END_GATEWAY, BlockComponents.GET_LOOT)),
                () -> assertSame(DefaultBlockHandlers.NO_LOOT, component(BlockTypes.MOB_SPAWNER, BlockComponents.GET_LOOT)),
                () -> assertSame(DefaultBlockHandlers.NO_LOOT, component(BlockTypes.SUSPICIOUS_SAND, BlockComponents.GET_LOOT)),
                () -> assertSlabPlaceHandler(BlockTypes.GRANITE_SLAB),
                () -> assertSlabPlaceHandler(BlockTypes.MOSSY_STONE_BRICK_SLAB)
        );

        for (BlockType type : BlockTypes.values()) {
            if (type.getTraits().contains(BlockTraits.EXPLODE)) {
                assertAll(type.toString(),
                        () -> assertSame(DefaultBlockHandlers.CAN_BE_USED, component(type, BlockComponents.CAN_BE_USED)),
                        () -> assertSame(TntBlockHandlers.USE, component(type, BlockComponents.USE)),
                        () -> assertSame(TntBlockHandlers.ON_EXPLOSION_HIT, component(type, BlockComponents.ON_EXPLOSION_HIT)),
                        () -> assertSame(TntBlockHandlers.ON_PROJECTILE_HIT, component(type, BlockComponents.ON_PROJECTILE_HIT)));
            }
        }
    }

    @Test
    void bubbleColumnContactDoesNotDependOnPhysicalCollision() {
        BlockState state = BlockTypes.BUBBLE_COLUMN.getDefaultState();
        VoxelShape inside = component(BlockTypes.BUBBLE_COLUMN, BlockComponents.GET_ENTITY_INSIDE_COLLISION_SHAPE)
                .execute(state, CollisionContext.empty());

        assertTrue(state.getCollisionShape().isEmpty());
        assertTrue(inside.covers(CloudVoxelShapes.block()));
        assertSame(BubbleColumnBlockHandlers.ON_ENTITY_INSIDE,
                component(BlockTypes.BUBBLE_COLUMN, BlockComponents.ON_ENTITY_INSIDE));
    }

    @Test
    void tripwireContactUsesItsOutlineRatherThanTheWholeBlock() {
        BlockState state = BlockTypes.TRIP_WIRE.getDefaultState();
        VoxelShape inside = component(BlockTypes.TRIP_WIRE, BlockComponents.GET_ENTITY_INSIDE_COLLISION_SHAPE)
                .execute(state, CollisionContext.empty());

        assertTrue(inside.covers(state.getOutlineShape()));
        assertTrue(state.getOutlineShape().covers(inside));
        assertFalse(inside.covers(CloudVoxelShapes.block()));
    }

    @Test
    void cakeSurvivalUsesSolidSupportWithoutRequiringAFullFace() {
        Server server = InterfaceProxy.create(Server.class, Map.of("getBlockRegistry", REGISTRY));
        Level level = InterfaceProxy.create(Level.class, Map.of("getServer", server));

        for (BlockType type : List.of(BlockTypes.CAKE, BlockTypes.CANDLE_CAKE, BlockTypes.RED_CANDLE_CAKE)) {
            for (BlockState state : type.getStates()) {
                Block prospectiveCake = InterfaceProxy.create(Block.class, Map.of("getLevel", level, "getRelativeState", state));
                assertTrue(CakeBlockHandlers.CAN_SURVIVE.execute(prospectiveCake), state.toString());
                assertTrue(BlockSupport.isSolidSupport(REGISTRY, state));
                assertFalse(BlockSupport.isFaceSturdy(REGISTRY, state, Direction.UP, SupportType.FULL));
            }
        }

        for (BlockState state : List.of(BlockTypes.AIR.getDefaultState(), BlockTypes.CANDLE.getDefaultState())) {
            Block prospectiveCake = InterfaceProxy.create(Block.class,
                    Map.of("getLevel", level, "getRelativeState", state));
            assertFalse(CakeBlockHandlers.CAN_SURVIVE.execute(prospectiveCake), state.toString());
        }

        assertTrue(BlockSupport.isSolidSupport(REGISTRY, BlockTypes.STONE.getDefaultState()));
    }

    @Test
    void ignitionLightsExistingBlocksWithoutLosingTheirState() {
        for (BlockType type : List.of(BlockTypes.CANDLE, BlockTypes.RED_CANDLE, BlockTypes.CANDLE_CAKE, BlockTypes.RED_CANDLE_CAKE)) {
            BlockState unlit = type.getDefaultState().withTrait(BlockTraits.IS_LIT, false);
            if (type.getTraits().contains(BlockTraits.CANDLES)) {
                unlit = unlit.withTrait(BlockTraits.CANDLES, 2);
            }

            Block block = InterfaceProxy.create(Block.class, Map.of("getState", unlit, "getLiquid", LiquidState.empty()));
            BlockState lit = Objects.requireNonNull(component(type, BlockComponents.GET_IGNITED_STATE).execute(block));
            assertEquals(unlit.withTrait(BlockTraits.IS_LIT, true), lit);
            Block alreadyLit = InterfaceProxy.create(Block.class, Map.of("getState", lit, "getLiquid", LiquidState.empty()));
            assertNull(component(type, BlockComponents.GET_IGNITED_STATE).execute(alreadyLit));
            Block waterlogged = InterfaceProxy.create(Block.class,
                    Map.of("getState", unlit, "getLiquid", LiquidState.of(BlockStates.WATER)));
            assertNull(component(type, BlockComponents.GET_IGNITED_STATE).execute(waterlogged));
        }

        for (BlockType type : List.of(BlockTypes.CAMPFIRE, BlockTypes.SOUL_CAMPFIRE)) {
            BlockState extinguished = type.getDefaultState().withTrait(BlockTraits.IS_EXTINGUISHED, true);
            Block block = InterfaceProxy.create(Block.class, Map.of("getState", extinguished, "getLiquid", LiquidState.empty()));
            assertEquals(extinguished.withTrait(BlockTraits.IS_EXTINGUISHED, false),
                    component(type, BlockComponents.GET_IGNITED_STATE).execute(block));
        }

        Block stone = InterfaceProxy.create(Block.class, Map.of("getState", BlockTypes.STONE.getDefaultState(), "getLiquid", LiquidState.empty()));
        assertNull(component(BlockTypes.STONE, BlockComponents.GET_IGNITED_STATE).execute(stone));
    }

    @Test
    void fireCannotSurviveBesideOrAboveCandles() {
        for (BlockType type : BlockTypes.values()) {
            if (!type.getTraits().contains(BlockTraits.CANDLES) && type != BlockTypes.CANDLE_CAKE) {
                continue;
            }

            for (BlockState state : type.getStates()) {
                Block candle = InterfaceProxy.create(Block.class, Map.of(
                        "getState", state, "getLiquid", LiquidState.empty(),
                        "isFaceSturdy", BlockSupport.isFaceSturdy(REGISTRY, state, Direction.UP, SupportType.FULL)));
                Block adjacent = InterfaceProxy.create(Block.class, Map.of(
                        "getSide", candle, "getLiquid", LiquidState.empty()));
                assertFalse(component(BlockTypes.FIRE, BlockComponents.CAN_SURVIVE).execute(adjacent), state.toString());
            }
        }
    }

    @Test
    void fireSurvivalUsesSupportFlammabilityAndLiquidState() {
        Block stone = InterfaceProxy.create(Block.class, Map.of("getState", BlockTypes.STONE.getDefaultState(), "getLiquid", LiquidState.empty(), "isFaceSturdy", true));
        Block supported = InterfaceProxy.create(Block.class, Map.of("getSide", stone, "getLiquid", LiquidState.empty()));
        assertTrue(component(BlockTypes.FIRE, BlockComponents.CAN_SURVIVE).execute(supported));

        Block wood = InterfaceProxy.create(Block.class, Map.of("getState", BlockTypes.OAK_PLANKS.getDefaultState(), "getLiquid", LiquidState.empty()));
        Block besideWood = InterfaceProxy.create(Block.class, Map.of("getSide", wood, "getLiquid", LiquidState.empty()));
        assertTrue(component(BlockTypes.FIRE, BlockComponents.CAN_SURVIVE).execute(besideWood));

        Block submerged = InterfaceProxy.create(Block.class, Map.of("getSide", stone, "getLiquid", LiquidState.of(BlockStates.WATER)));
        assertFalse(component(BlockTypes.FIRE, BlockComponents.CAN_SURVIVE).execute(submerged));

        Block wetWood = InterfaceProxy.create(Block.class, Map.of("getState", BlockTypes.OAK_PLANKS.getDefaultState(), "getLiquid", LiquidState.of(BlockStates.WATER)));
        Block besideWetWood = InterfaceProxy.create(Block.class, Map.of("getSide", wetWood, "getLiquid", LiquidState.empty()));
        assertFalse(component(BlockTypes.FIRE, BlockComponents.CAN_SURVIVE).execute(besideWetWood));
    }

    @Test
    void soulFireRequiresItsOwnBaseBlocks() {
        for (BlockType type : List.of(BlockTypes.SOUL_SAND, BlockTypes.SOUL_SOIL, BlockTypes.STONE)) {
            Block target = InterfaceProxy.create(Block.class, Map.of("getSideState", type.getDefaultState(), "getLiquid", LiquidState.empty()));
            boolean soulBase = type != BlockTypes.STONE;
            assertEquals(soulBase, component(BlockTypes.SOUL_FIRE, BlockComponents.CAN_SURVIVE).execute(target));
            assertSame(soulBase ? BlockStates.SOUL_FIRE : BlockStates.FIRE, FireBlockHandlers.placementState(target));
        }
    }

    @Test
    void underwaterCandlePlacementPreservesSourceWaterAndStaysUnlit() {
        Server server = InterfaceProxy.create(Server.class, Map.of("getBlockRegistry", REGISTRY));
        Level level = InterfaceProxy.create(Level.class, Map.of("getServer", server, "getBlockState", BlockTypes.STONE.getDefaultState()));

        for (BlockType type : BlockTypes.values()) {
            if (!type.getTraits().contains(BlockTraits.CANDLES)) {
                continue;
            }

            Block sourceWater = InterfaceProxy.create(Block.class, Map.of("getState", BlockStates.WATER));
            assertTrue(component(BlockTypes.WATER, BlockComponents.CAN_BE_REPLACED)
                    .execute(sourceWater, type.getDefaultState(), null, Direction.UP, Vector3f.ZERO));
            BlockLayers placement = Objects.requireNonNull(BlockLayerRules.resolveReplacement(
                    new BlockLayers(BlockStates.WATER, BlockStates.AIR), BlockLayer.PRIMARY, type.getDefaultState()));
            assertSame(BlockStates.WATER, placement.secondary());
            assertFalse(placement.primary().ensureTrait(BlockTraits.IS_LIT));
            Block placed = InterfaceProxy.create(Block.class, Map.of(
                    "getState", placement.primary(), "getLiquid", LiquidState.of(placement.secondary()),
                    "getLevel", level, "getPosition", Vector3i.ZERO,
                    "getRelativeState", BlockTypes.STONE.getDefaultState()));
            assertTrue(component(type, BlockComponents.CAN_SURVIVE).execute(placed));
            assertNull(component(type, BlockComponents.GET_IGNITED_STATE).execute(placed));
        }
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
    void barsAndPanesConnectToEachOtherAndWalls() {
        Set<BlockType> bars = Set.copyOf(BlockTypes.values().stream()
                .filter(type -> type == BlockTypes.IRON_BARS || type.getId().getName().endsWith("copper_bars"))
                .toList());
        Set<BlockType> panes = Set.copyOf(BlockTypes.values().stream()
                .filter(type -> type.getId().getName().endsWith("_pane"))
                .toList());

        assertAll(
                () -> assertEquals(bars, REGISTRY.getTag(BlockTags.BARS).getValues()),
                () -> assertEquals(panes, REGISTRY.getTag(BlockTags.GLASS_PANES).getValues()),
                () -> assertTrue(BarBlockHandlers.connectsTo(BlockTypes.GLASS_PANE.getDefaultState(), false)),
                () -> assertTrue(BarBlockHandlers.connectsTo(BlockTypes.COPPER_BARS.getDefaultState(), false)),
                () -> assertTrue(BarBlockHandlers.connectsTo(BlockTypes.COBBLESTONE_WALL.getDefaultState(), false)),
                () -> assertFalse(BarBlockHandlers.connectsTo(BlockTypes.OAK_FENCE.getDefaultState(), false)),
                () -> assertFalse(BarBlockHandlers.connectsTo(BlockTypes.OAK_LEAVES.getDefaultState(), true))
        );

        for (BlockType type : BlockTypes.values()) {
            if (bars.contains(type) || panes.contains(type)) {
                assertAll(type.toString(),
                        () -> assertSame(BarBlockHandlers.RESOLVE_PLACEMENT_STATE, component(type, BlockComponents.RESOLVE_PLACEMENT_STATE)),
                        () -> assertSame(BarBlockHandlers.ON_NEIGHBOUR_CHANGED, component(type, BlockComponents.ON_NEIGHBOUR_CHANGED)));
            }
        }
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

    @Test
    void groupsAllLightningRodVariantsForChanneling() {
        assertEquals(Set.of(
                BlockTypes.LIGHTNING_ROD,
                BlockTypes.EXPOSED_LIGHTNING_ROD,
                BlockTypes.WEATHERED_LIGHTNING_ROD,
                BlockTypes.OXIDIZED_LIGHTNING_ROD,
                BlockTypes.WAXED_LIGHTNING_ROD,
                BlockTypes.WAXED_EXPOSED_LIGHTNING_ROD,
                BlockTypes.WAXED_WEATHERED_LIGHTNING_ROD,
                BlockTypes.WAXED_OXIDIZED_LIGHTNING_ROD
        ), REGISTRY.getTag(BlockTags.LIGHTNING_RODS).getValues());
    }

    private static void assertSlabPlaceHandler(BlockType blockType) {
        assertInstanceOf(SlabPlaceHandler.class, component(blockType, BlockComponents.ON_PLACE));
    }

    private static <T> T component(BlockType blockType, ComponentType<T> componentType) {
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
