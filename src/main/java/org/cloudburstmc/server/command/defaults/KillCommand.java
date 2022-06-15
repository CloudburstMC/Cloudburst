package org.cloudburstmc.server.command.defaults;

import com.nukkitx.protocol.bedrock.data.command.CommandParamType;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.event.entity.EntityDamageEvent;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.command.Command;
import org.cloudburstmc.server.command.CommandUtils;
import org.cloudburstmc.server.command.data.CommandData;
import org.cloudburstmc.server.command.data.CommandParameter;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.locale.TranslationContainer;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.utils.TextFormat;

import java.util.StringJoiner;

/**
 * Created on 2015/12/08 by Pub4Game.
 * Package cn.nukkit.command.defaults in project Nukkit .
 */
public class KillCommand extends Command {

    public KillCommand() {
        super("kill", CommandData.builder("kill")
                .setDescription("commands.kill.description")
                .setUsageMessage("/kill [player]")
                .setAliases("suicide")
                .setPermissions("cloudburst.command.kill.self", "cloudburst.command.kill.other")
                .setParameters(new CommandParameter[]{
                        new CommandParameter("player", CommandParamType.TARGET, true)
                })
                .build());
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!this.testPermission(sender)) {
            return true;
        }
        if (args.length >= 2) {
            return false;
        }
        if (args.length == 1) {
            if (!sender.hasPermission("cloudburst.command.kill.other")) {
                sender.sendMessage(new TranslationContainer(TextFormat.RED + "%commands.generic.permission"));
                return true;
            }
            CloudPlayer player = (CloudPlayer) sender.getServer().getPlayer(args[0]);
            if (player != null) {
                EntityDamageEvent ev = new EntityDamageEvent(player, EntityDamageEvent.DamageCause.SUICIDE, 1000);
                sender.getServer().getEventManager().fire(ev);
                if (ev.isCancelled()) {
                    return true;
                }
                player.setLastDamageCause(ev);
                player.setHealth(0);
                CommandUtils.broadcastCommandMessage(sender, new TranslationContainer("%commands.kill.successful", player.getName()));
            } else if (args[0].equals("@e")) {
                StringJoiner joiner = new StringJoiner(", ");
                for (CloudLevel level : CloudServer.getInstance().getLevels()) {
                    for (Entity entity : level.getEntities()) {
                        if (!(entity instanceof CloudPlayer)) {
                            joiner.add(entity.getName());
                            entity.close();
                        }
                    }
                }
                String entities = joiner.toString();
                sender.sendMessage(new TranslationContainer("commands.kill.successful", entities.isEmpty() ? "0" : entities));
            } else if (args[0].equals("@s")) {
                if (!sender.hasPermission("cloudburst.command.kill.self")) {
                    sender.sendMessage(new TranslationContainer(TextFormat.RED + "%commands.generic.permission"));
                    return true;
                }
                if (!(sender instanceof CloudPlayer)) {
                    sender.sendMessage(new TranslationContainer("commands.locate.fail.noplayer"));
                    return true;
                }
                EntityDamageEvent ev = new EntityDamageEvent((CloudPlayer) sender, EntityDamageEvent.DamageCause.SUICIDE, 1000);
                sender.getServer().getEventManager().fire(ev);
                if (ev.isCancelled()) {
                    return true;
                }
                ((CloudPlayer) sender).setLastDamageCause(ev);
                ((CloudPlayer) sender).setHealth(0);
                sender.sendMessage(new TranslationContainer("commands.kill.successful", sender.getName()));
            } else if (args[0].equals("@a")) {
                if (!sender.hasPermission("cloudburst.command.kill.other")) {
                    sender.sendMessage(new TranslationContainer(TextFormat.RED + "%commands.generic.permission"));
                    return true;
                }
                for (CloudLevel level : CloudServer.getInstance().getLevels()) {
                    for (Entity entity : level.getEntities()) {
                        if (entity instanceof CloudPlayer) {
                            entity.setHealth(0);
                            sender.sendMessage(new TranslationContainer(TextFormat.GOLD + "%commands.kill.successful", entity.getName()));
                        }
                    }
                }
            } else {
                sender.sendMessage(new TranslationContainer(TextFormat.RED + "%commands.generic.player.notFound"));
            }
            return true;
        }
        if (sender instanceof CloudPlayer) {
            if (!sender.hasPermission("cloudburst.command.kill.self")) {
                sender.sendMessage(new TranslationContainer(TextFormat.RED + "%commands.generic.permission"));
                return true;
            }
            EntityDamageEvent ev = new EntityDamageEvent((CloudPlayer) sender, EntityDamageEvent.DamageCause.SUICIDE, 1000);
            sender.getServer().getEventManager().fire(ev);
            if (ev.isCancelled()) {
                return true;
            }
            ((CloudPlayer) sender).setLastDamageCause(ev);
            ((CloudPlayer) sender).setHealth(0);
            sender.sendMessage(new TranslationContainer("commands.kill.successful", sender.getName()));
        } else {
            return false;
        }
        return true;
    }
}
