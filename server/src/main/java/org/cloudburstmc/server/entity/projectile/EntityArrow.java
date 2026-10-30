package org.cloudburstmc.server.entity.projectile;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.damage.DamageSource;
import org.cloudburstmc.api.entity.damage.DamageTypes;
import org.cloudburstmc.api.entity.data.ArrowData;
import org.cloudburstmc.api.entity.data.EntityData;
import org.cloudburstmc.api.entity.projectile.Arrow;
import org.cloudburstmc.api.event.entity.PotionEffectCause;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.potion.PotionType;
import org.cloudburstmc.api.util.BlockHitResult;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.server.network.NetworkUtils;
import org.cloudburstmc.server.potion.CloudPotion;

import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

import static org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes.CUSTOM_DISPLAY;

public class EntityArrow extends EntityAbstractArrow implements Arrow {

    public EntityArrow(EntityType<Arrow> type, Location location) {
        super(type, location);
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.setItemStack(ItemStack.from(ItemTypes.ARROW));
    }

    @Override
    protected ArrowData data() {
        return (ArrowData) super.data();
    }

    @Override
    protected Class<? extends EntityData> minimumDataClass() {
        return ArrowData.class;
    }

    @Override
    protected void loadCustom(NbtMap tag) {
        // The item and ground ticks run before the abstract-arrow
        // load below, matching the historical load order. The item setter
        // re-runs to keep its validation, as the previous load did; the ground ticks
        // have a load default of zero, so they reset when absent.
        if (tag.containsKey("Item")) {
            ItemStack item = this.data().getItem();
            if (item != null) {
                this.setItemStack(item);
            }
        }
        this.data().setPotionGroundTicks(Math.max(0, getInt(tag, "PotionGroundTicks", 0)));
        super.loadCustom(tag);
    }

    @Override
    public ItemStack getItemStack() {
        return this.data().getItem();
    }

    @Override
    public void setItemStack(ItemStack item) {
        Objects.requireNonNull(item, "item");
        if (item.isEmpty() || item.getType() != ItemTypes.ARROW) {
            throw new IllegalArgumentException("Arrow projectiles must carry an arrow item");
        }

        PotionType potion = item.get(ItemDataComponents.POTION_TYPE);
        byte display = potion == null ? 0 : (byte) (NetworkUtils.potionToNetwork(potion) + 1);
        this.data().setItem(item.withCount(1));
        this.data().setPotionGroundTicks(0);
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

        PotionType potion = this.getItemStack().get(ItemDataComponents.POTION_TYPE);
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

        if (this.isEmbedded() && this.getItemStack().has(ItemDataComponents.POTION_TYPE)) {
            this.data().setPotionGroundTicks(this.data().getPotionGroundTicks() + tickDiff);
            if (this.data().getPotionGroundTicks() >= 600) {
                this.setItemStack(ItemStack.from(ItemTypes.ARROW));
                hasUpdate = true;
            }
        } else {
            this.data().setPotionGroundTicks(0);
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
