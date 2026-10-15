package org.cloudburstmc.api.event.inventory;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.inventory.InventoryScreen;
import org.cloudburstmc.api.inventory.view.SlotGroup;
import org.cloudburstmc.api.item.ItemStack;

import java.util.Objects;

/**
 * Fired before a player's inventory action is applied. Cancelling prevents the action.
 *
 * <p>Slot indices are relative to their {@link SlotGroup}. Transfers and swaps have
 * a source and destination. Other actions have no destination.</p>
 *
 * <p>{@link #setResultItem(ItemStack)} replaces the destination item without altering
 * source-slot updates or ingredient costs. Cancel the event to prevent those changes.</p>
 */
public class InventoryClickEvent extends InventoryEvent implements Cancellable {

    private final int slot;
    @NonNull
    private final SlotGroup slotGroup;
    private final ItemStack sourceItem;
    private final ItemStack cursorItem;
    private final ActionType actionType;
    private final ClickType clickType;

    private final int destinationSlot;
    @Nullable
    private final SlotGroup destinationSlotGroup;
    @Nullable
    private ItemStack resultItem;

    /**
     * @param screen               the inventory screen receiving the click
     * @param slot                 the source slot index within its group
     * @param slotGroup            the source slot group
     * @param sourceItem           the item in the source slot before the action
     * @param cursorItem           the destination item for a transfer, the selected creative item, or an empty stack
     * @param actionType           the inventory action
     * @param clickType            the kind of stack transfer
     * @param destinationSlot      the index within the destination group, or -1 when absent
     * @param destinationSlotGroup the destination group, or null when there is no destination
     * @param resultItem           the proposed destination item, or null to use the default result
     */
    protected InventoryClickEvent(InventoryScreen screen, int slot, @NonNull SlotGroup slotGroup,
                                  ItemStack sourceItem, ItemStack cursorItem, ActionType actionType,
                                  ClickType clickType,
                                  int destinationSlot, @Nullable SlotGroup destinationSlotGroup,
                                  @Nullable ItemStack resultItem) {
        super(screen);
        this.slot = slot;
        this.slotGroup = Objects.requireNonNull(slotGroup, "slotGroup");
        this.sourceItem = sourceItem;
        this.cursorItem = cursorItem;
        this.actionType = actionType;
        this.clickType = clickType;
        this.destinationSlot = destinationSlot;
        this.destinationSlotGroup = destinationSlotGroup;
        this.resultItem = resultItem;
    }

    /**
     * Returns the source slot index within {@link #getSlotGroup()}.
     *
     * @return the source slot index
     */
    public int getSlot() {
        return slot;
    }

    /**
     * Returns the group containing the source slot.
     *
     * @return the source slot group
     */
    public SlotGroup getSlotGroup() {
        return slotGroup;
    }

    /**
     * Returns the source item before the action.
     *
     * @return the source item, which may be empty
     */
    public ItemStack getSourceItem() {
        return sourceItem;
    }

    /**
     * Returns the destination item before a transfer or swap.
     * <p>
     * For creative selection, returns the item being selected. Drop and destroy actions
     * return {@link ItemStack#EMPTY}. This is not the player's held item.
     *
     * @return the item associated with the destination or creative selection
     */
    public ItemStack getCursorItem() {
        return cursorItem;
    }

    /**
     * Returns the type of action that triggered this event.
     *
     * @return the action type
     */
    public ActionType getActionType() {
        return actionType;
    }

    /**
     * Returns the kind of stack transfer, such as taking an entire stack or placing one item.
     *
     * @return the transfer kind, or {@link ClickType#UNKNOWN} when not applicable
     */
    public ClickType getClickType() {
        return clickType;
    }

    /**
     * Returns the slot index within {@link #getDestinationSlotGroup()}.
     *
     * @return the destination slot index, or -1 when there is no destination
     */
    public int getDestinationSlot() {
        return destinationSlot;
    }

    /**
     * Returns the group receiving items during a transfer or swap.
     *
     * @return the destination slot group, or null when there is no destination
     */
    @Nullable
    public SlotGroup getDestinationSlotGroup() {
        return destinationSlotGroup;
    }

    /**
     * Returns the proposed destination item, including any items already in that slot.
     * <p>
     * For swaps, this is the source item rather than a merged stack.
     *
     * @return the proposed item, or null to use the default result or when there is no destination
     */
    @Nullable
    public ItemStack getResultItem() {
        return resultItem;
    }

    /**
     * Overrides the item that will be placed into the destination slot.
     *
     * <p>An empty stack clears the destination but does not cancel source-slot changes
     * or ingredient costs. A null value restores the default result.
     * Has no effect for actions without a destination.</p>
     *
     * @param resultItem the item to place in the destination slot, or {@code null} to use the default result
     */
    public void setResultItem(@Nullable ItemStack resultItem) {
        this.resultItem = resultItem;
    }

    /**
     * Builds an event without applying an inventory action or notifying listeners.
     *
     * <p>Required fields: {@code screen}, {@code slot}, {@code slotGroup}, {@code sourceItem},
     * {@code cursorItem}, {@code actionType}, {@code clickType}.
     * Optional fields: {@code destinationSlot} (default {@code -1}), {@code destinationSlotGroup}
     * (default {@code null}), {@code resultItem} (default {@code null}).</p>
     */
    public static final class Builder {
        private InventoryScreen screen;
        private int slot = -1;
        private SlotGroup slotGroup;
        private ItemStack sourceItem;
        private ItemStack cursorItem;
        private ActionType actionType;
        private ClickType clickType;
        private int destinationSlot = -1;
        @Nullable
        private SlotGroup destinationSlotGroup;
        @Nullable
        private ItemStack resultItem;

        /**
         * @param screen the screen receiving the action
         * @return this builder
         */
        public Builder screen(@NonNull InventoryScreen screen) {
            this.screen = Objects.requireNonNull(screen, "screen");
            return this;
        }

        /**
         * @param slot the source slot index within its group
         * @return this builder
         */
        public Builder slot(int slot) {
            this.slot = slot;
            return this;
        }

        /**
         * @param slotGroup the source slot group
         * @return this builder
         */
        public Builder slotGroup(@NonNull SlotGroup slotGroup) {
            this.slotGroup = Objects.requireNonNull(slotGroup, "slotGroup");
            return this;
        }

        /**
         * @param sourceItem the source item before the action, which may be empty
         * @return this builder
         */
        public Builder sourceItem(@NonNull ItemStack sourceItem) {
            this.sourceItem = Objects.requireNonNull(sourceItem, "sourceItem");
            return this;
        }

        /**
         * @param cursorItem the destination item before the action, the selected creative item, or an empty stack
         * @return this builder
         * @see InventoryClickEvent#getCursorItem()
         */
        public Builder cursorItem(@NonNull ItemStack cursorItem) {
            this.cursorItem = Objects.requireNonNull(cursorItem, "cursorItem");
            return this;
        }

        /**
         * @param actionType the inventory action
         * @return this builder
         */
        public Builder actionType(@NonNull ActionType actionType) {
            this.actionType = Objects.requireNonNull(actionType, "actionType");
            return this;
        }

        /**
         * @param clickType the transfer kind, or {@link ClickType#UNKNOWN} when not applicable
         * @return this builder
         */
        public Builder clickType(@NonNull ClickType clickType) {
            this.clickType = Objects.requireNonNull(clickType, "clickType");
            return this;
        }

        /**
         * @param destinationSlot the index within the destination group, or -1 when absent
         * @return this builder
         */
        public Builder destinationSlot(int destinationSlot) {
            this.destinationSlot = destinationSlot;
            return this;
        }

        /**
         * @param destinationSlotGroup the destination group, or null when absent
         * @return this builder
         */
        public Builder destinationSlotGroup(@Nullable SlotGroup destinationSlotGroup) {
            this.destinationSlotGroup = destinationSlotGroup;
            return this;
        }

        /**
         * @param resultItem the proposed destination item, or null to use the default result
         * @return this builder
         * @see InventoryClickEvent#setResultItem(ItemStack)
         */
        public Builder resultItem(@Nullable ItemStack resultItem) {
            this.resultItem = resultItem;
            return this;
        }

        /**
         * @return a new event containing the supplied values
         * @throws NullPointerException if a required field has not been supplied
         */
        public InventoryClickEvent build() {
            Objects.requireNonNull(screen, "screen is required");
            Objects.requireNonNull(slotGroup, "slotGroup is required");
            Objects.requireNonNull(sourceItem, "sourceItem is required");
            Objects.requireNonNull(cursorItem, "cursorItem is required");
            Objects.requireNonNull(actionType, "actionType is required");
            Objects.requireNonNull(clickType, "clickType is required");
            return new InventoryClickEvent(screen, slot, slotGroup, sourceItem, cursorItem,
                    actionType, clickType, destinationSlot, destinationSlotGroup, resultItem);
        }
    }
}
