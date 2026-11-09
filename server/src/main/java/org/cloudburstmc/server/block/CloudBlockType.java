package org.cloudburstmc.server.block;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.block.trait.BlockTrait;
import org.cloudburstmc.api.item.ItemType;
import org.cloudburstmc.api.util.Identifier;

import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.*;

/**
 * Owns canonical block states and their registration bindings.
 */
public class CloudBlockType implements BlockType {

    private final Identifier id;
    private final Set<BlockTrait<?>> traits;
    private final List<BlockState> states;
    private final BlockState defaultState;

    private volatile Set<BlockTagKey> tags;
    private volatile ItemType itemType;
    private volatile LiquidType liquidType;
    private volatile @Nullable SlabFamily slabFamily;

    public CloudBlockType(Identifier id, BlockTrait<?>... traits) {
        checkNotNull(id, "id");
        checkNotNull(traits, "traits");
        traits = checkTraits(id, traits);
        this.id = id;
        this.traits = ImmutableSet.copyOf(traits);
        this.states = getPermutations(this, traits);

        Map<Map<BlockTrait<?>, Comparable<?>>, BlockState> blockStateMap = new HashMap<>();
        for (BlockState state : this.states) {
            blockStateMap.put(state.getTraits(), state);
        }

        Map<BlockTrait<?>, Comparable<?>> key = Arrays.stream(traits)
                .collect(Collectors.toMap(t -> t, BlockTrait::getDefaultValue));
        BlockState defaultState = blockStateMap.get(key);
        checkState(defaultState != null, "No default state exists for type '%s'", this.id);
        this.defaultState = defaultState;

        for (BlockState state : this.states) {
            CloudBlockState.require(state).initialize(blockStateMap);
        }
    }

    /**
     * @param type the type to check
     * @return the server-owned type
     * @throws IllegalArgumentException if the type is not owned by this server
     */
    public static CloudBlockType require(BlockType type) {
        if (type instanceof CloudBlockType cloudType) {
            return cloudType;
        }

        throw new IllegalArgumentException("Block type is not owned by this server: " + type);
    }

    /**
     * @param tags the complete tag membership to capture
     * @throws IllegalStateException if tags are already bound
     */
    public synchronized void bindTags(Set<BlockTagKey> tags) {
        checkState(this.tags == null, "Block type tags have already been bound: %s", this.id);
        this.tags = Set.copyOf(checkNotNull(tags, "tags"));
    }

    /**
     * Assigns the item representation during block-item registration.
     *
     * @param type the associated item type
     */
    public void linkItemType(ItemType type) {
        this.itemType = type;
    }

    /**
     * @param liquidType the liquid represented by this block type
     * @throws IllegalStateException if a liquid type is already bound
     */
    public synchronized void bindLiquidType(LiquidType liquidType) {
        checkState(this.liquidType == null, "Liquid type has already been bound: %s", this.id);
        this.liquidType = checkNotNull(liquidType, "liquidType");
    }

    /**
     * @return the bound liquid type
     * @throws IllegalStateException if no liquid type has been bound
     */
    public LiquidType getLiquidType() {
        checkState(this.liquidType != null, "Liquid type has not been bound: %s", this.id);
        return this.liquidType;
    }

    /**
     * @param family the single and double slab types sharing this material
     * @throws IllegalArgumentException if this type does not belong to the family
     * @throws IllegalStateException    if a slab family is already bound
     */
    public synchronized void bindSlabFamily(SlabFamily family) {
        checkState(this.slabFamily == null, "Slab family has already been bound: %s", this.id);
        checkNotNull(family, "family");
        checkArgument(family.single() == this || family.doubled() == this, "Type is not part of the slab family");
        this.slabFamily = family;
    }

    /**
     * @return the bound slab family, or {@code null} when none is assigned
     */
    public @Nullable SlabFamily getSlabFamily() {
        return this.slabFamily;
    }

    @Override
    public Identifier getId() {
        return id;
    }

    @Override
    public boolean is(BlockTagKey tag) {
        Set<BlockTagKey> boundTags = this.tags;
        checkState(boundTags != null, "Block type tags have not been bound: %s", this.id);
        return boundTags.contains(checkNotNull(tag, "tag"));
    }

    @Override
    public Set<BlockTagKey> getTags() {
        Set<BlockTagKey> boundTags = this.tags;
        checkState(boundTags != null, "Block type tags have not been bound: %s", this.id);
        return boundTags;
    }

    @Override
    public Set<BlockTrait<?>> getTraits() {
        return traits;
    }

    @Override
    public float getHardness() {
        return this.defaultState.getHardness();
    }

    @Override
    public float getExplosionResistance() {
        return this.defaultState.getExplosionResistance();
    }

    @Override
    public float getFriction() {
        return this.defaultState.getFriction();
    }

    @Override
    public float getTranslucency() {
        return this.defaultState.getTranslucency();
    }

    @Override
    public float getThickness() {
        return this.defaultState.getThickness();
    }

    @Override
    public boolean isSolid() {
        return this.defaultState.isSolid();
    }

    @Override
    public int getBurnOdds() {
        return this.defaultState.getBurnOdds();
    }

    @Override
    public int getFlameOdds() {
        return this.defaultState.getFlameOdds();
    }

    @Override
    public boolean requiresCorrectToolForDrops() {
        return this.defaultState.requiresCorrectToolForDrops();
    }

    @Override
    public boolean isLiquid() {
        return this.traits.contains(BlockTraits.LIQUID_DEPTH);
    }

    @Override
    public List<BlockState> getStates() {
        return states;
    }

    @Override
    public BlockState getDefaultState() {
        return defaultState;
    }

    @Override
    public Optional<ItemType> asItem() {
        return Optional.ofNullable(itemType);
    }

    @Override
    public String toString() {
        return id.toString();
    }

    private static BlockTrait<?>[] checkTraits(Identifier id, BlockTrait<?>[] traits) {
        Set<Identifier> names = new HashSet<>();
        long combinations = 1;
        for (BlockTrait<?> trait : traits) {
            checkNotNull(trait, "trait");
            checkArgument(names.add(trait.getName()), "%s defines duplicate block trait names: %s", id, trait.getName());
            int values = trait.getPossibleValues().size();
            checkArgument(combinations <= Integer.MAX_VALUE / values, "%s defines too many block states", id);
            combinations *= values;
        }
        return traits.clone();
    }

    private static List<BlockState> getPermutations(CloudBlockType type, BlockTrait<?>[] traits) {
        if (traits.length == 0) {
            return Collections.singletonList(new CloudBlockState(type, Collections.emptyMap()));
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
            states.add(new CloudBlockState(type, values.build()));

            int next = size - 1;
            while (next >= 0 && (indices[next] + 1 >= traits[next].getPossibleValues().size())) {
                next--;
            }

            if (next < 0) {
                break;
            }

            indices[next]++;

            for (int i = next + 1; i < size; i++) {
                indices[i] = 0;
            }
        }

        return states.build();
    }
}
