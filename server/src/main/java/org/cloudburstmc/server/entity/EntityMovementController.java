package org.cloudburstmc.server.entity;

import co.aikar.timings.Timing;
import co.aikar.timings.Timings;
import lombok.experimental.UtilityClass;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.util.BoundingBox;
import org.cloudburstmc.api.util.MovementType;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.level.collision.CloudVoxelShapes;

import java.util.Optional;

@UtilityClass
public class EntityMovementController {
    private static final float GROUND_PROBE_DISTANCE = 1.0E-4f;

    public static void move(CloudEntity entity, MovementType type, float dx, float dy, float dz) {
        if (dx == 0 && dz == 0 && dy == 0) {
            return;
        }

        Vector3f movement = applyStuckSpeed(entity, type, Vector3f.from(dx, dy, dz));
        dx = movement.getX();
        dy = movement.getY();
        dz = movement.getZ();

        if (entity.noPhysics) {
            moveWithoutPhysics(entity, dx, dy, dz);
            return;
        }

        try (Timing ignored = Timings.entityMoveTimer.startTiming()) {
            BoundingBox previousBox = entity.boundingBox;
            Vector3f requestedMovement = Vector3f.from(dx, dy, dz);
            Vector3f resolvedMovement = collide(entity, type, requestedMovement);
            EntityMovementResult movementResult = new EntityMovementResult(requestedMovement, resolvedMovement);

            if (resolvedMovement.getX() != 0 || resolvedMovement.getY() != 0 || resolvedMovement.getZ() != 0) {
                entity.boundingBox = entity.boundingBox.move(resolvedMovement);
            }

            syncPositionFromBox(entity);
            entity.checkChunks();
            entity.recordMovement(previousBox, entity.boundingBox);
            applyCollisionState(entity, movementResult);
            entity.updateFallState(entity.onGround);
            stopBlockedMotion(entity, movementResult);
            synchronizeForcedMovement(entity, type, previousBox);
        }
    }

    public static void setOnGroundWithMovement(CloudEntity entity, boolean onGround, boolean horizontalCollision, @Nullable Vector3f movement) {
        entity.onGround = onGround;
        entity.isCollidedHorizontally = horizontalCollision;
        checkSupportingBlock(entity, onGround, movement);
    }

    private static void synchronizeForcedMovement(CloudEntity entity, MovementType type, BoundingBox previousBox) {
        if (type != MovementType.SELF
                && type != MovementType.PLAYER
                && !previousBox.equals(entity.boundingBox)) {
            entity.sendAuthoritativeDisplacement();
        }
    }

    private static Vector3f applyStuckSpeed(CloudEntity entity, MovementType type, Vector3f movement) {
        if (entity.noPhysics || entity.stuckSpeedMultiplier.length() <= CloudVoxelShapes.EPSILON) {
            return movement;
        }

        Vector3f result = movement;
        if (type != MovementType.PISTON) {
            result = Vector3f.from(
                    movement.getX() * entity.stuckSpeedMultiplier.getX(),
                    movement.getY() * entity.stuckSpeedMultiplier.getY(),
                    movement.getZ() * entity.stuckSpeedMultiplier.getZ()
            );
        }

        entity.stuckSpeedMultiplier = Vector3f.ZERO;
        entity.motion = Vector3f.ZERO;
        return result;
    }

    private static void moveWithoutPhysics(CloudEntity entity, float dx, float dy, float dz) {
        BoundingBox previousBox = entity.boundingBox;
        entity.boundingBox = entity.boundingBox.move(dx, dy, dz);
        syncPositionFromBox(entity);
        entity.checkChunks();

        entity.isCollidedHorizontally = false;
        entity.isCollidedVertically = false;
        entity.verticalCollisionBelow = false;
        entity.isCollided = false;
        setOnGroundWithMovement(entity, false, false, Vector3f.from(dx, dy, dz));
        entity.recordMovement(previousBox, entity.boundingBox);
    }

    private static Vector3f collide(CloudEntity entity, MovementType type, Vector3f movement) {
        boolean includeEntityCollisions = entity.hasMovementEntityCollisions();
        if (type == MovementType.PISTON) {
            return entity.level.collideBoundingBox(entity, movement, entity.boundingBox, includeEntityCollisions);
        }

        BoundingBox originalBox = entity.boundingBox;
        Vector3f clipped = entity.level.collideBoundingBox(entity, movement, originalBox, includeEntityCollisions);
        boolean xCollision = movement.getX() != clipped.getX();
        boolean yCollision = movement.getY() != clipped.getY();
        boolean zCollision = movement.getZ() != clipped.getZ();
        boolean onGroundAfterCollision = yCollision && movement.getY() < 0;
        boolean supported = entity.onGround || hasGroundSupport(entity, originalBox, includeEntityCollisions);

        if (entity.getStepHeight() <= 0
                || (!supported && !onGroundAfterCollision)
                || (!xCollision && !zCollision)) {
            return clipped;
        }

        Vector3f stepped = entity.level.collideWithStep(entity, movement, clipped, originalBox, entity.getStepHeight(), includeEntityCollisions);
        if (stepped.equals(clipped)) {
            return clipped;
        }

        return stepped;
    }

    private static boolean hasGroundSupport(CloudEntity entity, BoundingBox box, boolean includeEntityCollisions) {
        Vector3f resolved = entity.level.collideBoundingBox(entity, Vector3f.from(0, -GROUND_PROBE_DISTANCE, 0), box, includeEntityCollisions);
        return resolved.getY() > -GROUND_PROBE_DISTANCE;
    }

    private static void applyCollisionState(CloudEntity entity, EntityMovementResult movementResult) {
        boolean supported = movementResult.requestedMovement().getY() <= 0 && hasGroundSupport(entity, entity.boundingBox, entity.hasMovementEntityCollisions());
        entity.isCollidedVertically = movementResult.collidedVertically();
        entity.isCollidedHorizontally = movementResult.collidedHorizontally();
        entity.isCollided = entity.isCollidedHorizontally || entity.isCollidedVertically;
        entity.verticalCollisionBelow = movementResult.collidedBelow();
        setOnGroundWithMovement(entity, entity.verticalCollisionBelow || supported, entity.isCollidedHorizontally, movementResult.resolvedMovement());
    }

    private static void stopBlockedMotion(CloudEntity entity, EntityMovementResult movementResult) {
        if (movementResult.requestedMovement().getX() != movementResult.resolvedMovement().getX()) {
            entity.motion = Vector3f.from(0, entity.motion.getY(), entity.motion.getZ());
        }

        if (movementResult.requestedMovement().getY() != movementResult.resolvedMovement().getY()) {
            entity.motion = Vector3f.from(entity.motion.getX(), 0, entity.motion.getZ());
        }

        if (movementResult.requestedMovement().getZ() != movementResult.resolvedMovement().getZ()) {
            entity.motion = Vector3f.from(entity.motion.getX(), entity.motion.getY(), 0);
        }
    }

    private static void checkSupportingBlock(CloudEntity entity, boolean onGround, @Nullable Vector3f movement) {
        if (!onGround) {
            entity.onGroundNoBlocks = false;
            entity.supportingBlockPosition = Optional.empty();
            return;
        }

        BoundingBox box = entity.getBoundingBox();
        BoundingBox testArea = new BoundingBox(
                box.getMinX(), box.getMinY() - CloudVoxelShapes.EPSILON, box.getMinZ(),
                box.getMaxX(), box.getMinY(), box.getMaxZ()
        );
        Optional<Vector3i> supportingBlock = entity.level.findSupportingBlock(entity, testArea);
        if (supportingBlock.isPresent() || entity.onGroundNoBlocks) {
            entity.supportingBlockPosition = supportingBlock;
        } else if (movement != null) {
            BoundingBox fallbackArea = testArea.move(-movement.getX(), 0, -movement.getZ());
            supportingBlock = entity.level.findSupportingBlock(entity, fallbackArea);
            entity.supportingBlockPosition = supportingBlock;
        }

        entity.onGroundNoBlocks = supportingBlock.isEmpty();
    }

    private static void syncPositionFromBox(CloudEntity entity) {
        entity.position = Vector3f.from(
                (entity.boundingBox.getMinX() + entity.boundingBox.getMaxX()) / 2,
                entity.boundingBox.getMinY(),
                (entity.boundingBox.getMinZ() + entity.boundingBox.getMaxZ()) / 2
        );
    }

}
