package org.cloudburstmc.server.command.defaults;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import org.cloudburstmc.api.command.CommandSourceStack;
import org.cloudburstmc.api.command.argument.CommandArguments;
import org.cloudburstmc.server.command.AdvertisedCommand;
import org.cloudburstmc.server.diagnostics.spark.CloudSpark;
import org.cloudburstmc.server.diagnostics.spark.SparkCommandTree;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SparkCommand extends AdvertisedCommand {

    private final CloudSpark spark;

    public SparkCommand(CloudSpark spark) {
        super("spark", "Server performance profiling", List.of(), spark.permissions());
        this.spark = spark;
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder, String label, CommandArguments arguments) {
        SparkCommandTree.configure(builder, this.spark.commands(), this::executeCommand, this::suggest);
    }

    @Override
    protected int execute(CommandContext<CommandSourceStack> context) {
        String input = SparkCommandTree.arguments(context);
        String[] arguments = input.isEmpty() ? new String[0] : input.split(" ");
        this.spark.executeCommand(sender(context), arguments);
        return success();
    }

    private CompletableFuture<Suggestions> suggest(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        String arguments = SparkCommandTree.arguments(context);
        SuggestionsBuilder token = builder.createOffset(builder.getInput().lastIndexOf(' ') + 1);
        this.spark.complete(sender(context), arguments.split(" ", -1)).forEach(token::suggest);
        return token.buildFuture();
    }
}
