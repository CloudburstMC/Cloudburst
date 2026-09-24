package org.cloudburstmc.server.entity.misc;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.Living;
import org.cloudburstmc.api.entity.damage.DamageSource;
import org.cloudburstmc.api.entity.damage.DamageTypes;
import org.cloudburstmc.api.entity.misc.AreaEffectCloud;
import org.cloudburstmc.api.event.entity.AreaEffectCloudApplyEvent;
import org.cloudburstmc.api.event.entity.EntityDamageEvent;
import org.cloudburstmc.api.event.entity.EntityRegainHealthEvent;
import org.cloudburstmc.api.event.entity.PotionEffectCause;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.level.particle.ParticleType;
import org.cloudburstmc.api.level.particle.ParticleTypes;
import org.cloudburstmc.api.potion.*;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.entity.EntityLiving;
import org.cloudburstmc.server.network.NetworkUtils;
import org.cloudburstmc.server.potion.CloudPotionColor;
import org.cloudburstmc.server.potion.PotionEffectDataSerializer;

import java.util.*;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;
import static org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes.*;
import static org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag.FIRE_IMMUNE;
import static org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag.NO_AI;

public class EntityAreaEffectCloud extends CloudEntity implements AreaEffectCloud {
    private static final String TAG_DURATION = "Duration";
    private static final String TAG_REAPPLICATION_DELAY = "ReapplicationDelay";
    private static final String TAG_DURATION_ON_USE = "DurationOnUse";
    private static final String TAG_RADIUS_ON_USE = "RadiusOnUse";
    private static final String TAG_RADIUS_PER_TICK = "RadiusPerTick";
    private static final String TAG_POTION_ID = "PotionId";
    private static final String TAG_RADIUS = "Radius";
    private static final String TAG_MOB_EFFECTS = "mobEffects";
    private static final String TAG_PARTICLE_COLOR = "ParticleColor";
    private static final String TAG_WAIT_TIME = "WaitTime";
    private static final String TAG_POTION_DURATION_SCALE = "PotionDurationScale";

    private final List<PotionEffect> customEffects = new ArrayList<>();
    private final Map<Long, Integer> victims = new HashMap<>();
    private int reapplicationDelay;
    private int durationOnUse;
    private float radiusOnUse;
    private int nextApply;
    private int particleColor;
    private boolean particleColorSet;
    private int lastAge;
    private int duration = 600;
    private float radiusPerTick;
    private float potionDurationScale = 1.0F;
    private PotionType potionType = PotionTypes.WATER;

    public EntityAreaEffectCloud(EntityType<?> type, Location location) {
        super(type, location);
    }

    @Override
    public int getWaitTime() {
        return this.data.get(AREA_EFFECT_CLOUD_WAITING);
    }

    @Override
    public void setWaitTime(int waitTime) {
        checkArgument(waitTime >= 0, "waitTime cannot be negative");
        this.data.set(AREA_EFFECT_CLOUD_WAITING, waitTime);
    }

    @Override
    public PotionType getPotionType() {
        return this.potionType;
    }

    @Override
    public void setPotionType(PotionType potionType) {
        this.potionType = requireNonNull(potionType, "potionType");
        this.data.set(AUX_VALUE_DATA, NetworkUtils.potionToNetwork(potionType));
        this.recalculatePotionColor();
    }

    private void recalculatePotionColor() {
        int a;
        int r;
        int g;
        int b;

        int color;
        if (this.particleColorSet) {
            color = this.particleColor;
            a = (color & 0xFF000000) >> 24;
            r = (color & 0x00FF0000) >> 16;
            g = (color & 0x0000FF00) >> 8;
            b = color & 0x000000FF;
        } else {
            a = 255;
            color = CloudPotionColor.calculateEffects(this.getApplicationEffects()).orElse(0x2828ff);
            r = color >> 16 & 0xff;
            g = color >> 8 & 0xff;
            b = color & 0xff;
        }

        this.updatePotionColor(((a & 0xff) << 24) | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff));
    }

    @Override
    public int getPotionColor() {
        return this.data.get(EFFECT_COLOR);
    }

    @Override
    public void setPotionColor(int argb) {
        this.particleColor = argb;
        this.particleColorSet = true;
        this.updatePotionColor(argb);
    }

    @Override
    public void setPotionColor(int alpha, int red, int green, int blue) {
        checkColorComponent(alpha, "alpha");
        checkColorComponent(red, "red");
        checkColorComponent(green, "green");
        checkColorComponent(blue, "blue");
        setPotionColor(((alpha & 0xff) << 24) | ((red & 0xff) << 16) | ((green & 0xff) << 8) | (blue & 0xff));
    }

    private void updatePotionColor(int argb) {
        this.data.set(EFFECT_COLOR, argb);
    }

    @Override
    public int getReapplicationDelay() {
        return this.reapplicationDelay;
    }

    @Override
    public void setReapplicationDelay(int reapplicationDelay) {
        checkArgument(reapplicationDelay >= 0, "reapplicationDelay cannot be negative");
        this.reapplicationDelay = reapplicationDelay;
    }

    @Override
    public int getDurationOnUse() {
        return this.durationOnUse;
    }

    @Override
    public void setDurationOnUse(int durationOnUse) {
        this.durationOnUse = durationOnUse;
    }

    @Override
    public float getRadiusOnUse() {
        return this.radiusOnUse;
    }

    @Override
    public void setRadiusOnUse(float radiusOnUse) {
        checkArgument(Float.isFinite(radiusOnUse), "radiusOnUse must be finite");
        this.radiusOnUse = radiusOnUse;
    }

    @Override
    public float getRadiusPerTick() {
        return this.radiusPerTick;
    }

    @Override
    public void setRadiusPerTick(float radiusPerTick) {
        checkArgument(Float.isFinite(radiusPerTick), "radiusPerTick must be finite");
        this.radiusPerTick = radiusPerTick;
    }

    @Override
    public int getDuration() {
        return this.duration;
    }

    @Override
    public void setDuration(int duration) {
        checkArgument(duration == PotionEffect.INFINITE_DURATION || duration >= 0,
                "duration must be non-negative or PotionEffect.INFINITE_DURATION");
        this.duration = duration;
    }

    public void setPotionDurationScale(float potionDurationScale) {
        checkArgument(Float.isFinite(potionDurationScale) && potionDurationScale >= 0.0F,
                "potionDurationScale must be finite and non-negative");
        this.potionDurationScale = potionDurationScale;
    }

    @Override
    public float getRadius() {
        return this.data.get(AREA_EFFECT_CLOUD_RADIUS);
    }

    @Override
    public void setRadius(float radius) {
        checkArgument(Float.isFinite(radius), "radius must be finite");
        this.data.set(AREA_EFFECT_CLOUD_RADIUS, Math.clamp(radius, 0.0F, 32.0F));
        if (this.boundingBox != null) {
            this.recalculateBoundingBox();
        }
    }

    @Override
    public ParticleType getParticle() {
        return NetworkUtils.particleFromNetwork(this.data.get(AREA_EFFECT_CLOUD_PARTICLE));
    }

    @Override
    public void setParticle(ParticleType particle) {
        this.data.set(AREA_EFFECT_CLOUD_PARTICLE, NetworkUtils.particleToNetwork(requireNonNull(particle, "particle")));
    }

    @Override
    public List<PotionEffect> getCustomEffects() {
        return List.copyOf(this.customEffects);
    }

    @Override
    public boolean hasCustomEffects() {
        return !this.customEffects.isEmpty();
    }

    @Override
    public boolean hasCustomEffect(EffectType type) {
        requireNonNull(type, "type");
        return this.customEffects.stream().anyMatch(effect -> effect.getType() == type);
    }

    @Override
    public boolean addCustomEffect(PotionEffect effect, boolean overwrite) {
        requireNonNull(effect, "effect");
        for (int index = 0; index < this.customEffects.size(); index++) {
            if (this.customEffects.get(index).getType() == effect.getType()) {
                if (!overwrite) {
                    return false;
                }

                this.customEffects.set(index, effect);
                this.recalculatePotionColor();
                return true;
            }
        }

        this.customEffects.add(effect);
        this.recalculatePotionColor();
        return true;
    }

    @Override
    public boolean removeCustomEffect(EffectType type) {
        requireNonNull(type, "type");
        boolean changed = this.customEffects.removeIf(effect -> effect.getType() == type);
        if (changed) {
            this.recalculatePotionColor();
        }

        return changed;
    }

    @Override
    public boolean clearCustomEffects() {
        if (this.customEffects.isEmpty()) {
            return false;
        }

        this.customEffects.clear();
        this.recalculatePotionColor();
        return true;
    }

    @Override
    protected void initEntity() {
        this.data.set(AREA_EFFECT_CLOUD_RADIUS, 3.0F);
        super.initEntity();
        this.invulnerable = true;
        this.data.setFlag(FIRE_IMMUNE, true);
        this.data.setFlag(NO_AI, true);
        this.data.set(AREA_EFFECT_CLOUD_DURATION, Integer.MAX_VALUE);
        this.data.set(AREA_EFFECT_CLOUD_CHANGE_RATE, Float.MIN_VALUE);
        this.data.set(AREA_EFFECT_CLOUD_CHANGE_ON_PICKUP, Float.MIN_VALUE);
        this.setParticle(ParticleTypes.MOB_SPELL_AMBIENT);
        this.data.set(AREA_EFFECT_CLOUD_PICKUP_COUNT, 0);
        this.setPotionType(PotionTypes.WATER);
        this.setDuration(600);
        this.setReapplicationDelay(20);
        this.setDurationOnUse(0);
        this.setRadiusOnUse(-0.5F);
        this.setRadiusPerTick(-0.005F);
        this.setWaitTime(10);
        this.setMaxHealth(1);
        this.setHealth(1);
    }

    @Override
    public void loadAdditionalData(NbtMap tag) {
        super.loadAdditionalData(tag);

        tag.listenForList(TAG_MOB_EFFECTS, NbtType.COMPOUND, effectTags -> {
            for (NbtMap effectTag : effectTags) {
                this.customEffects.add(PotionEffectDataSerializer.deserialize(effectTag));
            }
        });

        tag.listenForShort(TAG_POTION_ID, potionId -> this.setPotionType(NetworkUtils.potionFromNetwork(potionId)));
        tag.listenForInt(TAG_PARTICLE_COLOR, color -> {
            this.particleColor = color;
            this.particleColorSet = true;
            this.updatePotionColor(color);
        });
        tag.listenForInt(TAG_DURATION, this::setDuration);
        tag.listenForInt(TAG_DURATION_ON_USE, this::setDurationOnUse);
        tag.listenForInt(TAG_REAPPLICATION_DELAY, this::setReapplicationDelay);
        tag.listenForFloat(TAG_RADIUS, this::setRadius);
        tag.listenForFloat(TAG_RADIUS_ON_USE, this::setRadiusOnUse);
        tag.listenForFloat(TAG_RADIUS_PER_TICK, this::setRadiusPerTick);
        tag.listenForInt(TAG_WAIT_TIME, this::setWaitTime);
        tag.listenForFloat(TAG_POTION_DURATION_SCALE, this::setPotionDurationScale);
    }

    @Override
    public void saveAdditionalData(NbtMapBuilder tag) {
        super.saveAdditionalData(tag);

        List<NbtMap> effects = new ArrayList<>();
        for (PotionEffect effect : this.customEffects) {
            effects.add(PotionEffectDataSerializer.serialize(effect));
        }
        tag.putList(TAG_MOB_EFFECTS, NbtType.COMPOUND, effects);
        tag.putInt(TAG_PARTICLE_COLOR, getPotionColor());
        tag.putShort(TAG_POTION_ID, NetworkUtils.potionToNetwork(getPotionType()));
        tag.putInt(TAG_DURATION, getDuration());
        tag.putInt(TAG_DURATION_ON_USE, durationOnUse);
        tag.putInt(TAG_REAPPLICATION_DELAY, reapplicationDelay);
        tag.putFloat(TAG_RADIUS, getRadius());
        tag.putFloat(TAG_RADIUS_ON_USE, radiusOnUse);
        tag.putFloat(TAG_RADIUS_PER_TICK, getRadiusPerTick());
        tag.putInt(TAG_WAIT_TIME, getWaitTime());
        if (this.potionDurationScale != 1.0F) {
            tag.putFloat(TAG_POTION_DURATION_SCALE, this.potionDurationScale);
        }
    }

    @Override
    protected boolean applyDamage(EntityDamageEvent source) {
        return false;
    }

    @Override
    public boolean onUpdate(int currentTick) {
        if (this.closed) {
            return false;
        }

        this.timing.startTiming();

        super.onUpdate(currentTick);

        int age = this.age;
        float radius = getRadius();
        int waitTime = getWaitTime();
        if (age < waitTime) {
            this.lastAge = age;
        } else if (getDuration() != PotionEffect.INFINITE_DURATION && age - waitTime >= getDuration()) {
            kill();
            this.lastAge = age;
            this.timing.stopTiming();
            return true;
        } else {
            int tickDiff = age - lastAge;
            radius += getRadiusPerTick() * tickDiff;
            if (age >= this.nextApply) {
                this.nextApply = age + 5;
                this.victims.entrySet().removeIf(entry -> age >= entry.getValue());

                List<PotionEffect> applicationEffects = this.getApplicationEffects();
                if (applicationEffects.isEmpty()) {
                    this.victims.clear();
                }
                Set<Entity> collidingEntities = applicationEffects.isEmpty()
                        ? Set.of()
                        : level.getCollidingEntities(this, getBoundingBox());
                if (!collidingEntities.isEmpty()) {
                    List<Living> affectedEntities = new ArrayList<>();
                    for (Entity collidingEntity : collidingEntities) {
                        if (collidingEntity instanceof Living living
                                && !this.victims.containsKey(collidingEntity.getUniqueId())
                                && isWithinRadius(collidingEntity, radius)) {
                            affectedEntities.add(living);
                        }
                    }

                    AreaEffectCloudApplyEvent event = new AreaEffectCloudApplyEvent(this, affectedEntities);
                    this.server.getEventManager().fire(event);
                    if (!event.isCancelled() && !event.getAffectedEntities().isEmpty()) {
                        for (Living affectedEntity : event.getAffectedEntities()) {
                            this.victims.put(affectedEntity.getUniqueId(), age + this.reapplicationDelay);
                            for (PotionEffect effect : applicationEffects) {
                                if (effect.getType() == EffectTypes.INSTANT_HEALTH
                                        || effect.getType() == EffectTypes.INSTANT_DAMAGE) {
                                    boolean damage = effect.getType() == EffectTypes.INSTANT_DAMAGE;
                                    if (affectedEntity.isUndead()) {
                                        damage = !damage;
                                    }

                                    if (damage) {
                                        DamageSource.Builder sourceBuilder = DamageSource.builder(DamageTypes.INDIRECT_MAGIC)
                                                .directEntity(this);
                                        Entity owner = this.getOwner();
                                        if (owner != null) {
                                            sourceBuilder.causingEntity(owner);
                                        }

                                        DamageSource source = sourceBuilder.build();
                                        affectedEntity.damage(
                                                (float) (0.5 * (double) (6 << effect.getAmplifier())), source);
                                    } else {
                                        affectedEntity.heal(new EntityRegainHealthEvent(affectedEntity,
                                                (float) (0.5 * (double) (4 << effect.getAmplifier())),
                                                EntityRegainHealthEvent.CAUSE_MAGIC));
                                    }

                                    continue;
                                }

                                ((CloudEntity) affectedEntity).addPotionEffect(
                                        effect, this, PotionEffectCause.AREA_EFFECT_CLOUD);
                            }

                            if (this.radiusOnUse != 0) {
                                radius += this.radiusOnUse;
                                if (radius < 0.5F) {
                                    this.setRadius(radius);
                                    this.kill();
                                    this.timing.stopTiming();
                                    return true;
                                }
                            }
                            if (this.durationOnUse != 0 && this.getDuration() != PotionEffect.INFINITE_DURATION) {
                                int duration = this.getDuration() + this.durationOnUse;
                                if (duration <= 0) {
                                    this.kill();
                                    this.timing.stopTiming();
                                    return true;
                                }
                                this.setDuration(duration);
                            }
                        }
                    }
                }
            }
        }

        this.lastAge = age;

        if (radius < 0.5F && age >= waitTime) {
            setRadius(radius);
            kill();
        } else {
            setRadius(radius);
        }

        this.timing.stopTiming();

        return true;
    }

    private List<PotionEffect> getApplicationEffects() {
        List<PotionEffect> effects = new ArrayList<>(this.potionType.getEffects().size() + this.customEffects.size());
        this.potionType.getEffects().forEach(effect -> effects.add(this.scaleDuration(effect)));
        this.customEffects.forEach(effect -> effects.add(this.scaleDuration(effect)));
        return effects;
    }

    private PotionEffect scaleDuration(PotionEffect effect) {
        if (!effect.isInfinite() && effect.getDuration() != 0) {
            int duration = Math.max((int) Math.floor(effect.getDuration() * this.potionDurationScale), 1);
            return effect.withDuration(duration);
        }
        return effect;
    }

    private boolean isWithinRadius(Entity entity, float radius) {
        double x = entity.getX() - this.getX();
        double z = entity.getZ() - this.getZ();
        return x * x + z * z <= radius * radius;
    }

    private static void checkColorComponent(int component, String name) {
        checkArgument(component >= 0 && component <= 255, "%s must be between 0 and 255", name);
    }

    @Override
    public boolean canCollideWith(Entity entity) {
        return entity instanceof EntityLiving;
    }

    @Override
    public float getHeight() {
        return 0.5F;
    }

    @Override
    public float getWidth() {
        return this.getRadius() * 2.0F;
    }

    @Override
    public float getLength() {
        return this.getRadius() * 2.0F;
    }

    @Override
    public float getGravity() {
        return 0;
    }

    @Override
    public float getDrag() {
        return 0;
    }

}
