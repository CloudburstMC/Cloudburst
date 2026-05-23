package org.cloudburstmc.server.block.component;

import org.cloudburstmc.api.block.BlockComponents;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.block.component.*;
import org.cloudburstmc.api.item.ItemKeys;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemType;
import org.cloudburstmc.api.util.Randoms;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.server.registry.CloudBlockRegistry;
import org.cloudburstmc.server.registry.CloudItemRegistry;

import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;

public class DefaultBlockHandlers {

    public static final EntityBlockHandler ON_PROJECTILE_HIT = (block, entity) -> {
    };

    public static final ComplexBlockHandler ON_LIGHTNING_HIT = (block) -> {
    };

    public static final ComplexBlockHandler ON_REDSTONE_UPDATE = (block) -> {
    };

    public static final EntityBlockHandler ON_STAND_ON = (block, entity) -> {
    };

    public static final EntityBlockHandler ON_STEP_OFF = (block, entity) -> {
    };

    public static final EntityBlockHandler ON_STEP_ON = (block, entity) -> {
    };

    public static final PlayerBlockHandler ON_DESTROY = (block, player) -> {
        // TODO: Fire block destroy level event.
        block.set(BlockStates.AIR);
    };

    public static final PlayerBlockHandler POST_DESTROY = (block, player) -> {
        ResourceBlockHandler getResource = block.getComponents().get(BlockComponents.GET_RESOURCE);
        if (getResource != null) {
            getResource.execute(block, ThreadLocalRandom.current(), 0);
        }
    };

    public static final SpawnResourcesBlockHandler SPAWN_RESOURCES = (block, random, tool, bonusLootLevel) -> {
        ResourceCountBlockHandler getResourceCount = block.getComponents().get(BlockComponents.GET_RESOURCE_COUNT);
        int resourceCount = getResourceCount != null ? getResourceCount.execute(block, random, bonusLootLevel) : 1;
        if (resourceCount < 1) {
            return;
        }

        ResourceBlockHandler getResource = block.getComponents().get(BlockComponents.GET_RESOURCE);
        DropResourceBlockHandler dropResource = block.getComponents().get(BlockComponents.DROP_RESOURCE);

        for (int i = 0; i < resourceCount; i++) {
            if (!Randoms.chanceFloatGreaterThan(random, 0)) {
                if (getResource != null) {
                    ItemStack itemStack = getResource.execute(block, random, bonusLootLevel);
                    if (!itemStack.isEmpty() && dropResource != null) {
                        dropResource.execute(block, itemStack);
                    }
                }
            }
        }
    };

    public static final DropResourceBlockHandler DROP_RESOURCE = (block, itemStack) -> {
        // TODO: Check if game rule DO_TILE_DROPS is disabled?
        RandomGenerator random = ThreadLocalRandom.current(); // TODO: Use Level RNG

        Vector3f dropPos = block.getPosition().toFloat().add(
                (random.nextFloat() * 0.7f) + 0.15f,
                (random.nextFloat() * 0.7f) + 0.15f,
                (random.nextFloat() * 0.7f) + 0.15f
        );

        return block.getLevel().dropItem(dropPos, itemStack, null, 10);
    };

    // ON_REMOVE fires before a block is removed (e.g. by pistons or neighbour updates),
    // NOT as the cause of removal. Default is a no-op; ON_DESTROY handles the actual removal.
    public static final ComplexBlockHandler ON_REMOVE = (block) -> {
    };

    public static final ResourceCountBlockHandler GET_RESOURCE_COUNT = (block, random, bonusLevel) -> 1;

    public static final ResourceBlockHandler GET_RESOURCE = ((block, random, bonusLevel) -> {
        BlockState state = block.getState();
        ItemType itemType = CloudItemRegistry.get().getType(state.getType().getId(), 0);
        if (itemType == null) return ItemStack.EMPTY;
        return ItemStack.builder()
                .itemType(itemType)
                .data(ItemKeys.BLOCK_STATE, state)
                .amount(1)
                .build();
    });
    public static final UseCheckHandler CAN_BE_USED = (block, player) -> true;
    public static final BooleanBlockHandler CAN_BE_SILK_TOUCHED = (block) -> true;
    public static final BooleanBlockStateHandler CAN_PASS_THROUGH = (block) -> !CloudBlockRegistry.REGISTRY.getComponent(block.getType(), BlockComponents.SOLID).get();
    public static final BooleanBlockHandler CAN_BE_USED_IN_COMMANDS = (block) -> true;
    public static final BooleanBlockHandler CAN_CONTAIN_LIQUID = (block) -> false;
    public static final BooleanBlockHandler CAN_SPAWN_ON = (block) -> true;
}
