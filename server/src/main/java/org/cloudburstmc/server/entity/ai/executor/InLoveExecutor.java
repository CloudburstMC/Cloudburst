package org.cloudburstmc.server.entity.ai.executor;

import org.cloudburstmc.api.entity.EntityIntelligent;
import org.cloudburstmc.api.entity.ai.behavior.BehaviorExecutor;
import org.cloudburstmc.api.entity.ai.memory.MemoryTypes;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityEventType;
import org.cloudburstmc.server.entity.CloudEntity;

/**
 * Sets the entity in love mode for a specified duration and sends
 * love particles to viewers periodically.
 *
 * @author daoge_cmd
 */
public class InLoveExecutor implements BehaviorExecutor {

    protected final int duration;
    protected int tickCounter;

    public InLoveExecutor(int duration) {
        this.duration = duration;
    }

    @Override
    public void onStart(EntityIntelligent entity) {
        tickCounter = 0;
        entity.getMemoryStorage().put(MemoryTypes.IS_IN_LOVE, true);
        entity.getMemoryStorage().put(MemoryTypes.LAST_IN_LOVE_TIME, entity.getTick());
    }

    @Override
    public boolean execute(EntityIntelligent entity) {
        tickCounter++;
        if (tickCounter > duration || !entity.getMemoryStorage().get(MemoryTypes.IS_IN_LOVE)) {
            return false;
        }

        // Send love particles every 10 ticks
        if (tickCounter % 10 == 0) {
            ((CloudEntity) entity).broadcastEntityEvent(EntityEventType.LOVE_PARTICLES);
        }

        return true;
    }

    @Override
    public void onStop(EntityIntelligent entity) {
        entity.getMemoryStorage().put(MemoryTypes.IS_IN_LOVE, false);
    }

    @Override
    public void onInterrupt(EntityIntelligent entity) {
        entity.getMemoryStorage().put(MemoryTypes.IS_IN_LOVE, false);
    }
}
