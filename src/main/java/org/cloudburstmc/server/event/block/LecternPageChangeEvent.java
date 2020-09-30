package org.cloudburstmc.server.event.block;

import org.cloudburstmc.server.blockentity.Lectern;
import org.cloudburstmc.server.event.Cancellable;
import org.cloudburstmc.server.player.Player;

public class LecternPageChangeEvent extends BlockEvent implements Cancellable {

    private final Player player;
    private final Lectern lectern;
    private int newRawPage;

    public LecternPageChangeEvent(Player player, Lectern lectern, int newPage) {
        super(lectern.getBlock());
        this.player = player;
        this.lectern = lectern;
        this.newRawPage = newPage;
    }

    public Lectern getLectern() {
        return lectern;
    }

    public int getLeftPage() {
        return (newRawPage * 2) + 1;
    }

    public void setLeftPage(int newLeftPage) {
        this.newRawPage = (newLeftPage - 1) / 2;
    }

    public int getRightPage() {
        return getLeftPage() + 1;
    }

    public void setRightPage(int newRightPage) {
        this.setLeftPage(newRightPage - 1);
    }

    public int getNewRawPage() {
        return newRawPage;
    }

    public void setNewRawPage(int newRawPage) {
        this.newRawPage = newRawPage;
    }

    public int getMaxPage() {
        return lectern.getTotalPages();
    }

    public Player getPlayer() {
        return player;
    }

}