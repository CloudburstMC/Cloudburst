package org.cloudburstmc.server.entity.projectile;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.projectile.SplashPotion;
import org.cloudburstmc.api.event.potion.PotionCollideEvent;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.potion.PotionType;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.server.level.particle.Particle;
import org.cloudburstmc.server.level.particle.SpellParticle;
import org.cloudburstmc.server.potion.CloudEffect;
import org.cloudburstmc.server.potion.CloudPotion;

import java.util.Set;

import static org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes.AUX_VALUE_DATA;

/**
 * @author xtypr
 */
public class EntitySplashPotion extends EntityProjectile implements SplashPotion {

    private PotionType type;

    public EntitySplashPotion(EntityType<? extends SplashPotion> type, Location location) {
        super(type, location);
    }

    @Override
    protected void initEntity() {
        super.initEntity();

        /*Effect effect = Potion.getEffect(potionId, true); TODO: potion color

        if(effect != null) {
            int count = 0;
            int[] c = effect.getColor();
            count += effect.getAmplifier() + 1;

            int r = ((c[0] * (effect.getAmplifier() + 1)) / count) & 0xff;
            int g = ((c[1] * (effect.getAmplifier() + 1)) / count) & 0xff;
            int b = ((c[2] * (effect.getAmplifier() + 1)) / count) & 0xff;

            this.setDataProperty(new IntEntityData(Entity.DATA_UNKNOWN, (r << 16) + (g << 8) + b));
        }*/
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
        return 0.05f;
    }

    @Override
    public float getDrag() {
        return 0.01f;
    }

    @Override
    public void onCollideWithEntity(Entity entity) {
        this.splash(entity);
    }

    protected void splash(Entity collidedWith) {
        CloudPotion potion = new CloudPotion(this.getPotionType(), true);
        PotionCollideEvent event = new PotionCollideEvent(potion, this);
        this.server.getEventManager().fire(event);

        if (event.isCancelled()) {
            return;
        }

        this.close();

        potion = (CloudPotion) event.getPotion();
        if (potion == null) {
            return;
        }

        potion.setSplash(true);

        Particle particle;
        int r;
        int g;
        int b;

        CloudEffect effect = (CloudEffect) potion.getEffect();

        if (effect == null) {
            r = 40;
            g = 40;
            b = 255;
        } else {
            int[] colors = effect.getColor();
            r = colors[0];
            g = colors[1];
            b = colors[2];
        }

        particle = new SpellParticle(this.getPosition(), r, g, b);

        this.getLevel().addParticle(particle);
        this.getLevel().addLevelSoundEvent(this.getPosition(), SoundEvent.GLASS);

        Set<Entity> entities = this.getLevel().getNearbyEntities(this.getBoundingBox().grow(4.125f, 2.125f, 4.125f));
        for (Entity anEntity : entities) {
            double distance = anEntity.getPosition().distanceSquared(this.getPosition());
            if (distance < 16) {
                double d = anEntity.equals(collidedWith) ? 1 : 1 - Math.sqrt(distance) / 4;
                potion.applyPotion(anEntity, d);
            }
        }
    }

    @Override
    public boolean onUpdate(int currentTick) {
        if (this.closed) {
            return false;
        }

        this.timing.startTiming();

        boolean hasUpdate = super.onUpdate(currentTick);

        if (this.age > 1200) {
            this.kill();
            hasUpdate = true;
        } else if (this.isCollided) {
            this.splash(null);
            hasUpdate = true;
        }
        this.data.update();

        this.timing.stopTiming();
        return hasUpdate;
    }

    public short getPotionId() {
        return this.data.get(AUX_VALUE_DATA);
    }

    public void setPotionId(int potionId) {
        this.data.set(AUX_VALUE_DATA, (short) potionId);
    }

    @Override
    public PotionType getPotionType() {
        return this.type;
    }

    @Override
    public void setPotionType(PotionType type) {
        this.type = type;
    }
}
