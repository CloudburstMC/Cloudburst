package org.cloudburstmc.server.command.defaults;

import com.mojang.brigadier.context.CommandContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.api.command.CommandSourceStack;
import org.cloudburstmc.server.command.AdvertisedCommand;
import org.cloudburstmc.server.diagnostics.ServerTickStatistics;

import java.util.Locale;
import java.util.Objects;

// TODO: Move the command description and messages to language files.
public class MsptCommand extends AdvertisedCommand {
    private final ServerTickStatistics statistics;

    public MsptCommand(ServerTickStatistics statistics) {
        super("mspt", "View server tick times", "cloudburst.command.mspt");
        this.statistics = Objects.requireNonNull(statistics, "statistics");
    }

    @Override
    protected int execute(CommandContext<CommandSourceStack> context) {
        ServerTickStatistics.Snapshot snapshot = this.statistics.snapshot();
        CommandSender sender = sender(context);
        sender.sendMessage(Component.text("Server tick times (avg/min/max) from last 5s, 10s, 1m:", NamedTextColor.GOLD));
        sender.sendMessage(format(snapshot.fiveSecondMspt())
                .append(Component.text(", ", NamedTextColor.GRAY))
                .append(format(snapshot.tenSecondMspt()))
                .append(Component.text(", ", NamedTextColor.GRAY))
                .append(format(snapshot.oneMinuteMspt())));
        return success();
    }

    private static Component format(ServerTickStatistics.Durations durations) {
        return format(durations.average())
                .append(Component.text("/", NamedTextColor.GRAY))
                .append(format(durations.minimum()))
                .append(Component.text("/", NamedTextColor.GRAY))
                .append(format(durations.maximum()));
    }

    private static Component format(double milliseconds) {
        NamedTextColor color = milliseconds >= 50 ? NamedTextColor.RED : milliseconds >= 40 ? NamedTextColor.YELLOW : NamedTextColor.GREEN;
        return Component.text(String.format(Locale.ROOT, "%.1f", milliseconds), color);
    }
}
