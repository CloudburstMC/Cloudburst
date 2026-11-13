package org.cloudburstmc.server.block.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.block.component.LiquidChangeBlockHandler;
import org.cloudburstmc.api.block.component.PlacementStateHandler;
import org.cloudburstmc.api.block.component.SurviveBlockHandler;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.CollisionContext;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.data.AttachmentType;

@UtilityClass
public class GrindstoneBlockHandlers {

    public static final PlacementStateHandler RESOLVE_PLACEMENT_STATE = (state, block, player, face, click) -> {
        Direction facing = face.getAxis().isHorizontal() ? face : player == null ? state.ensureTrait(BlockTraits.DIRECTION) : player.getHorizontalDirection().getOpposite();
        AttachmentType attachment = switch (face) {
            case UP -> AttachmentType.STANDING;
            case DOWN -> AttachmentType.HANGING;
            default -> AttachmentType.SIDE;
        };
        return state.withTrait(BlockTraits.ATTACHMENT, attachment).withTrait(BlockTraits.DIRECTION, facing);
    };

    public static final SurviveBlockHandler CAN_SURVIVE = block -> {
        Direction support = supportDirection(block.getState());
        return !hasWater(block) && !block.getSide(support).getCollisionShape(CollisionContext.empty()).isEmpty();
    };

    public static final LiquidChangeBlockHandler ON_LIQUID_CHANGED = (block, previous) -> {
        if (hasWater(block)) {
            block.getLevel().breakBlock(block.getPosition(), ItemStack.EMPTY, null, true);
        }
    };

    private static boolean hasWater(Block block) {
        LiquidState liquid = block.getLiquid();
        return !liquid.isEmpty() && liquid.getType().isSameFamily(LiquidTypes.WATER);
    }

    private static Direction supportDirection(BlockState state) {
        return switch (state.ensureTrait(BlockTraits.ATTACHMENT)) {
            case STANDING -> Direction.DOWN;
            case HANGING -> Direction.UP;
            case SIDE, MULTIPLE -> state.ensureTrait(BlockTraits.DIRECTION).getOpposite();
        };
    }
}
