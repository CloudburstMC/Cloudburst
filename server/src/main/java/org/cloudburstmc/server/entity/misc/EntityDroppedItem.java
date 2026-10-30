package org.cloudburstmc.server.entity.misc;

import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.LiquidState;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.damage.DamageTypeTags;
import org.cloudburstmc.api.entity.damage.DamageTypes;
import org.cloudburstmc.api.entity.data.DroppedItemData;
import org.cloudburstmc.api.entity.data.EntityData;
import org.cloudburstmc.api.entity.misc.DroppedItem;
import org.cloudburstmc.api.event.entity.ItemDespawnEvent;
import org.cloudburstmc.api.item.ItemBehaviors;
import org.cloudburstmc.api.item.ItemDataComponents;
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
import org.cloudburstmc.server.event.entity.CloudEntityDamageEvent;
import org.cloudburstmc.server.item.ItemDisplayNameResolver;
import org.cloudburstmc.server.item.ItemUtils;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudItemRegistry;

import java.util.HashSet;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes.OWNER_EID;

public class EntityDroppedItem extends CloudEntity implements DroppedItem {

    private boolean fromFishing;

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
    public float getBaseOffset() {
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
    }

    @Override
    protected DroppedItemData data() {
        return (DroppedItemData) super.data();
    }

    @Override
    protected Class<? extends EntityData> minimumDataClass() {
        return DroppedItemData.class;
    }

    @Override
    protected void loadCustom(NbtMap tag) {
        // Migrated from applyData: health, age and owner run before the base load below,
        // matching the old applyData-before-loadCustom order. Each keeps its current value
        // when its key is absent, exactly as the old null checks did.
        if (tag.containsKey("Health")) {
            this.setHealth(getShort(tag, "Health", (short) this.getHealth()));
        }
        if (tag.containsKey("Age")) {
            this.age = getShort(tag, "Age", (short) this.age);
        }
        if (tag.containsKey("OwnerID")) {
            this.data.set(OWNER_EID, getLong(tag, "OwnerID", this.data.require(OWNER_EID)));
        }
        super.loadCustom(tag);
    }

    @Override
    protected void saveCustom(NbtMapBuilder tag) {
        super.saveCustom(tag);
        tag.putShort("Health", (short) this.getHealth());
        tag.putShort("Age", (short) this.age);
        tag.putLong("OwnerID", this.data.require(OWNER_EID));
    }

    @Override
    protected Set<String> customKeys() {
        Set<String> keys = new HashSet<>(super.customKeys());
        keys.add("Health");
        keys.add("Age");
        keys.add("OwnerID");
        return Set.copyOf(keys);
    }

    @Override
    protected boolean applyDamage(CloudEntityDamageEvent source) {
        return (source.getDamageType() == DamageTypes.OUT_OF_WORLD ||
                source.getDamageType() == DamageTypes.CACTUS ||
                source.getDamageType() == DamageTypes.ON_FIRE ||
                source.getDamageType().is(DamageTypeTags.IS_EXPLOSION) &&
                        !this.isInsideOfWater() && (this.getItem() == null ||
                        this.getItem().getType() != ItemTypes.NETHER_STAR)) && super.applyDamage(source);
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
            if (this.getItem().getCount() < CloudItemRegistry.get().requireComponent(getItem().getType(), ItemBehaviors.GET_MAX_STACK_SIZE).execute(getItem())) {
                for (Entity entity : this.getLevel().getNearbyEntities(this, getBoundingBox().inflate(1, 1, 1), false)) {
                    if (entity instanceof EntityDroppedItem) {
                        if (!entity.isAlive()) {
                            continue;
                        }
                        ItemStack closeItem = ((EntityDroppedItem) entity).getItem();
                        if (closeItem == null || !closeItem.isStackableWith(getItem())) {
                            continue;
                        }

                        if (!entity.isOnGround()) {
                            continue;
                        }

                        int newAmount = this.getItem().getCount() + closeItem.getCount();
                        if (newAmount > CloudItemRegistry.get().requireComponent(getItem().getType(), ItemBehaviors.GET_MAX_STACK_SIZE).execute(getItem())) {
                            continue;
                        }
                        entity.close();
                        this.data().setItem(getItem().withCount(newAmount));
                        EntityEventPacket packet = new EntityEventPacket();
                        packet.setRuntimeEntityId(this.getRuntimeId());
                        packet.setType(EntityEventType.UPDATE_ITEM_STACK_SIZE);
                        packet.setData(newAmount);
                        CloudServer.broadcastPacket(this.getViewers().toArray(new CloudPlayer[0]), packet);
                    }
                }
            }
        }

        this.entityBaseTick(tickDiff);

        if (isInsideOfFire()) {
            this.kill();
        }

        if (this.isAlive()) {
            if (this.getPickupDelay() > 0 && this.getPickupDelay() < 32767) {
                this.setPickupDelay(this.getPickupDelay() - tickDiff);
                if (this.getPickupDelay() < 0) {
                    this.setPickupDelay(0);
                }
            } else {
                for (Entity entity : this.level.getNearbyEntities(this, this.boundingBox.inflate(1, 0.5f, 1))) {
                    if (entity instanceof CloudPlayer) {
                        if (((CloudPlayer) entity).pickupEntity(this, true)) {
                            return true;
                        }
                    }
                }
            }

            Vector3f pos = this.getPosition();
            Block liquidBlock = this.level.getBlock(pos);
            LiquidState liquid = liquidBlock.getLiquid();
            float liquidHeight = liquid.getOwnHeight();
            if (!liquid.isEmpty() && liquid.isSameFamily(liquidBlock.up().getLiquid())) {
                liquidHeight = 1;
            }

            if (!liquid.isEmpty() && pos.getY() < liquidBlock.getY() + liquidHeight) {
                boolean water = liquid.getType().isSameFamily(org.cloudburstmc.api.block.LiquidTypes.WATER);
                float horizontalDrag = water ? 0.99f : 0.95f;
                this.motion = Vector3f.from(
                        this.motion.getX() * horizontalDrag,
                        this.motion.getY() + (this.motion.getY() < 0.06f ? 0.0005f : 0),
                        this.motion.getZ() * horizontalDrag
                );
            } else {
                this.motion = this.motion.sub(0, this.getGravity(), 0);
            }

            this.noPhysics = this.level.hasCollision(this, this.getBoundingBox().deflate(1.0E-7f, 1.0E-7f, 1.0E-7f));
            if (this.noPhysics) {
                this.moveTowardsClosestSpace(pos);
            }

            this.move(this.motion);

            double friction = 1 - this.getDrag();

            if (this.onGround && (Math.abs(this.motion.getX()) > 0.00001 || Math.abs(this.motion.getZ()) > 0.00001)) {
                BlockState block = this.getLevel().getBlockState(pos.add(0, -1, 0).toInt());
                friction *= block.getFriction();
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
                }
            }
        }

        this.timing.stopTiming();

        return this.isAlive();
    }

    @Override
    public String getName() {
        if (this.hasNameTag()) {
            return this.getNameTag();
        }

        String customName = this.getItem().get(ItemDataComponents.CUSTOM_NAME);
        return customName != null ? customName : super.getName();
    }

    @Override
    public Component displayName() {
        if (this.hasNameTag()) {
            return Component.text(this.getNameTag());
        }

        return ItemDisplayNameResolver.resolve(this.getItem());
    }

    public ItemStack getItem() {
        ItemStack item = this.data().getItem();
        return item == null || item.isEmpty() ? null : item;
    }

    @Override
    public void setItem(@NonNull ItemStack item) {
        checkNotNull(item, "item");
        checkArgument(!item.isEmpty(), "item must not be empty");
        this.data().setItem(item);
    }

    @Override
    public boolean canCollideWith(Entity entity) {
        return false;
    }

    public int getPickupDelay() {
        return this.data().getPickupDelay();
    }

    public void setPickupDelay(int pickupDelay) {
        this.data().setPickupDelay((short) pickupDelay);
    }

    public boolean isFromFishing() {
        return this.fromFishing;
    }

    public void setFromFishing(boolean fromFishing) {
        this.fromFishing = fromFishing;
    }

    @Override
    public BedrockPacket createAddEntityPacket() {
        Vector3f pos = this.getPosition();
        AddItemEntityPacket addEntity = new AddItemEntityPacket();
        addEntity.setUniqueEntityId(this.getUniqueId());
        addEntity.setRuntimeEntityId(this.getRuntimeId());
        addEntity.setPosition(Vector3f.from(pos.getX(), pos.getY() + this.getBaseOffset(), pos.getZ()));
        addEntity.setMotion(this.getMotion());
        addEntity.setFromFishing(this.fromFishing);
        addEntity.getMetadata().putAll(this.data.snapshot());
        addEntity.setItemInHand(ItemUtils.toNetwork(this.getItem()));
        return addEntity;
    }

    @Override
    public boolean canTriggerPressurePlate() {
        return true;
    }
}
