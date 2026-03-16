package org.cloudburstmc.server.command.defaults;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.api.event.player.PlayerTeleportEvent;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.math.GenericMath;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.command.CommandParamType;
import org.cloudburstmc.server.command.Command;
import org.cloudburstmc.server.command.CommandUtils;
import org.cloudburstmc.server.command.data.CommandData;
import org.cloudburstmc.server.command.data.CommandParameter;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.Arrays;
import java.util.Optional;

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
                sender.sendMessage(Component.translatable("commands.locate.fail.noplayer"));
                return true;
            }
            if (args.length == 1) {
                target = (CommandSender) sender.getServer().getPlayer(args[0].replace("@s", sender.getName()));
                if (target == null) {
                    sender.sendMessage(Component.text("Can't find player " + args[0]).color(NamedTextColor.RED));
                    return true;
                }
            }
        } else {
            target = (CommandSender) sender.getServer().getPlayer(args[0].replace("@s", sender.getName()));
            if (target == null) {
                sender.sendMessage(Component.text("Can't find player " + args[0]).color(NamedTextColor.RED));
                return true;
            }
            if (args.length == 2) {
                origin = target;
                target = (CommandSender) sender.getServer().getPlayer(args[1].replace("@s", sender.getName()));
                if (target == null) {
                    sender.sendMessage(Component.text("Can't find player " + args[1]).color(NamedTextColor.RED));
                    return true;
                }
            }
        }
        if (args.length < 3) {
            ((CloudPlayer) origin).teleport(((CloudPlayer) target).getLocation(), PlayerTeleportEvent.TeleportCause.COMMAND);
            CommandUtils.broadcastCommandMessage(sender, Component.translatable("commands.tp.success", Component.text(origin.getName()), Component.text(target.getName())));
            if (origin != sender) {
                origin.sendMessage(Component.translatable("commands.tp.successVictim", Component.text(target.getName())));
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
            if (optional.isEmpty()) {
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
            CommandUtils.broadcastCommandMessage(sender, Component.translatable("commands.tp.success.coordinates",
                    Component.text(target.getName()),
                    Component.text(String.valueOf(GenericMath.round(position.getX(), 2))),
                    Component.text(String.valueOf(GenericMath.round(position.getY(), 2))),
                    Component.text(String.valueOf(GenericMath.round(position.getZ(), 2)))));
            if (target != sender) {
                target.sendMessage(Component.translatable("commands.tp.successVictim", Component.text(position.toString())));
            }
            return true;
        }
        return false;
    }
}
