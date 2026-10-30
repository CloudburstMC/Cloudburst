package org.cloudburstmc.server.entity.misc;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.Living;
import org.cloudburstmc.api.entity.RegainReason;
import org.cloudburstmc.api.entity.damage.DamageSource;
import org.cloudburstmc.api.entity.damage.DamageTypes;
import org.cloudburstmc.api.entity.data.AreaEffectCloudData;
import org.cloudburstmc.api.entity.data.EntityData;
import org.cloudburstmc.api.entity.misc.AreaEffectCloud;
import org.cloudburstmc.api.event.entity.AreaEffectCloudApplyEvent;
import org.cloudburstmc.api.event.entity.PotionEffectCause;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.level.particle.ParticleType;
import org.cloudburstmc.api.level.particle.ParticleTypes;
import org.cloudburstmc.api.potion.*;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.entity.EntityLiving;
import org.cloudburstmc.server.event.entity.CloudEntityDamageEvent;
import org.cloudburstmc.server.network.NetworkUtils;
import org.cloudburstmc.server.potion.CloudPotionColor;

import java.util.*;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;
import static org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes.*;
import static org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag.FIRE_IMMUNE;
import static org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag.NO_AI;

public class EntityAreaEffectCloud extends CloudEntity implements AreaEffectCloud {
    private final Map<Long, Integer> victims = new HashMap<>();
    private int nextApply;
    private boolean particleColorSet;
    private int lastAge;

    public EntityAreaEffectCloud(EntityType<?> type, Location location) {
        super(type, location);
        this.data.set(AREA_EFFECT_CLOUD_RADIUS, 3.0F);
        this.data.set(AREA_EFFECT_CLOUD_WAITING, 10);
        this.data.set(AREA_EFFECT_CLOUD_PARTICLE, NetworkUtils.particleToNetwork(ParticleTypes.MOB_SPELL_AMBIENT));
        this.data.set(AUX_VALUE_DATA, NetworkUtils.potionToNetwork(PotionTypes.WATER));
        this.data().setPotionId(NetworkUtils.potionToNetwork(PotionTypes.WATER));
        this.recalculatePotionColor();
    }

    @Override
    public int getWaitTime() {
        return this.data().getWaitTime();
    }

    @Override
    public void setWaitTime(int waitTime) {
        checkArgument(waitTime >= 0, "waitTime cannot be negative");
        this.data().setWaitTime(waitTime);
        this.data.set(AREA_EFFECT_CLOUD_WAITING, waitTime);
    }

    @Override
    public PotionType getPotionType() {
        Short potionId = this.data().getPotionId();
        return potionId == null ? PotionTypes.WATER : NetworkUtils.potionFromNetwork(potionId);
    }

    @Override
    public void setPotionType(PotionType potionType) {
        requireNonNull(potionType, "potionType");
        this.data().setPotionId(NetworkUtils.potionToNetwork(potionType));
        this.data.set(AUX_VALUE_DATA, NetworkUtils.potionToNetwork(potionType));
        this.recalculatePotionColor();
    }

    private void recalculatePotionColor() {
        Integer particleColor = this.data().getParticleColor();
        int color = this.particleColorSet && particleColor != null ? particleColor
                : 0xff000000 | CloudPotionColor.calculateEffects(this.getApplicationEffects()).orElse(0x2828ff);
        this.updatePotionColor(color);
    }

    @Override
    public int getPotionColor() {
        return this.data.require(EFFECT_COLOR);
    }

    @Override
    public void setPotionColor(int argb) {
        this.data().setParticleColor(argb);
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
        this.data().setParticleColor(argb);
    }

    @Override
    public int getReapplicationDelay() {
        return this.data().getReapplicationDelay();
    }

    @Override
    public void setReapplicationDelay(int reapplicationDelay) {
        checkArgument(reapplicationDelay >= 0, "reapplicationDelay cannot be negative");
        this.data().setReapplicationDelay(reapplicationDelay);
    }

    @Override
    public int getDurationOnUse() {
        return this.data().getDurationOnUse();
    }

    @Override
    public void setDurationOnUse(int durationOnUse) {
        this.data().setDurationOnUse(durationOnUse);
    }

    @Override
    public float getRadiusOnUse() {
        return this.data().getRadiusOnUse();
    }

    @Override
    public void setRadiusOnUse(float radiusOnUse) {
        checkArgument(Float.isFinite(radiusOnUse), "radiusOnUse must be finite");
        this.data().setRadiusOnUse(radiusOnUse);
    }

    @Override
    public float getRadiusPerTick() {
        return this.data().getRadiusPerTick();
    }

    @Override
    public void setRadiusPerTick(float radiusPerTick) {
        checkArgument(Float.isFinite(radiusPerTick), "radiusPerTick must be finite");
        this.data().setRadiusPerTick(radiusPerTick);
    }

    @Override
    public int getDuration() {
        return this.data().getDuration();
    }

    @Override
    public void setDuration(int duration) {
        checkArgument(duration == PotionEffect.INFINITE_DURATION || duration >= 0,
                "duration must be non-negative or PotionEffect.INFINITE_DURATION");
        this.data().setDuration(duration);
    }

    private float potionDurationScale() {
        Float potionDurationScale = this.data().getPotionDurationScale();
        return potionDurationScale == null ? 1.0F : potionDurationScale;
    }

    public void setPotionDurationScale(float potionDurationScale) {
        checkArgument(Float.isFinite(potionDurationScale) && potionDurationScale >= 0.0F,
                "potionDurationScale must be finite and non-negative");
        this.data().setPotionDurationScale(potionDurationScale);
    }

    @Override
    public float getRadius() {
        return this.data.require(AREA_EFFECT_CLOUD_RADIUS);
    }

    @Override
    public void setRadius(float radius) {
        checkArgument(Float.isFinite(radius), "radius must be finite");
        this.data().setRadius(Math.clamp(radius, 0.0F, 32.0F));
        this.data.set(AREA_EFFECT_CLOUD_RADIUS, Math.clamp(radius, 0.0F, 32.0F));
        if (this.boundingBox != null) {
            this.recalculateBoundingBox();
        }
    }

    @Override
    public ParticleType getParticle() {
        return NetworkUtils.particleFromNetwork(this.data.require(AREA_EFFECT_CLOUD_PARTICLE));
    }

    @Override
    public void setParticle(ParticleType particle) {
        this.data.set(AREA_EFFECT_CLOUD_PARTICLE, NetworkUtils.particleToNetwork(requireNonNull(particle, "particle")));
    }

    @Override
    public List<PotionEffect> getCustomEffects() {
        return List.copyOf(this.data().getMobEffects());
    }

    @Override
    public boolean hasCustomEffects() {
        return !this.data().getMobEffects().isEmpty();
    }

    @Override
    public boolean hasCustomEffect(EffectType type) {
        requireNonNull(type, "type");
        return this.data().getMobEffects().stream().anyMatch(effect -> effect.getType() == type);
    }

    @Override
    public boolean addCustomEffect(PotionEffect effect, boolean overwrite) {
        requireNonNull(effect, "effect");
        for (int index = 0; index < this.data().getMobEffects().size(); index++) {
            if (this.data().getMobEffects().get(index).getType() == effect.getType()) {
                if (!overwrite) {
                    return false;
                }

                this.data().getMobEffects().set(index, effect);
                this.recalculatePotionColor();
                return true;
            }
        }

        this.data().getMobEffects().add(effect);
        this.recalculatePotionColor();
        return true;
    }

    @Override
    public boolean removeCustomEffect(EffectType type) {
        requireNonNull(type, "type");
        boolean changed = this.data().getMobEffects().removeIf(effect -> effect.getType() == type);
        if (changed) {
            this.recalculatePotionColor();
        }

        return changed;
    }

    @Override
    public boolean clearCustomEffects() {
        if (this.data().getMobEffects().isEmpty()) {
            return false;
        }

        this.data().getMobEffects().clear();
        this.recalculatePotionColor();
        return true;
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.setInvulnerable(true);
        this.data.setFlag(FIRE_IMMUNE, true);
        this.data.setFlag(NO_AI, true);
        this.data.set(AREA_EFFECT_CLOUD_DURATION, Integer.MAX_VALUE);
        this.data.set(AREA_EFFECT_CLOUD_CHANGE_RATE, Float.MIN_VALUE);
        this.data.set(AREA_EFFECT_CLOUD_CHANGE_ON_PICKUP, Float.MIN_VALUE);
        this.data.set(AREA_EFFECT_CLOUD_PICKUP_COUNT, 0);
        this.setDuration(600);
        this.setReapplicationDelay(20);
        this.setDurationOnUse(0);
        this.setRadiusOnUse(-0.5F);
        this.setRadiusPerTick(-0.005F);
        this.setMaxHealth(1);
        this.setHealth(1);
    }

    @Override
    protected AreaEffectCloudData data() {
        return (AreaEffectCloudData) super.data();
    }

    @Override
    protected Class<? extends EntityData> minimumDataClass() {
        return AreaEffectCloudData.class;
    }

    @Override
    protected void loadCustom(NbtMap tag) {
        // Migrated from applyData: every key runs before the base load below, matching the
        // old applyData-before-loadCustom order. The values are snapshotted first: the
        // potion setter recalculates the color into the data instance, which must not
        // clobber the stored color before the color setter restores it. The effect list
        // already sits in the data instance; every other setter re-runs to keep its
        // validation and metadata sync, exactly as the old applyData did. Each keeps its
        // current value when absent.
        Integer particleColor = this.data().getParticleColor();
        if (tag.containsKey("PotionId")) {
            Short potionId = this.data().getPotionId();
            if (potionId != null) {
                this.setPotionType(NetworkUtils.potionFromNetwork(potionId));
            }
        }
        if (tag.containsKey("ParticleColor") && particleColor != null) {
            this.setPotionColor(particleColor);
        }
        if (tag.containsKey("Duration")) {
            this.setDuration(this.data().getDuration());
        }
        if (tag.containsKey("DurationOnUse")) {
            this.setDurationOnUse(this.data().getDurationOnUse());
        }
        if (tag.containsKey("ReapplicationDelay")) {
            this.setReapplicationDelay(this.data().getReapplicationDelay());
        }
        if (tag.containsKey("Radius")) {
            this.setRadius(this.data().getRadius());
        }
        if (tag.containsKey("RadiusOnUse")) {
            this.setRadiusOnUse(this.data().getRadiusOnUse());
        }
        if (tag.containsKey("RadiusPerTick")) {
            this.setRadiusPerTick(this.data().getRadiusPerTick());
        }
        if (tag.containsKey("WaitTime")) {
            this.setWaitTime(this.data().getWaitTime());
        }
        if (tag.containsKey("PotionDurationScale")) {
            this.setPotionDurationScale(getFloat(tag, "PotionDurationScale", 1.0F));
        }
        super.loadCustom(tag);
    }

    @Override
    protected void saveCustom(NbtMapBuilder tag) {
        super.saveCustom(tag);
        // The duration scale is conditional: a scale of one is not written, as before.
        float potionDurationScale = this.potionDurationScale();
        if (potionDurationScale != 1.0F) {
            tag.putFloat("PotionDurationScale", potionDurationScale);
        }
    }

    @Override
    protected Set<String> customKeys() {
        Set<String> keys = new HashSet<>(super.customKeys());
        keys.add("PotionDurationScale");
        return Set.copyOf(keys);
    }

    @Override
    protected boolean applyDamage(CloudEntityDamageEvent source) {
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
                            this.victims.put(affectedEntity.getUniqueId(), age + this.getReapplicationDelay());
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
                                        affectedEntity.damage((float) (0.5 * (double) (6 << effect.getAmplifier())), source);
                                    } else {
                                        affectedEntity.heal((float) (0.5 * (double) (4 << effect.getAmplifier())), RegainReason.MAGIC);
                                    }

                                    continue;
                                }

                                ((CloudEntity) affectedEntity).addPotionEffect(effect, this, PotionEffectCause.AREA_EFFECT_CLOUD);
                            }

                            if (this.getRadiusOnUse() != 0) {
                                radius += this.getRadiusOnUse();
                                if (radius < 0.5F) {
                                    this.setRadius(radius);
                                    this.kill();
                                    this.timing.stopTiming();
                                    return true;
                                }
                            }

                            if (this.getDurationOnUse() != 0 && this.getDuration() != PotionEffect.INFINITE_DURATION) {
                                int duration = this.getDuration() + this.getDurationOnUse();
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
        List<PotionEffect> effects = new ArrayList<>(this.getPotionType().getEffects().size() + this.data().getMobEffects().size());
        this.getPotionType().getEffects().forEach(effect -> effects.add(this.scaleDuration(effect)));
        this.data().getMobEffects().forEach(effect -> effects.add(this.scaleDuration(effect)));
        return effects;
    }

    private PotionEffect scaleDuration(PotionEffect effect) {
        if (!effect.isInfinite() && effect.getDuration() != 0) {
            int duration = Math.max((int) Math.floor(effect.getDuration() * this.potionDurationScale()), 1);
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
