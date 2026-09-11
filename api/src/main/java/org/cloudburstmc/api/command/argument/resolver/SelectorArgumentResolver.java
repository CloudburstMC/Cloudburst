package org.cloudburstmc.api.command.argument.resolver;

/**
 * Resolves a parsed selector argument against a command source.
 *
 * @param <T> the resolved selector result type
 */
public interface SelectorArgumentResolver<T> extends ArgumentResolver<T> {
}
