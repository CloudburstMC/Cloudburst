package org.cloudburstmc.server.command.defaults;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.api.command.CommandSourceStack;
import org.cloudburstmc.api.command.Commands;
import org.cloudburstmc.api.command.argument.CommandArguments;
import org.cloudburstmc.server.command.AdvertisedCommand;
import org.cloudburstmc.server.diagnostics.ServerTickStatistics;

import java.util.Locale;
import java.util.Objects;

// TODO: Move the command description and messages to language files.
public class TpsCommand extends AdvertisedCommand {
    private final ServerTickStatistics statistics;
    private boolean memoryWarningShown;

    public TpsCommand(ServerTickStatistics statistics) {
        super("tps", "View server ticks per second", "cloudburst.command.tps");
        this.statistics = Objects.requireNonNull(statistics, "statistics");
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder, String label, CommandArguments arguments) {
        builder.executes(this::executeCommand)
                .then(Commands.literal("mem")
                        .requires(Commands.requiresPermission("cloudburst.command.tpsmemory"))
                        .executes(context -> this.executeCommand(context, this::showMemory)));
    }

    @Override
    protected int execute(CommandContext<CommandSourceStack> context) {
        ServerTickStatistics.Snapshot snapshot = this.statistics.snapshot();
        sender(context).sendMessage(Component.text("TPS from last 1m, 5m, 15m: ", NamedTextColor.GOLD)
                .append(format(snapshot.oneMinuteTps()))
                .append(Component.text(", ", NamedTextColor.GRAY))
                .append(format(snapshot.fiveMinuteTps()))
                .append(Component.text(", ", NamedTextColor.GRAY))
                .append(format(snapshot.fifteenMinuteTps())));
        return success();
    }

    private int showMemory(CommandContext<CommandSourceStack> context) {
        this.execute(context);

        Runtime runtime = Runtime.getRuntime();
        long committed = runtime.totalMemory();
        long used = committed - runtime.freeMemory();
        long megabyte = 1024 * 1024;

        CommandSender sender = sender(context);
        sender.sendMessage(Component.text("Current memory usage: ", NamedTextColor.GOLD)
                .append(Component.text(used / megabyte + "/" + committed / megabyte + " MB (Max: " + runtime.maxMemory() / megabyte + " MB)", NamedTextColor.GREEN)));

        if (!this.memoryWarningShown) {
            sender.sendMessage(Component.text("Memory usage fluctuates with garbage collection and is not a stable performance measure.", NamedTextColor.GOLD));
            this.memoryWarningShown = true;
        }

        return success();
    }

    private static Component format(double tps) {
        NamedTextColor color = tps > 18 ? NamedTextColor.GREEN : tps > 16 ? NamedTextColor.YELLOW : NamedTextColor.RED;
        return Component.text(String.format(Locale.ROOT, "%.1f", tps), color);
    }
}
