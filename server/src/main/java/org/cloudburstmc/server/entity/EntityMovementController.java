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

    public static boolean fastMove(CloudEntity entity, float dx, float dy, float dz) {
        if (dx == 0 && dy == 0 && dz == 0) {
            return true;
        }

        try (Timing ignored = Timings.entityMoveTimer.startTiming()) {
            BoundingBox previousBox = entity.boundingBox;
            BoundingBox newBox = entity.boundingBox.move(dx, dy, dz);
            boolean accepted = !entity.level.hasBlockCollision(entity, newBox);

            if (accepted) {
                entity.boundingBox = newBox;
            }

            syncPositionFromBox(entity, true);
            entity.checkChunks();
            entity.recordMovement(previousBox, entity.boundingBox);

            if (!entity.onGround || dy != 0) {
                BoundingBox groundCheck = entity.boundingBox.setMinY(entity.boundingBox.getMinY() - 0.75f);
                entity.onGround = entity.level.hasBlockCollision(entity, groundCheck);
            }

            entity.isCollided = entity.onGround;
            entity.updateFallState(entity.onGround);
            return accepted;
        }
    }

    public static boolean move(CloudEntity entity, MovementType type, float dx, float dy, float dz) {
        if (dx == 0 && dz == 0 && dy == 0) {
            return true;
        }

        Vector3f movement = applyStuckSpeed(entity, type, Vector3f.from(dx, dy, dz));
        dx = movement.getX();
        dy = movement.getY();
        dz = movement.getZ();

        if (entity.noPhysics) {
            moveWithoutPhysics(entity, dx, dy, dz);
            return true;
        }

        try (Timing ignored = Timings.entityMoveTimer.startTiming()) {
            entity.ySize *= 0.4;
            BoundingBox previousBox = entity.boundingBox;
            Vector3f requestedMovement = Vector3f.from(dx, dy, dz);
            Vector3f resolvedMovement = collide(entity, type, requestedMovement);
            EntityMovementResult movementResult = new EntityMovementResult(requestedMovement, resolvedMovement);

            if (resolvedMovement.getX() != 0 || resolvedMovement.getY() != 0 || resolvedMovement.getZ() != 0) {
                entity.boundingBox = entity.boundingBox.move(resolvedMovement);
            }

            syncPositionFromBox(entity, true);
            entity.checkChunks();
            entity.recordMovement(previousBox, entity.boundingBox);
            applyCollisionState(entity, movementResult);
            entity.updateFallState(entity.onGround);
            stopBlockedMotion(entity, movementResult);
            synchronizeForcedMovement(entity, type, previousBox);
        }

        return true;
    }

    private static void synchronizeForcedMovement(CloudEntity entity, MovementType type, BoundingBox previousBox) {
        if (type != MovementType.SELF
                && type != MovementType.PLAYER
                && !previousBox.equals(entity.boundingBox)) {
            entity.sendAuthoritativeDisplacement();
        }
    }

    public static void setOnGroundWithMovement(CloudEntity entity, boolean onGround, boolean horizontalCollision, @Nullable Vector3f movement) {
        entity.onGround = onGround;
        entity.isCollidedHorizontally = horizontalCollision;
        checkSupportingBlock(entity, onGround, movement);
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
        syncPositionFromBox(entity, false);
        entity.checkChunks();

        entity.isCollidedHorizontally = false;
        entity.isCollidedVertically = false;
        entity.verticalCollisionBelow = false;
        entity.isCollided = false;
        setOnGroundWithMovement(entity, false, false, Vector3f.from(dx, dy, dz));
        entity.recordMovement(previousBox, entity.boundingBox);
    }

    private static Vector3f collide(CloudEntity entity, MovementType type, Vector3f movement) {
        if (type == MovementType.PISTON) {
            return entity.level.collideBoundingBox(entity, movement, entity.boundingBox);
        }

        BoundingBox originalBox = entity.boundingBox;
        Vector3f clipped = entity.level.collideBoundingBox(entity, movement, originalBox);
        boolean xCollision = movement.getX() != clipped.getX();
        boolean yCollision = movement.getY() != clipped.getY();
        boolean zCollision = movement.getZ() != clipped.getZ();
        boolean onGroundAfterCollision = yCollision && movement.getY() < 0;

        if (entity.getStepHeight() <= 0
                || entity.ySize >= 0.05
                || (!entity.onGround && !onGroundAfterCollision)
                || (!xCollision && !zCollision)) {
            return clipped;
        }

        BoundingBox groundedBox = onGroundAfterCollision
                ? originalBox.move(0, clipped.getY(), 0)
                : originalBox;
        Vector3f stepAttempt = Vector3f.from(movement.getX(), entity.getStepHeight(), movement.getZ());
        Vector3f stepUp = entity.level.collideBoundingBox(entity, stepAttempt, groundedBox);
        BoundingBox stepBox = groundedBox.move(stepUp);
        Vector3f stepDown = entity.level.collideBoundingBox(entity, Vector3f.from(0, clipped.getY() - stepUp.getY(), 0), stepBox);
        Vector3f stepped = stepUp.add(0, stepDown.getY(), 0);

        if (stepped.getX() * stepped.getX() + stepped.getZ() * stepped.getZ() <= clipped.getX() * clipped.getX() + clipped.getZ() * clipped.getZ()) {
            return clipped;
        }

        entity.ySize += 0.5;
        return stepped;
    }

    private static void applyCollisionState(CloudEntity entity, EntityMovementResult movementResult) {
        entity.isCollidedVertically = movementResult.collidedVertically();
        entity.isCollidedHorizontally = movementResult.collidedHorizontally();
        entity.isCollided = entity.isCollidedHorizontally || entity.isCollidedVertically;
        entity.verticalCollisionBelow = movementResult.collidedBelow();
        setOnGroundWithMovement(entity, entity.verticalCollisionBelow, entity.isCollidedHorizontally, movementResult.resolvedMovement());
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

    private static void syncPositionFromBox(CloudEntity entity, boolean subtractYSize) {
        entity.position = Vector3f.from(
                (entity.boundingBox.getMinX() + entity.boundingBox.getMaxX()) / 2,
                entity.boundingBox.getMinY() - (subtractYSize ? entity.ySize : 0),
                (entity.boundingBox.getMinZ() + entity.boundingBox.getMaxZ()) / 2
        );
    }

}
