package org.cloudburstmc.server.entity.misc;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.LiquidTypes;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.misc.ExperienceOrb;
import org.cloudburstmc.api.event.entity.EntityRemoveCause;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.entity.data.CloudExperienceOrbData;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.IntConsumer;

import static org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes.VALUE;

public class EntityExperienceOrb extends CloudEntity implements ExperienceOrb {

    private static final int MERGE_INTERVAL = 20;
    private static final float MERGE_DISTANCE = 0.5f;
    private static final int[] DENOMINATIONS = {2477, 1237, 617, 307, 149, 73, 37, 17, 7, 3, 1};

    private @Nullable CloudPlayer closestPlayer;

    private final CloudExperienceOrbData persistenceState;

    public EntityExperienceOrb(EntityType<ExperienceOrb> entityType, Location location) {
        CloudExperienceOrbData state = new CloudExperienceOrbData();
        super(entityType, location, state);
        this.persistenceState = state;
        this.data.set(VALUE, 1);
    }

    public static void forEachOrbValue(int experience, IntConsumer action) {
        for (int value : DENOMINATIONS) {
            while (experience >= value) {
                action.accept(value);
                experience -= value;
            }
        }
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
    public boolean canCollideWith(Entity entity) {
        return false;
    }

    @Override
    protected void initEntity() {
        super.initEntity();

        this.setDefaultMaxHealth(5);
        setHealth(5);

        //call event item spawn event
    }

    @Override
    protected CloudExperienceOrbData data() {
        return this.persistenceState;
    }

    @Override
    protected void loadCustom(NbtMap tag) {
        if (tag.containsKey("Age")) {
            this.age = getShort(tag, "Age", (short) this.age);
        }

        if (tag.containsKey("experience value")) {
            this.setExperience(getInt(tag, "experience value", this.getExperience()));
        }

        if (tag.containsKey("Count")) {
            this.setCount(this.data().getCount());
        }

        if (tag.containsKey("PickupDelay")) {
            this.setPickupDelay(this.data().getPickupDelay());
        }

        super.loadCustom(tag);
    }

    @Override
    protected void saveCustom(NbtMapBuilder tag) {
        super.saveCustom(tag);
        tag.putShort("Age", (short) this.age);
        tag.putInt("experience value", this.getExperience());
    }

    @Override
    protected Set<String> customKeys() {
        Set<String> keys = new HashSet<>(super.customKeys());
        keys.add("Age");
        keys.add("experience value");
        return Set.copyOf(keys);
    }

    @Override
    protected boolean tickEntity(int currentTick, int elapsedTicks) {
        if (this.isClosed()) {
            return false;
        }

        boolean hasUpdate = this.entityBaseTick(elapsedTicks);
        if (this.isAlive()) {
            int pickupDelay = this.getPickupDelay();
            if (pickupDelay > 0 && pickupDelay < Short.MAX_VALUE) {
                this.setPickupDelay(pickupDelay - elapsedTicks);
            } else if (pickupDelay <= 0) {
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

            Vector3f position = this.getPosition();
            Block block = this.level.getLoadedBlock(position.getFloorX(), position.getFloorY(), position.getFloorZ());
            if (block != null && block.getLiquid().getType().isSameFamily(LiquidTypes.LAVA)) {
                ThreadLocalRandom random = ThreadLocalRandom.current();
                this.motion = Vector3f.from((random.nextFloat() - random.nextFloat()) * 0.2f, 0.2f,
                        (random.nextFloat() - random.nextFloat()) * 0.2f);
            }

            boolean colliding = this.level.hasCollision(this.getBoundingBox());

            if (this.age % MERGE_INTERVAL == 1) {
                mergeNearbyOrbs();
            }

            if (this.closestPlayer == null || !this.closestPlayer.isAlive() || this.closestPlayer.isSpectator()
                    || this.closestPlayer.getLevel() != this.level
                    || this.closestPlayer.getPosition().distanceSquared(this.getPosition()) > 64.0D) {
                this.closestPlayer = findClosestPlayer();
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

            if (this.isOnGround() && (Math.abs(this.motion.getX()) > 0.00001 || Math.abs(this.motion.getZ()) > 0.00001)) {
                BlockState state = this.getLevel().getBlockState(this.getPosition().add(0, -1, 0).toInt());
                friction = state.getFriction() * friction;
            }

            this.motion = this.motion.mul(friction, 1 - this.getDrag(), friction);

            if (this.isOnGround()) {
                this.motion = this.motion.mul(1, -0.4, 1);
            }

            this.updateMovement();

            if (this.age >= 6000) {
                this.remove(EntityRemoveCause.DESPAWN);
                hasUpdate = true;
            }

        }

        return hasUpdate || !this.isOnGround() || this.motion.abs().length() > 0.00001;
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
                this.setCount(this.getCount() + orb.getCount());
                this.age = Math.min(this.age, orb.age);
                orb.remove(EntityRemoveCause.MERGE);
            }
        }
    }

    private boolean canMerge(EntityExperienceOrb orb) {
        return orb != this && !orb.isClosed()
                && orb.getCount() <= Integer.MAX_VALUE - this.getCount()
                && orb.getPickupDelay() == this.getPickupDelay()
                && (orb.getRuntimeId() - this.getRuntimeId()) % 40 == 0
                && orb.getExperience() == this.getExperience();
    }

    public void consumeOne() {
        if (this.isClosed()) {
            return;
        }

        int remaining = this.getCount() - 1;
        if (remaining == 0) {
            this.data().setCount(0);
            this.remove(EntityRemoveCause.PICKUP);
        } else {
            this.setCount(remaining);
        }
    }

    @Override
    public int getExperience() {
        return this.data.require(VALUE);
    }

    @Override
    public void setExperience(int experience) {
        if (experience < 0) {
            throw new IllegalArgumentException("XP amount must be non-negative, got " + experience);
        }
        this.data.set(VALUE, experience);
    }

    @Override
    public int getCount() {
        return this.data().getCount();
    }

    @Override
    public void setCount(int count) {
        if (count < 1) {
            throw new IllegalArgumentException("XP orb count must be greater than 0, got " + count);
        }

        if (this.getCount() != count) {
            this.data().setCount(count);
            this.markChunkDirty();
        }
    }

    @Override
    public int getPickupDelay() {
        return this.data().getPickupDelay();
    }

    @Override
    public void setPickupDelay(int pickupDelay) {
        this.data().setPickupDelay(Math.clamp(pickupDelay, 0, Short.MAX_VALUE));
    }
}
