package org.cloudburstmc.server.command.defaults;

import org.cloudburstmc.server.command.Command;
import org.cloudburstmc.server.command.CommandSender;
import org.cloudburstmc.server.command.CommandUtils;
import org.cloudburstmc.server.command.data.CommandData;
import org.cloudburstmc.server.world.World;
import org.cloudburstmc.server.locale.TranslationContainer;
import org.cloudburstmc.server.player.Player;

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

        for (Player player : sender.getServer().getOnlinePlayers().values()) {
            player.save();
        }

        for (World world : sender.getServer().getWorlds()) {
            world.save(true);
        }

        CommandUtils.broadcastCommandMessage(sender, new TranslationContainer("%commands.save.success"));
        return true;
    }
}
