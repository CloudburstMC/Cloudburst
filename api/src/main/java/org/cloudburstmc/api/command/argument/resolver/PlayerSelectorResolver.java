package org.cloudburstmc.api.command.argument.resolver;

import org.cloudburstmc.api.player.Player;

import java.util.List;

/**
 * Resolves a player selector argument against a command source.
 *
 * @see org.cloudburstmc.api.command.argument.CommandArgumentTypes#player()
 * @see org.cloudburstmc.api.command.argument.CommandArgumentTypes#players()
 */
public interface PlayerSelectorResolver extends SelectorArgumentResolver<List<Player>> {
}
