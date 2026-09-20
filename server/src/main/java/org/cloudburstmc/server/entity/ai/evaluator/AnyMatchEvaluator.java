package org.cloudburstmc.server.entity.ai.evaluator;

import org.cloudburstmc.api.entity.EntityIntelligent;
import org.cloudburstmc.api.entity.ai.behavior.BehaviorEvaluator;

/**
 * OR-logic composite evaluator. Returns {@code true} if any
 * inner evaluator returns {@code true}.
 *
 * @author daoge_cmd
 */
public class AnyMatchEvaluator implements BehaviorEvaluator {

    protected final BehaviorEvaluator[] evaluators;

    public AnyMatchEvaluator(BehaviorEvaluator... evaluators) {
        this.evaluators = evaluators;
    }

    @Override
    public boolean evaluate(EntityIntelligent entity) {
        for (var evaluator : evaluators) {
            if (evaluator.evaluate(entity)) {
                return true;
            }
        }
        return false;
    }
}
