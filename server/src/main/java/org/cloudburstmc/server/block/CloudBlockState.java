package org.cloudburstmc.server.block;

import com.google.common.collect.ImmutableMap;
import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.block.data.BlockData;
import org.cloudburstmc.api.block.trait.BlockTrait;
import org.cloudburstmc.api.block.trait.BooleanBlockTrait;
import org.cloudburstmc.api.block.trait.IntegerBlockTrait;
import org.cloudburstmc.api.util.VoxelShape;
import org.cloudburstmc.api.util.data.BlockMirror;
import org.cloudburstmc.api.util.data.BlockRotation;

import java.awt.*;
import java.util.*;
import java.util.List;

import static com.google.common.base.Preconditions.*;

/**
 * Canonical block state with server-owned properties and trait transitions.
 */
public class CloudBlockState implements BlockState {

    private final CloudBlockType type;
    private final Map<BlockTrait<?>, Comparable<?>> traits;
    private volatile LiquidState liquidState;
    private volatile BlockStateProperties data;
    private Map<BlockTrait<?>, BlockState[]> blockStates;

    public CloudBlockState(CloudBlockType type, Map<BlockTrait<?>, Comparable<?>> traits) {
        this.type = checkNotNull(type, "type");
        this.traits = ImmutableMap.copyOf(traits);
    }

    /**
     * @param state the state to check
     * @return the server-owned state
     * @throws IllegalArgumentException if the state is not owned by this server
     */
    public static CloudBlockState require(BlockState state) {
        if (state instanceof CloudBlockState cloudState) {
            return cloudState;
        }

        throw new IllegalArgumentException("Block state is not owned by this server: " + state);
    }

    /**
     * Binds trait transitions after all states of this type have been created.
     *
     * @param map states indexed by their complete trait values
     * @throws IllegalStateException if transitions are already initialized
     */
    public void initialize(Map<Map<BlockTrait<?>, Comparable<?>>, BlockState> map) {
        checkState(this.blockStates == null, "BlockTrait states has already been built");
        Map<BlockTrait<?>, BlockState[]> statesMap = new IdentityHashMap<>();

        for (Map.Entry<BlockTrait<?>, Comparable<?>> entry : this.traits.entrySet()) {
            BlockTrait<?> trait = entry.getKey();
            BlockState[] states = new BlockState[trait.getPossibleValues().size()];
            statesMap.put(trait, states);

            List<? extends Comparable<?>> values = trait.getPossibleValues();
            for (int index = 0; index < values.size(); index++) {
                Comparable<?> value = values.get(index);
                states[index] = map.get(this.getTraitsWithValue(trait, value));
            }
        }

        this.blockStates = Collections.unmodifiableMap(statesMap);
    }

    /**
     * @param properties the physical properties assigned during registration
     * @throws IllegalStateException if properties are already bound
     */
    public synchronized void bindProperties(BlockStateProperties properties) {
        checkState(this.data == null, "Block state data has already been initialized");
        this.data = checkNotNull(properties, "properties");
    }

    @Override
    public BlockType getType() {
        return type;
    }

    @Override
    public Map<BlockTrait<?>, Comparable<?>> getTraits() {
        return traits;
    }

    @Override
    public <D extends BlockData> Optional<D> getData(Class<D> dataType) {
        return CloudBlockData.get(this, dataType);
    }

    @Override
    public boolean is(BlockTagKey tag) {
        return this.type.is(tag);
    }

    @Override
    public <T extends Comparable<T>> T ensureTrait(BlockTrait<T> trait) {
        checkNotNull(trait, "trait");
        T val = trait.getValueClass().cast(this.traits.get(trait));
        checkArgument(val != null, "Trait '%s' does not exist for type '%s'", trait, this.type);
        return val;
    }

    @Override
    public <T extends Comparable<T>> BlockState withTrait(BlockTrait<T> trait, T value) {
        checkNotNull(trait, "trait");
        return stateForTrait(trait, valueIndex(trait, value), value);
    }

    @Override
    public BlockState withTrait(IntegerBlockTrait trait, int value) {
        checkNotNull(trait, "trait");
        return stateForTrait(trait, integerIndex(trait, value), value);
    }

    @Override
    public BlockState withTrait(BooleanBlockTrait trait, boolean value) {
        checkNotNull(trait, "trait");
        return stateForTrait(trait, value ? 1 : 0, value);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public BlockState copyTraits(BlockState from) {
        checkNotNull(from, "from");

        BlockState result = this;
        for (Map.Entry<BlockTrait<?>, Comparable<?>> entry : from.getTraits().entrySet()) {
            if (this.traits.containsKey(entry.getKey())) {
                result = result.withTrait((BlockTrait) entry.getKey(), (Comparable) entry.getValue());
            }
        }

        return result;
    }

    @Override
    public BlockState incrementTrait(IntegerBlockTrait trait) {
        checkNotNull(trait, "trait");
        int value = ensureTrait(trait);
        return value == trait.getRange().getEnd() ? this : withTrait(trait, value + 1);
    }

    @Override
    public BlockState decrementTrait(IntegerBlockTrait trait) {
        checkNotNull(trait, "trait");
        int value = ensureTrait(trait);
        return value == trait.getRange().getStart() ? this : withTrait(trait, value - 1);
    }

    @Override
    public BlockState toggleTrait(BooleanBlockTrait trait) {
        checkNotNull(trait, "trait");
        boolean value = !ensureTrait(trait);
        return stateForTrait(trait, value ? 1 : 0, value);
    }

    @Override
    public BlockState rotate(BlockRotation rotation) {
        return CloudBlockTransforms.rotate(this, rotation);
    }

    @Override
    public BlockState mirror(BlockMirror mirror) {
        return CloudBlockTransforms.mirror(this, mirror);
    }

    @Override
    public VoxelShape getCollisionShape() {
        return data().collisionShape();
    }

    @Override
    public VoxelShape getOutlineShape() {
        return data().outlineShape();
    }

    @Override
    public float getHardness() {
        return data().hardness();
    }

    @Override
    public float getExplosionResistance() {
        return data().explosionResistance();
    }

    @Override
    public float getFriction() {
        return data().friction();
    }

    @Override
    public float getTranslucency() {
        return data().translucency();
    }

    @Override
    public float getThickness() {
        return data().thickness();
    }

    @Override
    public int getBurnOdds() {
        return data().burnOdds();
    }

    @Override
    public int getFlameOdds() {
        return data().flameOdds();
    }

    @Override
    public int getLightDampening() {
        return data().lightDampening();
    }

    @Override
    public int getLightEmission() {
        return data().lightEmission();
    }

    @Override
    public boolean isSolid() {
        return data().solid();
    }

    @Override
    public boolean requiresCorrectToolForDrops() {
        return data().requiresCorrectToolForDrops();
    }

    @Override
    public Color getMapColor() {
        return data().mapColor();
    }

    @Override
    public boolean canContainLiquidSource() {
        return data().canContainLiquidSource();
    }

    @Override
    public LiquidReaction getLiquidReaction() {
        return data().liquidReaction();
    }

    @Override
    public boolean canContainFlowingLiquid() {
        return !isReplaceable() && getLiquidReaction().allowsFlow();
    }

    @Override
    public boolean isReplaceable() {
        return this.type.is(BlockTags.REPLACEABLE);
    }

    @Override
    public LiquidState getLiquidState() {
        if (!this.type.isLiquid()) {
            return EmptyLiquidState.INSTANCE;
        }

        LiquidState liquidState = this.liquidState;
        if (liquidState == null) {
            synchronized (this) {
                liquidState = this.liquidState;
                if (liquidState == null) {
                    liquidState = new CloudLiquidState(this, this.type.getLiquidType());
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

    private ImmutableMap<BlockTrait<?>, Comparable<?>> getTraitsWithValue(BlockTrait<?> trait, Comparable<?> comparable) {
        ImmutableMap.Builder<BlockTrait<?>, Comparable<?>> builder = ImmutableMap.builder();
        this.traits.forEach((k, v) -> builder.put(k, k == trait ? comparable : v));
        return builder.build();
    }

    private static int valueIndex(BlockTrait<?> trait, Object value) {
        checkArgument(trait.getValueClass().isInstance(value), "Invalid value type for trait '%s': %s", trait, value);
        if (trait instanceof IntegerBlockTrait integer) {
            return integerIndex(integer, (Integer) value);
        }

        int index = trait.getPossibleValues().indexOf(value);
        checkArgument(index >= 0, "Unsupported value for trait '%s': %s", trait, value);
        return index;
    }

    private static int integerIndex(IntegerBlockTrait trait, int value) {
        if (value < trait.getRange().getStart() || value > trait.getRange().getEnd()) {
            throw new IllegalArgumentException("Value " + value + " is outside the range for trait " + trait);
        }
        return value - trait.getRange().getStart();
    }

    private BlockState stateForTrait(BlockTrait<?> trait, int index, Object value) {
        checkState(this.blockStates != null, "Block states have not been bound for type '%s'", this.type);

        BlockState[] states = this.blockStates.get(trait);
        checkArgument(states != null, "Trait '%s' does not exist for type '%s'", trait, this.type);
        checkArgument(index >= 0 && index < states.length, "Trait value '%s' is not valid for trait '%s' on type '%s'", value, trait, this.type);

        BlockState state = states[index];
        checkArgument(state != null, "Trait value '%s' is not valid for trait '%s' on type '%s'", value, trait, this.type);

        return state;
    }

    private BlockStateProperties data() {
        BlockStateProperties data = this.data;
        checkState(data != null, "Block state data has not been initialized");
        return data;
    }
}
