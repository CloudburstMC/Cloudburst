package org.cloudburstmc.server.command.defaults;

import com.nukkitx.protocol.bedrock.data.command.CommandParamType;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.server.command.Command;
import org.cloudburstmc.server.command.CommandUtils;
import org.cloudburstmc.server.command.data.CommandData;
import org.cloudburstmc.server.command.data.CommandParameter;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.locale.TranslationContainer;
import org.cloudburstmc.server.math.NukkitMath;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.utils.TextFormat;

/**
 * Created on 2015/12/13 by xtypr.
 * Package cn.nukkit.command.defaults in project Nukkit .
 */
public class SpawnpointCommand extends Command {
    public SpawnpointCommand() {
        super("spawnpoint", CommandData.builder("spawnpoint")
                .setDescription("commands.spawnpoint.description")
                .setUsageMessage("/spawnpoint [player] <position>")
                .setPermissions("cloudburst.command.spawnpoint")
                .setParameters(new CommandParameter[]{
                        new CommandParameter("blockPos", CommandParamType.POSITION, true),
                }, new CommandParameter[]{
                        new CommandParameter("target", CommandParamType.TARGET, false),
                        new CommandParameter("pos", CommandParamType.POSITION, true)
                })
                .build());
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!this.testPermission(sender)) {
            return true;
        }
        CloudPlayer target;
        if (args.length == 0) {
            if (sender instanceof CloudPlayer) {
                target = (CloudPlayer) sender;
            } else {
                sender.sendMessage(new TranslationContainer("commands.locate.fail.noplayer"));
                return true;
            }
        } else {
            target = sender.getServer().getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(new TranslationContainer(TextFormat.RED + "%commands.generic.player.notFound"));
                return true;
            }
        }
        CloudLevel level = target.getLevel();

        if (args.length == 4) {
            if (level != null) {
                int x;
                int y;
                int z;
                try {
                    x = Integer.parseInt(args[1]);
                    y = Integer.parseInt(args[2]);
                    z = Integer.parseInt(args[3]);
                } catch (NumberFormatException e1) {
                    return false;
                }
                if (y < 0) y = 0;
                if (y > 256) y = 256;
                target.setSpawn(Location.from(x, y, z, level));
                CommandUtils.broadcastCommandMessage(sender, new TranslationContainer("%commands.spawnpoint.success.single", target.getName(),
                        x, y, z));
                return true;
            }
        } else if (args.length <= 1) {
            if (sender instanceof CloudPlayer) {
                Location pos = ((CloudPlayer) sender).getLocation();
                target.setSpawn(pos);
                CommandUtils.broadcastCommandMessage(sender, new TranslationContainer("%commands.spawnpoint.success.single", target.getName(),
                        NukkitMath.round(pos.getX(), 2),
                        NukkitMath.round(pos.getY(), 2),
                        NukkitMath.round(pos.getZ(), 2)));
                return true;
            } else {
                sender.sendMessage(new TranslationContainer("commands.locate.fail.noplayer"));
                return true;
            }
        }
        return false;
    }
}
