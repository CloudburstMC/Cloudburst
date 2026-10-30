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
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityEventType;
import org.cloudburstmc.protocol.bedrock.packet.EntityEventPacket;
import org.cloudburstmc.server.CloudServer;

import java.util.HashSet;
import java.util.Set;

import static java.util.Objects.requireNonNull;
import static org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag.CRITICAL;

public abstract class EntityAbstractArrow extends EntityProjectile implements AbstractArrow {

    private final Set<Entity> piercedEntities = new HashSet<>();
    private Entity deflectedFrom;
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
        return switch (this.data().getPickup()) {
            case 0 -> ArrowPickupStatus.DISALLOWED;
            case 2 -> ArrowPickupStatus.CREATIVE_ONLY;
            default -> ArrowPickupStatus.ALLOWED;
        };
    }

    @Override
    public void setPickupStatus(ArrowPickupStatus status) {
        this.data().setPickup(switch (requireNonNull(status, "status")) {
            case DISALLOWED -> 0;
            case ALLOWED -> 1;
            case CREATIVE_ONLY -> 2;
        });
    }

    @Override
    public int getPierceLevel() {
        return this.data().getPierceLevel();
    }

    @Override
    public void setPierceLevel(int level) {
        if (level < 0 || level > 127) {
            throw new IllegalArgumentException("level must be between zero and 127");
        }

        this.data().setPierceLevel(level);
    }

    @Override
    protected AbstractArrowData data() {
        return (AbstractArrowData) super.data();
    }

    @Override
    protected Class<? extends EntityData> minimumDataClass() {
        return AbstractArrowData.class;
    }

    @Override
    protected void loadCustom(NbtMap tag) {
        // Migrated from applyData: the pierce level, pickup rule and embedded position run
        // before the projectile load below, matching the old applyData-before-loadCustom
        // order. The setters re-run to keep their validation, exactly as the old applyData
        // did. The pickup rule has no presence bit, so it always falls back to open pickup.
        // The embedded position only changes when all three keys are present.
        if (tag.containsKey("PierceLevel")) {
            this.setPierceLevel(this.data().getPierceLevel());
        }
        if (tag.containsKey("pickup")) {
            this.setPickupStatus(this.getPickupStatus());
        } else {
            this.setPickupStatus(ArrowPickupStatus.ALLOWED);
        }
        if (tag.containsKey("inBlockX") && tag.containsKey("inBlockY") && tag.containsKey("inBlockZ")) {
            Vector3i embedded = this.data().getEmbeddedPosition();
            int x = embedded == null ? 0 : embedded.getX();
            int y = embedded == null ? 0 : embedded.getY();
            int z = embedded == null ? 0 : embedded.getZ();
            this.data().setEmbeddedPosition(Vector3i.from(
                    getInt(tag, "inBlockX", x), getInt(tag, "inBlockY", y), getInt(tag, "inBlockZ", z)));
        }
        super.loadCustom(tag);
    }

    @Override
    protected void saveCustom(NbtMapBuilder tag) {
        super.saveCustom(tag);
        // The embedded position is conditional: it is only written while stuck in a block.
        Vector3i embeddedPosition = this.data().getEmbeddedPosition();
        if (this.data().isInGround() && embeddedPosition != null) {
            tag.putInt("inBlockX", embeddedPosition.getX());
            tag.putInt("inBlockY", embeddedPosition.getY());
            tag.putInt("inBlockZ", embeddedPosition.getZ());
        }
    }

    @Override
    protected Set<String> customKeys() {
        Set<String> keys = new HashSet<>(super.customKeys());
        keys.add("inBlockX");
        keys.add("inBlockY");
        keys.add("inBlockZ");
        return Set.copyOf(keys);
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return entity != this.deflectedFrom && !this.piercedEntities.contains(entity) && super.canHitEntity(entity);
    }

    @Override
    protected void onCollideWithEntity(Entity entity) {
        if (this.damageEntity(entity)) {
            this.piercedEntities.add(entity);
            if (this.piercedEntities.size() > this.getPierceLevel()) {
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
        this.data().setEmbeddedPosition(hit.block().getPosition());
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
        this.data().setInGround(true);
        this.onGround = true;
        this.setCritical(false);
        this.motion = Vector3f.ZERO;
    }

    protected final boolean isEmbedded() {
        return this.data().isInGround();
    }

    @Override
    protected float getWaterInertia() {
        return 0.6f;
    }

    @Override
    public boolean onUpdate(int currentTick) {
        Vector3i embeddedPosition = this.data().getEmbeddedPosition();
        if (this.data().isInGround() && embeddedPosition != null) {
            BlockState currentState = this.level.getBlock(embeddedPosition).getState();
            if (currentState != this.embeddedState) {
                if (currentState.getCollisionShape().isEmpty()) {
                    this.data().setInGround(false);
                    this.data().setEmbeddedPosition(null);
                    this.embeddedState = null;
                    this.isCollided = false;
                    this.onGround = false;
                } else {
                    this.embeddedState = currentState;
                }
            }
        }

        if (!this.data().isInGround()) {
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
