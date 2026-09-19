package org.cloudburstmc.server.entity.ai.route.posevaluator;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.entity.EntityIntelligent;

/**
 * Evaluates ground blocks during pathfinding to determine their suitability for standing.
 *
 * @author daoge_cmd
 */
@FunctionalInterface
public interface GroundPosEvaluator {

    /**
     * Evaluate a standing block for ground walking suitability.
     *
     * @param entity the entity
     * @param block  the block being evaluated
     * @return {@code true} if the entity can stand on this block
     */
    boolean evaluate(EntityIntelligent entity, Block block);
}
