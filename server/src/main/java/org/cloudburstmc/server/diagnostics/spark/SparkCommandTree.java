package org.cloudburstmc.server.diagnostics.spark;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import lombok.experimental.UtilityClass;
import me.lucko.spark.common.command.Command;
import org.cloudburstmc.api.command.CommandSourceStack;
import org.cloudburstmc.api.command.Commands;
import org.cloudburstmc.api.command.argument.CommandArgumentTypes;
import org.cloudburstmc.server.command.ServerOnlyArgumentType;

import java.util.Collection;
import java.util.List;

/**
 * Advertises command and action choices while leaving flag parsing and live completion to Spark.
 */
@UtilityClass
public class SparkCommandTree {

    /**
     * Adds permission-filtered commands and action choices with free-text flag arguments.
     * Execution and live completion are delegated to the supplied handlers.
     */
    public static void configure(LiteralArgumentBuilder<CommandSourceStack> root,
                                 Collection<Command> commands,
                                 com.mojang.brigadier.Command<CommandSourceStack> executor,
                                 SuggestionProvider<CommandSourceStack> suggestions) {
        for (Command command : commands) {
            List<String> actions = actions(command);
            for (String alias : command.aliases()) {
                LiteralArgumentBuilder<CommandSourceStack> branch = Commands.literal(alias)
                        .requires(source -> source.sender().hasPermission("spark")
                                || source.sender().hasPermission("spark." + command.primaryAlias()))
                        .executes(executor);

                if (!actions.isEmpty()) {
                    branch.then(Commands.argument("action", CommandArgumentTypes.fixedEnumNamed(
                                            "action", command.primaryAlias() + "Actions",
                                            actions.toArray(String[]::new)))
                            .executes(executor)
                            .suggests(suggestions)
                            .then(remainder(executor, suggestions)));
                }

                branch.then(remainder(executor, suggestions));
                root.then(branch);
            }
        }

        root.then(Commands.literal("help")
                .requires(source -> source.sender().hasPermission("spark") || commands.stream()
                        .anyMatch(command -> source.sender().hasPermission("spark." + command.primaryAlias())))
                .executes(executor));

        root.executes(executor)
                .then(RequiredArgumentBuilder.<CommandSourceStack, String>argument("args",
                                new ServerOnlyArgumentType<>(CommandArgumentTypes.text()))
                        .executes(executor)
                        .suggests(suggestions));
    }

    public static String arguments(CommandContext<CommandSourceStack> context) {
        int start = context.getNodes().getFirst().getRange().getEnd();
        return context.getInput().substring(start).stripLeading();
    }

    private static List<String> actions(Command command) {
        if (!command.allowSubCommand()) {
            return List.of();
        }

        return command.arguments().stream()
                .map(Command.ArgumentInfo::subCommandName)
                .filter(name -> !name.isBlank())
                .distinct()
                .toList();
    }

    private static RequiredArgumentBuilder<CommandSourceStack, String> remainder(
            com.mojang.brigadier.Command<CommandSourceStack> executor,
            SuggestionProvider<CommandSourceStack> suggestions) {
        return Commands.argument("args", CommandArgumentTypes.text())
                .executes(executor)
                .suggests(suggestions);
    }
}
