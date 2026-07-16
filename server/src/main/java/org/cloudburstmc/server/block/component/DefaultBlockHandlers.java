package org.cloudburstmc.server.block.component;

import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.block.component.*;
import org.cloudburstmc.api.blockentity.ShulkerBox;
import org.cloudburstmc.api.blockentity.ShulkerBoxAnimationState;
import org.cloudburstmc.api.event.entity.EntityDamageEvent;
import org.cloudburstmc.api.item.ItemKeys;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemType;
import org.cloudburstmc.api.util.CollisionContext;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.Randoms;
import org.cloudburstmc.api.util.VoxelShape;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.server.block.util.BlockSupport;
import org.cloudburstmc.server.block.util.ShulkerBoxGeometry;
import org.cloudburstmc.server.level.collision.CloudVoxelShapes;
import org.cloudburstmc.server.registry.CloudItemRegistry;

import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;

public class DefaultBlockHandlers {

    private static final float PIXEL = 1f / 16f;

    public static final VoxelShapeBlockHandler GET_ENTITY_INSIDE_COLLISION_SHAPE = (state, context) -> state.getCollisionShape();

    public static final BlockSupportShapeHandler GET_BLOCK_SUPPORT_SHAPE = (state, context) -> state.getCollisionShape();

    public static final BlockSupportShapeHandler FULL_BLOCK_SUPPORT_SHAPE = (state, context) -> CloudVoxelShapes.block();

    public static final BlockSupportShapeHandler EMPTY_BLOCK_SUPPORT_SHAPE = (state, context) -> CloudVoxelShapes.empty();

    public static final BlockSupportShapeHandler OUTLINE_BLOCK_SUPPORT_SHAPE = (state, context) -> state.getOutlineShape();

    public static final BlockSupportShapeHandler CHORUS_FLOWER_BLOCK_SUPPORT_SHAPE = (state, context) ->
            CloudVoxelShapes.box(PIXEL, 0, PIXEL, 15 * PIXEL, 15 * PIXEL, 15 * PIXEL);

    public static final BlockSupportShapeHandler SNOW_LAYER_BLOCK_SUPPORT_SHAPE = (state, context) ->
            CloudVoxelShapes.box(0, 0, 0, 1, (state.ensureTrait(BlockTraits.HEIGHT) + 1) * 2 * PIXEL, 1);

    public static final BlockSupportShapeHandler FENCE_GATE_BLOCK_SUPPORT_SHAPE = (state, context) -> {
        if (state.ensureTrait(BlockTraits.IS_OPEN)) {
            return CloudVoxelShapes.empty();
        }

        return switch (state.ensureTrait(BlockTraits.CARDINAL_DIRECTION).toDirection().getAxis()) {
            case X -> CloudVoxelShapes.box(6 * PIXEL, 5 * PIXEL, 0, 10 * PIXEL, 24 * PIXEL, 1);
            case Z -> CloudVoxelShapes.box(0, 5 * PIXEL, 6 * PIXEL, 1, 24 * PIXEL, 10 * PIXEL);
            case Y -> throw new IllegalStateException("Fence gate cannot face vertically");
        };
    };

    public static final BlockSupportShapeHandler SHULKER_BOX_BLOCK_SUPPORT_SHAPE = new BlockSupportShapeHandler() {
        @Override
        public VoxelShape execute(BlockState state, BlockShapeContext context) {
            if (!context.hasLevel()
                    || !(context.level().getBlockEntity(context.position()) instanceof ShulkerBox shulkerBox)
                    || shulkerBox.getAnimationState() == ShulkerBoxAnimationState.CLOSED) {
                return CloudVoxelShapes.block();
            }

            return shulkerOpenSupportShape(shulkerBox.getFacing().getOpposite());
        }

        @Override
        public ShapeContextRequirement contextRequirement() {
            return ShapeContextRequirement.BLOCK_ENTITY;
        }
    };

    public static final CollisionShapeHandler SHULKER_BOX_COLLISION_SHAPE = new CollisionShapeHandler() {
        @Override
        public VoxelShape execute(BlockState state, BlockShapeContext blockContext, CollisionContext collisionContext) {
            return shulkerBoxShape(blockContext, state.getCollisionShape());
        }

        @Override
        public ShapeContextRequirement contextRequirement() {
            return ShapeContextRequirement.BLOCK_ENTITY;
        }
    };

    public static final BlockShapeHandler SHULKER_BOX_OUTLINE_SHAPE = new BlockShapeHandler() {
        @Override
        public VoxelShape execute(BlockState state, BlockShapeContext context) {
            return shulkerBoxShape(context, state.getOutlineShape());
        }

        @Override
        public ShapeContextRequirement contextRequirement() {
            return ShapeContextRequirement.BLOCK_ENTITY;
        }
    };

    private static VoxelShape shulkerBoxShape(BlockShapeContext context, VoxelShape fallback) {
        if (!context.hasLevel()
                || !(context.level().getBlockEntity(context.position()) instanceof ShulkerBox shulkerBox)) {
            return fallback;
        }

        return ShulkerBoxGeometry.shape(shulkerBox.getFacing(), shulkerBox.getOpenProgress());
    }

    private static VoxelShape shulkerOpenSupportShape(Direction face) {
        return switch (face) {
            case DOWN -> CloudVoxelShapes.box(0, 0, 0, 1, PIXEL, 1);
            case UP -> CloudVoxelShapes.box(0, 15 * PIXEL, 0, 1, 1, 1);
            case NORTH -> CloudVoxelShapes.box(0, 0, 0, 1, 1, PIXEL);
            case SOUTH -> CloudVoxelShapes.box(0, 0, 15 * PIXEL, 1, 1, 1);
            case WEST -> CloudVoxelShapes.box(0, 0, 0, PIXEL, 1, 1);
            case EAST -> CloudVoxelShapes.box(15 * PIXEL, 0, 0, 1, 1, 1);
        };
    }

    public static final BooleanBlockStateHandler SUFFOCATING = (state) -> {
        if (state.is(BlockTags.TRANSPARENT)) {
            return false;
        }

        return BlockSupport.blocksMotion(state) && BlockSupport.isCollisionShapeFullBlock(state);
    };

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
        ResourceBlockHandler getResource = block.getComponent(BlockComponents.GET_RESOURCE);
        if (getResource != null) {
            getResource.execute(block, ThreadLocalRandom.current(), 0);
        }
    };

    public static final SpawnResourcesBlockHandler SPAWN_RESOURCES = (block, random, tool, bonusLootLevel) -> {
        ResourceCountBlockHandler getResourceCount = block.getComponent(BlockComponents.GET_RESOURCE_COUNT);
        int resourceCount = getResourceCount != null ? getResourceCount.execute(block, random, bonusLootLevel) : 1;
        if (resourceCount < 1) {
            return;
        }

        ResourceBlockHandler getResource = block.getComponent(BlockComponents.GET_RESOURCE);
        DropResourceBlockHandler dropResource = block.getComponent(BlockComponents.DROP_RESOURCE);

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
        ResourceBlockHandler getResource = block.getComponent(BlockComponents.GET_RESOURCE);
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
    public static final BooleanBlockHandler CAN_SPAWN_ON = (block) -> true;
}
