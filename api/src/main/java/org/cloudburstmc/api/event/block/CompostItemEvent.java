package org.cloudburstmc.api.event.block;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.item.ItemStack;

/**
 * Called before an item is consumed by a composter.
 */
public class CompostItemEvent extends BlockEvent {

    private final ItemStack item;
    private int levelsToRaise;

    public CompostItemEvent(Block composter, ItemStack item, int levelsToRaise) {
        super(composter);
        this.item = item;
        this.setLevelsToRaise(levelsToRaise);
    }

    /**
     * Returns the item being composted.
     */
    public ItemStack getItem() {
        return this.item;
    }

    /**
     * Returns how many layers the attempt would add. Zero is a failed attempt.
     */
    public int getLevelsToRaise() {
        return this.levelsToRaise;
    }

    /**
     * Sets the number of layers to add. Zero still consumes the item.
     * The composter cannot be filled past its maturation stage.
     *
     * @param levelsToRaise the nonnegative number of layers to add
     */
    public void setLevelsToRaise(int levelsToRaise) {
        if (levelsToRaise < 0) {
            throw new IllegalArgumentException("Levels to raise must be nonnegative");
        }

        this.levelsToRaise = levelsToRaise;
    }
}
