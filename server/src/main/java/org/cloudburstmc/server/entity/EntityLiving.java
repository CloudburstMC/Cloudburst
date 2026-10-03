package org.cloudburstmc.server.entity;

import co.aikar.timings.Timing;
import co.aikar.timings.Timings;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.entity.*;
import org.cloudburstmc.api.entity.damage.DamageEffect;
import org.cloudburstmc.api.entity.damage.DamageSource;
import org.cloudburstmc.api.entity.damage.DamageTypeTags;
import org.cloudburstmc.api.entity.damage.DamageTypes;
import org.cloudburstmc.api.entity.projectile.AbstractArrow;
import org.cloudburstmc.api.event.entity.EntityDamageEvent;
import org.cloudburstmc.api.event.entity.EntityDeathEvent;
import org.cloudburstmc.api.event.entity.EntityResurrectEvent;
import org.cloudburstmc.api.event.entity.PotionEffectCause;
import org.cloudburstmc.api.item.EquipmentSlot;
import org.cloudburstmc.api.item.ItemBehaviors;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.item.component.AttackBlockingComponent;
import org.cloudburstmc.api.item.component.DeathProtectionComponent;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.level.gamerule.GameRules;
import org.cloudburstmc.api.level.sound.SoundTypes;
import org.cloudburstmc.api.player.Ability;
import org.cloudburstmc.api.potion.EffectTypes;
import org.cloudburstmc.api.potion.PotionEffect;
import org.cloudburstmc.math.vector.Vector2f;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDamageCause;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityEventType;
import org.cloudburstmc.protocol.bedrock.packet.AnimatePacket;
import org.cloudburstmc.protocol.bedrock.packet.EntityEventPacket;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.entity.passive.EntityWaterAnimal;
import org.cloudburstmc.server.event.entity.CloudEntityDamageEvent;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudEntityRegistry;
import org.cloudburstmc.server.registry.CloudItemRegistry;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.function.DoubleUnaryOperator;

import static org.cloudburstmc.api.block.BlockTypes.MAGMA;
import static org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag.BREATHING;

public abstract class EntityLiving extends CloudEntity implements Living {

    private static final int HURT_COOLDOWN_TICKS = 10;
    private static final float DEFAULT_KNOCKBACK_STRENGTH = 0.4f;

    private boolean inPowderSnow;
    private int hurtCooldownTicks;
    private float lastDamageAmount;
    private boolean preparingDeath;
    private boolean preventingDeath;

    protected boolean invisible;
    protected int turtleTicks = 200;

    public EntityLiving(EntityType<?> type, Location location) {
        super(type, location);
    }

    @Override
    public void loadAdditionalData(NbtMap tag) {
        super.loadAdditionalData(tag);

        tag.listenForFloat("Health", this::setHealth);
    }

    @Override
    public void saveAdditionalData(NbtMapBuilder tag) {
        super.saveAdditionalData(tag);

        tag.putFloat("Health", this.getHealth());
    }

    @Override
    public void setHealth(float health) {
        boolean wasAlive = this.isAlive();
        super.setHealth(health);
        if (this.isAlive() && !wasAlive) {
            EntityEventPacket pk = new EntityEventPacket();
            pk.setRuntimeEntityId(this.getRuntimeId());
            pk.setType(EntityEventType.RESPAWN);
            CloudServer.broadcastPacket(this.hasSpawned, pk);
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
    public boolean isPushable() {
        return this.isAlive() && (!(this instanceof CloudPlayer player) || !player.isSpectator());
    }

    @Override
    public boolean isClimbing() {
        if (this instanceof CloudPlayer player && (player.isSpectator() || player.getAbilities().get(Ability.FLYING))) {
            return false;
        }

        BlockState state = this.getLevel().getBlockState(this.getPosition().toInt());
        if (this instanceof EntityHuman human && human.isGliding() && state.is(BlockTags.CAN_GLIDE_THROUGH)) {
            return false;
        }

        if (state.is(BlockTags.CLIMBABLE)) {
            return true;
        }

        if (!state.is(BlockTags.TRAPDOOR) || !Boolean.TRUE.equals(state.getTraits().get(BlockTraits.IS_OPEN))) {
            return false;
        }

        BlockState below = this.getLevel().getBlockState(this.getPosition().toInt().sub(0, 1, 0));
        return below.getType() == BlockTypes.LADDER
                && below.ensureTrait(BlockTraits.FACING_DIRECTION) == state.getTraits().get(BlockTraits.DIRECTION);
    }

    public void collidingWith(Entity ent) { // can override (IronGolem|Bats)
        ent.onEntityCollision(this);
    }


    @Override
    public boolean entityBaseTick() {
        return this.entityBaseTick(1);
    }

    @Override
    public boolean entityBaseTick(int tickDiff) {
        try (Timing ignored = Timings.livingEntityBaseTickTimer.startTiming()) {
            boolean isBreathing = !this.isInsideOfWater();
            if (this instanceof CloudPlayer && (((CloudPlayer) this).isCreative() || ((CloudPlayer) this).isSpectator())) {
                isBreathing = true;
            }

            if (this instanceof CloudPlayer) {
                if (!isBreathing && ((CloudPlayer) this).getArmor().getHelmet().getType() == ItemTypes.TURTLE_HELMET) {
                    if (turtleTicks > 0) {
                        isBreathing = true;
                        turtleTicks--;
                    }
                } else {
                    turtleTicks = 200;
                }
            }

            this.data.setFlag(BREATHING, isBreathing);

            boolean hasUpdate = super.entityBaseTick(tickDiff);
            this.tickFreezing(tickDiff);

            if (this.isAlive()) {

                if (this.isInsideOfSolid()) {
                    hasUpdate = true;
                    this.damage(1, DamageSource.of(DamageTypes.IN_WALL));
                }

                BlockState state = this.getLevel().getBlockState(this.getPosition().toInt());
                if (state.is(BlockTags.FALL_DAMAGE_RESETTING) || this.hasPotionEffect(EffectTypes.LEVITATION)) {
                    this.resetFallDistance();
                }

                if (!this.hasPotionEffect(EffectTypes.WATER_BREATHING) && this.isInsideOfWater()) {
                    if (this instanceof EntityWaterAnimal || (this instanceof CloudPlayer && (((CloudPlayer) this).isCreative() || ((CloudPlayer) this).isSpectator()))) {
                        this.setAirTicks(400);
                    } else {
                        if (turtleTicks == 0 || turtleTicks == 200) {
                            hasUpdate = true;
                            int airTicks = this.getAirTicks() - tickDiff;

                            if (airTicks <= -20) {
                                airTicks = 0;
                                this.damage(2, DamageSource.of(DamageTypes.DROWN));
                            }

                            setAirTicks(airTicks);
                        }
                    }
                } else {
                    if (this instanceof EntityWaterAnimal) {
                        hasUpdate = true;
                        int airTicks = getAirTicks() - tickDiff;

                        if (airTicks <= -20) {
                            airTicks = 0;
                            this.damage(2, DamageSource.of(DamageTypes.DRY_OUT));
                        }

                        setAirTicks(airTicks);
                    } else {
                        int airTicks = getAirTicks();

                        if (airTicks < 400) {
                            setAirTicks(Math.min(400, airTicks + tickDiff * 5));
                        }
                    }
                }
            }

            if (this.hurtCooldownTicks > 0) {
                this.hurtCooldownTicks = Math.max(this.hurtCooldownTicks - tickDiff, 0);
            }

            if (this.vehicle == null && this.isPushable()) {
                for (Entity entity : this.getLevel().getNearbyEntities(this, this.boundingBox)) {
                    if (entity.isPushable()) {
                        this.collidingWith(entity);
                    }
                }
            }

            // Used to check collisions with magma blocks
            Block block = this.getLevel().getBlock(this.getPosition().sub(0, 1, 0).toInt());
            if (block.getState().getType() == MAGMA) block.requireComponent(BlockComponents.ON_ENTITY_COLLIDE).execute(block, this);
            return hasUpdate;
        }
    }

    public void markInPowderSnow() {
        this.inPowderSnow = true;
    }

    private void tickFreezing(int tickDiff) {
        if (this.isFreezeTickingLocked()) {
            this.consumeInPowderSnow();
            return;
        }

        boolean canFreeze = this.canFreeze();
        if (this.consumeInPowderSnow() && canFreeze) {
            this.setFreezeTicks(this.getFreezeTicks() + tickDiff);
        } else {
            this.setFreezeTicks(this.getFreezeTicks() - 2 * tickDiff);
        }

        if (!this.isFrozen() || !canFreeze || !this.getLevel().getGameRules().get(GameRules.FREEZE_DAMAGE)) {
            return;
        }

        int previousTicksLived = Math.max(0, this.ticksLived - tickDiff);
        int damagePulses = this.ticksLived / 40 - previousTicksLived / 40;
        for (int pulse = 0; pulse < damagePulses; pulse++) {
            if (this.damage(1, DamageSource.of(DamageTypes.FREEZE)) && this instanceof CloudPlayer) {
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

        if (!(this instanceof EntityCreature creature)) {
            return true;
        }

        return creature.getArmor().getHelmet().getType() != ItemTypes.LEATHER_HELMET
                && creature.getArmor().getChestplate().getType() != ItemTypes.LEATHER_CHESTPLATE
                && creature.getArmor().getLeggings().getType() != ItemTypes.LEATHER_LEGGINGS
                && creature.getArmor().getBoots().getType() != ItemTypes.LEATHER_BOOTS;
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

    public boolean hasLineOfSight(Entity entity) {
        //todo
        return true;
    }

    @Override
    public <T extends Projectile> @Nullable T launchProjectile(EntityType<T> type, @Nullable Vector3f velocity, @Nullable Consumer<? super T> configurator) {
        Objects.requireNonNull(type, "type");
        Location location = Location.from(this.getPosition().add(0, this.getEyeHeight() - 0.1f, 0), this.getYaw(), this.getPitch(), this.level);
        T projectile = CloudEntityRegistry.get().newEntity(type, location);
        projectile.setShooter(this);
        projectile.setMotion(velocity == null ? this.getDirectionVector() : velocity);
        if (configurator != null) {
            configurator.accept(projectile);
        }

        return projectile.spawn() ? projectile : null;
    }

    @Override
    public boolean attack(Entity target) {
        Objects.requireNonNull(target, "target");
        float damage = CloudEntityRegistry.get()
                .requireComponent(this.getType(), EntityComponents.GET_ATTACK_DAMAGE)
                .execute(this);
        DamageSource source = DamageSource.of(DamageTypes.MOB_ATTACK, this);
        return target.damage(damage, source);
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
    public boolean isBlocking() {
        return !this.getBlockingItem().isEmpty();
    }

    @Override
    protected CloudEntityDamageEvent createDamageEvent(DamageSource source, float amount) {
        float cooldownDamage = source.getDamageType().is(DamageTypeTags.BYPASSES_COOLDOWN) || this.hurtCooldownTicks <= 0 ? 0 : this.lastDamageAmount;
        return new CloudEntityDamageEvent(this, source, amount, this.createBlockingReduction(source), this.createDamageAdjustment(source), this.getHelmetDamageMultiplier(source), cooldownDamage, this.createDamageReduction(source), this.getAbsorption());
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

        boolean blocks = blocking.blocksDirection(this.getDirectionPlane().dot(toSource.normalize()));
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
    protected boolean applyDamage(CloudEntityDamageEvent source) {
        if (this.isClosed() || !this.isAlive()) {
            return false;
        }

        Entity directEntity = source.getDamageSource().getDirectEntity();
        Entity causingEntity = source.getDamageSource().getCausingEntity();

        boolean fullDamage = source.getDamageType().is(DamageTypeTags.BYPASSES_COOLDOWN) || this.hurtCooldownTicks <= 0;
        if (!fullDamage && source.getAdjustedDamage() <= this.lastDamageAmount && source.getBlockedDamage() == 0) {
            return false;
        }

        if (!super.applyDamage(source)) {
            return false;
        }

        float cooldownAdjustedDamage = source.getDamageBeforeReductions();
        if (cooldownAdjustedDamage <= 0) {
            return false;
        }

        if (fullDamage) {
            Entity impactEntity = directEntity != null ? directEntity : causingEntity;
            if (impactEntity != null) {
                if (impactEntity.isOnFire() && !(impactEntity instanceof CloudPlayer)) {
                    this.setOnFire(2 * this.server.getDifficulty().getId());
                }
            }

            if (!source.getDamageType().is(DamageTypeTags.NO_KNOCKBACK)) {
                Vector2f direction;
                if (directEntity instanceof Projectile projectile) {
                    direction = projectile.getMotion().toVector2(true);
                } else {
                    Location origin = source.getDamageSource().getSourceLocation();
                    direction = origin == null ? Vector2f.ZERO : this.getPosition().sub(origin.getPosition()).toVector2(true);
                }

                this.knockBack(DEFAULT_KNOCKBACK_STRENGTH, direction.getX(), direction.getY(),
                        directEntity == null && causingEntity == null ? KnockbackCause.DAMAGE : KnockbackCause.ENTITY_ATTACK,
                        causingEntity == null ? directEntity : causingEntity);
            }

            if (!this.isInDeathSequence()) {
                this.broadcastHurtEvent(source.getDamageType().getDamageEffect());
            }
        }

        return true;
    }

    @Override
    protected void onDamageAccepted(CloudEntityDamageEvent event) {
        super.onDamageAccepted(event);
        float damage = event.getDamageBeforeReductions();
        if (damage > 0) {
            if (this.hurtCooldownTicks <= 0 || event.getDamageType().is(DamageTypeTags.BYPASSES_COOLDOWN)) {
                this.hurtCooldownTicks = HURT_COOLDOWN_TICKS;
            }

            this.lastDamageAmount = event.getAdjustedDamage();
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
        if (attacker instanceof EntityLiving livingAttacker
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

    protected void broadcastHurtEvent(DamageEffect effect) {
        CloudServer.broadcastPacket(this.hasSpawned, this.createHurtEventPacket(effect));
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
        if (!this.prepareDeath(event)) {
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

    protected boolean prepareDeath(EntityDeathEvent event) {
        if (this.preparingDeath || this.isClosed() || !this.isAlive()) {
            return false;
        }

        this.preparingDeath = true;
        try {
            this.server.getEventManager().fire(event);
            if (this.isClosed() || !this.isAlive()) {
                return false;
            }

            if (event.isCancelled()) {
                this.setHealth(event.getReviveHealth());
                return false;
            }

            return true;
        } finally {
            this.preparingDeath = false;
        }
    }

    protected void processDeath(EntityDeathEvent event) {
        EntityEventPacket packet = new EntityEventPacket();
        packet.setRuntimeEntityId(this.getRuntimeId());
        packet.setType(this.getDeathEventType());
        CloudServer.broadcastPacket(this.hasSpawned, packet);

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
