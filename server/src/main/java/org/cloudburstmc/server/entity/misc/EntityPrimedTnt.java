package org.cloudburstmc.server.entity.misc;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.damage.DamageTypes;
import org.cloudburstmc.api.entity.misc.PrimedTnt;
import org.cloudburstmc.api.event.entity.EntityRemoveCause;
import org.cloudburstmc.api.level.ExplosionBlockInteraction;
import org.cloudburstmc.api.level.ExplosionSettings;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.level.gamerule.GameRules;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.entity.data.CloudPrimedTntData;
import org.cloudburstmc.server.event.entity.CloudEntityDamageEvent;

import static org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes.FUSE_TIME;
import static org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag.IGNITED;

public class EntityPrimedTnt extends CloudEntity implements PrimedTnt {

    private static final float DEFAULT_YIELD = 4;

    private final CloudPrimedTntData persistenceState;

    public EntityPrimedTnt(EntityType<PrimedTnt> type, Location location) {
        CloudPrimedTntData state = new CloudPrimedTntData();
        super(type, location, state);
        this.persistenceState = state;
    }

    @Override
    public boolean blocksBuilding() {
        return true;
    }

    @Override
    public float getWidth() {
        return 0.98f;
    }

    @Override
    public float getLength() {
        return 0.98f;
    }

    @Override
    public float getHeight() {
        return 0.98f;
    }

    @Override
    public float getGravity() {
        return 0.04f;
    }

    @Override
    public float getDrag() {
        return 0.02f;
    }

    @Override
    public float getBaseOffset() {
        return 0.49f;
    }

    @Override
    public boolean canCollide() {
        return false;
    }

    @Override
    protected boolean applyDamage(CloudEntityDamageEvent event) {
        return event.getDamageType() == DamageTypes.OUT_OF_WORLD && super.applyDamage(event);
    }

    protected void initEntity() {
        super.initEntity();
        this.data.set(FUSE_TIME, -1);
    }

    @Override
    protected CloudPrimedTntData data() {
        return this.persistenceState;
    }

    @Override
    protected void loadCustom(NbtMap tag) {
        if (tag.containsKey("Fuse")) {
            this.setFuseTicks(this.data().getFuseTicks());
        }
        super.loadCustom(tag);
    }

    public boolean canCollideWith(Entity entity) {
        return false;
    }

    protected boolean tickEntity(int currentTick, int elapsedTicks) {
        if (this.isClosed()) {
            return false;
        }

        boolean hasUpdate = this.entityBaseTick(elapsedTicks);

        if (isAlive()) {
            this.motion = this.motion.sub(0, this.getGravity(), 0);
            move(this.motion);

            float friction = 1 - getDrag();
            this.motion = this.motion.mul(friction);

            updateMovement();

            if (this.isOnGround()) {
                this.motion = this.motion.mul(0.7, -0.5, 0.7);
            }

            this.data.setFlag(IGNITED, true);
            this.setFuseTicks(Math.max(0, this.getFuseTicks() - elapsedTicks));
            this.flushEntityData();
            if (this.getFuseTicks() == 0) {
                this.detonate();
                this.remove(EntityRemoveCause.EXPLODE);
            }
        }

        return !this.isClosed() && (hasUpdate || this.getFuseTicks() > 0 || this.motion.length() > 0.00001);
    }

    private void detonate() {
        if (!this.level.getGameRules().get(GameRules.TNT_EXPLODES)) {
            return;
        }

        ExplosionBlockInteraction interaction = this.level.getGameRules().get(GameRules.TNT_EXPLOSION_DROP_DECAY)
                ? ExplosionBlockInteraction.DESTROY_WITH_DECAY : ExplosionBlockInteraction.DESTROY;
        this.getLevel().explode(this.getPosition().add(0, this.getHeight() * 0.0625f, 0),
                new ExplosionSettings(this.getYield(), interaction, this.isIncendiary(), this, null));
    }

    @Override
    public int getFuseTicks() {
        return this.data().getFuseTicks();
    }

    @Override
    public void setFuseTicks(int fuseTicks) {
        if (fuseTicks < 0) {
            throw new IllegalArgumentException("Fuse must be non-negative");
        }

        this.data().setFuseTicks(fuseTicks);
        if (this.data.getFlag(IGNITED)) {
            this.data.set(FUSE_TIME, fuseTicks);
        }
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
        return incendiary == null ? false : incendiary;
    }

    @Override
    public void setIsIncendiary(boolean incendiary) {
        this.data().setIncendiary(incendiary);
        this.markChunkDirty();
    }

}
