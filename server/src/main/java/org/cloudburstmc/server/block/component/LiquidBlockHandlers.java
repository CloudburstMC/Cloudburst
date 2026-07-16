package org.cloudburstmc.server.block.component;

import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.event.block.*;
import org.cloudburstmc.api.level.gamerule.GameRules;
import org.cloudburstmc.api.util.BoundingBox;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.VoxelShape;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.level.Sound;
import org.cloudburstmc.server.level.collision.CloudVoxelShapes;
import org.cloudburstmc.server.utils.Hash;

import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class LiquidBlockHandlers {
    private static final int OCCLUSION_CACHE_SIZE = 2048;
    private static final Direction[] DIRECTIONS = Direction.values();
    private static final ThreadLocal<OcclusionResult[]> OCCLUSION_CACHE =
            ThreadLocal.withInitial(() -> new OcclusionResult[OCCLUSION_CACHE_SIZE]);

    public static void tick(Block block) {
        LiquidState liquid = block.getLiquid();
        if (liquid.isEmpty()) {
            return;
        }

        LiquidFamily family = LiquidFamily.of(liquid);
        CloudLevel level = (CloudLevel) block.getLevel();
        Vector3i pos = block.getPosition();
        if (family.harden(level, pos)) {
            return;
        }

        if (family == LiquidFamily.WATER && liquid.isSource()) {
            BubbleColumnBlockHandlers.update(block);
        }

        LiquidState next = family.nextState(level, pos, liquid);
        if (LiquidStateAccess.blockState(next) != LiquidStateAccess.blockState(liquid)) {
            LiquidLevelChangeEvent event = new LiquidLevelChangeEvent(block, next);
            level.getServer().getEventManager().fire(event);

            if (event.isCancelled()) {
                return;
            }

            next = event.getNewLiquid();
        }

        if (next.isEmpty()) {
            level.removeLiquid(pos);
            return;
        }

        if (LiquidStateAccess.blockState(next) != LiquidStateAccess.blockState(liquid)) {
            level.setLiquidState(pos, next);
            level.scheduleLiquidUpdate(pos, next, family.spreadDelay(level, pos, liquid, next));
        }

        Vector3i below = Direction.DOWN.relative(pos);
        Block belowBlock = loadedBlock(level, below);
        if (belowBlock == null) {
            return;
        }

        if (family == LiquidFamily.LAVA && LiquidFamily.WATER.matches(belowBlock.getLiquid())) {
            if (form(level, below, BlockStates.STONE)) {
                level.addSound(below, Sound.RANDOM_FIZZ, 0.5f, 2.6f);
            }
            return;
        }

        LiquidState belowLiquid = family.nextState(level, below, belowBlock.getLiquid());
        boolean flowedDown = canFlowInto(level, pos, below, belowLiquid, family);
        if (flowedDown) {
            flowInto(block, belowBlock, belowLiquid, family);
            if (sourceCount(level, pos, family) < 3) {
                return;
            }
        }

        int depth = next.getAmount();
        int spreadDepth = next.isFalling() ? 7 : depth - family.dropOff(level);

        if (spreadDepth > 0) {
            for (SpreadTarget spread : spreadTargets(level, pos, family)) {
                if (canMaybeFlowInto(block, spread.block(), pos, spread.block().getPosition(), family)
                        && accepts(spread.block().getState(), spread.liquid(), family)) {
                    flowInto(block, spread.block(), spread.liquid(), family);
                }
            }
        }
    }

    public static void schedule(Block block) {
        LiquidState liquid = block.getLiquid();
        if (!liquid.isEmpty()) {
            CloudLevel level = (CloudLevel) block.getLevel();
            level.scheduleLiquidUpdate(block.getPosition(), liquid, tickDelay(level, block.getPosition(), liquid));
        }
    }

    public static int tickDelay(CloudLevel level, Vector3i position, LiquidState liquid) {
        return LiquidFamily.of(liquid).tickDelay(level, position);
    }

    public static boolean canOccupySecondaryLayer(LiquidState liquid) {
        return LiquidFamily.of(liquid).canBeContained();
    }

    public static void randomTick(Block block, RandomGenerator random) {
        CloudLevel level = (CloudLevel) block.getLevel();
        if (!level.getGameRules().get(GameRules.DO_FIRE_TICK)) {
            return;
        }

        Vector3i origin = block.getPosition();
        int passes = random.nextInt(3);
        if (passes > 0) {
            Vector3i target = origin;
            for (int pass = 0; pass < passes; pass++) {
                target = target.add(random.nextInt(3) - 1, 1, random.nextInt(3) - 1);

                Block candidate = loadedBlock(level, target);
                if (candidate == null) {
                    return;
                }

                if (candidate.getState() == BlockStates.AIR) {
                    if (hasFlammableNeighbor(level, target)) {
                        ignite(level, candidate);
                        return;
                    }
                } else if (candidate.getState().getCollisionShape().covers(CloudVoxelShapes.block())) {
                    return;
                }
            }
        } else {
            for (int pass = 0; pass < 3; pass++) {
                Vector3i supportPos = origin.add(random.nextInt(3) - 1, 0, random.nextInt(3) - 1);
                Block support = loadedBlock(level, supportPos);
                Block above = loadedBlock(level, Direction.UP.relative(supportPos));

                if (support == null || above == null) {
                    return;
                }

                if (above.getState() == BlockStates.AIR && isFlammable(support)) {
                    ignite(level, above);
                }
            }
        }
    }

    private static boolean hasFlammableNeighbor(CloudLevel level, Vector3i pos) {
        for (Direction direction : DIRECTIONS) {
            Block block = loadedBlock(level, direction.relative(pos));
            if (block != null && isFlammable(block)) {
                return true;
            }
        }

        return false;
    }

    private static boolean isFlammable(Block block) {
        BlockState state = block.getState();
        return state.getFlameOdds() > 0;
    }

    private static void ignite(CloudLevel level, Block target) {
        BlockIgniteEvent event = new BlockIgniteEvent(target, null, null, BlockIgniteEvent.BlockIgniteCause.LAVA);
        level.getServer().getEventManager().fire(event);
        if (!event.isCancelled()) {
            target.set(BlockStates.FIRE);
        }
    }

    static boolean form(CloudLevel level, Vector3i pos, BlockState state) {
        BlockFormEvent event = new BlockFormEvent(level.getBlock(pos), state);
        level.getServer().getEventManager().fire(event);
        if (event.isCancelled()) {
            return false;
        }

        level.removeLiquid(pos);
        level.setBlockState(pos, event.getNewState());
        return true;
    }

    public static Vector3f flow(CloudLevel level, Vector3i pos, LiquidState liquid) {
        LiquidFamily family = LiquidFamily.of(liquid);
        int ownDecay = effectiveDecay(liquid, family);
        float x = 0;
        float z = 0;

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            Vector3i adjacentPos = direction.relative(pos);
            Block adjacentBlock = loadedBlock(level, adjacentPos);
            if (adjacentBlock == null) {
                continue;
            }

            LiquidState adjacent = adjacentBlock.getLiquid();
            int adjacentDecay = effectiveDecay(adjacent, family);
            boolean contributes = adjacentDecay >= 0;
            if (adjacentDecay < 0) {
                BlockState adjacentState = adjacentBlock.getState();
                LiquidReaction reaction = adjacentState.getLiquidReaction();
                if (!reaction.allowsFlow() && !reaction.removesBlock()) {
                    continue;
                }

                Block below = loadedBlock(level, Direction.DOWN.relative(adjacentPos));
                adjacentDecay = below == null ? -1 : effectiveDecay(below.getLiquid(), family);
                if (adjacentDecay >= 0) {
                    adjacentDecay -= ownDecay - 8;
                    contributes = true;
                }
            } else {
                adjacentDecay -= ownDecay;
            }

            if (contributes) {
                x += direction.getStepX() * adjacentDecay;
                z += direction.getStepZ() * adjacentDecay;
            }
        }

        Vector3f flow = Vector3f.from(x, 0, z);
        if (flow.lengthSquared() > 0) {
            flow = flow.normalize();
        }

        if (liquid.isFalling()) {
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                Vector3i side = direction.relative(pos);
                if (isFlowBlocked(level, pos, side, family)
                        && isFlowBlocked(level, Direction.UP.relative(pos), Direction.UP.relative(side), family)) {
                    flow = Vector3f.from(flow.getX(), -6, flow.getZ()).normalize();
                    break;
                }
            }
        }

        return flow;
    }

    private static int effectiveDecay(LiquidState state, LiquidFamily family) {
        if (!family.matches(state)) {
            return -1;
        }

        return state.isFalling() ? 0 : 8 - state.getAmount();
    }

    static LiquidState nextLiquid(CloudLevel level, Vector3i pos, LiquidFamily family) {
        int highestAmount = 0;
        int sources = 0;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            Vector3i adjacentPos = direction.relative(pos);
            Block adjacentBlock = loadedBlock(level, adjacentPos);
            if (adjacentBlock == null) {
                continue;
            }

            LiquidState adjacent = adjacentBlock.getLiquid();
            if (!family.matches(adjacent) || faceBlocked(level.getBlockState(pos), adjacentBlock.getState(), direction)) {
                continue;
            }

            highestAmount = Math.max(highestAmount, adjacent.getAmount());
            if (adjacent.isSource()) {
                sources++;
            }
        }

        if (family.formsSources() && sources >= 2) {
            Block supportBlock = loadedBlock(level, Direction.DOWN.relative(pos));
            if (supportBlock == null) {
                return LiquidState.empty();
            }

            LiquidState below = supportBlock.getLiquid();
            BlockState support = supportBlock.getState();

            if (support.getCollisionShape().getFaceShape(Direction.UP).covers(CloudVoxelShapes.block())
                    || family.matches(below) && below.isSource()) {
                return family.source();
            }
        }

        Block aboveBlock = loadedBlock(level, Direction.UP.relative(pos));
        if (aboveBlock != null) {
            LiquidState above = aboveBlock.getLiquid();
            if (family.matches(above) && !faceBlocked(level.getBlockState(pos), aboveBlock.getState(), Direction.UP)) {
                return family.falling();
            }
        }

        int nextAmount = highestAmount - family.dropOff(level);
        return nextAmount > 0 ? family.flowing(nextAmount) : LiquidState.empty();
    }

    private static int sourceCount(CloudLevel level, Vector3i pos, LiquidFamily family) {
        int count = 0;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            Block adjacent = loadedBlock(level, direction.relative(pos));
            if (adjacent == null) {
                continue;
            }
            LiquidState state = adjacent.getLiquid();
            if (family.matches(state) && state.isSource()) {
                count++;
            }
        }
        return count;
    }

    private static List<SpreadTarget> spreadTargets(CloudLevel level, Vector3i pos, LiquidFamily family) {
        SpreadContext context = new SpreadContext(level, family);
        List<SpreadTarget> targets = new ArrayList<>(4);
        int best = Integer.MAX_VALUE;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            Vector3i target = direction.relative(pos);
            if (!context.canMaybeFlowInto(pos, target)) {
                continue;
            }

            Block targetBlock = context.block(target);
            if (targetBlock == null) {
                continue;
            }

            LiquidState targetLiquid = family.nextState(level, target, targetBlock.getLiquid());
            if (!accepts(targetBlock.getState(), targetLiquid, family)) {
                continue;
            }

            int cost = context.isHole(target) ? 0 : slopeCost(context, target, direction.getOpposite(), 1);
            if (cost < best) {
                targets.clear();
                best = cost;
            }

            if (cost == best) {
                targets.add(new SpreadTarget(targetBlock, targetLiquid));
            }
        }

        return targets;
    }

    private static int slopeCost(SpreadContext context, Vector3i pos, Direction excluded, int distance) {
        int best = Integer.MAX_VALUE;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (direction == excluded) {
                continue;
            }

            Vector3i target = direction.relative(pos);
            if (!context.canMaybeFlowInto(pos, target)) {
                continue;
            }

            if (context.isHole(target)) {
                return distance;
            }

            if (distance < context.family.slopeDistance(context.level)) {
                best = Math.min(best, slopeCost(context, target, direction.getOpposite(), distance + 1));
            }
        }

        return best;
    }

    private static boolean isFlowBlocked(CloudLevel level, Vector3i from, Vector3i to, LiquidFamily family) {
        Block target = loadedBlock(level, to);
        if (target == null) {
            return true;
        }

        return !canFlowInto(level, from, to, family.nextState(level, to, target.getLiquid()), family);
    }

    private static boolean canFlowInto(CloudLevel level, Vector3i from, Vector3i to, LiquidState liquid, LiquidFamily family) {
        Block source = loadedBlock(level, from);
        Block target = loadedBlock(level, to);
        return canMaybeFlowInto(source, target, from, to, family) && accepts(target.getState(), liquid, family);
    }

    private static boolean canMaybeFlowInto(@Nullable Block source, @Nullable Block target, Vector3i from, Vector3i to, LiquidFamily family) {
        if (source == null || target == null) {
            return false;
        }

        LiquidState existingLiquid = target.getLiquid();
        if (!existingLiquid.isEmpty() && !family.matches(existingLiquid)) {
            return false;
        }

        if (family.matches(existingLiquid) && existingLiquid.isSource()) {
            return false;
        }

        BlockState state = target.getState();
        boolean canHoldAny = state == BlockStates.AIR || state.getType().isLiquid()
                || family.canBeContained() && (state.canContainLiquidSource() || state.canContainFlowingLiquid())
                || state.getLiquidReaction().removesBlock();
        return canHoldAny && !faceBlocked(source.getState(), state, direction(from, to));
    }

    private static boolean accepts(BlockState state, LiquidState liquid, LiquidFamily family) {
        return state == BlockStates.AIR || state.getType().isLiquid()
                || family.canBeContained() && (liquid.isSource()
                ? state.canContainLiquidSource() : state.canContainFlowingLiquid())
                || state.getLiquidReaction().removesBlock();
    }

    private static Direction direction(Vector3i from, Vector3i to) {
        int dx = to.getX() - from.getX();
        int dy = to.getY() - from.getY();
        int dz = to.getZ() - from.getZ();

        for (Direction direction : DIRECTIONS) {
            if (direction.getStepX() == dx && direction.getStepY() == dy && direction.getStepZ() == dz) {
                return direction;
            }
        }

        throw new IllegalArgumentException("Positions are not adjacent");
    }

    static @Nullable Block loadedBlock(CloudLevel level, Vector3i pos) {
        return level.getLoadedBlock(pos.getX(), pos.getY(), pos.getZ());
    }

    private static boolean faceBlocked(BlockState from, BlockState to, Direction direction) {
        int index = (System.identityHashCode(from) * 31
                ^ System.identityHashCode(to) * 17
                ^ direction.ordinal()) & (OCCLUSION_CACHE_SIZE - 1);
        OcclusionResult[] cache = OCCLUSION_CACHE.get();
        OcclusionResult cached = cache[index];
        if (cached != null && cached.from == from && cached.to == to && cached.direction == direction) {
            return cached.blocked;
        }

        VoxelShape fromShape = from.getCollisionShape();
        VoxelShape toShape = to.getCollisionShape();

        boolean blocked;
        if (fromShape.isEmpty() && toShape.isEmpty()) {
            blocked = false;
        } else if (CloudVoxelShapes.isFullBlock(fromShape) || CloudVoxelShapes.isFullBlock(toShape)) {
            blocked = true;
        } else {
            blocked = computeFaceBlocked(fromShape, toShape, direction);
        }

        cache[index] = new OcclusionResult(from, to, direction, blocked);
        return blocked;
    }

    private static boolean computeFaceBlocked(VoxelShape from, VoxelShape to, Direction direction) {
        VoxelShape first = from.getFaceShape(direction);
        VoxelShape second = to.getFaceShape(direction.getOpposite());

        int count = first.getBoundingBoxes().size() + second.getBoundingBoxes().size();
        float[] boxes = new float[count * 6];
        int offset = 0;

        for (VoxelShape shape : new VoxelShape[]{first, second}) {
            for (BoundingBox box : shape.getBoundingBoxes()) {
                boxes[offset++] = box.getMinX();
                boxes[offset++] = box.getMinY();
                boxes[offset++] = box.getMinZ();
                boxes[offset++] = box.getMaxX();
                boxes[offset++] = box.getMaxY();
                boxes[offset++] = box.getMaxZ();
            }
        }

        return CloudVoxelShapes.fromBoxes(boxes).covers(CloudVoxelShapes.block());
    }

    private record OcclusionResult(BlockState from, BlockState to, Direction direction, boolean blocked) {
    }

    private record SpreadTarget(Block block, LiquidState liquid) {
    }

    private static final class SpreadContext {
        private final CloudLevel level;
        private final LiquidFamily family;
        private final Long2ObjectOpenHashMap<Block> blocks = new Long2ObjectOpenHashMap<>();
        private final Long2ByteOpenHashMap[] flow = new Long2ByteOpenHashMap[DIRECTIONS.length];

        private SpreadContext(CloudLevel level, LiquidFamily family) {
            this.level = level;
            this.family = family;
        }

        private boolean canMaybeFlowInto(Vector3i from, Vector3i to) {
            Direction direction = direction(from, to);
            long key = Hash.hashBlock(to.getX(), to.getY(), to.getZ());
            int directionIndex = direction.ordinal();

            Long2ByteOpenHashMap directionFlow = this.flow[directionIndex];
            if (directionFlow == null) {
                directionFlow = new Long2ByteOpenHashMap();
                directionFlow.defaultReturnValue((byte) -1);
                this.flow[directionIndex] = directionFlow;
            }

            byte cached = directionFlow.get(key);
            if (cached >= 0) {
                return cached != 0;
            }

            boolean result = LiquidBlockHandlers.canMaybeFlowInto(block(from), block(to), from, to, this.family);
            directionFlow.put(key, (byte) (result ? 1 : 0));
            return result;
        }

        private boolean isHole(Vector3i pos) {
            Vector3i below = Direction.DOWN.relative(pos);
            Block target = block(below);
            return target != null && canMaybeFlowInto(pos, below)
                    && accepts(target.getState(), this.family.nextState(this.level, below, target.getLiquid()), this.family);
        }

        private @Nullable Block block(Vector3i pos) {
            long key = Hash.hashBlock(pos.getX(), pos.getY(), pos.getZ());
            if (this.blocks.containsKey(key)) {
                return this.blocks.get(key);
            }

            Block block = loadedBlock(this.level, pos);
            this.blocks.put(key, block);
            return block;
        }
    }

    private static void flowInto(Block source, Block target, LiquidState liquid, LiquidFamily family) {
        CloudLevel level = (CloudLevel) source.getLevel();
        Vector3i targetPos = target.getPosition();
        LiquidState existing = target.getLiquid();

        if (family.matches(existing) && existing.getAmount() >= liquid.getAmount()) {
            return;
        }

        LiquidFlowEvent event = new LiquidFlowEvent(source, target, liquid);
        level.getServer().getEventManager().fire(event);
        if (event.isCancelled()) {
            return;
        }

        LiquidReaction reaction = target.getState().getLiquidReaction();
        if (family == LiquidFamily.LAVA && target.getState() != BlockStates.AIR) {
            level.addSound(targetPos, Sound.RANDOM_FIZZ, 0.5f, 2.6f);
        }

        if (reaction.removesBlock() && !destroyTarget(source, target, liquid, reaction)) {
            return;
        }

        if (level.setLiquidState(targetPos, liquid)) {
            level.scheduleLiquidUpdate(targetPos, liquid, family.tickDelay(level, targetPos));
        }
    }

    private static boolean destroyTarget(Block source, Block target, LiquidState liquid, LiquidReaction reaction) {
        CloudLevel level = (CloudLevel) source.getLevel();
        LiquidDestroyBlockEvent event = new LiquidDestroyBlockEvent(source, target, liquid);
        level.getServer().getEventManager().fire(event);
        if (event.isCancelled()) {
            return false;
        }

        if (reaction == LiquidReaction.POPPED) {
            level.useBreakOn(target.getPosition());
        } else {
            target.set(BlockStates.AIR);
        }

        return true;
    }
}
