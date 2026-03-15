package org.cloudburstmc.server.command.defaults;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.protocol.bedrock.data.command.CommandParamType;
import org.cloudburstmc.server.command.Command;
import org.cloudburstmc.server.command.CommandUtils;
import org.cloudburstmc.server.command.data.CommandData;
import org.cloudburstmc.server.command.data.CommandParameter;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.math.NukkitMath;
import org.cloudburstmc.server.player.CloudPlayer;

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
                sender.sendMessage(Component.translatable("commands.locate.fail.noplayer"));
                return true;
            }
        } else {
            target = (CloudPlayer) sender.getServer().getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(Component.translatable("commands.generic.player.notFound").color(NamedTextColor.RED));
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
                CommandUtils.broadcastCommandMessage(sender, Component.translatable("commands.spawnpoint.success.single",
                        Component.text(target.getName()), Component.text(x), Component.text(y), Component.text(z)));
                return true;
            }
        } else if (args.length <= 1) {
            if (sender instanceof CloudPlayer) {
                Location pos = ((CloudPlayer) sender).getLocation();
                target.setSpawn(pos);
                CommandUtils.broadcastCommandMessage(sender, Component.translatable("commands.spawnpoint.success.single",
                        Component.text(target.getName()),
                        Component.text(NukkitMath.round(pos.getX(), 2)),
                        Component.text(NukkitMath.round(pos.getY(), 2)),
                        Component.text(NukkitMath.round(pos.getZ(), 2))));
                return true;
            } else {
                sender.sendMessage(Component.translatable("commands.locate.fail.noplayer"));
                return true;
            }
        }
        return false;
    }
}
