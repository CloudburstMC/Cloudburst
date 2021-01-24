package org.cloudburstmc.server.item.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.behavior.BlockBehaviorBedrock;
import org.cloudburstmc.server.block.behavior.BlockBehaviorObsidian;
import org.cloudburstmc.server.entity.EntityTypes;
import org.cloudburstmc.server.entity.misc.EnderCrystal;
import org.cloudburstmc.server.world.World;
import org.cloudburstmc.server.world.Location;
import org.cloudburstmc.server.world.chunk.Chunk;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.registry.EntityRegistry;
import org.cloudburstmc.server.utils.Identifier;

import java.util.concurrent.ThreadLocalRandom;

public class ItemEndCrystal extends Item {

    public ItemEndCrystal(Identifier id) {
        super(id);
    }

    @Override
    public boolean canBeActivated() {
        return true;
    }

    @Override
    public boolean onActivate(World world, Player player, Block block, Block target, Direction face, Vector3f clickPos) {
        if (!(target instanceof BlockBehaviorBedrock) && !(target instanceof BlockBehaviorObsidian)) return false;
        Chunk chunk = world.getLoadedChunk(block.getPosition());

        if (chunk == null) {
            return false;
        }

        Vector3f position = block.getPosition().toFloat().add(0.5, 0, 0.5);

        EnderCrystal enderCrystal = EntityRegistry.get().newEntity(EntityTypes.ENDER_CRYSTAL, Location.from(position, world));
        enderCrystal.setRotation(ThreadLocalRandom.current().nextFloat() * 360, 0);
        if (this.hasCustomName()) {
            enderCrystal.setNameTag(this.getCustomName());
        }

        if (player.isSurvival()) {
            Item item = player.getInventory().getItemInHand();
            item.setCount(item.getCount() - 1);
            player.getInventory().setItemInHand(item);
        }
        enderCrystal.spawnToAll();
        return true;
    }
}
