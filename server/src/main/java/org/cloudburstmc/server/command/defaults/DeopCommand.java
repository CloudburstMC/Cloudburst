package org.cloudburstmc.server.command.defaults;

import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.protocol.bedrock.data.command.CommandParamType;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.command.Command;
import org.cloudburstmc.server.command.CommandUtils;
import org.cloudburstmc.server.command.data.CommandData;
import org.cloudburstmc.server.command.data.CommandParameter;
import org.cloudburstmc.server.locale.TranslationContainer;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.utils.TextFormat;

/**
 * Created on 2015/11/12 by xtypr.
 * Package cn.nukkit.command.defaults in project Nukkit .
 */
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
        Player player = ((CloudServer) sender.getServer()).getPlayer(playerName);
        player.setOp(false);

        if (player instanceof CloudPlayer) {
            ((CloudPlayer) player).sendMessage(new TranslationContainer(TextFormat.GRAY + "%commands.deop.message"));
        }

        CommandUtils.broadcastCommandMessage(sender, new TranslationContainer("%commands.deop.success", player.getName()));

        return true;
    }
}
