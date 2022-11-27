package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.api.block.BlockBehaviors;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.block.behavior.*;
import org.cloudburstmc.api.item.ItemKeys;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.AxisAlignedBB;
import org.cloudburstmc.api.util.Randoms;
import org.cloudburstmc.api.util.SimpleAxisAlignedBB;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;

import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;

public class DefaultBlockBehaviours {

    public static final EntityBlockBehavior ON_PROJECTILE_HIT = (behavior, block, entity) -> {
    };

    public static final ComplexBlockBehavior ON_LIGHTNING_HIT = (behavior, block) -> {
    };

    public static final ComplexBlockBehavior ON_REDSTONE_UPDATE = (behavior, block) -> {
    };

    public static final EntityBlockBehavior ON_STAND_ON = (behavior, block, entity) -> {
    };

    public static final EntityBlockBehavior ON_STEP_OFF = (behavior, block, entity) -> {
    };

    public static final EntityBlockBehavior ON_STEP_ON = (behavior, block, entity) -> {
    };

    public static final PlayerBlockBehavior ON_DESTROY = (behavior, block, player) -> {
        // TODO: Fire block destroy level event.
        block.set(BlockStates.AIR);
    };

    public static final PlayerBlockBehavior POST_DESTROY = (behavior, block, player) -> {
        behavior.get(BlockBehaviors.GET_RESOURCE).execute(block, ThreadLocalRandom.current(), 0);
    };

    public static final SpawnResourcesBlockBehavior SPAWN_RESOURCES = (behavior, block, random, tool, bonusLootLevel) -> {
        int resourceCount = behavior.get(BlockBehaviors.GET_RESOURCE_COUNT).execute(block, random, bonusLootLevel);
        if (resourceCount < 1) {
            return;
        }

        for (int i = 0; i < resourceCount; i++) {
            if (!Randoms.chanceFloatGreaterThan(random, 0)) {
                ItemStack itemStack = behavior.get(BlockBehaviors.GET_RESOURCE).execute(block, random, bonusLootLevel);
                if (itemStack != ItemStack.AIR) {
                    behavior.get(BlockBehaviors.DROP_RESOURCE).execute(block, itemStack);
                }
            }
        }
    };

    public static final DropResourceBlockBehavior DROP_RESOURCE = (behavior, block, itemStack) -> {
        // TODO: Check if game rule DO_TILE_DROPS is disabled?
        RandomGenerator random = ThreadLocalRandom.current(); // TODO: Use Level RNG

        Vector3f dropPos = block.getPosition().toFloat().add(
                (random.nextFloat() * 0.7f) + 0.15f,
                (random.nextFloat() * 0.7f) + 0.15f,
                (random.nextFloat() * 0.7f) + 0.15f
        );

        return block.getLevel().dropItem(dropPos, itemStack, null, 10);
    };

    public static final PlayerBlockBehavior ON_REMOVE = (behavior, block, player) -> {
        block.set(BlockStates.AIR);
    };

    public static final ResourceCountBlockBehavior GET_RESOURCE_COUNT = (behavior, block, random, bonusLevel) -> 1;

    public static final ResourceBlockBehavior GET_RESOURCE = ((behavior, block, random, bonusLevel) -> {
        BlockState state = block.getState();
        return ItemStack.builder()
                .itemType(state.getType())
                .data(ItemKeys.BLOCK_STATE, state)
                .amount(1)
                .build();
    });
    public static final BooleanBlockBehavior CAN_BE_SILK_TOUCHED = (behavior, block) -> true;
    public static final BooleanBlockStateBehavior CAN_PASS_THROUGH = (behavior, block) -> false;
    public static final BooleanBlockBehavior CAN_BE_USED_IN_COMMANDS = (behavior, block) -> true;
    public static final BooleanBlockBehavior CAN_CONTAIN_LIQUID = (behavior, block) -> false;
    public static final BooleanBlockBehavior CAN_SPAWN_ON = (behavior, block) -> true;

    private static final AxisAlignedBB BOUNDING_BOX = new SimpleAxisAlignedBB(Vector3i.ZERO, Vector3i.ONE);

    public static final AABBBlockBehavior GET_BOUNDING_BOX = (behavior, state) -> BOUNDING_BOX;

    public static final BooleanBlockBehavior MAY_PLACE = (behavior, block) -> {
        int maxHeight = block.getLevel().getMaxHeight();
        int minHeight = block.getLevel().getMinHeight();

        return block.getPosition().getY() < maxHeight && block.getPosition().getY() >= minHeight &&
                behavior.get(BlockBehaviors.IS_REPLACEABLE) &&
                behavior.get(BlockBehaviors.MAY_PLACE_ON).execute(block);
    };
}
