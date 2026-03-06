package org.cloudburstmc.api.event.inventory;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.blockentity.BlockEntity;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.event.Event;
import org.cloudburstmc.api.inventory.view.BlockSlotGroup;
import org.cloudburstmc.api.inventory.view.SlotGroup;
import org.cloudburstmc.api.inventory.view.SlotGroupType;
import org.cloudburstmc.api.item.ItemStack;

import java.util.Optional;


/**
 * Called when items are moved between slot groups without a player's direct involvement
 * (e.g. a hopper pulling/pushing items between block entities).
 *
 * <p>This event extends {@link Event} directly rather than
 * {@link org.cloudburstmc.api.event.inventory.InventoryEvent}, because no player screen is
 * involved in an automated item transfer; there is no open
 * {@link org.cloudburstmc.api.inventory.InventoryScreen}. Events that involve a player
 * interacting with an open screen (clicks, open/close) extend
 * {@link org.cloudburstmc.api.event.inventory.InventoryEvent} instead.</p>
 */
public final class InventoryMoveItemEvent extends Event implements Cancellable {

    @Nullable
    private final SlotGroup source;
    @Nullable
    private final SlotGroup target;
    @NonNull
    private final SlotGroup initiator;
    private final Action action;
    private ItemStack item;

    private InventoryMoveItemEvent(@Nullable SlotGroup source, @Nullable SlotGroup target,
                                   @NonNull SlotGroup initiator, @NonNull ItemStack item, @NonNull Action action) {
        this.source = source;
        this.target = target;
        this.initiator = initiator;
        this.item = item;
        this.action = action;
    }

    /**
     * Creates an {@link Action#TRANSFER} event (items moving from one slot group to another).
     *
     * @param source    the slot group items are moving from (must not be {@code null})
     * @param target    the slot group items are moving into (must not be {@code null})
     * @param initiator the slot group that initiated the transfer (must not be {@code null})
     * @param item      the item stack being transferred (must not be {@code null})
     * @return a new {@code InventoryMoveItemEvent} with {@link Action#TRANSFER}
     */
    public static InventoryMoveItemEvent transfer(@NonNull SlotGroup source, @NonNull SlotGroup target,
                                                  @NonNull SlotGroup initiator, @NonNull ItemStack item) {
        java.util.Objects.requireNonNull(source, "source");
        java.util.Objects.requireNonNull(target, "target");
        java.util.Objects.requireNonNull(initiator, "initiator");
        java.util.Objects.requireNonNull(item, "item");
        return new InventoryMoveItemEvent(source, target, initiator, item, Action.TRANSFER);
    }

    /**
     * Creates an {@link Action#TRANSFER} event where one side is an item entity in the world
     * rather than a named slot group.
     *
     * <p>Use this overload when a hopper picks up items from the ground ({@code source} is
     * {@code null}) or drops items onto the ground ({@code target} is {@code null}).
     * Exactly one of {@code source} and {@code target} may be {@code null}.</p>
     *
     * @param source    the slot group items are moving from, or {@code null} for an item entity
     * @param target    the slot group items are moving into, or {@code null} for an item entity
     * @param initiator the slot group that initiated the transfer (must not be {@code null})
     * @param item      the item stack being transferred (must not be {@code null})
     * @return a new {@code InventoryMoveItemEvent} with {@link Action#TRANSFER}
     * @throws IllegalArgumentException if both {@code source} and {@code target} are {@code null}
     */
    public static InventoryMoveItemEvent transferWithWorld(@Nullable SlotGroup source, @Nullable SlotGroup target,
                                                           @NonNull SlotGroup initiator, @NonNull ItemStack item) {
        if (source == null && target == null) {
            throw new IllegalArgumentException("At least one of source or target must be non-null");
        }
        java.util.Objects.requireNonNull(initiator, "initiator");
        java.util.Objects.requireNonNull(item, "item");
        return new InventoryMoveItemEvent(source, target, initiator, item, Action.TRANSFER);
    }

    /**
     * Creates an {@link Action#DISPENSE} event (items ejected into the world with no destination).
     *
     * @param source    the slot group items are dispensed from (must not be {@code null})
     * @param initiator the slot group that initiated the dispense (must not be {@code null})
     * @param item      the item stack being dispensed (must not be {@code null})
     * @return a new {@code InventoryMoveItemEvent} with {@link Action#DISPENSE} and {@code null} target
     */
    public static InventoryMoveItemEvent dispense(@NonNull SlotGroup source, @NonNull SlotGroup initiator,
                                                  @NonNull ItemStack item) {
        java.util.Objects.requireNonNull(source, "source");
        java.util.Objects.requireNonNull(initiator, "initiator");
        java.util.Objects.requireNonNull(item, "item");
        return new InventoryMoveItemEvent(source, null, initiator, item, Action.DISPENSE);
    }

    /**
     * Returns the source slot group that items are moving from.
     *
     * <p>Returns {@code null} when items are being picked up from the world
     * (e.g. a hopper collecting a dropped item entity). In that case there is no
     * named slot group on the source side.</p>
     *
     * @return the source slot group, or {@code null} when the source is the world
     */
    @Nullable
    public SlotGroup getSource() {
        return source;
    }

    /**
     * Returns the destination slot group that items are moving into.
     *
     * <p>Returns {@code null} for {@link Action#DISPENSE} actions, where items are
     * ejected into the world and there is no receiving slot group.</p>
     *
     * @return the destination slot group, or {@code null} for {@link Action#DISPENSE}
     */
    @Nullable
    public SlotGroup getTarget() {
        return target;
    }

    /**
     * Returns the slot group that initiated the transfer.
     *
     * <p>For a hopper pushing items into a chest, the initiator is the hopper's slot group.
     * For a hopper pulling items from a chest, the initiator is still the hopper's slot group.
     * This lets plugins distinguish which side triggered the movement, even when
     * {@link #getSource()} and {@link #getTarget()} are both non-null.</p>
     *
     * @return the slot group that initiated this transfer (never {@code null})
     */
    @NonNull
    public SlotGroup getInitiator() {
        return initiator;
    }

    /**
     * Returns the {@link SlotGroupType} of the source slot group,
     * or {@code null} when the source is the world (e.g. item-entity pickup).
     *
     * @return the source slot group type, or {@code null}
     */
    @Nullable
    public SlotGroupType<? extends SlotGroup> getSourceType() {
        return source != null ? source.getSlotGroupType() : null;
    }

    /**
     * Returns the {@link SlotGroupType} of the destination slot group,
     * or {@code null} when there is no target (e.g. {@link Action#DISPENSE}).
     *
     * @return the target slot group type, or {@code null}
     */
    @Nullable
    public SlotGroupType<? extends SlotGroup> getTargetType() {
        return target != null ? target.getSlotGroupType() : null;
    }

    /**
     * Returns the {@link BlockEntity} backing the source slot group, if any.
     *
     * <p>Returns an empty {@link Optional} when the source is not a
     * {@link BlockSlotGroup} (e.g. a player inventory).</p>
     *
     * @return an optional block entity for the source
     */
    public Optional<BlockEntity> getSourceBlockEntity() {
        return source instanceof BlockSlotGroup b ? Optional.ofNullable(b.getBlockEntity()) : Optional.empty();
    }

    /**
     * Returns the {@link BlockEntity} backing the destination slot group, if any.
     *
     * <p>Returns an empty {@link Optional} when the target is not a
     * {@link BlockSlotGroup} (e.g. a player inventory).</p>
     *
     * @return an optional block entity for the target
     */
    public Optional<BlockEntity> getTargetBlockEntity() {
        return target instanceof BlockSlotGroup b ? Optional.ofNullable(b.getBlockEntity()) : Optional.empty();
    }

    /**
     * Returns the source slot group cast to {@link BlockSlotGroup}, if it is one.
     *
     * <p>Returns an empty {@link Optional} when the source is not backed by a block
     * (e.g. a player inventory).</p>
     *
     * @return an optional {@link BlockSlotGroup} for the source
     */
    public Optional<BlockSlotGroup<?>> getSourceSlotGroup() {
        return source instanceof BlockSlotGroup<?> b ? Optional.of(b) : Optional.empty();
    }

    /**
     * Returns the target slot group cast to {@link BlockSlotGroup}, if it is one.
     *
     * <p>Returns an empty {@link Optional} when the target is not backed by a block
     * (e.g. a player inventory).</p>
     *
     * @return an optional {@link BlockSlotGroup} for the target
     */
    public Optional<BlockSlotGroup<?>> getTargetSlotGroup() {
        return target instanceof BlockSlotGroup<?> b ? Optional.of(b) : Optional.empty();
    }

    /**
     * Returns the item stack being transferred.
     *
     * <p>For {@link Action#TRANSFER} this is the item moving from source to target.
     * For {@link Action#DISPENSE} this is the item being ejected into the world.</p>
     *
     * @return the item stack involved in this transfer (never {@code null})
     */
    @NonNull
    public ItemStack getItem() {
        return item;
    }

    /**
     * Replaces the item stack to be transferred.
     *
     * <p>Plugins can call this to change the item that will be moved or dispensed.
     * Setting a different amount lets plugins partially block transfers.</p>
     *
     * @param item the replacement item stack (must not be {@code null})
     */
    public void setItem(@NonNull ItemStack item) {
        this.item = item;
    }

    public Action getAction() {
        return action;
    }

    public enum Action {
        /**
         * Items are transferred between two inventories (e.g. hopper push/pull).
         * One side may be {@code null} when moving items to/from the world (item entities).
         */
        TRANSFER,
        /**
         * Items are dispensed out of a dispenser or dropper into the world.
         */
        DISPENSE
    }
}

