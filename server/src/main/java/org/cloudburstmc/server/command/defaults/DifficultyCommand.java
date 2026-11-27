package org.cloudburstmc.server.command.defaults;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.api.command.CommandSourceStack;
import org.cloudburstmc.api.command.Commands;
import org.cloudburstmc.api.command.argument.CommandArgumentTypes;
import org.cloudburstmc.api.command.argument.CommandArguments;
import org.cloudburstmc.api.level.Difficulty;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.command.AdvertisedCommand;
import org.cloudburstmc.server.command.CommandUtils;

public class DifficultyCommand extends AdvertisedCommand {

    public DifficultyCommand() {
        super("difficulty", "commands.difficulty.description", GamemodeCommand.NETWORK_DATA,
                "cloudburst.command.difficulty");
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder, String label, CommandArguments arguments) {
        builder.then(Commands.argument("difficulty", CommandArgumentTypes.fixedEnumNamed("difficulty", "Difficulty",
                        "normal", "peaceful", "easy", "hard", "p", "e", "n", "h"))
                .executes(this::executeCommand));
        builder.then(Commands.argument("difficultyValue", CommandArgumentTypes.integer("difficulty"))
                .executes(this::executeCommand));
    }

    @Override
    protected int execute(CommandContext<CommandSourceStack> context) {
        CommandSender sender = sender(context);
        Difficulty difficulty = hasArgument(context, "difficulty")
                ? difficulty(argumentValue(context, "difficulty"))
                : Difficulty.getByValue(argumentValue(context, "difficultyValue", Integer.class));
        if (difficulty == null) {
            return usage();
        }

        ((CloudServer) sender.getServer()).setDifficulty(difficulty);
        CommandUtils.broadcastCommandMessage(sender, Component.translatable("commands.difficulty.success", Component.translatable(difficulty)));
        return success();
    }

    private static @Nullable Difficulty difficulty(String input) {
        return switch (input) {
            case "peaceful", "p" -> Difficulty.PEACEFUL;
            case "easy", "e" -> Difficulty.EASY;
            case "normal", "n" -> Difficulty.NORMAL;
            case "hard", "h" -> Difficulty.HARD;
            default -> null;
        };
    }
}
