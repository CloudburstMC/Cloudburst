package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.GenericMath;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.SoundEvent;
import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.event.block.BlockFromToEvent;
import org.cloudburstmc.server.event.block.LiquidFlowEvent;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.level.Sound;
import org.cloudburstmc.server.level.particle.SmokeParticle;
import org.cloudburstmc.server.math.AxisAlignedBB;
import org.cloudburstmc.server.math.BlockFace;
import org.cloudburstmc.server.utils.Identifier;

import javax.annotation.Nonnull;
import java.util.concurrent.ThreadLocalRandom;

import static org.cloudburstmc.server.block.BlockTypes.AIR;

public abstract class BlockBehaviorLiquid extends BlockBehaviorTransparent {

    private static final byte CAN_FLOW_DOWN = 1;
    private static final byte CAN_FLOW = 0;
    private static final byte BLOCKED = -1;
    protected final Identifier flowingId;
    protected final Identifier stationaryId;
    public int adjacentSources = 0;
    protected Vector3f flowVector = null;
    private Long2ByteMap flowCostVisited = new Long2ByteOpenHashMap();

    public BlockBehaviorLiquid(Identifier flowingId, Identifier stationaryId) {
        this.flowingId = flowingId;
        this.stationaryId = stationaryId;
    }

    @Override
    public boolean canBeFlooded() {
        return true;
    }

    protected AxisAlignedBB recalculateBoundingBox() {
        return null;
    }

    public Item[] getDrops(BlockState blockState, Item hand) {
        return new Item[0];
    }

    @Override
    public boolean hasEntityCollision() {
        return true;
    }

    @Override
    public boolean isBreakable(Item item) {
        return false;
    }

    @Override
    public boolean canBeReplaced() {
        return true;
    }

    @Override
    public boolean isSolid() {
        return false;
    }

    @Override
    public boolean canHarvestWithHand() {
        return false;
    }

    @Override
    public AxisAlignedBB getBoundingBox(Block block) {
        return null;
    }

    @Override
    public float getMaxY() {
        return this.getY() + 1 - getFluidHeightPercent();
    }

    @Override
    protected AxisAlignedBB recalculateCollisionBoundingBox() {
        return this;
    }

    public float getFluidHeightPercent() {
        float d = (float) this.getMeta();
        if (d >= 8) {
            d = 0;
        }

        return (d + 1) / 9f;
    }

    protected int getFlowDecay(BlockState blockState) {
        if (!isSameLiquid(blockState.getId())) {
            if (blockState.getLayer() != 1 && blockState.isWaterlogged()) {
                return getFlowDecay(this.getLiquidBlock(blockState));
            }
            return -1;
        }
        return blockState.getMeta();
    }

    protected int getEffectiveFlowDecay(BlockState blockState) {
        if (!isSameLiquid(blockState.getId())) {
            return -1;
        }
        int decay = blockState.getMeta();
        if (decay >= 8) {
            decay = 0;
        }
        return decay;
    }

    public void clearCaches() {
        this.flowVector = null;
        this.flowCostVisited.clear();
    }

    public Vector3f getFlowVector() {
        if (this.flowVector != null) {
            return this.flowVector;
        }
        Vector3f vector = Vector3f.ZERO;
        int decay = this.getEffectiveFlowDecay(this);
        for (BlockFace face : BlockFace.Plane.HORIZONTAL) {
            BlockState sideBlockState = this.getSide(face);

            int blockDecay = this.getEffectiveFlowDecay(sideBlockState);
            if (blockDecay < 0) {
                if (!canBlockBeFlooded(sideBlockState)) {
                    continue;
                }
                blockDecay = this.getEffectiveFlowDecay(this.getLiquidBlock(sideBlockState.getX(), sideBlockState.getY() - 1, sideBlockState.getZ()));
                if (blockDecay >= 0) {
                    int realDecay = blockDecay - (decay - 8);
                    vector = vector.add(sideBlockState.getPosition().toFloat().sub(this.getPosition().toFloat()).mul(realDecay));
                }
            } else {
                int realDecay = blockDecay - decay;
                vector = vector.add(sideBlockState.getPosition().toFloat().sub(this.getPosition().toFloat()).mul(realDecay));
            }
        }
        if (this.getMeta() >= 8) {
            if (!this.canFlowInto(this.level.getBlock(this.getX(), this.getY(), this.getZ() - 1)) ||
                    !this.canFlowInto(this.level.getBlock(this.getX(), this.getY(), this.getZ() + 1)) ||
                    !this.canFlowInto(this.level.getBlock(this.getX() - 1, this.getY(), this.getZ())) ||
                    !this.canFlowInto(this.level.getBlock(this.getX() + 1, this.getY(), this.getZ())) ||
                    !this.canFlowInto(this.level.getBlock(this.getX(), this.getY() + 1, this.getZ() - 1)) ||
                    !this.canFlowInto(this.level.getBlock(this.getX(), this.getY() + 1, this.getZ() + 1)) ||
                    !this.canFlowInto(this.level.getBlock(this.getX() - 1, this.getY() + 1, this.getZ())) ||
                    !this.canFlowInto(this.level.getBlock(this.getX() + 1, this.getY() + 1, this.getZ()))) {
                vector = vector.normalize().add(0, -6, 0);
            }
        }
        return this.flowVector = GenericMath.normalizeSafe(vector);
    }

    @Override
    public Vector3f addVelocityToEntity(Entity entity, Vector3f vector) {
        if (entity.canBeMovedByCurrents()) {
            Vector3f flow = this.getFlowVector();
            return vector.add(flow);
        }
        return vector;
    }

    public int getFlowDecayPerBlock() {
        return 1;
    }

    @Override
    public int onUpdate(Block block, int type) {
        if (type == Level.BLOCK_UPDATE_NORMAL) {
            this.checkForHarden();
            // This check exists because if water is at layer1 with air at layer0, the water gets invisible
            if (usesWaterLogging() && layer > 0) {
                BlockState mainBlockState = layer(0);
                if (mainBlockState.getId() == AIR) {
                    this.level.setBlock(this.getPosition(), 1, mainBlockState, true, false);
                    this.level.setBlock(this.getPosition(), 0, this, true, false);
                } else if (!mainBlockState.canWaterlogSource() || !mainBlockState.canWaterlogFlowing() && this.getMeta() > 0) {
                    this.level.setBlock(this.getPosition(), 1, BlockState.get(AIR), true, true);
                    return type;
                }
            }
            this.level.scheduleUpdate(this, this.tickRate());
            return type;
        } else if (type == Level.BLOCK_UPDATE_SCHEDULED) {
            int decay = this.getFlowDecay(this);
            int multiplier = this.getFlowDecayPerBlock();
            if (decay > 0) {
                int smallestFlowDecay = -100;
                this.adjacentSources = 0;
                smallestFlowDecay = this.getSmallestFlowDecay(this.getLiquidBlock(this.getX(), this.getY(), this.getZ() - 1), smallestFlowDecay);
                smallestFlowDecay = this.getSmallestFlowDecay(this.getLiquidBlock(this.getX(), this.getY(), this.getZ() + 1), smallestFlowDecay);
                smallestFlowDecay = this.getSmallestFlowDecay(this.getLiquidBlock(this.getX() - 1, this.getY(), this.getZ()), smallestFlowDecay);
                smallestFlowDecay = this.getSmallestFlowDecay(this.getLiquidBlock(this.getX() + 1, this.getY(), this.getZ()), smallestFlowDecay);
                int newDecay = smallestFlowDecay + multiplier;
                if (newDecay >= 8 || smallestFlowDecay < 0) {
                    newDecay = -1;
                }
                int topFlowDecay = this.getFlowDecay(this.getLiquidBlock(this.getX(), this.getY() + 1, this.getZ()));
                if (topFlowDecay >= 0) {
                    newDecay = topFlowDecay | 0x08;
                }
                if (this.adjacentSources >= 2 && this instanceof BlockBehaviorWater) {
                    BlockState bottomBlockState = this.getLiquidBlock(this.getX(), this.getY() - 1, this.getZ());
                    if (bottomBlockState.isSolid()) {
                        newDecay = 0;
                    } else if (bottomBlockState instanceof BlockBehaviorWater && bottomBlockState.getMeta() == 0) {
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
                        to = getBlock(decay);
                    }
                    BlockFromToEvent event = new BlockFromToEvent(this, to);
                    level.getServer().getPluginManager().callEvent(event);
                    if (!event.isCancelled()) {
                        this.level.setBlock(this.getX(), this.getY(), this.getZ(), this.getLayer(), event.getTo(), true, true);
                        if (!decayed) {
                            this.level.scheduleUpdate(this, this.tickRate());
                        }
                    }
                }
            }
            if (decay >= 0) {
                BlockState bottomBlockState = this.getLevel().getBlock(this.getX(), this.getY() - 1, this.getZ());
                this.flowIntoBlock(bottomBlockState, decay | 0x08);
                if (decay == 0 || !canBlockBeFlooded(bottomBlockState)) {
                    int adjacentDecay;
                    if (decay >= 8) {
                        adjacentDecay = 1;
                    } else {
                        adjacentDecay = decay + multiplier;
                    }
                    if (adjacentDecay < 8) {
                        boolean[] flags = this.getOptimalFlowDirections();
                        if (flags[0]) {
                            this.flowIntoBlock(this.level.getBlock(this.getX() - 1, this.getY(), this.getZ()), adjacentDecay);
                        }
                        if (flags[1]) {
                            this.flowIntoBlock(this.level.getBlock(this.getX() + 1, this.getY(), this.getZ()), adjacentDecay);
                        }
                        if (flags[2]) {
                            this.flowIntoBlock(this.level.getBlock(this.getX(), this.getY(), this.getZ() - 1), adjacentDecay);
                        }
                        if (flags[3]) {
                            this.flowIntoBlock(this.level.getBlock(this.getX(), this.getY(), this.getZ() + 1), adjacentDecay);
                        }
                    }
                }
                this.checkForHarden();
            }
            return type;
        }
        return 0;
    }

    protected void flowIntoBlock(BlockState blockState, int newFlowDecay) {
        if (this.canFlowInto(blockState) && !(blockState instanceof BlockBehaviorLiquid)) {
            if (usesWaterLogging()) {
                BlockState layer1 = blockState.layer(1);
                if (layer1 instanceof BlockBehaviorLiquid) {
                    return;
                }

                if (blockState.canWaterlogFlowing()) {
                    blockState = layer1;
                }
            }
            LiquidFlowEvent event = new LiquidFlowEvent(blockState, this, newFlowDecay);
            getLevel().getServer().getPluginManager().callEvent(event);
            if (!event.isCancelled()) {
                if (blockState.getId() != AIR) {
                    this.level.useBreakOn(blockState.getPosition());
                }
                this.level.setBlock(blockState.getPosition(), getBlock(newFlowDecay), true, true);
                this.level.scheduleUpdate(blockState, this.tickRate());
            }
        }
    }

    private int calculateFlowCost(int blockX, int blockY, int blockZ, int accumulatedCost, int maxCost, int originOpposite, int lastOpposite) {
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
            } else if (j == 3) {
                ++z;
            }
            long hash = BlockState.key(x, y, z);
            if (!this.flowCostVisited.containsKey(hash)) {
                BlockState blockStateSide = this.level.getBlock(x, y, z);
                if (!this.canFlowInto(blockStateSide)) {
                    this.flowCostVisited.put(hash, BLOCKED);
                } else if (canBlockBeFlooded(this.level.getBlock(x, y - 1, z))) {
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
            int realCost = this.calculateFlowCost(x, y, z, accumulatedCost + 1, maxCost, originOpposite, j ^ 0x01);
            if (realCost < cost) {
                cost = realCost;
            }
        }
        return cost;
    }

    @Override
    public float getHardness() {
        return 100f;
    }

    @Override
    public float getResistance() {
        return 500;
    }

    private boolean[] getOptimalFlowDirections() {
        int[] flowCost = new int[]{
                1000,
                1000,
                1000,
                1000
        };
        int maxCost = 4 / this.getFlowDecayPerBlock();
        for (int j = 0; j < 4; ++j) {
            int x = this.getX();
            int y = this.getY();
            int z = this.getZ();
            if (j == 0) {
                --x;
            } else if (j == 1) {
                ++x;
            } else if (j == 2) {
                --z;
            } else {
                ++z;
            }
            BlockState blockState = this.level.getBlock(x, y, z);
            if (!this.canFlowInto(blockState)) {
                this.flowCostVisited.put(BlockState.key(x, y, z), BLOCKED);
            } else if (canBlockBeFlooded(this.level.getBlock(x, y - 1, z))) {
                this.flowCostVisited.put(BlockState.key(x, y, z), CAN_FLOW_DOWN);
                flowCost[j] = maxCost = 0;
            } else if (maxCost > 0) {
                this.flowCostVisited.put(BlockState.key(x, y, z), CAN_FLOW);
                flowCost[j] = this.calculateFlowCost(x, y, z, 1, maxCost, j ^ 0x01, j ^ 0x01);
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

    protected void checkForHarden() {
    }

    protected void triggerLavaMixEffects(Vector3f pos) {
        this.getLevel().addSound(pos.add(0.5, 0.5, 0.5), Sound.RANDOM_FIZZ, 1, 2.6F + (ThreadLocalRandom.current().nextFloat() - ThreadLocalRandom.current().nextFloat()) * 0.8F);

        for (int i = 0; i < 8; ++i) {
            this.getLevel().addParticle(new SmokeParticle(pos.toFloat().add(Math.random(), 1.2, Math.random())));
        }
    }

    public abstract BlockState getBlock(int meta);

    @Nonnull
    public BlockState getLiquidBlock(int x, int y, int z) {
        BlockState b = getLevel().getBlock(x, y, z);
        if (b.isWaterlogged()) {
            return getLevel().getBlock(x, y, z, 1);
        }
        return b;
    }

    @Nonnull
    public BlockState getLiquidBlock(BlockState blockState) {
        if (blockState.getLayer() == 1) return blockState;
        return getLiquidBlock(blockState.getPosition().getX(), blockState.getPosition().getY(), blockState.getPosition().getZ());
    }

    @Override
    public boolean canPassThrough() {
        return true;
    }

    @Override
    public void onEntityCollide(Entity entity) {
        entity.resetFallDistance();
    }

    protected boolean liquidCollide(BlockState cause, BlockState result) {
        BlockFromToEvent event = new BlockFromToEvent(this, result);
        this.level.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        }
        this.level.setBlock(this.getPosition(), event.getTo(), true, true);
        this.getLevel().addLevelSoundEvent(this.getPosition(), SoundEvent.FIZZ);
        return true;
    }

    protected boolean canFlowInto(BlockState blockState) {
        if (canBlockBeFlooded(blockState) && !(blockState instanceof BlockBehaviorLiquid && blockState.getMeta() == 0)) {
            if (usesWaterLogging()) {
                BlockState layer1 = blockState.layer(1);
                return !(layer1 instanceof BlockBehaviorLiquid && layer1.getMeta() == 0);
            }
            return true;
        }
        return false;
    }

    private boolean canBlockBeFlooded(BlockState blockState) {
        return blockState.canBeFlooded() || (usesWaterLogging() && blockState.canWaterlogFlowing());
    }

    @Override
    public Item toItem(BlockState state) {
        return Item.get(AIR, 0, 0);
    }

    public boolean usesWaterLogging() {
        return false;
    }

    public boolean isSameLiquid(Identifier other) {
        return other == getFlowingId() || other == getStationaryId();
    }

    public Identifier getFlowingId() {
        return flowingId;
    }

    public Identifier getStationaryId() {
        return stationaryId;
    }
}
