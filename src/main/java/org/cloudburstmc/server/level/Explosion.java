package org.cloudburstmc.server.level;

import cn.nukkit.math.*;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.protocol.bedrock.data.SoundEvent;
import it.unimi.dsi.fastutil.longs.LongArraySet;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.block.behavior.BlockBehaviorTNT;
import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.entity.misc.DroppedItem;
import org.cloudburstmc.server.entity.misc.ExperienceOrb;
import org.cloudburstmc.server.event.block.BlockUpdateEvent;
import org.cloudburstmc.server.event.entity.EntityDamageByBlockEvent;
import org.cloudburstmc.server.event.entity.EntityDamageByEntityEvent;
import org.cloudburstmc.server.event.entity.EntityDamageEvent;
import org.cloudburstmc.server.event.entity.EntityExplodeEvent;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.level.particle.HugeExplodeSeedParticle;
import org.cloudburstmc.server.math.AxisAlignedBB;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.math.NukkitMath;
import org.cloudburstmc.server.math.SimpleAxisAlignedBB;
import org.cloudburstmc.server.utils.Hash;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * author: Angelic47
 * Nukkit Project
 */
@Log4j2
public class Explosion {

    private final int rays = 16; //Rays
    private final Level level;
    private final Vector3f source;
    private final double size;

    private List<BlockState> affectedBlockStates = new ArrayList<>();
    private final double stepLen = 0.3d;

    private final Object what;

    public Explosion(Level level, Vector3f center, double size, Entity what) {
        this.level = level;
        this.source = center;
        this.size = Math.max(size, 0);
        this.what = what;
    }

    /**
     * @return bool
     * @deprecated
     */
    public boolean explode() {
        if (explodeA()) {
            return explodeB();
        }
        return false;
    }

    /**
     * @return bool
     */
    public boolean explodeA() {
        if (this.size < 0.1) {
            return false;
        }

        Vector3f vBlock = Vector3f.ZERO;

        int mRays = this.rays - 1;
        for (int i = 0; i < this.rays; ++i) {
            for (int j = 0; j < this.rays; ++j) {
                for (int k = 0; k < this.rays; ++k) {
                    if (i == 0 || i == mRays || j == 0 || j == mRays || k == 0 || k == mRays) {
                        Vector3f vector = Vector3f.from((double) i / (double) mRays * 2d - 1, (double) j / (double) mRays * 2d - 1, (double) k / (double) mRays * 2d - 1);
                        double len = vector.length();
                        vector = vector.div(len).mul(this.stepLen);
                        double pointerX = this.source.getX();
                        double pointerY = this.source.getY();
                        double pointerZ = this.source.getZ();

                        for (double blastForce = this.size * (ThreadLocalRandom.current().nextInt(700, 1301)) / 1000d; blastForce > 0; blastForce -= this.stepLen * 0.75d) {
                            int x = (int) pointerX;
                            int y = (int) pointerY;
                            int z = (int) pointerZ;
                            Vector3i blockPos = Vector3i.from(
                                    pointerX >= x ? x : x - 1,
                                    pointerY >= y ? y : y - 1,
                                    pointerZ >= z ? z : z - 1
                            );
                            if (vBlock.getY() < 0 || vBlock.getY() > 255) {
                                break;
                            }
                            BlockState blockState = this.level.getLoadedBlock(vBlock);

                            if (blockState != null && blockState.getId() != BlockTypes.AIR) {
                                BlockState layer1 = blockState.getExtra();
                                double resistance = Math.max(blockState.getResistance(), layer1.getResistance());
                                blastForce -= (resistance / 5 + 0.3d) * this.stepLen;
                                if (blastForce > 0) {
                                    if (!this.affectedBlockStates.contains(blockState)) {
                                        this.affectedBlockStates.add(blockState);
                                        if (layer1.getId() != BlockTypes.AIR) {
                                            this.affectedBlockStates.add(layer1);
                                        }
                                    }
                                }
                            }
                            pointerX += vector.getX();
                            pointerY += vector.getY();
                            pointerZ += vector.getZ();
                        }
                    }
                }
            }
        }

        return true;
    }

    public boolean explodeB() {

        LongArraySet updateBlocks = new LongArraySet();

        Vector3f source = Vector3f.from(this.source).floor();
        double yield = (1d / this.size) * 100d;

        if (this.what instanceof Entity) {
            EntityExplodeEvent ev = new EntityExplodeEvent((Entity) this.what, this.source, this.affectedBlockStates, yield);
            this.level.getServer().getPluginManager().callEvent(ev);
            if (ev.isCancelled()) {
                return false;
            } else {
                yield = ev.getYield();
                this.affectedBlockStates = ev.getBlockList();
            }
        }

        double explosionSize = this.size * 2d;
        float minX = NukkitMath.floorDouble(this.source.getX() - explosionSize - 1);
        float maxX = NukkitMath.ceilDouble(this.source.getX() + explosionSize + 1);
        float minY = NukkitMath.floorDouble(this.source.getY() - explosionSize - 1);
        float maxY = NukkitMath.ceilDouble(this.source.getY() + explosionSize + 1);
        float minZ = NukkitMath.floorDouble(this.source.getZ() - explosionSize - 1);
        float maxZ = NukkitMath.ceilDouble(this.source.getZ() + explosionSize + 1);

        AxisAlignedBB explosionBB = new SimpleAxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ);

        Set<Entity> entities = this.level.getNearbyEntities(explosionBB, this.what instanceof Entity ? (Entity) this.what : null);
        for (Entity entity : entities) {
            double distance = entity.getPosition().distance(this.source) / explosionSize;

            if (distance <= 1) {
                Vector3f motion = entity.getPosition().sub(this.source).normalize();
                int exposure = 1;
                double impact = (1 - distance) * exposure;
                int damage = (int) (((impact * impact + impact) / 2) * 8 * explosionSize + 1);

                if (this.what instanceof Entity) {
                    entity.attack(new EntityDamageByEntityEvent((Entity) this.what, entity, EntityDamageEvent.DamageCause.ENTITY_EXPLOSION, damage));
                } else if (this.what instanceof BlockState) {
                    entity.attack(new EntityDamageByBlockEvent((BlockState) this.what, entity, EntityDamageEvent.DamageCause.BLOCK_EXPLOSION, damage));
                } else {
                    entity.attack(new EntityDamageEvent(entity, EntityDamageEvent.DamageCause.BLOCK_EXPLOSION, damage));
                }

                if (!(entity instanceof DroppedItem || entity instanceof ExperienceOrb)) {
                    entity.setMotion(motion.mul(impact));
                }
            }
        }

        Item air = Item.get(BlockTypes.AIR, 0, 0);

        //Iterator iter = this.affectedBlocks.entrySet().iterator();
        for (BlockState blockState : this.affectedBlockStates) {
            //Block block = (Block) ((HashMap.Entry) iter.next()).getValue();
            if (blockState.getId() == BlockTypes.TNT) {
                ((BlockBehaviorTNT) blockState).prime(ThreadLocalRandom.current().nextInt(10, 31), this.what instanceof Entity ? (Entity) this.what : null);
            } else if (Math.random() * 100 < yield) {
                for (Item drop : blockState.getDrops(air)) {
                    this.level.dropItem(blockState.getPosition(), drop);
                }
            }

            this.level.setBlockId(blockState.getPosition(), blockState.getLayer(), BlockTypes.AIR);

            if (blockState.getLayer() != 0) {
                continue;
            }

            for (Direction side : Direction.values()) {
                BlockState sideBlockState = blockState.getSide(side);
                Vector3i sidePos = sideBlockState.getPosition();
                long index = Hash.hashBlock(sidePos.getX(), sidePos.getY(), sidePos.getZ());
                if (!this.affectedBlockStates.contains(sideBlockState) && !updateBlocks.contains(index)) {
                    BlockUpdateEvent ev = new BlockUpdateEvent(sideBlockState);
                    this.level.getServer().getPluginManager().callEvent(ev);
                    if (!ev.isCancelled()) {
                        ev.getBlock().onUpdate(Level.BLOCK_UPDATE_NORMAL);
                    }
                    BlockState extra = sideBlockState.getExtra();
                    if (extra.getId() != BlockTypes.AIR) {
                        ev = new BlockUpdateEvent(extra);
                        this.level.getServer().getPluginManager().callEvent(ev);
                        if (!ev.isCancelled()) {
                            ev.getBlock().onUpdate(Level.BLOCK_UPDATE_NORMAL);
                        }
                    }
                    updateBlocks.add(index);
                }
            }
        }

        this.level.addParticle(new HugeExplodeSeedParticle(this.source));
        this.level.addLevelSoundEvent(source, SoundEvent.EXPLODE);

        return true;
    }

}
