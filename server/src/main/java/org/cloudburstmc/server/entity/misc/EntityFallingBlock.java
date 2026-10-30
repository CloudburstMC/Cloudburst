package org.cloudburstmc.server.entity.misc;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockComponents;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.damage.DamageSource;
import org.cloudburstmc.api.entity.damage.DamageTypes;
import org.cloudburstmc.api.entity.data.EntityData;
import org.cloudburstmc.api.entity.data.FallingBlockData;
import org.cloudburstmc.api.entity.misc.FallingBlock;
import org.cloudburstmc.api.event.entity.EntityChangeBlockEvent;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.level.gamerule.GameRules;
import org.cloudburstmc.api.level.sound.SoundType;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.server.block.BlockPalette;
import org.cloudburstmc.server.block.BlockStateCodec;
import org.cloudburstmc.server.block.CloudBlock;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.event.entity.CloudEntityDamageEvent;

import java.util.Objects;

import static org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes.BLOCK;
import static org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag.FIRE_IMMUNE;
import static org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag.MOVING;

public class EntityFallingBlock extends CloudEntity implements FallingBlock {

    private static final int OUTSIDE_LEVEL_TIMEOUT = 100;
    private static final int MAX_LIFETIME = 600;
    private static final int SETTLED_REMOVAL_DELAY = 3;

    private boolean landingPending;
    private int settledRemovalDelay;

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
    public boolean blocksBuilding() {
        return true;
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
    protected boolean applyDamage(CloudEntityDamageEvent source) {
        return source.getDamageType() == DamageTypes.OUT_OF_WORLD && super.applyDamage(source);
    }

    @Override
    protected FallingBlockData data() {
        return (FallingBlockData) super.data();
    }

    @Override
    protected Class<? extends EntityData> minimumDataClass() {
        return FallingBlockData.class;
    }

    @Override
    protected void loadCustom(NbtMap tag) {
        // The block state, timers and behavior flags run before
        // the base load below, matching the historical load order. The block
        // state key is required, so a tag without it closes the entity, exactly as the old
        // null check did. The state is read only here: the mapper leaves it alone so a bad
        // state fails with the bare error, as before. Keys with a load default reset when
        // absent; the sounds keep their current values, exactly as the old null checks did,
        // and are saved in today's normalized form.
        if (!tag.containsKey("BlockState")) {
            this.close();
            return;
        }
        this.setBlockState(BlockStateCodec.read(tag.getCompound("BlockState")));
        this.data().setTime(getInt(tag, "Time", 0));
        this.data().setDropItem(getBoolean(tag, "DropItem", true));
        this.data().setCancelDrop(getBoolean(tag, "CancelDrop", false));
        this.data().setHurtEntities(getBoolean(tag, "HurtEntities", false));
        this.data().setDamagePerBlock(getFloat(tag, "DamagePerBlock", 0f));
        this.data().setMaximumDamage(getInt(tag, "MaximumDamage", 40));
        this.data().setAutoExpire(getBoolean(tag, "AutoExpire", true));

        if (tag.containsKey("LandingSound")) {
            Object raw = tag.get("LandingSound");
            if (raw instanceof String sound) {
                this.setLandingSound(SoundType.of(Identifier.parse(sound)));
            }
        }
        if (tag.containsKey("BreakSound")) {
            Object raw = tag.get("BreakSound");
            if (raw instanceof String sound) {
                this.setBreakSound(SoundType.of(Identifier.parse(sound)));
            }
        }
        super.loadCustom(tag);
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
        return this.data().getBlockState();
    }

    @Override
    public void setBlockState(BlockState blockState) {
        this.data().setBlockState(Objects.requireNonNull(blockState, "blockState"));
        this.updateBlockMetadata();
    }

    @Override
    public boolean doesDropItem() {
        return this.data().isDropItem();
    }

    @Override
    public void setDropItem(boolean dropItem) {
        this.data().setDropItem(dropItem);
    }

    @Override
    public boolean isDropCancelled() {
        return this.data().isCancelDrop();
    }

    @Override
    public void setDropCancelled(boolean cancelDrop) {
        this.data().setCancelDrop(cancelDrop);
    }

    @Override
    public boolean canHurtEntities() {
        return this.data().isHurtEntities();
    }

    @Override
    public void setHurtEntities(boolean hurtEntities) {
        this.data().setHurtEntities(hurtEntities);
    }

    @Override
    public float getDamagePerBlock() {
        return this.data().getDamagePerBlock();
    }

    @Override
    public void setDamagePerBlock(float damagePerBlock) {
        if (damagePerBlock < 0) {
            throw new IllegalArgumentException("damagePerBlock must not be negative");
        }

        this.data().setDamagePerBlock(damagePerBlock);
        if (damagePerBlock > 0) {
            this.data().setHurtEntities(true);
        }
    }

    @Override
    public int getMaximumDamage() {
        return this.data().getMaximumDamage();
    }

    @Override
    public void setMaximumDamage(int maximumDamage) {
        if (maximumDamage < 0) {
            throw new IllegalArgumentException("maximumDamage must not be negative");
        }
        this.data().setMaximumDamage(maximumDamage);
    }

    @Override
    public boolean doesAutoExpire() {
        return this.data().isAutoExpire();
    }

    @Override
    public void setAutoExpire(boolean autoExpire) {
        this.data().setAutoExpire(autoExpire);
    }

    public SoundType getLandingSound() {
        String landingSound = this.data().getLandingSound();
        return landingSound == null ? null : SoundType.of(Identifier.parse(landingSound));
    }

    public void setLandingSound(SoundType landingSound) {
        this.data().setLandingSound(landingSound == null ? null : landingSound.getId().toString());
    }

    public SoundType getBreakSound() {
        String breakSound = this.data().getBreakSound();
        return breakSound == null ? null : SoundType.of(Identifier.parse(breakSound));
    }

    public void setBreakSound(SoundType breakSound) {
        this.data().setBreakSound(breakSound == null ? null : breakSound.getId().toString());
    }

    public boolean startFalling(Vector3i position, BlockState replacement) {
        if (!this.level.removeBlockForFallingEntity(position, replacement, this)) {
            return false;
        }

        this.data().setTime(this.data().getTime() + 1);
        this.applyGravityAndMove();
        if (!this.spawn()) {
            this.level.setBlockState(position, this.getBlockState());
            return false;
        }

        this.spawnToAll();
        this.data.setFlag(MOVING, true);
        return true;
    }

    private boolean tickFallingBlock(int tickDiff) {
        if (this.settledRemovalDelay > 0) {
            this.settledRemovalDelay -= tickDiff;
            if (this.settledRemovalDelay <= 0) {
                this.close();
            }
            return true;
        }

        if (this.landingPending) {
            this.land(this.getPosition().toInt());
            return true;
        }

        boolean updated = this.entityBaseTick(tickDiff);
        if (!this.isAlive()) {
            return updated;
        }

        this.data().setTime(this.data().getTime() + tickDiff);
        this.applyGravityAndMove();

        Vector3i position = this.getPosition().toInt();
        if (this.onGround) {
            this.motion = Vector3f.ZERO;
            this.updateMovement();
            this.landingPending = true;
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
        this.level.getServer().getBlockRegistry().requireComponent(this.getBlockState().getType(), BlockComponents.ON_FALLING_LAND)
                .execute(this, target, distance);
        if (this.isDropCancelled()) {
            this.close();
            return;
        }

        if (this.level.getServer().getBlockRegistry().requireComponent(this.getBlockState().getType(), BlockComponents.BREAKS_ON_LANDING)) {
            this.close();
            this.breakBlock();
            return;
        }

        if (!this.canPlaceAt(target)) {
            this.close();
            this.breakBlock();
            return;
        }

        EntityChangeBlockEvent event = new EntityChangeBlockEvent(this, target, this.getBlockState());
        this.server.getEventManager().fire(event);
        if (event.isCancelled()) {
            this.close();
            return;
        }

        if (!this.level.placeBlockFromFallingEntity(position, event.getBlockState(), this)) {
            this.close();
            this.breakBlock();
            return;
        }

        this.data.setFlag(MOVING, false);
        this.settledRemovalDelay = SETTLED_REMOVAL_DELAY;
        this.playLandingSound(position);
    }

    private void applyGravityAndMove() {
        this.motion = this.motion.sub(0, this.getGravity(), 0);
        this.move(this.motion);
        float drag = 1 - this.getDrag();
        this.motion = this.motion.mul(drag, drag, drag);
    }

    private void damageEntities() {
        if (!this.canHurtEntities() || this.getDamagePerBlock() <= 0) {
            return;
        }

        float distance = Math.max(this.fallDistance, this.highestPosition - this.getY());
        int effectiveDistance = (int) Math.ceil(distance - 1);
        float damage = Math.min((float) Math.floor(effectiveDistance * this.getDamagePerBlock()), this.getMaximumDamage());
        if (damage <= 0) {
            return;
        }

        DamageSource source = this.level.getServer().getBlockRegistry().requireComponent(this.getBlockState().getType(), BlockComponents.GET_FALLING_DAMAGE_SOURCE).execute(this);
        for (Entity entity : this.level.getCollidingEntities(this.getBoundingBox())) {
            if (entity != this) {
                entity.damage(damage, source);
            }
        }
    }

    private boolean canPlaceAt(Block target) {
        if (!target.getState().isReplaceable()) {
            return false;
        }

        Block prospective = new CloudBlock(this.level, target.getPosition(),
                new BlockState[]{this.getBlockState(), target.getSecondaryState()});
        return prospective.requireComponent(BlockComponents.CAN_SURVIVE).execute(prospective)
                && !prospective.requireComponent(BlockComponents.IS_FREE_TO_FALL).execute(prospective);
    }

    private boolean hasExpired(Vector3i position) {
        if (!this.doesAutoExpire()) {
            return false;
        }

        boolean outsideLevel = position.getY() < this.level.getMinHeight()
                || position.getY() >= this.level.getMaxHeight();
        int time = this.data().getTime();
        return time > MAX_LIFETIME || time > OUTSIDE_LEVEL_TIMEOUT && outsideLevel;
    }

    private void breakBlock() {
        this.dropBlockItem();
        SoundType breakSound = this.getBreakSound();
        if (breakSound != null) {
            this.level.playSound(this.getPosition(), breakSound);
        }
    }

    private void dropBlockItem() {
        if (this.isDropCancelled() || !this.doesDropItem() || !this.level.getGameRules().get(GameRules.DO_ENTITY_DROPS)) {
            return;
        }

        this.getBlockState().getType().asItem().ifPresent(itemType -> this.level.dropItem(
                this.getPosition(),
                ItemStack.builder()
                        .itemType(itemType)
                        .setData(ItemDataComponents.BLOCK_STATE, this.getBlockState())
                        .amount(1)
                        .build()));
    }

    private void playLandingSound(Vector3i position) {
        SoundType landingSound = this.getLandingSound();
        if (landingSound != null) {
            this.level.playSound(position, landingSound);
        }
    }

    private void updateBlockMetadata() {
        this.data.set(BLOCK, BlockPalette.INSTANCE.getDefinition(this.getBlockState()));
    }
}
