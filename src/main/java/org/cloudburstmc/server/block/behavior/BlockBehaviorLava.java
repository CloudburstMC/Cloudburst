package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.math.vector.Vector3i;
import lombok.val;
import org.cloudburstmc.server.Server;
import org.cloudburstmc.server.block.*;
import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.entity.misc.PrimedTnt;
import org.cloudburstmc.server.event.block.BlockIgniteEvent;
import org.cloudburstmc.server.event.entity.EntityCombustByBlockEvent;
import org.cloudburstmc.server.event.entity.EntityDamageByBlockEvent;
import org.cloudburstmc.server.event.entity.EntityDamageEvent;
import org.cloudburstmc.server.item.behavior.Item;
import org.cloudburstmc.server.world.World;
import org.cloudburstmc.server.world.gamerule.GameRules;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.potion.Effect;
import org.cloudburstmc.server.utils.BlockColor;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import static org.cloudburstmc.server.block.BlockIds.FLOWING_LAVA;
import static org.cloudburstmc.server.block.BlockIds.LAVA;

public class BlockBehaviorLava extends BlockBehaviorLiquid {

    public BlockBehaviorLava() {
        super(FLOWING_LAVA, LAVA);
    }

    @Override
    public int getLightLevel(Block block) {
        return 15;
    }

    @Override
    public void onEntityCollide(Block block, Entity entity) {
        float highestPos = entity.getHighestPosition();
        entity.setHighestPosition(highestPos - (highestPos - entity.getY()) * 0.5f);

        // Always setting the duration to 15 seconds? TODO
        EntityCombustByBlockEvent ev = new EntityCombustByBlockEvent(block, entity, 15);
        Server.getInstance().getEventManager().fire(ev);
        if (!ev.isCancelled()
                // Making sure the entity is actually alive and not invulnerable.
                && entity.isAlive()
                && entity.getNoDamageTicks() == 0) {
            entity.setOnFire(ev.getDuration());
        }

        if (!entity.hasEffect(Effect.FIRE_RESISTANCE)) {
            entity.attack(new EntityDamageByBlockEvent(block, entity, EntityDamageEvent.DamageCause.LAVA, 4));
        }

        super.onEntityCollide(block, entity);
    }

    @Override
    public boolean place(Item item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        boolean ret = placeBlock(block, BlockState.get(BlockIds.FLOWING_LAVA));

        block.getWorld().scheduleUpdate(block.getPosition(), this.tickRate());
        return ret;
    }

    @Override
    public int onUpdate(Block block, int type) {
        int result = super.onUpdate(block, type);

        if (type == World.BLOCK_UPDATE_RANDOM && block.getWorld().getGameRules().get(GameRules.DO_FIRE_TICK)) {
            val pos = block.getPosition();
            val level = block.getWorld();

            Random random = ThreadLocalRandom.current();

            int i = random.nextInt(3);

            if (i > 0) {
                for (int k = 0; k < i; ++k) {
                    Vector3i v = pos.add(random.nextInt(3) - 1, 1, random.nextInt(3) - 1);
                    val b = level.getBlock(v);
                    val state = b.getState();

                    if (state.getType() == BlockIds.AIR) {
                        if (this.isSurroundingBlockFlammable(b)) {
                            BlockIgniteEvent e = new BlockIgniteEvent(b, block, null, BlockIgniteEvent.BlockIgniteCause.LAVA);
                            level.getServer().getEventManager().fire(e);

                            if (!e.isCancelled()) {
                                BlockState fire = BlockState.get(BlockIds.FIRE);
                                b.set(fire, true);
                                level.scheduleUpdate(v, fire.getBehavior().tickRate());
                                return World.BLOCK_UPDATE_RANDOM;
                            }

                            return 0;
                        }
                    } else if (state.inCategory(BlockCategory.SOLID)) {
                        return World.BLOCK_UPDATE_RANDOM;
                    }
                }
            } else {
                for (int k = 0; k < 3; ++k) {
                    Vector3i v = pos.add(random.nextInt(3) - 1, 0, random.nextInt(3) - 1);
                    val b = level.getBlock(v);
                    val state = b.getState();

                    if (b.up().getState().getType() == BlockIds.AIR && state.getBehavior().getBurnChance() > 0) {
                        BlockIgniteEvent e = new BlockIgniteEvent(b, block, null, BlockIgniteEvent.BlockIgniteCause.LAVA);
                        level.getServer().getEventManager().fire(e);

                        if (!e.isCancelled()) {
                            BlockState fire = BlockState.get(BlockIds.FIRE);
                            b.set(fire, true);
                            level.scheduleUpdate(v, fire.getBehavior().tickRate());
                        }
                    }
                }
            }
        }

        return result;
    }

    protected boolean isSurroundingBlockFlammable(Block block) {
        for (Direction face : Direction.values()) {
            if (block.getSide(face).getState().getBehavior().getBurnChance() > 0) {
                return true;
            }
        }

        return false;
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.LAVA_BLOCK_COLOR;
    }

    @Override
    public int tickRate() {
        return 30;
    }

    @Override
    public int getFlowDecayPerBlock(Block block) {
        if (block.getWorld().getDimension() == World.DIMENSION_NETHER) {
            return 1;
        }
        return 2;
    }

    @Override
    protected void checkForHarden(Block block) {
        Block colliding = null;

        for (Direction direction : Direction.values()) {
            if (direction == Direction.UP) {
                continue;
            }

            val side = block.getSide(direction);

            if (side.getState().getBehavior() instanceof BlockBehaviorWater
                    || side.getExtra().getBehavior() instanceof BlockBehaviorWater) {
                colliding = side;
            }
        }

        if (colliding != null) {
            int level = block.getState().ensureTrait(BlockTraits.FLUID_LEVEL);
            if (level == 0) {
                this.liquidCollide(colliding, BlockState.get(BlockIds.OBSIDIAN));
            } else if (level <= 4) {
                this.liquidCollide(colliding, BlockState.get(BlockIds.COBBLESTONE));
            }
        }
    }

    @Override
    protected void flowIntoBlock(Block block, int newFlowDecay) {
        val behavior = block.getState().getBehavior();
        if (behavior instanceof BlockBehaviorWater) {
            ((BlockBehaviorLiquid) behavior).liquidCollide(block, BlockState.get(BlockIds.STONE));
        } else {
            super.flowIntoBlock(block, newFlowDecay);
        }
    }

    @Override
    public Vector3f addVelocityToEntity(Block block, Vector3f vector, Entity entity) {
        if (!(entity instanceof PrimedTnt)) {
            return super.addVelocityToEntity(block, vector, entity);
        }
        return vector;
    }
}
