package org.cloudburstmc.api.entity.ai.behavior;

import org.cloudburstmc.api.entity.EntityIntelligent;

/**
 * Evaluates whether a behavior should be activated.
 *
 * @author daoge_cmd
 */
@FunctionalInterface
public interface BehaviorEvaluator {

    /**
     * Evaluate whether this behavior should be activated for the given entity.
     *
     * @param entity the entity to evaluate for
     * @return {@code true} if this behavior should be activated
     */
    boolean evaluate(EntityIntelligent entity);
}
