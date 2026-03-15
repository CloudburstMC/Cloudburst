package org.cloudburstmc.server.command.defaults;

import net.kyori.adventure.text.Component;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.server.command.Command;
import org.cloudburstmc.server.command.CommandUtils;
import org.cloudburstmc.server.command.data.CommandData;

/**
 * Created on 2015/11/13 by xtypr.
 * Package cn.nukkit.command.defaults in project Nukkit .
 */
public class SaveOffCommand extends Command {

    public SaveOffCommand() {
        super("save-off", CommandData.builder("save-off")
                .setDescription("commands.save.description")
                .setPermissions("cloudburst.command.save.disable")
                .build());
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!this.testPermission(sender)) {
            return true;
        }
        sender.getServer().setAutoSave(false);
        CommandUtils.broadcastCommandMessage(sender, Component.translatable("commands.save.disabled"));
        return true;
    }
}
