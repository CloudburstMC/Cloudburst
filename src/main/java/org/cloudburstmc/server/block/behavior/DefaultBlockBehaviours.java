package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3i;
import org.cloudburstmc.api.util.SimpleAxisAlignedBB;
import org.cloudburstmc.api.util.behavior.BehaviorBuilder;

import static org.cloudburstmc.api.block.BlockBehaviors.*;

public class DefaultBlockBehaviours {

    public static final BehaviorBuilder BLOCK_BEHAVIOR_BASE = BehaviorBuilder.create()
            .overwrite(IS_SOLID, true)
            .overwrite(IS_LIQUID, false)
            .overwrite(USES_WATERLOGGING, false)
            .overwrite(CAN_BE_USED, ((behavior, block) -> false))
            .overwrite(CAN_PASS_THROUGH, (behavior, block) -> false)
            .overwrite(GET_BOUNDING_BOX, ((behavior, state) -> new SimpleAxisAlignedBB(Vector3i.ZERO, Vector3i.ONE)));



//
//
//    public static final EntityBlockBehavior ON_PROJECTILE_HIT = (behavior, block, entity) -> {};
//
//    public static final ComplexBlockBehavior ON_LIGHTNING_HIT = (behavior, block) -> {};
//
//    public static final ComplexBlockBehavior ON_REDSTONE_UPDATE = (behavior, block) -> {};
//
//    public static final EntityBlockBehavior ON_STAND_ON = (behavior, block, entity) -> {};
//
//    public static final EntityBlockBehavior ON_STEP_OFF = (behavior, block, entity) -> {};
//
//    public static final EntityBlockBehavior ON_STEP_ON = (behavior, block, entity) -> {};
//
//    public static final PlayerBlockBehavior ON_DESTROY = (behavior, block, player) -> {
//        // TODO
//    };
//
//    public static final PlayerBlockBehavior ON_REMOVE = (behavior, block, player) -> {
//        block.set(BlockStates.AIR);
//    };
//
//    public static final ResourceCountBlockBehavior GET_RESOURCE_COUNT = (behavior, block, random, bonusLevel) -> 1;
//
//    public static final ResourceBlockBehavior GET_RESOURCE = ((behavior, block, random, bonusLevel) -> {
//        BlockState state = block.getState();
//        return ItemStack.builder()
//                .itemType(state.getType())
//                .data(ItemKeys.BLOCK_STATE, state)
//                .amount(1)
//                .build();
//    });
//
//    private static final AxisAlignedBB BOUNDING_BOX = new SimpleAxisAlignedBB(Vector3i.ZERO, Vector3i.ONE);
//
//    public static final AABBBlockBehavior GET_BOUNDING_BOX = (behavior, state) -> BOUNDING_BOX;
//
//    public static final BooleanBlockBehavior MAY_PLACE = (behavior, block) -> {
//        int maxHeight = block.getLevel().getMaxHeight();
//        int minHeight = block.getLevel().getMinHeight();
//
//        return block.getPosition().getY() < maxHeight && block.getPosition().getY() >= minHeight &&
//                behavior.get(BlockBehaviors.IS_REPLACEABLE) &&
//                behavior.get(BlockBehaviors.MAY_PLACE_ON).execute(block);
//    };
}
