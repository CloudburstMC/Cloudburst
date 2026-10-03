package org.cloudburstmc.server.command.defaults;

import com.mojang.brigadier.context.CommandContext;
import lombok.extern.log4j.Log4j2;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.api.command.CommandSourceStack;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.command.AdvertisedCommand;
import org.cloudburstmc.server.diagnostics.DebugReport;
import org.cloudburstmc.server.diagnostics.MclogsClient;

import java.net.URI;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

@Log4j2
public class DebugPasteCommand extends AdvertisedCommand {
    private final AtomicBoolean uploading = new AtomicBoolean();
    private final MclogsClient client = new MclogsClient();

    public DebugPasteCommand() {
        super("debugpaste", "commands.debug.description", "cloudburst.command.debug.perform");
    }

    @Override
    protected int execute(CommandContext<CommandSourceStack> context) {
        CommandSender sender = sender(context);
        CloudServer server = (CloudServer) sender.getServer();
        if (!this.uploading.compareAndSet(false, true)) {
            return failure(context, Component.text("A debug report is already being uploaded."));
        }

        try {
            String snapshot = DebugReport.capture(server);
            Path dataPath = server.getDataPath();
            sender.sendMessage(Component.text("Uploading a public debug report to mclo.gs. Logs may contain sensitive information.", NamedTextColor.YELLOW));
            server.getAsyncScheduler().runNow(null, task -> {
                try {
                    URI link = this.client.upload(DebugReport.collect(snapshot, dataPath));
                    server.getGlobalScheduler().execute(null, () -> sender.sendMessage(
                            Component.text("Debug report: ", NamedTextColor.GREEN)
                                    .append(Component.text(link.toString(), NamedTextColor.AQUA)
                                            .clickEvent(ClickEvent.openUrl(link.toString())))));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    reportFailure(server, sender, e);
                } catch (Exception e) {
                    reportFailure(server, sender, e);
                } finally {
                    this.uploading.set(false);
                }
            });
        } catch (RuntimeException e) {
            this.uploading.set(false);
            log.error("Unable to start debug report upload", e);
            return failure(context, Component.text("Unable to start the debug report upload. See the server log for details."));
        }

        return success();
    }

    private static void reportFailure(CloudServer server, CommandSender sender, Exception error) {
        log.error("Unable to upload debug report", error);
        server.getGlobalScheduler().execute(null, () -> sender.sendMessage(
                Component.text("Unable to upload the debug report. See the server log for details.", NamedTextColor.RED)));
    }
}
