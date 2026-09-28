package org.cloudburstmc.server.entity.ai.executor;

import org.cloudburstmc.api.entity.EntityIntelligent;
import org.cloudburstmc.api.entity.ai.behavior.BehaviorExecutor;
import org.cloudburstmc.api.entity.ai.memory.MemoryType;
import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.server.level.CloudLevel;

/**
 * Follows an entity whose runtime ID is stored in memory.
 *
 * @author daoge_cmd
 */
public class FollowEntityExecutor implements BehaviorExecutor {

    protected final MemoryType<Long> entityIdMemory;
    protected final float speed;
    protected final double maxRangeSq;
    protected final double minRangeSq;

    protected Vector3d lastTargetPos;

    public FollowEntityExecutor(MemoryType<Long> entityIdMemory, float speed, double maxRangeSq, double minRangeSq) {
        this.entityIdMemory = entityIdMemory;
        this.speed = speed;
        this.maxRangeSq = maxRangeSq;
        this.minRangeSq = minRangeSq;
    }

    @Override
    public void onStart(EntityIntelligent entity) {
        entity.setMovementSpeed(speed);
        lastTargetPos = null;
    }

    @Override
    public boolean execute(EntityIntelligent entity) {
        var targetId = entity.getMemoryStorage().get(entityIdMemory);
        if (targetId == null) return false;

        var targetEntity = ((CloudLevel) entity.getLevel()).getEntityByRuntimeId(targetId);
        if (targetEntity == null) return false;

        var targetLoc = targetEntity.getLocation();
        double distSq = entity.getPosition().distanceSquared(targetLoc.getPosition());

        if (distSq > maxRangeSq) return false;

        if (distSq > minRangeSq) {
            var targetPos = Vector3f.from(targetLoc.getX(), targetLoc.getY(), targetLoc.getZ());
            // Re-path only when target moved >1 block
            if (lastTargetPos == null || lastTargetPos.distanceSquared(targetPos.toDouble()) > 1.0) {
                EntityControlHelper.setRouteTarget(entity, targetPos);
                lastTargetPos = targetPos.toDouble();
            }
        } else {
            EntityControlHelper.removeRouteTarget(entity);
        }

        EntityControlHelper.setLookTarget(
                entity,
                Vector3f.from(targetLoc.getX(), targetLoc.getY() + targetEntity.getEyeHeight(), targetLoc.getZ()));

        return true;
    }

    @Override
    public void onStop(EntityIntelligent entity) {
        EntityControlHelper.removeRouteTarget(entity);
        EntityControlHelper.removeLookTarget(entity);
    }

    @Override
    public void onInterrupt(EntityIntelligent entity) {
        onStop(entity);
    }
}
