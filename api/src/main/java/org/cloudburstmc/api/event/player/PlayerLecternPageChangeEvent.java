package org.cloudburstmc.api.event.player;

import org.cloudburstmc.api.blockentity.Lectern;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.inventory.LecternScreen;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.player.Player;

/**
 * Called before a player changes the displayed spread on a lectern.
 * The displayed spread is shared by all readers. Cancelling leaves it unchanged.
 */
public class PlayerLecternPageChangeEvent extends PlayerEvent implements Cancellable {

    private final LecternScreen screen;
    private final Lectern lectern;
    private final ItemStack book;
    private final int oldPage;
    private int newPage;

    /**
     * Creates a page change event using the lectern's current book and spread.
     *
     * @param player  the player requesting the change
     * @param screen  the screen from which the change was requested
     * @param lectern the lectern being read
     * @param newPage the proposed zero-based spread index
     */
    public PlayerLecternPageChangeEvent(Player player, LecternScreen screen, Lectern lectern, int newPage) {
        super(player);
        this.screen = screen;
        this.lectern = lectern;
        this.book = lectern.getBook();
        this.oldPage = lectern.getPage();
        this.newPage = newPage;
    }

    /**
     * Returns the screen from which the player requested the change.
     */
    public LecternScreen getScreen() {
        return this.screen;
    }

    /**
     * Returns the lectern whose displayed spread may change.
     */
    public Lectern getLectern() {
        return this.lectern;
    }

    /**
     * Returns the book on the lectern when the page change was requested.
     */
    public ItemStack getBook() {
        return this.book;
    }

    /**
     * Returns the previous zero-based spread index.
     */
    public int getOldPage() {
        return this.oldPage;
    }

    /**
     * Returns the proposed zero-based spread index.
     */
    public int getNewPage() {
        return this.newPage;
    }

    /**
     * Sets the proposed zero-based spread index, clamped to the available spreads.
     *
     * @param page the proposed spread index
     */
    public void setNewPage(int page) {
        this.newPage = page;
    }
}
