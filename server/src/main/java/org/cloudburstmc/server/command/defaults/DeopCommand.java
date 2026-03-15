package org.cloudburstmc.server.command.defaults;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.protocol.bedrock.data.command.CommandParamType;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.command.Command;
import org.cloudburstmc.server.command.CommandUtils;
import org.cloudburstmc.server.command.data.CommandData;
import org.cloudburstmc.server.command.data.CommandParameter;
import org.cloudburstmc.server.player.CloudPlayer;

public class DeopCommand extends Command {
    public DeopCommand() {
        super("deop", CommandData.builder("deop")
                .setDescription("commands.deop.description")
                .setUsageMessage("/deop <player>")
                .setPermissions("cloudburst.command.op.take")
                .setParameters(new CommandParameter[]{
                        new CommandParameter("player", CommandParamType.TARGET, false)
                })
                .build());
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!this.testPermission(sender)) {
            return true;
        }

        if (args.length == 0) {
            return false;
        }

        String playerName = args[0];
        Player player = sender.getServer().getPlayer(playerName);
        player.setOp(false);

        if (player instanceof CloudPlayer) {
            ((CloudPlayer) player).sendMessage(Component.translatable("commands.deop.message").color(NamedTextColor.GRAY));
        }

        CommandUtils.broadcastCommandMessage(sender, Component.translatable("commands.deop.success", Component.text(player.getName())));

        return true;
    }
}
