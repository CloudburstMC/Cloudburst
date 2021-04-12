package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.GenericMath;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.protocol.bedrock.data.SoundEvent;
import it.unimi.dsi.fastutil.objects.Object2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import lombok.val;
import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.event.block.BlockFromToEvent;
import org.cloudburstmc.api.event.block.LiquidFlowEvent;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.util.AxisAlignedBB;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.Direction.Plane;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.level.Sound;
import org.cloudburstmc.server.level.particle.SmokeParticle;
import org.cloudburstmc.server.registry.CloudBlockRegistry;
import org.cloudburstmc.server.registry.CloudItemRegistry;

import java.util.concurrent.ThreadLocalRandom;

public abstract class BlockBehaviorLiquid extends BlockBehaviorTransparent {

    protected final BlockType flowingId;
    protected final BlockType stationaryId;
    public int adjacentSources = 0;
    protected Vector3f flowVector = null;
    private final Object2ReferenceMap<Vector3i, FlowState> flowCostVisited = new Object2ReferenceOpenHashMap<>();

    public BlockBehaviorLiquid(BlockType flowingId, BlockType stationaryId) {
        this.flowingId = flowingId;
        this.stationaryId = stationaryId;
    }

    protected AxisAlignedBB recalculateBoundingBox() {
        return null;
    }

    public ItemStack[] getDrops(Block block, ItemStack hand) {
        return new ItemStack[0];
    }

    @Override
    public boolean hasEntityCollision() {
        return true;
    }

    @Override
    public boolean isLiquid() {
        return true;
    }

    public float getMaxY(Block block) {
        return block.getY() + 1 - getFluidHeightPercent(block.getState());
    }
//
//    @Override //TODO: bounding box
//    protected AxisAlignedBB recalculateCollisionBoundingBox() {
//        return this;
//    }

    public static float getFluidHeightPercent(BlockState state) {
        return (state.ensureTrait(BlockTraits.FLUID_LEVEL) + 1) / 9f;
    }

    protected int getFlowDecay(BlockState state) {
        if (!isSameLiquid(state.getType())) {
            return -1;
        }

        return state.ensureTrait(BlockTraits.FLUID_LEVEL);
    }

    protected int getEffectiveFlowDecay(BlockState state) {
        if (!isSameLiquid(state.getType())) {
            return -1;
        }

        return state.ensureTrait(BlockTraits.FLUID_LEVEL);
    }

    public void clearCaches() {
        this.flowVector = null;
        this.flowCostVisited.clear();
    }

    public Vector3f getFlowVector(Block block) {
        if (this.flowVector != null) {
            return this.flowVector;
        }
        Vector3f vector = Vector3f.ZERO;
        val state = block.getState();
        val level = block.getLevel();
        int decay = this.getEffectiveFlowDecay(state);
        for (Direction face : Direction.Plane.HORIZONTAL) {
            val side = block.getSide(face);
            val sideState = side.getState();

            int blockDecay = this.getEffectiveFlowDecay(sideState);
            if (blockDecay < 0) {
                if (!canBlockBeFlooded(sideState)) {
                    continue;
                }
                blockDecay = this.getEffectiveFlowDecay(side.down().getLiquid());
                if (blockDecay >= 0) {
                    int realDecay = blockDecay - (decay - 8);
                    vector = vector.add(side.getPosition().toFloat().sub(block.getPosition().toFloat()).mul(realDecay));
                }
            } else {
                int realDecay = blockDecay - decay;
                vector = vector.add(side.getPosition().toFloat().sub(block.getPosition().toFloat()).mul(realDecay));
            }
        }
        if (state.ensureTrait(BlockTraits.IS_FLOWING)) {
            if (!this.canFlowInto(level.getBlock(block.getX(), block.getY(), block.getZ() - 1)) ||
                    !this.canFlowInto(level.getBlock(block.getX(), block.getY(), block.getZ() + 1)) ||
                    !this.canFlowInto(level.getBlock(block.getX() - 1, block.getY(), block.getZ())) ||
                    !this.canFlowInto(level.getBlock(block.getX() + 1, block.getY(), block.getZ())) ||
                    !this.canFlowInto(level.getBlock(block.getX(), block.getY() + 1, block.getZ() - 1)) ||
                    !this.canFlowInto(level.getBlock(block.getX(), block.getY() + 1, block.getZ() + 1)) ||
                    !this.canFlowInto(level.getBlock(block.getX() - 1, block.getY() + 1, block.getZ())) ||
                    !this.canFlowInto(level.getBlock(block.getX() + 1, block.getY() + 1, block.getZ()))) {
                vector = vector.normalize().add(0, -6, 0);
            }
        }
        return this.flowVector = GenericMath.normalizeSafe(vector);
    }

    @Override
    public Vector3f addVelocityToEntity(Block block, Vector3f vector, Entity entity) {
        if (entity.canBeMovedByCurrents()) {
            Vector3f flow = this.getFlowVector(block);
            return vector.add(flow);
        }
        return vector;
    }

    public int getFlowDecayPerBlock(Block block) {
        return 1;
    }

    @Override
    public int onUpdate(final Block block, int type) {
        if (type == Level.BLOCK_UPDATE_NORMAL) {
            this.checkForHarden(block);
            // This check exists because if water is at layer1 with air at layer0, the water gets invisible
            if (usesWaterLogging() && block.getExtra() != BlockStates.AIR) {
                val mainBlockState = block.getState();
//                val behavior = mainBlockState.getBehavior();

                if (mainBlockState == BlockStates.AIR) {
                    val liquid = block.getExtra();
                    block.set(mainBlockState, 1, true, false);
                    block.set(liquid, 0, true, false);
                }
//                else if (!behavior.canWaterlogSource(mainBlockState) || !behavior.canWaterlogFlowing(mainBlockState) && liquid.ensureTrait(BlockTraits.FLUID_LEVEL) > 0) {
//                    removeBlock(block);
//                    return type;
//                }
            }

            block.getLevel().scheduleUpdate(block.getPosition(), this.tickRate());
            return type;
        } else if (type == Level.BLOCK_UPDATE_SCHEDULED) {
            int layer = block.getLiquidLayer();
            BlockState currentState = block.getState(layer);

            val level = block.getLevel();
            int decay = this.getFlowDecay(currentState);
            boolean falling = currentState.ensureTrait(BlockTraits.IS_FLOWING);
            int multiplier = this.getFlowDecayPerBlock(block);
            if (decay > 0) {
                int smallestFlowDecay = -100;
                this.adjacentSources = 0;

                for (Direction direction : Plane.HORIZONTAL) {
                    smallestFlowDecay = this.getSmallestFlowDecay(block.getSide(direction).getLiquid(), smallestFlowDecay);
                }

                int newDecay = smallestFlowDecay + multiplier;
                if (newDecay >= 8 || smallestFlowDecay < 0) {
                    newDecay = -1;
                }

                boolean newFalling = false;
                int topFlowDecay = getFlowDecay(block.up().getLiquid());
                if (topFlowDecay >= 0) {
                    newFalling = true;
                    newDecay = 1;
                }

                if (adjacentSources >= 2 && isWater(currentState.getType())) {
                    BlockState bottomBlockState = block.down().getLiquid();
                    if (bottomBlockState.getBehavior().isSolid(bottomBlockState)) {
                        newDecay = 0;
                    } else if (isWater(bottomBlockState.getType()) && bottomBlockState.ensureTrait(BlockTraits.FLUID_LEVEL) == 0 && !bottomBlockState.ensureTrait(BlockTraits.IS_FLOWING)) {
                        newDecay = 0;
                    }
                }

                if (newDecay != decay || falling != newFalling) {
                    decay = newDecay;
                    falling = newFalling;
                    boolean decayed = decay < 0;
                    BlockState to;
                    if (decayed) {
                        to = BlockStates.AIR;
                    } else {
                        to = getState(decay, falling);
                    }
                    BlockFromToEvent event = new BlockFromToEvent(block, to);
                    level.getServer().getEventManager().fire(event);
                    if (!event.isCancelled()) {
                        block.set(event.getTo(), layer, true, true);

                        if (!decayed) {
                            level.scheduleUpdate(block.getPosition(), this.tickRate());
                        }
                    }
                }
            }
            if (decay >= 0 && decay < 7) {
                Block bottomBlock = block.down();
                this.flowIntoBlock(bottomBlock, decay, true);
                if (decay == 0 || !canBlockBeFlooded(bottomBlock.getState())) {
                    int adjacentDecay;
                    if (falling) {
                        adjacentDecay = 1;
                    } else {
                        adjacentDecay = decay + multiplier;
                    }

                    boolean[] flags = this.getOptimalFlowDirections(block);
                    for (Direction direction : Plane.HORIZONTAL) {
                        if (flags[direction.getHorizontalIndex()]) {
                            this.flowIntoBlock(block.getSide(direction), adjacentDecay, false);
                        }
                    }
                }
                this.checkForHarden(block.refresh());
            }
            return type;
        }
        return 0;
    }

    protected void flowIntoBlock(final Block block, int newFlowDecay, boolean falling) {
        val state = block.getLiquid();

        if (this.canFlowInto(block) && !state.inCategory(BlockCategory.LIQUID)) {
            val level = block.getLevel();
            boolean waterlog = usesWaterLogging() && state.getBehavior().canWaterlogFlowing(state);

            LiquidFlowEvent event = new LiquidFlowEvent(state, block, newFlowDecay);
            level.getServer().getEventManager().fire(event);
            if (!event.isCancelled()) {
                val newState = getState(newFlowDecay, falling);
                if (waterlog) {
                    block.setExtra(newState, true, true);
                } else {
                    if (state != BlockStates.AIR) {
                        level.useBreakOn(block.getPosition());
                    }

                    block.set(newState, true, true);
                }
            }
        }
    }

    private int calculateFlowCost(Level level, Vector3i blockPos, int accumulatedCost, int maxCost, Direction originOpposite, Direction lastOpposite) {
        int cost = 1000;

        for (Direction direction : Plane.HORIZONTAL) {
            if (direction == originOpposite || direction == lastOpposite) {
                continue;
            }

            val pos = direction.getOffset(blockPos);

            if (!this.flowCostVisited.containsKey(pos)) {
                Block side = level.getBlock(pos);
                if (!this.canFlowInto(side)) {
                    this.flowCostVisited.put(pos, FlowState.BLOCKED);
                } else if (canBlockBeFlooded(side.downState())) {
                    this.flowCostVisited.put(pos, FlowState.DOWN);
                } else {
                    this.flowCostVisited.put(pos, FlowState.NORMAL);
                }
            }

            val status = this.flowCostVisited.get(pos);
            if (status == FlowState.BLOCKED) {
                continue;
            } else if (status == FlowState.DOWN) {
                return accumulatedCost;
            }

            if (accumulatedCost >= maxCost) {
                continue;
            }

            int realCost = this.calculateFlowCost(level, pos, accumulatedCost + 1, maxCost, originOpposite, direction.getOpposite());
            if (realCost < cost) {
                cost = realCost;
            }
        }

        return cost;
    }


    private boolean[] getOptimalFlowDirections(Block block) {
        int[] flowCost = new int[]{
                1000,
                1000,
                1000,
                1000
        };
        int maxCost = 4 / this.getFlowDecayPerBlock(block);
        val level = block.getLevel();

        for (Direction direction : Plane.HORIZONTAL) {
            Block b = block.getSide(direction);

            if (!this.canFlowInto(b)) {
                this.flowCostVisited.put(b.getPosition(), FlowState.BLOCKED);
            } else if (canBlockBeFlooded(block.downState())) {
                this.flowCostVisited.put(b.getPosition(), FlowState.DOWN);
                flowCost[direction.getHorizontalIndex()] = maxCost = 0;
            } else if (maxCost > 0) {
                this.flowCostVisited.put(b.getPosition(), FlowState.NORMAL);
                flowCost[direction.getHorizontalIndex()] = this.calculateFlowCost(level, b.getPosition(), 1, maxCost, direction.getOpposite(), direction.getOpposite());
                maxCost = Math.min(maxCost, flowCost[direction.getHorizontalIndex()]);
            }
        }

        this.flowCostVisited.clear();
        float minCost = Float.MAX_VALUE;
        for (int i = 0; i < 4; i++) {
            float d = flowCost[i];
            if (d < minCost) {
                minCost = d;
            }
        }
        boolean[] isOptimalFlowDirection = new boolean[4];
        for (int i = 0; i < 4; ++i) {
            isOptimalFlowDirection[i] = (flowCost[i] == minCost);
        }
        return isOptimalFlowDirection;
    }

    private int getSmallestFlowDecay(BlockState blockState, int decay) {
        int blockDecay = this.getFlowDecay(blockState);
        if (blockDecay < 0) {
            return decay;
        } else if (blockDecay == 0) {
            ++this.adjacentSources;
        } else if (blockDecay >= 8) {
            blockDecay = 0;
        }
        return (decay >= 0 && blockDecay >= decay) ? decay : blockDecay;
    }

    protected void checkForHarden(Block block) {
    }

    protected void triggerLavaMixEffects(Level level, Vector3f pos) {
        ((CloudLevel) level).addSound(pos.add(0.5, 0.5, 0.5), Sound.RANDOM_FIZZ, 1, 2.6F + (ThreadLocalRandom.current().nextFloat() - ThreadLocalRandom.current().nextFloat()) * 0.8F);

        for (int i = 0; i < 8; ++i) {
            ((CloudLevel) level).addParticle(new SmokeParticle(pos.toFloat().add(Math.random(), 1.2, Math.random())));
        }
    }

    @Override
    public void onEntityCollide(Block block, Entity entity) {
        entity.resetFallDistance();
    }

    protected boolean liquidCollide(Block cause, BlockState result) {
        BlockFromToEvent event = new BlockFromToEvent(cause, result);
        cause.getLevel().getServer().getEventManager().fire(event);
        if (event.isCancelled()) {
            return false;
        }

        cause.set(event.getTo(), true);
        ((CloudLevel) cause.getLevel()).addLevelSoundEvent(cause.getPosition(), SoundEvent.FIZZ);
        return true;
    }

    protected boolean canFlowInto(Block block) {
        val state = block.getLiquid();

        if (!canBlockBeFlooded(state)) {
            return false;
        }

        if (state.inCategory(BlockCategory.LIQUID)) {
            return state.ensureTrait(BlockTraits.FLUID_LEVEL) != 0;
        }

        return true;
    }

    private boolean canBlockBeFlooded(BlockState state) {
        return state.getBehavior().canBeFlooded(state);
    }

    protected BlockState getState(int decay, boolean falling) {
        return CloudBlockRegistry.get().getBlock(flowingId)
                .withTrait(BlockTraits.FLUID_LEVEL, decay & 0x7)
                .withTrait(BlockTraits.IS_FLOWING, falling);
    }

    @Override
    public ItemStack toItem(Block block) {
        return CloudItemRegistry.AIR;
    }

    public boolean usesWaterLogging() {
        return false;
    }

    public boolean isSameLiquid(BlockType other) {
        return other == getFlowingType() || other == getStationaryId();
    }

    public BlockType getFlowingType() {
        return flowingId;
    }

    public BlockType getStationaryId() {
        return stationaryId;
    }

    public static boolean isWater(BlockType type) {
        return type == BlockTypes.WATER || type == BlockTypes.FLOWING_WATER;
    }

    public enum FlowState {
        NORMAL,
        DOWN,
        BLOCKED
    }
}
