package org.cloudburstmc.api.block;

import org.cloudburstmc.api.block.data.BlockData;
import org.cloudburstmc.api.block.trait.BlockTrait;
import org.cloudburstmc.api.block.trait.BooleanBlockTrait;
import org.cloudburstmc.api.block.trait.IntegerBlockTrait;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.util.VoxelShape;
import org.cloudburstmc.api.util.data.BlockMirror;
import org.cloudburstmc.api.util.data.BlockRotation;
import org.cloudburstmc.math.vector.Vector3i;

import java.awt.*;
import java.util.Map;
import java.util.Optional;

/**
 * A block type paired with immutable trait values and physical properties.
 *
 * <p>Use {@link #getData(Class)} for named property operations or
 * {@link #getTraits()} and {@link #ensureTrait(BlockTrait)} for direct trait
 * access. Property changes return another state and do not change blocks in a level.
 */
public interface BlockState {

    /**
     * @return the block type represented by this state
     */
    BlockType getType();

    /**
     * @return the immutable trait values of this state
     */
    Map<BlockTrait<?>, Comparable<?>> getTraits();

    /**
     * Returns a typed view for common block properties, such as
     * {@link org.cloudburstmc.api.block.data.Ageable} or
     * {@link org.cloudburstmc.api.block.data.Directional}.
     *
     * @param dataType the requested view interface
     * @param <D>      the view type
     * @return the view, or empty if this state does not support it
     */
    <D extends BlockData> Optional<D> getData(Class<D> dataType);

    /**
     * @param tag the block tag to test
     * @return whether this state's type belongs to the tag
     */
    boolean is(BlockTagKey tag);

    /**
     * Returns a required trait value.
     *
     * @param trait the trait to read
     * @param <T>   the trait value type
     * @return the current value
     * @throws IllegalArgumentException if the trait is absent from this state
     */
    <T extends Comparable<T>> T ensureTrait(BlockTrait<T> trait);

    /**
     * Returns a state with one trait changed, leaving this state unchanged.
     *
     * @param trait the trait to change
     * @param value the replacement value
     * @param <T>   the trait value type
     * @return the resulting state
     * @throws IllegalArgumentException if the trait or value is not supported
     */
    <T extends Comparable<T>> BlockState withTrait(BlockTrait<T> trait, T value);

    /**
     * @param trait the integer trait to change
     * @param value the replacement value
     * @return a state with the replacement value, leaving this state unchanged
     * @throws IllegalArgumentException if the trait or value is not supported
     */
    BlockState withTrait(IntegerBlockTrait trait, int value);

    /**
     * @param trait the boolean trait to change
     * @param value the replacement value
     * @return a state with the replacement value, leaving this state unchanged
     * @throws IllegalArgumentException if the trait or value is not supported
     */
    BlockState withTrait(BooleanBlockTrait trait, boolean value);

    /**
     * Copies values for traits shared with another state. Traits absent from this
     * block type are ignored, and its other trait values are retained.
     *
     * @param from state supplying trait values
     * @return state with the shared values applied
     */
    BlockState copyTraits(BlockState from);

    /**
     * Increases a trait by one without exceeding its declared maximum.
     *
     * @param trait the integer trait to change
     * @return the resulting state, leaving this state unchanged
     * @throws IllegalArgumentException if the trait is absent from this state
     */
    BlockState incrementTrait(IntegerBlockTrait trait);

    /**
     * Decreases a trait by one without going below its declared minimum.
     *
     * @param trait the integer trait to change
     * @return the resulting state, leaving this state unchanged
     * @throws IllegalArgumentException if the trait is absent from this state
     */
    BlockState decrementTrait(IntegerBlockTrait trait);

    /**
     * @param trait the boolean trait to invert
     * @return a state with the inverted value, leaving this state unchanged
     * @throws IllegalArgumentException if the trait is absent from this state
     */
    BlockState toggleTrait(BooleanBlockTrait trait);

    /**
     * Rotates orientation and connection properties around the vertical axis.
     * Other properties and this state remain unchanged.
     *
     * @param rotation the horizontal rotation
     * @return the resulting state
     * @throws IllegalArgumentException      if this type cannot represent the transformed properties
     * @throws UnsupportedOperationException if an encoded orientation has no supported transform
     */
    BlockState rotate(BlockRotation rotation);

    /**
     * Reflects orientation, connections and handed properties across a vertical plane.
     * Other properties and this state remain unchanged.
     *
     * @param mirror the reflection
     * @return the resulting state
     * @throws IllegalArgumentException      if this type cannot represent the transformed properties
     * @throws UnsupportedOperationException if an encoded orientation has no supported transform
     */
    BlockState mirror(BlockMirror mirror);

    /**
     * Returns state-only collision geometry in block-local coordinates.
     * Use {@link Block#getCollisionShape(org.cloudburstmc.api.util.CollisionContext)}
     * for position-dependent or entity-dependent geometry.
     *
     * @return collision shape
     */
    VoxelShape getCollisionShape();

    /**
     * Returns state-only selection geometry in block-local coordinates.
     * Use {@link Block#getOutlineShape()} for the outline at a level position.
     *
     * @return outline shape
     */
    VoxelShape getOutlineShape();

    /**
     * Checks whether this state could remain at the target position, retaining
     * compatible coexisting contents. Does not place blocks or check replacement
     * rules, entity collisions or player permissions.
     *
     * @param level    the level to query
     * @param position the target block position
     * @return whether the state is supported, or false outside the build limits
     */
    boolean isSupported(Level level, Vector3i position);

    /**
     * Checks support at the block's level position using its current surroundings.
     *
     * @param block the target block
     * @return whether this state could remain at the target
     */
    default boolean isSupported(Block block) {
        return this.isSupported(block.getLevel(), block.getPosition());
    }

    /**
     * @return this state's hardness
     */
    float getHardness();

    /**
     * @return this state's explosion resistance
     */
    float getExplosionResistance();

    /**
     * @return this state's surface friction
     */
    float getFriction();

    /**
     * @return this state's translucency
     */
    float getTranslucency();

    /**
     * @return this state's thickness
     */
    float getThickness();

    /**
     * @return this state's chance of catching fire
     */
    int getBurnOdds();

    /**
     * @return this state's chance of spreading fire
     */
    int getFlameOdds();

    /**
     * @return the light removed while passing through this state
     */
    int getLightDampening();

    /**
     * @return the light emitted by this state
     */
    int getLightEmission();

    /**
     * @return whether this state is solid
     */
    boolean isSolid();

    /**
     * @return whether this state's drops require the correct tool
     */
    boolean requiresCorrectToolForDrops();

    /**
     * @return this state's map color
     */
    Color getMapColor();

    /**
     * @return whether this state can share its position with a liquid source
     */
    boolean canContainLiquidSource();

    /**
     * @return this state's reaction to flowing liquid
     */
    LiquidReaction getLiquidReaction();

    /**
     * @return whether this state can share its position with flowing liquid
     */
    boolean canContainFlowingLiquid();

    /**
     * @return whether this state may be replaced by normal block placement
     */
    boolean isReplaceable();

    /**
     * Returns the liquid represented by this state, or empty for a non-liquid block.
     * Use {@link BlockSnapshot#getLiquid()} for the combined contents of block layers.
     *
     * @return this state's liquid
     */
    LiquidState getLiquidState();
}
