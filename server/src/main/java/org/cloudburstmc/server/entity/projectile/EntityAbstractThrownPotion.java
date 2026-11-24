package org.cloudburstmc.server.entity.projectile;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.projectile.ThrownPotion;
import org.cloudburstmc.api.event.entity.EntityRemoveCause;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.potion.PotionType;
import org.cloudburstmc.api.potion.PotionTypes;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.protocol.bedrock.data.LevelEvent;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.server.entity.data.CloudThrownPotionData;
import org.cloudburstmc.server.network.LevelEffectPacketFactory;
import org.cloudburstmc.server.network.NetworkUtils;
import org.cloudburstmc.server.potion.CloudPotionColor;

import static java.util.Objects.requireNonNull;
import static org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes.AUX_VALUE_DATA;
import static org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag.ENCHANTED;

public abstract class EntityAbstractThrownPotion extends EntityProjectile implements ThrownPotion {

    private final CloudThrownPotionData persistenceState;

    protected EntityAbstractThrownPotion(EntityType<? extends ThrownPotion> type, Location location) {
        CloudThrownPotionData state = new CloudThrownPotionData();
        super(type, location, state);
        this.persistenceState = state;
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.setPotionType(PotionTypes.WATER);
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
        return 0.05f;
    }

    @Override
    public float getDrag() {
        return 0.01f;
    }

    @Override
    protected void onCollideWithEntity(Entity entity) {
        this.splash(entity);
    }

    protected void splash(Entity collidedWith) {
        if (this.isClosed()) {
            return;
        }

        this.remove(EntityRemoveCause.HIT);
        int color = CloudPotionColor.calculateEffects(this.getPotionType().getEffects()).orElse(0x2828ff);
        this.getLevel().addChunkPacket(this.getPosition(), LevelEffectPacketFactory.event(this.getPosition(), LevelEvent.PARTICLE_POTION_SPLASH, 0xff000000 | color));
        this.getLevel().addLevelSoundEvent(this.getPosition(), SoundEvent.GLASS);

        this.applyPotionImpact(collidedWith);
    }

    protected abstract void applyPotionImpact(Entity collidedWith);

    @Override
    protected boolean tickEntity(int currentTick, int elapsedTicks) {
        if (this.isClosed()) {
            return false;
        }

        boolean hasUpdate = super.tickEntity(currentTick, elapsedTicks);
        if (this.isClosed()) {
            return hasUpdate;
        }

        if (this.age > 1200) {
            this.remove(EntityRemoveCause.DESPAWN);
            hasUpdate = true;
        } else if (this.hasBlockImpact()) {
            this.splash(null);
            hasUpdate = true;
        }

        return hasUpdate;
    }

    @Override
    public PotionType getPotionType() {
        PotionType potionId = this.data().getPotionId();
        return potionId == null ? PotionTypes.WATER : potionId;
    }

    @Override
    public void setPotionType(PotionType type) {
        requireNonNull(type, "type");
        this.data().setPotionId(type);
        this.data.set(AUX_VALUE_DATA, NetworkUtils.potionToNetwork(type));
        this.data.setFlag(ENCHANTED, type != PotionTypes.WATER && type != PotionTypes.MUNDANE && type != PotionTypes.THICK && type != PotionTypes.AWKWARD);
    }

    @Override
    protected CloudThrownPotionData data() {
        return this.persistenceState;
    }

    @Override
    protected void loadCustom(NbtMap tag) {
        if (tag.containsKey("PotionId")) {
            PotionType potionId = this.data().getPotionId();
            if (potionId != null) {
                this.setPotionType(potionId);
            }
        }
        super.loadCustom(tag);
    }
}
