package org.cloudburstmc.server.entity.misc;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.damage.DamageTypeTags;
import org.cloudburstmc.api.entity.damage.DamageTypes;
import org.cloudburstmc.api.entity.misc.ExperienceOrb;
import org.cloudburstmc.api.event.entity.EntityDamageEvent;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.player.CloudPlayer;

import static org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes.VALUE;

public class EntityExperienceOrb extends CloudEntity implements ExperienceOrb {

    private static final int MERGE_INTERVAL = 20;
    private static final float MERGE_DISTANCE = 0.5f;

    private @Nullable CloudPlayer closestPlayer;
    private int count = 1;
    private int pickupDelay;

    public EntityExperienceOrb(EntityType<ExperienceOrb> type, Location location) {
        super(type, location);
    }

    @Override
    public float getWidth() {
        return 0.25f;
    }

    @Override
    public float getLength() {
        return 0.25f;
    }

    @Override
    public float getHeight() {
        return 0.25f;
    }

    @Override
    public float getGravity() {
        return 0.03f;
    }

    @Override
    public float getDrag() {
        return 0.02f;
    }

    @Override
    public boolean canCollide() {
        return false;
    }

    @Override
    protected void initEntity() {
        super.initEntity();

        setMaxHealth(5);
        setHealth(5);

        this.data.set(VALUE, 1);

        //call event item spawn event
    }

    @Override
    public void loadAdditionalData(NbtMap tag) {
        super.loadAdditionalData(tag);

        tag.listenForInt("experience value", this::setExperience);
        tag.listenForShort("Age", v -> this.age = v);
        tag.listenForInt("Count", this::setCount);
        tag.listenForShort("PickupDelay", v -> this.pickupDelay = v);
    }

    @Override
    public void saveAdditionalData(NbtMapBuilder tag) {
        super.saveAdditionalData(tag);

        tag.putInt("experience value", this.getExperience());
        tag.putShort("Age", (short) this.age);
        tag.putInt("Count", this.count);
        tag.putShort("PickupDelay", (short) this.pickupDelay);
    }

    @Override
    public boolean attack(EntityDamageEvent source) {
        return (source.getDamageType() == DamageTypes.OUT_OF_WORLD ||
                source.getDamageType() == DamageTypes.ON_FIRE ||
                source.getDamageType().is(DamageTypeTags.IS_EXPLOSION) &&
                        !this.isInsideOfWater()) && super.attack(source);
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

        boolean hasUpdate = entityBaseTick(tickDiff);
        if (this.isAlive()) {
            if (this.pickupDelay > 0 && this.pickupDelay < 32767) { //Infinite delay
                this.pickupDelay -= tickDiff;
                if (this.pickupDelay < 0) {
                    this.pickupDelay = 0;
                }
            } else {
                for (Entity entity : this.level.getNearbyEntities(this, this.boundingBox)) {
                    if (entity instanceof CloudPlayer) {
                        if (((CloudPlayer) entity).pickupEntity(this, false)) {
                            return true;
                        }
                    }
                }
            }

            if (this.isInsideOfWater()) {
                this.motion = Vector3f.from(
                        this.motion.getX() * 0.99f,
                        Math.min(this.motion.getY() + 0.0005f, 0.06f),
                        this.motion.getZ() * 0.99f
                );
            } else {
                this.motion = this.motion.sub(0, this.getGravity(), 0);
            }

            boolean colliding = this.level.hasCollision(this.getBoundingBox());

            if (this.age % MERGE_INTERVAL == 1) {
                mergeNearbyOrbs();
            }

            if (this.closestPlayer == null || this.closestPlayer.getPosition().distanceSquared(this.getPosition()) > 64.0D) {
                this.closestPlayer = findClosestPlayer();
            }

            if (this.closestPlayer != null && (!this.closestPlayer.isAlive() || this.closestPlayer.isSpectator())) {
                this.closestPlayer = null;
            }

            if (this.closestPlayer != null) {
                Vector3f direction = this.closestPlayer.getPosition()
                        .add(0, this.closestPlayer.getEyeHeight() / 2, 0)
                        .sub(this.getPosition());
                float distance = direction.length();
                if (distance > 0 && distance < 8) {
                    float attraction = 1 - distance / 8;
                    this.motion = this.motion.add(direction.div(distance).mul(attraction * attraction * 0.1f));
                }
            }

            if (colliding && this.level.hasCollision(this.getBoundingBox().move(this.motion))) {
                this.noPhysics = true;
                if (this.moveTowardsClosestSpace(this.getPosition())) {
                    hasUpdate = true;
                }
            } else {
                this.noPhysics = false;
            }

            this.move(this.motion);

            double friction = 1d - this.getDrag();

            if (this.onGround && (Math.abs(this.motion.getX()) > 0.00001 || Math.abs(this.motion.getZ()) > 0.00001)) {
                BlockState state = this.getLevel().getBlockState(this.getPosition().add(0, -1, 0).toInt());
                friction = state.getFriction() * friction;
            }

            this.motion = this.motion.mul(friction, 1 - this.getDrag(), friction);

            if (this.onGround) {
                this.motion = this.motion.mul(1, -0.4, 1);
            }

            this.updateMovement();

            if (this.age >= 6000) {
                this.close();
                hasUpdate = true;
            }

        }

        return hasUpdate || !this.onGround || this.motion.abs().length() > 0.00001;
    }

    private @Nullable CloudPlayer findClosestPlayer() {
        CloudPlayer closest = null;
        float closestDistance = 64;
        for (CloudPlayer player : this.level.getPlayers().values()) {
            if (!player.isAlive() || player.isSpectator()) {
                continue;
            }

            float distance = player.getPosition().distanceSquared(this.getPosition());
            if (distance < closestDistance) {
                closest = player;
                closestDistance = distance;
            }
        }

        return closest;
    }

    private void mergeNearbyOrbs() {
        for (Entity entity : this.level.getNearbyEntities(this,
                this.boundingBox.inflate(MERGE_DISTANCE, MERGE_DISTANCE, MERGE_DISTANCE))) {
            if (entity instanceof EntityExperienceOrb orb && canMerge(orb)) {
                this.count += orb.count;
                this.age = Math.min(this.age, orb.age);
                orb.close();
            }
        }
    }

    private boolean canMerge(EntityExperienceOrb orb) {
        return !orb.isClosed()
                && (orb.getRuntimeId() - this.getRuntimeId()) % 40 == 0
                && orb.getExperience() == this.getExperience();
    }

    private void setCount(int count) {
        if (count < 1) {
            throw new IllegalArgumentException("XP orb count must be greater than 0, got " + count);
        }

        this.count = count;
    }

    public void consumeOne() {
        if (--this.count == 0) {
            this.close();
        }
    }

    public int getExperience() {
        return this.data.get(VALUE);
    }

    public void setExperience(int experience) {
        if (experience <= 0) {
            throw new IllegalArgumentException("XP amount must be greater than 0, got " + experience);
        }
        this.data.set(VALUE, experience);
    }

    @Override
    public boolean canCollideWith(Entity entity) {
        return false;
    }

    @Override
    public int getPickupDelay() {
        return pickupDelay;
    }

    @Override
    public void setPickupDelay(int pickupDelay) {
        this.pickupDelay = pickupDelay;
    }
}
