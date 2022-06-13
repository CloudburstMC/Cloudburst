package org.cloudburstmc.api.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.math.vector.Vector3i;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.behavior.Behavior;

public interface PlaceBlockBehavior {

    boolean execute(
            Behavior<PlaceBlockBehavior.Executor> behavior,
            BlockState blockState,
            Player player,
            Vector3i blockPosition,
            Direction face,
            Vector3f clickPosition
    );

    @FunctionalInterface
    interface Executor {

        boolean execute(
                BlockState blockState,
                Player player,
                Vector3i blockPosition,
                Direction face,
                Vector3f clickPosition
        );
    }
}
