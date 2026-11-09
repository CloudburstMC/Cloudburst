package org.cloudburstmc.server.command;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Marks a parsing branch that is omitted from the client command catalog.
 *
 * @param delegate argument parser and completion provider
 * @param <T>      parsed value type
 */
public record ServerOnlyArgumentType<T>(ArgumentType<T> delegate) implements ArgumentType<T> {

    public ServerOnlyArgumentType {
        Objects.requireNonNull(delegate, "delegate");
    }

    @Override
    public T parse(StringReader reader) throws CommandSyntaxException {
        return this.delegate.parse(reader);
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return this.delegate.listSuggestions(context, builder);
    }

    @Override
    public Collection<String> getExamples() {
        return this.delegate.getExamples();
    }
}
