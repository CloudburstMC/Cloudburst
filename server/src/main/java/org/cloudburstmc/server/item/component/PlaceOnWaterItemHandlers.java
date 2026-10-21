package org.cloudburstmc.server.item.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.component.UseHandler;
import org.cloudburstmc.api.item.component.UseOnHandler;
import org.cloudburstmc.api.level.BlockShapeMode;
import org.cloudburstmc.api.level.FluidCollisionMode;
import org.cloudburstmc.api.level.RayTraceContext;
import org.cloudburstmc.api.util.BlockHitResult;
import org.cloudburstmc.api.util.CollisionContext;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.player.CloudPlayer;

@UtilityClass
public class PlaceOnWaterItemHandlers {

    public static final UseHandler USE = PlaceOnWaterItemHandlers::place;
    public static final UseOnHandler USE_ON = (item, entity, position, face, click) -> place(item, entity);

    private static ItemStack place(ItemStack item, Entity entity) {
        if (!(entity instanceof CloudPlayer player) || player.isSpectator()) {
            return item;
        }

        CloudLevel level = player.getLevel();
        Vector3f eye = player.getPosition().add(0, player.getEyeHeight(), 0);
        Vector3f end = eye.add(player.getDirectionVector().mul(5));
        if (!(level.rayTraceBlocks(new RayTraceContext(eye, end, BlockShapeMode.OUTLINE,
                FluidCollisionMode.SOURCE_ONLY, CollisionContext.of(player))) instanceof BlockHitResult hit)
                || !player.canInteractWithBlock(hit.block().getPosition())) {
            return item;
        }

        Block target = level.getLoadedBlock(hit.block().getPosition().add(0, 1, 0));
        if (target == null || !player.canInteractWithBlock(target.getPosition()) || !target.getLiquid().isEmpty()) {
            return item;
        }

        ItemStack placed = level.tryPlaceBlock(target, target, Direction.UP, Vector3f.ZERO, item, player, true);
        return placed == null ? item : placed;
    }
}
