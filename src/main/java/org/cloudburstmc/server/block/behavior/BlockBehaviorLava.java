package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.math.vector.Vector3i;
import lombok.val;
import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.misc.PrimedTnt;
import org.cloudburstmc.api.event.block.BlockIgniteEvent;
import org.cloudburstmc.api.event.entity.EntityCombustByBlockEvent;
import org.cloudburstmc.api.event.entity.EntityDamageByBlockEvent;
import org.cloudburstmc.api.event.entity.EntityDamageEvent;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.gamerule.GameRules;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.potion.EffectTypes;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.data.BlockColor;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.registry.CloudBlockRegistry;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import static org.cloudburstmc.api.block.BlockTypes.FLOWING_LAVA;
import static org.cloudburstmc.api.block.BlockTypes.LAVA;

public class BlockBehaviorLava extends BlockBehaviorLiquid {

    public BlockBehaviorLava() {
        super(FLOWING_LAVA, LAVA);
    }


    @Override
    public void onEntityCollide(Block block, Entity entity) {
        float highestPos = entity.getHighestPosition();
        entity.setHighestPosition(highestPos - (highestPos - entity.getY()) * 0.5f);

        // Always setting the duration to 15 seconds? TODO
        EntityCombustByBlockEvent ev = new EntityCombustByBlockEvent(block, entity, 15);
        CloudServer.getInstance().getEventManager().fire(ev);
        if (!ev.isCancelled()
                // Making sure the entity is actually alive and not invulnerable.
                && entity.isAlive()
                && entity.getNoDamageTicks() == 0) {
            entity.setOnFire(ev.getDuration());
        }

        if (!entity.hasEffect(EffectTypes.FIRE_RESISTANCE)) {
            entity.attack(new EntityDamageByBlockEvent(block, entity, EntityDamageEvent.DamageCause.LAVA, 4));
        }

        super.onEntityCollide(block, entity);
    }

    @Override
    public boolean place(ItemStack item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        boolean ret = placeBlock(block, CloudBlockRegistry.get().getBlock(BlockTypes.FLOWING_LAVA));

        block.getLevel().scheduleUpdate(block.getPosition(), this.tickRate());
        return ret;
    }

    @Override
    public int onUpdate(Block block, int type) {
        int result = super.onUpdate(block, type);

        if (type == CloudLevel.BLOCK_UPDATE_RANDOM && block.getLevel().getGameRules().get(GameRules.DO_FIRE_TICK)) {
            val pos = block.getPosition();
            val level = block.getLevel();

            Random random = ThreadLocalRandom.current();

            int i = random.nextInt(3);

            if (i > 0) {
                for (int k = 0; k < i; ++k) {
                    Vector3i v = pos.add(random.nextInt(3) - 1, 1, random.nextInt(3) - 1);
                    val b = level.getBlock(v);
                    val state = b.getState();

                    if (state.getType() == BlockTypes.AIR) {
                        if (this.isSurroundingBlockFlammable(b)) {
                            BlockIgniteEvent e = new BlockIgniteEvent(b, block, null, BlockIgniteEvent.BlockIgniteCause.LAVA);
                            level.getServer().getEventManager().fire(e);

                            if (!e.isCancelled()) {
                                BlockState fire = CloudBlockRegistry.get().getBlock(BlockTypes.FIRE);
                                b.set(fire, true);
                                level.scheduleUpdate(v, fire.getBehavior().tickRate());
                                return CloudLevel.BLOCK_UPDATE_RANDOM;
                            }

                            return 0;
                        }
                    } else if (state.inCategory(BlockCategory.SOLID)) {
                        return CloudLevel.BLOCK_UPDATE_RANDOM;
                    }
                }
            } else {
                for (int k = 0; k < 3; ++k) {
                    Vector3i v = pos.add(random.nextInt(3) - 1, 0, random.nextInt(3) - 1);
                    val b = level.getBlock(v);
                    val state = b.getState();

                    if (b.up().getState().getType() == BlockTypes.AIR && state.getBehavior().getBurnChance(state) > 0) {
                        BlockIgniteEvent e = new BlockIgniteEvent(b, block, null, BlockIgniteEvent.BlockIgniteCause.LAVA);
                        level.getServer().getEventManager().fire(e);

                        if (!e.isCancelled()) {
                            BlockState fire = CloudBlockRegistry.get().getBlock(BlockTypes.FIRE);
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
            val sideState = block.getSide(face).getState();
            if (sideState.getBehavior().getBurnChance(sideState) > 0) {
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
        if (((CloudLevel) block.getLevel()).getDimension() == CloudLevel.DIMENSION_NETHER) {
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

            if (side.getState().getType() == BlockTypes.WATER
                    || side.getExtra().getType() == BlockTypes.WATER) {
                colliding = side;
            }
        }

        if (colliding != null) {
            int level = block.getState().ensureTrait(BlockTraits.FLUID_LEVEL);
            if (level == 0) {
                this.liquidCollide(colliding, CloudBlockRegistry.get().getBlock(BlockTypes.OBSIDIAN));
            } else if (level <= 4) {
                this.liquidCollide(colliding, CloudBlockRegistry.get().getBlock(BlockTypes.COBBLESTONE));
            }
        }
    }

    @Override
    protected void flowIntoBlock(Block block, int newFlowDecay, boolean falling) {
        val behavior = block.getState().getBehavior();
        if (behavior instanceof BlockBehaviorWater) {
            ((BlockBehaviorLiquid) behavior).liquidCollide(block, CloudBlockRegistry.get().getBlock(BlockTypes.STONE));
        } else {
            super.flowIntoBlock(block, newFlowDecay, falling);
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
