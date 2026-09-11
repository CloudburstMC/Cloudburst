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
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.server.command.CommandUtils;
import org.cloudburstmc.server.command.AdvertisedCommand;
import org.cloudburstmc.server.command.network.CommandNetworkData;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.player.CloudPlayer;

public class SetWorldSpawnCommand extends AdvertisedCommand {
    public SetWorldSpawnCommand() {
        super("setworldspawn", "commands.setworldspawn.description", CommandNetworkData.GAME_DIRECTORS,
                "cloudburst.command.setworldspawn");
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder, String label, CommandArguments arguments) {
        builder.executes(this::executeCommand);
        builder.then(Commands.argument("blockPos", CommandArgumentTypes.position())
                .executes(this::executeCommand));
    }

    @Override
    protected int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSender sender = sender(context);
        CloudLevel level;
        Vector3f pos;
        if (!hasArgument(context, "blockPos")) {
            if (sender instanceof CloudPlayer) {
                level = ((CloudPlayer) sender).getLevel();
                pos = ((CloudPlayer) sender).getPosition();
            } else {
                sender.sendMessage(Component.translatable("commands.locate.fail.noplayer"));
                return success();
            }
        } else {
            level = (CloudLevel) sender.getServer().getDefaultLevel();
            pos = argumentValue(context, "blockPos", PositionResolver.class).resolve(context.getSource());
        }
        level.setSpawnLocation(pos);

        CommandUtils.broadcastCommandMessage(sender, Component.translatable("commands.setworldspawn.success",
                Component.text(pos.getFloorX()), Component.text(pos.getFloorY()), Component.text(pos.getFloorZ())));
        return success();
    }
}
