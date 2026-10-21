package org.cloudburstmc.server.blockentity;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.blockentity.Conduit;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.Living;
import org.cloudburstmc.api.entity.damage.DamageSource;
import org.cloudburstmc.api.entity.damage.DamageTypes;
import org.cloudburstmc.api.event.entity.PotionEffectCause;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.api.level.sound.SoundTypes;
import org.cloudburstmc.api.potion.EffectTypes;
import org.cloudburstmc.api.potion.PotionEffect;
import org.cloudburstmc.api.util.BoundingBox;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.server.entity.hostile.EntityHostile;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

public class ConduitBlockEntity extends BaseBlockEntity implements Conduit {

    private static final int REFRESH_INTERVAL = 40;
    private static final int AMBIENT_INTERVAL = 80;
    private static final int HUNTING_RANGE = 8;
    private static final int EFFECT_DURATION = 260;
    private static final float ATTACK_DAMAGE = 4;

    private ConduitStructure structure = new ConduitStructure(List.of());
    private @Nullable Living target;
    private boolean initialized;
    private long nextShortSound;

    public ConduitBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position);
    }

    @Override
    public boolean onUpdate() {
        if (this.isClosed()) {
            return false;
        }

        if (!this.isValid()) {
            this.close();
            return false;
        }

        long tick = this.getLevel().getGameTime();
        if (!this.initialized || tick % REFRESH_INTERVAL == 0) {
            this.refreshStructure();
            this.initialized = true;
        }

        if (this.isClosed()) {
            return false;
        }

        super.onUpdate();
        if (this.isActive()) {
            if (tick % AMBIENT_INTERVAL == 0) {
                this.getLevel().playSound(this.getPosition(), SoundTypes.CONDUIT_AMBIENT);
            }

            if (tick >= this.nextShortSound) {
                this.nextShortSound = tick + 60 + ThreadLocalRandom.current().nextInt(40);
                this.getLevel().playSound(this.getPosition(), SoundTypes.CONDUIT_SHORT);
            }
        }

        return true;
    }

    @Override
    public boolean isSpawnable() {
        return true;
    }

    @Override
    public void saveAdditionalData(NbtMapBuilder tag) {
        super.saveAdditionalData(tag);
        tag.remove("Target");
        tag.remove("Active");
    }

    @Override
    protected void saveClientData(NbtMapBuilder tag) {
        Living current = this.getTarget();
        tag.putBoolean("Active", this.isActive());
        tag.putLong("Target", current == null ? -1 : current.getUniqueId());
    }

    @Override
    public boolean isActive() {
        return this.structure.isActive();
    }

    @Override
    public boolean isHunting() {
        return this.structure.isHunting();
    }

    @Override
    public Collection<Block> getFrameBlocks() {
        return this.structure.frameBlocks().stream()
                .map(this.getLevel()::getLoadedBlock)
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public int getFrameBlockCount() {
        return this.structure.frameBlocks().size();
    }

    @Override
    public int getRange() {
        return this.structure.range();
    }

    @Override
    public BoundingBox getHuntingArea() {
        Vector3i position = this.getPosition();
        return new BoundingBox(position.getX() - HUNTING_RANGE, position.getY() - HUNTING_RANGE, position.getZ() - HUNTING_RANGE,
                position.getX() + HUNTING_RANGE + 1, position.getY() + HUNTING_RANGE + 1, position.getZ() + HUNTING_RANGE + 1);
    }

    @Override
    public @Nullable Living getTarget() {
        Living current = this.target;
        return this.isHunting() && current != null && this.isValidTarget(current) ? current : null;
    }

    @Override
    public boolean hasTarget() {
        return this.getTarget() != null;
    }

    @Override
    public boolean setTarget(@Nullable Living target) {
        if (target == this.target || target != null && (!this.isHunting() || !this.isValidTarget(target))) {
            return false;
        }

        this.target = target;
        this.synchronizeState();
        return true;
    }

    private void refreshStructure() {
        boolean wasActive = this.isActive();
        Living previousTarget = this.target;
        this.structure = ConduitStructure.scan(this.getLevel(), this.getPosition());
        if (wasActive != this.isActive()) {
            this.getLevel().playSound(this.getPosition(), this.isActive() ? SoundTypes.CONDUIT_ACTIVATE : SoundTypes.CONDUIT_DEACTIVATE);
        }

        if (this.isActive()) {
            this.applyEffects();
        }

        if (this.isClosed()) {
            return;
        }

        if (!this.isHunting()) {
            this.target = null;
        } else if (this.target == null) {
            this.target = this.selectTarget();
        } else if (!this.isValidTarget(this.target)) {
            this.target = null;
        }

        Living current = this.target;
        if (current != null && current.damage(ATTACK_DAMAGE, DamageSource.builder(DamageTypes.MAGIC).block(this.getBlock()).build())) {
            this.getLevel().playSound(current.getPosition(), SoundTypes.CONDUIT_ATTACK);
        }

        if (wasActive != this.isActive() || previousTarget != this.target) {
            this.synchronizeState();
        }
    }

    private void applyEffects() {
        int range = this.getRange();
        PotionEffect effect = new PotionEffect(EffectTypes.CONDUIT_POWER, EFFECT_DURATION, 0, true, true);
        for (CloudPlayer player : this.getLevel().getPlayers().values()) {
            if (player.isAlive() && withinRange(this.getPosition(), player, range) && player.isInWaterOrRain()) {
                player.addPotionEffect(effect, null, PotionEffectCause.CONDUIT);
            }
        }
    }

    private @Nullable Living selectTarget() {
        List<Living> candidates = new ArrayList<>();
        for (Entity entity : this.getLevel().getNearbyEntities(this.getHuntingArea())) {
            if (entity instanceof EntityHostile hostile && this.isValidTarget(hostile) && hostile.isInWaterOrRain()) {
                candidates.add(hostile);
            }
        }

        return candidates.isEmpty() ? null : candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
    }

    private boolean isValidTarget(Living target) {
        return target.isAlive() && !target.isClosed() && target.getLevel() == this.getLevel() && withinRange(this.getPosition(), target, HUNTING_RANGE);
    }

    private void synchronizeState() {
        if (this.isClosed()) {
            return;
        }

        this.setDirty();
        if (this.initialized) {
            this.spawnToAll();
        }
    }

    private static boolean withinRange(Vector3i origin, Entity entity, int range) {
        Vector3i position = entity.getPosition().toInt();
        double dx = (double) position.getX() - origin.getX();
        double dy = (double) position.getY() - origin.getY();
        double dz = (double) position.getZ() - origin.getZ();
        return dx * dx + dy * dy + dz * dz < (double) range * range;
    }
}
