package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.block.BlockCategory;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.blockentity.Beacon;
import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.api.potion.EffectType;
import org.cloudburstmc.api.potion.EffectTypes;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
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
        //Only apply effects every 4 secs
        if (currentTick++ % 80 != 0) {
            return true;
        }

        int oldPowerLevel = this.powerLevel;
        //Get the power level based on the pyramid
        this.powerLevel = calculatePowerLevel();
        int newPowerLevel = this.powerLevel;

        //Skip beacons that do not have a pyramid or sky access
        if (newPowerLevel < 1 || !hasSkyAccess()) {
            if (oldPowerLevel > 0) {
                this.getLevel().addLevelSoundEvent(this.getPosition(), SoundEvent.BEACON_DEACTIVATE);
            }
            return true;
        } else if (oldPowerLevel < 1) {
            this.getLevel().addLevelSoundEvent(this.getPosition(), SoundEvent.BEACON_ACTIVATE);
        } else {
            this.getLevel().addLevelSoundEvent(this.getPosition(), SoundEvent.BEACON_AMBIENT);
        }

        //Get all players in game
        Map<Long, CloudPlayer> players = this.getLevel().getPlayers();

        //Calculate vars for beacon power
        int range = 10 + this.powerLevel * 10;
        int duration = 9 + this.powerLevel * 2;

        for (Map.Entry<Long, CloudPlayer> entry : players.entrySet()) {
            CloudPlayer p = entry.getValue();

            //If the player is in range
            if (p.getPosition().distance(this.getPosition().toFloat()) < range) {
                CloudEffect e;

                if (getPrimaryEffect() != null) {
                    //Apply the primary power
                    e = new CloudEffect(getPrimaryEffect())
                            .setDuration(duration * 20)
                            .setVisible(false);

                    //If secondary is selected as the primary too, apply 2 amplification
                    if (getSecondaryEffect() == getPrimaryEffect()) {
                        e.setAmplifier(1);
                    } else {
                        e.setAmplifier(0);
                    }

                    //Add the effect
                    p.addEffect(e);
                }

                //If we have a secondary power as regen, apply it
                if (getSecondaryEffect() == EffectTypes.REGENERATION) {
                    //Get the regen effect
                    e = new CloudEffect(EffectTypes.REGENERATION)
                            .setDuration(duration * 20)
                            .setAmplifier(0)
                            .setVisible(false);

                    //Add effect
                    p.addEffect(e);
                }
            }
        }

        return true;
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
