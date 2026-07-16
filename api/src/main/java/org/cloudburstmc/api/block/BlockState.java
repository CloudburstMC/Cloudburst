package org.cloudburstmc.api.block;

import com.google.common.collect.ImmutableMap;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.trait.BlockTrait;
import org.cloudburstmc.api.block.trait.BooleanBlockTrait;
import org.cloudburstmc.api.block.trait.IntegerBlockTrait;
import org.cloudburstmc.api.util.VoxelShape;

import java.awt.*;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * A block type paired with immutable trait values and physical properties.
 */
public final class BlockState {

    private final BlockType type;
    private final Map<BlockTrait<?>, Comparable<?>> traits;
    private volatile LiquidState liquidState;
    private volatile Data data;
    private Map<BlockTrait<?>, BlockState[]> blockStates;

    BlockState(BlockType type, Map<BlockTrait<?>, Comparable<?>> traits) {
        this.type = type;
        this.traits = traits;
    }

    public BlockType getType() {
        return type;
    }

    public Map<BlockTrait<?>, Comparable<?>> getTraits() {
        return traits;
    }

    public boolean is(BlockTagKey tag) {
        return this.type.is(tag);
    }

    @SuppressWarnings("unchecked")
    public <T extends Comparable<T>> T ensureTrait(BlockTrait<T> trait) {
        checkNotNull(trait, "trait");
        T val = (T) this.traits.get(trait);
        checkNotNull(val, "Trait '%s' does not exist for type '%s'", trait, this.type);
        return val;
    }

    public <T extends Comparable<T>> BlockState withTrait(BlockTrait<T> trait, T value) {
        checkNotNull(trait, "trait");
        return this.blockStates.get(trait)[trait.getIndex(value)];
    }

    public BlockState withTrait(IntegerBlockTrait trait, int value) {
        checkNotNull(trait, "trait");
        return this.blockStates.get(trait)[trait.getIndex(value)];
    }

    public BlockState withTrait(BooleanBlockTrait trait, boolean value) {
        checkNotNull(trait, "trait");
        return this.blockStates.get(trait)[trait.getIndex(value)];
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public BlockState copyTraits(BlockState from) {
        BlockState result = this;
        for (Map.Entry<BlockTrait<?>, Comparable<?>> entry : from.getTraits().entrySet()) {
            result = result.withTrait((BlockTrait) entry.getKey(), (Comparable) entry.getValue());
        }
        return result;
    }

    public BlockState incrementTrait(IntegerBlockTrait trait) {
        checkNotNull(trait, "trait");
        return withTrait(trait, Math.min(trait.getRange().getEnd(), ensureTrait(trait) + 1));
    }

    public BlockState decrementTrait(IntegerBlockTrait trait) {
        checkNotNull(trait, "trait");
        return withTrait(trait, Math.max(trait.getRange().getStart(), ensureTrait(trait) - 1));
    }

    public BlockState toggleTrait(BooleanBlockTrait trait) {
        return this.blockStates.get(trait)[trait.getIndex(!((Boolean) this.traits.get(trait)))];
    }

    /**
     * Returns entity and placement collision geometry in block-local coordinates.
     *
     * @return collision shape
     */
    public VoxelShape getCollisionShape() {
        return data().collisionShape();
    }

    /**
     * Returns selection geometry in block-local coordinates.
     *
     * @return outline shape
     */
    public VoxelShape getOutlineShape() {
        return data().outlineShape();
    }

    /** @return this state's hardness */
    public float getHardness() {
        return data().hardness();
    }

    /** @return this state's explosion resistance */
    public float getExplosionResistance() {
        return data().explosionResistance();
    }

    /** @return this state's surface friction */
    public float getFriction() {
        return data().friction();
    }

    /** @return this state's translucency */
    public float getTranslucency() {
        return data().translucency();
    }

    /** @return this state's thickness */
    public float getThickness() {
        return data().thickness();
    }

    /** @return this state's chance of catching fire */
    public int getBurnOdds() {
        return data().burnOdds();
    }

    /** @return this state's chance of spreading fire */
    public int getFlameOdds() {
        return data().flameOdds();
    }

    /** @return the light removed while passing through this state */
    public int getLightDampening() {
        return data().lightDampening();
    }

    /** @return the light emitted by this state */
    public int getLightEmission() {
        return data().lightEmission();
    }

    /** @return whether this state is solid */
    public boolean isSolid() {
        return data().solid();
    }

    /** @return whether this state's drops require the correct tool */
    public boolean requiresCorrectToolForDrops() {
        return data().requiresCorrectToolForDrops();
    }

    /** @return this state's map color */
    public Color getMapColor() {
        return data().mapColor();
    }

    /**
     * @return whether this state can share its position with a liquid source
     */
    public boolean canContainLiquidSource() {
        return data().canContainLiquidSource();
    }

    /**
     * @return this state's reaction to flowing liquid
     */
    public LiquidReaction getLiquidReaction() {
        return data().liquidReaction();
    }

    /**
     * @return whether this state can share its position with flowing liquid
     */
    public boolean canContainFlowingLiquid() {
        return !isReplaceable() && getLiquidReaction().allowsFlow();
    }

    /**
     * @return whether this state may be replaced by normal block placement
     */
    public boolean isReplaceable() {
        return this.type == BlockTypes.AIR || this.type.isLiquid() || getLiquidReaction().removesBlock();
    }

    LiquidState asLiquidState() {
        checkState(this.type.isLiquid(), "Block state is not liquid: %s", this);
        LiquidState liquidState = this.liquidState;
        if (liquidState == null) {
            synchronized (this) {
                liquidState = this.liquidState;
                if (liquidState == null) {
                    liquidState = new LiquidState(this, this.type.getLiquidType());
                    this.liquidState = liquidState;
                }
            }
        }

        return liquidState;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(this.type);
        if (!this.traits.isEmpty()) {
            builder.append('{');
            this.traits.forEach((trait, value) ->
                    builder.append(trait).append('=').append(value.toString().toLowerCase()).append(',').append(' '));
            builder.setLength(builder.length() - 1);
            builder.setCharAt(builder.length() - 1, '}');
        }
        return builder.toString();
    }

    void initialize(Map<Map<BlockTrait<?>, Comparable<?>>, BlockState> map) {
        checkState(this.blockStates == null, "BlockTrait states has already been built");
        Map<BlockTrait<?>, BlockState[]> statesMap = new IdentityHashMap<>();

        for (Map.Entry<BlockTrait<?>, Comparable<?>> entry : this.traits.entrySet()) {
            BlockTrait<?> trait = entry.getKey();
            BlockState[] states = new BlockState[trait.getPossibleValues().size()];
            statesMap.put(trait, states);

            for (Comparable<?> comparable : trait.getPossibleValues()) {
                states[trait.getIndex(comparable)] = map.get(this.getTraitsWithValue(trait, comparable));
            }
        }

        this.blockStates = Collections.unmodifiableMap(statesMap);
    }

    synchronized void bindData(VoxelShape collisionShape, @Nullable VoxelShape outlineShape,
                               float hardness, float explosionResistance, float friction,
                               float translucency, float thickness, int burnOdds, int flameOdds,
                               int lightDampening, int lightEmission, boolean solid,
                               boolean requiresCorrectToolForDrops, Color mapColor,
                               boolean canContainLiquidSource, LiquidReaction liquidReaction) {
        checkState(this.data == null, "Block state data has already been initialized");
        this.data = new Data(collisionShape, outlineShape, hardness, explosionResistance, friction,
                translucency, thickness, burnOdds, flameOdds, lightDampening, lightEmission, solid,
                requiresCorrectToolForDrops, mapColor, canContainLiquidSource, liquidReaction);
    }

    private ImmutableMap<BlockTrait<?>, Comparable<?>> getTraitsWithValue(BlockTrait<?> trait, Comparable<?> comparable) {
        ImmutableMap.Builder<BlockTrait<?>, Comparable<?>> builder = ImmutableMap.builder();
        this.traits.forEach((k, v) -> builder.put(k, k == trait ? comparable : v));
        return builder.build();
    }

    private Data data() {
        Data data = this.data;
        checkState(data != null, "Block state data has not been initialized");
        return data;
    }

    private record Data(
            VoxelShape collisionShape,
            VoxelShape outlineShape,
            float hardness,
            float explosionResistance,
            float friction,
            float translucency,
            float thickness,
            int burnOdds,
            int flameOdds,
            int lightDampening,
            int lightEmission,
            boolean solid,
            boolean requiresCorrectToolForDrops,
            Color mapColor,
            boolean canContainLiquidSource,
            LiquidReaction liquidReaction
    ) {
        private Data {
            collisionShape = checkNotNull(collisionShape, "collisionShape");
            outlineShape = outlineShape == null ? collisionShape : outlineShape;
            mapColor = checkNotNull(mapColor, "mapColor");
            liquidReaction = checkNotNull(liquidReaction, "liquidReaction");
        }
    }
}
