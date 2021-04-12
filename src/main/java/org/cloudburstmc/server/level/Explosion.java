package org.cloudburstmc.server.level;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.protocol.bedrock.data.SoundEvent;
import it.unimi.dsi.fastutil.longs.LongArraySet;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.misc.DroppedItem;
import org.cloudburstmc.api.entity.misc.ExperienceOrb;
import org.cloudburstmc.api.event.block.BlockUpdateEvent;
import org.cloudburstmc.api.event.entity.EntityDamageByBlockEvent;
import org.cloudburstmc.api.event.entity.EntityDamageByEntityEvent;
import org.cloudburstmc.api.event.entity.EntityDamageEvent;
import org.cloudburstmc.api.event.entity.EntityExplodeEvent;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.AxisAlignedBB;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.SimpleAxisAlignedBB;
import org.cloudburstmc.server.block.behavior.BlockBehaviorTNT;
import org.cloudburstmc.server.level.particle.HugeExplodeSeedParticle;
import org.cloudburstmc.server.math.NukkitMath;
import org.cloudburstmc.server.registry.CloudItemRegistry;
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
    private final CloudLevel level;
    private final Vector3f source;
    private final double size;

    private List<Block> affectedBlockStates = new ArrayList<>();
    private final double stepLen = 0.3d;

    private final Object what;

    public Explosion(CloudLevel level, Vector3f center, double size, Entity what) {
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
                            Block block = this.level.getLoadedBlock(vBlock);

                            if (block != null && block.getState() != BlockStates.AIR) {
                                val state = block.getState();
                                BlockState layer1 = block.getExtra();
                                double resistance = Math.max(state.getBehavior().getResistance(state), layer1.getBehavior().getResistance(layer1));
                                blastForce -= (resistance / 5 + 0.3d) * this.stepLen;
                                if (blastForce > 0) {
                                    if (!this.affectedBlockStates.contains(block)) {
                                        this.affectedBlockStates.add(block);
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
            this.level.getServer().getEventManager().fire(ev);
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
                } else if (this.what instanceof Block) {
                    entity.attack(new EntityDamageByBlockEvent((Block) this.what, entity, EntityDamageEvent.DamageCause.BLOCK_EXPLOSION, damage));
                } else {
                    entity.attack(new EntityDamageEvent(entity, EntityDamageEvent.DamageCause.BLOCK_EXPLOSION, damage));
                }

                if (!(entity instanceof DroppedItem || entity instanceof ExperienceOrb)) {
                    entity.setMotion(motion.mul(impact));
                }
            }
        }

        ItemStack air = CloudItemRegistry.AIR;

        //Iterator iter = this.affectedBlocks.entrySet().iterator();
        for (Block block : this.affectedBlockStates) {
            val state = block.getState();
            val behavior = state.getBehavior();
            //Block block = (Block) ((HashMap.Entry) iter.next()).getValue();
            if (state.getType() == BlockTypes.TNT) {
                ((BlockBehaviorTNT) behavior).prime(block, ThreadLocalRandom.current().nextInt(10, 31), this.what instanceof Entity ? (Entity) this.what : null);
            } else if (Math.random() * 100 < yield) {
                for (ItemStack drop : behavior.getDrops(block, air)) {
                    this.level.dropItem(block.getPosition(), drop);
                }
            }

            behavior.onBreak(block, air);

            for (Direction side : Direction.values()) {
                Block sideBlock = block.getSide(side);
                Vector3i sidePos = sideBlock.getPosition();
                long index = Hash.hashBlock(sidePos.getX(), sidePos.getY(), sidePos.getZ());
                if (!this.affectedBlockStates.contains(sideBlock) && !updateBlocks.contains(index)) {
                    BlockUpdateEvent ev = new BlockUpdateEvent(sideBlock);
                    this.level.getServer().getEventManager().fire(ev);
                    if (!ev.isCancelled()) {
                        val b = ev.getBlock();
                        b.getState().getBehavior().onUpdate(b, CloudLevel.BLOCK_UPDATE_NORMAL);
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
