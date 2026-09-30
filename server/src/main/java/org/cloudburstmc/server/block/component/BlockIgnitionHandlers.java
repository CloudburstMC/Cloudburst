package org.cloudburstmc.server.block.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.component.IgnitionStateHandler;
import org.cloudburstmc.api.block.trait.BooleanBlockTrait;

@UtilityClass
public class BlockIgnitionHandlers {

    public static IgnitionStateHandler litState(BooleanBlockTrait trait, boolean litValue) {
        return block -> {
            BlockState state = block.getState();
            return block.getLiquid().isEmpty() && state.ensureTrait(trait) != litValue
                    ? state.withTrait(trait, litValue) : null;
        };
    }
}
