package org.cloudburstmc.server.entity.ai.executor;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityIntelligent;
import org.cloudburstmc.api.entity.ai.behavior.BehaviorExecutor;
import org.cloudburstmc.api.entity.ai.memory.MemoryType;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.server.level.CloudLevel;

/**
 * Looks at an entity whose runtime ID is stored in memory for a duration.
 *
 * @author daoge_cmd
 */
public class LookAtEntityExecutor implements BehaviorExecutor {

    protected final MemoryType<Long> entityIdMemory;
    protected final int duration;

    protected int tickCounter;

    public LookAtEntityExecutor(MemoryType<Long> entityIdMemory, int duration) {
        this.entityIdMemory = entityIdMemory;
        this.duration = duration;
    }

    @Override
    public void onStart(EntityIntelligent entity) {
        tickCounter = 0;
    }

    @Override
    public boolean execute(EntityIntelligent entity) {
        tickCounter++;
        if (tickCounter > duration) return false;

        Long targetId = entity.getMemoryStorage().get(entityIdMemory);
        if (targetId == null) return false;

        Entity targetEntity = ((CloudLevel) entity.getLevel()).getEntityByRuntimeId(targetId);
        if (targetEntity == null || targetEntity.isClosed() || !targetEntity.isAlive()) return false;

        var targetPos = targetEntity.getPosition();
        EntityControlHelper.setLookTarget(
                entity,
                Vector3f.from(targetPos.getX(), targetPos.getY() + targetEntity.getEyeHeight(), targetPos.getZ()));

        return true;
    }

    @Override
    public void onStop(EntityIntelligent entity) {
        EntityControlHelper.removeLookTarget(entity);
    }

    @Override
    public void onInterrupt(EntityIntelligent entity) {
        onStop(entity);
    }
}
