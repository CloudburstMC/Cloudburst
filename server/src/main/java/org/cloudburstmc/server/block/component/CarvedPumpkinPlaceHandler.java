package org.cloudburstmc.server.block.component;

import lombok.NoArgsConstructor;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.event.entity.CreatureSpawnEvent;
import org.cloudburstmc.api.event.entity.CreatureSpawnReason;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.entity.passive.EntitySnowGolem;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.registry.CloudEntityRegistry;

@NoArgsConstructor
public class CarvedPumpkinPlaceHandler extends DefaultBlockPlaceHandler {

    @Override
    public boolean execute(BlockState state, Player player, Vector3i position, Direction face, Vector3f clickPosition) {
        if (!super.execute(state, player, position, face, clickPosition)) {
            return false;
        }

        CloudLevel level = (CloudLevel) player.getLevel();
        Vector3i middle = Direction.DOWN.relative(position);
        Vector3i bottom = Direction.DOWN.relative(middle);
        if (level.getBlockState(middle) != BlockStates.SNOW || level.getBlockState(bottom) != BlockStates.SNOW) {
            return true;
        }

        Location location = Location.from(bottom.toFloat().add(0.5f, 0, 0.5f), level);
        EntitySnowGolem golem = (EntitySnowGolem) CloudEntityRegistry.get().newEntity(EntityTypes.SNOW_GOLEM, location);
        CreatureSpawnEvent event = new CreatureSpawnEvent(golem, CreatureSpawnReason.BUILD_SNOWMAN);

        level.setBlockState(position, BlockStates.AIR, true, true);
        level.setBlockState(middle, BlockStates.AIR, true, true);
        level.setBlockState(bottom, BlockStates.AIR, true, true);

        if (!golem.spawn(event)) {
            level.setBlockState(position, state, true, true);
            level.setBlockState(middle, BlockStates.SNOW, true, true);
            level.setBlockState(bottom, BlockStates.SNOW, true, true);
        }

        return true;
    }
}
