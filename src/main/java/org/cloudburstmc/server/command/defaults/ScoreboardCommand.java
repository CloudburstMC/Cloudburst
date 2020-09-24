package org.cloudburstmc.server.command.defaults;

import com.nukkitx.protocol.bedrock.data.command.CommandParamType;
import org.cloudburstmc.server.command.Command;
import org.cloudburstmc.server.command.CommandSender;
import org.cloudburstmc.server.command.data.CommandData;
import org.cloudburstmc.server.command.data.CommandParameter;
import org.cloudburstmc.server.locale.TranslationContainer;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.scoreboard.*;
import org.cloudburstmc.server.scoreboard.impl.CloudScoreboardObjective;
import org.cloudburstmc.server.utils.TextFormat;

/**
 * @author lukeeey
 */
public class ScoreboardCommand extends Command {

    public ScoreboardCommand() {
        super(CommandData.builder("scoreboard")
                .setDescription("commands.scoreboard.description")
                .setUsageMessage("/scoreboard <args>")
                .setPermissions("cloudburst.command.scoreboard")
                .addParameters(new CommandParameter[]{
                        new CommandParameter("objectives", new String[]{"objectives"}),
                        new CommandParameter("add", new String[]{"add"}),
                        new CommandParameter("objectiveName", CommandParamType.STRING, false),
                        new CommandParameter("dummy", new String[]{"dummy"}),
                        new CommandParameter("displayName", CommandParamType.STRING, true)
                }).addParameters(new CommandParameter[]{
                        new CommandParameter("objectives", new String[]{"objectives"}),
                        new CommandParameter("remove", new String[]{"remove"}),
                        new CommandParameter("objective", CommandParamType.STRING, false)
                }).addParameters(new CommandParameter[]{
                        new CommandParameter("objectives", new String[]{"objectives"}),
                        new CommandParameter("list", new String[]{"list"})
                }).addParameters(new CommandParameter[]{
                        new CommandParameter("objectives", new String[]{"objectives"}),
                        new CommandParameter("setdisplay", new String[]{"setdisplay"}),
                        new CommandParameter("displaySlot", new String[]{"list", "sidebar"}),
                        new CommandParameter("objective", CommandParamType.STRING, true),
                        new CommandParameter("sortOrder", true, new String[]{"ascending", "descending"})
                }).addParameters(new CommandParameter[]{
                        new CommandParameter("objectives", new String[]{"objectives"}),
                        new CommandParameter("setdisplay", new String[]{"setdisplay"}),
                        new CommandParameter("belowname", new String[]{"belowname"}),
                        new CommandParameter("objective", CommandParamType.STRING, true),
                }).addParameters(new CommandParameter[]{
                        new CommandParameter("players", new String[]{"players"}),
                        new CommandParameter("list", new String[]{"list"}),
                        new CommandParameter("playerName", CommandParamType.TARGET, true)
                }).addParameters(new CommandParameter[]{
                        new CommandParameter("players", new String[]{"players"}),
                        new CommandParameter("reset", new String[]{"reset"}),
                        new CommandParameter("player", CommandParamType.TARGET, false),
                        new CommandParameter("objective", CommandParamType.STRING, true)
                }).addParameters(new CommandParameter[]{
                        new CommandParameter("players", new String[]{"players"}),
                        new CommandParameter("test", new String[]{"test"}),
                        new CommandParameter("objective", CommandParamType.STRING, false),
                        new CommandParameter("min", CommandParamType.WILDCARD_INT, false),
                        new CommandParameter("max", CommandParamType.WILDCARD_INT, true)
                }).addParameters(new CommandParameter[]{
                        new CommandParameter("players", new String[]{"players"}),
                        new CommandParameter("random", new String[]{"random"}),
                        new CommandParameter("player", CommandParamType.TARGET, false),
                        new CommandParameter("objective", CommandParamType.STRING, false),
                        new CommandParameter("min", CommandParamType.INT, false),
                        new CommandParameter("max", CommandParamType.INT, false)
                }).addParameters(new CommandParameter[]{
                        new CommandParameter("players", new String[]{"players"}),
                        new CommandParameter("actionList", new String[]{"set", "add", "remove"}),
                        new CommandParameter("player", CommandParamType.TARGET, false),
                        new CommandParameter("objective", CommandParamType.STRING, false),
                        new CommandParameter("count", CommandParamType.INT, false)
                }).addParameters(new CommandParameter[]{
                        new CommandParameter("players", new String[]{"players"}),
                        new CommandParameter("operation", new String[]{"operation"}),
                        new CommandParameter("targetName", CommandParamType.TARGET, false),
                        new CommandParameter("targetObjective", CommandParamType.STRING, false),
                        new CommandParameter("operation", CommandParamType.OPERATOR, false),
                        new CommandParameter("selector", CommandParamType.TARGET, false),
                        new CommandParameter("objective", CommandParamType.STRING, false)
                }).build());
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!this.testPermission(sender)) {
            return true;
        }
        if (args.length == 0 || args.length == 1) {
            return false;
        }

        Scoreboard scoreboard = ((Player) sender).getScoreboard(); // TODO: per-world scoreboard, THIS IS JUST FOR TESTING!

        if(scoreboard == null) {
            scoreboard = Scoreboard.builder().players((Player) sender).build();
            ((Player) sender).setScoreboard(scoreboard);
        }

        if (args[0].equalsIgnoreCase("objectives")) {
            switch (args[1]) {
                case "add":
                    if (args.length < 4) {
                        return false;
                    }
                    String name = args[2];
                    String displayName = name;

                    if(args.length == 5) {
                        displayName = args[4]; // criteria is 3rd, always dummy
                    }

                    if(name.length() > CloudScoreboardObjective.MAX_NAME_LENGTH) {
                        sender.sendMessage(new TranslationContainer("%commands.scoreboard.objectives.add.tooLong", name, CloudScoreboardObjective.MAX_NAME_LENGTH));
                        return true;
                    }
                    if(scoreboard.getObjective(name) != null) {
                        sender.sendMessage(new TranslationContainer("%commands.scoreboard.objectives.add.alreadyExists", name));
                        return true;
                    }
                    if(displayName.length() > CloudScoreboardObjective.MAX_DISPLAY_NAME_LENGTH) {
                        sender.sendMessage(new TranslationContainer("%commands.scoreboard.objectives.add.displayTooLong", name, CloudScoreboardObjective.MAX_DISPLAY_NAME_LENGTH));
                        return true;
                    }

                    scoreboard.registerObjective(ScoreboardObjective.builder()
                            .name(name)
                            .displayName(displayName)
                            .displayMode(DisplayMode.SIDEBAR)
                            .criteria(ScoreboardCriteria.DUMMY)
                            .build());

                    sender.sendMessage(new TranslationContainer("%commands.scoreboard.objectives.add.success", name));
                    break;
                case "remove":
                    if(args.length != 3) {
                        return false;
                    }
                    if(scoreboard.getObjective(args[2]) == null) {
                        return true; // TODO
                    }
                    scoreboard.deregisterObjective(args[2]);
                    sender.sendMessage(new TranslationContainer("%commands.scoreboard.objectives.remove.success", args[2]));
                    break;
                case "list":
                    if (scoreboard.getObjectives().isEmpty()) {
                        sender.sendMessage(new TranslationContainer(TextFormat.RED + "%commands.scoreboard.objectives.list.empty"));
                        return true;
                    }

                    sender.sendMessage(new TranslationContainer(TextFormat.GREEN + "%commands.scoreboard.objectives.list.count",
                            scoreboard.getObjectives().size()));

                    scoreboard.getObjectives().forEach(objective -> sender.sendMessage(new TranslationContainer("%commands.scoreboard.objectives.list.entry",
                            objective.getName(), objective.getDisplayName(), objective.getCriteria().name().toLowerCase())));
                    break;
                case "setdisplay":
                    if(args.length < 3) {
                        return false;
                    }
                    DisplayMode displayMode;

                    switch(args[2]) {
                        case "belowname":
                            displayMode = DisplayMode.BELOWNAME;
                            break;
                        case "list":
                            displayMode = DisplayMode.LIST;
                            break;
                        case "sidebar":
                            displayMode = DisplayMode.SIDEBAR;
                            break;
                        default:
                            sender.sendMessage(new TranslationContainer("%commands.scoreboard.objectives.setdisplay.invalidSlot", args[2]));
                            return true;
                    }


                    if(args.length == 3) {
                        // TODO: clear display slot
                        return true;
                    }
                    String objectiveName = args[3];
                    ScoreboardObjective objective = scoreboard.getObjective(objectiveName);

                    if(objective == null) {
                        sender.sendMessage(new TranslationContainer(TextFormat.RED + "%commands.scoreboard.objectiveNotFound", objectiveName));
                        return true;
                    }

                    objective.setDisplayMode(displayMode);

                    if(args.length == 5) {
                        SortOrder sortOrder = SortOrder.DESCENDING;

                        switch(args[4]) {
                            case "ascending": sortOrder = SortOrder.ASCENDING; break;
                            case "descending": sortOrder = SortOrder.DESCENDING; break;
                        }

                        objective.setSortOrder(sortOrder);
                    }

//                    sender.sendMessage(new TranslationContainer("%commands.scoreboard.objectives.setdisplay.successSet", args[2], ));

                    break;
            }
        }

        else if (args[0].equalsIgnoreCase("players")) {
            switch (args[1]) {
                case "list": // TODO: tracked players
                    if(args.length == 2) {
                        sender.sendMessage(new TranslationContainer(TextFormat.GREEN + "%commands.scoreboard.players.list.count", 0));
                        // TODO
                        return true;
                    }
                    break;
                case "reset":
                    break;
                case "test": break;
                case "random": break;
                case "set":
                case "add":
                case "remove":
                    if (args.length != 5) {
                        return false;
                    }

                    // TODO: You can add a score to a player that isnt online in vanilla, meaning just any string of text
                    //       Does a player thats offline need to be set to ScoreType.FAKE, as we cant provide a player instance?

                    ScoreboardObjective objective = scoreboard.getObjective(args[3]);
                    if (objective == null) {
                        sender.sendMessage(new TranslationContainer(TextFormat.RED + "%commands.scoreboard.objectiveNotFound", args[3]));
                        return true;
                    }

                    int count = Integer.parseInt(args[4]);

                    Score score = objective.getScore(args[2]);
                    switch(args[1]) {
                        case "add":
                            int amount = this.setScore(objective, args[2], ((Player) sender), (score != null ? score.getAmount() + count : count));
                            sender.sendMessage(new TranslationContainer("%commands.scoreboard.players.add.success", count, args[3], args[2], amount));
                            break;
                        case "set":
                            this.setScore(objective, args[2], ((Player) sender), count);
                            sender.sendMessage(new TranslationContainer("%commands.scoreboard.players.set.success", args[3], args[2], args[4]));
                            break;
                        case "remove":
                            int removeAmount = this.setScore(objective, args[2], ((Player) sender), (score != null ? score.getAmount() - count : -count));
                            sender.sendMessage(new TranslationContainer("%commands.scoreboard.players.remove.success", count, args[3], args[2], removeAmount));
                            break;
                    }

                break;
            }
        }

        return true;
    }

    private int setScore(ScoreboardObjective objective, String name, Player player, int count) {
        Score score = objective.getScore(name);

        if(score != null) {
            score.setAmount(count);
            return score.getAmount();
        } else {
            objective.getOrCreateScore(name, ScoreType.FAKE, name, count);
            return count;
        }
    }
}
