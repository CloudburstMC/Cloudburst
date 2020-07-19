package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.Server;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.entity.impl.projectile.EntityArrow;
import org.cloudburstmc.server.event.block.BlockBurnEvent;
import org.cloudburstmc.server.event.block.BlockFadeEvent;
import org.cloudburstmc.server.event.block.BlockIgniteEvent;
import org.cloudburstmc.server.event.entity.EntityCombustByBlockEvent;
import org.cloudburstmc.server.event.entity.EntityDamageByBlockEvent;
import org.cloudburstmc.server.event.entity.EntityDamageEvent;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.level.gamerule.GameRules;
import org.cloudburstmc.server.math.AxisAlignedBB;
import org.cloudburstmc.server.math.BlockFace;
import org.cloudburstmc.server.potion.Effect;
import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.Identifier;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class BlockBehaviorFire extends FloodableBlockBehavior {

    public BlockBehaviorFire(Identifier id) {
        super(id);
    }

    @Override
    public boolean hasEntityCollision() {
        return true;
    }

    @Override
    public int getLightLevel() {
        return 15;
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
    public void onEntityCollide(Entity entity) {
        if (!entity.hasEffect(Effect.FIRE_RESISTANCE)) {
            entity.attack(new EntityDamageByBlockEvent(this, entity, EntityDamageEvent.DamageCause.FIRE, 1));
        }

        EntityCombustByBlockEvent ev = new EntityCombustByBlockEvent(this, entity, 8);
        if (entity instanceof EntityArrow) {
            ev.setCancelled();
        }
        Server.getInstance().getPluginManager().callEvent(ev);
        if (!ev.isCancelled() && entity.isAlive() && entity.getNoDamageTicks() == 0) {
            entity.setOnFire(ev.getDuration());
        }
    }

    @Override
    public Item[] getDrops(Item hand) {
        return new Item[0];
    }

    @Override
    public int onUpdate(int type) {
        if (type == Level.BLOCK_UPDATE_NORMAL || type == Level.BLOCK_UPDATE_RANDOM) {
            if (!this.isBlockTopFacingSurfaceSolid(this.down()) && !this.canNeighborBurn()) {
                BlockFadeEvent event = new BlockFadeEvent(this, get(BlockTypes.AIR));
                level.getServer().getPluginManager().callEvent(event);
                if (!event.isCancelled()) {
                    level.setBlock(this.getPosition(), event.getNewState(), true);
                }
            }

            return Level.BLOCK_UPDATE_NORMAL;
        } else if (type == Level.BLOCK_UPDATE_SCHEDULED && this.level.getGameRules().get(GameRules.DO_FIRE_TICK)) {
            boolean forever = this.down().getId() == BlockTypes.NETHERRACK || this.down().getId() == BlockTypes.MAGMA;

            ThreadLocalRandom random = ThreadLocalRandom.current();

            //TODO: END

            if (!this.isBlockTopFacingSurfaceSolid(this.down()) && !this.canNeighborBurn()) {
                BlockFadeEvent event = new BlockFadeEvent(this, get(BlockTypes.AIR));
                level.getServer().getPluginManager().callEvent(event);
                if (!event.isCancelled()) {
                    level.setBlock(this.getPosition(), event.getNewState(), true);
                }
            }

            if (!forever && this.getLevel().isRaining() &&
                    (this.getLevel().canBlockSeeSky(this.getPosition()) ||
                            this.getLevel().canBlockSeeSky(this.getPosition().east()) ||
                            this.getLevel().canBlockSeeSky(this.getPosition().west()) ||
                            this.getLevel().canBlockSeeSky(this.getPosition().south()) ||
                            this.getLevel().canBlockSeeSky(this.getPosition().north()))
            ) {
                BlockFadeEvent event = new BlockFadeEvent(this, get(BlockTypes.AIR));
                level.getServer().getPluginManager().callEvent(event);
                if (!event.isCancelled()) {
                    level.setBlock(this.getPosition(), event.getNewState(), true);
                }
            } else {
                int meta = this.getMeta();

                if (meta < 15) {
                    int newMeta = meta + random.nextInt(3);
                    this.setMeta(Math.min(newMeta, 15));
                    this.getLevel().setBlock(this.getPosition(), this, true);
                }

                this.getLevel().scheduleUpdate(this, this.tickRate() + random.nextInt(10));

                if (!forever && !this.canNeighborBurn()) {
                    if (!this.isBlockTopFacingSurfaceSolid(this.down()) || meta > 3) {
                        BlockFadeEvent event = new BlockFadeEvent(this, get(BlockTypes.AIR));
                        level.getServer().getPluginManager().callEvent(event);
                        if (!event.isCancelled()) {
                            level.setBlock(this.getPosition(), event.getNewState(), true);
                        }
                    }
                } else if (!forever && !(this.down().getBurnAbility() > 0) && meta == 15 && random.nextInt(4) == 0) {
                    BlockFadeEvent event = new BlockFadeEvent(this, get(BlockTypes.AIR));
                    level.getServer().getPluginManager().callEvent(event);
                    if (!event.isCancelled()) {
                        level.setBlock(this.getPosition(), event.getNewState(), true);
                    }
                } else {
                    int o = 0;

                    //TODO: decrease the o if the rainfall values are high

                    this.tryToCatchBlockOnFire(this.east(), 300 + o, meta);
                    this.tryToCatchBlockOnFire(this.west(), 300 + o, meta);
                    this.tryToCatchBlockOnFire(this.down(), 250 + o, meta);
                    this.tryToCatchBlockOnFire(this.up(), 250 + o, meta);
                    this.tryToCatchBlockOnFire(this.south(), 300 + o, meta);
                    this.tryToCatchBlockOnFire(this.north(), 300 + o, meta);

                    for (int x = (int) (this.getX() - 1); x <= (int) (this.getX() + 1); ++x) {
                        for (int z = (int) (this.getZ() - 1); z <= (int) (this.getZ() + 1); ++z) {
                            for (int y = (int) (this.getY() - 1); y <= (int) (this.getY() + 4); ++y) {
                                if (x != (int) this.getX() || y != (int) this.getY() || z != (int) this.getZ()) {
                                    int k = 100;

                                    if (y > this.getY() + 1) {
                                        k += (y - (this.getY() + 1)) * 100;
                                    }

                                    BlockState blockState = this.getLevel().getBlock(x, y, z);
                                    int chance = this.getChanceOfNeighborsEncouragingFire(blockState);

                                    if (chance > 0) {
                                        int t = (chance + 40 + this.getLevel().getServer().getDifficulty().ordinal() * 7) / (meta + 30);

                                        //TODO: decrease the t if the rainfall values are high

                                        if (t > 0 && random.nextInt(k) <= t) {
                                            int damage = meta + random.nextInt(5) / 4;

                                            if (damage > 15) {
                                                damage = 15;
                                            }

                                            BlockIgniteEvent e = new BlockIgniteEvent(blockState, this, null, BlockIgniteEvent.BlockIgniteCause.SPREAD);
                                            this.level.getServer().getPluginManager().callEvent(e);

                                            if (!e.isCancelled()) {
                                                this.getLevel().setBlock(blockState.getPosition(), BlockState.get(BlockTypes.FIRE, damage), true);
                                                this.getLevel().scheduleUpdate(blockState, this.tickRate());
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

    private void tryToCatchBlockOnFire(BlockState blockState, int bound, int damage) {
        int burnAbility = blockState.getBurnAbility();

        Random random = ThreadLocalRandom.current();

        if (random.nextInt(bound) < burnAbility) {

            if (random.nextInt(damage + 10) < 5) {
                int meta = damage + random.nextInt(5) / 4;

                if (meta > 15) {
                    meta = 15;
                }

                BlockIgniteEvent e = new BlockIgniteEvent(blockState, this, null, BlockIgniteEvent.BlockIgniteCause.SPREAD);
                this.level.getServer().getPluginManager().callEvent(e);

                if (!e.isCancelled()) {
                    this.getLevel().setBlock(blockState.getPosition(), BlockState.get(BlockTypes.FIRE, meta), true);
                    this.getLevel().scheduleUpdate(blockState, this.tickRate());
                }
            } else {
                BlockBurnEvent ev = new BlockBurnEvent(blockState);
                this.getLevel().getServer().getPluginManager().callEvent(ev);

                if (!ev.isCancelled()) {
                    this.getLevel().setBlock(blockState.getPosition(), BlockState.AIR, true);
                }
            }

            if (blockState instanceof BlockBehaviorTNT) {
                ((BlockBehaviorTNT) blockState).prime();
            }
        }
    }

    private int getChanceOfNeighborsEncouragingFire(BlockState blockState) {
        if (blockState.getId() != BlockTypes.AIR) {
            return 0;
        } else {
            int chance = 0;
            chance = Math.max(chance, blockState.east().getBurnChance());
            chance = Math.max(chance, blockState.west().getBurnChance());
            chance = Math.max(chance, blockState.down().getBurnChance());
            chance = Math.max(chance, blockState.up().getBurnChance());
            chance = Math.max(chance, blockState.south().getBurnChance());
            chance = Math.max(chance, blockState.north().getBurnChance());
            return chance;
        }
    }

    public boolean canNeighborBurn() {
        for (BlockFace face : BlockFace.values()) {
            if (this.getSide(face).getBurnChance() > 0) {
                return true;
            }
        }

        return false;
    }

    public boolean isBlockTopFacingSurfaceSolid(BlockState blockState) {
        if (blockState != null) {
            if (blockState.isSolid()) {
                return true;
            } else {
                if (blockState instanceof BlockBehaviorStairs &&
                        (blockState.getMeta() & 4) == 4) {

                    return true;
                } else if (blockState instanceof BlockBehaviorSlab &&
                        (blockState.getMeta() & 8) == 8) {

                    return true;
                } else if (blockState instanceof BlockBehaviorSnowLayer &&
                        (blockState.getMeta() & 7) == 7) {

                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public int tickRate() {
        return 30;
    }

    @Override
    public BlockColor getColor() {
        return BlockColor.LAVA_BLOCK_COLOR;
    }

    @Override
    protected AxisAlignedBB recalculateCollisionBoundingBox() {
        return this;
    }

    @Override
    public Item toItem() {
        return Item.get(BlockTypes.AIR);
    }
}
