package org.cloudburstmc.server.block;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import org.cloudburstmc.server.block.behavior.BlockBehavior;
import org.cloudburstmc.server.block.trait.BlockTrait;
import org.cloudburstmc.server.block.trait.BooleanBlockTrait;
import org.cloudburstmc.server.block.trait.IntegerBlockTrait;
import org.cloudburstmc.server.registry.BlockRegistry;
import org.cloudburstmc.server.utils.Identifier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;
import java.util.Map.Entry;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

@ParametersAreNonnullByDefault
public final class CloudBlockState implements BlockState {

    private final Identifier id;
    private final BlockType type;
    private final ImmutableMap<BlockTrait<?>, Comparable<?>> traits;
    private final Reference2IntMap<BlockTrait<?>> traitPalette;
//    private final ImmutableList<NbtMap> tags;
    private CloudBlockState[][] table = null;
    private BlockState defaultState;

    CloudBlockState(Identifier id, BlockType type, ImmutableMap<BlockTrait<?>, Comparable<?>> traits,
                    Reference2IntMap<BlockTrait<?>> traitPalette/*, ImmutableList<NbtMap> tags*/) {
        Preconditions.checkNotNull(id, "id");
        Preconditions.checkNotNull(type, "type");
        this.id = id;
        this.type = type;
        this.traits = traits;
        this.traitPalette = traitPalette;
//        this.tags = tags;
    }

    @Nonnull
    @Override
    public Identifier getId() {
        return id;
    }

    @Nonnull
    @Override
    public BlockType getType() {
        return type;
    }

//    public ImmutableCollection<NbtMap> getVanillaTags() {
//        return tags;
//    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends Comparable<T>> T getTrait(BlockTrait<T> trait) {
        return (T) this.traits.get(trait);
    }

    @Nonnull
    @Override
    public <T extends Comparable<T>> T ensureTrait(BlockTrait<T> trait) {
        return checkNotNull(this.getTrait(trait), "trait does not exist for specific block");
    }

    @Nonnull
    @Override
    public ImmutableMap<BlockTrait<?>, Comparable<?>> getTraits() {
        return this.traits;
    }

    @Nonnull
    @Override
    public <T extends Comparable<T>> CloudBlockState withTrait(BlockTrait<T> trait, T value) {
        checkNotNull(trait, "trait");
        int traitIndex = this.traitPalette.getInt(trait);
        return this.table[traitIndex][trait.getIndex(value)];
    }

    @Nonnull
    @Override
    public CloudBlockState withTrait(IntegerBlockTrait trait, int value) {
        checkNotNull(trait, "trait");
        int traitIndex = this.traitPalette.getInt(trait);
        return this.table[traitIndex][trait.getIndex(value)];
    }

    @Nonnull
    @Override
    public CloudBlockState withTrait(BooleanBlockTrait trait, boolean value) {
        checkNotNull(trait, "trait");
        int traitIndex = this.traitPalette.getInt(trait);
        return this.table[traitIndex][trait.getIndex(value)];
    }

    @SuppressWarnings("rawtypes")
    @Nonnull
    @Override
    public BlockState copyTraits(BlockState from) {
        BlockState result = this;

        //TODO: direct access?
        for (Entry<BlockTrait<?>, Comparable<?>> entry : from.getTraits().entrySet()) {
            result = result.withTrait((BlockTrait) entry.getKey(), (Comparable) entry.getValue());
        }

        return result;
    }

    @Override
    public BlockBehavior getBehavior() {
        return BlockRegistry.get().getBehavior(this.type);
    }

    @Nonnull
    @Override
    public BlockState defaultState() {
        return defaultState;
    }

    public boolean isInitialized() {
        return this.table != null;
    }

    public void initialize(BlockState defaultState, Map<Map<BlockTrait<?>, Comparable<?>>, CloudBlockState> map) {
        checkState(this.table == null, "BlockTrait table has already been built");
        this.defaultState = defaultState;
        this.table = new CloudBlockState[this.traitPalette.size()][];

        for (Map.Entry<BlockTrait<?>, Comparable<?>> entry : this.traits.entrySet()) {
            BlockTrait<?> trait = entry.getKey();
            int traitIndex = this.traitPalette.getInt(trait);
            this.table[traitIndex] = new CloudBlockState[trait.getPossibleValues().size()];

            for (Comparable<?> comparable : trait.getPossibleValues()) {
                this.table[traitIndex][trait.getIndex(comparable)] = map.get(this.getTraitsWithValue(trait, comparable));
            }
        }
    }

    private ImmutableMap<BlockTrait<?>, Comparable<?>> getTraitsWithValue(BlockTrait<?> trait, Comparable<?> comparable) {
        ImmutableMap.Builder<BlockTrait<?>, Comparable<?>> builder = ImmutableMap.builder();
        this.traits.forEach((k, v) -> builder.put(k, k == trait ? comparable : v)); //this actually performs better than using a loop
        return builder.build();
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
}
