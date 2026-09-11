package org.cloudburstmc.server.command.defaults;

import co.aikar.timings.Timings;
import co.aikar.timings.TimingsExport;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.kyori.adventure.text.Component;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.api.command.CommandSourceStack;
import org.cloudburstmc.api.command.Commands;
import org.cloudburstmc.api.command.argument.CommandArguments;
import org.cloudburstmc.api.command.argument.CommandArgumentTypes;
import org.cloudburstmc.server.command.AdvertisedCommand;

import java.util.Locale;

public class TimingsCommand extends AdvertisedCommand {

    public TimingsCommand() {
        super("timings", "cloudburst.command.timings.description", "cloudburst.command.timings");
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder, String label, CommandArguments arguments) {
        builder.then(Commands.argument("action", CommandArgumentTypes.fixedEnumNamed("action", "TimingsAction",
                        "on", "off", "reset", "report", "paste", "verbon", "verboff"))
                .executes(this::executeCommand));
    }

    @Override
    protected int execute(CommandContext<CommandSourceStack> context) {
        CommandSender sender = sender(context);
        String mode = argumentValue(context, "action").toLowerCase(Locale.ROOT);

        if (mode.equals("on")) {
            Timings.setTimingsEnabled(true);
            Timings.reset();
            sender.sendMessage(Component.translatable("cloudburst.command.timings.enable"));
            return success();
        } else if (mode.equals("off")) {
            Timings.setTimingsEnabled(false);
            sender.sendMessage(Component.translatable("cloudburst.command.timings.disable"));
            return success();
        }

        if (!Timings.isTimingsEnabled()) {
            sender.sendMessage(Component.translatable("cloudburst.command.timings.timingsDisabled"));
            return success();
        }

        switch (mode) {
            case "verbon":
                sender.sendMessage(Component.translatable("cloudburst.command.timings.verboseEnable"));
                Timings.setVerboseEnabled(true);
                break;
            case "verboff":
                sender.sendMessage(Component.translatable("cloudburst.command.timings.verboseDisable"));
                Timings.setVerboseEnabled(false);
                break;
            case "reset":
                Timings.reset();
                sender.sendMessage(Component.translatable("cloudburst.command.timings.reset"));
                break;
            case "report":
            case "paste":
                TimingsExport.reportTimings(sender);
                break;
        }

        return success();
    }
}
