package org.cloudburstmc.server.entity.misc;

import lombok.Setter;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.LiquidState;
import org.cloudburstmc.api.block.LiquidTypes;
import org.cloudburstmc.api.blockentity.Bell;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.damage.DamageTypeTags;
import org.cloudburstmc.api.entity.damage.DamageTypes;
import org.cloudburstmc.api.entity.misc.DroppedItem;
import org.cloudburstmc.api.event.entity.EntityRemoveCause;
import org.cloudburstmc.api.event.entity.ItemDespawnEvent;
import org.cloudburstmc.api.item.ItemBehaviors;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.item.data.ShulkerBoxContents;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityEventType;
import org.cloudburstmc.protocol.bedrock.packet.AddItemEntityPacket;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.EntityEventPacket;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.entity.data.CloudDroppedItemData;
import org.cloudburstmc.server.event.entity.CloudEntityDamageEvent;
import org.cloudburstmc.server.item.ItemDisplayNameResolver;
import org.cloudburstmc.server.item.ItemStackPacketCodec;
import org.cloudburstmc.server.item.ShulkerBoxItems;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudItemRegistry;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes.OWNER_EID;

public class EntityDroppedItem extends CloudEntity implements DroppedItem {

    private static final int LIFETIME_TICKS = 6000;
    private static final int NEVER_PICK_UP = 32767;

    @Setter
    private boolean fromFishing;

    private final CloudDroppedItemData persistenceState;

    public EntityDroppedItem(EntityType<DroppedItem> type, Location location) {
        CloudDroppedItemData state = new CloudDroppedItemData();
        super(type, location, state);
        this.persistenceState = state;
    }

    @Override
    protected CloudDroppedItemData data() {
        return this.persistenceState;
    }

    @Override
    public ItemStack getItem() {
        return this.data().getItem();
    }

    @Override
    public int getPickupDelay() {
        return this.data().getPickupDelay();
    }

    @Override
    public void setPickupDelay(int pickupDelay) {
        this.data().setPickupDelay(Math.clamp(pickupDelay, 0, NEVER_PICK_UP));
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

        this.setDefaultMaxHealth(5);
    }

    @Override
    protected void loadCustom(NbtMap tag) {
        super.loadCustom(tag);

        this.setPickupDelay(this.getPickupDelay());
        tag.listenForShort("Health", this::setHealth);
        tag.listenForShort("Age", v -> this.age = v);
        tag.listenForLong("OwnerID", v -> this.data.set(OWNER_EID, v));
    }

    @Override
    protected void saveCustom(NbtMapBuilder tag) {
        super.saveCustom(tag);

        tag.putShort("Health", (short) this.getHealth());
        tag.putShort("Age", (short) this.age);
        tag.putLong("OwnerID", this.data.require(OWNER_EID));
    }

    @Override
    protected boolean applyDamage(CloudEntityDamageEvent event) {
        boolean alive = this.isAlive();
        boolean applied = (event.getDamageType() == DamageTypes.OUT_OF_WORLD ||
                event.getDamageType() == DamageTypes.CACTUS ||
                event.getDamageType() == DamageTypes.ON_FIRE ||
                event.getDamageType().is(DamageTypeTags.IS_EXPLOSION) &&
                        !this.isInsideOfWater() && (this.data().getItem() == null ||
                        this.data().getItem().getType() != ItemTypes.NETHER_STAR)) && super.applyDamage(event);
        if (applied && alive && !this.isAlive() && event.getDamageType() != DamageTypes.OUT_OF_WORLD) {
            this.releaseShulkerContents();
        }

        return applied;
    }

    private void releaseShulkerContents() {
        if (this.data().getItem() == null || !ShulkerBoxItems.TYPES.contains(this.data().getItem().getType())) {
            return;
        }

        ShulkerBoxContents contents = this.data().getItem().get(ItemDataComponents.SHULKER_BOX_CONTENTS);
        if (contents != null) {
            for (ItemStack stack : contents.items().values()) {
                this.getLevel().dropItem(this.getPosition(), stack);
            }
        }
    }

    @Override
    protected boolean tickEntity(int currentTick, int elapsedTicks) {
        if (this.isClosed()) {
            return false;
        }

        ItemStack item = this.getItem();
        if (item == null || item.isEmpty()) {
            this.remove(EntityRemoveCause.DESPAWN);
            return false;
        }

        if (this.age % 60 == 0 && this.isOnGround() && this.isAlive() && !this.hasExpired() && this.getPickupDelay() != NEVER_PICK_UP) {
            if (this.getItem().getCount() < CloudItemRegistry.get().requireComponent(getItem().getType(), ItemBehaviors.GET_MAX_STACK_SIZE).execute(getItem())) {
                for (Entity entity : this.getLevel().getNearbyEntities(this, getBoundingBox().inflate(1, 1, 1), false)) {
                    if (entity instanceof EntityDroppedItem other && other != this) {
                        if (!other.isAlive() || other.hasExpired() || other.getPickupDelay() == NEVER_PICK_UP) {
                            continue;
                        }

                        ItemStack closeItem = other.getItem();
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

                        int pickupDelay = Math.max(this.getPickupDelay(), other.getPickupDelay());
                        int age = Math.min(this.age, other.age);
                        other.remove(EntityRemoveCause.MERGE);
                        this.data().setItem(getItem().withCount(newAmount));
                        this.setPickupDelay(pickupDelay);
                        this.age = age;

                        EntityEventPacket packet = new EntityEventPacket();
                        packet.setRuntimeEntityId(this.getRuntimeId());
                        packet.setType(EntityEventType.UPDATE_ITEM_STACK_SIZE);
                        packet.setData(newAmount);
                        this.broadcastToViewers(packet);
                    }
                }
            }
        }

        this.entityBaseTick(elapsedTicks);

        if (this.isAlive() && !this.isInvulnerable() && this.isInsideOfFire()) {
            this.kill();
            this.releaseShulkerContents();
        }

        if (this.isAlive()) {
            int pickupDelay = this.getPickupDelay();
            if (pickupDelay > 0 && pickupDelay != NEVER_PICK_UP) {
                this.setPickupDelay(Math.max(0, pickupDelay - elapsedTicks));
            } else if (pickupDelay <= 0) {
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
                boolean water = liquid.getType().isSameFamily(LiquidTypes.WATER);
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

            boolean wasOnGround = this.isOnGround();
            this.move(this.motion);

            if (!wasOnGround && this.isOnGround() && this.level.getLoadedBlockEntity(this.getLandingBlockPosition()) instanceof Bell bell) {
                bell.ring(this, null);
            }

            double friction = 1 - this.getDrag();

            if (this.isOnGround() && (Math.abs(this.motion.getX()) > 0.00001 || Math.abs(this.motion.getZ()) > 0.00001)) {
                BlockState block = this.getLevel().getBlockState(pos.add(0, -1, 0).toInt());
                friction *= block.getFriction();
            }

            this.motion = this.motion.mul(friction, 1 - this.getDrag(), friction);

            if (this.isOnGround()) {
                this.motion = this.motion.mul(1, -0.5, 1);
            }

            this.updateMovement();

            if (this.hasExpired()) {
                this.expire();
            }
        }

        return this.isAlive();
    }

    private boolean hasExpired() {
        return this.age >= LIFETIME_TICKS;
    }

    private void expire() {
        ItemDespawnEvent event = new ItemDespawnEvent(this, this.getLocation());
        this.server.getEventManager().fire(event);
        if (this.isClosed()) {
            return;
        }

        if (event.isCancelled()) {
            this.age = 0;
            this.markChunkDirty();
        } else {
            this.remove(EntityRemoveCause.DESPAWN);
        }
    }

    @Override
    protected String getDefaultName() {
        String customName = this.data().getItem().get(ItemDataComponents.CUSTOM_NAME);
        return customName != null ? customName : super.getDefaultName();
    }

    @Override
    public Component displayName() {
        Component name = this.customName();
        if (name != null) {
            return name;
        }

        return ItemDisplayNameResolver.resolve(this.data().getItem());
    }

    @Override
    public void setItem(@NonNull ItemStack item) {
        checkNotNull(item, "item");
        checkArgument(!item.isEmpty(), "item must not be empty");
        if (item.equals(this.data().getItem())) {
            return;
        }
        this.data().setItem(item);
        for (CloudPlayer viewer : this.getViewers()) {
            this.despawnFrom(viewer);
            this.spawnTo(viewer);
        }
    }

    @Override
    public boolean canCollideWith(Entity entity) {
        return false;
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
        addEntity.setItemInHand(ItemStackPacketCodec.toNetwork(this.getItem()));
        return addEntity;
    }
}
