package org.cloudburstmc.server.level.feature.tree;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.data.MultipleFacing;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.level.generator.GenerationRegion;
import org.cloudburstmc.server.level.generator.standard.misc.IntRange;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.cloudburstmc.server.testutil.BlockTestWorld;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TreeVineDirectionTest {

    @Test
    void jungleVinesFaceTheirSupportingTrunkInsteadOfUsingDirectionIndices() {
        FeatureJungleTree normal = new FeatureJungleTree(new IntRange(4), GenerationTreeSpecies.JUNGLE);
        FeatureHugeJungleTree huge = new FeatureHugeJungleTree(new IntRange(12), GenerationTreeSpecies.JUNGLE);

        for (Direction face : Direction.Plane.HORIZONTAL) {
            BlockTestWorld world = new BlockTestWorld(BlockTestRegistry.get());
            GenerationRegion region = region(world);
            normal.placeVines(region, new Random(0), 0, 64, 0, face);
            huge.placeVines(region, new Random(0), 8, 64, 0, face);

            Vector3i normalPosition = face.getOpposite().relative(Vector3i.from(0, 64, 0));
            Vector3i hugePosition = face.getOpposite().relative(Vector3i.from(8, 64, 0));
            assertEquals(Set.of(face), world.state(normalPosition).getData(MultipleFacing.class).orElseThrow().getFaces());
            assertEquals(Set.of(face), world.state(hugePosition).getData(MultipleFacing.class).orElseThrow().getFaces());
        }
    }

    @Test
    void hangingSwampVinesKeepTheLeafFacingOnEverySegment() {
        FeatureSwampTree tree = new FeatureSwampTree(new IntRange(4), GenerationTreeSpecies.OAK);
        for (Direction outward : Direction.Plane.HORIZONTAL) {
            BlockTestWorld world = new BlockTestWorld(BlockTestRegistry.get());
            tree.placeVines(region(world), new Random(0), 0, 64, 0, outward, BlockStates.OAK_LEAVES);

            for (int y = 61; y <= 64; y++) {
                BlockState vine = world.state(Vector3i.from(0, y, 0));
                assertEquals(Set.of(outward.getOpposite()), vine.getData(MultipleFacing.class).orElseThrow().getFaces());
                int expected = switch (outward.getOpposite()) {
                    case SOUTH -> 1;
                    case WEST -> 2;
                    case NORTH -> 4;
                    case EAST -> 8;
                    default -> throw new AssertionError("Expected horizontal direction");
                };
                assertEquals(expected, vine.ensureTrait(BlockTraits.VINE_DIRECTION_BITS));
            }
        }
    }

    private static GenerationRegion region(BlockTestWorld world) {
        return InterfaceProxy.create(GenerationRegion.class, (proxy, method, arguments) -> {
            if (method.isDefault()) {
                return InvocationHandler.invokeDefault(proxy, method, arguments);
            }

            Vector3i position = Vector3i.from((int) arguments[0], (int) arguments[1], (int) arguments[2]);
            return switch (method.getName()) {
                case "getBlockState" -> world.state(position);
                case "setBlockState" -> {
                    world.setState(position, (BlockState) arguments[4]);
                    yield true;
                }
                default -> throw new AssertionError("Unexpected generation access: " + method);
            };
        });
    }
}
