package org.cloudburstmc.server.command.defaults;

import net.kyori.adventure.text.Component;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.server.command.Command;
import org.cloudburstmc.server.command.data.CommandData;
import org.cloudburstmc.server.player.CloudPlayer;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class SeedCommand extends Command {

    public SeedCommand() {
        super("seed", CommandData.builder("seed")
                .setPermissions("cloudburst.command.seed")
                .build());
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!this.testPermission(sender)) {
            return true;
        }

        long seed;
        if (sender instanceof CloudPlayer) {
            seed = ((CloudPlayer) sender).getLevel().getSeed();
        } else {
            seed = sender.getServer().getDefaultLevel().getSeed();
        }

        sender.sendMessage(Component.translatable("commands.seed.success", Component.text(seed)));

        return true;
    }
}
