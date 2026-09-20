package org.cloudburstmc.server.entity.ai.evaluator;

import org.cloudburstmc.api.entity.EntityIntelligent;
import org.cloudburstmc.api.entity.ai.behavior.BehaviorEvaluator;

/**
 * AND-logic composite evaluator. Returns {@code true} only if all
 * inner evaluators return {@code true}.
 *
 * @author daoge_cmd
 */
public class AllMatchEvaluator implements BehaviorEvaluator {

    protected final BehaviorEvaluator[] evaluators;

    public AllMatchEvaluator(BehaviorEvaluator... evaluators) {
        this.evaluators = evaluators;
    }

    @Override
    public boolean evaluate(EntityIntelligent entity) {
        for (var evaluator : evaluators) {
            if (!evaluator.evaluate(entity)) {
                return false;
            }
        }
        return true;
    }
}
