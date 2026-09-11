package org.cloudburstmc.server.command.defaults;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.api.command.CommandSourceStack;
import org.cloudburstmc.api.command.Commands;
import org.cloudburstmc.api.command.argument.CommandArguments;
import org.cloudburstmc.api.command.argument.CommandArgumentTypes;
import org.cloudburstmc.api.command.argument.resolver.BlockPositionResolver;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.block.util.BlockStateMetaMappings;
import org.cloudburstmc.server.command.AdvertisedCommand;
import org.cloudburstmc.server.command.network.CommandNetworkData;

import java.util.Locale;

public class SetBlockCommand extends AdvertisedCommand {

    public SetBlockCommand() {
        super("setblock", "commands.setblock.description", CommandNetworkData.GAME_DIRECTORS,
                "cloudburst.command.setblock");
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder, String label, CommandArguments arguments) {
        builder.then(Commands.argument("position", CommandArgumentTypes.blockPosition())
                .then(Commands.argument("tileName", arguments.block())
                        .executes(this::executeCommand)
                        .then(Commands.argument("tileData", CommandArgumentTypes.integer())
                                .executes(this::executeCommand)
                                .then(Commands.argument("oldBlockHandling", CommandArgumentTypes.fixedEnumNamed("oldBlockHandling", "SetBlockMode", "replace", "destroy", "keep"))
                                        .executes(this::executeCommand)))));
    }

    @Override
    protected int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSender sender = sender(context);
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.translatable("commands.locate.fail.noplayer"));
            return success();
        }

        Vector3i pos = argumentValue(context, "position", BlockPositionResolver.class)
                .resolve(context.getSource());

        if (pos.getY() < 0 || pos.getY() > 255) {
            sender.sendMessage(Component.translatable("commands.setblock.outOfWorld").color(NamedTextColor.RED));
            return success();
        }

        BlockType type = argumentValue(context, "tileName", BlockType.class);
        int meta = hasArgument(context, "tileData") ? argumentValue(context, "tileData", Integer.class) : 0;

        BlockState state = BlockStateMetaMappings.getStateFromMeta(type.getId(), meta);
        if (state == null) {
            sender.sendMessage(Component.translatable("commands.setblock.notFound", Component.text(type.getId().toString())).color(NamedTextColor.RED));
            return success();
        }

        SetBlockMode setType = hasArgument(context, "oldBlockHandling")
                ? SetBlockMode.valueOf(argumentValue(context, "oldBlockHandling").toUpperCase(Locale.ROOT))
                : SetBlockMode.REPLACE;

        if (setType != SetBlockMode.REPLACE) {
            BlockState existing = player.getLevel().getBlockState(pos);

            if (existing != BlockStates.AIR) {
                if (setType == SetBlockMode.DESTROY) {
                    player.getLevel().breakBlock(pos);
                } else {
                    sender.sendMessage(Component.translatable("commands.setblock.noChange").color(NamedTextColor.RED));
                    return success();
                }
            }
        }

        player.getLevel().setBlockState(pos, state);
        sender.sendMessage(Component.translatable("commands.setblock.success"));

        return success();
    }
}
