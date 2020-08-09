package org.cloudburstmc.server.command.defaults;

import com.nukkitx.protocol.bedrock.data.command.CommandParamType;
import org.cloudburstmc.server.command.Command;
import org.cloudburstmc.server.command.CommandSender;
import org.cloudburstmc.server.command.CommandUtils;
import org.cloudburstmc.server.command.data.CommandData;
import org.cloudburstmc.server.command.data.CommandParameter;
import org.cloudburstmc.server.locale.TranslationContainer;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class PardonCommand extends Command {

    public PardonCommand() {
        super("pardon", CommandData.builder("pardon")
                .setUsageMessage("/unban <player>")
                .setPermissions("nukkit.command.unban.player")
                .setAliases("unban")
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

        if (args.length != 1) {
            return false;
        }

        sender.getServer().getNameBans().remove(args[0]);

        CommandUtils.broadcastCommandMessage(sender, new TranslationContainer("%commands.unban.success", args[0]));

        return true;
    }
}
