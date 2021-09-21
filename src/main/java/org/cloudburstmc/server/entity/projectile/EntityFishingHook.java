package org.cloudburstmc.server.entity.projectile;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.entity.EntityEventType;
import com.nukkitx.protocol.bedrock.packet.EntityEventPacket;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.misc.DroppedItem;
import org.cloudburstmc.api.entity.projectile.FishingHook;
import org.cloudburstmc.api.event.entity.EntityDamageByChildEntityEvent;
import org.cloudburstmc.api.event.entity.EntityDamageByEntityEvent;
import org.cloudburstmc.api.event.entity.EntityDamageEvent;
import org.cloudburstmc.api.event.entity.ProjectileHitEvent;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.util.MovingObjectPosition;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.item.randomitem.Fishing;
import org.cloudburstmc.server.level.particle.BubbleParticle;
import org.cloudburstmc.server.level.particle.WaterParticle;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.EntityRegistry;

import javax.annotation.Nullable;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;


/**
 * Created by PetteriM1
 */
public class EntityFishingHook extends EntityProjectile implements FishingHook {

    public static final int WAIT_CHANCE = 120;
    public static final int CHANCE = 40;

    public boolean chance = false;
    public int waitChance = WAIT_CHANCE * 2;
    public boolean attracted = false;
    public int attractTimer = 0;
    public boolean caught = false;
    public int coughtTimer = 0;

    public Vector3f fish = null;

    private ItemStack rod;

    public EntityFishingHook(EntityType<FishingHook> type, Location location) {
        super(type, location);
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        if (this.age > 0) {
            this.close();
        }
    }

    @Override
    public float getWidth() {
        return 0.2f;
    }

    @Override
    public float getLength() {
        return 0.2f;
    }

    @Override
    public float getHeight() {
        return 0.2f;
    }

    @Override
    public float getGravity() {
        return 0.07f;
    }

    @Override
    public float getDrag() {
        return 0.05f;
    }

    @Nullable
    public ItemStack getRod() {
        return rod;
    }

    public void setRod(@Nullable ItemStack rod) {
        this.rod = rod;
    }

    @Override
    public boolean onUpdate(int currentTick) {
        boolean hasUpdate = super.onUpdate(currentTick);
        if (hasUpdate) {
            return false;
        }

        if (this.isInsideOfWater()) {
            this.motion = Vector3f.from(0, getGravity() * -0.04, 0);
            hasUpdate = true;
        } else if (this.isCollided && this.keepMovement) {
            this.motion = Vector3f.ZERO;
            this.keepMovement = false;
            hasUpdate = true;
        }

        Random random = new Random();

        if (this.isInsideOfWater()) {
            if (!this.attracted) {
                if (this.waitChance > 0) {
                    --this.waitChance;
                }
                if (this.waitChance == 0) {
                    if (random.nextInt(100) < 90) {
                        this.attractTimer = (random.nextInt(40) + 20);
                        this.spawnFish();
                        this.caught = false;
                        this.attracted = true;
                    } else {
                        this.waitChance = WAIT_CHANCE;
                    }
                }
            } else if (!this.caught) {
                if (this.attractFish()) {
                    this.coughtTimer = (random.nextInt(20) + 30);
                    this.fishBites();
                    this.caught = true;
                }
            } else {
                if (this.coughtTimer > 0) {
                    --this.coughtTimer;
                }
                if (this.coughtTimer == 0) {
                    this.attracted = false;
                    this.caught = false;
                    this.waitChance = WAIT_CHANCE * 3;
                }
            }
        }

        return hasUpdate;
    }

    public int getWaterHeight() {
        for (int y = this.getPosition().getFloorY(); y < 256; y++) {
            var id = this.getLevel().getBlockState(getPosition().getFloorX(), y, getPosition().getFloorZ()).getType();
            if (id == BlockTypes.AIR) {
                return y;
            }
        }
        return this.getPosition().getFloorY();
    }

    public void fishBites() {
        EntityEventPacket hookPacket = new EntityEventPacket();
        hookPacket.setRuntimeEntityId(this.getRuntimeId());
        hookPacket.setType(EntityEventType.FISH_HOOK_TIME);
        CloudServer.broadcastPacket(this.getViewers(), hookPacket);

        EntityEventPacket bubblePacket = new EntityEventPacket();
        bubblePacket.setRuntimeEntityId(this.getRuntimeId());
        bubblePacket.setType(EntityEventType.FISH_HOOK_BUBBLE);
        CloudServer.broadcastPacket(this.getViewers(), bubblePacket);

        EntityEventPacket teasePacket = new EntityEventPacket();
        teasePacket.setRuntimeEntityId(this.getRuntimeId());
        teasePacket.setType(EntityEventType.FISH_HOOK_TEASE);
        CloudServer.broadcastPacket(this.getViewers(), teasePacket);

        Random random = ThreadLocalRandom.current();
        for (int i = 0; i < 5; i++) {
            this.getLevel().addParticle(new BubbleParticle(Vector3f.from(
                    this.getX() + random.nextDouble() * 0.5 - 0.25,
                    this.getWaterHeight(),
                    this.getZ() + random.nextDouble() * 0.5 - 0.25
            )));
        }
    }

    public void spawnFish() {
        Random random = new Random();
        this.fish = Vector3f.from(
                this.getX() + (random.nextDouble() * 1.2 + 1) * (random.nextBoolean() ? -1 : 1),
                this.getWaterHeight(),
                this.getZ() + (random.nextDouble() * 1.2 + 1) * (random.nextBoolean() ? -1 : 1)
        );
    }

    public boolean attractFish() {
        double multiply = 0.1;
        this.fish = Vector3f.from(
                this.fish.getX() + (this.getX() - this.fish.getX()) * multiply,
                this.fish.getY(),
                this.fish.getZ() + (this.getZ() - this.fish.getZ()) * multiply
        );
        if (new Random().nextInt(100) < 85) {
            this.getLevel().addParticle(new WaterParticle(this.fish));
        }
        double dist = Math.abs(Math.sqrt(this.getX() * this.getX() + this.getZ() * this.getZ()) - Math.sqrt(this.fish.getX() * this.fish.getX() + this.fish.getZ() * this.fish.getZ()));
        return dist < 0.15;
    }

    public void reelLine() {
        Entity owner = this.getOwner();
        if (owner instanceof CloudPlayer && this.caught) {
            ItemStack item = Fishing.getFishingResult(this.rod);
            int experience = new Random().nextInt((3 - 1) + 1) + 1;
            Vector3f motion;

            motion = owner.getPosition().sub(this.getPosition()).mul(0.1);
            motion = motion.add(0, Math.sqrt(owner.getPosition().distance(this.getPosition())) * 0.08, 0);

            DroppedItem droppedItem = EntityRegistry.get().newEntity(EntityTypes.ITEM, this.getLocation());
            droppedItem.setMotion(motion);
            droppedItem.setHealth(5);
            droppedItem.setItem(item);
            droppedItem.setPickupDelay(1);
            droppedItem.setOwner(owner);
            droppedItem.spawnToAll();

            CloudPlayer player = (CloudPlayer) owner;
            if (experience > 0) {
                player.addExperience(experience);
            }
        }
        if (owner instanceof CloudPlayer) {
            EntityEventPacket packet = new EntityEventPacket();
            packet.setRuntimeEntityId(this.getRuntimeId());
            packet.setType(EntityEventType.FISH_HOOK_TEASE);
            CloudServer.broadcastPacket(this.getViewers(), packet);
        }
        if (!this.closed) {
            this.close();
        }
    }

    @Override
    public void onCollideWithEntity(Entity entity) {
        this.server.getEventManager().fire(new ProjectileHitEvent(this, MovingObjectPosition.fromEntity(entity)));
        float damage = this.getResultDamage();

        Entity owner = this.getOwner();

        EntityDamageEvent ev;
        if (owner == null) {
            ev = new EntityDamageByEntityEvent(this, entity, EntityDamageEvent.DamageCause.PROJECTILE, damage);
        } else {
            ev = new EntityDamageByChildEntityEvent(owner, this, entity, EntityDamageEvent.DamageCause.PROJECTILE, damage);
        }

        entity.attack(ev);
    }

    @Override
    public boolean isCritical() {
        return false;
    }

    @Override
    public void setCritical(boolean critical) {
        // no-op
    }
}
