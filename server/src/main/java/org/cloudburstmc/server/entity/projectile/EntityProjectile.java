package org.cloudburstmc.server.entity.projectile;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.BlockComponents;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityComponents;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.damage.DamageSource;
import org.cloudburstmc.api.entity.damage.DamageType;
import org.cloudburstmc.api.entity.damage.DamageTypes;
import org.cloudburstmc.api.entity.misc.EnderCrystal;
import org.cloudburstmc.api.entity.misc.Painting;
import org.cloudburstmc.api.entity.projectile.Projectile;
import org.cloudburstmc.api.entity.projectile.ProjectileSource;
import org.cloudburstmc.api.entity.vehicle.Vehicle;
import org.cloudburstmc.api.event.entity.EntityCombustByEntityEvent;
import org.cloudburstmc.api.event.entity.EntityRemoveCause;
import org.cloudburstmc.api.event.entity.ProjectileHitEvent;
import org.cloudburstmc.api.level.BlockShapeMode;
import org.cloudburstmc.api.level.FluidCollisionMode;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.level.RayTraceContext;
import org.cloudburstmc.api.util.*;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.entity.CloudLivingEntity;
import org.cloudburstmc.server.entity.data.CloudProjectileData;
import org.cloudburstmc.server.event.entity.CloudEntityDamageEvent;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudEntityRegistry;

public abstract class EntityProjectile extends CloudEntity implements Projectile {

    protected boolean hadCollision;
    private boolean blockImpact;
    private boolean leftShooter;
    private @Nullable ProjectileSource nonEntityShooter;

    private final CloudProjectileData persistenceState;

    public EntityProjectile(EntityType<?> type, Location location) {
        this(type, location, new CloudProjectileData());
    }

    protected EntityProjectile(EntityType<?> type, Location location, CloudProjectileData state) {
        super(type, location, state);
        this.persistenceState = state;
    }

    @Override
    protected void onMotionChanged() {
        super.onMotionChanged();
        Vector3f motion = this.getMotion();
        if (motion.lengthSquared() > 0) {
            this.orientToMotion(motion);
        }
    }

    @Override
    public @Nullable ProjectileSource getShooter() {
        Entity owner = this.getOwner();
        return owner instanceof ProjectileSource source ? source : this.nonEntityShooter;
    }

    @Override
    public void setShooter(@Nullable ProjectileSource shooter) {
        if (shooter instanceof Entity entity) {
            this.setOwner(entity);
        } else {
            this.setOwner(null);
            this.nonEntityShooter = shooter;
        }
    }

    @Override
    public void setOwner(@Nullable Entity owner) {
        super.setOwner(owner);
        this.nonEntityShooter = null;
        this.leftShooter = false;
    }

    @Override
    protected CloudProjectileData data() {
        return this.persistenceState;
    }

    @Override
    protected void loadCustom(NbtMap tag) {
        if (tag.containsKey("damage")) {
            this.setDamage(this.data().getDamage());
        }
        super.loadCustom(tag);
    }

    public float getDamage() {
        return this.data().getDamage();
    }

    public int getResultDamage() {
        double damage = (double) this.motion.length() * this.getDamage();
        return (int) Math.ceil(Math.clamp(damage, 0.0, Integer.MAX_VALUE));
    }

    public void setDamage(float damage) {
        if (!Float.isFinite(damage) || damage < 0) {
            throw new IllegalArgumentException("damage must be finite and nonnegative");
        }

        this.data().setDamage(damage);
    }

    protected float getBaseDamage() {
        return 0;
    }

    protected boolean canHitEntity(Entity entity) {
        Entity owner = this.getOwner();
        return entity != null && entity.isAlive()
                && (!(entity instanceof CloudPlayer player) || !player.isSpectator())
                && (this.leftShooter || owner == null || rootVehicle(entity) != rootVehicle(owner))
                && this.canCollideWith(entity);
    }

    protected boolean applyDamage(CloudEntityDamageEvent event) {
        return event.getDamageType() == DamageTypes.OUT_OF_WORLD && super.applyDamage(event);
    }

    protected void onCollideWithEntity(Entity entity) {
        this.damageEntity(entity);
        this.remove(EntityRemoveCause.HIT);
    }

    protected boolean damageEntity(Entity entity) {
        float damage = this.getResultDamage();
        if (CloudEntity.applyDamage(entity, damage, this.createProjectileDamageSource())) {
            this.hadCollision = true;
            if (this.fireTicks > 0) {
                EntityCombustByEntityEvent event = new EntityCombustByEntityEvent(this, entity, 5);
                this.server.getEventManager().fire(event);
                if (!event.isCancelled()) {
                    entity.setOnFire(event.getDuration());
                }
            }

            return true;
        }

        return false;
    }

    protected final DamageSource createProjectileDamageSource() {
        DamageType damageType = CloudEntityRegistry.get()
                .requireComponent(this.getType(), EntityComponents.GET_PROJECTILE_DAMAGE_TYPE)
                .execute(this);
        DamageSource.Builder source = DamageSource.builder(damageType)
                .directEntity(this);

        Entity owner = this.getOwner();
        if (owner != null) {
            source.causingEntity(owner);
        } else if (damageType == DamageTypes.UNATTRIBUTED_FIREBALL) {
            source.causingEntity(this);
        }

        return source.build();
    }

    @Override
    protected void initEntity() {
        super.initEntity();

        this.data().setDamage(this.getBaseDamage());
        this.setDefaultMaxHealth(1);
        this.setHealth(1);
    }

    @Override
    public boolean canCollideWith(Entity entity) {
        return !this.isOnGround() && (entity instanceof CloudLivingEntity || entity instanceof EnderCrystal
                || entity instanceof Painting || entity instanceof Vehicle);
    }

    @Override
    protected boolean hasMovementEntityCollisions() {
        return false;
    }

    @Override
    protected boolean tickEntity(int currentTick, int elapsedTicks) {
        if (this.isClosed()) {
            return false;
        }

        boolean hasUpdate = this.entityBaseTick(elapsedTicks);

        if (this.isAlive()) {
            Vector3f nextMotion = this.motion.mul(this.isInsideOfWater() ? this.getWaterInertia() : 1 - this.getDrag())
                    .sub(0, this.getGravity(), 0);
            Vector3f nextPosition = this.position.add(nextMotion);
            RayTraceContext trace = new RayTraceContext(this.position, nextPosition, BlockShapeMode.COLLIDER,
                    FluidCollisionMode.NONE, CollisionContext.of(this));

            float hitMargin = Math.clamp((this.age - 2) / 20.0f, 0.0f, 0.3f);
            HitResult hit = this.traceMovement(trace, hitMargin);
            if (hit instanceof EntityHitResult entityHit) {
                ProjectileHitEvent event = new ProjectileHitEvent(this, entityHit);
                this.dispatchHitEvent(event);
                if (this.isClosed()) {
                    return true;
                }

                if (!event.isCancelled()) {
                    this.motion = nextMotion;
                    this.setPosition(entityHit.position());
                    this.onCollideWithEntity(entityHit.entity());
                    if (!this.isClosed()) {
                        this.afterHit();
                    }
                    this.flushEntityData();
                    return true;
                }

                hit = this.level.rayTraceBlocks(trace);
            }

            if (hit instanceof BlockHitResult blockHit) {
                ProjectileHitEvent event = new ProjectileHitEvent(this, blockHit);
                this.dispatchHitEvent(event);
                this.handleBlockImpact(blockHit, nextMotion, !event.isCancelled());
                return true;
            }

            if (hit instanceof MissHitResult(Vector3f stopPosition, MissReason reason) && reason == MissReason.UNLOADED) {
                this.setPosition(stopPosition);
                this.updateMovement();
                this.flushEntityData();
                return hasUpdate;
            }

            this.motion = nextMotion;
            this.setPosition(nextPosition);
            if (this.motion.lengthSquared() > 0.0000000001f) {
                this.orientToMotion(this.motion);
                hasUpdate = true;
            }

            this.updateMovement();
        }

        this.flushEntityData();
        return hasUpdate;
    }

    protected HitResult traceMovement(RayTraceContext trace, float hitMargin) {
        this.updateLeftShooter();
        return this.level.rayTrace(trace, hitMargin, this::canHitEntity);
    }

    protected void dispatchHitEvent(ProjectileHitEvent event) {
        this.server.getEventManager().fire(event);
    }

    protected final void notifyHitBlock(BlockHitResult hit) {
        hit.block().requireComponent(BlockComponents.ON_PROJECTILE_HIT).execute(hit, this);
    }

    protected void onBlockCollision(BlockHitResult hit) {
    }

    protected void afterHit() {
    }

    protected final boolean hasBlockImpact() {
        return this.blockImpact;
    }

    protected final void clearBlockImpact() {
        this.blockImpact = false;
    }

    protected float getWaterInertia() {
        return 0.8f;
    }

    private void handleBlockImpact(BlockHitResult hit, Vector3f impactMotion, boolean notifyBlock) {
        if (this.isClosed()) {
            return;
        }

        this.motion = impactMotion;
        this.setPosition(hit.position());
        this.blockImpact = true;
        this.hadCollision = true;
        this.onBlockCollision(hit);
        if (notifyBlock) {
            this.notifyHitBlock(hit);
        }

        if (!this.isClosed()) {
            this.afterHit();
        }

        if (!this.isClosed()) {
            this.motion = Vector3f.ZERO;
            this.updateMovement();
            this.flushEntityData();
        }
    }

    private void orientToMotion(Vector3f motion) {
        double horizontal = Math.hypot(motion.getX(), motion.getZ());
        this.yaw = (float) Math.toDegrees(Math.atan2(motion.getX(), motion.getZ()));
        this.pitch = (float) Math.toDegrees(Math.atan2(motion.getY(), horizontal));
    }

    private void updateLeftShooter() {
        if (this.leftShooter) {
            return;
        }

        Entity shooter = this.getOwner();
        BoundingBox clearanceBox = this.getBoundingBox().expandTowards(this.motion).inflate(1, 1, 1);
        if (shooter == null || !intersectsEntityTree(rootVehicle(shooter), clearanceBox)) {
            this.leftShooter = true;
        }
    }

    private static boolean intersectsEntityTree(Entity entity, BoundingBox box) {
        if (box.intersects(entity.getBoundingBox())) {
            return true;
        }

        for (Entity passenger : entity.getPassengers()) {
            if (intersectsEntityTree(passenger, box)) {
                return true;
            }
        }

        return false;
    }
}
