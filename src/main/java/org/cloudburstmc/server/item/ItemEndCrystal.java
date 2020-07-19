package org.cloudburstmc.server.item;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.block.behavior.BlockBehaviorBedrock;
import org.cloudburstmc.server.block.behavior.BlockBehaviorObsidian;
import org.cloudburstmc.server.entity.EntityTypes;
import org.cloudburstmc.server.entity.misc.EnderCrystal;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.level.Location;
import org.cloudburstmc.server.level.chunk.Chunk;
import org.cloudburstmc.server.math.BlockFace;
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
    public boolean onActivate(Level level, Player player, Block block, Block target, BlockFace face, Vector3f clickPos) {
        if (!(target instanceof BlockBehaviorBedrock) && !(target instanceof BlockBehaviorObsidian)) return false;
        Chunk chunk = level.getLoadedChunk(blockState.getPosition());

        if (chunk == null) {
            return false;
        }

        Vector3f position = blockState.getPosition().toFloat().add(0.5, 0, 0.5);

        EnderCrystal enderCrystal = EntityRegistry.get().newEntity(EntityTypes.ENDER_CRYSTAL, Location.from(position, level));
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
