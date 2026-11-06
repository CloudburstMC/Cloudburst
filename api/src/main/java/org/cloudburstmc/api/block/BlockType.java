package org.cloudburstmc.api.block;

import org.cloudburstmc.api.block.trait.BlockTrait;
import org.cloudburstmc.api.item.ItemType;
import org.cloudburstmc.api.util.Identifier;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * A kind of block and its possible states.
 * Create unregistered types through
 * {@link org.cloudburstmc.api.registry.BlockRegistry#createType(Identifier, BlockTrait[])}.
 */
public interface BlockType {

    /**
     * @return the block identifier
     */
    Identifier getId();

    /**
     * @param tag the block tag to test
     * @return whether this type belongs to the tag
     */
    boolean is(BlockTagKey tag);

    /**
     * @return the immutable tags assigned to this type
     */
    Set<BlockTagKey> getTags();

    /**
     * @return the immutable set of traits supported by this type
     */
    Set<BlockTrait<?>> getTraits();

    /**
     * @return the hardness of this type's default state
     */
    float getHardness();

    /**
     * @return the explosion resistance of this type's default state
     */
    float getExplosionResistance();

    /**
     * @return the friction of this type's default state
     */
    float getFriction();

    /**
     * @return the translucency of this type's default state
     */
    float getTranslucency();

    /**
     * @return the thickness of this type's default state
     */
    float getThickness();

    /**
     * @return whether this type's default state is solid
     */
    boolean isSolid();

    /**
     * @return the burn odds of this type's default state
     */
    int getBurnOdds();

    /**
     * @return the flame odds of this type's default state
     */
    int getFlameOdds();

    /**
     * @return whether this type's default state requires the correct tool for drops
     */
    boolean requiresCorrectToolForDrops();

    /**
     * @return whether states of this type represent liquid
     */
    boolean isLiquid();

    /**
     * @return the immutable list of possible states for this type
     */
    List<BlockState> getStates();

    /**
     * @return the state using each trait's default value
     */
    BlockState getDefaultState();

    /**
     * Returns the item associated with this block, if it has an item representation.
     * Several block types may share one item, and their identifiers need not match.
     *
     * @return the associated item, or empty when this block has no item representation
     */
    Optional<ItemType> asItem();
}
