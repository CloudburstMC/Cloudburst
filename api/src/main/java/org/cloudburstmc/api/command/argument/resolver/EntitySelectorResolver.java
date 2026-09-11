package org.cloudburstmc.api.command.argument.resolver;

import org.cloudburstmc.api.entity.Entity;

import java.util.List;

/**
 * Resolves an entity selector argument against a command source.
 *
 * @see org.cloudburstmc.api.command.argument.CommandArgumentTypes#entity()
 * @see org.cloudburstmc.api.command.argument.CommandArgumentTypes#entities()
 */
public interface EntitySelectorResolver extends SelectorArgumentResolver<List<Entity>> {
}
