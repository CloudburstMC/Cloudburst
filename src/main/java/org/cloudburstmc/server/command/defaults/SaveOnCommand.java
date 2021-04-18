package org.cloudburstmc.server.command.defaults;

import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.server.command.Command;
import org.cloudburstmc.server.command.CommandUtils;
import org.cloudburstmc.server.command.data.CommandData;
import org.cloudburstmc.server.locale.TranslationContainer;

/**
 * Created on 2015/11/13 by xtypr.
 * Package cn.nukkit.command.defaults in project Nukkit .
 */
public class SaveOnCommand extends Command {

    public SaveOnCommand() {
        super("save-on", CommandData.builder("save-on")
                .setDescription("commands.save-on.description")
                .setPermissions("cloudburst.command.save.enable")
                .build());
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!this.testPermission(sender)) {
            return true;
        }
        sender.getServer().setAutoSave(true);
        CommandUtils.broadcastCommandMessage(sender, new TranslationContainer("%commands.save.enabled"));
        return true;
    }
}
