package org.cloudburstmc.server.entity.projectile;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.Living;
import org.cloudburstmc.api.entity.damage.DamageSource;
import org.cloudburstmc.api.entity.projectile.SplashPotion;
import org.cloudburstmc.api.event.entity.PotionEffectCause;
import org.cloudburstmc.api.event.entity.PotionSplashEvent;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.potion.PotionType;
import org.cloudburstmc.api.potion.PotionTypes;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.protocol.bedrock.data.LevelEvent;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.server.level.particle.GenericParticle;
import org.cloudburstmc.server.network.NetworkUtils;
import org.cloudburstmc.server.potion.CloudPotion;
import org.cloudburstmc.server.potion.CloudPotionColor;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static java.util.Objects.requireNonNull;
import static org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes.AUX_VALUE_DATA;
import static org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag.ENCHANTED;

public class EntitySplashPotion extends EntityProjectile implements SplashPotion {

    private static final String TAG_POTION_ID = "PotionId";

    private PotionType type = PotionTypes.WATER;

    public EntitySplashPotion(EntityType<? extends SplashPotion> type, Location location) {
        super(type, location);
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.setPotionType(PotionTypes.WATER);
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
    protected void onCollideWithEntity(Entity entity) {
        this.splash(entity);
    }

    protected void splash(Entity collidedWith) {
        this.close();
        int color = CloudPotionColor.calculateEffects(this.type.getEffects()).orElse(0x2828ff);
        this.getLevel().addParticle(new GenericParticle(this.getPosition(), LevelEvent.PARTICLE_POTION_SPLASH,
                0xff000000 | color));
        this.getLevel().addLevelSoundEvent(this.getPosition(), SoundEvent.GLASS);

        this.applyPotionImpact(collidedWith);
    }

    protected void applyPotionImpact(Entity collidedWith) {
        Map<Living, Double> affectedEntities = new LinkedHashMap<>();
        DamageSource damageSource = this.createProjectileDamageSource();
        Set<Entity> entities = this.getLevel().getNearbyEntities(this.getBoundingBox().inflate(4.125f, 2.125f, 4.125f));
        for (Entity anEntity : entities) {
            double distance = anEntity.getPosition().distanceSquared(this.getPosition());
            if (anEntity instanceof Living living && distance < 16) {
                double d = anEntity.equals(collidedWith) ? 1 : 1 - Math.sqrt(distance) / 4;
                affectedEntities.put(living, d);
            }
        }

        PotionSplashEvent event = new PotionSplashEvent(this, affectedEntities);
        this.server.getEventManager().fire(event);
        if (event.isCancelled()) {
            return;
        }

        CloudPotion potion = new CloudPotion(this.type);
        for (Living living : event.getAffectedEntities()) {
            potion.apply(living, event.getIntensity(living), damageSource, this, PotionEffectCause.POTION_SPLASH);
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
        this.timing.stopTiming();
        return hasUpdate;
    }

    @Override
    public PotionType getPotionType() {
        return this.type;
    }

    @Override
    public void setPotionType(PotionType type) {
        this.type = requireNonNull(type, "type");
        this.data.set(AUX_VALUE_DATA, NetworkUtils.potionToNetwork(type));
        this.data.setFlag(ENCHANTED, type != PotionTypes.WATER && type != PotionTypes.MUNDANE
                && type != PotionTypes.THICK && type != PotionTypes.AWKWARD);
    }

    @Override
    public void loadAdditionalData(NbtMap tag) {
        super.loadAdditionalData(tag);
        tag.listenForShort(TAG_POTION_ID, id -> this.setPotionType(NetworkUtils.potionFromNetwork(id)));
    }

    @Override
    public void saveAdditionalData(NbtMapBuilder tag) {
        super.saveAdditionalData(tag);
        tag.putShort(TAG_POTION_ID, NetworkUtils.potionToNetwork(this.type));
    }
}
