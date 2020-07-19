package org.cloudburstmc.server.dispenser;

import org.cloudburstmc.server.block.behavior.BlockBehaviorDispenser;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.math.BlockFace;

/**
 * @author CreeperFace
 */
public class DefaultDispenseBehavior implements DispenseBehavior {

    @Override
    public void dispense(BlockBehaviorDispenser block, Item stack) {

    }

    private int getParticleMetadataForFace(BlockFace face) {
        return face.getXOffset() + 1 + (face.getZOffset() + 1) * 3;
    }
}
