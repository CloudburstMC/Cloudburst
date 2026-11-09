package org.cloudburstmc.server.block.component;

import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.block.component.BlockShapeContext;
import org.cloudburstmc.api.block.component.RedstoneConnectionHandler;
import org.cloudburstmc.api.data.ComponentType;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.player.Ability;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.player.PlayerAbilities;
import org.cloudburstmc.api.registry.BlockRegistry;
import org.cloudburstmc.api.util.CollisionContext;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.component.ComponentMap;
import org.cloudburstmc.api.util.data.CardinalDirection;
import org.cloudburstmc.api.util.data.LeverDirection;
import org.cloudburstmc.api.util.data.RedstoneSide;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.block.util.RedstoneSignals;
import org.cloudburstmc.server.level.collision.CloudVoxelShapes;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.cloudburstmc.server.testutil.BlockTestWorld;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RedstoneWireBlockHandlersTest {

    private static final BlockRegistry REGISTRY = BlockTestRegistry.get();
    private static final Vector3i POSITION = Vector3i.from(0, 64, 0);
    private static final BlockState WIRE = BlockTypes.REDSTONE_WIRE.getDefaultState();

    @Test
    void connectionComponentDefaultsToFalse() {
        var handler = REGISTRY.getDefaultComponent(BlockComponents.CONNECTS_TO_REDSTONE_WIRE);
        Block block = world().block(POSITION);
        assertNotNull(handler);
        assertFalse(handler.execute(block, null));
        for (Direction direction : Direction.values()) {
            assertFalse(handler.execute(block, direction));
        }
        assertSame(handler, REGISTRY.requireComponent(BlockTypes.STONE, BlockComponents.CONNECTS_TO_REDSTONE_WIRE));
    }

    @Test
    void reusablePredicatesRespectNullableDirectionAndState() {
        BlockTestWorld world = world();
        Block wire = world.block(POSITION);
        assertFalse(RedstoneConnectionHandlers.ALL_SIDES.execute(wire, null));
        assertTrue(RedstoneConnectionHandlers.WIRE.execute(wire, null));
        for (Direction direction : Direction.values()) {
            assertTrue(RedstoneConnectionHandlers.ALL_SIDES.execute(wire, direction));
            assertTrue(RedstoneConnectionHandlers.WIRE.execute(wire, direction));
        }
        for (CardinalDirection facing : List.of(CardinalDirection.NORTH, CardinalDirection.EAST,
                CardinalDirection.SOUTH, CardinalDirection.WEST)) {
            world.setState(POSITION, BlockTypes.UNPOWERED_REPEATER.getDefaultState().withTrait(BlockTraits.CARDINAL_DIRECTION, facing));
            Block repeater = world.block(POSITION);
            assertFalse(RedstoneConnectionHandlers.REPEATER.execute(repeater, null));
            for (Direction direction : Direction.values()) {
                assertEquals(direction.getAxis() == facing.toDirection().getAxis(),
                        RedstoneConnectionHandlers.REPEATER.execute(repeater, direction));
            }
        }
        for (Direction facing : Direction.values()) {
            world.setState(POSITION, BlockTypes.OBSERVER.getDefaultState().withTrait(BlockTraits.FACING_DIRECTION, facing));
            Block observer = world.block(POSITION);
            assertFalse(RedstoneConnectionHandlers.OBSERVER.execute(observer, null));
            for (Direction direction : Direction.values()) {
                assertEquals(direction == facing, RedstoneConnectionHandlers.OBSERVER.execute(observer, direction));
            }
        }
    }

    @Test
    void pluginPredicateReceivesCandidateStatePositionAndWireToCandidateDirection() {
        Vector3i candidate = POSITION.add(1, 0, 0);
        List<Direction> directions = new ArrayList<>();
        RedstoneConnectionHandler custom = (block, direction) -> {
            assertEquals(candidate, block.getPosition());
            assertSame(BlockStates.STONE, block.getState());
            assertSame(BlockStates.STONE, block.getLevel().getBlockState(candidate));
            directions.add(direction);
            return direction == Direction.EAST;
        };
        BlockTestWorld world = world(Map.of(BlockTypes.STONE, custom));
        world.setState(POSITION.add(0, 0, -1), WIRE);
        world.setState(candidate, BlockStates.STONE);
        assertSide(placement(world), Direction.EAST, RedstoneSide.SIDE);
        assertEquals(List.of(Direction.EAST), directions);
    }

    @Test
    void pluginPredicatesControlVerticalOnlyConnections() {
        List<Vector3i> queried = new ArrayList<>();
        RedstoneConnectionHandler vertical = (block, direction) -> {
            assertNull(direction);
            queried.add(block.getPosition());
            return true;
        };
        BlockTestWorld world = world(Map.of(BlockTypes.TARGET, vertical));
        world.setState(POSITION.add(0, 0, -1), WIRE);
        world.setState(POSITION.add(1, 0, 0), BlockStates.STONE);
        world.setState(POSITION.add(1, 1, 0), BlockTypes.TARGET.getDefaultState());
        assertSide(placement(world), Direction.EAST, RedstoneSide.UP);
        assertEquals(List.of(POSITION.add(1, 1, 0)), queried);

        queried.clear();
        world.setState(POSITION.add(1, 1, 0), BlockStates.AIR);
        world.setState(POSITION.add(1, 0, 0), BlockStates.AIR);
        world.setState(POSITION.add(1, -1, 0), BlockTypes.TARGET.getDefaultState());
        assertSide(placement(world), Direction.EAST, RedstoneSide.SIDE);
        assertEquals(List.of(POSITION.add(1, -1, 0)), queried);
    }

    @Test
    void aPluginCanRejectConnectionsEvenForAWireState() {
        BlockTestWorld world = world(Map.of(BlockTypes.REDSTONE_WIRE, (block, direction) -> false));
        world.setState(POSITION.add(1, 0, 0), WIRE);
        assertSame(WIRE, RedstoneWireBlockHandlers.resolveConnections(world.block(POSITION).getLevel(), POSITION, WIRE));
    }

    @Test
    void retainsNativeBedrockOutlineAndEmptyCollisionForEveryWireState() {
        List<BlockState> states = REGISTRY.getBlockStates().stream()
                .filter(state -> state.getType() == BlockTypes.REDSTONE_WIRE).toList();
        assertEquals(1296, states.size());
        var expected = CloudVoxelShapes.box(0, 0, 0, 1, 1f / 16f, 1);
        for (BlockState state : states) {
            var outline = REGISTRY.requireComponent(BlockTypes.REDSTONE_WIRE, BlockComponents.GET_OUTLINE_SHAPE)
                    .execute(state, BlockShapeContext.empty());
            assertTrue(outline.covers(expected));
            assertTrue(expected.covers(outline));
            assertTrue(REGISTRY.requireComponent(BlockTypes.REDSTONE_WIRE, BlockComponents.GET_COLLISION_SHAPE)
                    .execute(state, BlockShapeContext.empty(), CollisionContext.empty()).isEmpty());
        }
    }

    @Test
    void placementCreatesACrossButResolvingAnIsolatedDotKeepsTheDot() {
        BlockTestWorld world = world();
        assertSame(WIRE, RedstoneWireBlockHandlers.resolveConnections(world.block(POSITION).getLevel(), POSITION, WIRE));
        BlockState placed = placement(world);
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            assertSide(placed, direction, RedstoneSide.SIDE);
        }
    }

    @Test
    void aSingleNeighborExtendsToALineAndACornerDoesNotExtend() {
        BlockTestWorld world = world();
        world.setState(POSITION.add(1, 0, 0), WIRE);
        BlockState line = placement(world);
        assertSide(line, Direction.EAST, RedstoneSide.SIDE);
        assertSide(line, Direction.WEST, RedstoneSide.SIDE);
        assertSide(line, Direction.NORTH, RedstoneSide.NONE);
        assertSide(line, Direction.SOUTH, RedstoneSide.NONE);

        world.setState(POSITION.add(0, 0, -1), WIRE);
        BlockState corner = placement(world);
        assertSide(corner, Direction.EAST, RedstoneSide.SIDE);
        assertSide(corner, Direction.NORTH, RedstoneSide.SIDE);
        assertSide(corner, Direction.WEST, RedstoneSide.NONE);
        assertSide(corner, Direction.SOUTH, RedstoneSide.NONE);
    }

    @Test
    void connectsUpSturdySidesAndAcrossTrapdoorsButNotThroughACeiling() {
        BlockTestWorld world = world();
        world.setState(POSITION.add(0, 0, -1), WIRE);
        world.setState(POSITION.add(1, 0, 0), BlockStates.STONE);
        world.setState(POSITION.add(1, 1, 0), WIRE);
        assertSide(placement(world), Direction.EAST, RedstoneSide.UP);

        world.setState(POSITION.add(0, 1, 0), BlockStates.STONE);
        assertSide(placement(world), Direction.EAST, RedstoneSide.NONE);

        world.setState(POSITION.add(0, 1, 0), BlockStates.AIR);
        world.setState(POSITION.add(1, 0, 0), BlockTypes.OAK_TRAPDOOR.getDefaultState());
        assertSide(placement(world), Direction.EAST, RedstoneSide.SIDE);
    }

    @Test
    void repeatersConnectAlongTheirAxisAndObserversOnlyAtTheirOutput() {
        BlockTestWorld world = world();
        world.setState(POSITION.add(0, 0, -1), WIRE);
        Vector3i east = POSITION.add(1, 0, 0);
        for (var type : List.of(BlockTypes.POWERED_REPEATER, BlockTypes.UNPOWERED_REPEATER)) {
            world.setState(east, type.getDefaultState().withTrait(BlockTraits.CARDINAL_DIRECTION, CardinalDirection.NORTH));
            assertSide(placement(world), Direction.EAST, RedstoneSide.NONE);
            world.setState(east, type.getDefaultState().withTrait(BlockTraits.CARDINAL_DIRECTION, CardinalDirection.WEST));
            assertSide(placement(world), Direction.EAST, RedstoneSide.SIDE);
        }
        world.setState(east, BlockTypes.OBSERVER.getDefaultState().withTrait(BlockTraits.FACING_DIRECTION, Direction.EAST));
        assertSide(placement(world), Direction.EAST, RedstoneSide.SIDE);
        world.setState(east, BlockTypes.OBSERVER.getDefaultState().withTrait(BlockTraits.FACING_DIRECTION, Direction.WEST));
        assertSide(placement(world), Direction.EAST, RedstoneSide.NONE);
    }

    @Test
    void unpoweredSourcesStillConnectButOrdinaryConductorsDoNot() {
        BlockTestWorld world = world();
        world.setState(POSITION.add(0, 0, -1), WIRE);
        for (var type : List.of(BlockTypes.LEVER, BlockTypes.UNLIT_REDSTONE_TORCH, BlockTypes.UNPOWERED_COMPARATOR,
                BlockTypes.OAK_PRESSURE_PLATE, BlockTypes.TARGET, BlockTypes.JUKEBOX)) {
            world.setState(POSITION.add(1, 0, 0), type.getDefaultState());
            assertSide(placement(world), Direction.EAST, RedstoneSide.SIDE);
        }
        world.setState(POSITION.add(1, 0, 0), BlockStates.STONE);
        assertSide(placement(world), Direction.EAST, RedstoneSide.NONE);
    }

    @Test
    void neighborChangesRefreshConnectionsAndPreserveAnIsolatedDot() {
        BlockTestWorld world = world();
        RedstoneWireBlockHandlers.ON_NEIGHBOUR_CHANGED.execute(world.block(POSITION), world.block(POSITION.add(1, 0, 0)));
        assertSame(WIRE, world.state(POSITION));

        world.setState(POSITION.add(1, 0, 0), WIRE);
        RedstoneWireBlockHandlers.ON_NEIGHBOUR_CHANGED.execute(world.block(POSITION), world.block(POSITION.add(1, 0, 0)));
        assertSide(world.state(POSITION), Direction.EAST, RedstoneSide.SIDE);
        assertSide(world.state(POSITION), Direction.WEST, RedstoneSide.SIDE);
        assertTrue(world.getNeighborUpdates().contains(POSITION.add(0, 1, 0)));

        world.setState(POSITION.add(1, 0, 0), BlockStates.AIR);
        RedstoneWireBlockHandlers.ON_NEIGHBOUR_CHANGED.execute(world.block(POSITION), world.block(POSITION.add(1, 0, 0)));
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            assertSide(world.state(POSITION), direction, RedstoneSide.SIDE);
        }
    }

    @Test
    void requiresASturdyTopOrHopperAndBreaksOnSupportLoss() {
        BlockTestWorld world = world();
        assertTrue(RedstoneWireBlockHandlers.CAN_SURVIVE.execute(world.block(POSITION)));
        world.setState(POSITION.add(0, -1, 0), BlockTypes.HOPPER.getDefaultState());
        assertTrue(RedstoneWireBlockHandlers.CAN_SURVIVE.execute(world.block(POSITION)));
        world.setState(POSITION.add(0, -1, 0), BlockStates.AIR);
        RedstoneWireBlockHandlers.ON_NEIGHBOUR_CHANGED.execute(world.block(POSITION), world.block(POSITION.add(0, -1, 0)));
        assertSame(BlockStates.AIR, world.state(POSITION));
        assertEquals(1, world.getBreakCount());
    }

    @Test
    void ceilingUpdatesRemoveAndRestoreUpwardConnections() {
        BlockTestWorld world = world();
        world.setState(POSITION.add(0, 0, -1), WIRE);
        world.setState(POSITION.add(1, 0, 0), BlockStates.STONE);
        world.setState(POSITION.add(1, 1, 0), WIRE);
        world.setState(POSITION, placement(world));
        assertSide(world.state(POSITION), Direction.EAST, RedstoneSide.UP);

        Vector3i ceiling = POSITION.add(0, 1, 0);
        world.setState(ceiling, BlockStates.STONE);
        RedstoneWireBlockHandlers.ON_NEIGHBOUR_CHANGED.execute(world.block(POSITION), world.block(ceiling));
        assertSide(world.state(POSITION), Direction.EAST, RedstoneSide.NONE);

        world.setState(ceiling, BlockStates.AIR);
        RedstoneWireBlockHandlers.ON_NEIGHBOUR_CHANGED.execute(world.block(POSITION), world.block(ceiling));
        assertSide(world.state(POSITION), Direction.EAST, RedstoneSide.UP);
    }

    @Test
    void useTogglesOnlyDotAndCrossPreservesPowerAndRequiresBuildPermission() {
        BlockTestWorld world = world();
        world.setState(POSITION, WIRE.withTrait(BlockTraits.REDSTONE_SIGNAL, 9));
        assertFalse(use(world, false));
        assertTrue(use(world, true));
        assertEquals(9, world.state(POSITION).ensureTrait(BlockTraits.REDSTONE_SIGNAL));
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            assertSide(world.state(POSITION), direction, RedstoneSide.SIDE);
        }
        assertTrue(use(world, true));
        assertSame(WIRE.withTrait(BlockTraits.REDSTONE_SIGNAL, 9), world.state(POSITION));

        world.setState(POSITION.add(1, 0, 0), WIRE);
        world.setState(POSITION, placement(world));
        assertFalse(use(world, true));
    }

    @Test
    void emitsDownAndAlongResolvedConnectionsButNeverUp() {
        BlockTestWorld world = world();
        world.setState(POSITION, WIRE.withTrait(BlockTraits.REDSTONE_SIGNAL, 11));
        assertEquals(11, RedstoneWireBlockHandlers.GET_REDSTONE_SIGNAL.execute(world.block(POSITION), Direction.DOWN));
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            assertEquals(0, RedstoneWireBlockHandlers.GET_REDSTONE_SIGNAL.execute(world.block(POSITION), direction));
        }
        world.setState(POSITION.add(1, 0, 0), WIRE);
        world.setState(POSITION, placement(world).withTrait(BlockTraits.REDSTONE_SIGNAL, 11));
        for (var handler : List.of(RedstoneWireBlockHandlers.GET_REDSTONE_SIGNAL, RedstoneWireBlockHandlers.GET_DIRECT_REDSTONE_SIGNAL)) {
            assertEquals(11, handler.execute(world.block(POSITION), Direction.DOWN));
            assertEquals(11, handler.execute(world.block(POSITION), Direction.EAST));
            assertEquals(11, handler.execute(world.block(POSITION), Direction.WEST));
            assertEquals(0, handler.execute(world.block(POSITION), Direction.UP));
            assertEquals(0, handler.execute(world.block(POSITION), Direction.NORTH));
            assertEquals(0, handler.execute(world.block(POSITION), Direction.SOUTH));
        }
    }

    @Test
    void powersFromSourcesAttenuatesWireAndTurnsOffAfterSourceRemoval() {
        BlockTestWorld world = world();
        world.setState(POSITION.add(-1, 0, 0), BlockTypes.REDSTONE_BLOCK.getDefaultState());
        RedstoneWireBlockHandlers.ON_ADDED.execute(world.block(POSITION));
        assertEquals(15, world.state(POSITION).ensureTrait(BlockTraits.REDSTONE_SIGNAL));
        assertEquals(15, RedstoneSignals.getExternalSignal(world.block(POSITION)));

        Vector3i next = POSITION.add(1, 0, 0);
        world.setState(next.add(0, -1, 0), BlockStates.STONE);
        world.setState(next, WIRE);
        RedstoneWireBlockHandlers.ON_REDSTONE_UPDATE.execute(world.block(next));
        assertEquals(14, world.state(next).ensureTrait(BlockTraits.REDSTONE_SIGNAL));
        assertEquals(0, RedstoneSignals.getExternalSignal(world.block(next)));
        assertEquals(15, RedstoneSignals.getNeighborSignal(world.block(next)));

        world.setState(POSITION.add(-1, 0, 0), BlockStates.AIR);
        for (int i = 0; i < 16; i++) {
            RedstoneWireBlockHandlers.ON_REDSTONE_UPDATE.execute(world.block(POSITION));
            RedstoneWireBlockHandlers.ON_REDSTONE_UPDATE.execute(world.block(next));
        }
        assertEquals(0, world.state(POSITION).ensureTrait(BlockTraits.REDSTONE_SIGNAL));
        assertEquals(0, world.state(next).ensureTrait(BlockTraits.REDSTONE_SIGNAL));
    }

    @Test
    void conductorDoesNotFeedWirePowerBackIntoTheSameWire() {
        BlockTestWorld world = world();
        world.setState(POSITION, WIRE.withTrait(BlockTraits.REDSTONE_SIGNAL, 15));
        assertEquals(15, RedstoneSignals.getNeighborSignal(world.block(POSITION)));
        assertEquals(0, RedstoneSignals.getExternalSignal(world.block(POSITION)));
        assertEquals(0, RedstoneWireBlockHandlers.calculateTargetStrength(world.block(POSITION)));
    }

    @Test
    void receivesStrongPowerThroughAConductor() {
        BlockTestWorld world = world();
        world.setState(POSITION.add(0, -2, 0), BlockTypes.LEVER.getDefaultState()
                .withTrait(BlockTraits.IS_OPEN, true)
                .withTrait(BlockTraits.LEVER_DIRECTION, LeverDirection.DOWN_EAST_WEST));
        assertEquals(15, RedstoneSignals.getExternalSignal(world.block(POSITION)));
    }

    @Test
    void slopePowerRespectsConductorAndCeilingRules() {
        BlockTestWorld world = world();
        world.setState(POSITION.add(1, 0, 0), BlockStates.STONE);
        world.setState(POSITION.add(1, 1, 0), WIRE.withTrait(BlockTraits.REDSTONE_SIGNAL, 14));
        assertEquals(13, RedstoneWireBlockHandlers.calculateTargetStrength(world.block(POSITION)));
        world.setState(POSITION.add(0, 1, 0), BlockStates.STONE);
        assertEquals(0, RedstoneWireBlockHandlers.calculateTargetStrength(world.block(POSITION)));

        world.setState(POSITION.add(1, 1, 0), BlockStates.AIR);
        world.setState(POSITION.add(0, 1, 0), BlockStates.AIR);
        world.setState(POSITION.add(1, 0, 0), BlockStates.AIR);
        world.setState(POSITION.add(1, -1, 0), WIRE.withTrait(BlockTraits.REDSTONE_SIGNAL, 14));
        assertEquals(13, RedstoneWireBlockHandlers.calculateTargetStrength(world.block(POSITION)));
        world.setState(POSITION.add(1, 0, 0), BlockStates.STONE);
        assertEquals(0, RedstoneWireBlockHandlers.calculateTargetStrength(world.block(POSITION)));
    }

    @Test
    void unloadedSignalSourcesAreIgnored() {
        BlockTestWorld world = world();
        Vector3i source = POSITION.add(1, 0, 0);
        world.setState(source, BlockTypes.REDSTONE_BLOCK.getDefaultState());
        assertEquals(15, RedstoneSignals.getExternalSignal(world.block(POSITION)));
        world.unload(source);
        assertEquals(0, RedstoneSignals.getExternalSignal(world.block(POSITION)));
    }

    @Test
    void removalNotifiesTheSecondRingAndStaleUpdatesDoNotRestoreWire() {
        BlockTestWorld world = world();
        Block removed = world.block(POSITION);
        world.setState(POSITION, BlockStates.AIR);
        RedstoneWireBlockHandlers.ON_REMOVE.execute(removed);
        assertEquals(7, world.getNeighborUpdates().size());
        RedstoneWireBlockHandlers.ON_REDSTONE_UPDATE.execute(removed);
        assertSame(BlockStates.AIR, world.state(POSITION));
    }

    private static BlockTestWorld world() {
        return world(REGISTRY);
    }

    private static BlockTestWorld world(Map<BlockType, RedstoneConnectionHandler> customConnections) {
        BlockRegistry registry = InterfaceProxy.create(BlockRegistry.class, (proxy, method, arguments) -> {
            if (method.getName().equals("getComponents") && arguments[0] instanceof BlockType blockType
                    && customConnections.containsKey(blockType)) {
                return new ComponentMap() {
                    @Override
                    @SuppressWarnings("unchecked")
                    public <H> H get(ComponentType<H> type) {
                        if (type == BlockComponents.CONNECTS_TO_REDSTONE_WIRE) {
                            return (H) customConnections.get(blockType);
                        }
                        return REGISTRY.requireComponent(blockType, type);
                    }
                };
            }
            if (method.isDefault()) {
                return InvocationHandler.invokeDefault(proxy, method, arguments);
            }
            return method.invoke(REGISTRY, arguments);
        });
        return world(registry);
    }

    private static BlockTestWorld world(BlockRegistry registry) {
        BlockTestWorld world = new BlockTestWorld(registry);
        world.setState(POSITION.add(0, -1, 0), BlockStates.STONE);
        world.setState(POSITION, WIRE);
        return world;
    }

    private static BlockState placement(BlockTestWorld world) {
        return RedstoneWireBlockHandlers.RESOLVE_PLACEMENT_STATE.execute(WIRE, world.block(POSITION), null, Direction.UP, Vector3f.ZERO);
    }

    private static void assertSide(BlockState state, Direction direction, RedstoneSide side) {
        assertEquals(side, state.ensureTrait(RedstoneWireBlockHandlers.connectionTrait(direction)));
    }

    private static boolean use(BlockTestWorld world, boolean mayBuild) {
        PlayerAbilities abilities = InterfaceProxy.create(PlayerAbilities.class, (proxy, method, arguments) -> {
            if (method.getName().equals("get")) {
                return arguments[0] == Ability.BUILD && mayBuild;
            }

            throw new AssertionError("Unexpected ability access: " + method);
        });
        Player player = InterfaceProxy.create(Player.class, Map.of("getAbilities", abilities));
        return RedstoneWireBlockHandlers.USE.execute(world.block(POSITION), player, Direction.UP, ItemStack.EMPTY);
    }
}
