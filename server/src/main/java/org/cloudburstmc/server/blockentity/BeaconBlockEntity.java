package org.cloudburstmc.server.blockentity;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.blockentity.Beacon;
import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.event.block.BeaconActivatedEvent;
import org.cloudburstmc.api.event.block.BeaconDeactivatedEvent;
import org.cloudburstmc.api.event.block.BeaconEffectEvent;
import org.cloudburstmc.api.event.entity.PotionEffectCause;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.api.level.sound.SoundType;
import org.cloudburstmc.api.level.sound.SoundTypes;
import org.cloudburstmc.api.potion.EffectType;
import org.cloudburstmc.api.potion.EffectTypes;
import org.cloudburstmc.api.potion.PotionEffect;
import org.cloudburstmc.api.util.BoundingBox;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.server.blockentity.data.CloudBeaconData;
import org.cloudburstmc.server.blockentity.data.CloudBlockEntityData;
import org.cloudburstmc.server.level.chunk.LockedChunk;
import org.cloudburstmc.server.network.NetworkUtils;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.Map;

public class BeaconBlockEntity extends BaseBlockEntity implements Beacon {

    private static final int MAX_TIER = 4;
    private static final int UPDATE_INTERVAL = 80;
    private static final Map<EffectType, Integer> PRIMARY_TIERS = Map.of(
            EffectTypes.SPEED, 1,
            EffectTypes.HASTE, 1,
            EffectTypes.RESISTANCE, 2,
            EffectTypes.JUMP_BOOST, 2,
            EffectTypes.STRENGTH, 3
    );

    private int tier;
    private boolean active;
    private double effectRange = -1;
    private int nextUpdate;

    public BeaconBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position);
    }

    @Override
    protected Class<? extends CloudBlockEntityData> minimumDataClass() {
        return CloudBeaconData.class;
    }

    @Override
    protected CloudBeaconData data() {
        return (CloudBeaconData) super.data();
    }

    @Override
    public int getTier() {
        return this.tier;
    }

    @Override
    public boolean isActive() {
        return this.active;
    }

    @Override
    public double getEffectRange() {
        return this.effectRange < 0 ? 10 + 10 * this.tier : this.effectRange;
    }

    @Override
    public void setEffectRange(double range) {
        if (!Double.isFinite(range) || range < 0) {
            throw new IllegalArgumentException("Beacon range must be finite and non-negative");
        }

        this.effectRange = range;
        this.setDirty();
    }

    @Override
    public void resetEffectRange() {
        this.effectRange = -1;
        this.setDirty();
    }

    @Override
    public @Nullable EffectType getPrimaryEffect() {
        return this.data().getPrimary();
    }

    @Override
    public @Nullable EffectType getSecondaryEffect() {
        return this.data().getSecondary();
    }

    @Override
    public void setEffects(@Nullable EffectType primary, @Nullable EffectType secondary) {
        encodeEffect(primary);
        encodeEffect(secondary);

        if (this.data().getPrimary() != primary || this.data().getSecondary() != secondary) {
            this.data().setPrimary(primary);
            this.data().setSecondary(secondary);
            this.setDirty();
            this.spawnToAll();
        }
    }

    public static boolean supportsSelection(int tier, @Nullable EffectType primary, @Nullable EffectType secondary) {
        if (primary == null || PRIMARY_TIERS.getOrDefault(primary, Integer.MAX_VALUE) > tier) {
            return false;
        }

        return secondary == null || (tier == MAX_TIER && (secondary == primary || secondary == EffectTypes.REGENERATION));
    }

    public void confirmEffects(@Nullable EffectType primary, @Nullable EffectType secondary) {
        this.setEffects(primary, secondary);
        this.playSound(SoundTypes.BEACON_POWER);
    }

    @Override
    public boolean isSpawnable() {
        return true;
    }

    @Override
    public boolean onUpdate() {
        super.onUpdate();

        int tick = this.getServer().getTick();
        if (tick - this.nextUpdate >= 0) {
            this.nextUpdate = tick + UPDATE_INTERVAL;
            boolean wasActive = this.active;
            this.refreshStructure();
            if (this.active && !this.isClosed() && this.isValid()) {
                if (wasActive) {
                    this.playSound(SoundTypes.BEACON_AMBIENT);
                }

                this.applyEffects();
            }
        }

        return !this.isClosed();
    }

    public void refreshStructure() {
        int previousTier = this.tier;
        this.tier = this.scanPyramid();
        this.setActive(this.tier > 0 && this.hasClearBeam());

        if (this.tier != previousTier && !this.isClosed()) {
            this.spawnToAll();
        }
    }

    @Override
    public void onBreak() {
        this.setActive(false);
    }

    @Override
    protected void loadCustom(NbtMap tag) {

        this.data().setPrimary(decodeStoredEffect(tag.getInt("Primary", tag.getInt("primary", 0))));
        this.data().setSecondary(decodeStoredEffect(tag.getInt("Secondary", tag.getInt("secondary", 0))));

        double range = tag.get("EffectRange") instanceof Double value ? value : -1;
        this.effectRange = Double.isFinite(range) && range >= 0 ? range : -1;
    }

    @Override
    protected void saveCustom(NbtMapBuilder tag) {
        tag.remove("primary");
        tag.remove("secondary");
        tag.putInt("Primary", encodeEffect(this.data().getPrimary()));
        tag.putInt("Secondary", encodeEffect(this.data().getSecondary()));

        if (this.effectRange >= 0) {
            tag.putDouble("EffectRange", this.effectRange);
        } else {
            tag.remove("EffectRange");
        }
    }

    @Override
    protected void saveCustomClient(NbtMapBuilder tag) {
        tag.putInt("Levels", this.tier);
        tag.putInt("Primary", encodeEffect(this.data().getPrimary()));
        tag.putInt("Secondary", encodeEffect(this.data().getSecondary()));
    }

    private void setActive(boolean active) {
        if (this.active == active) {
            return;
        }

        this.active = active;
        this.playSound(active ? SoundTypes.BEACON_ACTIVATE : SoundTypes.BEACON_DEACTIVATE);

        Block block = this.getBlock();
        this.getServer().getEventManager().fire(active ? new BeaconActivatedEvent(block) : new BeaconDeactivatedEvent(block));
    }

    private int scanPyramid() {
        Vector3i position = this.getPosition();

        for (int depth = 1; depth <= MAX_TIER; depth++) {
            int y = position.getY() - depth;
            if (y < this.getLevel().getMinHeight()) {
                return depth - 1;
            }

            for (int x = position.getX() - depth; x <= position.getX() + depth; x++) {
                for (int z = position.getZ() - depth; z <= position.getZ() + depth; z++) {
                    Block block = this.getLevel().getLoadedBlock(x, y, z);
                    if (block == null || !block.getState().is(BlockTags.BEACON_BASE_BLOCKS)) {
                        return depth - 1;
                    }
                }
            }
        }

        return MAX_TIER;
    }

    private boolean hasClearBeam() {
        Vector3i position = this.getPosition();
        int x = position.getX() & 15;
        int z = position.getZ() & 15;

        try (LockedChunk chunk = this.getChunk().lockForRead()) {
            int highest = chunk.getHighestBlock(x, z);
            for (int y = position.getY() + 1; y <= highest; y++) {
                BlockState state = chunk.getBlockState(x, y, z, BlockLayer.PRIMARY);
                if (state.getType() != BlockTypes.BEDROCK && state.getLightDampening() >= 15) {
                    return false;
                }
            }
        }

        return true;
    }

    private void applyEffects() {
        EffectType primary = this.data().getPrimary();
        if (primary == null) {
            return;
        }

        EffectType secondary = this.tier == MAX_TIER ? this.data().getSecondary() : null;
        int duration = (9 + 2 * this.tier) * 20;
        PotionEffect main = new PotionEffect(primary, duration, secondary == primary ? 1 : 0, true, true);
        PotionEffect extra = secondary == null || secondary == primary ? null : new PotionEffect(secondary, duration, 0, true, true);

        double range = this.getEffectRange();
        Vector3i position = this.getPosition();

        for (CloudPlayer player : this.getLevel().getPlayers().values().toArray(CloudPlayer[]::new)) {
            if (!player.isAlive() || player.isSpectator() || player.getLevel() != this.getLevel()) {
                continue;
            }

            BoundingBox bounds = player.getBoundingBox();
            if (bounds.getMaxX() <= position.getX() - range
                    || bounds.getMinX() >= position.getX() + range + 1
                    || bounds.getMaxZ() <= position.getZ() - range
                    || bounds.getMinZ() >= position.getZ() + range + 1
                    || bounds.getMaxY() <= position.getY() - range) {
                continue;
            }

            this.applyEffect(player, main, true);
            if (this.isClosed() || !this.isValid()) {
                return;
            }

            if (extra != null && player.getLevel() == this.getLevel()) {
                this.applyEffect(player, extra, false);
                if (this.isClosed() || !this.isValid()) {
                    return;
                }
            }
        }
    }

    private void applyEffect(CloudPlayer player, PotionEffect effect, boolean primary) {
        BeaconEffectEvent event = new BeaconEffectEvent(this.getBlock(), effect, player, primary);
        this.getServer().getEventManager().fire(event);

        if (!event.isCancelled() && !this.isClosed() && this.isValid() && player.getLevel() == this.getLevel()) {
            player.addPotionEffect(event.getEffect(), null, PotionEffectCause.BEACON);
        }
    }

    private void playSound(SoundType sound) {
        this.getLevel().playSound(this.getPosition().toFloat().add(0.5f, 0.5f, 0.5f), sound);
    }

    private static int encodeEffect(@Nullable EffectType effect) {
        return effect == null ? 0 : Byte.toUnsignedInt(NetworkUtils.effectToNetwork(effect));
    }

    private static @Nullable EffectType decodeStoredEffect(int id) {
        if (id <= 0 || id > 255) {
            return null;
        }

        try {
            return NetworkUtils.effectFromNetwork((byte) id);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
