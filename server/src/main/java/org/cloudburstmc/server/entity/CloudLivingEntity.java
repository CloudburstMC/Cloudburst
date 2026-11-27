package org.cloudburstmc.server.entity;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTags;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.entity.*;
import org.cloudburstmc.api.entity.damage.DamageEffect;
import org.cloudburstmc.api.entity.damage.DamageSource;
import org.cloudburstmc.api.entity.damage.DamageTypeTags;
import org.cloudburstmc.api.entity.damage.DamageTypes;
import org.cloudburstmc.api.entity.projectile.AbstractArrow;
import org.cloudburstmc.api.entity.projectile.Projectile;
import org.cloudburstmc.api.event.entity.EntityDamageEvent;
import org.cloudburstmc.api.event.entity.EntityDeathEvent;
import org.cloudburstmc.api.event.entity.EntityResurrectEvent;
import org.cloudburstmc.api.event.entity.PotionEffectCause;
import org.cloudburstmc.api.inventory.view.ArmorView;
import org.cloudburstmc.api.item.EquipmentSlot;
import org.cloudburstmc.api.item.ItemBehaviors;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.item.component.AttackBlockingComponent;
import org.cloudburstmc.api.item.component.DeathProtectionComponent;
import org.cloudburstmc.api.level.BlockShapeMode;
import org.cloudburstmc.api.level.FluidCollisionMode;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.level.RayTraceContext;
import org.cloudburstmc.api.level.gamerule.GameRules;
import org.cloudburstmc.api.level.sound.SoundTypes;
import org.cloudburstmc.api.player.Ability;
import org.cloudburstmc.api.potion.EffectTypes;
import org.cloudburstmc.api.potion.PotionEffect;
import org.cloudburstmc.api.util.CollisionContext;
import org.cloudburstmc.api.util.MissHitResult;
import org.cloudburstmc.api.util.MissReason;
import org.cloudburstmc.math.vector.Vector2f;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDamageCause;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityEventType;
import org.cloudburstmc.protocol.bedrock.packet.AnimatePacket;
import org.cloudburstmc.protocol.bedrock.packet.EntityEventPacket;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.entity.data.CloudLivingData;
import org.cloudburstmc.server.event.entity.CloudEntityDamageEvent;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.potion.InfestedPotionEffect;
import org.cloudburstmc.server.registry.CloudEntityRegistry;
import org.cloudburstmc.server.registry.CloudItemRegistry;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.function.DoubleUnaryOperator;

import static com.google.common.base.Preconditions.checkArgument;
import static org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag.BABY;
import static org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag.BREATHING;

public abstract class CloudLivingEntity extends CloudEntity implements LivingEntity {

    private static final int HURT_COOLDOWN_TICKS = 10;
    private static final float DEFAULT_KNOCKBACK_STRENGTH = 0.4f;
    private static final int MAX_AIR_TICKS = 400;
    private static final float MAX_SIGHT_DISTANCE = 128;
    private static final Set<EntityType<?>> DRY_OUT_TYPES = Set.of(
            EntityTypes.AXOLOTL, EntityTypes.COD, EntityTypes.GLOW_SQUID, EntityTypes.NAUTILUS,
            EntityTypes.PUFFERFISH, EntityTypes.SALMON, EntityTypes.SQUID, EntityTypes.TADPOLE,
            EntityTypes.TROPICAL_FISH
    );
    private static final Set<EntityType<?>> OTHER_UNDERWATER_BREATHERS = Set.of(
            EntityTypes.ARMOR_STAND, EntityTypes.COPPER_GOLEM, EntityTypes.ELDER_GUARDIAN,
            EntityTypes.FROG, EntityTypes.GUARDIAN, EntityTypes.TURTLE
    );

    private boolean inPowderSnow;
    private int hurtCooldownTicks;
    private float lastDamageAmount;
    private boolean preparingDeath;
    private boolean preventingDeath;

    protected boolean invisible;
    private final CloudLivingData persistenceState;

    public CloudLivingEntity(EntityType<?> type, Location location) {
        this(type, location, new CloudLivingData());
    }

    protected CloudLivingEntity(EntityType<?> type, Location location, CloudLivingData state) {
        super(type, location, state);
        this.persistenceState = state;
    }

    @Override
    protected CloudLivingData data() {
        return this.persistenceState;
    }

    @Override
    protected void loadCustom(NbtMap tag) {
        super.loadCustom(tag);
        if (tag.containsKey("AbsorptionAmount") || !this.hasPotionEffect(EffectTypes.ABSORPTION)) {
            float absorption = getFloat(tag, "AbsorptionAmount", 0f);
            this.setAbsorption(Float.isFinite(absorption) ? Math.max(0f, absorption) : 0f);
        }

        float savedHealth = getFloat(tag, "Health", this.getHealth());
        if (Float.isFinite(savedHealth) && savedHealth >= 0) {
            this.setHealth(savedHealth);
        }
    }

    @Override
    protected void saveCustom(NbtMapBuilder tag) {
        super.saveCustom(tag);
        tag.putFloat("Health", this.getHealth());
        if (this.getAbsorption() > 0 || this.hasPotionEffect(EffectTypes.ABSORPTION)) {
            tag.putFloat("AbsorptionAmount", this.getAbsorption());
        }
    }

    @Override
    protected Set<String> customKeys() {
        Set<String> keys = new HashSet<>(super.customKeys());
        keys.add("Health");
        keys.add("AbsorptionAmount");
        return Set.copyOf(keys);
    }

    @Override
    public void setHealth(float health) {
        boolean wasAlive = this.isAlive();
        super.setHealth(health);
        if (this.isAlive() && !wasAlive) {
            EntityEventPacket pk = new EntityEventPacket();
            pk.setRuntimeEntityId(this.getRuntimeId());
            pk.setType(EntityEventType.RESPAWN);
            this.broadcastToViewers(pk);
        }
    }

    @Override
    public float getGravity() {
        return 0.08f;
    }

    @Override
    public float getDrag() {
        return 0.02f;
    }

    @Override
    protected float getStepHeight() {
        return 0.6f;
    }

    @Override
    public boolean isBaby() {
        return this.data.getFlag(BABY);
    }

    @Override
    public boolean isPushable() {
        return this.isAlive() && (!(this instanceof CloudPlayer player) || !player.isSpectator());
    }

    @Override
    public boolean blocksBuilding() {
        return true;
    }

    @Override
    public boolean isClimbing() {
        if (this.level == null || this.getVehicle() != null || this instanceof CloudPlayer player && (player.isSpectator() || player.getAbilities().get(Ability.FLYING))) {
            return false;
        }

        Vector3i feet = this.getPosition().toInt();
        BlockState state = this.level.getBlockState(feet);
        if (this instanceof HumanEntity human && human.isGliding() && state.is(BlockTags.CAN_GLIDE_THROUGH)) {
            return false;
        }

        if (state.is(BlockTags.CLIMBABLE)) {
            return true;
        }

        if (!state.is(BlockTags.TRAPDOOR) || !state.ensureTrait(BlockTraits.IS_OPEN)) {
            return false;
        }

        BlockState below = this.level.getBlockState(feet.sub(0, 1, 0));
        return below.getType() == BlockTypes.LADDER
                && below.ensureTrait(BlockTraits.FACING_DIRECTION) == state.ensureTrait(BlockTraits.DIRECTION);
    }

    public void collidingWith(Entity ent) {
        if (ent instanceof CloudEntity other) {
            other.onEntityCollision(this);
        }
    }


    @Override
    public boolean entityBaseTick(int tickDiff) {
        if (this.isClosed()) {
            return false;
        }

        boolean hasUpdate = super.entityBaseTick(tickDiff);
        if (!this.canContinueLivingTick()) {
            this.inPowderSnow = false;
            return hasUpdate;
        }

        this.tickFreezing(tickDiff);
        if (this.canContinueLivingTick()) {
            hasUpdate |= this.tickLivingEnvironment(tickDiff);
        }

        if (!this.canContinueLivingTick()) {
            return hasUpdate;
        }

        this.hurtCooldownTicks = Math.max(this.hurtCooldownTicks - tickDiff, 0);
        this.tickLivingCollisions();
        return hasUpdate;
    }

    private boolean canContinueLivingTick() {
        return !this.isClosed() && this.isAlive();
    }

    private boolean tickLivingEnvironment(int tickDiff) {
        boolean suffocating = this.isInsideOfSolid();
        if (suffocating) {
            this.damage(1, DamageSource.of(DamageTypes.IN_WALL));
            if (!this.canContinueLivingTick()) {
                return true;
            }
        }

        BlockState state = this.getLevel().getBlockState(this.getPosition().toInt());
        if (state.is(BlockTags.FALL_DAMAGE_RESETTING) || this.hasPotionEffect(EffectTypes.LEVITATION)) {
            this.resetFallDistance();
        }

        return this.tickAirSupply(tickDiff) || suffocating;
    }

    private boolean tickAirSupply(int tickDiff) {
        boolean inWater = this.isInsideOfWater();
        boolean dryingOut = !inWater && DRY_OUT_TYPES.contains(this.getType()) && !(this.getType() == EntityTypes.AXOLOTL ? this.isInWaterOrRain() : this.isTouchingWater());
        boolean canBreathe = !dryingOut && (!inWater || this.canBreatheUnderwater()
                || this.hasPotionEffect(EffectTypes.WATER_BREATHING) || this.hasPotionEffect(EffectTypes.CONDUIT_POWER)
                || this instanceof CloudPlayer player && (player.isCreative() || player.isSpectator())
                || this.level.getBlockState(this.position.add(0, this.getEyeHeight(), 0).toInt()).getType() == BlockTypes.BUBBLE_COLUMN);
        this.data.setFlag(BREATHING, canBreathe);

        if (!canBreathe) {
            long remainingAir = (long) this.getAirTicks() - tickDiff;
            if (remainingAir <= -20) {
                this.setAirTicks(0);
                this.damage(2, DamageSource.of(inWater ? DamageTypes.DROWN : DamageTypes.DRY_OUT));
            } else {
                this.setAirTicks((int) remainingAir);
            }

            return true;
        }

        if (inWater) {
            this.setAirTicks(MAX_AIR_TICKS);
        } else if (this.getAirTicks() < MAX_AIR_TICKS) {
            this.setAirTicks((int) Math.min(MAX_AIR_TICKS, this.getAirTicks() + 5L * tickDiff));
        }

        return false;
    }

    private void tickLivingCollisions() {
        if (this.getVehicle() == null && this.isPushable()) {
            for (Entity entity : this.getLevel().getNearbyEntities(this, this.boundingBox)) {
                if (entity.isPushable()) {
                    this.collidingWith(entity);
                    if (!this.canContinueLivingTick()) {
                        return;
                    }
                }
            }
        }
    }

    protected boolean canBreatheUnderwater() {
        EntityType<?> type = this.getType();
        return this.isUndead() || DRY_OUT_TYPES.contains(type) || OTHER_UNDERWATER_BREATHERS.contains(type);
    }

    public void markInPowderSnow() {
        this.inPowderSnow = true;
    }

    public void freezeForTicks(int ticks) {
        checkArgument(ticks >= 0, "ticks cannot be negative");
        if (!this.isFreezeTickingLocked() && this.canFreeze()) {
            this.setFreezeTicks(this.getFreezeTicks() + Math.min(ticks, this.getMaxFreezeTicks() - this.getFreezeTicks()));
        }
    }

    private void tickFreezing(int tickDiff) {
        boolean canFreeze = this.canFreeze();
        boolean hasFreezingEffect = this.hasPotionEffect(EffectTypes.FREEZING);
        boolean inPowderSnow = this.consumeInPowderSnow();
        if (!this.isFreezeTickingLocked()) {
            if (inPowderSnow && canFreeze) {
                this.setFreezeTicks(this.getFreezeTicks() + tickDiff);
            } else if (!canFreeze || !hasFreezingEffect) {
                this.setFreezeTicks(this.getFreezeTicks() - 2 * tickDiff);
            }
        }

        if (!this.isFrozen() || !canFreeze || !this.getLevel().getGameRules().get(GameRules.FREEZE_DAMAGE)) {
            return;
        }

        int previousTicksLived = Math.max(0, this.ticksLived - tickDiff);
        int damagePulses = this.ticksLived / 40 - previousTicksLived / 40;
        for (int pulse = 0; pulse < damagePulses && this.canContinueLivingTick(); pulse++) {
            if (this.damage(hasFreezingEffect ? 1.5f : 1f, DamageSource.of(DamageTypes.FREEZE)) && this.canContinueLivingTick() && this instanceof CloudPlayer) {
                this.getLevel().playSound(this.getPosition(), SoundTypes.MOB_PLAYER_HURT_FREEZE);
            }
        }
    }

    protected boolean canFreeze() {
        if (this instanceof CloudPlayer player && player.isSpectator()) {
            return false;
        }

        if (!CloudEntityRegistry.get().requireComponent(this.getType(), EntityComponents.CAN_FREEZE).execute(this)) {
            return false;
        }

        if (!(this instanceof CloudEquippedLivingEntity equipped)) {
            return true;
        }

        ArmorView armor = equipped.getArmor();
        return armor.getHelmet().getType() != ItemTypes.LEATHER_HELMET
                && armor.getChestplate().getType() != ItemTypes.LEATHER_CHESTPLATE
                && armor.getLeggings().getType() != ItemTypes.LEATHER_LEGGINGS
                && armor.getBoots().getType() != ItemTypes.LEATHER_BOOTS;
    }

    private boolean consumeInPowderSnow() {
        boolean result = this.inPowderSnow;
        this.inPowderSnow = false;
        return result;
    }

    @Override
    protected void onBelowLevel() {
        this.damage(4, DamageSource.of(DamageTypes.OUT_OF_WORLD));
    }

    @Override
    public boolean hasLineOfSight(Entity target) {
        if (this.level == null || target.getLevel() != this.level) {
            return false;
        }

        Vector3f start = this.getPosition().add(0, this.getEyeHeight(), 0);
        Vector3f end = target.getPosition().add(0, target.getEyeHeight(), 0);
        if (start.distanceSquared(end) > MAX_SIGHT_DISTANCE * MAX_SIGHT_DISTANCE) {
            return false;
        }

        RayTraceContext context = new RayTraceContext(start, end, BlockShapeMode.COLLIDER, FluidCollisionMode.NONE, CollisionContext.of(this));
        return this.getLevel().rayTraceBlocks(context) instanceof MissHitResult miss && miss.reason() == MissReason.CLEAR;
    }

    @Override
    public <T extends Projectile> @Nullable T launchProjectile(EntityType<T> projectileType, @Nullable Vector3f velocity, @Nullable Consumer<? super T> configure) {
        Objects.requireNonNull(projectileType, "projectileType");
        Location location = Location.from(this.getPosition().add(0, this.getEyeHeight() - 0.1f, 0), this.getYaw(), this.getPitch(), this.level);
        T projectile = CloudEntityRegistry.get().newEntity(projectileType, location);
        projectile.setShooter(this);
        projectile.setMotion(velocity == null ? this.getDirectionVector() : velocity);
        if (configure != null) {
            configure.accept(projectile);
        }

        return projectile.spawn() ? projectile : null;
    }

    @Override
    public boolean attack(Entity target) {
        Objects.requireNonNull(target, "target");
        float damage = CloudEntityRegistry.get()
                .requireComponent(this.getType(), EntityComponents.GET_ATTACK_DAMAGE)
                .execute(this);
        if (target instanceof Damageable damageable) {
            return damageable.damage(damage, this);
        }

        DamageSource source = DamageSource.builder(this instanceof CloudPlayer ? DamageTypes.PLAYER_ATTACK : DamageTypes.MOB_ATTACK)
                .directEntity(this)
                .causingEntity(this)
                .weaponItem(this instanceof HumanEntity human ? human.getMainHandItem() : ItemStack.EMPTY)
                .build();
        return CloudEntity.applyDamage(target, damage, source);
    }

    public void broadcastCriticalHit() {
        AnimatePacket animate = new AnimatePacket();
        animate.setAction(AnimatePacket.Action.CRITICAL_HIT);
        animate.setRuntimeEntityId(this.getRuntimeId());
        this.getLevel().addChunkPacket(this.getPosition(), animate);
        this.getLevel().addLevelSoundEvent(this.getPosition(), SoundEvent.ATTACK_CRITICAL);
    }

    @Override
    public ItemStack getBlockingItem() {
        return ItemStack.EMPTY;
    }

    @Override
    protected CloudEntityDamageEvent createDamageEvent(DamageSource source, float amount) {
        return new LivingDamageEvent(this, source, amount, this.getCooldownDamage(source));
    }

    private float getCooldownDamage(DamageSource source) {
        return this.hurtCooldownTicks > 0 && !source.getDamageType().is(DamageTypeTags.BYPASSES_COOLDOWN) ? this.lastDamageAmount : 0;
    }

    private static float getCooldownDamage(CloudEntityDamageEvent event) {
        return event instanceof LivingDamageEvent living ? living.cooldownDamage : 0;
    }

    private static class LivingDamageEvent extends CloudEntityDamageEvent {
        private final float cooldownDamage;

        private LivingDamageEvent(CloudLivingEntity target, DamageSource source, float amount, float cooldownDamage) {
            super(target, source, amount, target.createBlockingReduction(source), target.createDamageAdjustment(source),
                    target.getHelmetDamageMultiplier(source), cooldownDamage, target.createDamageReduction(source),
                    target.getAbsorption());
            this.cooldownDamage = cooldownDamage;
        }
    }

    protected DoubleUnaryOperator createBlockingReduction(DamageSource source) {
        if (source.getDamageType().is(DamageTypeTags.BYPASSES_SHIELD) || source.getDirectEntity() instanceof AbstractArrow arrow && arrow.getPierceLevel() > 0) {
            return _ -> 0;
        }

        ItemStack item = this.getBlockingItem();
        Location origin = source.getSourceLocation();
        if (item.isEmpty() || origin == null || origin.getLevel() != this.level) {
            return _ -> 0;
        }

        AttackBlockingComponent blocking = CloudItemRegistry.get().getComponent(item.getType(), ItemBehaviors.BLOCKS_ATTACKS);
        if (blocking == null) {
            return _ -> 0;
        }

        Vector2f toSource = origin.getPosition().sub(this.getPosition()).toVector2(true);
        if (toSource.lengthSquared() == 0) {
            return _ -> 0;
        }

        boolean blocks = blocking.blocksDirection(this.getHorizontalLookVector().dot(toSource.normalize()));
        return damage -> blocks ? damage : 0;
    }

    protected DoubleUnaryOperator createDamageAdjustment(DamageSource source) {
        float multiplier = source.getDamageType().is(DamageTypeTags.IS_FREEZING)
                ? CloudEntityRegistry.get().requireComponent(this.getType(), EntityComponents.GET_FREEZING_DAMAGE_MULTIPLIER).execute(this)
                : 1;
        if (!Float.isFinite(multiplier) || multiplier < 0) {
            throw new IllegalArgumentException("Freezing damage multiplier must be finite and non-negative");
        }
        return damage -> damage * multiplier;
    }

    protected float getHelmetDamageMultiplier(DamageSource source) {
        return 1;
    }

    /**
     * Captures living-entity defenses for one hit without applying their side effects.
     *
     * @param source the damage source
     * @return the reduction applied after blocking and the hurt cooldown
     */
    protected DoubleUnaryOperator createDamageReduction(DamageSource source) {
        if (source.getDamageType().is(DamageTypeTags.BYPASSES_EFFECTS) || source.getDamageType().is(DamageTypeTags.BYPASSES_RESISTANCE)) {
            return damage -> damage;
        }

        PotionEffect resistanceEffect = this.getPotionEffect(EffectTypes.RESISTANCE);
        if (resistanceEffect == null) {
            return damage -> damage;
        }

        float factor = (float) Math.max(0, 1 - ((double) resistanceEffect.getAmplifier() + 1) / 5);
        return damage -> damage * factor;
    }


    @Override
    protected boolean applyDamage(CloudEntityDamageEvent event) {
        if (this.isClosed() || !this.isAlive()) {
            return false;
        }

        float cooldownDamage = getCooldownDamage(event);
        if (cooldownDamage > 0 && event.getAdjustedDamage() <= cooldownDamage && event.getBlockedDamage() == 0) {
            return false;
        }

        if (!super.applyDamage(event) || event.getDamageBeforeReductions() <= 0) {
            return false;
        }

        if (cooldownDamage == 0 && !this.isClosed()) {
            this.applyDamageImpact(event);
        }

        return true;
    }

    @Override
    protected boolean prepareDamage(CloudEntityDamageEvent event) {
        return super.prepareDamage(event) && (event.getDamageBeforeReductions() > 0 || event.getBlockedDamage() > 0);
    }

    private void applyDamageImpact(CloudEntityDamageEvent event) {
        DamageSource source = event.getDamageSource();
        Entity directEntity = source.getDirectEntity();
        Entity causingEntity = source.getCausingEntity();
        Entity impactEntity = directEntity != null ? directEntity : causingEntity;
        if (impactEntity != null && impactEntity.isOnFire() && !(impactEntity instanceof CloudPlayer)) {
            this.setOnFire(2 * this.server.getDifficulty().getValue());
        }

        if (!this.isClosed() && !event.getDamageType().is(DamageTypeTags.NO_KNOCKBACK)) {
            Vector2f direction;
            if (directEntity instanceof Projectile projectile) {
                direction = projectile.getMotion().toVector2(true);
            } else {
                Location origin = source.getSourceLocation();
                direction = origin == null ? Vector2f.ZERO : this.getPosition().sub(origin.getPosition()).toVector2(true);
            }

            this.knockBack(DEFAULT_KNOCKBACK_STRENGTH, direction.getX(), direction.getY(),
                    impactEntity == null ? KnockbackCause.DAMAGE : KnockbackCause.ENTITY_ATTACK,
                    causingEntity == null ? directEntity : causingEntity
            );
        }

        if (!this.isClosed() && !this.isInDeathSequence()) {
            this.broadcastHurtEvent(event.getDamageType().getDamageEffect());
        }
    }

    @Override
    protected void onDamageAccepted(CloudEntityDamageEvent event) {
        super.onDamageAccepted(event);
        float damage = event.getDamageBeforeReductions();
        if (damage > 0) {
            if (getCooldownDamage(event) == 0) {
                this.hurtCooldownTicks = HURT_COOLDOWN_TICKS;
                this.lastDamageAmount = event.getAdjustedDamage();
            } else if (this.hurtCooldownTicks > 0) {
                this.lastDamageAmount = Math.max(this.lastDamageAmount, event.getAdjustedDamage());
            }
        }

        if (event.getBlockedDamage() > 0) {
            this.onDamageBlocked(event);
        }

        if (damage > 0 && !this.isClosed() && this.isAlive()) {
            this.applyDamageEffects(event, damage);
        }
    }

    protected void onDamageBlocked(EntityDamageEvent event) {
        Entity attacker = event.getDamageSource().getDirectEntity();
        if (attacker instanceof CloudLivingEntity livingAttacker
                && !event.getDamageType().is(DamageTypeTags.IS_PROJECTILE)
                && !event.getDamageType().is(DamageTypeTags.NO_KNOCKBACK)) {
            Vector2f direction = attacker.getPosition().sub(this.getPosition()).toVector2(true);
            livingAttacker.knockBack(0.5f, direction.getX(), direction.getY(), KnockbackCause.SHIELD_BLOCK, this);
        }
    }

    /**
     * Runs entity-specific effects after cancellation is checked and before health changes.
     *
     * @param source                 the applied damage event
     * @param damageBeforeReductions damage after the hurt cooldown and before reductions
     */
    protected void applyDamageEffects(CloudEntityDamageEvent source, float damageBeforeReductions) {
    }

    @Override
    protected void onDamageApplied(DamageSource source) {
        super.onDamageApplied(source);
        if (this.hasPotionEffect(EffectTypes.INFESTED)) {
            InfestedPotionEffect.onHurt(this);
        }
    }

    protected void broadcastHurtEvent(DamageEffect effect) {
        this.broadcastToViewers(this.createHurtEventPacket(effect));
    }

    protected final EntityEventPacket createHurtEventPacket(DamageEffect effect) {
        EntityEventPacket packet = new EntityEventPacket();
        packet.setRuntimeEntityId(this.getRuntimeId());
        packet.setType(EntityEventType.HURT);
        packet.setData(getHurtEventData(effect));
        return packet;
    }

    protected static int getHurtEventData(DamageEffect effect) {
        EntityDamageCause cause = switch (effect) {
            case HURT -> EntityDamageCause.OVERRIDE;
            case THORNS -> EntityDamageCause.THORNS;
            case DROWNING -> EntityDamageCause.DROWNING;
            case BURNING -> EntityDamageCause.FIRE_TICK;
            case POKING -> EntityDamageCause.CONTACT;
            case FREEZING -> EntityDamageCause.FREEZING;
        };

        return cause.ordinal() - 1;
    }

    public void knockBack(float strength, float diffX, float diffZ, KnockbackCause cause, @Nullable Entity sourceEntity) {
        if (!Float.isFinite(strength) || strength < 0 || !Float.isFinite(diffX) || !Float.isFinite(diffZ)) {
            throw new IllegalArgumentException("Knockback strength and direction must be finite, with non-negative strength");
        }

        float effectiveStrength = strength * (1 - this.getKnockbackResistance());
        if (effectiveStrength <= 0) {
            return;
        }

        double distance = Math.hypot(diffX, diffZ);
        if (distance < Math.sqrt(1.0e-5)) {
            double angle = ThreadLocalRandom.current().nextDouble(Math.PI * 2);
            diffX = (float) Math.cos(angle);
            diffZ = (float) Math.sin(angle);
            distance = 1;
        }

        float pushX = (float) (diffX / distance * effectiveStrength);
        float pushZ = (float) (diffZ / distance * effectiveStrength);
        Vector3f currentMotion = this.getMotion();
        float verticalMotion = this.isOnGround() ? Math.min(0.4f, currentMotion.getY() / 2 + effectiveStrength) : currentMotion.getY();
        Vector3f resultingMotion = Vector3f.from(currentMotion.getX() / 2 + pushX, verticalMotion, currentMotion.getZ() / 2 + pushZ);
        this.applyKnockback(resultingMotion.sub(currentMotion), cause, sourceEntity);
    }

    protected float getKnockbackResistance() {
        return 0;
    }

    public float getExplosionKnockbackResistance() {
        return 0;
    }

    @Override
    protected boolean tryPreventDeath(DamageSource source) {
        if (this.preventingDeath || this.preparingDeath || this.isClosed() || !this.isAlive()
                || source.getDamageType().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }

        this.preventingDeath = true;

        try {
            return this.activateDeathProtection();
        } finally {
            this.preventingDeath = false;
        }
    }

    private boolean activateDeathProtection() {
        DeathProtectionUse held = this.findDeathProtection();
        EntityResurrectEvent event = new EntityResurrectEvent(this, held == null ? null : held.hand());
        this.server.getEventManager().fire(event);
        if (event.isCancelled() || this.isClosed() || !this.isAlive()) {
            return false;
        }

        DeathProtectionComponent protection = held == null
                ? CloudItemRegistry.get().requireComponent(ItemTypes.TOTEM_OF_UNDYING, ItemBehaviors.DEATH_PROTECTION)
                : held.protection();

        // The animation needs the held item before the authoritative consumption update arrives.
        EntityEventPacket packet = new EntityEventPacket();
        packet.setRuntimeEntityId(this.getRuntimeId());
        packet.setType(EntityEventType.CONSUME_TOTEM);
        CloudServer.broadcastPacket(this.getViewers(), packet);
        if (this instanceof CloudPlayer player) {
            player.sendPacket(packet);
        }

        if (held != null) {
            held.update().accept(held.item().decreaseCount());
        }

        if (this.isClosed() || !this.isAlive()) {
            return false;
        }

        this.setHealth(protection.health());
        if (protection.clearEffects()) {
            this.clearActivePotionEffects(PotionEffectCause.TOTEM);
        }

        for (PotionEffect effect : protection.effects()) {
            if (this.isClosed() || !this.isAlive()) {
                return false;
            }
            this.addPotionEffect(effect, this, PotionEffectCause.TOTEM);
        }

        if (this.isClosed() || !this.isAlive()) {
            return false;
        }

        this.getLevel().playSound(this.getPosition(), protection.sound());
        return true;
    }

    protected @Nullable DeathProtectionUse findDeathProtection() {
        return null;
    }

    @Override
    public void kill() {
        if (!this.isAlive()) {
            return;
        }

        EntityDeathEvent event = this.createDeathEvent();
        if (this.shouldAbortDeath(event)) {
            return;
        }

        super.kill();
        this.processDeath(event);
    }

    protected EntityDeathEvent createDeathEvent() {
        return new EntityDeathEvent(this, this.getDeathDamageSource(), Arrays.asList(this.getDrops()), 0);
    }

    protected DamageSource getDeathDamageSource() {
        EntityDamageEvent lastDamage = this.getLastDamageCause();
        return lastDamage == null ? DamageSource.of(DamageTypes.GENERIC_KILL) : lastDamage.getDamageSource();
    }

    /**
     * Fires the death event and reports whether death should stop.
     */
    protected boolean shouldAbortDeath(EntityDeathEvent event) {
        if (this.preparingDeath || this.isClosed() || !this.isAlive()) {
            return true;
        }

        this.preparingDeath = true;
        try {
            this.server.getEventManager().fire(event);
            if (this.isClosed() || !this.isAlive()) {
                return true;
            }

            if (event.isCancelled()) {
                this.setHealth(event.getReviveHealth());
                return true;
            }

            return false;
        } finally {
            this.preparingDeath = false;
        }
    }

    protected void processDeath(EntityDeathEvent event) {
        EntityEventPacket packet = new EntityEventPacket();
        packet.setRuntimeEntityId(this.getRuntimeId());
        packet.setType(this.getDeathEventType());
        this.broadcastToViewers(packet);

        if (this.getLevel().getGameRules().get(GameRules.DO_MOB_LOOT)) {
            for (ItemStack item : event.getDrops()) {
                this.getLevel().dropItem(this.getPosition(), item);
            }

            if (event.getDroppedExperience() > 0) {
                this.dropDeathExperience(event.getDroppedExperience());
            }
        }
    }

    protected EntityEventType getDeathEventType() {
        return EntityEventType.DEATH;
    }

    protected boolean isInDeathSequence() {
        return this.getHealth() <= 0;
    }

    public ItemStack[] getDrops() {
        return new ItemStack[0];
    }

    protected void dropDeathExperience(int experience) {
        this.getLevel().dropExpOrb(this.getPosition(), experience);
    }

    protected record DeathProtectionUse(EquipmentSlot hand, ItemStack item, DeathProtectionComponent protection, Consumer<ItemStack> update) {
        public DeathProtectionUse {
            Objects.requireNonNull(hand, "hand");
            Objects.requireNonNull(item, "item");
            Objects.requireNonNull(protection, "protection");
            Objects.requireNonNull(update, "update");
        }
    }
}
