package org.cloudburstmc.server.entity.impl.projectile;

import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import com.nukkitx.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.entity.EntityType;
import org.cloudburstmc.server.entity.projectile.ThrownTrident;
import org.cloudburstmc.server.event.entity.EntityDamageByChildEntityEvent;
import org.cloudburstmc.server.event.entity.EntityDamageByEntityEvent;
import org.cloudburstmc.server.event.entity.EntityDamageEvent;
import org.cloudburstmc.server.event.entity.ProjectileHitEvent;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.ItemUtils;
import org.cloudburstmc.server.level.Location;
import org.cloudburstmc.server.level.MovingObjectPosition;

import java.util.concurrent.ThreadLocalRandom;

import static com.nukkitx.protocol.bedrock.data.entity.EntityFlag.CRITICAL;
import static org.cloudburstmc.server.block.BlockTypes.AIR;

/**
 * Created by PetteriM1
 */
public class EntityThrownTrident extends EntityProjectile implements ThrownTrident {

    protected ItemStack trident;
    protected float gravity = 0.04f;
    protected float drag = 0.01f;

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
    protected void initEntity() {
        super.initEntity();

        this.damage = 8;
        this.trident = ItemStack.get(AIR);
        closeOnCollide = false;
    }

    @Override
    public void loadAdditionalData(NbtMap tag) {
        super.loadAdditionalData(tag);

        tag.listenForCompound("Trident", itemTag -> this.trident = ItemUtils.deserializeItem(itemTag));
    }

    @Override
    public void saveAdditionalData(NbtMapBuilder tag) {
        super.saveAdditionalData(tag);

        tag.putCompound("Trident", ItemUtils.serializeItem(this.trident));
    }

    @Override
    public ItemStack getTrident() {
        return this.trident != null ? this.trident : ItemStack.get(AIR);
    }

    @Override
    public void setTrident(ItemStack item) {
        this.trident = item;
    }

    public void setCritical() {
        this.setCritical(true);
    }

    public boolean isCritical() {
        return this.data.getFlag(CRITICAL);
    }

    public void setCritical(boolean value) {
        this.data.setFlag(CRITICAL, value);
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
        return 8;
    }

    @Override
    public boolean onUpdate(int currentTick) {
        if (this.closed) {
            return false;
        }

        this.timing.startTiming();

        if (this.isCollided && !this.hadCollision) {
            this.getLevel().addLevelSoundEvent(this.getPosition(), SoundEvent.ITEM_TRIDENT_HIT_GROUND);
        }

        boolean hasUpdate = super.onUpdate(currentTick);

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

    @Override
    public void onCollideWithEntity(Entity entity) {
        this.server.getEventManager().fire(new ProjectileHitEvent(this, MovingObjectPosition.fromEntity(entity)));
        float damage = this.getResultDamage();

        EntityDamageEvent ev;
        if (this.getOwner() == null) {
            ev = new EntityDamageByEntityEvent(this, entity, EntityDamageEvent.DamageCause.PROJECTILE, damage);
        } else {
            ev = new EntityDamageByChildEntityEvent(this.getOwner(), this, entity, EntityDamageEvent.DamageCause.PROJECTILE, damage);
        }
        entity.attack(ev);
        this.getLevel().addLevelSoundEvent(this.getPosition(), SoundEvent.ITEM_TRIDENT_HIT);
        this.hadCollision = true;
//        this.close();
//        EntityThrownTrident newTrident = create(this);
//        newTrident.setTrident(this.trident);
//        newTrident.spawnToAll();
    }
}
