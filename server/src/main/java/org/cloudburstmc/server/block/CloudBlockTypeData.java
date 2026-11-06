package org.cloudburstmc.server.block;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.data.*;
import org.cloudburstmc.api.block.data.Comparator;
import org.cloudburstmc.api.block.trait.BlockTrait;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.data.*;

import java.util.*;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

/**
 * Typed views of doors, beds, redstone devices and stored block contents.
 */
@UtilityClass
public class CloudBlockTypeData {

    private static final int BOOKSHELF_SLOTS = 6;

    private static final Map<Direction, CardinalDirection> CARDINAL_FACES = Map.of(
            Direction.NORTH, CardinalDirection.NORTH,
            Direction.SOUTH, CardinalDirection.SOUTH,
            Direction.WEST, CardinalDirection.WEST,
            Direction.EAST, CardinalDirection.EAST);

    private static final Map<Class<? extends BlockData>, ViewDefinition<?>> VIEWS = indexViews(List.of(
            view(Door.class, DoorData::new, BlockTraits.CARDINAL_DIRECTION, BlockTraits.IS_DOOR_HINGE, BlockTraits.IS_OPEN, BlockTraits.IS_UPPER_BLOCK),
            view(TrapDoor.class, TrapDoorData::new, BlockTraits.DIRECTION, BlockTraits.IS_OPEN, BlockTraits.IS_UPSIDE_DOWN),
            view(Bed.class, BedData::new, BlockTraits.DIRECTION, BlockTraits.IS_HEAD_PIECE, BlockTraits.IS_OCCUPIED),
            view(Repeater.class, RepeaterData::new, BlockTraits.CARDINAL_DIRECTION, BlockTraits.REPEATER_DELAY),
            view(Comparator.class, ComparatorData::new, BlockTraits.CARDINAL_DIRECTION, BlockTraits.IS_OUTPUT_LIT, BlockTraits.IS_OUTPUT_SUBTRACT),
            view(Farmland.class, FarmlandData::new, BlockTraits.MOISTURIZED_AMOUNT),
            view(Cake.class, CakeData::new, BlockTraits.BITE_COUNTER),
            view(Beehive.class, BeehiveData::new, BlockTraits.DIRECTION, BlockTraits.HONEY_LEVEL),
            view(ChiseledBookshelf.class, BookshelfData::new, BlockTraits.DIRECTION, BlockTraits.BOOKS_STORED))
    );

    /**
     * @param state the state to view
     * @param dataType the requested block-specific interface
     * @param <D> the view type
     * @return the supported view, or empty when unavailable
     */
    public static <D extends BlockData> Optional<D> get(BlockState state, Class<D> dataType) {
        ViewDefinition<?> definition = VIEWS.get(dataType);
        return definition == null ? Optional.empty() : definition.create(state).map(dataType::cast);
    }

    private static <D extends BlockData> ViewDefinition<D> view(Class<D> type, Function<BlockState, D> factory, BlockTrait<?>... traits) {
        return new ViewDefinition<>(type, Set.of(traits), factory);
    }

    private static Map<Class<? extends BlockData>, ViewDefinition<?>> indexViews(List<ViewDefinition<?>> definitions) {
        Map<Class<? extends BlockData>, ViewDefinition<?>> views = new HashMap<>();
        for (ViewDefinition<?> definition : definitions) {
            checkArgument(views.putIfAbsent(definition.type(), definition) == null, "Duplicate block data view: %s", definition.type());
        }

        return Map.copyOf(views);
    }

    private record ViewDefinition<D extends BlockData>(Class<D> type, Set<BlockTrait<?>> traits, Function<BlockState, D> factory) {

        private Optional<D> create(BlockState state) {
            if (!state.getTraits().keySet().containsAll(this.traits)) {
                return Optional.empty();
            }

            if (Powerable.class.isAssignableFrom(this.type) && state.getData(Powerable.class).isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(this.factory.apply(state));
        }
    }

    private interface CardinalFacingView extends Directional {

        @Override
        public default Direction getFacing() {
            return this.getState().ensureTrait(BlockTraits.CARDINAL_DIRECTION).toDirection();
        }

        @Override
        public default Set<Direction> getFaces() {
            return CARDINAL_FACES.keySet();
        }

        @Override
        public default BlockState withFacing(Direction facing) {
            CardinalDirection value = CARDINAL_FACES.get(requireNonNull(facing, "facing"));
            checkArgument(value != null, "Unsupported facing: %s", facing);
            return this.getState().withTrait(BlockTraits.CARDINAL_DIRECTION, value);
        }
    }

    private interface HorizontalFacingView extends Directional {

        @Override
        public default Direction getFacing() {
            return this.getState().ensureTrait(BlockTraits.DIRECTION);
        }

        @Override
        public default Set<Direction> getFaces() {
            return CARDINAL_FACES.keySet();
        }

        @Override
        public default BlockState withFacing(Direction facing) {
            checkArgument(CARDINAL_FACES.containsKey(requireNonNull(facing, "facing")), "Unsupported facing: %s", facing);
            return this.getState().withTrait(BlockTraits.DIRECTION, facing);
        }
    }

    private record DoorData(BlockState state) implements Door, CardinalFacingView {

        @Override
        public BlockState getState() {
            return this.state;
        }

        @Override
        public DoorHinge getHinge() {
            return this.state.ensureTrait(BlockTraits.IS_DOOR_HINGE) ? DoorHinge.RIGHT : DoorHinge.LEFT;
        }

        @Override
        public BlockState withHinge(DoorHinge hinge) {
            return this.state.withTrait(BlockTraits.IS_DOOR_HINGE, requireNonNull(hinge, "hinge") == DoorHinge.RIGHT);
        }

        @Override
        public BlockHalf getHalf() {
            return this.state.ensureTrait(BlockTraits.IS_UPPER_BLOCK) ? BlockHalf.TOP : BlockHalf.BOTTOM;
        }

        @Override
        public BlockState withHalf(BlockHalf half) {
            return this.state.withTrait(BlockTraits.IS_UPPER_BLOCK, requireNonNull(half, "half") == BlockHalf.TOP);
        }

        @Override
        public boolean isOpen() {
            return this.state.ensureTrait(BlockTraits.IS_OPEN);
        }

        @Override
        public BlockState withOpen(boolean open) {
            return this.state.withTrait(BlockTraits.IS_OPEN, open);
        }
    }

    private record TrapDoorData(BlockState state) implements TrapDoor, HorizontalFacingView {

        @Override
        public BlockState getState() {
            return this.state;
        }

        @Override
        public BlockHalf getHalf() {
            return this.state.ensureTrait(BlockTraits.IS_UPSIDE_DOWN) ? BlockHalf.TOP : BlockHalf.BOTTOM;
        }

        @Override
        public BlockState withHalf(BlockHalf half) {
            return this.state.withTrait(BlockTraits.IS_UPSIDE_DOWN, requireNonNull(half, "half") == BlockHalf.TOP);
        }

        @Override
        public boolean isOpen() {
            return this.state.ensureTrait(BlockTraits.IS_OPEN);
        }

        @Override
        public BlockState withOpen(boolean open) {
            return this.state.withTrait(BlockTraits.IS_OPEN, open);
        }
    }

    private record BedData(BlockState state) implements Bed, HorizontalFacingView {

        @Override
        public BlockState getState() {
            return this.state;
        }

        @Override
        public BedPart getPart() {
            return this.state.ensureTrait(BlockTraits.IS_HEAD_PIECE) ? BedPart.HEAD : BedPart.FOOT;
        }

        @Override
        public BlockState withPart(BedPart part) {
            return this.state.withTrait(BlockTraits.IS_HEAD_PIECE, requireNonNull(part, "part") == BedPart.HEAD);
        }

        @Override
        public boolean isOccupied() {
            return this.state.ensureTrait(BlockTraits.IS_OCCUPIED);
        }

        @Override
        public BlockState withOccupied(boolean occupied) {
            return this.state.withTrait(BlockTraits.IS_OCCUPIED, occupied);
        }
    }

    private record RepeaterData(BlockState state) implements Repeater, CardinalFacingView {

        @Override
        public BlockState getState() {
            return this.state;
        }

        @Override
        public int getDelay() {
            return this.state.ensureTrait(BlockTraits.REPEATER_DELAY) + 1;
        }

        @Override
        public int getMinimumDelay() {
            return BlockTraits.REPEATER_DELAY.getRange().getStart() + 1;
        }

        @Override
        public int getMaximumDelay() {
            return BlockTraits.REPEATER_DELAY.getRange().getEnd() + 1;
        }

        @Override
        public BlockState withDelay(int delay) {
            checkArgument(delay >= this.getMinimumDelay() && delay <= this.getMaximumDelay(), "Unsupported repeater delay: %s", delay);
            return this.state.withTrait(BlockTraits.REPEATER_DELAY, delay - 1);
        }

        @Override
        public boolean isPowered() {
            return this.state.getData(Powerable.class).orElseThrow().isPowered();
        }

        @Override
        public BlockState withPowered(boolean powered) {
            return this.state.getData(Powerable.class).orElseThrow().withPowered(powered);
        }
    }

    private record ComparatorData(BlockState state) implements Comparator, CardinalFacingView {

        @Override
        public BlockState getState() {
            return this.state;
        }

        @Override
        public ComparatorMode getMode() {
            return this.state.ensureTrait(BlockTraits.IS_OUTPUT_SUBTRACT) ? ComparatorMode.SUBTRACT : ComparatorMode.COMPARE;
        }

        @Override
        public BlockState withMode(ComparatorMode mode) {
            return this.state.withTrait(BlockTraits.IS_OUTPUT_SUBTRACT, requireNonNull(mode, "mode") == ComparatorMode.SUBTRACT);
        }

        @Override
        public boolean isPowered() {
            return this.state.getData(Powerable.class).orElseThrow().isPowered();
        }

        @Override
        public BlockState withPowered(boolean powered) {
            return this.state.getData(Powerable.class).orElseThrow().withPowered(powered);
        }
    }

    private record FarmlandData(BlockState state) implements Farmland {

        @Override
        public BlockState getState() {
            return this.state;
        }

        @Override
        public int getMoisture() {
            return this.state.ensureTrait(BlockTraits.MOISTURIZED_AMOUNT);
        }

        @Override
        public int getMaximumMoisture() {
            return BlockTraits.MOISTURIZED_AMOUNT.getRange().getEnd();
        }

        @Override
        public BlockState withMoisture(int moisture) {
            return this.state.withTrait(BlockTraits.MOISTURIZED_AMOUNT, moisture);
        }
    }

    private record CakeData(BlockState state) implements Cake {

        @Override
        public BlockState getState() {
            return this.state;
        }

        @Override
        public int getBites() {
            return this.state.ensureTrait(BlockTraits.BITE_COUNTER);
        }

        @Override
        public int getMaximumBites() {
            return BlockTraits.BITE_COUNTER.getRange().getEnd();
        }

        @Override
        public BlockState withBites(int bites) {
            return this.state.withTrait(BlockTraits.BITE_COUNTER, bites);
        }
    }

    private record BeehiveData(BlockState state) implements Beehive, HorizontalFacingView {

        @Override
        public BlockState getState() {
            return this.state;
        }

        @Override
        public int getHoneyLevel() {
            return this.state.ensureTrait(BlockTraits.HONEY_LEVEL);
        }

        @Override
        public int getMaximumHoneyLevel() {
            return BlockTraits.HONEY_LEVEL.getRange().getEnd();
        }

        @Override
        public BlockState withHoneyLevel(int honeyLevel) {
            return this.state.withTrait(BlockTraits.HONEY_LEVEL, honeyLevel);
        }
    }

    private record BookshelfData(BlockState state) implements ChiseledBookshelf, HorizontalFacingView {

        @Override
        public BlockState getState() {
            return this.state;
        }

        @Override
        public int getSlotCount() {
            return BOOKSHELF_SLOTS;
        }

        @Override
        public boolean isSlotOccupied(int slot) {
            return (this.state.ensureTrait(BlockTraits.BOOKS_STORED) & slotBit(slot)) != 0;
        }

        @Override
        public Set<Integer> getOccupiedSlots() {
            Set<Integer> slots = new HashSet<>();
            for (int slot = 0; slot < this.getSlotCount(); slot++) {
                if (this.isSlotOccupied(slot)) {
                    slots.add(slot);
                }
            }

            return Set.copyOf(slots);
        }

        @Override
        public BlockState withSlotOccupied(int slot, boolean occupied) {
            int bit = slotBit(slot);
            int books = this.state.ensureTrait(BlockTraits.BOOKS_STORED);
            return this.state.withTrait(BlockTraits.BOOKS_STORED, occupied ? books | bit : books & ~bit);
        }

        private static int slotBit(int slot) {
            checkArgument(slot >= 0 && slot < BOOKSHELF_SLOTS, "Unsupported bookshelf slot: %s", slot);
            return 1 << slot;
        }
    }
}
