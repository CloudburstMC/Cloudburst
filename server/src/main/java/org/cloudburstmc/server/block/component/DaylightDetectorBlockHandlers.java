package org.cloudburstmc.server.block.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.block.component.*;
import org.cloudburstmc.api.blockentity.BlockEntityTypes;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.registry.CloudBlockEntityRegistry;

import java.util.List;

@UtilityClass
public class DaylightDetectorBlockHandlers {

    public static final ComplexBlockHandler ON_ADDED = block -> {
        if (block.getLevel().getBlockEntity(block.getPosition()) == null) {
            CloudBlockEntityRegistry.get().newEntity(BlockEntityTypes.DAYLIGHT_DETECTOR, block);
        }
    };

    public static final UseBlockHandler USE = (block, player, _, _) ->
            player != null && (player.isSurvival() || player.isCreative()) && setInverted(block, block.getState().getType() == BlockTypes.DAYLIGHT_DETECTOR);

    public static final PickBlockHandler GET_PICK_BLOCK = _ -> ItemStack.from(BlockTypes.DAYLIGHT_DETECTOR.getDefaultState());

    public static final BlockLootHandler GET_LOOT = (block, _) -> List.of(GET_PICK_BLOCK.execute(block));

    public static final RedstoneSignalHandler GET_REDSTONE_SIGNAL = (_, _) -> {
        // TODO: Sample sky light on mode changes and every 20 ticks in dimensions with skylight.
        // TODO: Fire BlockRedstoneEvent before applying a changed signal and notifying neighbors.
        return 0;
    };

    public static boolean setInverted(Block block, boolean inverted) {
        BlockState state = block.getState();
        if (state.getType() != BlockTypes.DAYLIGHT_DETECTOR && state.getType() != BlockTypes.DAYLIGHT_DETECTOR_INVERTED) {
            return false;
        }

        if (inverted != (state.getType() == BlockTypes.DAYLIGHT_DETECTOR_INVERTED)) {
            BlockState replacement = inverted ? BlockTypes.DAYLIGHT_DETECTOR_INVERTED.getDefaultState() : BlockTypes.DAYLIGHT_DETECTOR.getDefaultState();
            block.set(replacement.copyTraits(state), true, false);
        }

        return true;
    }
}
