package org.cloudburstmc.server.command.defaults;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.command.CommandParamType;
import org.cloudburstmc.server.command.Command;
import org.cloudburstmc.server.command.CommandSender;
import org.cloudburstmc.server.command.CommandUtils;
import org.cloudburstmc.server.command.data.CommandData;
import org.cloudburstmc.server.command.data.CommandParameter;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.locale.TranslationContainer;
import org.cloudburstmc.server.player.Player;

/**
 * Created on 2015/12/13 by xtypr.
 * Package cn.nukkit.command.defaults in project Nukkit .
 */
public class SetWorldSpawnCommand extends Command {
    public SetWorldSpawnCommand() {
        super("setworldspawn", CommandData.builder("setworldspawn")
                .setDescription("commands.setworldspawn.description")
                .setUsageMessage("/setworldspawn <position>")
                .setPermissions("nukkit.command.setworldspawn")
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
        Level level;
        Vector3f pos;
        if (args.length == 0) {
            if (sender instanceof Player) {
                level = ((Player) sender).getLevel();
                pos = ((Player) sender).getPosition();
            } else {
                sender.sendMessage(new TranslationContainer("commands.locate.fail.noplayer"));
                return true;
            }
        } else if (args.length == 3) {
            level = sender.getServer().getDefaultLevel();
            try {
                pos = Vector3f.from(Integer.parseInt(args[0]), Integer.parseInt(args[1]), Integer.parseInt(args[2]));
            } catch (NumberFormatException e1) {
                return false;
            }
        } else {
            return false;
        }
        level.setSpawnLocation(pos);

        CommandUtils.broadcastCommandMessage(sender, new TranslationContainer("%commands.setworldspawn.success", pos.getFloorX(),
                pos.getFloorY(), pos.getFloorZ()));
        return true;
    }
}
