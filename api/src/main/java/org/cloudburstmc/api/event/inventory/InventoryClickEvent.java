package org.cloudburstmc.api.event.inventory;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.inventory.InventoryScreen;
import org.cloudburstmc.api.inventory.view.SlotGroup;
import org.cloudburstmc.api.item.ItemStack;

import java.util.Objects;

/**
 * Called when a player clicks a slot in an open inventory screen.
 *
 * <p>For transfer and swap actions, {@link #getDestinationSlot()} and
 * {@link #getDestinationSlotGroup()} indicate where the item is going.
 * Plugins may override the resulting item placed in the destination slot via
 * {@link #setResultItem(ItemStack)}; the source slot reduction is unaffected.
 * For non-transfer actions (DROP, DESTROY, CRAFT_CREATIVE) the destination
 * fields are {@code -1} / {@code null} and {@code resultItem} is ignored.</p>
 *
 * <h2>Construction</h2>
 * <p>Use {@link Builder} to construct instances:</p>
 * <pre>{@code
 * InventoryClickEvent event = new InventoryClickEvent.Builder()
 *     .screen(screen)
 *     .slot(srcViewSlot)
 *     .slotGroup(srcGroup)
 *     .sourceItem(sourceItem)
 *     .cursorItem(cursorItem)
 *     .actionType(ActionType.TAKE)
 *     .clickType(ClickType.TAKE_ALL)
 *     .destinationSlot(dstViewSlot)
 *     .destinationSlotGroup(dstGroup)
 *     .resultItem(newDest)
 *     .build();
 * }</pre>
 */
public final class InventoryClickEvent extends InventoryEvent implements Cancellable {

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

    private InventoryClickEvent(InventoryScreen screen, int slot, @NonNull SlotGroup slotGroup,
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
     * Returns the slot index that was clicked within the slot group.
     *
     * @return the clicked slot index
     */
    public int getSlot() {
        return slot;
    }

    /**
     * Returns the slot group of the inventory that was clicked.
     *
     * @return the clicked slot group
     */
    public SlotGroup getSlotGroup() {
        return slotGroup;
    }

    /**
     * Returns the item that was in the clicked slot at the time of the click.
     *
     * @return the item in the clicked slot
     */
    public ItemStack getSourceItem() {
        return sourceItem;
    }

    /**
     * Returns the item the player was holding on their cursor at the time of the click.
     *
     * <p>This is the item attached to the mouse pointer during inventory interaction,
     * <em>not</em> the item in the player's hand/hotbar slot. For actions that do not
     * involve a cursor item (such as DROP or DESTROY), this may be {@link ItemStack#EMPTY}.</p>
     *
     * @return the item on the player's cursor
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
     * Returns the click sub-type for this action.
     *
     * <p>While {@link #getActionType()} describes the broad category (TAKE, PLACE, SWAP, …),
     * this value describes the precise gesture (for example whether the player took
     * all items or only half, or placed the full stack, a single item, or a split amount.
     * The client sends the exact count with each {@code ItemStackRequest}, so the server
     * always knows whether it was a "take half" or "take all" click.</p>
     *
     * @return the click sub-type, never {@code null}
     */
    public ClickType getClickType() {
        return clickType;
    }

    /**
     * Returns the destination slot index for TAKE, PLACE, and SWAP actions,
     * or {@code -1} for actions that have no destination slot (DROP, DESTROY, CRAFT_CREATIVE).
     *
     * @return the destination slot index, or {@code -1}
     */
    public int getDestinationSlot() {
        return destinationSlot;
    }

    /**
     * Returns the destination {@link SlotGroup} for TAKE, PLACE, and SWAP actions,
     * or {@code null} for actions that have no destination (DROP, DESTROY, CRAFT_CREATIVE).
     *
     * <p>For {@link ActionType#TAKE} actions the item is being moved <em>to</em> the player's
     * cursor, so the destination is always the cursor slot group
     * ({@link org.cloudburstmc.api.inventory.view.SlotGroupTypes#CURSOR}).</p>
     *
     * @return the destination slot group, or {@code null}
     */
    @Nullable
    public SlotGroup getDestinationSlotGroup() {
        return destinationSlotGroup;
    }

    /**
     * Returns the item that will be placed into the destination slot after this action.
     *
     * <p>For TAKE/PLACE, this is the merged or moved item. For SWAP, this is the item
     * that will land in the destination slot. Plugins may call {@link #setResultItem(ItemStack)}
     * to override what ends up in the destination.
     * Returns {@code null} for actions with no destination (DROP, DESTROY, CRAFT_CREATIVE).</p>
     *
     * @return the result item, or {@code null}
     */
    @Nullable
    public ItemStack getResultItem() {
        return resultItem;
    }

    /**
     * Overrides the item that will be placed into the destination slot.
     *
     * <p>Has no effect for DROP, DESTROY, or CRAFT_CREATIVE actions.</p>
     *
     * @param resultItem the item to place in the destination slot, or {@code null} to use the default result
     */
    public void setResultItem(@Nullable ItemStack resultItem) {
        this.resultItem = resultItem;
    }

    /**
     * Describes the type of inventory interaction that triggered {@link InventoryClickEvent}.
     * <p>
     * These map directly to {@code ItemStackRequest} action types.
     */
    public enum ActionType {
        /**
         * Player moves some or all of a stack from one slot to another
         * (a pick-up or partial pick-up in Java terms).
         */
        TAKE,
        /**
         * Player places some or all of a held stack into a slot.
         */
        PLACE,
        /**
         * Player swaps the contents of two slots, including swapping with the
         * off-hand slot via the dedicated off-hand button.
         */
        SWAP,
        /**
         * Player drops an item out of a slot into the world.
         */
        DROP,
        /**
         * Player destroys an item (only possible in creative mode).
         */
        DESTROY,
        /**
         * Player picks a creative item from the creative inventory.
         */
        CRAFT_CREATIVE,
        /**
         * Any other or unrecognised action type.
         */
        UNKNOWN
    }

    /**
     * Sub-type of an inventory click, derived from the count sent.
     */
    public enum ClickType {
        /**
         * Player took the entire stack from a slot (count == source stack count).
         * Corresponds to a left-click on a slot when the cursor is empty.
         */
        TAKE_ALL,
        /**
         * Player took half the stack (count == source stack count / 2, rounded down).
         * This is the "pick up half" gesture.
         */
        TAKE_HALF,
        /**
         * Player took a specific count that is neither all nor half (1 &lt; count &lt; stack size).
         */
        TAKE_SOME,
        /**
         * Player placed the entire held stack into a slot (count == held item count).
         * Corresponds to a left-click on a slot when the cursor is non-empty.
         */
        PLACE_ALL,
        /**
         * Player placed a single item from the held stack into a slot (count == 1).
         * Corresponds to a right-click on a slot when the cursor is non-empty.
         */
        PLACE_ONE,
        /**
         * Player placed a specific count greater than one but less than the full held stack.
         */
        PLACE_SOME,
        /**
         * The click type could not be determined, or it does not fit any of the above
         * categories (e.g. SWAP, DROP, DESTROY, CRAFT_CREATIVE actions).
         */
        UNKNOWN
    }

    /**
     * Builder for {@link InventoryClickEvent}.
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

        public Builder screen(@NonNull InventoryScreen screen) {
            this.screen = Objects.requireNonNull(screen, "screen");
            return this;
        }

        public Builder slot(int slot) {
            this.slot = slot;
            return this;
        }

        public Builder slotGroup(@NonNull SlotGroup slotGroup) {
            this.slotGroup = Objects.requireNonNull(slotGroup, "slotGroup");
            return this;
        }

        public Builder sourceItem(@NonNull ItemStack sourceItem) {
            this.sourceItem = Objects.requireNonNull(sourceItem, "sourceItem");
            return this;
        }

        public Builder cursorItem(@NonNull ItemStack cursorItem) {
            this.cursorItem = Objects.requireNonNull(cursorItem, "cursorItem");
            return this;
        }

        public Builder actionType(@NonNull ActionType actionType) {
            this.actionType = Objects.requireNonNull(actionType, "actionType");
            return this;
        }

        public Builder clickType(@NonNull ClickType clickType) {
            this.clickType = Objects.requireNonNull(clickType, "clickType");
            return this;
        }

        public Builder destinationSlot(int destinationSlot) {
            this.destinationSlot = destinationSlot;
            return this;
        }

        public Builder destinationSlotGroup(@Nullable SlotGroup destinationSlotGroup) {
            this.destinationSlotGroup = destinationSlotGroup;
            return this;
        }

        public Builder resultItem(@Nullable ItemStack resultItem) {
            this.resultItem = resultItem;
            return this;
        }

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
