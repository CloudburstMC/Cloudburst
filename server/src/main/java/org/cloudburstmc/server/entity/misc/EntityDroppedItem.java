package org.cloudburstmc.server.entity.misc;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.api.block.BlockBehaviors;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.misc.DroppedItem;
import org.cloudburstmc.api.event.entity.EntityDamageEvent;
import org.cloudburstmc.api.event.entity.ItemDespawnEvent;
import org.cloudburstmc.api.event.entity.ItemSpawnEvent;
import org.cloudburstmc.api.item.ItemBehaviors;
import org.cloudburstmc.api.item.ItemKeys;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityEventType;
import org.cloudburstmc.protocol.bedrock.packet.AddItemEntityPacket;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.EntityEventPacket;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.item.ItemUtils;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudBlockRegistry;
import org.cloudburstmc.server.registry.CloudItemRegistry;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.cloudburstmc.api.block.BlockTypes.FLOWING_WATER;
import static org.cloudburstmc.api.block.BlockTypes.WATER;
import static org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes.OWNER_EID;

/**
 * @author MagicDroidX
 */
public class EntityDroppedItem extends CloudEntity implements DroppedItem {

    protected ItemStack item;
    protected int pickupDelay;

    public EntityDroppedItem(EntityType<DroppedItem> type, Location location) {
        super(type, location);
    }

    @Override
    public float getWidth() {
        return 0.25f;
    }

    @Override
    public float getLength() {
        return 0.25f;
    }

    @Override
    public float getHeight() {
        return 0.25f;
    }

    @Override
    public float getGravity() {
        return 0.04f;
    }

    @Override
    public float getDrag() {
        return 0.02f;
    }

    @Override
    protected float getBaseOffset() {
        return 0.125f;
    }

    @Override
    public boolean canCollide() {
        return false;
    }

    @Override
    protected void initEntity() {
        super.initEntity();

        this.setMaxHealth(5);

        this.server.getEventManager().fire(new ItemSpawnEvent(this));
    }

    @Override
    public void loadAdditionalData(NbtMap tag) {
        super.loadAdditionalData(tag);

        tag.listenForShort("Health", this::setHealth);
        tag.listenForShort("PickupDelay", this::setPickupDelay);
        tag.listenForShort("Age", v -> this.age = v);
        tag.listenForLong("OwnerID", v -> this.data.set(OWNER_EID, v));
        tag.listenForCompound("Item", itemTag -> this.item = ItemUtils.deserializeItem(itemTag));
    }

    @Override
    public void saveAdditionalData(NbtMapBuilder tag) {
        super.saveAdditionalData(tag);

        tag.putShort("Health", (short) this.getHealth());
        tag.putShort("PickupDelay", (short) this.pickupDelay);
        tag.putShort("Age", (short) this.age);
        tag.putLong("OwnerID", this.data.get(OWNER_EID));
        tag.putCompound("Item", ItemUtils.serializeItem(this.item));
    }

    @Override
    public boolean attack(EntityDamageEvent source) {
        return (source.getCause() == EntityDamageEvent.DamageCause.VOID ||
                source.getCause() == EntityDamageEvent.DamageCause.CONTACT ||
                source.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK ||
                (source.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION ||
                        source.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) &&
                        !this.isInsideOfWater() && (this.item == null ||
                        this.item.getType() != ItemTypes.NETHER_STAR)) && super.attack(source);
    }

    @Override
    public boolean onUpdate(int currentTick) {
        if (this.closed) {
            return false;
        }

        int tickDiff = currentTick - this.lastUpdate;

        if (tickDiff <= 0 && !this.justCreated) {
            return true;
        }

        this.lastUpdate = currentTick;

        this.timing.startTiming();

        if (this.age % 60 == 0 && this.onGround && this.getItem() != null && this.isAlive()) {
            if (this.getItem().getCount() < CloudItemRegistry.get().getBehavior(getItem().getType(), ItemBehaviors.GET_MAX_STACK_SIZE).execute()) {
                for (Entity entity : this.getLevel().getNearbyEntities(getBoundingBox().grow(1, 1, 1), this, false)) {
                    if (entity instanceof EntityDroppedItem) {
                        if (!entity.isAlive()) {
                            continue;
                        }
                        ItemStack closeItem = ((EntityDroppedItem) entity).getItem();
                        if (!closeItem.isCombinable(getItem())) {
                            continue;
                        }
                        if (!entity.isOnGround()) {
                            continue;
                        }
                        int newAmount = this.getItem().getCount() + closeItem.getCount();
                        if (newAmount > CloudItemRegistry.get().getBehavior(getItem().getType(), ItemBehaviors.GET_MAX_STACK_SIZE).execute()) {
                            continue;
                        }
                        entity.close();
                        this.item = getItem().withCount(newAmount);
                        EntityEventPacket packet = new EntityEventPacket();
                        packet.setRuntimeEntityId(this.getRuntimeId());
                        packet.setType(EntityEventType.UPDATE_ITEM_STACK_SIZE);
                        packet.setData(newAmount);
                        CloudServer.broadcastPacket(this.getViewers().toArray(new CloudPlayer[0]), packet);
                    }
                }
            }
        }

        boolean hasUpdate = this.entityBaseTick(tickDiff);

        if (isInsideOfFire()) {
            this.kill();
        }

        if (this.isAlive()) {
            if (this.pickupDelay > 0 && this.pickupDelay < 32767) {
                this.pickupDelay -= tickDiff;
                if (this.pickupDelay < 0) {
                    this.pickupDelay = 0;
                }
            } else {
                for (Entity entity : this.level.getNearbyEntities(this.boundingBox.grow(1, 0.5f, 1), this)) {
                    if (entity instanceof CloudPlayer) {
                        if (((CloudPlayer) entity).pickupEntity(this, true)) {
                            return true;
                        }
                    }
                }
            }

            Vector3f pos = this.getPosition();
            var b = this.level.getBlockState(pos.getFloorX(), pos.getFloorY(), pos.getFloorZ()).getType();

            if (b == FLOWING_WATER || b == WATER) {
                this.motion = Vector3f.from(this.motion.getX(), this.getGravity() - 0.06, this.motion.getZ());
            } else if (!this.isOnGround()) {
                this.motion = this.motion.sub(0, this.getGravity(), 0);
            }

            if (this.checkObstruction(pos)) {
                hasUpdate = true;
            }

            this.move(this.motion);

            double friction = 1 - this.getDrag();

            if (this.onGround && (Math.abs(this.motion.getX()) > 0.00001 || Math.abs(this.motion.getZ()) > 0.00001)) {
                var block = this.getLevel().getBlockState(pos.add(0, -1, -1).toInt());
                friction *= CloudBlockRegistry.REGISTRY.getBehavior(block.getType(), BlockBehaviors.GET_FRICTION).execute(block);
            }

            this.motion = this.motion.mul(friction, 1 - this.getDrag(), friction);

            if (this.onGround) {
                this.motion = this.motion.mul(1, -0.5, 1);
            }

            this.updateMovement();

            if (this.age > 6000) {
                ItemDespawnEvent ev = new ItemDespawnEvent(this);
                this.server.getEventManager().fire(ev);
                if (ev.isCancelled()) {
                    this.age = 0;
                } else {
                    this.kill();
                    hasUpdate = true;
                }
            }
        }

        this.timing.stopTiming();

        return hasUpdate || !this.onGround || this.motion.length() > 0.00001;
    }

    @Override
    public String getName() {
        return this.hasNameTag() ? this.getNameTag() : this.item.get(ItemKeys.CUSTOM_NAME);
    }

    public ItemStack getItem() {
        return item;
    }

    @Override
    public void setItem(@NonNull ItemStack item) {
        checkNotNull(item, "item");
        checkArgument(this.item == null, "Item has already been set");
        this.item = item;
    }

    @Override
    public boolean canCollideWith(Entity entity) {
        return false;
    }

    public int getPickupDelay() {
        return pickupDelay;
    }

    public void setPickupDelay(int pickupDelay) {
        this.pickupDelay = pickupDelay;
    }

    @Override
    public BedrockPacket createAddEntityPacket() {
        AddItemEntityPacket addEntity = new AddItemEntityPacket();
        addEntity.setUniqueEntityId(this.getUniqueId());
        addEntity.setRuntimeEntityId(this.getRuntimeId());
        addEntity.setPosition(this.getPosition());
        addEntity.setMotion(this.getMotion());
        this.data.putAllIn(addEntity.getMetadata());
        addEntity.setItemInHand(ItemUtils.toNetwork(this.getItem()));
        return addEntity;
    }

    @Override
    public boolean canTriggerPressurePlate() {
        return true;
    }
}
