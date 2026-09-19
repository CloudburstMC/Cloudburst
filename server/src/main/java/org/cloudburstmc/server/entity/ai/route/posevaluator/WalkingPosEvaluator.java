package org.cloudburstmc.server.entity.ai.route.posevaluator;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.entity.EntityIntelligent;

public class WalkingPosEvaluator implements GroundPosEvaluator {

    @Override
    public boolean evaluate(EntityIntelligent entity, Block block) {
        var blockType = block.getState().getType();

        // Avoid lava
        if (blockType == BlockTypes.LAVA) return false;

        // Avoid cactus
        if (blockType == BlockTypes.CACTUS) return false;

        // Water is a valid standing surface
        if (blockType == BlockTypes.WATER) return true;

        return block.getState().isSolid();
    }
}
