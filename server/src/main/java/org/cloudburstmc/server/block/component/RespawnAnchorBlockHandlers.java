package org.cloudburstmc.server.block.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.block.component.UseBlockHandler;
import org.cloudburstmc.api.block.component.UseCheckHandler;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.level.gamerule.GameRules;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.level.Explosion;
import org.cloudburstmc.server.player.CloudPlayer;

/**
 * Block interaction handlers for the respawn anchor block.
 */
@UtilityClass
public class RespawnAnchorBlockHandlers {

    private static final int MAX_CHARGES = 4;

    /**
     * Deny interaction while the player is sneaking with a non-empty item in hand.
     */
    public static final UseCheckHandler CAN_BE_USED = (block, player) ->
            !player.isSneaking() || player.getInventory().getSelectedItem().isEmpty();

    public static final UseBlockHandler RESPAWN_ANCHOR = (block, player, direction, item) -> {
        if (!(player instanceof CloudPlayer cloudPlayer)) {
            return false;
        }

        CloudLevel level = (CloudLevel) block.getLevel();
        Vector3i pos = block.getPosition();
        int currentCharge;
        try {
            currentCharge = block.getState().ensureTrait(BlockTraits.RESPAWN_ANCHOR_CHARGE);
        } catch (Exception e) {
            currentCharge = 0;
        }

        if (!item.isEmpty()
                && item.getType().getId() == BlockTypes.GLOWSTONE.getId()
                && currentCharge < MAX_CHARGES) {
            int newCharge = currentCharge + 1;
            BlockState newState = block.getState().withTrait(BlockTraits.RESPAWN_ANCHOR_CHARGE, newCharge);
            level.setBlockState(pos.getX(), pos.getY(), pos.getZ(), 0, newState, false, true);

            if (!cloudPlayer.isCreative()) {
                cloudPlayer.getInventory().setSelectedItem(item.decreaseCount());
            }

            level.addLevelSoundEvent(pos, SoundEvent.RESPAWN_ANCHOR_CHARGE);
            return true;
        }

        if (currentCharge <= 0) {
            return false;
        }

        if (level.getDimension() != CloudLevel.DIMENSION_NETHER) {
            if (!level.getGameRules().get(GameRules.RESPAWN_BLOCKS_EXPLODE)) {
                return true;
            }

            level.setBlockState(pos.getX(), pos.getY(), pos.getZ(), 0, BlockStates.AIR, false, true);
            Explosion explosion = new Explosion(level,
                    Vector3f.from(pos.getX() + 0.5f, pos.getY() + 0.5f, pos.getZ() + 0.5f),
                    5, cloudPlayer);
            explosion.explodeA();
            explosion.explodeB();
            return true;
        }

        Location anchorLoc = Location.from(pos.toFloat(), 0f, 0f, level);
        boolean changed = cloudPlayer.setSpawnFromAnchor(pos, anchorLoc);
        if (changed) {
            level.addLevelSoundEvent(pos, SoundEvent.RESPAWN_ANCHOR_SET_SPAWN);
        }

        return true;
    };
}
