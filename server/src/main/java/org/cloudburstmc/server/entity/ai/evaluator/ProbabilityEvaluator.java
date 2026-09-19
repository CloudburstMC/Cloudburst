package org.cloudburstmc.server.entity.ai.evaluator;

import org.cloudburstmc.api.entity.EntityIntelligent;
import org.cloudburstmc.api.entity.ai.behavior.BehaviorEvaluator;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Random chance evaluator. Returns {@code true} with probability
 * {@code probability / total}.
 *
 * @author daoge_cmd
 */
public class ProbabilityEvaluator implements BehaviorEvaluator {

    protected final int probability;
    protected final int total;

    public ProbabilityEvaluator(int probability, int total) {
        this.probability = probability;
        this.total = total;
    }

    @Override
    public boolean evaluate(EntityIntelligent entity) {
        return ThreadLocalRandom.current().nextInt(total) < probability;
    }
}
