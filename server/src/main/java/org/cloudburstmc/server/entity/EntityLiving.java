package org.cloudburstmc.server.entity;

import co.aikar.timings.Timing;
import co.aikar.timings.Timings;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockComponents;
import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.entity.*;
import org.cloudburstmc.api.entity.damage.DamageEffect;
import org.cloudburstmc.api.entity.damage.DamageSource;
import org.cloudburstmc.api.entity.damage.DamageTypeTags;
import org.cloudburstmc.api.entity.damage.DamageTypes;
import org.cloudburstmc.api.event.entity.EntityDamageEvent;
import org.cloudburstmc.api.event.entity.EntityDeathEvent;
import org.cloudburstmc.api.event.entity.ProjectileLaunchEvent;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.level.gamerule.GameRules;
import org.cloudburstmc.api.potion.Effect;
import org.cloudburstmc.api.potion.EffectTypes;
import org.cloudburstmc.math.vector.Vector2f;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDamageCause;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityEventType;
import org.cloudburstmc.protocol.bedrock.packet.AnimatePacket;
import org.cloudburstmc.protocol.bedrock.packet.EntityEventPacket;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.entity.passive.EntityWaterAnimal;
import org.cloudburstmc.server.level.Sound;
import org.cloudburstmc.server.math.BlockRayTrace;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudEntityRegistry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import static org.cloudburstmc.api.block.BlockTypes.AIR;
import static org.cloudburstmc.api.block.BlockTypes.MAGMA;
import static org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag.BREATHING;

public abstract class EntityLiving extends CloudEntity implements Living {

    private static final int HURT_COOLDOWN_TICKS = 10;
    private static final float DEFAULT_KNOCKBACK_STRENGTH = 1f;

    private boolean inPowderSnow;
    private int hurtCooldownTicks;
    private float lastDamageAmount;

    protected boolean invisible;
    protected int turtleTicks = 200;

    public EntityLiving(EntityType<?> type, Location location) {
        super(type, location);
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
    public <T extends Projectile> T launchProjectile(EntityType<T> type, Vector3f velocity, Consumer<? super T> configurator) {
        Objects.requireNonNull(type, "type");
        Location location = Location.from(this.getPosition().add(0, this.getEyeHeight() - 0.1f, 0),
                this.getYaw(), this.getPitch(), this.level);
        T projectile = CloudEntityRegistry.get().newEntity(type, location);
        projectile.setShooter(this);
        projectile.setMotion(velocity == null ? this.getDirectionVector() : velocity);
        if (configurator != null) {
            configurator.accept(projectile);
        }

        ProjectileLaunchEvent event = new ProjectileLaunchEvent(projectile);
        ((CloudEntity) projectile).spawn(event);
        return projectile;
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

    public boolean hasLineOfSight(Entity entity) {
        //todo
        return true;
    }

    public void collidingWith(Entity ent) { // can override (IronGolem|Bats)
        ent.onEntityCollision(this);
    }

    @Override
    public boolean isPushable() {
        return this.isAlive() && (!(this instanceof CloudPlayer player) || !player.isSpectator());
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

    @Override
    protected boolean applyDamage(EntityDamageEvent source) {
        if (this.isClosed() || !this.isAlive()) {
            return false;
        }

        Entity directEntity = source.getDamageSource().getDirectEntity();
        Entity causingEntity = source.getDamageSource().getCausingEntity();

        float incomingDamage = source.getDamage();
        boolean fullDamage = source.getDamageType().is(DamageTypeTags.BYPASSES_COOLDOWN) || (this.hurtCooldownTicks <= 0 && this.noDamageTicks <= 0);
        float cooldownAdjustedDamage = fullDamage ? incomingDamage : Math.max(incomingDamage - this.lastDamageAmount, 0);

        if (cooldownAdjustedDamage <= 0) {
            return false;
        }

        source.setDamage(cooldownAdjustedDamage);
        this.applyDamageReductions(source);

        if (!super.applyDamage(source)) {
            return false;
        }

        this.afterDamageApplied(source, cooldownAdjustedDamage);

        if (fullDamage) {
            Entity impactEntity = directEntity != null ? directEntity : causingEntity;
            if (impactEntity != null && !source.getDamageType().is(DamageTypeTags.NO_KNOCKBACK)) {
                if (impactEntity.isOnFire() && !(impactEntity instanceof CloudPlayer)) {
                    this.setOnFire(2 * this.server.getDifficulty().getId());
                }

                Vector2f diff = this.getPosition().sub(impactEntity.getPosition()).toVector2(true);
                this.knockBack(impactEntity, DEFAULT_KNOCKBACK_STRENGTH, diff.getX(), diff.getY());
            }

            if (!this.isInDeathSequence()) {
                this.broadcastHurtEvent(source.getDamageType().getDamageEffect());
            }

            this.hurtCooldownTicks = HURT_COOLDOWN_TICKS;
        }

        this.lastDamageAmount = incomingDamage;
        return true;
    }

    /**
     * Applies living-entity damage reductions in gameplay order.
     *
     * @param source the mutable damage event
     */
    protected void applyDamageReductions(EntityDamageEvent source) {
        if (source.getDamageType().is(DamageTypeTags.BYPASSES_EFFECTS) || source.getDamageType().is(DamageTypeTags.BYPASSES_RESISTANCE)) {
            return;
        }

        Effect resistanceEffect = this.getEffect(EffectTypes.RESISTANCE);
        if (resistanceEffect == null) {
            return;
        }

        int resistance = (resistanceEffect.getAmplifier() + 1) * 5;
        source.setDamage(source.getDamage() * Math.max(25 - resistance, 0) / 25f);
    }

    /**
     * Runs entity-specific effects after damage has been accepted.
     *
     * @param source                 the applied damage event
     * @param damageBeforeReductions damage after the hurt cooldown and before reductions
     */
    protected void afterDamageApplied(EntityDamageEvent source, float damageBeforeReductions) {
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

    protected float getKnockbackResistance() {
        return 0;
    }

    public void broadcastCriticalHit() {
        AnimatePacket animate = new AnimatePacket();
        animate.setAction(AnimatePacket.Action.CRITICAL_HIT);
        animate.setRuntimeEntityId(this.getRuntimeId());
        this.getLevel().addChunkPacket(this.getPosition(), animate);
        this.getLevel().addLevelSoundEvent(this.getPosition(), SoundEvent.ATTACK_CRITICAL);
    }

    protected EntityEventType getDeathEventType() {
        return EntityEventType.DEATH;
    }

    protected boolean isInDeathSequence() {
        return this.getHealth() <= 0;
    }

    @Override
    protected void onBelowLevel() {
        this.damage(4, DamageSource.of(DamageTypes.OUT_OF_WORLD));
    }

    public void knockBack(Entity attacker, float strength, float diffX, float diffZ) {
        float effectiveStrength = 0.4f * strength * (1 - this.getKnockbackResistance());
        if (effectiveStrength <= 0) {
            return;
        }

        float distanceSquared = diffX * diffX + diffZ * diffZ;
        if (distanceSquared < 1.0e-5f) {
            Vector2f direction = attacker.getDirectionPlane();
            diffX = direction.getX();
            diffZ = direction.getY();
            distanceSquared = diffX * diffX + diffZ * diffZ;
            if (distanceSquared < 1.0e-5f) {
                return;
            }
        }

        float inverseDistance = 1 / (float) Math.sqrt(distanceSquared);
        float pushX = diffX * inverseDistance * effectiveStrength;
        float pushZ = diffZ * inverseDistance * effectiveStrength;
        Vector3f currentMotion = this.getMotion();
        float verticalMotion = this.isOnGround() ? Math.min(0.4f, currentMotion.getY() / 2 + effectiveStrength) : currentMotion.getY();
        this.setMotion(Vector3f.from(currentMotion.getX() / 2 + pushX, verticalMotion, currentMotion.getZ() / 2 + pushZ));
    }

    @Override
    public void kill() {
        if (!this.isAlive()) {
            return;
        }
        super.kill();
        this.processDeath();
    }

    protected void processDeath() {
        EntityEventPacket packet = new EntityEventPacket();
        packet.setRuntimeEntityId(this.getRuntimeId());
        packet.setType(this.getDeathEventType());
        CloudServer.broadcastPacket(this.hasSpawned, packet);

        EntityDeathEvent ev = new EntityDeathEvent(this, this.getDrops());
        this.server.getEventManager().fire(ev);

        if (this.getLevel().getGameRules().get(GameRules.DO_ENTITY_DROPS)) {
            for (ItemStack item : ev.getDrops()) {
                this.getLevel().dropItem(this.getPosition(), item);
            }
        }
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

                var block = this.getLevel().getBlockState(this.getPosition().toInt()).getType();
                boolean ignore = block == BlockTypes.LADDER || block == BlockTypes.VINE || block == BlockTypes.WEB;
                if (ignore || this.hasEffect(EffectTypes.LEVITATION)) {
                    this.resetFallDistance();
                }

                if (!this.hasEffect(EffectTypes.WATER_BREATHING) && this.isInsideOfWater()) {
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
            float multiplier = CloudEntityRegistry.get()
                    .requireComponent(this.getType(), EntityComponents.GET_FREEZING_DAMAGE_MULTIPLIER)
                    .execute(this);
            if (this.damage(multiplier, DamageSource.of(DamageTypes.FREEZE)) && this instanceof CloudPlayer) {
                this.getLevel().addSound(this.getPosition(), Sound.MOB_PLAYER_HURT_FREEZE);
            }
        }
    }

    private boolean consumeInPowderSnow() {
        boolean result = this.inPowderSnow;
        this.inPowderSnow = false;
        return result;
    }

    public ItemStack[] getDrops() {
        return new ItemStack[0];
    }

    public Block[] getLineOfSight(int maxDistance) {
        return this.getLineOfSight(maxDistance, 0);
    }

    public Block[] getLineOfSight(int maxDistance, int maxLength) {
        return this.getLineOfSight(maxDistance, maxLength, new BlockType[0]);
    }

    public Block[] getLineOfSight(int maxDistance, int maxLength, BlockType[] transparent) {
        if (maxDistance > 120) {
            maxDistance = 120;
        }

        if (transparent != null && transparent.length == 0) {
            transparent = null;
        }

        List<Block> blocks = new ArrayList<>();

        Vector3f position = getPosition().add(0, this.getEyeHeight(), 0);
        for (Vector3i pos : BlockRayTrace.of(position, getDirectionVector(), maxDistance)) {
            Block block = this.getLevel().getLoadedBlock(pos);
            if (block == null) {
                break;
            }
            blocks.add(block);

            if (maxLength != 0 && blocks.size() > maxLength) {
                blocks.remove(0);
            }

            var id = block.getState().getType();

            if (transparent == null) {
                if (id != AIR) {
                    break;
                }
            } else {
                if (Arrays.binarySearch(transparent, id) < 0) {
                    break;
                }
            }
        }

        return blocks.toArray(new Block[0]);
    }

    public Block getTargetBlock(int maxDistance) {
        return getTargetBlock(maxDistance, new BlockType[0]);
    }

    public Block getTargetBlock(int maxDistance, BlockType[] transparent) {
        try {
            Block[] blocks = this.getLineOfSight(maxDistance, 1, transparent);
            Block block = blocks[0];
            if (block != null) {
                if (transparent != null && transparent.length != 0) {
                    if (Arrays.binarySearch(transparent, block.getState().getType()) < 0) {
                        return block;
                    }
                } else {
                    return block;
                }
            }
        } catch (Exception ignored) {

        }

        return null;
    }

    public int getAirTicks() {
        return this.data.get(EntityDataTypes.AIR_SUPPLY);
    }

    public void setAirTicks(int ticks) {
        this.data.set(EntityDataTypes.AIR_SUPPLY, (short) ticks);
    }
}
