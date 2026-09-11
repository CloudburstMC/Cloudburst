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
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.math.GenericMath;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.server.command.CommandUtils;
import org.cloudburstmc.server.command.AdvertisedCommand;
import org.cloudburstmc.server.command.network.CommandNetworkData;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.List;

public class SpawnpointCommand extends AdvertisedCommand {
    public SpawnpointCommand() {
        super("spawnpoint", "commands.spawnpoint.description", CommandNetworkData.GAME_DIRECTORS,
                "cloudburst.command.spawnpoint");
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder, String label, CommandArguments arguments) {
        builder.executes(this::executeCommand);
        builder.then(Commands.argument("player", arguments.players())
                .executes(this::executeCommand)
                .then(Commands.argument("spawnPos", CommandArgumentTypes.position())
                        .executes(this::executeCommand)));
    }

    @Override
    protected int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSender sender = sender(context);
        List<CloudPlayer> targets;
        if (hasArgument(context, "player")) {
            targets = cloudPlayersArgument(context, "player");
            if (targets.isEmpty()) {
                return success();
            }
        } else if (sender instanceof CloudPlayer player) {
            targets = List.of(player);
        } else {
            sender.sendMessage(Component.translatable("commands.locate.fail.noplayer"));
            return success();
        }

        Location spawn;
        if (hasArgument(context, "spawnPos")) {
            Vector3f position = argumentValue(context, "spawnPos", PositionResolver.class)
                    .resolve(context.getSource());

            int y = Math.clamp(position.getFloorY(), context.getSource().level().getMinHeight(),
                    context.getSource().level().getMaxHeight() - 1);
            spawn = Location.from(position.getFloorX(), y, position.getFloorZ(),
                    context.getSource().level());
        } else {
            spawn = context.getSource().location();
        }

        for (CloudPlayer target : targets) {
            target.setSpawn(spawn);
            CommandUtils.broadcastCommandMessage(sender, Component.translatable("commands.spawnpoint.success.single",
                    Component.text(target.getName()),
                    Component.text(GenericMath.round(spawn.getX(), 2)),
                    Component.text(GenericMath.round(spawn.getY(), 2)),
                    Component.text(GenericMath.round(spawn.getZ(), 2))));
        }
        return success();
    }
}
