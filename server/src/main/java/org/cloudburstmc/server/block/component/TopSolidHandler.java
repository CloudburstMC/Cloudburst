package org.cloudburstmc.server.block.component;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.api.block.BlockComponents;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.component.BooleanBlockStateHandler;
import org.cloudburstmc.server.registry.CloudBlockRegistry;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class TopSolidHandler implements BooleanBlockStateHandler {

    public static final TopSolidHandler INSTANCE = new TopSolidHandler();

    @Override
    public boolean execute(BlockState state) {
        float[] boxes = state.getCollisionBoxes();
        if (boxes == null) {
            return CloudBlockRegistry.REGISTRY.getComponent(state.getType(), BlockComponents.SOLID).get();
        }

        if (boxes.length == 0) {
            return false;
        }

        for (int i = 4; i < boxes.length; i += 6) {
            if (boxes[i] == 1.0f) {
                return true;
            }
        }

        return false;
    }
}

