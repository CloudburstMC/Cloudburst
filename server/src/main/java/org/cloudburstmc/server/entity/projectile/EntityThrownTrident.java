package org.cloudburstmc.server.entity.projectile;

import org.cloudburstmc.api.block.BlockTags;
import org.cloudburstmc.api.enchantment.Enchantment;
import org.cloudburstmc.api.enchantment.EnchantmentType;
import org.cloudburstmc.api.enchantment.EnchantmentTypes;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.data.EntityData;
import org.cloudburstmc.api.entity.data.ThrownTridentData;
import org.cloudburstmc.api.entity.projectile.ThrownTrident;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.util.BlockHitResult;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudEnchantmentRegistry;

import java.util.Map;

public class EntityThrownTrident extends EntityAbstractArrow implements ThrownTrident {

    public EntityThrownTrident(EntityType<ThrownTrident> type, Location location) {
        super(type, location);
    }

    @Override
    public float getWidth() {
        return 0.05f;
    }

    @Override
    public float getLength() {
        return 0.5f;
    }

    @Override
    public float getHeight() {
        return 0.05f;
    }

    @Override
    public float getGravity() {
        return 0.04f;
    }

    @Override
    public float getDrag() {
        return 0.01f;
    }

    @Override
    protected float getWaterInertia() {
        return 0.99f;
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.data().setTrident(ItemStack.EMPTY);
    }

    @Override
    protected ThrownTridentData data() {
        return (ThrownTridentData) super.data();
    }

    @Override
    protected Class<? extends EntityData> minimumDataClass() {
        return ThrownTridentData.class;
    }

    @Override
    public ItemStack getTrident() {
        ItemStack trident = this.data().getTrident();
        return trident != null ? trident : ItemStack.EMPTY;
    }

    @Override
    public void setTrident(ItemStack item) {
        this.data().setTrident(item == null ? ItemStack.EMPTY : item);
    }

    @Override
    public int getResultDamage() {
        return (int) Math.ceil(this.getDamage());
    }

    @Override
    protected float getBaseDamage() {
        return 8;
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return !this.data().isDealtDamage() && super.canHitEntity(entity);
    }

    @Override
    public boolean onUpdate(int currentTick) {
        if (this.closed) {
            return false;
        }

        this.timing.startTiming();

        boolean hasUpdate = (this.isEmbedded() || this.data().isDealtDamage())
                && this.loyaltyLevel() > 0 && this.getOwner() instanceof CloudPlayer player
                && player.isOnline() && player.isAlive() && !player.isSpectator()
                ? this.returnTo(player, currentTick) : super.onUpdate(currentTick);

        if (this.onGround || this.hadCollision) {
            this.setCritical(false);
        }

        if (this.age > 1200) {
            this.close();
            hasUpdate = true;
        }

        this.timing.stopTiming();

        return hasUpdate;
    }

    private int loyaltyLevel() {
        return this.enchantmentLevel(EnchantmentTypes.LOYALTY);
    }

    private int enchantmentLevel(EnchantmentType type) {
        Enchantment enchantment = this.getTrident().getOrDefault(ItemDataComponents.ENCHANTMENTS, Map.of())
                .get(type);
        return enchantment == null ? 0 : enchantment.level();
    }

    private boolean returnTo(CloudPlayer owner, int currentTick) {
        int tickDiff = currentTick - this.lastUpdate;
        if (tickDiff <= 0) {
            return false;
        }

        this.lastUpdate = currentTick;
        this.entityBaseTick(tickDiff);
        if (!this.data.getFlag(EntityFlag.RETURN_TRIDENT)) {
            this.data.setFlag(EntityFlag.RETURN_TRIDENT, true);
            this.getLevel().addLevelSoundEvent(this.getPosition(), SoundEvent.ITEM_TRIDENT_RETURN);
        }

        Vector3f target = owner.getPosition().add(0, owner.getEyeHeight() * 0.5f, 0);
        Vector3f offset = target.sub(this.getPosition());
        if (offset.lengthSquared() < 2.25f) {
            this.motion = Vector3f.ZERO;
            owner.pickupEntity(this, true);
            this.flushEntityData();
            return true;
        }

        this.motion = this.motion.mul(0.95f).add(offset.normalize().mul(0.05f * this.loyaltyLevel()));
        this.setPosition(this.getPosition().add(this.motion));
        this.updateMovement();
        this.flushEntityData();
        return true;
    }

    @Override
    protected void onBlockCollision(BlockHitResult hit) {
        super.onBlockCollision(hit);
        this.getLevel().addLevelSoundEvent(hit.position(), SoundEvent.ITEM_TRIDENT_HIT_GROUND);
        if (hit.block().getState().is(BlockTags.LIGHTNING_RODS) && this.getLevel().canBlockSeeSky(hit.block().getPosition())) {
            this.channelLightning(hit.position());
        }
    }

    @Override
    protected void onCollideWithEntity(Entity entity) {
        float damage = CloudEnchantmentRegistry.get().modifyDamage(this.getTrident(), entity, this.getResultDamage());
        if (entity.damage(damage, this.createProjectileDamageSource())) {
            if (this.getOwner() != null) {
                CloudEnchantmentRegistry.get().applyPostAttackEffects(this.getTrident(), this.getOwner(), entity);
            }

            if (this.getLevel().canBlockSeeSky(entity.getPosition())) {
                this.channelLightning(entity.getPosition());
            }
        }

        this.getLevel().addLevelSoundEvent(this.getPosition(), SoundEvent.ITEM_TRIDENT_HIT);
        this.data().setDealtDamage(true);
        this.motion = Vector3f.from(-this.motion.getX() * 0.02f, -this.motion.getY() * 0.2f, -this.motion.getZ() * 0.02f);
        this.updateMovement();
    }

    private void channelLightning(Vector3f position) {
        if (this.enchantmentLevel(EnchantmentTypes.CHANNELING) > 0 && this.getLevel().isThundering() && this.getLevel().strikeLightning(position)) {
            this.getLevel().addLevelSoundEvent(position, SoundEvent.ITEM_TRIDENT_THUNDER);
        }
    }
}
