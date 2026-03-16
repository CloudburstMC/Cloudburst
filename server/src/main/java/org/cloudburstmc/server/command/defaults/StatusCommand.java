package org.cloudburstmc.server.command.defaults;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.math.GenericMath;
import org.cloudburstmc.server.Bootstrap;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.command.Command;
import org.cloudburstmc.server.command.data.CommandData;
import org.cloudburstmc.server.level.CloudLevel;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class StatusCommand extends Command {

    public StatusCommand() {
        super("status", CommandData.builder("status")
                .setDescription("cloudburst.command.status.description")
                .setPermissions("cloudburst.command.status")
                .build());
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!this.testPermission(sender)) {
            return true;
        }

        CloudServer server = (CloudServer) sender.getServer();
        sender.sendMessage(Component.text("---- ").color(NamedTextColor.GREEN)
                .append(Component.text("Server status").color(NamedTextColor.WHITE))
                .append(Component.text(" ----").color(NamedTextColor.GREEN)));

        long time = System.currentTimeMillis() - Bootstrap.START_TIME;
        sender.sendMessage(Component.text("Uptime: ").color(NamedTextColor.YELLOW).append(formatUptime(time)));

        NamedTextColor tpsColor = NamedTextColor.GREEN;
        float tps = server.getTicksPerSecond();
        if (tps < 17) {
            tpsColor = NamedTextColor.YELLOW;
        }
        if (tps < 12) {
            tpsColor = NamedTextColor.RED;
        }

        sender.sendMessage(Component.text("Current TPS: ").color(NamedTextColor.YELLOW)
                .append(Component.text(GenericMath.round(tps, 2)).color(tpsColor)));
        sender.sendMessage(Component.text("Load: ").color(NamedTextColor.YELLOW)
                .append(Component.text(server.getTickUsage() + "%").color(tpsColor)));
        sender.sendMessage(Component.text("Network upload: ").color(NamedTextColor.YELLOW)
                .append(Component.text(GenericMath.round((server.getNetwork().getUpload() / 1024 * 1000), 2) + " kB/s").color(NamedTextColor.GREEN)));
        sender.sendMessage(Component.text("Network download: ").color(NamedTextColor.YELLOW)
                .append(Component.text(GenericMath.round((server.getNetwork().getDownload() / 1024 * 1000), 2) + " kB/s").color(NamedTextColor.GREEN)));
        sender.sendMessage(Component.text("Thread count: ").color(NamedTextColor.YELLOW)
                .append(Component.text(Thread.getAllStackTraces().size()).color(NamedTextColor.GREEN)));

        Runtime runtime = Runtime.getRuntime();
        double totalMB = GenericMath.round(((double) runtime.totalMemory()) / 1024 / 1024, 2);
        double usedMB = GenericMath.round((double) (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024, 2);
        double maxMB = GenericMath.round(((double) runtime.maxMemory()) / 1024 / 1024, 2);
        double usage = usedMB / maxMB * 100;
        NamedTextColor usageColor = NamedTextColor.GREEN;
        if (usage > 85) {
            usageColor = NamedTextColor.YELLOW;
        }

        sender.sendMessage(Component.text("Used memory: ").color(NamedTextColor.YELLOW)
                .append(Component.text(usedMB + " MB. (" + GenericMath.round(usage, 2) + "%)").color(usageColor)));

        sender.sendMessage(Component.text("Total memory: ").color(NamedTextColor.YELLOW)
                .append(Component.text(totalMB + " MB.").color(NamedTextColor.RED)));

        sender.sendMessage(Component.text("Maximum VM memory: ").color(NamedTextColor.YELLOW)
                .append(Component.text(maxMB + " MB.").color(NamedTextColor.RED)));

        sender.sendMessage(Component.text("Available processors: ").color(NamedTextColor.YELLOW)
                .append(Component.text(runtime.availableProcessors()).color(NamedTextColor.GREEN)));

        NamedTextColor playerColor = NamedTextColor.GREEN;
        if (((float) server.getOnlinePlayers().size() / (float) server.getMaxPlayers()) > 0.85) {
            playerColor = NamedTextColor.YELLOW;
        }

        sender.sendMessage(Component.text("Players: ").color(NamedTextColor.YELLOW)
                .append(Component.text(server.getOnlinePlayers().size()).color(playerColor))
                .append(Component.text(" online, ").color(NamedTextColor.GREEN))
                .append(Component.text(server.getMaxPlayers()).color(NamedTextColor.RED))
                .append(Component.text(" max. ").color(NamedTextColor.GREEN)));

        for (CloudLevel level : server.getLevels()) {
            String nameInfo = !Objects.equals(level.getId(), level.getName())
                    ? " (" + level.getName() + ")" : "";
            boolean slowTick = level.getTickRate() > 1 || level.getTickRateTime() > 40;
            String tickRateInfo = level.getTickRate() > 1 ? " (tick rate " + level.getTickRate() + ")" : "";
            sender.sendMessage(Component.text("World \"" + level.getId() + "\"" + nameInfo + ": ").color(NamedTextColor.YELLOW)
                    .append(Component.text(level.getChunks().size()).color(NamedTextColor.RED))
                    .append(Component.text(" chunks, ").color(NamedTextColor.GREEN))
                    .append(Component.text(level.getEntities().length).color(NamedTextColor.RED))
                    .append(Component.text(" entities, ").color(NamedTextColor.GREEN))
                    .append(Component.text(level.getBlockEntities().size()).color(NamedTextColor.RED))
                    .append(Component.text(" blockEntities. Time ").color(NamedTextColor.GREEN))
                    .append(Component.text(GenericMath.round(level.getTickRateTime(), 2) + "ms" + tickRateInfo)
                            .color(slowTick ? NamedTextColor.RED : NamedTextColor.YELLOW)));
        }

        return true;
    }

    private static Component formatUptime(long uptime) {
        long days = TimeUnit.MILLISECONDS.toDays(uptime);
        uptime -= TimeUnit.DAYS.toMillis(days);
        long hours = TimeUnit.MILLISECONDS.toHours(uptime);
        uptime -= TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(uptime);
        uptime -= TimeUnit.MINUTES.toMillis(minutes);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(uptime);

        return Component.text(days).color(NamedTextColor.RED)
                .append(Component.text(" days ").color(NamedTextColor.YELLOW))
                .append(Component.text(hours).color(NamedTextColor.RED))
                .append(Component.text(" hours ").color(NamedTextColor.YELLOW))
                .append(Component.text(minutes).color(NamedTextColor.RED))
                .append(Component.text(" minutes ").color(NamedTextColor.YELLOW))
                .append(Component.text(seconds).color(NamedTextColor.RED))
                .append(Component.text(" seconds").color(NamedTextColor.YELLOW));
    }
}
