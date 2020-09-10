package org.cloudburstmc.server.item.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.event.entity.CreatureSpawnEvent;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.level.Location;
import org.cloudburstmc.server.level.chunk.Chunk;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.registry.EntityRegistry;

import java.util.concurrent.ThreadLocalRandom;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class ItemSpawnEggBehavior extends CloudItemBehavior {

    @Override
    public boolean canBeActivated() {
        return true;
    }

    @Override
    public boolean onActivate(ItemStack itemStack, Player player, Block block, Block target, Direction face, Vector3f clickPos, Level level) {
        Chunk chunk = level.getLoadedChunk(block.getPosition());

        if (chunk == null) {
            return false;
        }

        Location location = Location.from(block.getPosition().toFloat().add(0.5, 0, 0.5),
                ThreadLocalRandom.current().nextFloat() * 360, 0, level);
        CreatureSpawnEvent ev = new CreatureSpawnEvent(EntityRegistry.get().getEntityType(this.getMeta()), // FIXME: 04/01/2020 Use string identifier in NBT
                location, CreatureSpawnEvent.SpawnReason.SPAWN_EGG);
        level.getServer().getEventManager().fire(ev);

        if (ev.isCancelled()) {
            return false;
        }

        Entity entity = EntityRegistry.get().newEntity(ev.getEntityType(), ev.getLocation());

        itemStack.getName().ifPresent(entity::setNameTag);

        if (player.isSurvival()) {
            player.getInventory().decrementCount(player.getInventory().getHeldItemIndex());
        }
        entity.spawnToAll();
        return true;
    }
}
