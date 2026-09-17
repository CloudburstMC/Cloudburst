package org.cloudburstmc.server.command.defaults;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.api.command.CommandSourceStack;
import org.cloudburstmc.api.command.Commands;
import org.cloudburstmc.api.command.argument.CommandArgumentTypes;
import org.cloudburstmc.api.command.argument.CommandArguments;
import org.cloudburstmc.api.item.*;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.server.command.AdvertisedCommand;
import org.cloudburstmc.server.command.CommandUtils;
import org.cloudburstmc.server.command.network.CommandNetworkData;
import org.cloudburstmc.server.item.ItemUtils;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.List;
import java.util.stream.Collectors;

public class GiveCommand extends AdvertisedCommand {
    public GiveCommand() {
        super("give", "commands.give.description", CommandNetworkData.DEFAULT, "cloudburst.command.give");
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder, String label, CommandArguments arguments) {
        builder.then(Commands.argument("player", arguments.players())
                .then(Commands.argument("itemName", arguments.item())
                        .executes(this::executeCommand)
                        .then(Commands.argument("amount", CommandArgumentTypes.integer(1, Short.MAX_VALUE))
                                .executes(this::executeCommand)
                                .then(Commands.argument("data", CommandArgumentTypes.integer(0, Short.MAX_VALUE))
                                        .executes(this::executeCommand)
                                        .then(Commands.argument("components", CommandArgumentTypes.json())
                                                .executes(this::executeCommand))))));
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
        ItemStack prototype = air
                ? ItemStack.EMPTY
                : ItemUtils.deserializeItem(type.getId(), data, 1, NbtMap.EMPTY);
        if (prototype.isEmpty() && !air) {
            sender.sendMessage(Component.translatable("commands.give.item.invalid", Component.text(type.getId().toString())).color(NamedTextColor.RED));
            return success();
        }

        if (!prototype.isEmpty() && hasArgument(context, "components")) {
            ItemStackBuilder builder = prototype.toBuilder();
            try {
                ItemCommandComponents.parse(
                                argumentValue(context, "components"),
                                sender.getServer().getBlockRegistry()::get)
                        .applyTo(builder);
            } catch (IllegalArgumentException e) {
                return failure(context, Component.text(e.getMessage()).color(NamedTextColor.RED));
            }

            prototype = builder.build();
        }

        String customName = prototype.get(ItemDataComponents.CUSTOM_NAME);
        String itemDisplay = customName == null ? type.getId().toString() : customName + " (" + prototype.getType().getId() + ")";
        for (CloudPlayer player : players) {
            if (!prototype.isEmpty()) {
                give(player, prototype, amount);
            }
        }

        String recipients = players.stream()
                .map(CloudPlayer::getName)
                .collect(Collectors.joining(", "));
        CommandUtils.broadcastCommandMessage(sender, Component.translatable("commands.give.success",
                Component.text(itemDisplay),
                Component.text(amount),
                Component.text(recipients)));
        return players.size();
    }

    private static void give(CloudPlayer player, ItemStack prototype, int amount) {
        int maxStackSize = player.getServer().getItemRegistry()
                .requireComponent(prototype.getType(), ItemBehaviors.GET_MAX_STACK_SIZE)
                .execute(prototype);

        int remaining = amount;
        while (remaining > 0) {
            int count = Math.min(remaining, maxStackSize);
            ItemStack stack = prototype.withCount(count);
            for (ItemStack leftover : player.getContainer().addItem(stack)) {
                player.dropItem(leftover);
            }

            remaining -= count;
        }
    }
}
