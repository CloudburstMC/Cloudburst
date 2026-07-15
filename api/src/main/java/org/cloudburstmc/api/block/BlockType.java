package org.cloudburstmc.api.block;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.cloudburstmc.api.block.trait.BlockTrait;
import org.cloudburstmc.api.item.ItemType;
import org.cloudburstmc.api.util.Identifier;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.*;

public final class BlockType {

    private static final BlockTrait<?>[] EMPTY = new BlockTrait[0];

    private final Identifier id;
    private final Set<BlockTrait<?>> traits;
    private final List<BlockState> states;
    private final BlockState defaultState;

    private volatile Set<BlockTagKey> tags;
    private ItemType itemType;

    private BlockType(Identifier id, BlockTrait<?>[] traits) {
        this.id = id;
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

    public static BlockType of(Identifier id) {
        return of(id, EMPTY);
    }

    public static BlockType of(Identifier id, BlockTrait<?>... traits) {
        checkNotNull(id, "id");
        checkNotNull(traits, "traits");

        if (traits == EMPTY) {
            return new BlockType(id, EMPTY);
        }

        LinkedHashSet<BlockTrait<?>> traitSet = new LinkedHashSet<>();
        Collections.addAll(traitSet, traits);
        BlockTrait<?>[] cleanedTraits = traitSet.toArray(new BlockTrait[traits.length]);
        checkArgument(Arrays.equals(traits, cleanedTraits), "%s defines duplicate block traits", id);

        return new BlockType(id, cleanedTraits);
    }

    public Identifier getId() {
        return id;
    }

    public boolean is(BlockTagKey tag) {
        Set<BlockTagKey> boundTags = this.tags;
        checkState(boundTags != null, "Block type tags have not been bound: %s", this.id);
        return boundTags.contains(checkNotNull(tag, "tag"));
    }

    public Set<BlockTagKey> getTags() {
        Set<BlockTagKey> boundTags = this.tags;
        checkState(boundTags != null, "Block type tags have not been bound: %s", this.id);
        return boundTags;
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

    public Optional<ItemType> asItem() {
        return Optional.ofNullable(itemType);
    }

    @Override
    public String toString() {
        return id.toString();
    }

    /**
     * Assigns this type's tag membership during registration.
     * This operation may be performed once.
     *
     * @param tags tag keys assigned to this type
     * @throws IllegalStateException if tags are already bound
     */
    public synchronized void bindTags(Set<BlockTagKey> tags) {
        checkState(this.tags == null, "Block type tags have already been bound: %s", this.id);
        this.tags = Set.copyOf(checkNotNull(tags, "tags"));
    }

    public void linkItemType(ItemType type) {
        this.itemType = type;
    }

    private static List<BlockState> getPermutations(BlockType type, BlockTrait<?>[] traits) {
        if (traits.length == 0) {
            return Collections.singletonList(new BlockState(type, Collections.emptyMap()));
        }

        ImmutableList.Builder<BlockState> states = ImmutableList.builder();
        int size = traits.length;
        int[] indices = new int[size];
        Arrays.fill(indices, 0);

        while (true) {
            ImmutableMap.Builder<BlockTrait<?>, Comparable<?>> values = ImmutableMap.builder();
            for (int i = 0; i < size; i++) {
                BlockTrait<?> trait = traits[i];
                values.put(trait, trait.getPossibleValues().get(indices[i]));
            }
            states.add(new BlockState(type, values.build()));

            int next = size - 1;
            while (next >= 0 && (indices[next] + 1 >= traits[next].getPossibleValues().size())) {
                next--;
            }

            if (next < 0) break;

            indices[next]++;

            for (int i = next + 1; i < size; i++) {
                indices[i] = 0;
            }
        }

        return states.build();
    }
}
