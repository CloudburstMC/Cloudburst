package org.cloudburstmc.server.command.defaults;

import com.nukkitx.protocol.bedrock.data.command.CommandParamType;
import lombok.val;
import org.cloudburstmc.server.command.Command;
import org.cloudburstmc.server.command.CommandSender;
import org.cloudburstmc.server.command.CommandUtils;
import org.cloudburstmc.server.command.data.CommandData;
import org.cloudburstmc.server.command.data.CommandParameter;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.locale.TranslationContainer;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.registry.EnchantmentRegistry;
import org.cloudburstmc.server.utils.TextFormat;

/**
 * Created by Pub4Game on 23.01.2016.
 */
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
        Player player = sender.getServer().getPlayer(args[0]);
        if (player == null) {
            sender.sendMessage(new TranslationContainer(TextFormat.RED + "%commands.generic.player.notFound"));
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
        val registry = EnchantmentRegistry.get();
        val enchantment = registry.getEnchantment(registry.getType(enchantId), enchantLevel);
        if (enchantment == null) {
            sender.sendMessage(new TranslationContainer("%commands.enchant.notFound", enchantId));
            return true;
        }

        ItemStack item = player.getInventory().getItemInHand();
        if (item.isNull()) {
            sender.sendMessage(new TranslationContainer("%commands.enchant.noItem", item.getName()));
            return true;
        }

        item = item.withEnchantment(enchantment);

        player.getInventory().setItemInHand(item);
        CommandUtils.broadcastCommandMessage(sender, new TranslationContainer("%commands.enchant.success", sender.getName()));
        return true;
    }

    public short getIdByName(String value) throws NumberFormatException {
        switch (value) {
            case "protection":
                return 0;
            case "fire_protection":
                return 1;
            case "feather_falling":
                return 2;
            case "blast_protection":
                return 3;
            case "projectile_projection":
                return 4;
            case "thorns":
                return 5;
            case "respiration":
                return 6;
            case "aqua_affinity":
                return 7;
            case "depth_strider":
                return 8;
            case "sharpness":
                return 9;
            case "smite":
                return 10;
            case "bane_of_arthropods":
                return 11;
            case "knockback":
                return 12;
            case "fire_aspect":
                return 13;
            case "looting":
                return 14;
            case "efficiency":
                return 15;
            case "silk_touch":
                return 16;
            case "durability":
                return 17;
            case "fortune":
                return 18;
            case "power":
                return 19;
            case "punch":
                return 20;
            case "flame":
                return 21;
            case "infinity":
                return 22;
            case "luck_of_the_sea":
                return 23;
            case "lure":
                return 24;
            case "frost_walker":
                return 25;
            case "mending":
                return 26;
            case "binding_curse":
                return 27;
            case "vanishing_curse":
                return 28;
            case "impaling":
                return 29;
            case "loyality":
                return 30;
            case "riptide":
                return 31;
            case "channeling":
                return 32;
            default:
                return Short.parseShort(value);
        }
    }
}
