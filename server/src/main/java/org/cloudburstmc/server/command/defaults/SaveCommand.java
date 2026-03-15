package org.cloudburstmc.server.command.defaults;

import net.kyori.adventure.text.Component;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.command.Command;
import org.cloudburstmc.server.command.CommandUtils;
import org.cloudburstmc.server.command.data.CommandData;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.player.CloudPlayer;

/**
 * Created on 2015/11/13 by xtypr.
 * Package cn.nukkit.command.defaults in project Nukkit .
 */
public class SaveCommand extends Command {

    public SaveCommand() {
        super("save-all", CommandData.builder("save-all")
                .setDescription("commands.save.description")
                .setPermissions("cloudburst.command.save.perform")
                .build());
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!this.testPermission(sender)) {
            return true;
        }

        CommandUtils.broadcastCommandMessage(sender, Component.translatable("commands.save.start"));

        for (CloudPlayer player : ((CloudServer) sender.getServer()).getOnlinePlayers().values()) {
            player.save();
        }

        for (CloudLevel level : ((CloudServer) sender.getServer()).getLevels()) {
            level.save(true);
        }

        CommandUtils.broadcastCommandMessage(sender, Component.translatable("commands.save.success"));
        return true;
    }
}
