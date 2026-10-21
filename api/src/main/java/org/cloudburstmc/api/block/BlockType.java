package org.cloudburstmc.api.block;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.cloudburstmc.api.block.trait.BlockTrait;
import org.cloudburstmc.api.item.ItemType;
import org.cloudburstmc.api.util.Identifier;

import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.*;

/**
 * A kind of block and its possible states.
 */
public final class BlockType {

    private static final BlockTrait<?>[] EMPTY = new BlockTrait[0];

    private final Identifier id;
    private final Set<BlockTrait<?>> traits;
    private final List<BlockState> states;
    private final BlockState defaultState;

    private volatile Set<BlockTagKey> tags;
    private ItemType itemType;
    private LiquidType liquidType;

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

    /**
     * Creates a block type without traits. This does not register the type.
     *
     * @param id the block identifier
     * @return the new block type
     */
    public static BlockType of(Identifier id) {
        return of(id, EMPTY);
    }

    /**
     * Creates a block type with all combinations of the supplied trait values.
     * This does not register the type.
     *
     * @param id     the block identifier
     * @param traits the supported traits
     * @return the new block type
     * @throws IllegalArgumentException if a trait is repeated
     */
    public static BlockType of(Identifier id, BlockTrait<?>... traits) {
        checkNotNull(id, "id");
        checkNotNull(traits, "traits");

        if (traits == EMPTY) {
            return new BlockType(id, EMPTY);
        }

        BlockTrait<?>[] checkedTraits = checkTraits(id, traits);
        return new BlockType(id, checkedTraits);
    }

    /**
     * @return the block identifier
     */
    public Identifier getId() {
        return id;
    }

    /**
     * @param tag the block tag to test
     * @return whether this type belongs to the tag
     */
    public boolean is(BlockTagKey tag) {
        Set<BlockTagKey> boundTags = this.tags;
        checkState(boundTags != null, "Block type tags have not been bound: %s", this.id);
        return boundTags.contains(checkNotNull(tag, "tag"));
    }

    /**
     * @return the immutable tags assigned to this type
     */
    public Set<BlockTagKey> getTags() {
        Set<BlockTagKey> boundTags = this.tags;
        checkState(boundTags != null, "Block type tags have not been bound: %s", this.id);
        return boundTags;
    }

    /**
     * @return the immutable set of traits supported by this type
     */
    public Set<BlockTrait<?>> getTraits() {
        return traits;
    }

    /**
     * @return the hardness of this type's default state
     */
    public float getHardness() {
        return this.defaultState.getHardness();
    }

    /**
     * @return the explosion resistance of this type's default state
     */
    public float getExplosionResistance() {
        return this.defaultState.getExplosionResistance();
    }

    /**
     * @return the friction of this type's default state
     */
    public float getFriction() {
        return this.defaultState.getFriction();
    }

    /**
     * @return the translucency of this type's default state
     */
    public float getTranslucency() {
        return this.defaultState.getTranslucency();
    }

    /**
     * @return the thickness of this type's default state
     */
    public float getThickness() {
        return this.defaultState.getThickness();
    }

    /**
     * @return whether this type's default state is solid
     */
    public boolean isSolid() {
        return this.defaultState.isSolid();
    }

    /**
     * @return the burn odds of this type's default state
     */
    public int getBurnOdds() {
        return this.defaultState.getBurnOdds();
    }

    /**
     * @return the flame odds of this type's default state
     */
    public int getFlameOdds() {
        return this.defaultState.getFlameOdds();
    }

    /**
     * @return whether this type's default state requires the correct tool for drops
     */
    public boolean requiresCorrectToolForDrops() {
        return this.defaultState.requiresCorrectToolForDrops();
    }

    /**
     * @return whether states of this type represent liquid
     */
    public boolean isLiquid() {
        return this.traits.contains(BlockTraits.LIQUID_DEPTH);
    }

    /**
     * @return the immutable list of possible states for this type
     */
    public List<BlockState> getStates() {
        return states;
    }

    /**
     * @return the state using each trait's default value
     */
    public BlockState getDefaultState() {
        return defaultState;
    }

    /**
     * Returns the item associated with this block, if it has an item representation.
     * Several block types may share one item, and their identifiers need not match.
     *
     * @return the associated item, or empty when this block has no item representation
     */
    public Optional<ItemType> asItem() {
        return Optional.ofNullable(itemType);
    }

    @Override
    public String toString() {
        return id.toString();
    }

    synchronized void bindTags(Set<BlockTagKey> tags) {
        checkState(this.tags == null, "Block type tags have already been bound: %s", this.id);
        this.tags = Set.copyOf(checkNotNull(tags, "tags"));
    }

    void linkItemType(ItemType type) {
        this.itemType = type;
    }

    synchronized void bindLiquidType(LiquidType liquidType) {
        checkState(this.liquidType == null, "Liquid type has already been bound: %s", this.id);
        this.liquidType = checkNotNull(liquidType, "liquidType");
    }

    LiquidType getLiquidType() {
        checkState(this.liquidType != null, "Liquid type has not been bound: %s", this.id);
        return this.liquidType;
    }

    private static BlockTrait<?>[] checkTraits(Identifier id, BlockTrait<?>[] traits) {
        LinkedHashSet<BlockTrait<?>> traitSet = new LinkedHashSet<>();
        Collections.addAll(traitSet, traits);
        BlockTrait<?>[] checkedTraits = traitSet.toArray(new BlockTrait[traits.length]);
        checkArgument(Arrays.equals(traits, checkedTraits), "%s defines duplicate block traits", id);
        return checkedTraits;
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
