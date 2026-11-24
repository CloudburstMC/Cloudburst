package org.cloudburstmc.server.entity.projectile;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.projectile.ShulkerBullet;
import org.cloudburstmc.api.event.entity.EntityRemoveCause;
import org.cloudburstmc.api.event.entity.PotionEffectCause;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.level.sound.SoundPlayback;
import org.cloudburstmc.api.level.sound.SoundTypes;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.potion.EffectTypes;
import org.cloudburstmc.api.potion.PotionEffect;
import org.cloudburstmc.api.util.BlockHitResult;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.entity.CloudLivingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class EntityShulkerBullet extends EntityProjectile implements ShulkerBullet {

    private static final int LEVITATION_TICKS = 200;
    private static final SoundPlayback BLOCK_HIT_SOUND = new SoundPlayback(SoundTypes.MOB_SHULKER_BULLET_HIT, 1f, 1f);
    private static final List<Direction> FLIGHT_DIRECTIONS = List.of(Direction.values());

    private @Nullable Entity target;
    private @Nullable Direction flightDirection;
    private int turnTicks;
    private float targetSpeed;

    public EntityShulkerBullet(EntityType<ShulkerBullet> type, Location location) {
        super(type, location);
    }

    @Override
    public @Nullable Entity getTarget() {
        Entity current = this.target;
        if (current != null && this.isInvalidTarget(current)) {
            this.setTarget(null);
        }
        return this.target;
    }

    @Override
    public void setTarget(@Nullable Entity target) {
        if (target != null && this.isInvalidTarget(target)) {
            throw new IllegalArgumentException("Target must be a live entity in the same level");
        }

        this.target = target;
        this.flightDirection = null;
        this.turnTicks = 0;
        this.targetSpeed = 0.15f;
    }

    private boolean isInvalidTarget(Entity target) {
        return target == this || !target.isAlive() || target.isClosed() || target.getLevel() != this.getLevel()
                || (target instanceof Player player && player.isSpectator());
    }

    @Override
    public float getGravity() {
        return this.getTarget() == null ? 0.04f : 0f;
    }

    @Override
    public float getDrag() {
        return 0;
    }

    @Override
    protected boolean tickEntity(int currentTick, int elapsedTicks) {
        Entity currentTarget = this.getTarget();
        if (this.isAlive() && currentTarget != null && elapsedTicks > 0) {
            this.steerTowards(currentTarget, elapsedTicks);
        }
        return super.tickEntity(currentTick, elapsedTicks);
    }

    private void steerTowards(Entity target, int elapsedTicks) {
        Vector3f center = target.getPosition().add(0, target.getHeight() * 0.5f, 0);
        Vector3f delta = center.sub(this.getPosition());
        Vector3f direction = delta.lengthSquared() > 0 ? delta.normalize() : Vector3f.ZERO;
        this.turnTicks = Math.max(0, this.turnTicks - elapsedTicks);

        if (delta.lengthSquared() >= 4) {
            Vector3i position = this.getPosition().toInt();
            Vector3i destination = center.toInt();
            Direction previous = this.flightDirection;
            if (this.turnTicks == 0 || previous != null && (axisDistance(position, destination, previous.getAxis()) == 0
                    || this.isFlightBlockedAt(previous.relative(position)))) {
                this.flightDirection = this.chooseDirection(position, destination, previous);
                this.turnTicks = 10 * ThreadLocalRandom.current().nextInt(1, 6);
                this.targetSpeed = 0.15f;
            }

            if (this.flightDirection != null) {
                direction = this.flightDirection.relative(Vector3i.ZERO).toFloat();
            }
        } else {
            this.flightDirection = null;
        }

        this.targetSpeed = Math.min(1f, this.targetSpeed * 1.025f);
        Vector3f desiredMotion = direction.mul(this.targetSpeed);
        this.motion = this.motion.add(desiredMotion.sub(this.motion).mul(0.2f));
    }

    private @Nullable Direction chooseDirection(Vector3i position, Vector3i destination, @Nullable Direction previous) {
        List<Direction> available = new ArrayList<>(6);
        List<Direction> towardTarget = new ArrayList<>(3);
        for (Direction direction : FLIGHT_DIRECTIONS) {
            Vector3i next = direction.relative(position);
            if (this.isFlightBlockedAt(next)) {
                continue;
            }
            available.add(direction);
            if ((previous == null || direction.getAxis() != previous.getAxis())
                    && axisDistance(next, destination, direction.getAxis()) < axisDistance(position, destination, direction.getAxis())) {
                towardTarget.add(direction);
            }
        }

        List<Direction> options = towardTarget.isEmpty() ? available : towardTarget;
        return options.isEmpty() ? null : options.get(ThreadLocalRandom.current().nextInt(options.size()));
    }

    private static long axisDistance(Vector3i a, Vector3i b, Direction.Axis axis) {
        return Math.abs(switch (axis) {
            case X -> (long) a.getX() - b.getX();
            case Y -> (long) a.getY() - b.getY();
            case Z -> (long) a.getZ() - b.getZ();
        });
    }

    private boolean isFlightBlockedAt(Vector3i position) {
        Block block = this.getLevel().getLoadedBlock(position);
        return block == null || !block.getState().getCollisionShape().isEmpty();
    }

    @Override
    protected float getBaseDamage() {
        return 4f;
    }

    @Override
    public int getResultDamage() {
        return (int) Math.ceil(this.getDamage());
    }

    @Override
    protected void onCollideWithEntity(Entity target) {
        if (this.damageEntity(target) && target instanceof CloudLivingEntity living && living.isAlive()) {
            Entity owner = this.getOwner();
            living.addPotionEffect(new PotionEffect(EffectTypes.LEVITATION, LEVITATION_TICKS, 0),
                    owner == null ? this : owner, PotionEffectCause.ATTACK);
        }
        this.remove(EntityRemoveCause.HIT);
    }

    @Override
    protected void onBlockCollision(BlockHitResult hit) {
        this.getLevel().playSound(hit.position(), BLOCK_HIT_SOUND);
    }

    @Override
    protected void afterHit() {
        this.remove(EntityRemoveCause.HIT);
    }
}
