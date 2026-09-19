package org.cloudburstmc.server.entity.ai.evaluator;

import org.cloudburstmc.api.entity.EntityIntelligent;
import org.cloudburstmc.api.entity.ai.behavior.BehaviorEvaluator;
import org.cloudburstmc.api.entity.ai.memory.MemoryType;

/**
 * Checks if a specific memory type has a value stored (is not empty).
 *
 * @author daoge_cmd
 */
public class MemoryCheckNotEmptyEvaluator implements BehaviorEvaluator {

    protected final MemoryType<?> type;

    public MemoryCheckNotEmptyEvaluator(MemoryType<?> type) {
        this.type = type;
    }

    @Override
    public boolean evaluate(EntityIntelligent entity) {
        return entity.getMemoryStorage().notEmpty(type);
    }
}
