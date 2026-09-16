package org.cloudburstmc.server.entity.projectile;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityComponents;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.Projectile;
import org.cloudburstmc.api.entity.damage.DamageSource;
import org.cloudburstmc.api.entity.damage.DamageType;
import org.cloudburstmc.api.entity.damage.DamageTypes;
import org.cloudburstmc.api.entity.misc.EnderCrystal;
import org.cloudburstmc.api.event.entity.EntityCombustByEntityEvent;
import org.cloudburstmc.api.event.entity.EntityDamageEvent;
import org.cloudburstmc.api.event.entity.ProjectileHitEvent;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.util.BoundingBox;
import org.cloudburstmc.api.util.MovingObjectPosition;
import org.cloudburstmc.math.GenericMath;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.entity.EntityLiving;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudEntityRegistry;

import java.util.Set;

import static org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag.CRITICAL;

public abstract class EntityProjectile extends CloudEntity implements Projectile {

    public boolean hadCollision = false;
    public boolean closeOnCollide = true;
    protected float damage;
    private boolean leftShooter;

    public EntityProjectile(EntityType<?> type, Location location) {
        super(type, location);
    }

    @Override
    public void loadAdditionalData(NbtMap tag) {
        super.loadAdditionalData(tag);

        tag.listenForNumber("damage", v -> this.damage = v.floatValue());
    }

    @Override
    public void saveAdditionalData(NbtMapBuilder tag) {
        super.saveAdditionalData(tag);

        tag.putFloat("damage", this.damage);
    }

    public int getResultDamage() {
        return GenericMath.ceil(this.motion.length() * getDamage());
    }

    public float getDamage() {
        return damage <= 0 ? getBaseDamage() : damage;
    }

    public void setDamage(float damage) {
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

    public void onCollideWithEntity(Entity entity) {
        this.server.getEventManager().fire(new ProjectileHitEvent(this, MovingObjectPosition.fromEntity(entity)));
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
        if (closeOnCollide) {
            this.close();
        }
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

            this.updateLeftShooter();

            MovingObjectPosition movingObjectPosition = null;

            if (!this.isCollided) {
                this.motion = motion.sub(0, this.getGravity(), 0);
            }

            Vector3f moveVector = this.position.add(this.motion);

            Set<Entity> collidingEntities = this.getLevel().getCollidingEntities(
                    this,
                    this.boundingBox.expandTowards(this.motion).inflate(1, 1, 1));

            double nearDistance = Integer.MAX_VALUE;
            Entity nearEntity = null;

            for (Entity entity : collidingEntities) {
                if (!this.canHitEntity(entity)) {
                    continue;
                }

                BoundingBox boundingBox = entity.getBoundingBox().inflate(0.3f, 0.3f, 0.3f);
                MovingObjectPosition ob = boundingBox.clip(this.getPosition(), moveVector);

                if (ob == null) {
                    continue;
                }

                double distance = this.position.distanceSquared(ob.hitVector);

                if (distance < nearDistance) {
                    nearDistance = distance;
                    nearEntity = entity;
                }
            }

            if (nearEntity != null) {
                movingObjectPosition = MovingObjectPosition.fromEntity(nearEntity);
            }

            if (movingObjectPosition != null) {
                if (movingObjectPosition.entityHit != null) {
                    onCollideWithEntity(movingObjectPosition.entityHit);
                    return true;
                }
            }

            this.move(this.motion);

            if (this.isCollided && !this.hadCollision) { //collide with block
                this.hadCollision = true;

                this.motion = Vector3f.ZERO;

                this.server.getEventManager().fire(new ProjectileHitEvent(this,
                        MovingObjectPosition.fromBlock(this.position.toInt(), -1, this.getPosition())));
                return false;
            } else if (!this.isCollided && this.hadCollision) {
                this.hadCollision = false;
            }

            if (!this.hadCollision || motion.length() > 0.00001) {
                double f = Math.sqrt((this.motion.getX() * this.motion.getX()) + (this.motion.getZ() * this.motion.getZ()));
                this.yaw = (float) (Math.atan2(this.motion.getX(), this.motion.getZ()) * 180 / Math.PI);
                this.pitch = (float) (Math.atan2(this.motion.getY(), f) * 180 / Math.PI);
                hasUpdate = true;
            }

            this.updateMovement();

        }

        return hasUpdate;
    }

    public void setCritical() {
        this.setCritical(true);
    }

    public boolean isCritical() {
        return this.data.getFlag(CRITICAL);
    }

    public void setCritical(boolean value) {
        this.data.setFlag(CRITICAL, value);
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
