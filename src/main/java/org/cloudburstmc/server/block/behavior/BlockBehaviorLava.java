package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.math.vector.Vector3i;
import org.cloudburstmc.server.Server;
import org.cloudburstmc.server.block.BlockFactory;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.entity.misc.PrimedTnt;
import org.cloudburstmc.server.event.block.BlockIgniteEvent;
import org.cloudburstmc.server.event.entity.EntityCombustByBlockEvent;
import org.cloudburstmc.server.event.entity.EntityDamageByBlockEvent;
import org.cloudburstmc.server.event.entity.EntityDamageEvent;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.level.gamerule.GameRules;
import org.cloudburstmc.server.math.BlockFace;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.potion.Effect;
import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.Identifier;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class BlockBehaviorLava extends BlockBehaviorLiquid {

    protected BlockBehaviorLava(Identifier id, Identifier flowingId, Identifier stationaryId) {
        super(id, flowingId, stationaryId);
    }

    protected BlockBehaviorLava(Identifier flowingId, Identifier stationaryId) {
        this(flowingId, flowingId, stationaryId);
    }

    @Override
    public int getLightLevel() {
        return 15;
    }

    @Override
    public void onEntityCollide(Entity entity) {
        float highestPos = entity.getHighestPosition();
        entity.setHighestPosition(highestPos - (highestPos - entity.getY()) * 0.5f);

        // Always setting the duration to 15 seconds? TODO
        EntityCombustByBlockEvent ev = new EntityCombustByBlockEvent(this, entity, 15);
        Server.getInstance().getPluginManager().callEvent(ev);
        if (!ev.isCancelled()
                // Making sure the entity is actually alive and not invulnerable.
                && entity.isAlive()
                && entity.getNoDamageTicks() == 0) {
            entity.setOnFire(ev.getDuration());
        }

        if (!entity.hasEffect(Effect.FIRE_RESISTANCE)) {
            entity.attack(new EntityDamageByBlockEvent(this, entity, EntityDamageEvent.DamageCause.LAVA, 4));
        }

        super.onEntityCollide(entity);
    }

    @Override
    public boolean place(Item item, BlockState blockState, BlockState target, BlockFace face, Vector3f clickPos, Player player) {
        boolean ret = this.getLevel().setBlock(this.getPosition(), this, true, false);
        this.getLevel().scheduleUpdate(this, this.tickRate());

        return ret;
    }

    @Override
    public int onUpdate(int type) {
        int result = super.onUpdate(type);

        if (type == Level.BLOCK_UPDATE_RANDOM && this.level.getGameRules().get(GameRules.DO_FIRE_TICK)) {
            Random random = ThreadLocalRandom.current();

            int i = random.nextInt(3);

            if (i > 0) {
                for (int k = 0; k < i; ++k) {
                    Vector3i v = this.getPosition().add(random.nextInt(3) - 1, 1, random.nextInt(3) - 1);
                    BlockState blockState = this.getLevel().getBlock(v);

                    if (blockState.getId() == BlockTypes.AIR) {
                        if (this.isSurroundingBlockFlammable(blockState)) {
                            BlockIgniteEvent e = new BlockIgniteEvent(blockState, this, null, BlockIgniteEvent.BlockIgniteCause.LAVA);
                            this.level.getServer().getPluginManager().callEvent(e);

                            if (!e.isCancelled()) {
                                BlockState fire = BlockState.get(BlockTypes.FIRE);
                                this.getLevel().setBlock(v, fire, true);
                                this.getLevel().scheduleUpdate(fire, fire.tickRate());
                                return Level.BLOCK_UPDATE_RANDOM;
                            }

                            return 0;
                        }
                    } else if (blockState.isSolid()) {
                        return Level.BLOCK_UPDATE_RANDOM;
                    }
                }
            } else {
                for (int k = 0; k < 3; ++k) {
                    Vector3i v = this.getPosition().add(random.nextInt(3) - 1, 0, random.nextInt(3) - 1);
                    BlockState blockState = this.getLevel().getBlock(v);

                    if (blockState.up().getId() == BlockTypes.AIR && blockState.getBurnChance() > 0) {
                        BlockIgniteEvent e = new BlockIgniteEvent(blockState, this, null, BlockIgniteEvent.BlockIgniteCause.LAVA);
                        this.level.getServer().getPluginManager().callEvent(e);

                        if (!e.isCancelled()) {
                            BlockState fire = BlockState.get(BlockTypes.FIRE);
                            this.getLevel().setBlock(v, fire, true);
                            this.getLevel().scheduleUpdate(fire, fire.tickRate());
                        }
                    }
                }
            }
        }

        return result;
    }

    protected boolean isSurroundingBlockFlammable(BlockState blockState) {
        for (BlockFace face : BlockFace.values()) {
            if (blockState.getSide(face).getBurnChance() > 0) {
                return true;
            }
        }

        return false;
    }

    @Override
    public BlockColor getColor() {
        return BlockColor.LAVA_BLOCK_COLOR;
    }

    @Override
    public BlockState getBlock(int meta) {
        return BlockState.get(BlockTypes.FLOWING_LAVA, meta);
    }

    @Override
    public int tickRate() {
        return 30;
    }

    @Override
    public int getFlowDecayPerBlock() {
        if (this.level.getDimension() == Level.DIMENSION_NETHER) {
            return 1;
        }
        return 2;
    }

    @Override
    protected void checkForHarden() {
        BlockState colliding = null;
        for (int side = 1; side < 6; ++side) { //don't check downwards side
            BlockState blockStateSide = this.getSide(BlockFace.fromIndex(side));
            if (blockStateSide instanceof BlockBehaviorWater
                    || (blockStateSide = blockStateSide.layer(1)) instanceof BlockBehaviorWater) {
                colliding = blockStateSide;
                break;
            }
        }
        if (colliding != null) {
            if (this.getMeta() == 0) {
                this.liquidCollide(colliding, BlockState.get(BlockTypes.OBSIDIAN));
            } else if (this.getMeta() <= 4) {
                this.liquidCollide(colliding, BlockState.get(BlockTypes.COBBLESTONE));
            }
        }
    }

    @Override
    protected void flowIntoBlock(BlockState blockState, int newFlowDecay) {
        if (blockState instanceof BlockBehaviorWater) {
            ((BlockBehaviorLiquid) blockState).liquidCollide(this, BlockState.get(BlockTypes.STONE));
        } else {
            super.flowIntoBlock(blockState, newFlowDecay);
        }
    }

    @Override
    public Vector3f addVelocityToEntity(Entity entity, Vector3f vector) {
        if (!(entity instanceof PrimedTnt)) {
            return super.addVelocityToEntity(entity, vector);
        }
        return vector;
    }


    public static BlockFactory factory(Identifier stationaryId) {
        return id -> new BlockBehaviorLava(id, stationaryId);
    }
}
