package org.cloudburstmc.server.command.defaults;

import net.kyori.adventure.text.Component;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.command.CommandParamType;
import org.cloudburstmc.server.command.Command;
import org.cloudburstmc.server.command.CommandUtils;
import org.cloudburstmc.server.command.data.CommandData;
import org.cloudburstmc.server.command.data.CommandParameter;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.player.CloudPlayer;

public class SetWorldSpawnCommand extends Command {
    public SetWorldSpawnCommand() {
        super("setworldspawn", CommandData.builder("setworldspawn")
                .setDescription("commands.setworldspawn.description")
                .setUsageMessage("/setworldspawn <position>")
                .setPermissions("cloudburst.command.setworldspawn")
                .setParameters(new CommandParameter[]{
                        new CommandParameter("blockPos", CommandParamType.POSITION, true)
                })
                .build());
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!this.testPermission(sender)) {
            return true;
        }
        CloudLevel level;
        Vector3f pos;
        if (args.length == 0) {
            if (sender instanceof CloudPlayer) {
                level = ((CloudPlayer) sender).getLevel();
                pos = ((CloudPlayer) sender).getPosition();
            } else {
                sender.sendMessage(Component.translatable("commands.locate.fail.noplayer"));
                return true;
            }
        } else if (args.length == 3) {
            level = (CloudLevel) sender.getServer().getDefaultLevel();
            try {
                pos = Vector3f.from(Integer.parseInt(args[0]), Integer.parseInt(args[1]), Integer.parseInt(args[2]));
            } catch (NumberFormatException e1) {
                return false;
            }
        } else {
            return false;
        }
        level.setSpawnLocation(pos);

        CommandUtils.broadcastCommandMessage(sender, Component.translatable("commands.setworldspawn.success",
                Component.text(pos.getFloorX()), Component.text(pos.getFloorY()), Component.text(pos.getFloorZ())));
        return true;
    }
}
