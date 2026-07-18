package org.cloudburstmc.server.entity.misc;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockComponents;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.damage.DamageSource;
import org.cloudburstmc.api.entity.damage.DamageTypes;
import org.cloudburstmc.api.entity.misc.FallingBlock;
import org.cloudburstmc.api.event.entity.EntityBlockChangeEvent;
import org.cloudburstmc.api.event.entity.EntityDamageEvent;
import org.cloudburstmc.api.item.ItemKeys;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.level.gamerule.GameRules;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.server.block.CloudBlock;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.level.Sound;
import org.cloudburstmc.server.registry.CloudBlockRegistry;

import java.util.Objects;

import static org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes.BLOCK;
import static org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag.FIRE_IMMUNE;

public class EntityFallingBlock extends CloudEntity implements FallingBlock {

    private static final int OUTSIDE_LEVEL_TIMEOUT = 100;
    private static final int MAX_LIFETIME = 600;

    private BlockState blockState = BlockStates.SAND;
    private Sound landingSound;
    private Sound breakSound;
    private int time;
    private boolean dropItem = true;
    private boolean cancelDrop;
    private boolean hurtEntities;
    private float damagePerBlock;
    private int maximumDamage = 40;
    private boolean autoExpire = true;

    public EntityFallingBlock(EntityType<FallingBlock> type, Location location) {
        super(type, location);
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.fireProof = true;
        this.data.setFlag(FIRE_IMMUNE, true);
    }

    @Override
    public float getWidth() {
        return 0.98f;
    }

    @Override
    public float getLength() {
        return 0.98f;
    }

    @Override
    public float getHeight() {
        return 0.98f;
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
        return 0.49f;
    }

    @Override
    public boolean canCollide() {
        return false;
    }

    @Override
    public boolean canCollideWith(Entity entity) {
        return false;
    }

    @Override
    public boolean canBeMovedByCurrents() {
        return false;
    }

    @Override
    public boolean attack(EntityDamageEvent source) {
        return source.getDamageType() == DamageTypes.VOID && super.attack(source);
    }

    @Override
    public void loadAdditionalData(NbtMap tag) {
        super.loadAdditionalData(tag);
        if (!tag.containsKey("BlockState")) {
            this.close();
            return;
        }

        this.setBlockState(CloudBlockRegistry.REGISTRY.getBlock(tag.getCompound("BlockState")));
        this.time = tag.getInt("Time");
        this.dropItem = !tag.containsKey("DropItem") || tag.getBoolean("DropItem");
        this.cancelDrop = tag.getBoolean("CancelDrop");
        this.hurtEntities = tag.getBoolean("HurtEntities");
        this.damagePerBlock = tag.getFloat("DamagePerBlock");
        this.maximumDamage = tag.containsKey("MaximumDamage") ? tag.getInt("MaximumDamage") : 40;
        this.autoExpire = !tag.containsKey("AutoExpire") || tag.getBoolean("AutoExpire");

        if (tag.containsKey("LandingSound")) {
            this.landingSound = Sound.valueOf(tag.getString("LandingSound"));
        }

        if (tag.containsKey("BreakSound")) {
            this.breakSound = Sound.valueOf(tag.getString("BreakSound"));
        }
    }

    @Override
    public void saveAdditionalData(NbtMapBuilder tag) {
        super.saveAdditionalData(tag);
        tag.putCompound("BlockState", CloudBlockRegistry.REGISTRY.getDefinition(this.blockState).getState());
        tag.putInt("Time", this.time);
        tag.putBoolean("DropItem", this.dropItem);
        tag.putBoolean("CancelDrop", this.cancelDrop);
        tag.putBoolean("HurtEntities", this.hurtEntities);
        tag.putFloat("DamagePerBlock", this.damagePerBlock);
        tag.putInt("MaximumDamage", this.maximumDamage);
        tag.putBoolean("AutoExpire", this.autoExpire);

        if (this.landingSound != null) {
            tag.putString("LandingSound", this.landingSound.name());
        }

        if (this.breakSound != null) {
            tag.putString("BreakSound", this.breakSound.name());
        }
    }

    @Override
    public boolean onUpdate(int currentTick) {
        if (this.closed) {
            return false;
        }

        int tickDiff = currentTick - this.lastUpdate;
        if (tickDiff <= 0) {
            return true;
        }
        this.lastUpdate = currentTick;

        this.timing.startTiming();
        try {
            return this.tickFallingBlock(tickDiff);
        } finally {
            this.timing.stopTiming();
        }
    }

    @Override
    public BlockState getBlockState() {
        return this.blockState;
    }

    @Override
    public void setBlockState(BlockState blockState) {
        this.blockState = Objects.requireNonNull(blockState, "blockState");
        this.updateBlockMetadata();
    }

    @Override
    public boolean doesDropItem() {
        return this.dropItem;
    }

    @Override
    public void setDropItem(boolean dropItem) {
        this.dropItem = dropItem;
    }

    @Override
    public boolean isDropCancelled() {
        return this.cancelDrop;
    }

    @Override
    public void setDropCancelled(boolean cancelDrop) {
        this.cancelDrop = cancelDrop;
    }

    @Override
    public boolean canHurtEntities() {
        return this.hurtEntities;
    }

    @Override
    public void setHurtEntities(boolean hurtEntities) {
        this.hurtEntities = hurtEntities;
    }

    @Override
    public float getDamagePerBlock() {
        return this.damagePerBlock;
    }

    @Override
    public void setDamagePerBlock(float damagePerBlock) {
        if (damagePerBlock < 0) {
            throw new IllegalArgumentException("damagePerBlock must not be negative");
        }

        this.damagePerBlock = damagePerBlock;
        if (damagePerBlock > 0) {
            this.hurtEntities = true;
        }
    }

    @Override
    public int getMaximumDamage() {
        return this.maximumDamage;
    }

    @Override
    public void setMaximumDamage(int maximumDamage) {
        if (maximumDamage < 0) {
            throw new IllegalArgumentException("maximumDamage must not be negative");
        }
        this.maximumDamage = maximumDamage;
    }

    @Override
    public boolean doesAutoExpire() {
        return this.autoExpire;
    }

    @Override
    public void setAutoExpire(boolean autoExpire) {
        this.autoExpire = autoExpire;
    }

    public void setLandingSound(Sound landingSound) {
        this.landingSound = landingSound;
    }

    public void setBreakSound(Sound breakSound) {
        this.breakSound = breakSound;
    }

    private boolean tickFallingBlock(int tickDiff) {
        boolean updated = this.entityBaseTick(tickDiff);
        if (!this.isAlive()) {
            return updated;
        }

        this.time += tickDiff;
        this.motion = this.motion.sub(0, this.getGravity(), 0);
        this.move(this.motion);
        float drag = 1 - this.getDrag();
        this.motion = this.motion.mul(drag, drag, drag);

        Vector3i position = this.getPosition().toInt();
        if (this.onGround) {
            this.land(position);
            return true;
        }

        if (this.hasExpired(position)) {
            this.dropBlockItem();
            this.close();
            return true;
        }

        this.updateMovement();
        return updated || this.motion.lengthSquared() > 0.00001f;
    }

    private void land(Vector3i position) {
        Block target = this.level.getBlock(position);
        this.damageEntities();
        float distance = Math.max(this.fallDistance, this.highestPosition - this.getY());
        CloudBlockRegistry.REGISTRY.requireComponent(this.blockState.getType(), BlockComponents.ON_FALLING_LAND)
                .execute(this, target, distance);
        if (this.cancelDrop) {
            this.close();
            return;
        }

        if (!this.canPlaceAt(target)) {
            this.close();
            this.breakBlock();
            return;
        }

        EntityBlockChangeEvent event = new EntityBlockChangeEvent(this, target, this.blockState);
        this.server.getEventManager().fire(event);
        if (event.isCancelled()) {
            this.close();
            return;
        }

        if (!this.level.setBlockState(position, event.getTo(), true, true)) {
            this.close();
            this.breakBlock();
            return;
        }

        this.close();
        this.playLandingSound(position);
    }

    private void damageEntities() {
        if (!this.hurtEntities || this.damagePerBlock <= 0) {
            return;
        }

        float distance = Math.max(this.fallDistance, this.highestPosition - this.getY());
        int effectiveDistance = (int) Math.ceil(distance - 1);
        float damage = Math.min((float) Math.floor(effectiveDistance * this.damagePerBlock), this.maximumDamage);
        if (damage <= 0) {
            return;
        }

        for (Entity entity : this.level.getCollidingEntities(this.getBoundingBox())) {
            if (entity != this) {
                DamageSource source = DamageSource.builder(DamageTypes.FALLING_BLOCK)
                        .directEntity(this).causingEntity(this).location(this.getLocation()).build();
                EntityDamageEvent event = new EntityDamageEvent(entity, source, damage);
                event.setKnockback(0);
                entity.attack(event);
            }
        }
    }

    private boolean canPlaceAt(Block target) {
        if (!target.getState().isReplaceable()) {
            return false;
        }

        Block prospective = new CloudBlock(this.level, target.getPosition(),
                new BlockState[]{this.blockState, target.getExtra()});
        return prospective.requireComponent(BlockComponents.CAN_SURVIVE).execute(prospective)
                && !prospective.requireComponent(BlockComponents.IS_FREE_TO_FALL).execute(prospective);
    }

    private boolean hasExpired(Vector3i position) {
        if (!this.autoExpire) {
            return false;
        }

        boolean outsideLevel = position.getY() < this.level.getMinHeight()
                || position.getY() >= this.level.getMaxHeight();
        return this.time > MAX_LIFETIME || this.time > OUTSIDE_LEVEL_TIMEOUT && outsideLevel;
    }

    private void breakBlock() {
        this.dropBlockItem();
        if (this.breakSound != null) {
            this.level.addSound(this.getPosition(), this.breakSound);
        }
    }

    private void dropBlockItem() {
        if (this.cancelDrop || !this.dropItem || !this.level.getGameRules().get(GameRules.DO_ENTITY_DROPS)) {
            return;
        }

        this.blockState.getType().asItem().ifPresent(itemType -> this.level.dropItem(
                this.getPosition(),
                ItemStack.builder()
                        .itemType(itemType)
                        .data(ItemKeys.BLOCK_STATE, this.blockState)
                        .amount(1)
                        .build()));
    }

    private void playLandingSound(Vector3i position) {
        if (this.landingSound != null) {
            this.level.addSound(position, this.landingSound);
        }
    }

    private void updateBlockMetadata() {
        this.data.set(BLOCK, CloudBlockRegistry.REGISTRY.getDefinition(this.blockState));
    }
}
