package org.cloudburstmc.server.block.component;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.component.PlaceBlockHandler;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.block.util.RailConnector;
import org.cloudburstmc.server.level.CloudLevel;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class RailPlaceHandler implements PlaceBlockHandler {

    public static final RailPlaceHandler INSTANCE = new RailPlaceHandler();

    @Override
    public boolean execute(BlockState blockState, Player player, Vector3i blockPosition, Direction face, Vector3f clickPosition) {
        if (player == null) {
            return false;
        }

        CloudLevel level = (CloudLevel) player.getLevel();
        if (!RailConnector.hasRigidSupport(level, blockPosition)) {
            return false;
        }

        RailConnector.place(level, blockPosition, blockState);
        return true;
    }
}
