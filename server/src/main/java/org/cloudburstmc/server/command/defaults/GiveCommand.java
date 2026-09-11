package org.cloudburstmc.server.command.defaults;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.api.command.CommandSourceStack;
import org.cloudburstmc.api.command.Commands;
import org.cloudburstmc.api.command.argument.CommandArguments;
import org.cloudburstmc.api.command.argument.CommandArgumentTypes;
import org.cloudburstmc.api.item.ItemKeys;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemType;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.server.command.CommandUtils;
import org.cloudburstmc.server.command.AdvertisedCommand;
import org.cloudburstmc.server.command.network.CommandNetworkData;
import org.cloudburstmc.server.item.ItemUtils;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.List;

public class GiveCommand extends AdvertisedCommand {
    public GiveCommand() {
        super("give", "commands.give.description", CommandNetworkData.GAME_DIRECTORS, "cloudburst.command.give");
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder, String label, CommandArguments arguments) {
        builder.then(Commands.argument("player", arguments.players())
                .then(Commands.argument("itemName", arguments.item())
                        .executes(this::executeCommand)
                        .then(Commands.argument("amount", CommandArgumentTypes.integer(1, Short.MAX_VALUE))
                                .executes(this::executeCommand)
                                .then(Commands.argument("data", CommandArgumentTypes.integer(0, Short.MAX_VALUE))
                                        .executes(this::executeCommand)))));
    }

    @Override
    protected int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSender sender = sender(context);
        ItemType type = argumentValue(context, "itemName", ItemType.class);
        List<CloudPlayer> players = cloudPlayersArgument(context, "player");
        if (players.isEmpty()) {
            return success();
        }

        int amount = hasArgument(context, "amount") ? argumentValue(context, "amount", Integer.class) : 1;
        short data = hasArgument(context, "data")
                ? argumentValue(context, "data", Integer.class).shortValue()
                : 0;
        boolean air = type.isAir();
        ItemStack stack = air
                ? ItemStack.EMPTY
                : ItemUtils.deserializeItem(type.getId(), data, amount, NbtMap.EMPTY);
        if (stack.isEmpty() && !air) {
            sender.sendMessage(Component.translatable("commands.give.item.invalid",
                    Component.text(type.getId().toString())).color(NamedTextColor.RED));
            return success();
        }

        String customName = stack.get(ItemKeys.CUSTOM_NAME);
        String itemDisplay = customName == null
                ? type.getId().toString()
                : customName + " (" + stack.getType().getId() + ")";
        for (CloudPlayer player : players) {
            if (!stack.isEmpty()) {
                for (ItemStack remaining : player.getContainer().addItem(stack)) {
                    player.dropItem(remaining);
                }
            }
            CommandUtils.broadcastCommandMessage(sender, Component.translatable("commands.give.success",
                    Component.text(itemDisplay),
                    Component.text(amount),
                    Component.text(player.getName())));
        }

        return success();
    }
}
