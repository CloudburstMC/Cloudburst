package org.cloudburstmc.server.command.defaults;

import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.server.command.Command;
import org.cloudburstmc.server.command.CommandUtils;
import org.cloudburstmc.server.command.data.CommandData;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.locale.TranslationContainer;
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

        CommandUtils.broadcastCommandMessage(sender, new TranslationContainer("%commands.save.start"));

        for (CloudPlayer player : sender.getServer().getOnlinePlayers().values()) {
            player.save();
        }

        for (CloudLevel level : sender.getServer().getLevels()) {
            level.save(true);
        }

        CommandUtils.broadcastCommandMessage(sender, new TranslationContainer("%commands.save.success"));
        return true;
    }
}
