package org.cloudburstmc.api.block;

import com.google.common.collect.ImmutableMap;
import org.cloudburstmc.api.block.behavior.BlockBehavior;
import org.cloudburstmc.api.block.trait.BlockTrait;
import org.cloudburstmc.api.block.trait.BooleanBlockTrait;
import org.cloudburstmc.api.block.trait.IntegerBlockTrait;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public class BlockState {

    private final BlockType type;
    private final Map<BlockTrait<?>, Comparable<?>> traits;
    private Map<BlockTrait<?>, BlockState[]> blockStates;
    private BlockBehavior behavior;

    public BlockState(BlockType type, Map<BlockTrait<?>, Comparable<?>> traits) {
        this(type, traits, null);
    }

    public BlockState(BlockType type, Map<BlockTrait<?>, Comparable<?>> traits, BlockBehavior behavior) {
        this.type = type;
        this.traits = traits;
        this.behavior = behavior;
    }

    public void setBehavior(BlockBehavior behavior) {
        this.behavior = behavior;
    }

    public BlockType getType() {
        return type;
    }

    public Map<BlockTrait<?>, Comparable<?>> getTraits() {
        return traits;
    }

    @Nonnull
    public <T extends Comparable<T>> BlockState withTrait(BlockTrait<T> trait, T value) {
        checkNotNull(trait, "trait");
        return this.blockStates.get(trait)[trait.getIndex(value)];
    }

    @Nonnull
    public BlockState withTrait(IntegerBlockTrait trait, int value) {
        checkNotNull(trait, "trait");
        return this.blockStates.get(trait)[trait.getIndex(value)];
    }

    @Nonnull
    public BlockState withTrait(BooleanBlockTrait trait, boolean value) {
        checkNotNull(trait, "trait");
        return this.blockStates.get(trait)[trait.getIndex(value)];
    }

    public <T extends Comparable<T>> T ensureTrait(BlockTrait<T> trait) {
        checkNotNull(trait, "trait");
        return (T) this.traits.get(trait);
    }

    @Nonnull
    @SuppressWarnings({"rawtypes", "unchecked"})
    public BlockState copyTraits(BlockState from) {
        BlockState result = this;

        //TODO: direct access?
        for (Map.Entry<BlockTrait<?>, Comparable<?>> entry : from.getTraits().entrySet()) {
            result = result.withTrait((BlockTrait) entry.getKey(), (Comparable) entry.getValue());
        }

        return result;
    }

    @Nonnull
    public BlockState incrementTrait(IntegerBlockTrait trait) {
        checkNotNull(trait, "trait");
        return withTrait(trait, Math.min(trait.getRange().getEnd(), ensureTrait(trait) + 1));
    }

    @Nonnull
    public BlockState decrementTrait(IntegerBlockTrait trait) {
        checkNotNull(trait, "trait");
        return withTrait(trait, Math.max(trait.getRange().getStart(), ensureTrait(trait) - 1));
    }

    public boolean inCategory(BlockCategory category) {
        return BlockCategories.inCategory(this.getType(), category);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(this.type);
        if (!this.traits.isEmpty()) {
            builder.append('{');
            this.traits.forEach((trait, value) -> builder.append(trait).append('=').append(value.toString().toLowerCase()).append(',').append(' '));
            builder.setLength(builder.length() - 1);
            builder.setCharAt(builder.length() - 1, '}');
        }
        return builder.toString();
    }

    public BlockBehavior getBehavior() {
        return this.behavior;
    }

    public BlockState toggleTrait(BooleanBlockTrait trait) {
        return this.blockStates.get(trait)[trait.getIndex(!((Boolean) this.traits.get(trait)))];
    }

    //------------------------      INTERNAL      ------------------------

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
        this.traits.forEach((k, v) -> builder.put(k, k == trait ? comparable : v)); //this actually performs better than using a loop
        return builder.build();
    }
}
