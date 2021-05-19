package org.cloudburstmc.api.block;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.trait.BlockTrait;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemType;
import org.cloudburstmc.api.item.TierType;
import org.cloudburstmc.api.item.ToolType;
import org.cloudburstmc.api.util.AxisAlignedBB;
import org.cloudburstmc.api.util.Identifier;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public final class BlockType implements ItemType {

    private final Identifier id;
    private final Set<BlockTrait<?>> traits;
    private final List<BlockState> states;
    private final BlockState defaultState;

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

    public Identifier getId() {
        return id;
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

    public static BlockType of(Identifier id, BlockTrait<?>... traits) {
        checkNotNull(id, "id");
        checkNotNull(traits, "traits");

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
    public boolean isBlock() {
        return true;
    }

    @Override
    public boolean isPlaceable() {
        return false;
    }

    public boolean blocksMotion() {
        return true;
    }

    public boolean blocksWater() {
        return true;
    }

    public boolean isFloodable() {
        return false;
    }

    public boolean isReplaceable() {
        return false;
    }

    public boolean isTransparent() {
        return false;
    }

    public int getTranslucency() {
        return 0;
    }

    public int getFilterLevel() {
        return 0;
    }

    public boolean isSolid() {
        return true;
    }

    public boolean isDiggable() {
        return false;
    }

    public int getBurnChance() {
        return 0;
    }

    public int getBurnAbility() {
        return 0;
    }

    public float getHardness() {
        return 0f;
    }

    public float getFriction() {
        return 0f;
    }

    public float getResistance() {
        return 0f;
    }

    @Nullable
    @Override
    public BlockType getBlock() {
        return null;
    }

    @Nullable
    @Override
    public Class<?> getMetadataClass() {
        return null;
    }

    @Override
    public int getMaximumStackSize() {
        return 64;
    }

    @Override
    public ItemStack createItem(int amount, Object... metadata) {
        return null; // TODO - Need to inject an Item or Block Registry? Or make ItemStack not an interface so we can create a new instance?
    }

    @Nullable
    @Override
    public ToolType getToolType() {
        return null;
    }

    @Nullable
    @Override
    public TierType getTierType() {
        return null;
    }

    public AxisAlignedBB getBoundingBox() {
        return  null;
        //TODO
    }

    // Move these to BlockBehavior instead?
    public boolean isPowerSource() {
        return false;
    }

    public boolean canBeSilkTouched() {
        return true;
    }

    public boolean waterlogsSource() {
        return false;
    }

    public boolean breaksFlowing() {
        return false;
    }

    @Override
    public String toString() {
        return getId().toString();
    }
}
