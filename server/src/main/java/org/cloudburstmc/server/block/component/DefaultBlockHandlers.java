package org.cloudburstmc.server.block.component;

import org.cloudburstmc.api.block.BlockComponents;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.block.BlockTags;
import org.cloudburstmc.api.block.component.*;
import org.cloudburstmc.api.event.entity.EntityDamageEvent;
import org.cloudburstmc.api.item.ItemKeys;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemType;
import org.cloudburstmc.api.util.Randoms;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.server.block.util.BlockSupport;
import org.cloudburstmc.server.level.collision.CloudVoxelShapes;
import org.cloudburstmc.server.registry.CloudItemRegistry;

import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;

public class DefaultBlockHandlers {

    public static final VoxelShapeBlockHandler GET_ENTITY_INSIDE_COLLISION_SHAPE = (state, context) -> state.getCollisionShape();

    public static final VoxelShapeBlockHandler GET_BLOCK_SUPPORT_SHAPE = (state, context) -> state.getCollisionShape();

    public static final FaceSupportBlockHandler IS_FACE_STURDY = org.cloudburstmc.server.block.util.BlockSupport::defaultFaceSturdy;

    public static final BooleanBlockStateHandler BLOCKS_MOTION = BlockSupport::defaultBlocksMotion;

    public static final BooleanBlockStateHandler CAN_OCCLUDE = (state) -> true;

    public static final BooleanBlockStateHandler PASSABLE = (state) -> !BlockSupport.blocksMotion(state);

    public static final BooleanBlockStateHandler SUFFOCATING = (state) -> {
        if (state.getType().hasTag(BlockTags.TRANSPARENT)) {
            return false;
        }

        return BlockSupport.blocksMotion(state) && BlockSupport.isCollisionShapeFullBlock(state);
    };

    public static final BooleanBlockStateHandler VIEW_BLOCKING = SUFFOCATING;

    public static final EntityInsideBlockHandler ON_ENTITY_INSIDE = (block, entity, precise) -> {
    };

    public static final EntityInsideBlockHandler CACTUS_ENTITY_INSIDE = (block, entity, precise) ->
            entity.attack(new EntityDamageEvent(entity, EntityDamageEvent.DamageCause.CONTACT, 1));

    public static final EntityInsideBlockHandler FIRE_ENTITY_INSIDE = (block, entity, precise) -> {
        entity.setOnFire(8);
        entity.attack(new EntityDamageEvent(entity, EntityDamageEvent.DamageCause.FIRE, 1));
    };

    public static final EntityInsideBlockHandler LAVA_ENTITY_INSIDE = (block, entity, precise) -> {
        entity.setOnFire(15);
        entity.attack(new EntityDamageEvent(entity, EntityDamageEvent.DamageCause.LAVA, 4));
    };

    public static final EntityInsideBlockHandler WEB_ENTITY_INSIDE = (block, entity, precise) ->
            entity.makeStuckInBlock(block.getState(), Vector3f.from(0.25f, 0.05f, 0.25f));

    public static final EntityInsideBlockHandler POWDER_SNOW_ENTITY_INSIDE = (block, entity, precise) ->
            entity.makeStuckInBlock(block.getState(), Vector3f.from(0.9f, 1.5f, 0.9f));

    public static final EntityInsideBlockHandler SWEET_BERRY_BUSH_ENTITY_INSIDE = (block, entity, precise) ->
            entity.makeStuckInBlock(block.getState(), Vector3f.from(0.8f, 0.75f, 0.8f));

    public static final VoxelShapeBlockHandler FULL_ENTITY_INSIDE_COLLISION_SHAPE = (state, context) -> CloudVoxelShapes.block();

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

    public static final ResourceBlockHandler GET_RESOURCE = (block, random, bonusLevel) -> {
        BlockState state = block.getState();
        ItemType itemType = CloudItemRegistry.get().getType(state.getType().getId(), 0);
        if (itemType == null) return ItemStack.EMPTY;
        return ItemStack.builder()
                .itemType(itemType)
                .data(ItemKeys.BLOCK_STATE, state.getType().getDefaultState())
                .amount(1)
                .build();
    };

    public static final ResourceBlockHandler GET_SILK_TOUCH_RESOURCE = (block, random, bonusLevel) -> {
        ResourceBlockHandler getResource = block.getComponents().get(BlockComponents.GET_RESOURCE);
        return getResource != null ? getResource.execute(block, random, bonusLevel) : ItemStack.EMPTY;
    };

    public static final PickBlockHandler GET_PICK_BLOCK = (block) -> {
        BlockState defaultState = block.getState().getType().getDefaultState();
        return defaultState.getType().asItem()
                .map(itemType -> ItemStack.builder()
                        .itemType(itemType)
                        .data(ItemKeys.BLOCK_STATE, defaultState)
                        .amount(1)
                        .build())
                .orElse(ItemStack.EMPTY);
    };

    public static final UseCheckHandler CAN_BE_USED = (block, player) -> true;
    public static final BooleanBlockHandler CAN_BE_SILK_TOUCHED = (block) -> true;
    public static final BooleanBlockHandler CAN_BE_USED_IN_COMMANDS = (block) -> true;
    public static final BooleanBlockHandler CAN_CONTAIN_LIQUID = (block) -> false;
    public static final BooleanBlockHandler CAN_SPAWN_ON = (block) -> true;
}
