package org.cloudburstmc.server.registry;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.entity.damage.DamageSource;
import org.cloudburstmc.api.entity.damage.DamageTypes;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.registry.BlockRegistry;
import org.cloudburstmc.api.registry.RegistryException;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.api.util.VoxelShape;
import org.cloudburstmc.api.util.component.ComponentBuilder;
import org.cloudburstmc.server.block.BlockGeometryData;
import org.cloudburstmc.server.block.BlockPalette;
import org.cloudburstmc.server.block.component.*;
import org.cloudburstmc.server.block.serializer.DefaultBlockSerializer;
import org.cloudburstmc.server.block.trait.BlockTraitSerializers;
import org.cloudburstmc.server.block.util.BlockSupport;
import org.cloudburstmc.server.level.collision.CloudVoxelShapes;
import org.cloudburstmc.server.registry.component.CloudComponentMap;

import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Owns registered block states, tags and behavior components.
 */
public class CloudBlockRegistry extends CloudComponentRegistry<BlockType> implements BlockRegistry {

    private final BlockPalette palette = BlockPalette.INSTANCE;
    private final List<BlockType> types;
    private final List<BlockState> states;
    private volatile boolean closed;

    public CloudBlockRegistry(CloudItemRegistry itemRegistry) {
        checkNotNull(itemRegistry, "itemRegistry");

        BlockTraitSerializers.init();
        this.types = List.copyOf(BlockTypes.values());
        this.registerDefaultComponents();
        this.types.forEach(this::registerVanilla);

        this.palette.generateRuntimeIds();
        this.states = this.palette.getSerializedPalette().values().stream().distinct().toList();
        this.types.forEach(this::configureModelOffsetShapes);

        VanillaBlockItems.register(itemRegistry);
        VanillaBlockBehaviors.configure(this);
        VanillaBlockTags.freeze();
    }

    @Override
    public BlockState getBlock(BlockType type) {
        checkNotNull(type, "type");
        if (this.getComponentMap(type) == null) {
            throw new RegistryException("Block type is not registered: " + type.getId());
        }
        return type.getDefaultState();
    }

    @Override
    public @Nullable BlockState getBlock(ItemStack item) {
        return checkNotNull(item, "item").getBlockState().orElse(null);
    }

    @Override
    public @Nullable BlockState getBlock(Identifier id) {
        return this.palette.getState(checkNotNull(id, "id"));
    }

    @Override
    public List<BlockState> getBlockStates() {
        return this.states;
    }

    @Override
    public List<BlockType> values() {
        return this.types;
    }

    @Override
    public BlockTag getTag(BlockTagKey key) {
        return VanillaBlockTags.resolve(checkNotNull(key, "key"));
    }

    @Override
    public VoxelShape getBlockSupportShape(BlockState state) {
        return BlockSupport.getBlockSupportShape(this, checkNotNull(state, "state"));
    }

    @Override
    public boolean isFaceSturdy(BlockState state, Direction face, SupportType supportType) {
        return BlockSupport.isFaceSturdy(
                this,
                checkNotNull(state, "state"),
                checkNotNull(face, "face"),
                checkNotNull(supportType, "supportType")
        );
    }

    @Override
    public synchronized ComponentBuilder configure(BlockType type) {
        this.requireOpen();
        CloudComponentMap components = this.getComponentMap(checkNotNull(type, "type"));
        if (components == null) {
            throw new RegistryException("Block type is not registered: " + type.getId());
        }
        return components;
    }

    @Override
    public synchronized void close() {
        if (this.closed) {
            return;
        }

        this.freezeComponentMaps();
        this.closed = true;
    }

    private void requireOpen() {
        if (this.closed) {
            throw new RegistryException("Block registration is closed");
        }
    }

    private void registerVanilla(BlockType type) {
        VanillaBlockTags.bind(type);
        this.putComponents(type, new CloudComponentMap(this));
        this.palette.addBlock(type, DefaultBlockSerializer.INSTANCE);
    }

    private void configureModelOffsetShapes(BlockType type) {
        ComponentBuilder components = this.configure(type);

        boolean collisionOffset = type.getStates().stream().anyMatch(state -> {
            BlockGeometryData geometry = this.palette.getGeometry(state);
            return geometry != null && geometry.collisionOffset() != null;
        });

        if (collisionOffset) {
            ModelOffsetShapeHandler handler = new ModelOffsetShapeHandler(this.palette, false);
            components.set(BlockComponents.GET_COLLISION_SHAPE, handler);
            components.set(BlockComponents.GET_BLOCK_SUPPORT_SHAPE, handler);
        }

        boolean outlineOffset = type.getStates().stream().anyMatch(state -> {
            BlockGeometryData geometry = this.palette.getGeometry(state);
            return geometry != null && geometry.outlineOffset() != null;
        });

        if (outlineOffset) {
            components.set(BlockComponents.GET_OUTLINE_SHAPE, new ModelOffsetShapeHandler(this.palette, true));
        }
    }

    private void registerDefaultComponents() {
        this.registerComponent(BlockComponents.AFTER_BREAK, (block, context) -> {});
        this.registerComponent(BlockComponents.ATTACK, DefaultBlockHandlers.ATTACK);
        this.registerComponent(BlockComponents.BREAKS_ON_LANDING, false);
        this.registerComponent(BlockComponents.BUCKET_PICKUP, (block, player) -> ItemStack.EMPTY);
        this.registerComponent(BlockComponents.BUTTON_PRESS_DURATION_TICKS, ButtonBlockHandlers.WOODEN_PRESS_TICKS);
        this.registerComponent(BlockComponents.CAN_BE_PLACED, block -> block.requireComponent(BlockComponents.CAN_SURVIVE).execute(block));
        this.registerComponent(BlockComponents.CAN_BE_REPLACED, (block, replacement, player, face, click) -> block.getState().isReplaceable() && block.getState().getType() != replacement.getType());
        this.registerComponent(BlockComponents.CAN_BE_USED, (block, player) -> false);
        this.registerComponent(BlockComponents.CAN_BE_USED_IN_COMMANDS, DefaultBlockHandlers.CAN_BE_USED_IN_COMMANDS);
        this.registerComponent(BlockComponents.CAN_RANDOM_TICK, false);
        this.registerComponent(BlockComponents.CAN_SLIDE, (block) -> false);
        this.registerComponent(BlockComponents.CAN_SPAWN_ON, DefaultBlockHandlers.CAN_SPAWN_ON);
        this.registerComponent(BlockComponents.CAN_SURVIVE, (block) -> true);
        this.registerComponent(BlockComponents.CHECK_ALIVE, (block) -> {});
        this.registerComponent(BlockComponents.CONDUCTS_REDSTONE, BlockSupport::isViewBlocking);
        this.registerComponent(BlockComponents.CONNECTS_TO_REDSTONE_WIRE, (block, direction) -> false);
        this.registerComponent(BlockComponents.FERTILIZE, (block, player, random) -> FertilizationResult.none());
        this.registerComponent(BlockComponents.GET_BLOCK_ENTITY, (block) -> Optional.empty());
        this.registerComponent(BlockComponents.GET_BLOCK_SUPPORT_SHAPE, DefaultBlockHandlers.GET_BLOCK_SUPPORT_SHAPE);
        this.registerComponent(BlockComponents.GET_COLLISION_SHAPE, new GetCollisionShapeHandler());
        this.registerComponent(BlockComponents.GET_COLOR, (block) -> null);
        this.registerComponent(BlockComponents.GET_DESCRIPTION_ID, (state) -> state.getType().getId().toString());
        this.registerComponent(BlockComponents.GET_DIRECT_REDSTONE_SIGNAL, (block, face) -> 0);
        this.registerComponent(BlockComponents.GET_ENTITY_INSIDE_COLLISION_SHAPE, DefaultBlockHandlers.GET_ENTITY_INSIDE_COLLISION_SHAPE);
        this.registerComponent(BlockComponents.GET_EXPERIENCE, DefaultBlockHandlers.GET_EXPERIENCE);
        this.registerComponent(BlockComponents.GET_EXPLOSION_LOOT, DefaultBlockHandlers.GET_EXPLOSION_LOOT);
        this.registerComponent(BlockComponents.GET_FALLING_DAMAGE_SOURCE, entity -> DamageSource.of(DamageTypes.FALLING_BLOCK, entity));
        this.registerComponent(BlockComponents.GET_GRAVITY, (block) -> 0.02f);
        this.registerComponent(BlockComponents.GET_IGNITED_STATE, block -> null);
        this.registerComponent(BlockComponents.GET_LIGHT_OCCLUSION_SHAPE, state -> CloudVoxelShapes.empty());
        this.registerComponent(BlockComponents.GET_LOOT, DefaultBlockHandlers.GET_LOOT);
        this.registerComponent(BlockComponents.GET_MAP_COLOR, (block) -> block.getState().getMapColor());
        this.registerComponent(BlockComponents.GET_OUTLINE_SHAPE, new GetOutlineShapeHandler());
        this.registerComponent(BlockComponents.GET_PICK_BLOCK, DefaultBlockHandlers.GET_PICK_BLOCK);
        this.registerComponent(BlockComponents.GET_REDSTONE_SIGNAL, (block, face) -> 0);
        this.registerComponent(BlockComponents.IS_BREAKABLE, (block, item) -> true);
        this.registerComponent(BlockComponents.IS_FREE_TO_FALL, (block) -> false);
        this.registerComponent(BlockComponents.IS_SOLID_SUPPORT, BlockState::isSolid);
        this.registerComponent(BlockComponents.MAY_PICK, (block) -> true);
        this.registerComponent(BlockComponents.MAY_PLACE, (block, direction) -> true);
        this.registerComponent(BlockComponents.MAY_PLACE_ON, (block) -> true);
        this.registerComponent(BlockComponents.ON_ADDED, block -> {});
        this.registerComponent(BlockComponents.ON_DESTROY, DefaultBlockHandlers.ON_DESTROY);
        this.registerComponent(BlockComponents.ON_ENTITY_COLLIDE, (block, entity) -> {});
        this.registerComponent(BlockComponents.ON_ENTITY_INSIDE, DefaultBlockHandlers.ON_ENTITY_INSIDE);
        this.registerComponent(BlockComponents.ON_EXPLOSION_HIT, DefaultBlockHandlers.ON_EXPLOSION_HIT);
        this.registerComponent(BlockComponents.ON_FALL_ON, DefaultBlockHandlers.ON_FALL_ON);
        this.registerComponent(BlockComponents.ON_FALLING_LAND, (entity, target, fallDistance) -> {});
        this.registerComponent(BlockComponents.ON_LIGHTNING_HIT, DefaultBlockHandlers.ON_LIGHTNING_HIT);
        this.registerComponent(BlockComponents.ON_LIQUID_CHANGED, (block, previousLiquid) -> {});
        this.registerComponent(BlockComponents.ON_LIQUID_TICK, block -> {});
        this.registerComponent(BlockComponents.ON_NEIGHBOUR_CHANGED, (block, neighbor) -> {});
        this.registerComponent(BlockComponents.ON_PLACE, new DefaultBlockPlaceHandler());
        this.registerComponent(BlockComponents.ON_PROJECTILE_HIT, DefaultBlockHandlers.ON_PROJECTILE_HIT);
        this.registerComponent(BlockComponents.ON_RANDOM_TICK, (block, randomGenerator) -> {});
        this.registerComponent(BlockComponents.ON_REDSTONE_UPDATE, DefaultBlockHandlers.ON_REDSTONE_UPDATE);
        this.registerComponent(BlockComponents.ON_REMOVE, DefaultBlockHandlers.ON_REMOVE);
        this.registerComponent(BlockComponents.ON_STAND_ON, DefaultBlockHandlers.ON_STAND_ON);
        this.registerComponent(BlockComponents.ON_STATE_CHANGED, block -> {});
        this.registerComponent(BlockComponents.ON_STEP_OFF, DefaultBlockHandlers.ON_STEP_OFF);
        this.registerComponent(BlockComponents.ON_STEP_ON, DefaultBlockHandlers.ON_STEP_ON);
        this.registerComponent(BlockComponents.ON_TICK, (block, randomGenerator) -> {});
        this.registerComponent(BlockComponents.RESOLVE_PLACEMENT_STATE, DefaultPlacementStateHandler.INSTANCE);
        this.registerComponent(BlockComponents.START_FALLING, (block) -> {});
        this.registerComponent(BlockComponents.SUFFOCATING, DefaultBlockHandlers.SUFFOCATING);
        this.registerComponent(BlockComponents.USE, (block, player, direction, item) -> false);
    }
}
