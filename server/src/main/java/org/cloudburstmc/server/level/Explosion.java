package org.cloudburstmc.server.level;

import it.unimi.dsi.fastutil.longs.LongArraySet;
import lombok.extern.log4j.Log4j2;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.block.LiquidType;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.Explosive;
import org.cloudburstmc.api.entity.damage.DamageSource;
import org.cloudburstmc.api.entity.damage.DamageType;
import org.cloudburstmc.api.entity.damage.DamageTypes;
import org.cloudburstmc.api.entity.misc.DroppedItem;
import org.cloudburstmc.api.entity.misc.ExperienceOrb;
import org.cloudburstmc.api.event.entity.EntityExplodeEvent;
import org.cloudburstmc.api.level.BlockShapeMode;
import org.cloudburstmc.api.level.FluidCollisionMode;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.level.RayTraceContext;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.BoundingBox;
import org.cloudburstmc.api.util.CollisionContext;
import org.cloudburstmc.api.util.MissHitResult;
import org.cloudburstmc.api.util.MissReason;
import org.cloudburstmc.math.GenericMath;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.server.level.particle.HugeExplodeSeedParticle;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import static org.cloudburstmc.api.block.LiquidTypes.WATER;

@Log4j2
public class Explosion {

    private final int rays = 16; //Rays
    private final CloudLevel level;
    private final Vector3f source;
    private final double size;
    private final double stepLen = 0.3d;
    private final @Nullable Entity sourceEntity;

    private boolean doesDamage = true;
    private boolean destroysBlocks = true;
    private List<Block> affectedBlockStates = new ArrayList<>();

    public Explosion(CloudLevel level, Vector3f center, double size, @Nullable Entity sourceEntity) {
        this.level = level;
        this.source = center;
        this.size = Math.max(size, 0);
        this.sourceEntity = sourceEntity;
    }

    public static float getSeenPercent(Vector3f center, Entity entity) {
        BoundingBox box = entity.getBoundingBox();
        double xStep = 1 / ((box.getMaxX() - box.getMinX()) * 2 + 1);
        double yStep = 1 / ((box.getMaxY() - box.getMinY()) * 2 + 1);
        double zStep = 1 / ((box.getMaxZ() - box.getMinZ()) * 2 + 1);

        double xOffset = (1 - Math.floor(1 / xStep) * xStep) / 2;
        double zOffset = (1 - Math.floor(1 / zStep) * zStep) / 2;

        int clear = 0;
        int samples = 0;
        for (double x = 0; x <= 1; x += xStep) {
            for (double y = 0; y <= 1; y += yStep) {
                for (double z = 0; z <= 1; z += zStep) {
                    Vector3f sample = Vector3f.from(
                            box.getMinX() + (box.getMaxX() - box.getMinX()) * x + xOffset,
                            box.getMinY() + (box.getMaxY() - box.getMinY()) * y,
                            box.getMinZ() + (box.getMaxZ() - box.getMinZ()) * z + zOffset
                    );

                    if (entity.getLevel().rayTraceBlocks(new RayTraceContext(sample, center,
                            BlockShapeMode.COLLIDER, FluidCollisionMode.NONE,
                            CollisionContext.of(entity))) instanceof MissHitResult miss
                            && miss.reason() == MissReason.CLEAR) {
                        clear++;
                    }
                    samples++;
                }
            }
        }

        return samples == 0 ? 0 : (float) clear / samples;
    }

    public void setDestroysBlocks(boolean destroysBlocks) {
        this.destroysBlocks = destroysBlocks;
    }

    /**
     * @return bool
     */
    public boolean explodeA() {
        if (this.sourceEntity instanceof Explosive) {
            Vector3f pos = this.sourceEntity.getPosition();
            LiquidType liquid = this.level.getBlock(pos).getLiquid().getType();
            if (liquid.isSameFamily(WATER)) {
                this.doesDamage = false;
                return true;
            }
        }

        if (this.size < 0.1) {
            return false;
        }

        if (!this.destroysBlocks) {
            return true;
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
                            if (vBlock.getY() < -64 || vBlock.getY() > 319) {
                                break;
                            }
                            Block block = this.level.getLoadedBlock(vBlock);

                            if (block != null && block.getState() != BlockStates.AIR) {
                                var state = block.getState();
                                BlockState layer1 = block.getSecondaryState();

                                double resistance = Math.max(state.getExplosionResistance(), layer1.getExplosionResistance());
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

        Vector3f explosionPosition = this.source.floor();
        double yield = (1d / this.size) * 100d;

        if (this.sourceEntity != null) {
            EntityExplodeEvent ev = new EntityExplodeEvent(this.sourceEntity, this.source, this.affectedBlockStates, yield);
            this.level.getServer().getEventManager().fire(ev);
            if (ev.isCancelled()) {
                return false;
            } else {
                yield = ev.getYield();
                this.affectedBlockStates = ev.getBlockList();
            }
        }

        double explosionSize = this.size * 2d;
        float minX = GenericMath.floor(this.source.getX() - explosionSize - 1);
        float maxX = GenericMath.ceil(this.source.getX() + explosionSize + 1);
        float minY = GenericMath.floor(this.source.getY() - explosionSize - 1);
        float maxY = GenericMath.ceil(this.source.getY() + explosionSize + 1);
        float minZ = GenericMath.floor(this.source.getZ() - explosionSize - 1);
        float maxZ = GenericMath.ceil(this.source.getZ() + explosionSize + 1);

        BoundingBox explosionBB = new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);

        DamageSource damageSource = this.createDamageSource();
        Set<Entity> entities = this.level.getNearbyEntities(this.sourceEntity, explosionBB);
        for (Entity entity : entities) {
            double distance = entity.getPosition().distance(this.source) / explosionSize;

            if (distance <= 1) {
                Vector3f motion = entity.getPosition().sub(this.source).normalize();
                float exposure = getSeenPercent(this.source, entity);
                double impact = (1 - distance) * exposure;
                int damage = this.doesDamage ? (int) (((impact * impact + impact) / 2) * 8 * explosionSize + 1) : 0;

                entity.damage(damage, damageSource);

                if (!(entity instanceof DroppedItem || entity instanceof ExperienceOrb)) {
                    entity.setMotion(motion.mul(impact));
                }
            }
        }

        this.level.addParticle(new HugeExplodeSeedParticle(this.source));
        this.level.addLevelSoundEvent(explosionPosition, SoundEvent.EXPLODE);

        return true;
    }

    private DamageSource createDamageSource() {
        DamageType damageType = this.sourceEntity instanceof Player
                ? DamageTypes.PLAYER_EXPLOSION
                : DamageTypes.EXPLOSION;
        DamageSource.Builder source = DamageSource.builder(damageType)
                .damageLocation(Location.from(this.source, this.level));
        if (this.sourceEntity != null) {
            source.directEntity(this.sourceEntity).causingEntity(this.sourceEntity);
        }

        return source.build();
    }
}
