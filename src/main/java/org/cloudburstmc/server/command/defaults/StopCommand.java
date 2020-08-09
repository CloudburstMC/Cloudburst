package org.cloudburstmc.server.command.defaults;

import org.cloudburstmc.server.command.Command;
import org.cloudburstmc.server.command.CommandSender;
import org.cloudburstmc.server.command.CommandUtils;
import org.cloudburstmc.server.command.data.CommandData;
import org.cloudburstmc.server.locale.TranslationContainer;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class StopCommand extends Command {

    public StopCommand() {
        super("stop", CommandData.builder("stop")
                .setDescription("commands.stop.description")
                .setPermissions("nukkit.command.stop")
                .build());
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!this.testPermission(sender)) {
            return true;
        }

        CommandUtils.broadcastCommandMessage(sender, new TranslationContainer("%commands.stop.start"));

        sender.getServer().shutdown();

        return true;
    }
}
