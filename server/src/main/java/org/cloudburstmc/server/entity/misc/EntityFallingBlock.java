package org.cloudburstmc.server.entity.misc;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockChange;
import org.cloudburstmc.api.block.BlockComponents;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.component.FallingBlockDropHandler;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.damage.DamageSource;
import org.cloudburstmc.api.entity.damage.DamageTypes;
import org.cloudburstmc.api.entity.misc.FallingBlock;
import org.cloudburstmc.api.event.entity.EntityChangeBlockEvent;
import org.cloudburstmc.api.event.entity.EntitySpawnEvent;
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
import org.cloudburstmc.server.block.component.FallingBlockHandlers;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.entity.data.EntityData;
import org.cloudburstmc.server.entity.data.FallingBlockData;
import org.cloudburstmc.server.event.entity.CloudEntityDamageEvent;

import java.util.List;
import java.util.Objects;

import static org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes.BLOCK;
import static org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag.FIRE_IMMUNE;
import static org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag.MOVING;

public class EntityFallingBlock extends CloudEntity implements FallingBlock {

    public SoundType getLandingSound() {
        String id = this.data().getLandingSound();
        return id == null ? null : SoundType.of(Identifier.parse(id));
    }

    public void setLandingSound(SoundType sound) {
        this.data().setLandingSound(sound == null ? null : sound.getId().toString());
    }

    public SoundType getBreakSound() {
        String id = this.data().getBreakSound();
        return id == null ? null : SoundType.of(Identifier.parse(id));
    }

    public void setBreakSound(SoundType sound) {
        this.data().setBreakSound(sound == null ? null : sound.getId().toString());
    }

    private static final int OUTSIDE_LEVEL_TIMEOUT = 100;
    private static final int MAX_LIFETIME = 600;
    private static final int SETTLED_REMOVAL_DELAY = 3;

    private boolean landingPending;
    private int settledRemovalDelay;

    public EntityFallingBlock(EntityType<FallingBlock> type, Location location) {
        super(type, location);
    }

    @Override
    protected Class<? extends EntityData> minimumDataClass() {
        return FallingBlockData.class;
    }

    @Override
    protected FallingBlockData data() {
        return (FallingBlockData) super.data();
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
    public float getBaseOffset() {
        return 0.49f;
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
    public boolean blocksBuilding() {
        return true;
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
    protected void loadCustom(NbtMap tag) {
        super.loadCustom(tag);

        if (!tag.containsKey("BlockState")) {
            this.close();
            return;
        }

        this.setBlockState(BlockStateCodec.read(tag.getCompound("BlockState")));
        this.data().setTime(tag.getInt("Time"));
        this.data().setDropItem(!tag.containsKey("DropItem") || tag.getBoolean("DropItem"));
        this.data().setCancelDrop(tag.getBoolean("CancelDrop"));
        this.data().setHurtEntities(tag.getBoolean("HurtEntities"));
        this.data().setDamagePerBlock(tag.getFloat("DamagePerBlock"));
        this.data().setMaximumDamage(tag.containsKey("MaximumDamage") ? tag.getInt("MaximumDamage") : 40);
        this.data().setAutoExpire(!tag.containsKey("AutoExpire") || tag.getBoolean("AutoExpire"));

        if (tag.containsKey("LandingSound")) {
            this.setLandingSound(SoundType.of(Identifier.parse(tag.getString("LandingSound"))));
        }

        if (tag.containsKey("BreakSound")) {
            this.setBreakSound(SoundType.of(Identifier.parse(tag.getString("BreakSound"))));
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

    public boolean isUnsettled() {
        return !this.isClosed() && this.settledRemovalDelay == 0;
    }

    public boolean startFalling(Block source, BlockState replacement) {
        boolean spawned = this.spawn(new EntitySpawnEvent(this), () -> {
            Vector3i position = source.getPosition();
            if (this.level.getBlockState(position) != source.getState()
                    || !this.level.removeBlockForFallingEntity(position, replacement, this)) {
                return false;
            }

            this.data().setTime(this.data().getTime() + 1);
            this.applyGravityAndMove();
            return true;
        });

        if (!spawned) {
            return false;
        }

        this.data.setFlag(MOVING, true);
        return true;
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
        return this.tickFallingBlock(tickDiff);
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
            this.dropBlockItem(FallingBlockHandlers.BLOCK_ITEM_DROP);
            this.close();
            return true;
        }

        this.updateMovement();
        return updated || this.motion.lengthSquared() > 0.00001f;
    }

    private void applyGravityAndMove() {
        this.motion = this.motion.sub(0, this.getGravity(), 0);
        this.move(this.motion);
        float drag = 1 - this.getDrag();
        this.motion = this.motion.mul(drag, drag, drag);
    }

    private boolean hasExpired(Vector3i position) {
        if (!this.data().isAutoExpire()) {
            return false;
        }

        boolean outsideLevel = position.getY() < this.level.getMinHeight() || position.getY() >= this.level.getMaxHeight();
        return this.data().getTime() > MAX_LIFETIME || this.data().getTime() > OUTSIDE_LEVEL_TIMEOUT && outsideLevel;
    }

    private void land(Vector3i position) {
        Block target = this.level.getBlock(position);
        this.damageEntities();

        float distance = Math.max(this.fallDistance, this.highestPosition - this.getY());
        this.level.getServer().getBlockRegistry()
                .requireComponent(this.data().getBlockState().getType(), BlockComponents.ON_FALLING_LAND)
                .execute(this, target, distance);

        if (this.data().isCancelDrop()) {
            this.close();
            return;
        }

        if (this.level.getServer().getBlockRegistry()
                .requireComponent(this.data().getBlockState().getType(), BlockComponents.BREAKS_ON_LANDING)) {
            this.close();
            this.breakBlock();
            return;
        }

        List<BlockChange> changes = List.copyOf(this.level.getServer().getBlockRegistry()
                .requireComponent(this.data().getBlockState().getType(), BlockComponents.RESOLVE_FALLING_PLACEMENT)
                .execute(this.data().getBlockState(), target));
        if (changes.isEmpty()) {
            this.close();
            this.breakBlock();
            return;
        }

        for (BlockChange change : changes) {
            EntityChangeBlockEvent event = new EntityChangeBlockEvent(this, change.block(), change.newState());
            this.server.getEventManager().fire(event);
            if (event.isCancelled()) {
                this.close();
                return;
            }
        }

        if (!this.level.placeBlocksFromFallingEntity(changes, this)) {
            this.close();
            this.breakBlock();
            return;
        }

        this.data.setFlag(MOVING, false);
        this.settledRemovalDelay = SETTLED_REMOVAL_DELAY;
        this.playLandingSound(position);
    }

    private void damageEntities() {
        if (!this.data().isHurtEntities() || this.data().getDamagePerBlock() <= 0) {
            return;
        }

        float distance = Math.max(this.fallDistance, this.highestPosition - this.getY());
        int effectiveDistance = (int) Math.ceil(distance - 1);
        float damage = Math.min((float) Math.floor(effectiveDistance * this.data().getDamagePerBlock()), this.data().getMaximumDamage());
        if (damage <= 0) {
            return;
        }

        DamageSource source = this.level.getServer().getBlockRegistry()
                .requireComponent(this.data().getBlockState().getType(), BlockComponents.GET_FALLING_DAMAGE_SOURCE)
                .execute(this);

        for (Entity entity : this.level.getCollidingEntities(this.getBoundingBox())) {
            if (entity != this) {
                entity.damage(damage, source);
            }
        }
    }

    private void playLandingSound(Vector3i position) {
        if (this.getLandingSound() != null) {
            this.level.playSound(position, this.getLandingSound());
        }
    }

    private void breakBlock() {
        this.dropBlockItem(this.level.getServer().getBlockRegistry()
                .requireComponent(this.data().getBlockState().getType(), BlockComponents.GET_FALLING_BREAK_DROP));

        if (this.getBreakSound() != null) {
            this.level.playSound(this.getPosition(), this.getBreakSound());
        }
    }

    private void dropBlockItem(FallingBlockDropHandler drops) {
        if (this.data().isCancelDrop() || !this.data().isDropItem() || !this.level.getGameRules().get(GameRules.DO_ENTITY_DROPS)) {
            return;
        }

        ItemStack item = drops.execute(this.data().getBlockState());
        if (!item.isEmpty()) {
            this.level.dropItem(this.getPosition(), item);
        }
    }

    private void updateBlockMetadata() {
        this.data.set(BLOCK, BlockPalette.INSTANCE.getDefinition(this.data().getBlockState()));
    }
}
