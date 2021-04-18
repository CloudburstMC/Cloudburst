package org.cloudburstmc.server.item.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.misc.EnderCrystal;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.server.level.chunk.CloudChunk;
import org.cloudburstmc.server.registry.EntityRegistry;

import java.util.concurrent.ThreadLocalRandom;

public class ItemEndCrystalBehavior extends CloudItemBehavior {

    @Override
    public boolean canBeActivated() {
        return true;
    }

    @Override
    public ItemStack onActivate(ItemStack itemStack, Player player, Block block, Block target, Direction face, Vector3f clickPos, Level level) {
        var targetType = target.getState().getType();
        if (!(targetType == BlockTypes.BEDROCK) && !(targetType == BlockTypes.OBSIDIAN)) return null;
        CloudChunk chunk = (CloudChunk) level.getLoadedChunk(block.getPosition());

        if (chunk == null) {
            return null;
        }

        Vector3f position = block.getPosition().toFloat().add(0.5, 0, 0.5);

        EnderCrystal enderCrystal = EntityRegistry.get().newEntity(EntityTypes.ENDER_CRYSTAL, Location.from(position, level));
        enderCrystal.setRotation(ThreadLocalRandom.current().nextFloat() * 360, 0);
        if (itemStack.hasName()) {
            enderCrystal.setNameTag(itemStack.getName());
        }

        enderCrystal.spawnToAll();

        if (player.isSurvival()) {
            return itemStack.decrementAmount();
        }

        return null;
    }
}
