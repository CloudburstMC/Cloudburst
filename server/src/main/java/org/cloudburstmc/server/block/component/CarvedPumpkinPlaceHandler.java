package org.cloudburstmc.server.block.component;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.event.entity.CreatureSpawnEvent;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.entity.passive.EntitySnowGolem;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.registry.CloudBlockRegistry;
import org.cloudburstmc.server.registry.EntityRegistry;

public final class CarvedPumpkinPlaceHandler extends DefaultBlockPlaceHandler {

    public CarvedPumpkinPlaceHandler(CloudBlockRegistry registry) {
        super(registry);
    }

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
        CreatureSpawnEvent event = new CreatureSpawnEvent(
                EntityTypes.SNOW_GOLEM, location, CreatureSpawnEvent.SpawnReason.BUILD_SNOWMAN);
        level.getServer().getEventManager().fire(event);
        if (event.isCancelled()) {
            return true;
        }

        level.setBlockState(position, BlockStates.AIR, true, true);
        level.setBlockState(middle, BlockStates.AIR, true, true);
        level.setBlockState(bottom, BlockStates.AIR, true, true);

        EntitySnowGolem golem = (EntitySnowGolem) EntityRegistry.get()
                .newEntity(EntityTypes.SNOW_GOLEM, event.getLocation());
        golem.spawnToAll();
        return true;
    }
}
