package org.cloudburstmc.server.entity.projectile;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.damage.DamageSource;
import org.cloudburstmc.api.entity.damage.DamageTypes;
import org.cloudburstmc.api.entity.projectile.Arrow;
import org.cloudburstmc.api.event.entity.PotionEffectCause;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.potion.PotionType;
import org.cloudburstmc.api.util.BlockHitResult;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.server.item.ItemUtils;
import org.cloudburstmc.server.network.NetworkUtils;
import org.cloudburstmc.server.potion.CloudPotion;

import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

import static org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes.CUSTOM_DISPLAY;

public class EntityArrow extends EntityAbstractArrow implements Arrow {

    private ItemStack itemStack = ItemStack.from(ItemTypes.ARROW);
    private int potionGroundTicks;

    public EntityArrow(EntityType<Arrow> type, Location location) {
        super(type, location);
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.setItemStack(this.itemStack);
    }

    @Override
    public void loadAdditionalData(NbtMap tag) {
        super.loadAdditionalData(tag);
        tag.listenForCompound("Item", item -> this.setItemStack(ItemUtils.deserializeItem(item)));
        this.potionGroundTicks = Math.max(0, tag.getInt("PotionGroundTicks"));
    }

    @Override
    public void saveAdditionalData(NbtMapBuilder tag) {
        super.saveAdditionalData(tag);
        tag.putCompound("Item", ItemUtils.serializeItem(this.itemStack));
        tag.putInt("PotionGroundTicks", this.potionGroundTicks);
    }

    @Override
    public ItemStack getItemStack() {
        return this.itemStack;
    }

    @Override
    public void setItemStack(ItemStack item) {
        Objects.requireNonNull(item, "item");
        if (item.isEmpty() || item.getType() != ItemTypes.ARROW) {
            throw new IllegalArgumentException("Arrow projectiles must carry an arrow item");
        }

        PotionType potion = item.get(ItemDataComponents.POTION_TYPE);
        byte display = potion == null ? 0 : (byte) (NetworkUtils.potionToNetwork(potion) + 1);
        this.itemStack = item.withCount(1);
        this.potionGroundTicks = 0;
        this.data.set(CUSTOM_DISPLAY, display);
    }

    @Override
    public float getWidth() {
        return 0.5f;
    }

    @Override
    public float getLength() {
        return 0.5f;
    }

    @Override
    public float getHeight() {
        return 0.5f;
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
    public int getResultDamage() {
        int base = super.getResultDamage();

        if (this.isCritical()) {
            base += ThreadLocalRandom.current().nextInt(base / 2 + 2);
        }

        return base;
    }

    @Override
    protected float getBaseDamage() {
        return 2;
    }

    @Override
    protected boolean damageEntity(Entity entity) {
        if (!super.damageEntity(entity)) {
            return false;
        }

        PotionType potion = this.itemStack.get(ItemDataComponents.POTION_TYPE);
        if (potion != null) {
            Entity owner = this.getOwner();
            DamageSource.Builder source = DamageSource.builder(DamageTypes.INDIRECT_MAGIC).directEntity(this);
            if (owner != null) {
                source.causingEntity(owner);
            }

            new CloudPotion(potion).apply(entity, 1.0, 0.125f, source.build(), owner == null ? this : owner, PotionEffectCause.ARROW);
        }

        return true;
    }

    @Override
    protected void onBlockCollision(BlockHitResult hit) {
        super.onBlockCollision(hit);
        this.getLevel().addLevelSoundEvent(hit.position(), SoundEvent.BOW_HIT);
    }

    @Override
    public boolean onUpdate(int currentTick) {
        if (this.closed) {
            return false;
        }

        this.timing.startTiming();

        int tickDiff = Math.max(0, currentTick - this.lastUpdate);
        boolean hasUpdate = super.onUpdate(currentTick);

        if (this.isEmbedded() && this.itemStack.has(ItemDataComponents.POTION_TYPE)) {
            this.potionGroundTicks += tickDiff;
            if (this.potionGroundTicks >= 600) {
                this.setItemStack(ItemStack.from(ItemTypes.ARROW));
                hasUpdate = true;
            }
        } else {
            this.potionGroundTicks = 0;
        }

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
}
