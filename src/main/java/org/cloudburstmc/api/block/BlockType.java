package org.cloudburstmc.api.block;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.cloudburstmc.api.block.trait.BlockTrait;
import org.cloudburstmc.api.item.ItemKeys;
import org.cloudburstmc.api.item.ItemType;
import org.cloudburstmc.api.util.Identifier;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public final class BlockType extends ItemType {

    private final Set<BlockTrait<?>> traits;
    private final List<BlockState> states;
    private final BlockState defaultState;

    private BlockType(Identifier id, BlockTrait<?>[] traits) {
        super(id, Collections.singleton(ItemKeys.BLOCK_STATE));
        this.traits = ImmutableSet.copyOf(traits);
        this.states = getPermutations(this, traits);

        Map<Map<BlockTrait<?>, Comparable<?>>, BlockState> blockStateMap = new HashMap<>();
        for (BlockState state : this.states) {
            blockStateMap.put(state.getTraits(), state);
        }

        Map<BlockTrait<?>, Comparable<?>> key = Arrays.stream(traits)
                .collect(Collectors.toMap(t -> t, BlockTrait::getDefaultValue));
        this.defaultState = blockStateMap.get(key);

        for (BlockState state : this.states) {
            state.initialize(blockStateMap);
        }
    }

    public Set<BlockTrait<?>> getTraits() {
        return traits;
    }

    public List<BlockState> getStates() {
        return states;
    }

    public BlockState getDefaultState() {
        return defaultState;
    }

    public void forEachPermutation(Consumer<BlockState> action) {
        this.states.forEach(action);
    }

    private static final BlockTrait<?>[] EMPTY = new BlockTrait[0];

    public static BlockType of(Identifier id) {
        return of(id, EMPTY);
    }

    public static BlockType of(Identifier id, BlockTrait<?>... traits) {
        checkNotNull(id, "id");
        checkNotNull(traits, "traits");

        if (traits == EMPTY) {
            return new BlockType(id, EMPTY);
        }

        // Check for duplicate block traits.
        LinkedHashSet<BlockTrait<?>> traitSet = new LinkedHashSet<>();
        Collections.addAll(traitSet, traits);
        BlockTrait<?>[] cleanedTraits = traitSet.toArray(new BlockTrait[traits.length]);
        checkArgument(Arrays.equals(traits, cleanedTraits), "%s defines duplicate block traits", id);

        return new BlockType(id, cleanedTraits);
    }

    private static List<BlockState> getPermutations(BlockType type, BlockTrait<?>[] traits) {
        if (traits.length == 0) {
            return Collections.singletonList(new BlockState(type, Collections.emptyMap()));
        }

        ImmutableList.Builder<BlockState> states = ImmutableList.builder();
        int size = traits.length;

        // to keep track of next element in each of
        // the n arrays
        int[] indices = new int[size];

        // initialize with first element's index
        Arrays.fill(indices, 0);

        while (true) {
            // Generate BlockState
            ImmutableMap.Builder<BlockTrait<?>, Comparable<?>> values = ImmutableMap.builder();
            for (int i = 0; i < size; i++) {
                BlockTrait<?> trait = traits[i];

                values.put(trait, trait.getPossibleValues().get(indices[i]));
            }
            states.add(new BlockState(type, values.build()));

            // find the rightmost array that has more
            // elements left after the current element
            // in that array
            int next = size - 1;
            while (next >= 0 && (indices[next] + 1 >= traits[next].getPossibleValues().size())) {
                next--;
            }

            // no such array is found so no more
            // combinations left
            if (next < 0) break;

            // if found move to next element in that
            // array
            indices[next]++;

            // for all arrays to the right of this
            // array current index again points to
            // first element
            for (int i = next + 1; i < size; i++) {
                indices[i] = 0;
            }
        }

        return states.build();
    }

    @Override
    public String toString() {
        return getId().toString();
    }
}
