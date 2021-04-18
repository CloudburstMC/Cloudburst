package org.cloudburstmc.server.item.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.event.entity.CreatureSpawnEvent;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;
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
    public ItemStack onActivate(ItemStack itemStack, Player player, Block block, Block target, Direction face, Vector3f clickPos, Level level) {
        Chunk chunk = level.getLoadedChunk(block.getPosition());

        if (chunk == null) {
            return null;
        }

        Location location = Location.from(block.getPosition().toFloat().add(0.5, 0, 0.5),
                ThreadLocalRandom.current().nextFloat() * 360, 0, level);
        CreatureSpawnEvent ev = new CreatureSpawnEvent(itemStack.getMetadata(EntityType.class), // FIXME: 04/01/2020 Use string identifier in NBT
                location, CreatureSpawnEvent.SpawnReason.SPAWN_EGG);
        level.getServer().getEventManager().fire(ev);

        if (ev.isCancelled()) {
            return null;
        }

        Entity entity = EntityRegistry.get().newEntity(ev.getEntityType(), ev.getLocation());

        if (itemStack.hasName()) {
            entity.setNameTag(itemStack.getName());
        }

        entity.spawnToAll();

        if (player.isSurvival()) {
            return itemStack.decrementAmount();
        }

        return null;
    }
}
