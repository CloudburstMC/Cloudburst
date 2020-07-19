package org.cloudburstmc.server.item;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.event.entity.CreatureSpawnEvent;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.level.Location;
import org.cloudburstmc.server.level.chunk.Chunk;
import org.cloudburstmc.server.math.BlockFace;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.registry.EntityRegistry;
import org.cloudburstmc.server.utils.Identifier;

import java.util.concurrent.ThreadLocalRandom;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class ItemSpawnEgg extends Item {

    public ItemSpawnEgg(Identifier id) {
        super(id);
    }

    @Override
    public boolean canBeActivated() {
        return true;
    }

    @Override
    public boolean onActivate(Level level, Player player, Block block, Block target, BlockFace face, Vector3f clickPos) {
        Chunk chunk = level.getLoadedChunk(blockState.getPosition());

        if (chunk == null) {
            return false;
        }

        Location location = Location.from(blockState.getPosition().toFloat().add(0.5, 0, 0.5),
                ThreadLocalRandom.current().nextFloat() * 360, 0, level);
        CreatureSpawnEvent ev = new CreatureSpawnEvent(EntityRegistry.get().getEntityType(this.getMeta()), // FIXME: 04/01/2020 Use string identifier in NBT
                location, CreatureSpawnEvent.SpawnReason.SPAWN_EGG);
        level.getServer().getPluginManager().callEvent(ev);

        if (ev.isCancelled()) {
            return false;
        }

        Entity entity = EntityRegistry.get().newEntity(ev.getEntityType(), ev.getLocation());
        if (this.hasCustomName()) {
            entity.setNameTag(this.getCustomName());
        }

        if (player.isSurvival()) {
            player.getInventory().decreaseCount(player.getInventory().getHeldItemIndex());
        }
        entity.spawnToAll();
        return true;
    }
}
