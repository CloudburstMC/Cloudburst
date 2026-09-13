package org.cloudburstmc.api.boss;

import net.kyori.adventure.text.Component;
import org.cloudburstmc.api.player.Player;

import java.util.List;

/**
 * A boss bar that can be shown to one or more players.
 */
public interface BossBar {

    /**
     * Returns this bar's title.
     *
     * @return the title
     */
    Component getTitle();

    /**
     * Sets this bar's title.
     *
     * @param title the new title
     */
    void setTitle(Component title);

    /**
     * Returns this bar's color.
     *
     * @return the color
     */
    BossBarColor getColor();

    /**
     * Sets this bar's color.
     *
     * @param color the new color
     */
    void setColor(BossBarColor color);

    /**
     * Returns this bar's style.
     *
     * @return the style
     */
    BossBarStyle getStyle();

    /**
     * Sets this bar's style.
     *
     * @param style the new style
     */
    void setStyle(BossBarStyle style);

    /**
     * Returns the filled portion of this bar.
     *
     * @return a value from {@code 0.0} to {@code 1.0}
     */
    double getProgress();

    /**
     * Sets the filled portion of this bar.
     *
     * @param progress a value from {@code 0.0} to {@code 1.0}
     * @throws IllegalArgumentException if the value is outside the supported range or is not finite
     */
    void setProgress(double progress);

    /**
     * Attaches a player to this bar.
     *
     * @param player the player to attach
     */
    void addPlayer(Player player);

    /**
     * Detaches a player from this bar.
     *
     * @param player the player to detach
     */
    void removePlayer(Player player);

    /**
     * Detaches every player from this bar.
     */
    void removeAll();

    /**
     * Returns an immutable snapshot of the players attached to this bar.
     *
     * @return the attached players
     */
    List<Player> getPlayers();

    /**
     * Returns whether this bar is displayed to attached players.
     *
     * @return whether the bar is visible
     */
    boolean isVisible();

    /**
     * Sets whether this bar is displayed to attached players.
     *
     * @param visible whether the bar is visible
     */
    void setVisible(boolean visible);
}
