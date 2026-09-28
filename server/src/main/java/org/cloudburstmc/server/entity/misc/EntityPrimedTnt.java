package org.cloudburstmc.server.entity.misc;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.Explosive;
import org.cloudburstmc.api.entity.damage.DamageTypes;
import org.cloudburstmc.api.entity.misc.PrimedTnt;
import org.cloudburstmc.api.level.ExplosionBlockInteraction;
import org.cloudburstmc.api.level.ExplosionSettings;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.level.gamerule.GameRules;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.event.entity.CloudEntityDamageEvent;

import static org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes.FUSE_TIME;
import static org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag.IGNITED;

public class EntityPrimedTnt extends CloudEntity implements PrimedTnt, Explosive {

    private int fuse = 80;

    public EntityPrimedTnt(EntityType<PrimedTnt> type, Location location) {
        super(type, location);
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
    protected boolean applyDamage(CloudEntityDamageEvent source) {
        return source.getDamageType() == DamageTypes.OUT_OF_WORLD && super.applyDamage(source);
    }

    protected void initEntity() {
        super.initEntity();
        this.data.set(FUSE_TIME, -1);
    }

    @Override
    public void loadAdditionalData(NbtMap tag) {
        super.loadAdditionalData(tag);

        tag.listenForInt("Fuse", this::setFuse);
    }

    @Override
    public void saveAdditionalData(NbtMapBuilder tag) {
        super.saveAdditionalData(tag);

        tag.putInt("Fuse", this.getFuse());
    }

    public boolean canCollideWith(Entity entity) {
        return false;
    }

    public boolean onUpdate(int currentTick) {
        if (closed) {
            return false;
        }

        this.timing.startTiming();
        int tickDiff = currentTick - lastUpdate;
        if (tickDiff <= 0 && !justCreated) {
            return true;
        }

        lastUpdate = currentTick;
        boolean hasUpdate = entityBaseTick(tickDiff);

        if (isAlive()) {
            this.motion = this.motion.sub(0, this.getGravity(), 0);
            move(this.motion);

            float friction = 1 - getDrag();
            this.motion = this.motion.mul(friction);

            updateMovement();

            if (onGround) {
                this.motion = this.motion.mul(0.7, -0.5, 0.7);
            }

            this.data.setFlag(IGNITED, true);
            this.setFuse(Math.max(0, this.fuse - tickDiff));
            this.flushEntityData();
            if (this.fuse == 0) {
                this.explode();
                this.close();
            }
        }

        this.timing.stopTiming();

        return !this.closed && (hasUpdate || this.fuse > 0 || this.motion.length() > 0.00001);
    }

    public void explode() {
        if (!this.level.getGameRules().get(GameRules.TNT_EXPLODES)) {
            return;
        }

        ExplosionBlockInteraction interaction = this.level.getGameRules().get(GameRules.TNT_EXPLOSION_DROP_DECAY)
                ? ExplosionBlockInteraction.DESTROY_WITH_DECAY : ExplosionBlockInteraction.DESTROY;
        this.getLevel().explode(this.getPosition().add(0, this.getHeight() * 0.0625f, 0),
                new ExplosionSettings(4, interaction, false, this, null));
    }

    @Override
    public int getFuse() {
        return this.fuse;
    }

    @Override
    public void setFuse(int fuse) {
        if (fuse < 0) {
            throw new IllegalArgumentException("Fuse must be non-negative");
        }

        this.fuse = fuse;
        if (this.data.getFlag(IGNITED)) {
            this.data.set(FUSE_TIME, fuse);
        }
    }
}
