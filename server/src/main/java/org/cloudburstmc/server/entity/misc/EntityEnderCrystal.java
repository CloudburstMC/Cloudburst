package org.cloudburstmc.server.entity.misc;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.Explosive;
import org.cloudburstmc.api.entity.damage.DamageTypeTags;
import org.cloudburstmc.api.entity.data.EnderCrystalData;
import org.cloudburstmc.api.entity.data.EntityData;
import org.cloudburstmc.api.entity.hostile.EnderDragon;
import org.cloudburstmc.api.entity.misc.EnderCrystal;
import org.cloudburstmc.api.level.ExplosionBlockInteraction;
import org.cloudburstmc.api.level.ExplosionSettings;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.event.entity.CloudEntityDamageEvent;
import org.cloudburstmc.server.level.CloudLevel;

import java.util.HashSet;
import java.util.Set;

import static org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes.BLOCK_TARGET_POS;
import static org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag.FIRE_IMMUNE;

public class EntityEnderCrystal extends CloudEntity implements EnderCrystal, Explosive {

    public EntityEnderCrystal(EntityType<EnderCrystal> type, Location location) {
        super(type, location);
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
    public boolean onUpdate(int currentTick) {
        boolean updated = super.onUpdate(currentTick);
        if (!this.isClosed() && this.getLevel().getDimension() == CloudLevel.DIMENSION_THE_END) {
            Vector3i position = Vector3i.from(this.getPosition().getFloorX(), this.getPosition().getFloorY(), this.getPosition().getFloorZ());
            if (this.getLevel().getBlockState(position) == BlockStates.AIR) {
                this.getLevel().setBlockState(position, BlockStates.FIRE);
            }
        }

        return updated;
    }

    @Override
    protected boolean applyDamage(CloudEntityDamageEvent source) {
        if (this.isDamageImmune(source.getDamageSource()) || source.getDamageType().is(DamageTypeTags.IS_FIRE)
                || source.getDamageSource().getCausingEntity() instanceof EnderDragon) {
            return false;
        }

        this.getServer().getEventManager().fire(source);
        if (source.isCancelled() || source.getDamage() <= 0 || this.isClosed() || !this.isAlive()) {
            return false;
        }

        this.setLastDamageCause(source);
        if (source.getDamageType().is(DamageTypeTags.IS_EXPLOSION)) {
            this.close();
        } else {
            Entity cause = source.getDamageSource().getCausingEntity();
            if (cause != null) {
                this.setOwner(cause);
            }

            this.explode();
        }

        return true;
    }

    @Override
    public void explode() {
        if (this.isClosed()) {
            return;
        }

        Vector3f position = this.getPosition();
        this.close();
        this.getLevel().explode(position, new ExplosionSettings(6, ExplosionBlockInteraction.DESTROY, false, this, null));
    }

    @Override
    public boolean canCollideWith(Entity entity) {
        return false;
    }

    @Override
    protected EnderCrystalData data() {
        return (EnderCrystalData) super.data();
    }

    @Override
    protected Class<? extends EntityData> minimumDataClass() {
        return EnderCrystalData.class;
    }

    @Override
    protected void loadCustom(NbtMap tag) {
        // The base flag and beam target run before the base
        // load below, matching the historical load order, and keep
        // their current values when absent. The beam is read leniently from its
        // compound, exactly as the old pass-through did.
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
        this.data.setFlag(EntityFlag.SHOW_BOTTOM, showingBase);
    }

    @Override
    public @Nullable Vector3i getBeamTarget() {
        Vector3i target = this.data.get(BLOCK_TARGET_POS);
        return target == null || target.equals(Vector3i.ZERO) ? null : target;
    }

    @Override
    public void setBeamTarget(@Nullable Vector3i target) {
        this.data.set(BLOCK_TARGET_POS, target == null ? Vector3i.ZERO : target);
    }
}
