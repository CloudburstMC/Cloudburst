package org.cloudburstmc.server.entity.ai.controller;

import org.cloudburstmc.api.entity.EntityIntelligent;
import org.cloudburstmc.api.entity.ai.controller.Controller;
import org.cloudburstmc.api.util.BoundingBox;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.server.level.CloudLevel;

public class WalkController implements Controller {

    protected static int JUMP_COOL_DOWN = 0;
    protected static float STEP_HEIGHT = 0;
    protected static float JUMP_MOTION = 0;

    protected int currentJumpCoolDown = JUMP_COOL_DOWN;

    public WalkController() {
        this(0.42f, 0.6f, 10);
    }

    public WalkController(float jumpMotion, float stepHeight, int jumpCoolDown) {
        JUMP_MOTION = jumpMotion;
        STEP_HEIGHT = stepHeight;
        JUMP_COOL_DOWN = jumpCoolDown;
    }

    @Override
    public boolean control(EntityIntelligent entity) {
        currentJumpCoolDown++;

        if (!entity.hasMoveDirection()) {
            return false;
        }

        Vector3f end = entity.getMoveDirectionEnd();
        if (end == null) {
            return false;
        }

        var loc = entity.getLocation();
        float speed = entity.getMovementSpeed();

        Vector3f motion = entity.getMotion();
        if (motion.getX() * motion.getX() + motion.getZ() * motion.getZ() > speed * speed * 0.4756f) {
            return false;
        }

        double dx = end.getX() - loc.getX();
        double dz = end.getZ() - loc.getZ();
        double horizontalDistSq = dx * dx + dz * dz;
        double horizontalDist = Math.sqrt(horizontalDistSq);
        if (horizontalDist < 0.01) {
            return false;
        }

        double effectiveSpeed = Math.min(speed, horizontalDist);
        double factor = effectiveSpeed / horizontalDist;
        float targetMx = (float) (dx * factor);
        float targetMz = (float) (dz * factor);

        float motionY = motion.getY();
        if (entity.isOnGround() && currentJumpCoolDown >= JUMP_COOL_DOWN) {
            double dy = end.getY() - loc.getY();
            BoundingBox box = entity.getBoundingBox();
            double entityWidth = box.getMaxX() - box.getMinX();

            boolean shouldJump = dy > STEP_HEIGHT && horizontalDistSq < Math.max(2.25, entityWidth);

            if (!shouldJump) {
                shouldJump = dy > 0 && collidesBlocks(entity, targetMx, targetMz);
            }

            if (shouldJump) {
                motionY = JUMP_MOTION;
                currentJumpCoolDown = 0;
            }
        }

        entity.setMotion(Vector3f.from(targetMx, motionY, targetMz));
        return true;
    }

    protected boolean collidesBlocks(EntityIntelligent entity, float dx, float dz) {
        BoundingBox b = entity.getBoundingBox();
        BoundingBox moved = new BoundingBox(
                b.getMinX() + dx,
                b.getMinY() + 0.05f,
                b.getMinZ() + dz,
                b.getMaxX() + dx,
                b.getMaxY(),
                b.getMaxZ() + dz);

        CloudLevel level = (CloudLevel) entity.getLevel();
        return level.hasLoadedBlockIntersecting(
                moved, block -> level.isFullBlock(block.getPosition(), block.getState()));
    }
}
