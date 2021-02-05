package org.cloudburstmc.server.command.defaults;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.command.CommandParamType;
import org.cloudburstmc.api.event.player.PlayerTeleportEvent;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.server.command.Command;
import org.cloudburstmc.server.command.CommandSender;
import org.cloudburstmc.server.command.CommandUtils;
import org.cloudburstmc.server.command.data.CommandData;
import org.cloudburstmc.server.command.data.CommandParameter;
import org.cloudburstmc.server.locale.TranslationContainer;
import org.cloudburstmc.server.math.NukkitMath;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.utils.TextFormat;

import java.util.Arrays;
import java.util.Optional;

/**
 * Created on 2015/11/12 by Pub4Game and milkice.
 * Package cn.nukkit.command.defaults in project Nukkit .
 */
public class TeleportCommand extends Command {
    public TeleportCommand() {
        super("tp", CommandData.builder("tp")
                .setDescription("commands.tp.description")
                .setUsageMessage("/tp [player] <position|target>")
                .setPermissions("cloudburst.command.teleport")
                .addParameters(new CommandParameter[]{new CommandParameter("player", CommandParamType.TARGET, false)})
                .addParameters(new CommandParameter[]{
                        new CommandParameter("player", CommandParamType.TARGET, false),
                        new CommandParameter("target", CommandParamType.TARGET, false),
                }).addParameters(new CommandParameter[]{
                        new CommandParameter("player", CommandParamType.TARGET, false),
                        new CommandParameter("position", CommandParamType.POSITION, false),
                }).addParameters(new CommandParameter[]{
                        new CommandParameter("position", CommandParamType.POSITION, false),
                }).build());
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!this.testPermission(sender)) {
            return true;
        }
        if (args.length < 1 || args.length > 6) {
            return false;
        }
        CommandSender target;
        CommandSender origin = sender;
        if (args.length == 1 || args.length == 3) {
            if (sender instanceof CloudPlayer) {
                target = sender;
            } else {
                sender.sendMessage(new TranslationContainer("commands.locate.fail.noplayer"));
                return true;
            }
            if (args.length == 1) {
                target = sender.getServer().getPlayer(args[0].replace("@s", sender.getName()));
                if (target == null) {
                    sender.sendMessage(TextFormat.RED + "Can't find player " + args[0]);
                    return true;
                }
            }
        } else {
            target = sender.getServer().getPlayer(args[0].replace("@s", sender.getName()));
            if (target == null) {
                sender.sendMessage(TextFormat.RED + "Can't find player " + args[0]);
                return true;
            }
            if (args.length == 2) {
                origin = target;
                target = sender.getServer().getPlayer(args[1].replace("@s", sender.getName()));
                if (target == null) {
                    sender.sendMessage(TextFormat.RED + "Can't find player " + args[1]);
                    return true;
                }
            }
        }
        if (args.length < 3) {
            ((CloudPlayer) origin).teleport(((CloudPlayer) target).getLocation(), PlayerTeleportEvent.TeleportCause.COMMAND);
            CommandUtils.broadcastCommandMessage(sender, new TranslationContainer("%commands.tp.success", origin.getName(), target.getName()));
            if (origin != sender) {
                origin.sendMessage(new TranslationContainer("commands.tp.successVictim", target.getName()));
            }
            return true;
        } else if (((CloudPlayer) target).getLevel() != null) {
            int pos;
            if (args.length == 4 || args.length == 6) {
                pos = 1;
            } else {
                pos = 0;
            }
            Optional<Vector3f> optional = CommandUtils.parseVector3f(Arrays.copyOfRange(args, pos, pos += 3), ((CloudPlayer) target).getPosition());
            if (!optional.isPresent()) {
                return false;
            }
            Vector3f position = optional.get();
            float yaw = ((CloudPlayer) target).getYaw();
            float pitch = ((CloudPlayer) target).getPitch();
            if (position.getY() < 0) position = Vector3f.from(position.getX(), 0, position.getZ());
            if (args.length == 6 || (args.length == 5 && pos == 3)) {
                yaw = Float.parseFloat(args[pos++]);
                pitch = Float.parseFloat(args[pos++]);
            }
            ((CloudPlayer) target).teleport(Location.from(position, yaw, pitch, ((CloudPlayer) target).getLevel()), PlayerTeleportEvent.TeleportCause.COMMAND);
            CommandUtils.broadcastCommandMessage(sender, new TranslationContainer("%commands.tp.success.coordinates",
                    target.getName(), String.valueOf(NukkitMath.round(position.getX(), 2)),
                    String.valueOf(NukkitMath.round(position.getY(), 2)),
                    String.valueOf(NukkitMath.round(position.getZ(), 2))));
            if (target != sender) {
                target.sendMessage(new TranslationContainer("commands.tp.successVictim", position.toString()));
            }
            return true;
        }
        return false;
    }
}
