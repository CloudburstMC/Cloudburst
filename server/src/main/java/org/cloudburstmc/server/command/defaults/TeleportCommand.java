package org.cloudburstmc.server.command.defaults;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kyori.adventure.text.Component;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.api.command.CommandSourceStack;
import org.cloudburstmc.api.command.Commands;
import org.cloudburstmc.api.command.argument.CommandArguments;
import org.cloudburstmc.api.command.argument.CommandArgumentTypes;
import org.cloudburstmc.api.command.argument.resolver.PositionResolver;
import org.cloudburstmc.api.command.argument.resolver.RotationResolver;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.event.player.PlayerTeleportEvent;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.math.GenericMath;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.server.command.CommandUtils;
import org.cloudburstmc.server.command.AdvertisedCommand;
import org.cloudburstmc.server.command.network.CommandNetworkData;

import java.util.List;

public class TeleportCommand extends AdvertisedCommand {
    public TeleportCommand() {
        super("teleport", "commands.tp.description", List.of("tp"), CommandNetworkData.GAME_DIRECTORS,
                "cloudburst.command.teleport");
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder, String label, CommandArguments arguments) {
        builder.then(Commands.argument("player", arguments.entities())
                .executes(this::executeCommand)
                .then(Commands.argument("target", arguments.entity())
                        .executes(this::executeCommand))
                .then(Commands.argument("playerPosition", CommandArgumentTypes.position("position"))
                        .executes(this::executeCommand)
                        .then(Commands.argument("playerYaw", CommandArgumentTypes.rotation("yaw"))
                                .then(Commands.argument("playerPitch", CommandArgumentTypes.rotation("pitch"))
                                        .executes(this::executeCommand)))));
        builder.then(Commands.argument("position", CommandArgumentTypes.position())
                .executes(this::executeCommand)
                .then(Commands.argument("yaw", CommandArgumentTypes.rotation())
                        .then(Commands.argument("pitch", CommandArgumentTypes.rotation())
                                .executes(this::executeCommand))));
    }

    @Override
    protected int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSender sender = sender(context);
        if (hasArgument(context, "target")) {
            List<Entity> origins = CommandArgumentTypes.entities(context, "player");
            Entity target = CommandArgumentTypes.entity(context, "target");
            if (origins.isEmpty()) {
                return success();
            }

            for (Entity origin : origins) {
                teleportToEntity(sender, origin, target);
            }
            return success();
        }

        if (hasArgument(context, "playerPosition")) {
            List<Entity> targets = CommandArgumentTypes.entities(context, "player");
            if (targets.isEmpty()) {
                return success();
            }

            for (Entity target : targets) {
                teleportToPosition(context.getSource(), sender, target,
                        argumentValue(context, "playerPosition", PositionResolver.class),
                        rotation(context, "playerYaw", context.getSource().location().getYaw()),
                        rotation(context, "playerPitch", context.getSource().location().getPitch()));
            }
            return success();
        }

        if (hasArgument(context, "position")) {
            Entity target = context.getSource().executor();
            if (target == null) {
                sender.sendMessage(Component.translatable("commands.locate.fail.noplayer"));
                return success();
            }

            return teleportToPosition(context.getSource(), sender, target,
                    argumentValue(context, "position", PositionResolver.class),
                    rotation(context, "yaw", context.getSource().location().getYaw()),
                    rotation(context, "pitch", context.getSource().location().getPitch()));
        }

        if (hasArgument(context, "player")) {
            Entity origin = context.getSource().executor();
            if (origin == null) {
                sender.sendMessage(Component.translatable("commands.locate.fail.noplayer"));
                return success();
            }

            Entity target = CommandArgumentTypes.entity(context, "player");
            teleportToEntity(sender, origin, target);
            return success();
        }

        return usage();
    }

    private static void teleportToEntity(CommandSender sender, Entity origin, Entity target) {
        origin.teleport(target.getLocation(), PlayerTeleportEvent.TeleportCause.COMMAND);
        CommandUtils.broadcastCommandMessage(sender, Component.translatable("commands.tp.success",
                Component.text(origin.getName()), Component.text(target.getName())));
        if (origin instanceof Player player && origin != sender) {
            player.sendMessage(Component.translatable("commands.tp.successVictim",
                    Component.text(target.getName())));
        }
    }

    private static int teleportToPosition(CommandSourceStack source, CommandSender sender, Entity target,
                                          PositionResolver positionResolver, Float yawInput, Float pitchInput)
            throws CommandSyntaxException {
        Vector3f position = positionResolver.resolve(source);

        float yaw = target.getYaw();
        float pitch = target.getPitch();

        if (yawInput != null) {
            yaw = yawInput;
        }

        if (pitchInput != null) {
            pitch = pitchInput;
        }

        target.teleport(Location.from(position, yaw, pitch, source.level()), PlayerTeleportEvent.TeleportCause.COMMAND);
        CommandUtils.broadcastCommandMessage(sender, Component.translatable("commands.tp.success.coordinates",
                Component.text(target.getName()),
                Component.text(String.valueOf(GenericMath.round(position.getX(), 2))),
                Component.text(String.valueOf(GenericMath.round(position.getY(), 2))),
                Component.text(String.valueOf(GenericMath.round(position.getZ(), 2)))));

        if (target instanceof Player player && target != sender) {
            player.sendMessage(Component.translatable("commands.tp.successVictim",
                    Component.text(position.toString())));
        }

        return success();
    }

    private static Float rotation(CommandContext<CommandSourceStack> context, String name, float origin) {
        if (!hasArgument(context, name)) {
            return null;
        }

        return argumentValue(context, name, RotationResolver.class).resolve(origin);
    }
}
