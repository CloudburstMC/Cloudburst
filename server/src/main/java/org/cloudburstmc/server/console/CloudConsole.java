package org.cloudburstmc.server.console;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.minecrell.terminalconsole.TerminalConsoleAppender;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.event.server.ServerCommandEvent;
import org.cloudburstmc.server.Bootstrap;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.diagnostics.ServerTickStatistics;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@Log4j2
public class CloudConsole {

    private final CloudServer server;
    private final Object inputLock = new Object();
    private final BlockingQueue<Optional<String>> startupInput = new LinkedBlockingQueue<>();
    private boolean commandsEnabled;

    private boolean statusStarted;
    private long lastStatusNanos;
    private long lastBytesSent;
    private long lastBytesReceived;

    public void start() {
        try {
            Terminal terminal = TerminalConsoleAppender.getTerminal();
            if (terminal == null) {
                this.readStream();
            } else {
                this.readTerminal(terminal);
            }
        } catch (UserInterruptException interrupted) {
            this.server.shutdown();
        } catch (IOException failure) {
            log.error("Unable to read console input", failure);
        } finally {
            synchronized (this.inputLock) {
                if (!this.commandsEnabled) {
                    this.startupInput.add(Optional.empty());
                }
            }
        }
    }

    public String readStartupInput() throws IOException {
        synchronized (this.inputLock) {
            if (this.commandsEnabled) {
                throw new IllegalStateException("Console commands are already enabled");
            }
        }

        try {
            return this.startupInput.take().orElseThrow(() -> new EOFException("Console input closed. Specify --language for unattended first startup."));
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            InterruptedIOException failure = new InterruptedIOException("Console startup input interrupted");
            failure.initCause(interrupted);
            throw failure;
        }
    }

    public void enableCommands() {
        synchronized (this.inputLock) {
            if (this.commandsEnabled) {
                return;
            }

            this.startupInput.clear();
            this.commandsEnabled = true;
        }
    }

    private void readTerminal(Terminal terminal) {
        LineReader reader = this.createReader(terminal);
        TerminalConsoleAppender.setReader(reader);
        try {
            while (this.server.isRunning() && !Thread.currentThread().isInterrupted()) {
                String input;
                try {
                    input = reader.readLine("> ");
                } catch (EndOfFileException closed) {
                    return;
                }
                if (input == null) {
                    return;
                }
                this.acceptInput(input);
            }
        } finally {
            TerminalConsoleAppender.setReader(null);
        }
    }

    private LineReader createReader(Terminal terminal) {
        return LineReaderBuilder.builder()
                .terminal(terminal)
                .completer(new CloudConsoleCompleter(this.server))
                .appName("Cloudburst")
                .option(LineReader.Option.HISTORY_BEEP, false)
                .option(LineReader.Option.HISTORY_IGNORE_DUPS, true)
                .option(LineReader.Option.HISTORY_IGNORE_SPACE, true)
                .option(LineReader.Option.DISABLE_EVENT_EXPANSION, true)
                .option(LineReader.Option.INSERT_TAB, false)
                .build();
    }

    private void readStream() throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            String input;
            while (this.server.isRunning() && !Thread.currentThread().isInterrupted() && (input = reader.readLine()) != null) {
                this.acceptInput(input);
            }
        }
    }

    private void acceptInput(String input) {
        String command = input.strip();
        if (command.isEmpty() || !this.server.isRunning()) {
            return;
        }

        synchronized (this.inputLock) {
            if (!this.commandsEnabled) {
                this.startupInput.add(Optional.of(command));
                return;
            }
        }

        this.server.getGlobalScheduler().execute(null, () -> this.dispatchCommand(command));
    }

    private void dispatchCommand(String command) {
        if (!this.server.isRunning()) {
            return;
        }

        ServerCommandEvent event = new ServerCommandEvent(this.server.getConsoleSender(), command);
        this.server.getEventManager().fire(event);
        if (!event.isCancelled()) {
            this.server.dispatchCommand(event.getSender(), event.getCommand());
        }
    }

    public void updateStatus() {
        Terminal terminal = titleTerminal();
        if (terminal == null) {
            return;
        }

        long now = System.nanoTime();
        long sent = this.server.getNetwork().getBytesSent();
        long received = this.server.getNetwork().getBytesReceived();
        if (!this.statusStarted) {
            this.statusStarted = true;
            this.lastStatusNanos = now;
            this.lastBytesSent = sent;
            this.lastBytesReceived = received;
            return;
        }

        long elapsed = now - this.lastStatusNanos;
        if (elapsed < TimeUnit.SECONDS.toNanos(1)) {
            return;
        }

        double seconds = elapsed / 1_000_000_000d;
        double upload = (sent - this.lastBytesSent) / seconds / 1024d;
        double download = (received - this.lastBytesReceived) / seconds / 1024d;
        this.lastStatusNanos = now;
        this.lastBytesSent = sent;
        this.lastBytesReceived = received;

        Runtime runtime = Runtime.getRuntime();
        long usedHeap = runtime.totalMemory() - runtime.freeMemory();
        double heapUsage = usedHeap * 100d / runtime.maxMemory();
        ServerTickStatistics.Snapshot ticks = this.server.getTickStatistics().snapshot();
        writeTitle(terminal, String.format(Locale.ROOT,
                "%s %s | Online %d/%d | Memory %.0f%% | TPS %.1f | MSPT %.1f | U %.1f D %.1f KiB/s",
                this.server.getName(), this.server.getImplementationVersion(),
                this.server.getOnlinePlayers().size(), this.server.getMaxPlayers(), heapUsage,
                ticks.oneMinuteTps(), ticks.fiveSecondMspt().average(), upload, download));
    }

    public static void setWindowTitle(String title) {
        Terminal terminal = titleTerminal();
        if (terminal != null) {
            writeTitle(terminal, title);
        }
    }

    private static @Nullable Terminal titleTerminal() {
        if (Bootstrap.isTitleDisabled()) {
            return null;
        }

        Terminal terminal = TerminalConsoleAppender.getTerminal();
        if (terminal == null || terminal.getType().startsWith("dumb")) {
            return null;
        }

        return terminal;
    }

    private static void writeTitle(Terminal terminal, String title) {
        String safeTitle = title.replaceAll("\\p{javaISOControl}", "");
        terminal.writer().print("\033]0;" + safeTitle + "\007");
        terminal.writer().flush();
    }
}
