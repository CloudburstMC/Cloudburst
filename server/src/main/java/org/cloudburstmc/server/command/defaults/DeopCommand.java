package org.cloudburstmc.server.command.defaults;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.api.command.CommandSourceStack;
import org.cloudburstmc.api.command.Commands;
import org.cloudburstmc.api.command.argument.CommandArguments;
import org.cloudburstmc.api.command.argument.CommandArgumentTypes;
import org.cloudburstmc.api.player.OfflinePlayer;
import org.cloudburstmc.server.command.CommandUtils;
import org.cloudburstmc.server.command.AdvertisedCommand;
import org.cloudburstmc.server.command.network.CommandNetworkData;

public class DeopCommand extends AdvertisedCommand {
    public DeopCommand() {
        super("deop", "commands.deop.description", CommandNetworkData.ADMIN_NOT_CHEAT,
                "cloudburst.command.op.take");
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder, String label, CommandArguments arguments) {
        builder.then(Commands.argument("player", CommandArgumentTypes.string())
                .executes(this::executeCommand));
    }

    @Override
    protected int execute(CommandContext<CommandSourceStack> context) {
        CommandSender sender = sender(context);
        String playerName = argumentValue(context, "player");
        OfflinePlayer player = sender.getServer().getOfflinePlayer(playerName);
        player.setOp(false);

        player.getPlayer().ifPresent(onlinePlayer -> onlinePlayer.sendMessage(
                Component.translatable("commands.deop.message").color(NamedTextColor.GRAY)));

        CommandUtils.broadcastCommandMessage(sender, Component.translatable("commands.deop.success",
                Component.text(CommandUtils.displayName(player, playerName))));

        return success();
    }
}
