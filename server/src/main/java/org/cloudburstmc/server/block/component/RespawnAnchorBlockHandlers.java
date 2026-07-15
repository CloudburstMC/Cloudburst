package org.cloudburstmc.server.block.component;

import lombok.experimental.UtilityClass;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.block.component.UseBlockHandler;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.level.gamerule.GameRules;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.server.entity.vehicle.DismountHelper;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.level.Explosion;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.List;

@UtilityClass
public class RespawnAnchorBlockHandlers {

    private static final int MAX_CHARGES = 4;
    private static final List<Vector3i> HORIZONTAL_OFFSETS = List.of(
            Vector3i.from(0, 0, -1), Vector3i.from(-1, 0, 0),
            Vector3i.from(0, 0, 1), Vector3i.from(1, 0, 0),
            Vector3i.from(-1, 0, -1), Vector3i.from(1, 0, -1),
            Vector3i.from(-1, 0, 1), Vector3i.from(1, 0, 1)
    );

    public static final UseBlockHandler RESPAWN_ANCHOR = (block, player, direction, item) -> {
        if (!(player instanceof CloudPlayer cloudPlayer)) {
            return false;
        }

        CloudLevel level = (CloudLevel) block.getLevel();
        Vector3i pos = block.getPosition();
        int currentCharge = block.getState().ensureTrait(BlockTraits.RESPAWN_ANCHOR_CHARGE);

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

    public static @Nullable Vector3f findStandUpPosition(CloudLevel level, Vector3i position) {
        Vector3f safePosition = findStandUpPosition(level, position, true);
        return safePosition != null ? safePosition : findStandUpPosition(level, position, false);
    }

    private static @Nullable Vector3f findStandUpPosition(CloudLevel level, Vector3i position, boolean avoidDanger) {
        Vector3f safePosition = findHorizontalStandUpPosition(level, position, 0, avoidDanger);
        if (safePosition != null) {
            return safePosition;
        }
        safePosition = findHorizontalStandUpPosition(level, position, -1, avoidDanger);
        if (safePosition != null) {
            return safePosition;
        }
        safePosition = findHorizontalStandUpPosition(level, position, 1, avoidDanger);
        return safePosition != null ? safePosition
                : DismountHelper.findSafeDismountLocation(level, position.add(0, 1, 0), avoidDanger);
    }

    private static @Nullable Vector3f findHorizontalStandUpPosition(CloudLevel level, Vector3i position,
                                                                     int yOffset, boolean avoidDanger) {
        for (Vector3i horizontalOffset : HORIZONTAL_OFFSETS) {
            Vector3i candidate = position.add(horizontalOffset).add(0, yOffset, 0);
            Vector3f safePosition = DismountHelper.findSafeDismountLocation(level, candidate, avoidDanger);
            if (safePosition != null) {
                return safePosition;
            }
        }
        return null;
    }
}
