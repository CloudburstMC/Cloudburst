package org.cloudburstmc.server.block;

import com.google.common.collect.ImmutableMap;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import org.cloudburstmc.server.block.trait.BlockTrait;
import org.cloudburstmc.server.block.trait.BooleanBlockTrait;
import org.cloudburstmc.server.block.trait.IntegerBlockTrait;
import org.cloudburstmc.server.utils.Identifier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

@ParametersAreNonnullByDefault
public final class CloudBlockState implements BlockState {
    private final Identifier identifier;
    private final ImmutableMap<BlockTrait<?>, Comparable<?>> traits;
    private final Reference2IntMap<BlockTrait<?>> traitPalette;
    private CloudBlockState[][] table = null;

    CloudBlockState(Identifier identifier, ImmutableMap<BlockTrait<?>, Comparable<?>> traits,
                    Reference2IntMap<BlockTrait<?>> traitPalette) {
        this.identifier = identifier;
        this.traits = traits;
        this.traitPalette = traitPalette;
    }

    @Nonnull
    @Override
    public Identifier getType() {
        return identifier;
    }

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

    public void buildStateTable(Map<Map<BlockTrait<?>, Comparable<?>>, CloudBlockState> map) {
        checkState(this.table == null, "BlockTrait table has already been built");
        this.table = new CloudBlockState[this.traitPalette.size()][];

        for (Map.Entry<BlockTrait<?>, Comparable<?>> entry : this.traits.entrySet()) {
            BlockTrait<?> trait = entry.getKey();
            int traitIndex = this.traitPalette.getInt(trait);

            for (Comparable<?> comparable : trait.getPossibleValues()) {
                if (comparable != entry.getValue()) {
                    this.table[traitIndex][trait.getIndex(comparable)] = map.get(this.getTraitsWithValue(trait, comparable));
                }
            }
        }
    }

    private ImmutableMap<BlockTrait<?>, Comparable<?>> getTraitsWithValue(BlockTrait<?> trait, Comparable<?> comparable) {
        return ImmutableMap.<BlockTrait<?>, Comparable<?>>builder().putAll(this.traits).put(trait, comparable).build();
    }
}
