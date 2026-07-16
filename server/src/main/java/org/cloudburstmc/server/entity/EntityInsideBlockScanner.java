package org.cloudburstmc.server.entity;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockComponents;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.util.BoundingBox;
import org.cloudburstmc.api.util.CollisionContext;
import org.cloudburstmc.api.util.VoxelShape;
import org.cloudburstmc.api.util.component.ComponentMap;
import org.cloudburstmc.math.GenericMath;
import org.cloudburstmc.math.vector.Vector3i;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.cloudburstmc.api.block.BlockTypes.PORTAL;

@UtilityClass
public class EntityInsideBlockScanner {

    public static void scan(CloudEntity entity) {
        List<EntityMovementSegment> movements = entity.drainMovementSegments();

        Optional<Vector3i> portal = Optional.empty();
        Set<Vector3i> insideBlocks = new HashSet<>();
        Set<Vector3i> collisionBlocks = new HashSet<>();
        for (EntityMovementSegment movement : movements) {
            Optional<Vector3i> movementPortal = scanMovement(entity, movement, insideBlocks, collisionBlocks);
            if (portal.isEmpty() && movementPortal.isPresent()) {
                portal = movementPortal;
            }
        }

        if (portal.isPresent()) {
            entity.portalEntryBlock = portal.get();
            entity.onInsidePortal();
            return;
        }

        if (entity.portalCooldown <= 0) {
            entity.inPortalTicks = Math.max(0, entity.inPortalTicks - 4);
            if (entity.inPortalTicks == 0) {
                entity.portalEntryBlock = null;
            }
        }
    }

    private static Optional<Vector3i> scanMovement(
            CloudEntity entity,
            EntityMovementSegment movement,
            Set<Vector3i> insideBlocks,
            Set<Vector3i> collisionBlocks
    ) {
        Optional<Vector3i> portal = Optional.empty();
        int steps = movement.steps();
        for (int step = 0; step <= steps; step++) {
            float progress = (float) step / steps;
            BoundingBox box = movement.interpolate(progress);
            Optional<Vector3i> stepPortal = scanLoadedBlocks(entity, box, insideBlocks);
            if (portal.isEmpty() && stepPortal.isPresent()) {
                portal = stepPortal;
            }

            entity.level.forEachBlockCollision(entity, box, block -> {
                if (collisionBlocks.add(block.getPosition())) {
                    block.getComponent(BlockComponents.ON_ENTITY_COLLIDE).execute(block, entity);
                }
            });
        }

        return portal;
    }

    private static Optional<Vector3i> scanLoadedBlocks(CloudEntity entity, BoundingBox box, Set<Vector3i> insideBlocks) {
        int minX = GenericMath.floor(box.getMinX());
        int minY = GenericMath.floor(box.getMinY());
        int minZ = GenericMath.floor(box.getMinZ());
        int maxX = GenericMath.ceil(box.getMaxX());
        int maxY = GenericMath.ceil(box.getMaxY());
        int maxZ = GenericMath.ceil(box.getMaxZ());
        Optional<Vector3i> portal = Optional.empty();

        for (int z = minZ; z <= maxZ; ++z) {
            for (int x = minX; x <= maxX; ++x) {
                for (int y = minY; y <= maxY; ++y) {
                    if (!intersectsUnitBlock(box, x, y, z)) {
                        continue;
                    }

                    Block block = entity.level.getLoadedBlock(x, y, z);
                    if (block == null || block.getState() == BlockStates.AIR) {
                        continue;
                    }

                    BlockState state = block.getState();
                    if (state.getType() == PORTAL) {
                        portal = Optional.of(block.getPosition());
                        continue;
                    }

                    ComponentMap behaviors = block.getComponents();
                    VoxelShape insideShape = behaviors.get(BlockComponents.GET_ENTITY_INSIDE_COLLISION_SHAPE)
                            .execute(state, CollisionContext.of(entity));
                    if (!insideShape.isEmpty()
                            && insideShape.overlaps(box, x, y, z)
                            && insideBlocks.add(block.getPosition())) {
                        behaviors.get(BlockComponents.ON_ENTITY_INSIDE).execute(block, entity, true);
                    }
                }
            }
        }

        return portal;
    }

    private static boolean intersectsUnitBlock(BoundingBox boundingBox, int x, int y, int z) {
        return boundingBox.getMaxX() > x
                && boundingBox.getMinX() < x + 1
                && boundingBox.getMaxY() > y
                && boundingBox.getMinY() < y + 1
                && boundingBox.getMaxZ() > z
                && boundingBox.getMinZ() < z + 1;
    }
}
