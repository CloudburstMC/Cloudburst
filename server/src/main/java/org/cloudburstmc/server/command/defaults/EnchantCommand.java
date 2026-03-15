package org.cloudburstmc.server.command.defaults;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.api.enchantment.Enchantment;
import org.cloudburstmc.api.item.ItemKeys;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.protocol.bedrock.data.command.CommandParamType;
import org.cloudburstmc.server.command.Command;
import org.cloudburstmc.server.command.CommandUtils;
import org.cloudburstmc.server.command.data.CommandData;
import org.cloudburstmc.server.command.data.CommandParameter;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.EnchantmentRegistry;

public class EnchantCommand extends Command {

    public EnchantCommand() {
        super("enchant", CommandData.builder("enchant")
                .setDescription("commands.enchant.description")
                .setUsageMessage("/enchant <player> <enchant ID> [level]")
                .setPermissions("cloudburst.command.enchant")
                .setParameters(
                        new CommandParameter[]{
                                new CommandParameter("player", CommandParamType.TARGET, false),
                                new CommandParameter("enchantment ID", CommandParamType.INT, false),
                                new CommandParameter("level", CommandParamType.INT, true)
                        }, new CommandParameter[]{
                                new CommandParameter("player", CommandParamType.TARGET, false),
                                new CommandParameter("id", false, CommandParameter.ENUM_TYPE_ENCHANTMENT_LIST),
                                new CommandParameter("level", CommandParamType.INT, true)
                        })
                .build());
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!this.testPermission(sender)) {
            return true;
        }
        if (args.length < 2) {
            return false;
        }
        CloudPlayer player = (CloudPlayer) sender.getServer().getPlayer(args[0]);
        if (player == null) {
            sender.sendMessage(Component.translatable("commands.generic.player.notFound").color(NamedTextColor.RED));
            return true;
        }
        short enchantId;
        int enchantLevel;
        try {
            enchantId = getIdByName(args[1]);
            enchantLevel = args.length == 3 ? Integer.parseInt(args[2]) : 1;
        } catch (NumberFormatException e) {
            return false;
        }
        var registry = EnchantmentRegistry.get();
        var enchantment = registry.getEnchantment(registry.getType(enchantId), enchantLevel);
        if (enchantment == null) {
            sender.sendMessage(Component.translatable("commands.enchant.notFound", Component.text(enchantId)));
            return true;
        }

        ItemStack item = player.getInventory().getSelectedItem();
        if (item.isEmpty()) {
            sender.sendMessage(Component.translatable("commands.enchant.noItem", Component.text(String.valueOf(item.get(ItemKeys.CUSTOM_NAME)))));
            return true;
        }

        //TODO new format?
        item.get(ItemKeys.ENCHANTMENTS).put(registry.getType(enchantId), new Enchantment(registry.getType(enchantId), enchantLevel));

        player.getInventory().setSelectedItem(item);
        CommandUtils.broadcastCommandMessage(sender, Component.translatable("commands.enchant.success", Component.text(sender.getName())));
        return true;
    }

    public short getIdByName(String value) throws NumberFormatException {
        value = value.toLowerCase();
        return switch (value) {
            case "protection" -> 0;
            case "fire_protection" -> 1;
            case "feather_falling" -> 2;
            case "blast_protection" -> 3;
            case "projectile_projection" -> 4;
            case "thorns" -> 5;
            case "respiration" -> 6;
            case "aqua_affinity" -> 7;
            case "depth_strider" -> 8;
            case "sharpness" -> 9;
            case "smite" -> 10;
            case "bane_of_arthropods" -> 11;
            case "knockback" -> 12;
            case "fire_aspect" -> 13;
            case "looting" -> 14;
            case "efficiency" -> 15;
            case "silk_touch" -> 16;
            case "durability", "unbreaking" -> 17;
            case "fortune" -> 18;
            case "power" -> 19;
            case "punch" -> 20;
            case "flame" -> 21;
            case "infinity" -> 22;
            case "luck_of_the_sea" -> 23;
            case "lure" -> 24;
            case "frost_walker" -> 25;
            case "mending" -> 26;
            case "binding_curse" -> 27;
            case "vanishing_curse" -> 28;
            case "impaling" -> 29;
            case "riptide" -> 30;
            case "loyalty" -> 31;
            case "channeling" -> 32;
            default -> Short.parseShort(value);
        };
    }
}
