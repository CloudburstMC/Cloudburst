package org.cloudburstmc.server.testutil;

import lombok.Getter;
import org.cloudburstmc.api.Server;
import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.block.component.BlockShapeContext;
import org.cloudburstmc.api.blockentity.BlockEntity;
import org.cloudburstmc.api.event.Event;
import org.cloudburstmc.api.event.EventManager;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.gamerule.GameRules;
import org.cloudburstmc.api.level.particle.ParticleEmission;
import org.cloudburstmc.api.level.particle.ParticleEmitter;
import org.cloudburstmc.api.level.particle.ParticleOptions;
import org.cloudburstmc.api.level.particle.ParticleType;
import org.cloudburstmc.api.level.sound.SoundPlayback;
import org.cloudburstmc.api.level.sound.SoundType;
import org.cloudburstmc.api.registry.BlockRegistry;
import org.cloudburstmc.api.util.CollisionContext;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.block.BlockLayerRules;
import org.cloudburstmc.server.block.BlockLayers;
import org.cloudburstmc.server.level.gamerule.CloudGameRules;

import java.lang.reflect.InvocationHandler;
import java.util.*;
import java.util.function.Consumer;

/**
 * Sparse world fixture for block-handler tests. Records changes, events and tick delays
 * without executing scheduled ticks. Neighbor callbacks can be enabled for selected block types.
 */
public class BlockTestWorld {

    private final BlockRegistry registry;
    private final Server server;
    private final Level level;

    private final Map<Vector3i, BlockState> states = new HashMap<>();
    private final Map<Vector3i, BlockEntity> blockEntities = new HashMap<>();
    private final Map<Vector3i, LiquidState> liquids = new HashMap<>();
    private final Map<Vector3i, BlockState> secondaryStates = new HashMap<>();
    private final Map<Vector3i, Integer> scheduledUpdates = new HashMap<>();
    private final Set<Vector3i> unloadedPositions = new HashSet<>();
    private final Set<Vector3i> obstructedPositions = new HashSet<>();
    private final Set<BlockType> neighborUpdateTypes = new HashSet<>();
    private final List<ItemStack> drops = new ArrayList<>();
    private final List<Integer> scheduledDelays = new ArrayList<>();
    private final List<Event> events = new ArrayList<>();
    private final List<BlockBreakEffect> blockBreakEffects = new ArrayList<>();
    private final List<ParticleType> particles = new ArrayList<>();
    private final List<ParticleEmitter> particleEmitters = new ArrayList<>();
    private final List<Vector3f> particlePositions = new ArrayList<>();
    private final List<SoundType> sounds = new ArrayList<>();
    private final List<SoundPlayback> soundPlaybacks = new ArrayList<>();
    private final List<Vector3i> neighborUpdates = new ArrayList<>();
    private final CloudGameRules gameRules = new CloudGameRules();

    private Consumer<Event> eventListener = event -> {};

    @Getter
    private int breakCount;

    public BlockTestWorld(BlockRegistry registry) {
        this.registry = registry;
        this.gameRules.set(GameRules.DO_TILE_DROPS, true);
        EventManager eventManager = InterfaceProxy.create(EventManager.class, (proxy, method, arguments) -> {
            if (!method.getName().equals("fire")) {
                throw new AssertionError("Unexpected event access: " + method);
            }

            Event event = (Event) arguments[0];
            this.events.add(event);
            this.eventListener.accept(event);
            return null;
        });

        this.server = InterfaceProxy.create(Server.class, Map.of("getBlockRegistry", registry, "getEventManager", eventManager));
        this.level = InterfaceProxy.create(Level.class, (proxy, method, arguments) -> switch (method.getName()) {
            case "getServer" -> this.server;
            case "getGameRules" -> this.gameRules;
            case "getMinHeight" -> -64;
            case "getMaxHeight" -> 320;
            case "getTime" -> 0L;
            case "hasDaylightCycle" -> true;
            case "getBlockState" -> this.state(position(arguments));
            case "getBlock" -> this.block(position(arguments));
            case "getBlockEntity" -> this.blockEntities.get(position(arguments));
            case "getLoadedBlock" -> this.unloadedPositions.contains(position(arguments)) ? null : this.block(position(arguments));
            case "hasPlacementObstruction" -> this.obstructedPositions.contains((Vector3i) arguments[1]);
            case "setBlockState" -> this.applyState(arguments);
            case "batchBlockUpdates" -> {
                ((Runnable) arguments[0]).run();
                yield null;
            }
            case "playSound" -> {
                SoundPlayback playback;
                if (arguments[1] instanceof SoundPlayback supplied) {
                    playback = supplied;
                } else {
                    float volume = arguments.length > 2 && arguments[2] instanceof Number value ? value.floatValue() : 1;
                    float pitch = arguments.length > 3 && arguments[3] instanceof Number value ? value.floatValue() : 1;
                    playback = new SoundPlayback((SoundType) arguments[1], volume, pitch);
                }

                this.sounds.add(playback.sound());
                this.soundPlaybacks.add(playback);
                yield null;
            }
            case "spawnParticleEffect" -> {
                this.particleEmitters.add((ParticleEmitter) arguments[0]);
                this.particlePositions.add((Vector3f) arguments[1]);
                yield null;
            }
            case "spawnParticle" -> {
                ParticleOptions options = arguments[0] instanceof ParticleEmission emission ? emission.options() : (ParticleOptions) arguments[0];
                this.particles.add(options.getType());
                this.particlePositions.add((Vector3f) arguments[1]);
                yield null;
            }
            case "dropItem" -> {
                this.drops.add((ItemStack) arguments[1]);
                yield null;
            }
            case "playBlockBreakEffect" -> {
                this.blockBreakEffects.add(new BlockBreakEffect((Vector3i) arguments[0], (BlockState) arguments[1]));
                yield null;
            }
            case "updateAround" -> {
                this.neighborUpdates.add((Vector3i) arguments[0]);
                yield null;
            }
            case "scheduleUpdate" -> {
                this.scheduledDelays.add((Integer) arguments[1]);
                Vector3i position = arguments[0] instanceof Block block ? block.getPosition() : (Vector3i) arguments[0];
                this.scheduledUpdates.put(position, (Integer) arguments[1]);
                yield null;
            }
            case "isUpdateScheduled" -> this.scheduledUpdates.containsKey((Vector3i) arguments[0]);
            case "cancelScheduledUpdate" -> this.scheduledUpdates.remove((Vector3i) arguments[0]) != null;
            case "breakBlock" -> {
                this.setState((Vector3i) arguments[0], BlockStates.AIR);
                this.breakCount++;
                yield ItemStack.EMPTY;
            }
            default -> throw new AssertionError("Unexpected level access: " + method);
        });
    }

    public void setState(Vector3i position, BlockState state) {
        this.states.put(position, state);
    }

    public void setBlockEntity(Vector3i position, BlockEntity entity) {
        this.blockEntities.put(position, entity);
    }

    public void setLiquid(Vector3i position, LiquidState liquid) {
        this.liquids.put(position, liquid);
    }

    public void unload(Vector3i position) {
        this.unloadedPositions.add(position);
    }

    public void obstruct(Vector3i position) {
        this.obstructedPositions.add(position);
    }

    public void enableNeighborUpdates(BlockType... types) {
        this.neighborUpdateTypes.addAll(List.of(types));
    }

    public BlockState state(Vector3i position) {
        return this.states.getOrDefault(position, BlockStates.AIR);
    }

    public Block block(Vector3i position) {
        BlockState state = this.state(position);
        BlockState secondary = this.secondaryState(position);
        LiquidState liquid = this.liquid(position);
        return InterfaceProxy.create(Block.class, (proxy, method, arguments) -> switch (method.getName()) {
            case "getState" -> arguments == null || arguments.length == 0 || arguments[0] == BlockLayer.PRIMARY ? state : secondary;
            case "getSecondaryState" -> secondary;
            case "getLiquidLayer" -> state.getType().isLiquid() ? BlockLayer.PRIMARY : liquid.isEmpty() ? null : BlockLayer.SECONDARY;
            case "getPosition" -> position;
            case "getLevel" -> this.level;
            case "getLiquid" -> liquid;
            case "refresh" -> this.block(position);
            case "getCollisionShape" -> this.registry.requireComponent(state.getType(), BlockComponents.GET_COLLISION_SHAPE)
                    .execute(state, BlockShapeContext.at(this.level, position), (CollisionContext) arguments[0]);
            case "getOutlineShape" -> this.registry.requireComponent(state.getType(), BlockComponents.GET_OUTLINE_SHAPE)
                    .execute(state, BlockShapeContext.at(this.level, position));
            case "getSide" -> this.block(((Direction) arguments[0]).relative(position));
            case "getRelative" -> this.block(position.add((Integer) arguments[0], (Integer) arguments[1], (Integer) arguments[2]));
            case "getRelativeState" -> this.state(position.add((Integer) arguments[0], (Integer) arguments[1], (Integer) arguments[2]));
            case "getComponents" -> this.registry.requireComponents(state.getType());
            case "set" -> {
                this.setState(position, (BlockState) arguments[0]);
                yield null;
            }
            default -> {
                if (method.isDefault()) {
                    yield InvocationHandler.invokeDefault(proxy, method, arguments);
                }
                throw new AssertionError("Unexpected block access: " + method);
            }
        });
    }

    public List<Integer> getScheduledDelays() {
        return List.copyOf(this.scheduledDelays);
    }

    public Map<Vector3i, Integer> getScheduledUpdates() {
        return Map.copyOf(this.scheduledUpdates);
    }

    public List<BlockBreakEffect> getBlockBreakEffects() {
        return List.copyOf(this.blockBreakEffects);
    }

    public List<ParticleType> getParticles() {
        return List.copyOf(this.particles);
    }

    public List<ParticleEmitter> getParticleEmitters() {
        return List.copyOf(this.particleEmitters);
    }

    public List<Vector3f> getParticlePositions() {
        return List.copyOf(this.particlePositions);
    }

    public List<SoundType> getSounds() {
        return List.copyOf(this.sounds);
    }

    public List<SoundPlayback> getSoundPlaybacks() {
        return List.copyOf(this.soundPlaybacks);
    }

    public List<Vector3i> getNeighborUpdates() {
        return List.copyOf(this.neighborUpdates);
    }

    public List<ItemStack> getDrops() {
        return List.copyOf(this.drops);
    }

    public <E extends Event> List<E> getEvents(Class<E> eventType) {
        return this.events.stream().filter(eventType::isInstance).map(eventType::cast).toList();
    }

    public <E extends Event> void onEvent(Class<E> eventType, Consumer<E> listener) {
        this.eventListener = event -> {
            if (eventType.isInstance(event)) {
                listener.accept(eventType.cast(event));
            }
        };
    }

    private LiquidState liquid(Vector3i position) {
        LiquidState override = this.liquids.get(position);
        if (override != null) {
            return override;
        }

        BlockState state = this.state(position);
        BlockState liquid = state.getType().isLiquid() ? state : this.secondaryState(position);
        return liquid.getType().isLiquid() ? LiquidState.of(liquid) : LiquidState.empty();
    }

    private BlockState secondaryState(Vector3i position) {
        LiquidState override = this.liquids.get(position);
        if (override != null && !this.state(position).getType().isLiquid()) {
            return LiquidStateAccess.blockState(override);
        }

        return this.secondaryStates.getOrDefault(position, BlockStates.AIR);
    }

    private boolean applyState(Object[] arguments) {
        Vector3i position = position(arguments);
        int index = arguments[0] instanceof Vector3i ? 1 : 3;
        BlockLayer layer = arguments[index] instanceof BlockLayer requested ? requested : BlockLayer.PRIMARY;
        BlockState state = (BlockState) arguments[index + (arguments[index] instanceof BlockLayer ? 1 : 0)];
        BlockLayers before = new BlockLayers(this.state(position), this.secondaryState(position));
        BlockLayers after = BlockLayerRules.resolveReplacement(before, layer, state);
        if (after == null || before.equals(after)) {
            return false;
        }

        this.states.put(position, after.primary());
        this.secondaryStates.put(position, after.secondary());
        this.liquids.remove(position);
        boolean update = !(arguments[arguments.length - 1] instanceof Boolean flag) || flag;
        if (update) {
            this.neighborUpdates.add(position);
            if (!this.neighborUpdateTypes.isEmpty()) {
                Block changed = this.block(position);
                for (Direction direction : Direction.values()) {
                    Vector3i neighborPosition = direction.relative(position);
                    if (!this.unloadedPositions.contains(neighborPosition)) {
                        Block neighbor = this.block(neighborPosition);
                        if (this.neighborUpdateTypes.contains(neighbor.getState().getType())) {
                            this.registry.requireComponent(neighbor.getState().getType(), BlockComponents.ON_NEIGHBOUR_CHANGED)
                                    .execute(neighbor, changed);
                        }
                    }
                }
            }
        }

        return true;
    }

    private static Vector3i position(Object[] arguments) {
        return arguments[0] instanceof Vector3i position ? position : Vector3i.from((Integer) arguments[0], (Integer) arguments[1], (Integer) arguments[2]);
    }

    public record BlockBreakEffect(Vector3i position, BlockState state) {
    }
}
