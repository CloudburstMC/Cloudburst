package org.cloudburstmc.server.command.defaults;

import com.nukkitx.protocol.bedrock.data.command.CommandParamType;
import org.cloudburstmc.api.level.gamerule.GameRule;
import org.cloudburstmc.api.registry.GameRuleRegistry;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.command.Command;
import org.cloudburstmc.server.command.CommandSender;
import org.cloudburstmc.server.command.data.CommandData;
import org.cloudburstmc.server.command.data.CommandParameter;
import org.cloudburstmc.server.level.gamerule.GameRuleMap;
import org.cloudburstmc.server.locale.TranslationContainer;
import org.cloudburstmc.server.player.Player;

import java.util.Arrays;
import java.util.StringJoiner;

public class GameruleCommand extends Command {
    private static final GameRuleRegistry registry = CloudServer.getInstance().getGameRuleRegistry();

    public GameruleCommand() {
        super("gamerule", CommandData.builder("gamerule")
                .setDescription("commands.gamerule.description")
                .setUsageMessage("/gamerule <gamerule> [value]")
                .setPermissions("cloudburst.command.gamerule")
                .setParameters(new CommandParameter[]{
                        new CommandParameter("gamerule", true, registry.getRuleNames().toArray(new String[0])),
                        new CommandParameter("value", CommandParamType.STRING, true)
                })
                .build());
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!this.testPermission(sender)) {
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(new TranslationContainer("commands.locate.fail.noplayer"));
            return true;
        }
        GameRuleMap rules = ((Player) sender).getLevel().getGameRules();

        switch (args.length) {
            case 0:
                StringJoiner rulesJoiner = new StringJoiner(", ");
                for (String rule : registry.getRuleNames()) {
                    rulesJoiner.add(rule.toLowerCase());
                }
                sender.sendMessage(rulesJoiner.toString());
                return true;
            case 1:
                GameRule gameRule = registry.fromString(args[0]);
                if (gameRule == null || !rules.contains(gameRule)) {
                    sender.sendMessage(new TranslationContainer("commands.generic.syntax", "/gamerule", args[0]));
                    return true;
                }

                sender.sendMessage(gameRule.getName() + " = " + rules.get(gameRule).toString());
                return true;
            default:
                gameRule = registry.fromString(args[0]);

                if (gameRule == null) {
                    sender.sendMessage(new TranslationContainer("commands.generic.syntax",
                            "/gamerule ", args[0], " " + String.join(" ", Arrays.copyOfRange(args, 1, args.length))));
                    return true;
                }

                try {
                    rules.put(gameRule, gameRule.parse(args[1]));
                    sender.sendMessage(new TranslationContainer("commands.gamerule.success", gameRule.getName(), args[1]));
                } catch (NumberFormatException e) {
                    sender.sendMessage(new TranslationContainer("commands.generic.syntax", "/gamerule " + args[0] + " ", args[1], " " + String.join(" ", Arrays.copyOfRange(args, 2, args.length))));
                }
                return true;
        }
    }
}
