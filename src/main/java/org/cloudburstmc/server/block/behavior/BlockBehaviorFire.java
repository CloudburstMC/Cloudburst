package org.cloudburstmc.server.block.behavior;

import lombok.val;
import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.projectile.Arrow;
import org.cloudburstmc.api.event.block.BlockBurnEvent;
import org.cloudburstmc.api.event.block.BlockFadeEvent;
import org.cloudburstmc.api.event.block.BlockIgniteEvent;
import org.cloudburstmc.api.event.entity.EntityCombustByBlockEvent;
import org.cloudburstmc.api.event.entity.EntityDamageByBlockEvent;
import org.cloudburstmc.api.event.entity.EntityDamageEvent;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.gamerule.GameRules;
import org.cloudburstmc.api.potion.EffectTypes;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.Direction.Plane;
import org.cloudburstmc.api.util.data.BlockColor;
import org.cloudburstmc.api.util.data.SlabSlot;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.registry.CloudBlockRegistry;
import org.cloudburstmc.server.registry.CloudItemRegistry;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class BlockBehaviorFire extends FloodableBlockBehavior {

    @Override
    public boolean hasEntityCollision() {
        return true;
    }

    @Override
    public void onEntityCollide(Block block, Entity entity) {
        if (!entity.hasEffect(EffectTypes.FIRE_RESISTANCE)) {
            entity.attack(new EntityDamageByBlockEvent(block, entity, EntityDamageEvent.DamageCause.FIRE, 1));
        }

        EntityCombustByBlockEvent ev = new EntityCombustByBlockEvent(block, entity, 8);
        if (entity instanceof Arrow) {
            ev.setCancelled();
        }
        CloudServer.getInstance().getEventManager().fire(ev);
        if (!ev.isCancelled() && entity.isAlive() && entity.getNoDamageTicks() == 0) {
            entity.setOnFire(ev.getDuration());
        }
    }

    @Override
    public ItemStack[] getDrops(Block block, ItemStack hand) {
        return new ItemStack[0];
    }

    @Override
    public int onUpdate(Block block, int type) {
        val level = block.getLevel();
        val position = block.getPosition();

        if (type == Level.BLOCK_UPDATE_NORMAL || type == Level.BLOCK_UPDATE_RANDOM) {
            if (!BlockBehaviorFire.isBlockTopFacingSurfaceSolid(block.down().getState()) && !BlockBehaviorFire.canNeighborBurn(block)) {
                BlockFadeEvent event = new BlockFadeEvent(block, BlockStates.AIR);
                level.getServer().getEventManager().fire(event);
                if (!event.isCancelled()) {
                    level.setBlockState(position, event.getNewState(), true);
                }
            }

            return Level.BLOCK_UPDATE_NORMAL;
        } else if (type == Level.BLOCK_UPDATE_SCHEDULED && level.getGameRules().get(GameRules.DO_FIRE_TICK)) {
            val down = block.down().getState();
            boolean forever = down.getType() == BlockTypes.NETHERRACK || down.getType() == BlockTypes.MAGMA;

            ThreadLocalRandom random = ThreadLocalRandom.current();

            //TODO: END

            if (!BlockBehaviorFire.isBlockTopFacingSurfaceSolid(down) && !BlockBehaviorFire.canNeighborBurn(block)) {
                BlockFadeEvent event = new BlockFadeEvent(block, CloudBlockRegistry.get().getBlock(BlockTypes.AIR));
                level.getServer().getEventManager().fire(event);
                if (!event.isCancelled()) {
                    level.setBlockState(position, event.getNewState(), true);
                }
            }

            if (!forever && level.isRaining() &&
                    (level.canBlockSeeSky(position) ||
                            level.canBlockSeeSky(position.east()) ||
                            level.canBlockSeeSky(position.west()) ||
                            level.canBlockSeeSky(position.south()) ||
                            level.canBlockSeeSky(position.north()))
            ) {
                BlockFadeEvent event = new BlockFadeEvent(block, CloudBlockRegistry.get().getBlock(BlockTypes.AIR));
                level.getServer().getEventManager().fire(event);
                if (!event.isCancelled()) {
                    block.set(event.getNewState(), true);
                }
            } else {
                val state = block.getState();
                int age = state.ensureTrait(BlockTraits.AGE);

                if (age < 15) {
                    int newAge = age + random.nextInt(3);
                    block.set(state.withTrait(BlockTraits.AGE, Math.min(newAge, 15)));
                }

                level.scheduleUpdate(block.getPosition(), this.tickRate() + random.nextInt(10));

                if (!forever && !BlockBehaviorFire.canNeighborBurn(block)) {
                    if (!BlockBehaviorFire.isBlockTopFacingSurfaceSolid(down) || age > 3) {
                        BlockFadeEvent event = new BlockFadeEvent(block, CloudBlockRegistry.get().getBlock(BlockTypes.AIR));
                        level.getServer().getEventManager().fire(event);
                        if (!event.isCancelled()) {
                            level.setBlockState(position, event.getNewState(), true);
                        }
                    }
                } else if (!forever && !(down.getBehavior().getBurnAbility(down) > 0) && age == 15 && random.nextInt(4) == 0) {
                    BlockFadeEvent event = new BlockFadeEvent(block, CloudBlockRegistry.get().getBlock(BlockTypes.AIR));
                    level.getServer().getEventManager().fire(event);
                    if (!event.isCancelled()) {
                        level.setBlockState(position, event.getNewState(), true);
                    }
                } else {
                    int o = 0;

                    //TODO: decrease the o if the rainfall values are high

                    for (Direction direction : Plane.HORIZONTAL) {
                        this.tryToCatchBlockOnFire(block, block.getSide(direction), 300 + o, age);
                    }

                    for (Direction direction : Plane.VERTICAL) {
                        this.tryToCatchBlockOnFire(block, block.getSide(direction), 250 + o, age);
                    }

                    for (int x = block.getX() - 1; x <= (block.getX() + 1); ++x) {
                        for (int z = block.getZ() - 1; z <= (block.getZ() + 1); ++z) {
                            for (int y = block.getY() - 1; y <= (block.getY() + 4); ++y) {
                                if (x != block.getX() || y != block.getY() || z != block.getZ()) {
                                    int k = 100;

                                    if (y > block.getY() + 1) {
                                        k += (y - (block.getY() + 1)) * 100;
                                    }

                                    val blockState = level.getBlock(x, y, z);
                                    int chance = this.getChanceOfNeighborsEncouragingFire(blockState);

                                    if (chance > 0) {
                                        int t = (chance + 40 + level.getServer().getDifficulty().ordinal() * 7) / (age + 30);

                                        //TODO: decrease the t if the rainfall values are high

                                        if (t > 0 && random.nextInt(k) <= t) {
                                            int age_ = age + random.nextInt(5) / 4;

                                            if (age_ > 15) {
                                                age_ = 15;
                                            }

                                            BlockIgniteEvent e = new BlockIgniteEvent(blockState, block, null, BlockIgniteEvent.BlockIgniteCause.SPREAD);
                                            level.getServer().getEventManager().fire(e);

                                            if (!e.isCancelled()) {
                                                blockState.set(CloudBlockRegistry.get().getBlock(BlockTypes.FIRE).withTrait(BlockTraits.AGE, age_));
                                                level.scheduleUpdate(blockState.getPosition(), this.tickRate());
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return 0;
    }

    private void tryToCatchBlockOnFire(Block fire, Block block, int bound, int damage) {
        val state = block.getState();
        val behavior = state.getBehavior();

        int burnAbility = behavior.getBurnAbility(state);

        Random random = ThreadLocalRandom.current();

        if (random.nextInt(bound) < burnAbility) {

            if (random.nextInt(damage + 10) < 5) {
                int meta = damage + random.nextInt(5) / 4;

                if (meta > 15) {
                    meta = 15;
                }

                BlockIgniteEvent e = new BlockIgniteEvent(block, fire, null, BlockIgniteEvent.BlockIgniteCause.SPREAD);
                block.getLevel().getServer().getEventManager().fire(e);

                if (!e.isCancelled()) {
                    block.set(CloudBlockRegistry.get().getBlock(BlockTypes.FIRE).withTrait(BlockTraits.AGE, meta), true);
                    block.getLevel().scheduleUpdate(block.getPosition(), this.tickRate());
                }
            } else {
                BlockBurnEvent ev = new BlockBurnEvent(block);
                block.getLevel().getServer().getEventManager().fire(ev);

                if (!ev.isCancelled()) {
                    removeBlock(block, true);
                }
            }

            if (state.getType() == BlockTypes.TNT && behavior instanceof BlockBehaviorTNT) {
                ((BlockBehaviorTNT) behavior).prime(block);
            }
        }
    }

    private int getChanceOfNeighborsEncouragingFire(Block block) {
        val state = block.getState();
        if (state.getType() != BlockTypes.AIR) {
            return 0;
        } else {
            int chance = 0;

            for (Direction direction : Direction.values()) {
                val sideState = block.getSide(direction).getState();
                chance = Math.max(chance, sideState.getBehavior().getBurnChance(sideState));
            }

            return chance;
        }
    }

    public static boolean canNeighborBurn(Block block) {
        for (Direction face : Direction.values()) {
            val sideState = block.getSide(face).getState();
            if (sideState.getBehavior().getBurnChance(sideState) > 0) {
                return true;
            }
        }

        return false;
    }

    public static boolean isBlockTopFacingSurfaceSolid(BlockState state) {
        if (state.inCategory(BlockCategory.SOLID)) {
            return true;
        } else {
            if (state.inCategory(BlockCategory.STAIRS)) {
                return state.ensureTrait(BlockTraits.IS_UPSIDE_DOWN);
            } else if (state.inCategory(BlockCategory.SLAB)) {
                return state.ensureTrait(BlockTraits.SLAB_SLOT) != SlabSlot.BOTTOM;
            } else if (state.getType() == BlockTypes.SNOW_LAYER) {
                return state.ensureTrait(BlockTraits.HEIGHT) == 7;
            }
        }

        return false;
    }

    @Override
    public int tickRate() {
        return 30;
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.LAVA_BLOCK_COLOR;
    }

    @Override
    public ItemStack toItem(Block block) {
        return CloudItemRegistry.AIR;
    }
}
