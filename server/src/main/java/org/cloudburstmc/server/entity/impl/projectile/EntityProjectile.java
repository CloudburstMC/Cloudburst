package org.cloudburstmc.server.entity.impl.projectile;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import org.cloudburstmc.api.event.entity.EntityDamageEvent;
import org.cloudburstmc.api.util.AxisAlignedBB;
import org.cloudburstmc.api.util.MovingObjectPosition;
import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.entity.EntityType;
import org.cloudburstmc.server.entity.impl.BaseEntity;
import org.cloudburstmc.server.entity.impl.EntityLiving;
import org.cloudburstmc.server.entity.misc.EnderCrystal;
import org.cloudburstmc.server.event.entity.EntityCombustByEntityEvent;
import org.cloudburstmc.server.event.entity.EntityDamageByChildEntityEvent;
import org.cloudburstmc.server.event.entity.EntityDamageByEntityEvent;
import org.cloudburstmc.server.event.entity.ProjectileHitEvent;
import org.cloudburstmc.server.level.Location;
import org.cloudburstmc.server.math.NukkitMath;

import java.util.Set;

import static com.nukkitx.protocol.bedrock.data.entity.EntityFlag.CRITICAL;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public abstract class EntityProjectile extends BaseEntity {

    protected float damage;
    public boolean hadCollision = false;
    public boolean closeOnCollide = true;

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
        return NukkitMath.ceilFloat(this.motion.length() * getDamage());
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

    public boolean attack(EntityDamageEvent source) {
        return source.getCause() == EntityDamageEvent.DamageCause.VOID && super.attack(source);
    }

    public void onCollideWithEntity(Entity entity) {
        this.server.getEventManager().fire(new ProjectileHitEvent(this, MovingObjectPosition.fromEntity(entity)));
        float damage = this.getResultDamage();

        EntityDamageEvent ev;
        if (this.getOwner() == null) {
            ev = new EntityDamageByEntityEvent(this, entity, EntityDamageEvent.DamageCause.PROJECTILE, damage);
        } else {
            ev = new EntityDamageByChildEntityEvent(this.getOwner(), this, entity, EntityDamageEvent.DamageCause.PROJECTILE, damage);
        }
        if (entity.attack(ev)) {
            this.hadCollision = true;

            if (this.fireTicks > 0) {
                EntityCombustByEntityEvent event = new EntityCombustByEntityEvent(this, entity, 5);
                this.server.getEventManager().fire(ev);
                if (!event.isCancelled()) {
                    entity.setOnFire(event.getDuration());
                }
            }
        }
        if (closeOnCollide) {
            this.close();
        }
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

            MovingObjectPosition movingObjectPosition = null;

            if (!this.isCollided) {
                this.motion = motion.sub(0, this.getGravity(), 0);
            }

            Vector3f moveVector = this.position.add(this.motion);

            Set<Entity> collidingEntities = this.getLevel().getCollidingEntities(
                    this.boundingBox.addCoord(this.motion).expand(1, 1, 1),
                    this);

            double nearDistance = Integer.MAX_VALUE;
            Entity nearEntity = null;

            for (Entity entity : collidingEntities) {
                if (/*!entity.canCollideWith(this) or */
                        (entity == this.getOwner() && this.ticksLived < 5)
                ) {
                    continue;
                }

                AxisAlignedBB axisalignedbb = entity.getBoundingBox().grow(0.3f, 0.3f, 0.3f);
                MovingObjectPosition ob = axisalignedbb.calculateIntercept(this.getPosition(), moveVector);

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
}
