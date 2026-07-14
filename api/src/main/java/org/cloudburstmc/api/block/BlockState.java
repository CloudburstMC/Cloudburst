package org.cloudburstmc.api.block;

import com.google.common.collect.ImmutableMap;
import org.cloudburstmc.api.block.trait.BlockTrait;
import org.cloudburstmc.api.block.trait.BooleanBlockTrait;
import org.cloudburstmc.api.block.trait.IntegerBlockTrait;
import org.cloudburstmc.api.util.VoxelShape;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * An immutable snapshot of a block type and its current trait values.
 * <p>
 * Shape data is injected at startup by {@code BlockPalette}
 * from {@code BlockPropertyData} for states where geometry varies by trait.
 */
public final class BlockState {

    private final BlockType type;
    private final Map<BlockTrait<?>, Comparable<?>> traits;

    private VoxelShape collisionShape;
    private VoxelShape outlineShape;

    private Map<BlockTrait<?>, BlockState[]> blockStates;

    public BlockState(BlockType type, Map<BlockTrait<?>, Comparable<?>> traits) {
        this.type = type;
        this.traits = traits;
    }

    public BlockType getType() {
        return type;
    }

    public Map<BlockTrait<?>, Comparable<?>> getTraits() {
        return traits;
    }

    /**
     * Shape used for entity and placement collision.
     */
    public VoxelShape getCollisionShape() {
        checkState(this.collisionShape != null, "Block state data has not been initialized");
        return this.collisionShape;
    }

    /**
     * Shape used for block selection/outline.
     */
    public VoxelShape getOutlineShape() {
        checkState(this.outlineShape != null, "Block state data has not been initialized");
        return this.outlineShape;
    }

    /**
     * Populates the per-state shape data. Called at most once per state by
     * {@code BlockPalette} during startup using data from {@code BlockPropertyData.BY_STATE_HASH}.
     */
    public void initStateData(VoxelShape collisionShape, VoxelShape outlineShape) {
        this.collisionShape = checkNotNull(collisionShape, "collisionShape");
        this.outlineShape = outlineShape == null ? collisionShape : outlineShape;
    }

    public boolean hasTag(BlockTag tag) {
        return BlockTags.hasTag(this.type, tag);
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

    @SuppressWarnings("unchecked")
    public <T extends Comparable<T>> T ensureTrait(BlockTrait<T> trait) {
        checkNotNull(trait, "trait");
        T val = (T) this.traits.get(trait);
        checkNotNull(val, "Trait '%s' does not exist for type '%s'", trait, this.type);
        return val;
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

    private ImmutableMap<BlockTrait<?>, Comparable<?>> getTraitsWithValue(BlockTrait<?> trait, Comparable<?> comparable) {
        ImmutableMap.Builder<BlockTrait<?>, Comparable<?>> builder = ImmutableMap.builder();
        this.traits.forEach((k, v) -> builder.put(k, k == trait ? comparable : v));
        return builder.build();
    }
}
