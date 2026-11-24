package org.cloudburstmc.server.entity.projectile;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.projectile.LargeFireball;
import org.cloudburstmc.api.event.entity.EntityRemoveCause;
import org.cloudburstmc.api.level.ExplosionBlockInteraction;
import org.cloudburstmc.api.level.ExplosionSettings;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.level.gamerule.GameRules;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.entity.data.CloudLargeFireballData;

public class EntityLargeFireball extends EntityProjectile implements LargeFireball {

    private static final float IMPACT_DAMAGE = 6;
    private static final float DEFAULT_YIELD = 1;

    private final CloudLargeFireballData persistenceState;

    public EntityLargeFireball(EntityType<?> entityType, Location location) {
        CloudLargeFireballData state = new CloudLargeFireballData();
        super(entityType, location, state);
        this.persistenceState = state;
    }

    @Override
    protected CloudLargeFireballData data() {
        return this.persistenceState;
    }

    @Override
    public float getYield() {
        Float yield = this.data().getExplosionYield();
        return yield != null && Float.isFinite(yield) && yield >= 0 ? yield : DEFAULT_YIELD;
    }

    @Override
    public void setYield(float yield) {
        if (!Float.isFinite(yield) || yield < 0) {
            throw new IllegalArgumentException("Explosion radius must be finite and non-negative");
        }

        this.data().setExplosionYield(yield);
        this.markChunkDirty();
    }

    @Override
    public boolean isIncendiary() {
        Boolean incendiary = this.data().getIncendiary();
        return incendiary == null || incendiary;
    }

    @Override
    public void setIsIncendiary(boolean incendiary) {
        this.data().setIncendiary(incendiary);
        this.markChunkDirty();
    }


    @Override
    protected void onCollideWithEntity(Entity target) {
        CloudEntity.applyDamage(target, IMPACT_DAMAGE, this.createProjectileDamageSource());
    }

    @Override
    protected void afterHit() {
        boolean mobGriefing = this.getLevel().getGameRules().get(GameRules.MOB_GRIEFING);
        ExplosionBlockInteraction blockInteraction = mobGriefing ? ExplosionBlockInteraction.DESTROY : ExplosionBlockInteraction.KEEP;
        this.getLevel().explode(this.getPosition(), new ExplosionSettings(this.getYield(), blockInteraction, mobGriefing && this.isIncendiary(), this, null));
        this.remove(EntityRemoveCause.HIT);
    }
}
