package org.cloudburstmc.server.entity.misc;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.Explosive;
import org.cloudburstmc.api.entity.misc.PrimedTnt;
import org.cloudburstmc.api.event.entity.EntityDamageEvent;
import org.cloudburstmc.api.event.entity.EntityExplosionPrimeEvent;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.level.gamerule.GameRules;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.server.entity.BaseEntity;
import org.cloudburstmc.server.level.Explosion;

import static org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes.FUSE_TIME;
import static org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag.IGNITED;

/**
 * @author MagicDroidX
 */
public class EntityPrimedTnt extends BaseEntity implements PrimedTnt, Explosive {

    protected int fuse = 80;

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
    protected float getBaseOffset() {
        return 0.49f;
    }

    @Override
    public boolean canCollide() {
        return false;
    }

    @Override
    public boolean attack(EntityDamageEvent source) {
        return source.getCause() == EntityDamageEvent.DamageCause.VOID && super.attack(source);
    }

    protected void initEntity() {
        super.initEntity();

        this.data.setFlag(IGNITED, true);
        this.data.set(FUSE_TIME, fuse);

        this.getLevel().addLevelSoundEvent(this.getPosition(), SoundEvent.FIZZ);
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

        if (fuse <= 5 || fuse % 5 == 0) {
            this.data.set(FUSE_TIME, fuse);

            this.data.update();
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
                this.motion = this.motion.mul(-0.5, 0.7, 0.7);
            }

            fuse -= tickDiff;

            if (fuse <= 0) {
                if (this.level.getGameRules().get(GameRules.TNT_EXPLODES))
                    explode();
                kill();
            }

        }

        this.timing.stopTiming();

        return hasUpdate || fuse >= 0 || this.motion.length() > 0.00001;
    }

    public void explode() {
        EntityExplosionPrimeEvent event = new EntityExplosionPrimeEvent(this, 4);
        server.getEventManager().fire(event);
        if (event.isCancelled()) {
            return;
        }
        Explosion explosion = new Explosion(this.getLevel(), this.getPosition(), event.getForce(), this);
        if (event.isBlockBreaking()) {
            explosion.explodeA();
        }
        explosion.explodeB();
    }

    @Override
    public int getFuse() {
        return this.data.get(FUSE_TIME);
    }

    @Override
    public void setFuse(int fuse) {
        this.data.set(FUSE_TIME, fuse);
    }
}
