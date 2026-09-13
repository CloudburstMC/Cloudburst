package org.cloudburstmc.server.entity.misc;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.Explosive;
import org.cloudburstmc.api.entity.damage.DamageTypeTags;
import org.cloudburstmc.api.entity.misc.EnderCrystal;
import org.cloudburstmc.api.event.entity.EntityDamageEvent;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.level.gamerule.GameRules;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.level.Explosion;

import static org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes.BLOCK_TARGET_POS;
import static org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag.FIRE_IMMUNE;

public class EntityEnderCrystal extends CloudEntity implements EnderCrystal, Explosive {

    private static final String TAG_SHOW_BOTTOM = "ShowBottom";
    private static final String TAG_BEAM_TARGET = "BeamTarget";

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
    public float getWidth() {
        return 0.98f;
    }

    @Override
    public boolean attack(EntityDamageEvent source) {
        if (this.isInvulnerable() || source.getDamageType().is(DamageTypeTags.IS_FIRE)) {
            return false;
        }

        if (!super.attack(source)) {
            return false;
        }

        explode();

        return true;
    }

    @Override
    public void explode() {
        Explosion explode = new Explosion(this.getLevel(), this.getPosition(), 6, this);
        explode.setDestroysBlocks(this.level.getGameRules().get(GameRules.MOB_GRIEFING));

        this.close();

        if (explode.explodeA()) {
            explode.explodeB();
        }
    }

    @Override
    public boolean canCollideWith(Entity entity) {
        return false;
    }

    @Override
    public void loadAdditionalData(NbtMap tag) {
        super.loadAdditionalData(tag);
        tag.listenForBoolean(TAG_SHOW_BOTTOM, this::setShowingBase);
        tag.listenForCompound(TAG_BEAM_TARGET, beam -> this.setBeamTarget(Vector3i.from(
                beam.getInt("X"), beam.getInt("Y"), beam.getInt("Z"))));
    }

    @Override
    public void saveAdditionalData(NbtMapBuilder tag) {
        super.saveAdditionalData(tag);
        tag.putBoolean(TAG_SHOW_BOTTOM, this.isShowingBase());
        Vector3i beamTarget = this.getBeamTarget();
        if (beamTarget != null) {
            tag.putCompound(TAG_BEAM_TARGET, NbtMap.builder()
                    .putInt("X", beamTarget.getX())
                    .putInt("Y", beamTarget.getY())
                    .putInt("Z", beamTarget.getZ())
                    .build());
        }
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
