package org.cloudburstmc.server.entity;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.LivingEntity;
import org.cloudburstmc.api.entity.Mob;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.server.entity.data.CloudMobData;

/**
 * Base for non-human living creatures with a transient behavior target.
 */
public abstract class CloudMob extends CloudEquippedLivingEntity implements Mob {

    private @Nullable CloudLivingEntity target;

    public CloudMob(EntityType<?> type, Location location) {
        this(type, location, new CloudMobData());
    }

    protected CloudMob(EntityType<?> type, Location location, CloudMobData state) {
        super(type, location, state);
    }

    @Override
    protected CloudMobData data() {
        return (CloudMobData) super.data();
    }

    @Override
    public final @Nullable LivingEntity getTarget() {
        this.clearInvalidTarget();
        return this.target;
    }

    @Override
    public final void setTarget(@Nullable LivingEntity target) {
        if (target == null) {
            this.target = null;
            return;
        }

        if (!(target instanceof CloudLivingEntity living) || this.isInvalidTarget(living)) {
            throw new IllegalArgumentException("Target must be an eligible living entity in the same level");
        }

        this.target = living;
    }

    @Override
    public boolean entityBaseTick(int tickDiff) {
        boolean updated = super.entityBaseTick(tickDiff);
        this.clearInvalidTarget();
        return updated;
    }

    private void clearInvalidTarget() {
        if (this.isInvalidTarget(this.target)) {
            this.target = null;
        }
    }

    private boolean isInvalidTarget(@Nullable CloudLivingEntity target) {
        return target == null || target == this || this.isClosed() || !this.isAlive()
                || target.isClosed() || !target.isAlive() || target.getLevel() != this.getLevel()
                || (target instanceof Player player && (player.isCreative() || player.isSpectator()));
    }
}
