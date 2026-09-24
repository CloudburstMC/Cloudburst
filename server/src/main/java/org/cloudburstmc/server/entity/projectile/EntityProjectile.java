package org.cloudburstmc.server.entity.projectile;

import lombok.Getter;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.BlockComponents;
import org.cloudburstmc.api.entity.*;
import org.cloudburstmc.api.entity.damage.DamageSource;
import org.cloudburstmc.api.entity.damage.DamageType;
import org.cloudburstmc.api.entity.damage.DamageTypes;
import org.cloudburstmc.api.entity.misc.EnderCrystal;
import org.cloudburstmc.api.event.entity.EntityCombustByEntityEvent;
import org.cloudburstmc.api.event.entity.EntityDamageEvent;
import org.cloudburstmc.api.event.entity.ProjectileHitEvent;
import org.cloudburstmc.api.level.BlockShapeMode;
import org.cloudburstmc.api.level.FluidCollisionMode;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.level.RayTraceContext;
import org.cloudburstmc.api.util.*;
import org.cloudburstmc.math.GenericMath;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.entity.EntityLiving;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudEntityRegistry;

public abstract class EntityProjectile extends CloudEntity implements Projectile {

    protected boolean hadCollision;
    @Getter
    protected float damage;
    private boolean leftShooter;
    private @Nullable ProjectileSource shooter;

    public EntityProjectile(EntityType<?> type, Location location) {
        super(type, location);
    }

    @Override
    public boolean setMotion(Vector3f motion) {
        if (motion.lengthSquared() > 0) {
            this.orientToMotion(motion);
        }

        return super.setMotion(motion);
    }

    @Override
    public @Nullable ProjectileSource getShooter() {
        Entity owner = this.getOwner();
        return owner instanceof ProjectileSource source ? source : this.shooter;
    }

    @Override
    public void setShooter(@Nullable ProjectileSource shooter) {
        this.setOwner(shooter instanceof Entity entity ? entity : null);
        this.shooter = shooter;
    }

    @Override
    public void setOwner(@Nullable Entity owner) {
        super.setOwner(owner);
        this.shooter = owner instanceof ProjectileSource source ? source : null;
        this.leftShooter = false;
    }

    @Override
    public void loadAdditionalData(NbtMap tag) {
        super.loadAdditionalData(tag);
        tag.listenForNumber("damage", v -> this.setDamage(v.floatValue()));
    }

    @Override
    public void saveAdditionalData(NbtMapBuilder tag) {
        super.saveAdditionalData(tag);
        tag.putFloat("damage", this.damage);
    }

    public int getResultDamage() {
        return GenericMath.ceil(this.motion.length() * getDamage());
    }

    public void setDamage(float damage) {
        if (!Float.isFinite(damage) || damage < 0) {
            throw new IllegalArgumentException("damage must be finite and nonnegative");
        }

        this.damage = damage;
    }

    protected float getBaseDamage() {
        return 0;
    }

    protected boolean canHitEntity(Entity entity) {
        return entity != null && entity.isAlive()
                && (!(entity instanceof CloudPlayer player) || !player.isSpectator())
                && (entity != this.getOwner() || this.leftShooter)
                && this.canCollideWith(entity);
    }

    protected boolean applyDamage(EntityDamageEvent source) {
        return source.getDamageType() == DamageTypes.OUT_OF_WORLD && super.applyDamage(source);
    }

    protected void onCollideWithEntity(Entity entity) {
        float damage = this.getResultDamage();
        if (entity.damage(damage, this.createProjectileDamageSource())) {
            this.hadCollision = true;

            if (this.fireTicks > 0) {
                EntityCombustByEntityEvent event = new EntityCombustByEntityEvent(this, entity, 5);
                this.server.getEventManager().fire(event);
                if (!event.isCancelled()) {
                    entity.setOnFire(event.getDuration());
                }
            }
        }

        this.close();
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

        this.damage = this.getBaseDamage();
        this.setMaxHealth(1);
        this.setHealth(1);
    }

    @Override
    public boolean canCollideWith(Entity entity) {
        return (entity instanceof EntityLiving || entity instanceof EnderCrystal) && !this.onGround;
    }

    @Override
    protected boolean hasMovementEntityCollisions() {
        return false;
    }

    @Override
    public boolean onUpdate(int currentTick) {
        if (this.closed) {
            return false;
        }

        int tickDiff = currentTick - this.lastUpdate;
        if (tickDiff <= 0 && !this.justCreated) {
            return true;
        }

        this.lastUpdate = currentTick;
        boolean hasUpdate = this.entityBaseTick(tickDiff);

        if (this.isAlive()) {
            Vector3f nextMotion = this.motion.mul(this.isInsideOfWater() ? this.getWaterInertia() : 1 - this.getDrag())
                    .sub(0, this.getGravity(), 0);
            Vector3f moveVector = this.position.add(nextMotion);
            RayTraceContext trace = new RayTraceContext(this.position, moveVector, BlockShapeMode.COLLIDER,
                    FluidCollisionMode.NONE, CollisionContext.of(this));

            float hitMargin = Math.clamp((this.age - 2) / 20.0f, 0.0f, 0.3f);
            HitResult hit = this.traceMovement(trace, hitMargin);
            if (hit instanceof EntityHitResult entityHit) {
                ProjectileHitEvent event = new ProjectileHitEvent(this, entityHit);
                this.server.getEventManager().fire(event);
                if (!event.isCancelled()) {
                    this.motion = nextMotion;
                    this.setPosition(entityHit.position());
                    this.onCollideWithEntity(entityHit.entity());
                    this.data.update();
                    return true;
                }

                hit = this.level.rayTraceBlocks(trace);
            }

            if (hit instanceof BlockHitResult blockHit) {
                ProjectileHitEvent event = new ProjectileHitEvent(this, blockHit);
                this.server.getEventManager().fire(event);
                if (!event.isCancelled()) {
                    this.motion = nextMotion;
                    this.setPosition(blockHit.position());
                    this.isCollided = true;
                    this.hadCollision = true;
                    this.onBlockCollision(blockHit);
                    blockHit.block().requireComponent(BlockComponents.ON_PROJECTILE_HIT).execute(blockHit.block(), this);
                    this.motion = Vector3f.ZERO;
                    this.updateMovement();
                    this.data.update();
                    return true;
                }
            }

            if (hit instanceof MissHitResult(Vector3f position1, MissReason reason) && reason == MissReason.UNLOADED) {
                this.setPosition(position1);
                this.updateMovement();
                this.data.update();
                return hasUpdate;
            }

            this.motion = nextMotion;
            this.setPosition(moveVector);
            if (this.motion.lengthSquared() > 0.0000000001f) {
                this.orientToMotion(this.motion);
                hasUpdate = true;
            }

            this.updateMovement();
        }

        this.data.update();
        return hasUpdate;
    }

    protected void onBlockCollision(BlockHitResult hit) {
    }

    protected final HitResult traceMovement(RayTraceContext trace, float hitMargin) {
        this.updateLeftShooter();
        return this.level.rayTrace(trace, hitMargin, this::canHitEntity);
    }

    protected float getWaterInertia() {
        return 0.8f;
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
        if (shooter == null || !intersectsVehicle(shooter, clearanceBox)) {
            this.leftShooter = true;
        }
    }

    private static boolean intersectsVehicle(Entity entity, BoundingBox box) {
        Entity root = entity;
        while (root.getVehicle() != null) {
            root = root.getVehicle();
        }

        return intersectsEntityTree(root, box);
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
