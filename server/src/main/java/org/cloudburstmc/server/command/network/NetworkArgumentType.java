package org.cloudburstmc.server.command.network;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import org.cloudburstmc.api.command.CommandSourceStack;
import org.cloudburstmc.api.command.argument.CommandArgumentType;
import org.cloudburstmc.protocol.bedrock.data.command.CommandParam;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public record NetworkArgumentType<T>(CommandArgumentType<T> parser, Presentation presentation) implements ArgumentType<T> {

    public NetworkArgumentType {
        Objects.requireNonNull(parser, "parser");
        Objects.requireNonNull(presentation, "presentation");
    }

    public static <T> RequiredArgumentBuilder<CommandSourceStack, T> argument(String name, CommandArgumentType<T> parser, Presentation presentation) {
        return RequiredArgumentBuilder.argument(name, new NetworkArgumentType<>(parser, presentation));
    }

    @Override
    public T parse(StringReader reader) throws CommandSyntaxException {
        return this.parser.parse(reader);
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return this.parser.listSuggestions(context, builder);
    }

    @Override
    public Collection<String> getExamples() {
        return this.parser.getExamples();
    }

    public sealed interface Presentation permits NativeParameter, SoftEnumParameter {
    }

    public record NativeParameter(CommandParam type) implements Presentation {
        public NativeParameter {
            Objects.requireNonNull(type, "type");
        }
    }

    public record SoftEnumParameter(String enumName, List<String> values) implements Presentation {
        public SoftEnumParameter {
            Objects.requireNonNull(enumName, "enumName");
            values = List.copyOf(values);
        }
    }
}
