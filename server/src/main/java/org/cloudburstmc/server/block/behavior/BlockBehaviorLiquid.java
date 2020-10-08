package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.GenericMath;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.SoundEvent;
import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import lombok.val;
import lombok.var;
import org.cloudburstmc.server.block.*;
import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.event.block.BlockFromToEvent;
import org.cloudburstmc.server.event.block.LiquidFlowEvent;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.level.Sound;
import org.cloudburstmc.server.level.particle.SmokeParticle;
import org.cloudburstmc.server.math.AxisAlignedBB;
import org.cloudburstmc.server.math.Direction;

import javax.annotation.Nonnull;
import java.util.concurrent.ThreadLocalRandom;

import static org.cloudburstmc.server.block.BlockTypes.AIR;

public abstract class BlockBehaviorLiquid extends BlockBehaviorTransparent {

    private static final byte CAN_FLOW_DOWN = 1;
    private static final byte CAN_FLOW = 0;
    private static final byte BLOCKED = -1;
    protected final BlockType flowingId;
    protected final BlockType stationaryId;
    public int adjacentSources = 0;
    protected Vector3f flowVector = null;
    private Long2ByteMap flowCostVisited = new Long2ByteOpenHashMap();

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

    @Override
    public AxisAlignedBB getBoundingBox() {
        return null;
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
                blockDecay = this.getEffectiveFlowDecay(this.getLiquidBlock(level, side.getX(), side.getY() - 1, side.getZ()));
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
    public int onUpdate(Block block, int type) {
        if (type == Level.BLOCK_UPDATE_NORMAL) {
            this.checkForHarden(block);
            // This check exists because if water is at layer1 with air at layer0, the water gets invisible
            if (usesWaterLogging() && block.getExtra() != BlockStates.AIR) {
                val mainBlockState = block.getState();
                val behavior = mainBlockState.getBehavior();
                val liquid = block.getExtra();

                if (mainBlockState.getType() == AIR) {
                    block.set(mainBlockState, 1, true, false);
                    block.set(liquid, 0, true, false);
                } else if (!behavior.canWaterlogSource(mainBlockState) || !behavior.canWaterlogFlowing(mainBlockState) && liquid.ensureTrait(BlockTraits.FLUID_LEVEL) > 0) {
                    removeBlock(block);
                    return type;
                }
            }

            block.getLevel().scheduleUpdate(block.getPosition(), this.tickRate());
            return type;
        } else if (type == Level.BLOCK_UPDATE_SCHEDULED) {
            BlockState currentState;

            if (block.getExtra() != BlockStates.AIR) {
                currentState = block.getExtra();
            } else {
                currentState = block.getState();
            }

            val level = block.getLevel();
            int decay = this.getFlowDecay(currentState);
            int multiplier = this.getFlowDecayPerBlock(block);
            if (decay > 0) {
                int smallestFlowDecay = -100;
                this.adjacentSources = 0;
                smallestFlowDecay = this.getSmallestFlowDecay(this.getLiquidBlock(level, block.getX(), block.getY(), block.getZ() - 1), smallestFlowDecay);
                smallestFlowDecay = this.getSmallestFlowDecay(this.getLiquidBlock(level, block.getX(), block.getY(), block.getZ() + 1), smallestFlowDecay);
                smallestFlowDecay = this.getSmallestFlowDecay(this.getLiquidBlock(level, block.getX() - 1, block.getY(), block.getZ()), smallestFlowDecay);
                smallestFlowDecay = this.getSmallestFlowDecay(this.getLiquidBlock(level, block.getX() + 1, block.getY(), block.getZ()), smallestFlowDecay);
                int newDecay = smallestFlowDecay + multiplier;
                if (newDecay >= 8 || smallestFlowDecay < 0) {
                    newDecay = -1;
                }
                int topFlowDecay = getFlowDecay(getLiquidBlock(level, block.getX(), block.getY() + 1, block.getZ()));
                if (topFlowDecay >= 0) {
                    newDecay = topFlowDecay | 0x08;
                }
                if (adjacentSources >= 2 && block instanceof BlockBehaviorWater) {
                    BlockState bottomBlockState = getLiquidBlock(level, block.getX(), block.getY() - 1, block.getZ());
                    if (bottomBlockState.inCategory(BlockCategory.SOLID)) {
                        newDecay = 0;
                    } else if (bottomBlockState instanceof BlockBehaviorWater && bottomBlockState.ensureTrait(BlockTraits.FLUID_LEVEL) == 0) {
                        newDecay = 0;
                    }
                }
                if (newDecay != decay) {
                    decay = newDecay;
                    boolean decayed = decay < 0;
                    BlockState to;
                    if (decayed) {
                        to = BlockState.get(AIR);
                    } else {
                        to = getState(decay);
                    }
                    BlockFromToEvent event = new BlockFromToEvent(block, to);
                    level.getServer().getEventManager().fire(event);
                    if (!event.isCancelled()) {
                        block.set(event.getTo(), 1, true, true);

                        if (!decayed) {
                            level.scheduleUpdate(block.getPosition(), this.tickRate());
                        }
                    }
                }
            }
            if (decay >= 0) {
                Block bottomBlock = level.getBlock(block.getX(), block.getY() - 1, block.getZ());
                this.flowIntoBlock(bottomBlock, decay | 0x08);
                if (decay == 0 || !canBlockBeFlooded(bottomBlock.getState())) {
                    int adjacentDecay;
                    if (decay >= 8) {
                        adjacentDecay = 1;
                    } else {
                        adjacentDecay = decay + multiplier;
                    }
                    if (adjacentDecay < 8) {
                        boolean[] flags = this.getOptimalFlowDirections(block);
                        if (flags[0]) {
                            this.flowIntoBlock(level.getBlock(block.getX() - 1, block.getY(), block.getZ()), adjacentDecay);
                        }
                        if (flags[1]) {
                            this.flowIntoBlock(level.getBlock(block.getX() + 1, block.getY(), block.getZ()), adjacentDecay);
                        }
                        if (flags[2]) {
                            this.flowIntoBlock(level.getBlock(block.getX(), block.getY(), block.getZ() - 1), adjacentDecay);
                        }
                        if (flags[3]) {
                            this.flowIntoBlock(level.getBlock(block.getX(), block.getY(), block.getZ() + 1), adjacentDecay);
                        }
                    }
                }
                this.checkForHarden(block.refresh());
            }
            return type;
        }
        return 0;
    }

    protected void flowIntoBlock(Block block, int newFlowDecay) {
        var state = block.getState();

        if (this.canFlowInto(block) && !state.inCategory(BlockCategory.LIQUID)) {
            val level = block.getLevel();
            boolean waterlog = false;

            if (usesWaterLogging()) {
                BlockState liquid = block.getExtra();
                if (liquid.inCategory(BlockCategory.LIQUID)) {
                    return;
                }

                if (!state.getBehavior().canWaterlogFlowing(state)) {
                    state = liquid;
                } else {
                    waterlog = true;
                }
            }

            LiquidFlowEvent event = new LiquidFlowEvent(state, block, newFlowDecay);
            level.getServer().getEventManager().fire(event);
            if (!event.isCancelled()) {

                if (waterlog) {
                    block.setExtra(getState(newFlowDecay), true);
                } else {
                    if (state.getType() != AIR) {
                        level.useBreakOn(block.getPosition());
                    }

                    block.set(getState(newFlowDecay), true);
                }

                level.scheduleUpdate(block.getPosition(), this.tickRate());
            }
        }
    }

    private int calculateFlowCost(Level level, int blockX, int blockY, int blockZ, int accumulatedCost, int maxCost, int originOpposite, int lastOpposite) {
        int cost = 1000;
        for (int j = 0; j < 4; ++j) {
            if (j == originOpposite || j == lastOpposite) {
                continue;
            }
            int x = blockX;
            int y = blockY;
            int z = blockZ;
            if (j == 0) {
                --x;
            } else if (j == 1) {
                ++x;
            } else if (j == 2) {
                --z;
            } else {
                ++z;
            }

            long hash = blockHash(x, y, z);
            if (!this.flowCostVisited.containsKey(hash)) {
                Block blockStateSide = level.getBlock(x, y, z);
                if (!this.canFlowInto(blockStateSide)) {
                    this.flowCostVisited.put(hash, BLOCKED);
                } else if (canBlockBeFlooded(level.getBlockAt(x, y - 1, z))) {
                    this.flowCostVisited.put(hash, CAN_FLOW_DOWN);
                } else {
                    this.flowCostVisited.put(hash, CAN_FLOW);
                }
            }
            byte status = this.flowCostVisited.get(hash);
            if (status == BLOCKED) {
                continue;
            } else if (status == CAN_FLOW_DOWN) {
                return accumulatedCost;
            }
            if (accumulatedCost >= maxCost) {
                continue;
            }
            int realCost = this.calculateFlowCost(level, x, y, z, accumulatedCost + 1, maxCost, originOpposite, j ^ 0x01);
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
        for (int j = 0; j < 4; ++j) {
            int x = block.getX();
            int y = block.getY();
            int z = block.getZ();
            if (j == 0) {
                --x;
            } else if (j == 1) {
                ++x;
            } else if (j == 2) {
                --z;
            } else {
                ++z;
            }
            Block blockState = level.getBlock(x, y, z);
            if (!this.canFlowInto(blockState)) {
                this.flowCostVisited.put(blockHash(x, y, z), BLOCKED);
            } else if (canBlockBeFlooded(level.getBlockAt(x, y - 1, z))) {
                this.flowCostVisited.put(blockHash(x, y, z), CAN_FLOW_DOWN);
                flowCost[j] = maxCost = 0;
            } else if (maxCost > 0) {
                this.flowCostVisited.put(blockHash(x, y, z), CAN_FLOW);
                flowCost[j] = this.calculateFlowCost(level, x, y, z, 1, maxCost, j ^ 0x01, j ^ 0x01);
                maxCost = Math.min(maxCost, flowCost[j]);
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
        level.addSound(pos.add(0.5, 0.5, 0.5), Sound.RANDOM_FIZZ, 1, 2.6F + (ThreadLocalRandom.current().nextFloat() - ThreadLocalRandom.current().nextFloat()) * 0.8F);

        for (int i = 0; i < 8; ++i) {
            level.addParticle(new SmokeParticle(pos.toFloat().add(Math.random(), 1.2, Math.random())));
        }
    }

    @Nonnull
    public BlockState getLiquidBlock(Level level, int x, int y, int z) {
        return getLiquidBlock(level.getBlock(x, y, z));
    }

    @Nonnull
    public BlockState getLiquidBlock(Block block) {
        if (block.getExtra() != BlockStates.AIR) {
            return block.getExtra();
        }

        return block.getState();
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
        cause.getLevel().addLevelSoundEvent(cause.getPosition(), SoundEvent.FIZZ);
        return true;
    }

    protected boolean canFlowInto(Block block) {
        val state = block.getState();
        if (canBlockBeFlooded(state) && !(state.inCategory(BlockCategory.LIQUID) && state.ensureTrait(BlockTraits.FLUID_LEVEL) == 0)) {
            if (usesWaterLogging()) {
                BlockState layer1 = block.getExtra();
                return !(layer1.inCategory(BlockCategory.LIQUID) && layer1.ensureTrait(BlockTraits.FLUID_LEVEL) == 0);
            }
            return true;
        }
        return false;
    }

    private boolean canBlockBeFlooded(BlockState state) {
        val behavior = state.getBehavior();
        return behavior.canBeFlooded(state) || (usesWaterLogging() && behavior.canWaterlogFlowing(state));
    }

    protected BlockState getState(int decay) {
        return BlockState.get(flowingId)
                .withTrait(BlockTraits.FLUID_LEVEL, decay & 0x7)
                .withTrait(BlockTraits.IS_FLOWING, (decay & 0x8) == 0x8);
    }

    @Override
    public ItemStack toItem(Block block) {
        return ItemStack.get(AIR);
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

    private static long blockHash(int x, int y, int z) {
        if (y < 0 || y >= 256) {
            throw new IllegalArgumentException("Y coordinate y is out of range!");
        }
        return (((long) x & (long) 0xFFFFFFF) << 36) | (((long) y & (long) 0xFF) << 28) | ((long) z & (long) 0xFFFFFFF);
    }
}
