package org.cloudburstmc.server.command.defaults;

import co.aikar.timings.Timings;
import co.aikar.timings.TimingsExport;
import org.cloudburstmc.server.command.Command;
import org.cloudburstmc.server.command.CommandSender;
import org.cloudburstmc.server.command.data.CommandData;
import org.cloudburstmc.server.command.data.CommandParameter;
import org.cloudburstmc.server.locale.TranslationContainer;

/**
 * @author fromgate
 * @author Pub4Game
 */
public class TimingsCommand extends Command {

    public TimingsCommand() {
        super("timings", CommandData.builder("timings")
                .setDescription("cloudburst.command.timings.description")
                .setUsageMessage("/timings <on|off|paste>")
                .setPermissions("cloudburst.command.timings")
                .setParameters(new CommandParameter[]{
                        new CommandParameter("on|off|paste")
                })
                .build());
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!this.testPermission(sender)) {
            return true;
        }

        if (args.length != 1) {
            return false;
        }

        String mode = args[0].toLowerCase();

        if (mode.equals("on")) {
            Timings.setTimingsEnabled(true);
            Timings.reset();
            sender.sendMessage(new TranslationContainer("cloudburst.command.timings.enable"));
            return true;
        } else if (mode.equals("off")) {
            Timings.setTimingsEnabled(false);
            sender.sendMessage(new TranslationContainer("cloudburst.command.timings.disable"));
            return true;
        }

        if (!Timings.isTimingsEnabled()) {
            sender.sendMessage(new TranslationContainer("cloudburst.command.timings.timingsDisabled"));
            return true;
        }

        switch (mode) {
            case "verbon":
                sender.sendMessage(new TranslationContainer("cloudburst.command.timings.verboseEnable"));
                Timings.setVerboseEnabled(true);
                break;
            case "verboff":
                sender.sendMessage(new TranslationContainer("cloudburst.command.timings.verboseDisable"));
                Timings.setVerboseEnabled(true);
                break;
            case "reset":
                Timings.reset();
                sender.sendMessage(new TranslationContainer("cloudburst.command.timings.reset"));
                break;
            case "report":
            case "paste":
                TimingsExport.reportTimings(sender);
                break;
        }
        return true;
    }
}

