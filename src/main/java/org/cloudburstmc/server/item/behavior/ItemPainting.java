package org.cloudburstmc.server.item.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockCategory;
import org.cloudburstmc.server.entity.EntityTypes;
import org.cloudburstmc.server.entity.impl.misc.EntityPainting;
import org.cloudburstmc.server.entity.misc.Painting;
import org.cloudburstmc.server.world.World;
import org.cloudburstmc.server.world.Location;
import org.cloudburstmc.server.world.chunk.Chunk;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.registry.EntityRegistry;
import org.cloudburstmc.server.utils.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class ItemPainting extends Item {
    private static final int[] DIRECTION = {2, 3, 4, 5};
    private static final int[] RIGHT = {4, 5, 3, 2};
    private static final double OFFSET = 0.53125;

    public ItemPainting(Identifier id) {
        super(id);
    }

    @Override
    public boolean canBeActivated() {
        return true;
    }

    @Override
    public boolean onActivate(World world, Player player, Block block, Block target, Direction face, Vector3f clickPos) {
        Chunk chunk = world.getLoadedChunk(block.getPosition());

        if (chunk == null || target.getState().inCategory(BlockCategory.TRANSPARENT)
                || face.getHorizontalIndex() == -1 || block.getState().inCategory(BlockCategory.SOLID)) {
            return false;
        }

        List<EntityPainting.Motive> validMotives = new ArrayList<>();
        for (EntityPainting.Motive motive : EntityPainting.motives) {
            boolean valid = true;
            for (int x = 0; x < motive.width && valid; x++) {
                for (int z = 0; z < motive.height && valid; z++) {
                    if (target.getSideState(Direction.fromIndex(RIGHT[face.getIndex() - 2]), x).inCategory(BlockCategory.TRANSPARENT) ||
                            target.upState(z).inCategory(BlockCategory.TRANSPARENT) ||
                            block.getSideState(Direction.fromIndex(RIGHT[face.getIndex() - 2]), x).inCategory(BlockCategory.SOLID) ||
                            block.upState(z).inCategory(BlockCategory.SOLID)) {
                        valid = false;
                    }
                }
            }

            if (valid) {
                validMotives.add(motive);
            }
        }
        int direction = DIRECTION[face.getIndex() - 2];
        EntityPainting.Motive motive = validMotives.get(ThreadLocalRandom.current().nextInt(validMotives.size()));

        Vector3f position = target.getPosition().toFloat().add(0.5, 0.5, 0.5);
        double widthOffset = offset(motive.width);

        switch (face.getHorizontalIndex()) {
            case 0:
                position = position.add(widthOffset, 0, OFFSET);
                break;
            case 1:
                position = position.add(-OFFSET, 0, widthOffset);
                break;
            case 2:
                position = position.sub(widthOffset, 0, OFFSET);
                break;
            case 3:
                position = position.add(OFFSET, 0, -widthOffset);
                break;
        }
        position = position.add(0, offset(motive.height), 0);

        Painting entity = EntityRegistry.get().newEntity(EntityTypes.PAINTING, Location.from(position, world));
        entity.setRotation(direction * 90, 0);
        entity.setMotive(motive);

        if (player.isSurvival()) {
            Item item = player.getInventory().getItemInHand();
            item.setCount(item.getCount() - 1);
            player.getInventory().setItemInHand(item);
        }

        entity.spawnToAll();
        return true;
    }

    private static double offset(int value) {
        return value > 1 ? 0.5 : 0;
    }
}
