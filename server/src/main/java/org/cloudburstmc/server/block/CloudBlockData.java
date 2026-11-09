package org.cloudburstmc.server.block;

import lombok.experimental.UtilityClass;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.block.data.*;
import org.cloudburstmc.api.block.trait.BooleanBlockTrait;
import org.cloudburstmc.api.block.trait.EnumBlockTrait;
import org.cloudburstmc.api.block.trait.IntegerBlockTrait;
import org.cloudburstmc.api.registry.BlockRegistry;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.api.util.data.CardinalDirection;

import java.util.*;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Map.entry;
import static java.util.Objects.requireNonNull;

/**
 * Resolves typed property views without changing canonical block states.
 */
@UtilityClass
public class CloudBlockData {

    private static final Map<String, StateVariants> LIT_VARIANTS = indexVariants(List.of(
            new StateVariants("furnace", "lit_furnace"),
            new StateVariants("blast_furnace", "lit_blast_furnace"),
            new StateVariants("smoker", "lit_smoker"),
            new StateVariants("redstone_lamp", "lit_redstone_lamp"),
            new StateVariants("unlit_redstone_torch", "redstone_torch"),
            new StateVariants("redstone_ore", "lit_redstone_ore"),
            new StateVariants("deepslate_redstone_ore", "lit_deepslate_redstone_ore")));

    private static final Map<String, StateVariants> POWER_VARIANTS = indexVariants(List.of(
            new StateVariants("unpowered_repeater", "powered_repeater"),
            new StateVariants("unpowered_comparator", "powered_comparator")));

    private static final Set<Direction.Axis> PORTAL_AXES = Set.of(Direction.Axis.X, Direction.Axis.Z);

    private static final Map<String, AgeProperty> AGES = Map.ofEntries(
            entry("wheat", new AgeProperty(BlockTraits.GROWTH, 7)),
            entry("carrots", new AgeProperty(BlockTraits.GROWTH, 7)),
            entry("potatoes", new AgeProperty(BlockTraits.GROWTH, 7)),
            entry("beetroot", new AgeProperty(BlockTraits.GROWTH, 7)),
            entry("pitcher_crop", new AgeProperty(BlockTraits.GROWTH, 7)),
            entry("torchflower_crop", new AgeProperty(BlockTraits.GROWTH, 1)),
            entry("melon_stem", new AgeProperty(BlockTraits.GROWTH, 7)),
            entry("pumpkin_stem", new AgeProperty(BlockTraits.GROWTH, 7)),
            entry("sweet_berry_bush", new AgeProperty(BlockTraits.GROWTH, 3)),
            entry("cactus", new AgeProperty(BlockTraits.AGE, 15)),
            entry("reeds", new AgeProperty(BlockTraits.AGE, 15)),
            entry("cocoa", new AgeProperty(BlockTraits.AGE, 2)),
            entry("nether_wart", new AgeProperty(BlockTraits.AGE, 3)),
            entry("chorus_flower", new AgeProperty(BlockTraits.AGE, 5)),
            entry("fire", new AgeProperty(BlockTraits.AGE, 15)),
            entry("soul_fire", new AgeProperty(BlockTraits.AGE, 15)),
            entry("frosted_ice", new AgeProperty(BlockTraits.AGE, 3)),
            entry("kelp", new AgeProperty(BlockTraits.KELP_AGE, 25)),
            entry("cave_vines", new AgeProperty(BlockTraits.CAVE_VINE_AGE, 25)),
            entry("cave_vines_head_with_berries", new AgeProperty(BlockTraits.CAVE_VINE_AGE, 25)),
            entry("cave_vines_body_with_berries", new AgeProperty(BlockTraits.CAVE_VINE_AGE, 25)),
            entry("twisting_vines", new AgeProperty(BlockTraits.TWISTING_VINES_AGE, 25)),
            entry("weeping_vines", new AgeProperty(BlockTraits.WEEPING_VINES_AGE, 25)));

    /**
     * @param state    the state to view
     * @param dataType the requested property interface
     * @param <D>      the view type
     * @return the supported view, or empty when unavailable
     */
    public static <D extends BlockData> Optional<D> get(BlockState state, Class<D> dataType) {
        requireNonNull(state, "state");
        requireNonNull(dataType, "dataType");

        @Nullable BlockData data = null;
        if (dataType == Ageable.class) {
            AgeProperty age = state.getType().getId().getNamespace().equals("minecraft") ? AGES.get(state.getType().getId().getName()) : null;
            if (age != null && state.getTraits().containsKey(age.trait()) && state.ensureTrait(age.trait()) <= age.maximum()) {
                data = new AgeData(state, age);
            }
        } else if (dataType == Directional.class) {
            data = directional(state);
        } else if (dataType == Orientable.class) {
            data = orientable(state);
        } else if (dataType == Lightable.class) {
            data = lightable(state);
        } else if (dataType == Openable.class && state.getTraits().containsKey(BlockTraits.IS_OPEN) && !isLever(state)) {
            data = new OpenData(state);
        } else if (dataType == Powerable.class) {
            data = powerable(state);
        } else if (dataType == AnaloguePowerable.class && state.getTraits().containsKey(BlockTraits.REDSTONE_SIGNAL)) {
            data = new SignalData(state);
        } else if (dataType == Rotatable.class && state.getTraits().containsKey(BlockTraits.SIGN_DIRECTION)) {
            data = new RotationData(state);
        } else {
            Optional<D> structure = CloudBlockStructureData.get(state, dataType);
            if (structure.isPresent()) {
                return structure;
            }

            Optional<D> connections = CloudBlockConnections.get(state, dataType);
            if (connections.isPresent()) {
                return connections;
            }

            Optional<D> properties = CloudBlockPropertyData.get(state, dataType);
            return properties.isPresent() ? properties : CloudBlockTypeData.get(state, dataType);
        }

        return Optional.ofNullable(dataType.cast(data));
    }

    /**
     * @param snapshot the layers to capture for the view
     * @param dataType the requested property interface
     * @param <D>      the view type
     * @return a view of the captured layers or primary state, or empty when unsupported
     */
    public static <D extends BlockData> Optional<D> get(BlockSnapshot snapshot, Class<D> dataType) {
        requireNonNull(snapshot, "snapshot");
        requireNonNull(dataType, "dataType");
        if (dataType != Waterlogged.class) {
            return snapshot.getState().getData(dataType);
        }

        BlockState primary = snapshot.getState();
        BlockState secondary = snapshot.getSecondaryState();
        boolean water = secondary.getLiquidState().getType().isSameFamily(LiquidTypes.WATER);
        if (primary.getType().isLiquid() || primary.getType() == BlockTypes.BUBBLE_COLUMN
                || !primary.canContainLiquidSource()
                || !BlockLayerRules.canCoexist(primary, BlockStates.AIR)
                || (secondary != BlockStates.AIR && !water)
                || !BlockLayerRules.canCoexist(primary, secondary)
                || !BlockLayerRules.canCoexist(primary, BlockStates.WATER)) {
            return Optional.empty();
        }

        BlockSnapshot captured = new CloudBlockSnapshot(primary, secondary);
        return Optional.of(dataType.cast(new WaterData(captured)));
    }

    private static Map<String, StateVariants> indexVariants(List<StateVariants> definitions) {
        Map<String, StateVariants> variants = new HashMap<>();
        for (StateVariants variant : definitions) {
            variants.put(variant.inactive(), variant);
            variants.put(variant.active(), variant);
        }

        return Map.copyOf(variants);
    }

    private static @Nullable Lightable lightable(BlockState state) {
        if (state.getTraits().containsKey(BlockTraits.IS_LIT)) {
            return new LitData(state, BlockTraits.IS_LIT, false);
        }

        if (state.getTraits().containsKey(BlockTraits.IS_EXTINGUISHED)) {
            return new LitData(state, BlockTraits.IS_EXTINGUISHED, true);
        }

        if (state.getType().getId().getNamespace().equals("minecraft")) {
            StateVariants variant = LIT_VARIANTS.get(state.getType().getId().getName());
            if (variant != null) {
                return new VariantLitData(state, variant);
            }
        }

        return null;
    }

    private static @Nullable Powerable powerable(BlockState state) {
        if (state.getTraits().containsKey(BlockTraits.IS_POWERED)) {
            return new PoweredData(state, BlockTraits.IS_POWERED);
        }

        if (state.getTraits().containsKey(BlockTraits.IS_BUTTON_PRESSED)) {
            return new PoweredData(state, BlockTraits.IS_BUTTON_PRESSED);
        }

        if (isLever(state)) {
            return new PoweredData(state, BlockTraits.IS_OPEN);
        }

        if (state.getTraits().containsKey(BlockTraits.REDSTONE_SIGNAL)) {
            return new SignalData(state);
        }

        if (state.getType().getId().getNamespace().equals("minecraft")) {
            StateVariants variants = POWER_VARIANTS.get(state.getType().getId().getName());
            if (variants != null) {
                return new VariantPowerData(state, variants);
            }
        }

        return null;
    }

    private static boolean isLever(BlockState state) {
        Identifier id = state.getType().getId();
        return id.getNamespace().equals("minecraft") && id.getName().equals("lever");
    }

    private static @Nullable Directional directional(BlockState state) {
        if (state.getTraits().containsKey(BlockTraits.FACING_DIRECTION)) {
            return directionData(state, BlockTraits.FACING_DIRECTION, Function.identity());
        }

        if (state.getTraits().containsKey(BlockTraits.CARDINAL_DIRECTION)) {
            return directionData(state, BlockTraits.CARDINAL_DIRECTION, CardinalDirection::toDirection);
        }

        if (state.getTraits().containsKey(BlockTraits.DIRECTION)) {
            return directionData(state, BlockTraits.DIRECTION, Function.identity());
        }

        if (state.getTraits().containsKey(BlockTraits.BLOCK_FACE)) {
            return directionData(state, BlockTraits.BLOCK_FACE, Function.identity());
        }

        if (state.getTraits().containsKey(BlockTraits.CORAL_DIRECTION)) {
            return directionData(state, BlockTraits.CORAL_DIRECTION, Function.identity());
        }

        if (state.getTraits().containsKey(BlockTraits.TORCH_DIRECTION)) {
            return directionData(state, BlockTraits.TORCH_DIRECTION, Function.identity());
        }

        return null;
    }

    private static <E extends Enum<E>> @Nullable Directional directionData(BlockState state, EnumBlockTrait<E> trait, Function<E, Direction> direction) {
        Map<Direction, E> values = new EnumMap<>(Direction.class);
        for (E value : trait.getPossibleValues()) {
            if (trait == BlockTraits.TORCH_DIRECTION && value == Direction.DOWN) {
                continue;
            }
            values.put(direction.apply(value), value);
        }

        if (!values.containsValue(state.ensureTrait(trait))) {
            return null;
        }

        return new FacingData<>(state, trait, direction, Map.copyOf(values));
    }

    private static @Nullable Orientable orientable(BlockState state) {
        if (state.getTraits().containsKey(BlockTraits.AXIS)) {
            return new AxisData(state, BlockTraits.AXIS, Set.copyOf(BlockTraits.AXIS.getPossibleValues()));
        }

        if (state.getTraits().containsKey(BlockTraits.PORTAL_AXIS) && PORTAL_AXES.contains(state.ensureTrait(BlockTraits.PORTAL_AXIS))) {
            return new AxisData(state, BlockTraits.PORTAL_AXIS, PORTAL_AXES);
        }

        return null;
    }

    private record AgeProperty(IntegerBlockTrait trait, int maximum) {
    }

    private record AgeData(BlockState state, AgeProperty age) implements Ageable {

        @Override
        public BlockState getState() {
            return this.state;
        }

        @Override
        public int getAge() {
            return this.state.ensureTrait(this.age.trait());
        }

        @Override
        public int getMaximumAge() {
            return this.age.maximum();
        }

        @Override
        public BlockState withAge(int age) {
            checkArgument(age >= 0 && age <= this.age.maximum(), "Age must be between 0 and %s", this.age.maximum());
            return this.state.withTrait(this.age.trait(), age);
        }
    }

    private record FacingData<E extends Enum<E>>(BlockState state, EnumBlockTrait<E> trait, Function<E, Direction> direction, Map<Direction, E> values) implements Directional {

        @Override
        public BlockState getState() {
            return this.state;
        }

        @Override
        public Direction getFacing() {
            return this.direction.apply(this.state.ensureTrait(this.trait));
        }

        @Override
        public Set<Direction> getFaces() {
            return this.values.keySet();
        }

        @Override
        public BlockState withFacing(Direction facing) {
            E value = this.values.get(requireNonNull(facing, "facing"));
            checkArgument(value != null, "Unsupported facing: %s", facing);
            return this.state.withTrait(this.trait, value);
        }
    }

    private record AxisData(BlockState state, EnumBlockTrait<Direction.Axis> trait, Set<Direction.Axis> axes) implements Orientable {

        @Override
        public BlockState getState() {
            return this.state;
        }

        @Override
        public Direction.Axis getAxis() {
            return this.state.ensureTrait(this.trait);
        }

        @Override
        public Set<Direction.Axis> getAxes() {
            return this.axes;
        }

        @Override
        public BlockState withAxis(Direction.Axis axis) {
            checkArgument(this.axes.contains(requireNonNull(axis, "axis")), "Unsupported axis: %s", axis);
            return this.state.withTrait(this.trait, axis);
        }
    }

    private record OpenData(BlockState state) implements Openable {

        @Override
        public BlockState getState() {
            return this.state;
        }

        @Override
        public boolean isOpen() {
            return this.state.ensureTrait(BlockTraits.IS_OPEN);
        }

        @Override
        public BlockState withOpen(boolean open) {
            return this.state.withTrait(BlockTraits.IS_OPEN, open);
        }
    }

    private record LitData(BlockState state, BooleanBlockTrait trait, boolean inverted) implements Lightable {

        @Override
        public BlockState getState() {
            return this.state;
        }

        @Override
        public boolean isLit() {
            return this.state.ensureTrait(this.trait) != this.inverted;
        }

        @Override
        public BlockState withLit(boolean lit) {
            return this.state.withTrait(this.trait, lit != this.inverted);
        }
    }

    private record StateVariants(String inactive, String active) {

        public boolean isActive(BlockState state) {
            return state.getType().getId().getName().equals(this.active);
        }

        public BlockState withValue(BlockState state, boolean active) {
            if (this.isActive(state) == active) {
                return state;
            }

            String name = active ? this.active : this.inactive;
            return BlockRegistry.get().getOrThrow(Identifier.parse(name)).getDefaultState().copyTraits(state);
        }
    }

    private record VariantLitData(BlockState state, StateVariants variant) implements Lightable {

        @Override
        public BlockState getState() {
            return this.state;
        }

        @Override
        public boolean isLit() {
            return this.variant.isActive(this.state);
        }

        @Override
        public BlockState withLit(boolean lit) {
            return this.variant.withValue(this.state, lit);
        }
    }

    private record PoweredData(BlockState state, BooleanBlockTrait trait) implements Powerable {

        @Override
        public BlockState getState() {
            return this.state;
        }

        @Override
        public boolean isPowered() {
            return this.state.ensureTrait(this.trait);
        }

        @Override
        public BlockState withPowered(boolean powered) {
            return this.state.withTrait(this.trait, powered);
        }
    }

    private record SignalData(BlockState state) implements AnaloguePowerable, Powerable {

        @Override
        public BlockState getState() {
            return this.state;
        }

        @Override
        public int getPower() {
            return this.state.ensureTrait(BlockTraits.REDSTONE_SIGNAL);
        }

        @Override
        public int getMaximumPower() {
            return BlockTraits.REDSTONE_SIGNAL.getRange().getEnd();
        }

        @Override
        public BlockState withPower(int power) {
            return this.state.withTrait(BlockTraits.REDSTONE_SIGNAL, power);
        }

        @Override
        public boolean isPowered() {
            return this.getPower() > 0;
        }

        @Override
        public BlockState withPowered(boolean powered) {
            if (this.isPowered() == powered) {
                return this.state;
            }

            return this.withPower(powered ? this.getMaximumPower() : 0);
        }
    }

    private record VariantPowerData(BlockState state, StateVariants variants) implements Powerable {

        @Override
        public BlockState getState() {
            return this.state;
        }

        @Override
        public boolean isPowered() {
            return this.variants.isActive(this.state);
        }

        @Override
        public BlockState withPowered(boolean powered) {
            BlockState changed = this.variants.withValue(this.state, powered);
            if (changed.getTraits().containsKey(BlockTraits.IS_OUTPUT_LIT)) {
                return changed.withTrait(BlockTraits.IS_OUTPUT_LIT, powered);
            }

            return changed;
        }
    }

    private record RotationData(BlockState state) implements Rotatable {

        @Override
        public BlockState getState() {
            return this.state;
        }

        @Override
        public CardinalDirection getRotation() {
            return this.state.ensureTrait(BlockTraits.SIGN_DIRECTION);
        }

        @Override
        public Set<CardinalDirection> getRotations() {
            return Set.copyOf(BlockTraits.SIGN_DIRECTION.getPossibleValues());
        }

        @Override
        public BlockState withRotation(CardinalDirection rotation) {
            return this.state.withTrait(BlockTraits.SIGN_DIRECTION, rotation);
        }
    }

    private record WaterData(BlockSnapshot snapshot) implements Waterlogged {

        @Override
        public BlockSnapshot getSnapshot() {
            return this.snapshot;
        }

        @Override
        public boolean isWaterlogged() {
            return this.snapshot.getSecondaryState().getLiquidState().getType().isSameFamily(LiquidTypes.WATER);
        }

        @Override
        public BlockSnapshot withWaterlogged(boolean waterlogged) {
            if (this.isWaterlogged() == waterlogged) {
                return this.snapshot;
            }

            BlockLayers layers = new BlockLayers(this.snapshot.getState(), this.snapshot.getSecondaryState());
            BlockState water = waterlogged ? BlockStates.WATER : BlockStates.AIR;
            BlockLayers changed = BlockLayerRules.resolveReplacement(layers, BlockLayer.SECONDARY, water);
            checkArgument(changed != null, "Block cannot contain the requested water state");
            return new CloudBlockSnapshot(changed.primary(), changed.secondary());
        }
    }

}
