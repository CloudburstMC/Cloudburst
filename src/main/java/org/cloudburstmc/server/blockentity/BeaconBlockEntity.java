package org.cloudburstmc.server.blockentity;

import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import com.nukkitx.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.api.block.BlockCategory;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.blockentity.Beacon;
import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.api.potion.EffectType;
import org.cloudburstmc.api.potion.EffectTypes;
import org.cloudburstmc.server.inventory.CloudBeaconInventory;
import org.cloudburstmc.server.network.NetworkUtils;
import org.cloudburstmc.server.network.protocol.types.ContainerIds;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.potion.CloudEffect;

import java.util.Map;

import static org.cloudburstmc.api.block.BlockTypes.*;

/**
 * author: Rover656
 */
public class BeaconBlockEntity extends BaseBlockEntity implements Beacon {

    private EffectType primaryEffect;
    private EffectType secondaryEffect;
    private int powerLevel;

    public BeaconBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position);
    }

    @Override
    public void loadAdditionalData(NbtMap tag) {
        super.loadAdditionalData(tag);

        tag.listenForInt("primary", this::setPrimaryEffect);
        tag.listenForInt("secondary", this::setSecondaryEffect);
    }

    @Override
    public void saveAdditionalData(NbtMapBuilder tag) {
        super.saveAdditionalData(tag);
        tag.putInt("primary", NetworkUtils.effectToNetwork(this.getPrimaryEffect()));
        tag.putInt("secondary", NetworkUtils.effectToNetwork(this.getSecondaryEffect()));
    }

    @Override
    public boolean isValid() {
        return getBlockState().getType() == BlockTypes.BEACON;
    }

    private long currentTick = 0;

    @Override
    public boolean onUpdate() {
        if (currentTick++ % 80 != 0) {
            return true;
        }

        updateBeacon();

        return true;
    }

    private void updateBeacon() {
        int oldPowerLevel = this.powerLevel;
        this.powerLevel = calculatePowerLevel();
        int newPowerLevel = this.powerLevel;

        if (newPowerLevel < 1 || !hasSkyAccess()) {
            if (oldPowerLevel > 0) {
                this.getLevel().addLevelSoundEvent(this.getPosition(), SoundEvent.BEACON_DEACTIVATE);
            }
            return;
        } else if (oldPowerLevel < 1) {
            this.getLevel().addLevelSoundEvent(this.getPosition(), SoundEvent.BEACON_ACTIVATE);
        } else {
            this.getLevel().addLevelSoundEvent(this.getPosition(), SoundEvent.BEACON_AMBIENT);
        }

        applyBeaconEffects();
    }

    private void applyBeaconEffects() {
        Map<Long, CloudPlayer> players = this.getLevel().getPlayers();

        int range = 10 + this.powerLevel * 10;
        int duration = 9 + this.powerLevel * 2;

        for (Map.Entry<Long, CloudPlayer> entry : players.entrySet()) {
            CloudPlayer p = entry.getValue();

            if (p.getPosition().distance(this.getPosition().toFloat()) < range) {
                applyPrimaryEffect(p, duration);
                applySecondaryEffect(p, duration);
            }
        }
    }

    private void applyPrimaryEffect(CloudPlayer player, int duration) {
        if (getPrimaryEffect() != null) {
            CloudEffect effect = new CloudEffect(getPrimaryEffect())
                    .setDuration(duration * 20)
                    .setVisible(false);

            if (getSecondaryEffect() == getPrimaryEffect()) {
                effect.setAmplifier(1);
            } else {
                effect.setAmplifier(0);
            }

            player.addEffect(effect);
        }
    }

    private void applySecondaryEffect(CloudPlayer player, int duration) {
        if (getSecondaryEffect() == EffectTypes.REGENERATION) {
            CloudEffect effect = new CloudEffect(EffectTypes.REGENERATION)
                    .setDuration(duration * 20)
                    .setAmplifier(0)
                    .setVisible(false);

            player.addEffect(effect);
        }
    }


    private static final int POWER_LEVEL_MAX = 4;

    private boolean hasSkyAccess() {
        //Check every block from our y coord to the top of the world
        for (int y = getPosition().getY() + 1; y <= 255; y++) {
            var state = getLevel().getBlockState(getPosition().getX(), y, getPosition().getZ());
            if (!state.inCategory(BlockCategory.TRANSPARENT)) {
                //There is no sky access
                return false;
            }
        }

        return true;
    }

    private int calculatePowerLevel() {
        int tileX = this.getPosition().getX();
        int tileY = this.getPosition().getY();
        int tileZ = this.getPosition().getZ();

        //The power level that we're testing for
        for (int powerLevel = 1; powerLevel <= POWER_LEVEL_MAX; powerLevel++) {
            int queryY = tileY - powerLevel; //Layer below the beacon block

            for (int queryX = tileX - powerLevel; queryX <= tileX + powerLevel; queryX++) {
                for (int queryZ = tileZ - powerLevel; queryZ <= tileZ + powerLevel; queryZ++) {

                    var testBlockId = getLevel().getBlockState(queryX, queryY, queryZ).getType();
                    if (testBlockId != IRON_BLOCK && testBlockId != GOLD_BLOCK && testBlockId != EMERALD_BLOCK &&
                            testBlockId != DIAMOND_BLOCK) {
                        return powerLevel - 1;
                    }
                }
            }
        }

        return POWER_LEVEL_MAX;
    }

    public EffectType getPrimaryEffect() {
        return primaryEffect;
    }

    public void setPrimaryEffect(EffectType primaryEfect) {
        if (primaryEfect != this.primaryEffect) {
            this.primaryEffect = primaryEfect;
            setDirty();
            this.spawnToAll();
        }
    }

    public EffectType getSecondaryEffect() {
        return secondaryEffect;
    }

    public void setSecondaryEffect(EffectType secondaryEffect) {
        if (secondaryEffect != this.secondaryEffect) {
            this.secondaryEffect = secondaryEffect;
            setDirty();
            this.spawnToAll();
        }
    }

    public void setPrimaryEffect(int legacyId) {
        this.setPrimaryEffect(NetworkUtils.effectFromLegacy((byte) legacyId));
    }

    public void setSecondaryEffect(int legacyId) {
        this.setSecondaryEffect(NetworkUtils.effectFromLegacy((byte) legacyId));
    }

    @Override
    public boolean updateNbtMap(NbtMap nbt, CloudPlayer player) {
        this.setPrimaryEffect(NetworkUtils.effectFromLegacy((byte) nbt.getInt("primary")));
        this.setSecondaryEffect(NetworkUtils.effectFromLegacy((byte) nbt.getInt("secondary")));

        this.getLevel().addLevelSoundEvent(this.getPosition(), SoundEvent.BEACON_POWER);

        CloudBeaconInventory inv = (CloudBeaconInventory) player.getWindowById(ContainerIds.BEACON);

        inv.clear(0);
        this.scheduleUpdate();
        return true;
    }

    @Override
    public boolean isSpawnable() {
        return true;
    }
}
