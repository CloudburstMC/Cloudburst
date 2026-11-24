package org.cloudburstmc.server.entity.misc;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.damage.DamageTypeTags;
import org.cloudburstmc.api.entity.hostile.EnderDragon;
import org.cloudburstmc.api.entity.misc.EnderCrystal;
import org.cloudburstmc.api.event.entity.EntityRemoveCause;
import org.cloudburstmc.api.level.ExplosionBlockInteraction;
import org.cloudburstmc.api.level.ExplosionSettings;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.cloudburstmc.server.block.component.FireBlockHandlers;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.entity.data.CloudEnderCrystalData;
import org.cloudburstmc.server.event.entity.CloudEntityDamageEvent;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.level.Explosion;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes.BLOCK_TARGET_POS;
import static org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag.FIRE_IMMUNE;

public class EntityEnderCrystal extends CloudEntity implements EnderCrystal {

    private final CloudEnderCrystalData persistenceState;
    private boolean exploding;

    public EntityEnderCrystal(EntityType<EnderCrystal> type, Location location) {
        CloudEnderCrystalData state = new CloudEnderCrystalData();
        super(type, location, state);
        this.persistenceState = state;
    }

    @Override
    protected void initEntity() {
        super.initEntity();

        this.fireProof = true;
        this.data.setFlag(FIRE_IMMUNE, true);
        this.setShowingBase(true);
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
    public float getWidth() {
        return 0.98f;
    }

    @Override
    protected boolean tickEntity(int currentTick, int elapsedTicks) {
        boolean updated = super.tickEntity(currentTick, elapsedTicks);
        if (!this.isClosed() && this.getLevel().getDimension() == CloudLevel.DIMENSION_THE_END) {
            Vector3i position = Vector3i.from(this.getPosition().getFloorX(), this.getPosition().getFloorY(), this.getPosition().getFloorZ());
            Block block = this.getLevel().getBlock(position);
            if (block.getState() == BlockStates.AIR) {
                this.getLevel().setBlockState(position, FireBlockHandlers.placementState(block));
            }
        }

        return updated;
    }

    @Override
    protected boolean applyDamage(CloudEntityDamageEvent event) {
        if (this.isDamageImmune(event.getDamageSource()) || event.getDamageType().is(DamageTypeTags.IS_FIRE)
                || event.getDamageSource().getCausingEntity() instanceof EnderDragon) {
            return false;
        }

        this.getServer().getEventManager().fire(event);
        if (event.isCancelled() || event.getDamage() <= 0 || this.isClosed() || !this.isAlive()) {
            return false;
        }

        if (event.getDamageType().is(DamageTypeTags.IS_EXPLOSION)) {
            this.setLastDamageCause(event);
            this.remove(EntityRemoveCause.DEATH);
            return true;
        }

        return this.primeAndExplode(event);
    }

    private boolean primeAndExplode(@Nullable CloudEntityDamageEvent hit) {
        if (this.isClosed() || this.exploding) {
            return false;
        }

        CloudLevel level = this.getLevel();
        this.exploding = true;
        try {
            ExplosionSettings primed = level.primeExplosion(new ExplosionSettings(6, ExplosionBlockInteraction.DESTROY, false, this, null));
            if (primed == null) {
                if (hit != null) {
                    hit.setCancelled(true);
                }

                return false;
            }

            if (this.isClosed() || this.getLevel() != level) {
                return false;
            }

            if (hit != null) {
                this.setLastDamageCause(hit);
                Entity attacker = hit.getDamageSource().getCausingEntity();
                if (attacker != null) {
                    this.setOwner(attacker);
                }
            }

            Vector3f position = this.getPosition();
            this.remove(EntityRemoveCause.EXPLODE);
            new Explosion(level, position, primed).explode();
            return true;
        } finally {
            this.exploding = false;
        }
    }

    @Override
    public boolean canCollideWith(Entity entity) {
        return false;
    }

    @Override
    protected CloudEnderCrystalData data() {
        return this.persistenceState;
    }

    @Override
    protected void loadCustom(NbtMap tag) {
        if (tag.containsKey("ShowBottom")) {
            this.setShowingBase(getBoolean(tag, "ShowBottom", this.isShowingBase()));
        }

        if (tag.containsKey("BeamTarget")) {
            Object raw = tag.get("BeamTarget");
            if (raw instanceof NbtMap beam) {
                this.setBeamTarget(Vector3i.from(beam.getInt("X"), beam.getInt("Y"), beam.getInt("Z")));
            }
        }

        super.loadCustom(tag);
    }

    @Override
    protected void saveCustom(NbtMapBuilder tag) {
        super.saveCustom(tag);
        tag.putBoolean("ShowBottom", this.isShowingBase());
        Vector3i target = this.getBeamTarget();
        if (target != null) {
            tag.putCompound("BeamTarget", NbtMap.builder()
                    .putInt("X", target.getX())
                    .putInt("Y", target.getY())
                    .putInt("Z", target.getZ())
                    .build());
        }
    }

    @Override
    protected Set<String> customKeys() {
        Set<String> keys = new HashSet<>(super.customKeys());
        keys.add("ShowBottom");
        keys.add("BeamTarget");
        return Set.copyOf(keys);
    }

    @Override
    public boolean isShowingBase() {
        return this.data.getFlag(EntityFlag.SHOW_BOTTOM);
    }

    @Override
    public void setShowingBase(boolean showingBase) {
        if (this.isShowingBase() != showingBase) {
            this.data.setFlag(EntityFlag.SHOW_BOTTOM, showingBase);
            if (this.isValid()) {
                this.markChunkDirty();
            }
        }
    }

    @Override
    public @Nullable Vector3i getBeamTarget() {
        return this.data().getBeamTarget();
    }

    @Override
    public void setBeamTarget(@Nullable Vector3i target) {
        if (Objects.equals(this.getBeamTarget(), target)) {
            return;
        }

        this.data().setBeamTarget(target);
        this.data.set(BLOCK_TARGET_POS, target == null ? Vector3i.ZERO : target);
        if (this.isValid()) {
            this.markChunkDirty();
        }
    }
}
