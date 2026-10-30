package org.cloudburstmc.server.entity.projectile;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.data.AbstractArrowData;
import org.cloudburstmc.api.entity.data.EntityData;
import org.cloudburstmc.api.entity.projectile.AbstractArrow;
import org.cloudburstmc.api.entity.projectile.ArrowPickupStatus;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.util.BlockHitResult;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityEventType;
import org.cloudburstmc.protocol.bedrock.packet.EntityEventPacket;
import org.cloudburstmc.server.CloudServer;

import java.util.HashSet;
import java.util.Set;

import static java.util.Objects.requireNonNull;
import static org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag.CRITICAL;

public abstract class EntityAbstractArrow extends EntityProjectile implements AbstractArrow {

    private ArrowPickupStatus pickupStatus = ArrowPickupStatus.ALLOWED;
    private int pierceLevel;
    private final Set<Entity> piercedEntities = new HashSet<>();
    private Entity deflectedFrom;
    private boolean inGround;
    private Vector3i embeddedPosition;
    private BlockState embeddedState;

    public EntityAbstractArrow(EntityType<? extends AbstractArrow> type, Location location) {
        super(type, location);
    }

    @Override
    public boolean isCritical() {
        return this.data.getFlag(CRITICAL);
    }

    @Override
    public void setCritical(boolean critical) {
        this.data.setFlag(CRITICAL, critical);
    }

    @Override
    public ArrowPickupStatus getPickupStatus() {
        return this.pickupStatus;
    }

    @Override
    public void setPickupStatus(ArrowPickupStatus status) {
        this.pickupStatus = requireNonNull(status, "status");
    }

    @Override
    public int getPierceLevel() {
        return this.pierceLevel;
    }

    @Override
    public void setPierceLevel(int level) {
        if (level < 0 || level > 127) {
            throw new IllegalArgumentException("level must be between zero and 127");
        }

        this.pierceLevel = level;
    }

    @Override
    protected Class<? extends EntityData> minimumDataClass() {
        return AbstractArrowData.class;
    }

    @Override
    protected void applyData(EntityData data) {
        super.applyData(data);
        AbstractArrowData arrowData = (AbstractArrowData) data;
        if (arrowData.getInGround() != null) {
            this.inGround = arrowData.getInGround();
        }
        if (arrowData.getPierceLevel() != null) {
            this.setPierceLevel(arrowData.getPierceLevel());
        }
        if (arrowData.getInBlockX() != null && arrowData.getInBlockY() != null && arrowData.getInBlockZ() != null) {
            this.embeddedPosition = Vector3i.from(arrowData.getInBlockX(), arrowData.getInBlockY(), arrowData.getInBlockZ());
        }

        this.pickupStatus = switch (arrowData.getPickup()) {
            case 0 -> ArrowPickupStatus.DISALLOWED;
            case 2 -> ArrowPickupStatus.CREATIVE_ONLY;
            default -> ArrowPickupStatus.ALLOWED;
        };
    }

    @Override
    protected void fillData(EntityData data) {
        super.fillData(data);
        AbstractArrowData arrowData = (AbstractArrowData) data;
        arrowData.setInGround(this.inGround);
        arrowData.setPierceLevel(this.pierceLevel);
        if (this.inGround && this.embeddedPosition != null) {
            arrowData.setInBlockX(this.embeddedPosition.getX());
            arrowData.setInBlockY(this.embeddedPosition.getY());
            arrowData.setInBlockZ(this.embeddedPosition.getZ());
        }

        arrowData.setPickup(switch (this.pickupStatus) {
            case DISALLOWED -> 0;
            case ALLOWED -> 1;
            case CREATIVE_ONLY -> 2;
        });
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return entity != this.deflectedFrom && !this.piercedEntities.contains(entity) && super.canHitEntity(entity);
    }

    @Override
    protected void onCollideWithEntity(Entity entity) {
        if (this.damageEntity(entity)) {
            this.piercedEntities.add(entity);
            if (this.piercedEntities.size() > this.pierceLevel) {
                this.close();
            } else {
                this.updateMovement();
            }

            return;
        }

        this.deflectedFrom = entity;
        this.setCritical(false);
        this.motion = this.motion.mul(-0.2f);
        this.yaw = (this.yaw + 180) % 360;
        this.pitch = -this.pitch;
        this.updateMovement();
    }

    @Override
    protected void onBlockCollision(BlockHitResult hit) {
        this.embeddedPosition = hit.block().getPosition();
        this.embeddedState = hit.block().getState();
        this.setPosition(this.getPosition().sub(
                Math.signum(this.motion.getX()) * 0.05f,
                Math.signum(this.motion.getY()) * 0.05f,
                Math.signum(this.motion.getZ()) * 0.05f));
        this.embed();

        EntityEventPacket shake = new EntityEventPacket();
        shake.setRuntimeEntityId(this.getRuntimeId());
        shake.setType(EntityEventType.ARROW_SHAKE);
        shake.setData(7);
        CloudServer.broadcastPacket(this.getViewers(), shake);
        this.sendAuthoritativeDisplacement();
    }

    protected final void embed() {
        this.inGround = true;
        this.onGround = true;
        this.setCritical(false);
        this.motion = Vector3f.ZERO;
    }

    protected final boolean isEmbedded() {
        return this.inGround;
    }

    @Override
    protected float getWaterInertia() {
        return 0.6f;
    }

    @Override
    public boolean onUpdate(int currentTick) {
        if (this.inGround && this.embeddedPosition != null) {
            BlockState currentState = this.level.getBlock(this.embeddedPosition).getState();
            if (currentState != this.embeddedState) {
                if (currentState.getCollisionShape().isEmpty()) {
                    this.inGround = false;
                    this.embeddedPosition = null;
                    this.embeddedState = null;
                    this.isCollided = false;
                    this.onGround = false;
                } else {
                    this.embeddedState = currentState;
                }
            }
        }

        if (!this.inGround) {
            return super.onUpdate(currentTick);
        }

        int tickDiff = currentTick - this.lastUpdate;
        if (tickDiff <= 0) {
            return false;
        }

        this.lastUpdate = currentTick;
        boolean hasUpdate = this.entityBaseTick(tickDiff);
        this.motion = Vector3f.ZERO;
        this.flushEntityData();
        return hasUpdate;
    }
}
