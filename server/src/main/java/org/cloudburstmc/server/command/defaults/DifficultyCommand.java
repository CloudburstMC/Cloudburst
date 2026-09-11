package org.cloudburstmc.server.command.defaults;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.kyori.adventure.text.Component;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.api.command.CommandSourceStack;
import org.cloudburstmc.api.command.Commands;
import org.cloudburstmc.api.command.argument.CommandArguments;
import org.cloudburstmc.api.command.argument.CommandArgumentTypes;
import org.cloudburstmc.api.level.Difficulty;
import org.cloudburstmc.protocol.bedrock.packet.SetDifficultyPacket;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.command.CommandUtils;
import org.cloudburstmc.server.command.AdvertisedCommand;
import org.cloudburstmc.server.command.network.CommandNetworkData;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.Set;

public class DifficultyCommand extends AdvertisedCommand {

    public DifficultyCommand() {
        super("difficulty", "commands.difficulty.description", CommandNetworkData.GAME_DIRECTORS,
                "cloudburst.command.difficulty");
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder, String label, CommandArguments arguments) {
        builder.then(Commands.argument("difficulty", CommandArgumentTypes.integer(0, 3))
                .executes(this::executeCommand));
        builder.then(Commands.argument("difficultyName", CommandArgumentTypes.fixedEnumNamed("difficulty", "Difficulty",
                        "peaceful", "p", "easy", "e", "normal", "n", "hard", "h"))
                .executes(this::executeCommand));
    }

    @Override
    protected int execute(CommandContext<CommandSourceStack> context) {
        CommandSender sender = sender(context);
        String difficultyValue = hasArgument(context, "difficulty")
                ? String.valueOf(argumentValue(context, "difficulty", Integer.class))
                : argumentValue(context, "difficultyName");
        Difficulty difficulty = Difficulty.fromString(difficultyValue);

        if (((CloudServer) sender.getServer()).isHardcore()) {
            difficulty = Difficulty.HARD;
        }

        if (difficulty != null) {
            ((CloudServer) sender.getServer()).getConfig().setDifficulty(difficulty);

            SetDifficultyPacket packet = new SetDifficultyPacket();
            packet.setDifficulty(sender.getServer().getDifficulty().ordinal());
            CloudServer.broadcastPacket((Set<CloudPlayer>) ((CloudServer) sender.getServer()).getOnlinePlayers().values(), packet);

            CommandUtils.broadcastCommandMessage(sender, Component.translatable("commands.difficulty.success", Component.translatable(difficulty)));
        } else {
            return usage();
        }

        return success();
    }
}
