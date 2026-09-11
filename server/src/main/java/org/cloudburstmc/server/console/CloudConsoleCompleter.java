package org.cloudburstmc.server.console;

import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.command.CloudCommandSourceStack;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor
public class CloudConsoleCompleter implements Completer {
    private final CloudServer server;

    @Override
    public void complete(LineReader lineReader, ParsedLine parsedLine, List<Candidate> candidates) {
        if (this.server.getDefaultLevel() == null) {
            return;
        }

        String line = parsedLine.line();
        int cursor = parsedLine.cursor();
        if (line.startsWith("/")) {
            line = line.substring(1);
            cursor--;
        }

        Suggestions suggestions = this.suggestions(line, Math.max(cursor, 0));
        int suggestionStart = suggestions.getRange().getStart();
        for (Suggestion suggestion : suggestions.getList()) {
            if (suggestion.getText().isEmpty()) {
                continue;
            }

            String value = line.substring(suggestionStart, suggestion.getRange().getStart()) + suggestion.getText();
            String description = suggestion.getTooltip() == null ? null : suggestion.getTooltip().getString();
            candidates.add(new Candidate(value, value, null, description, null, null, false));
        }
    }

    private Suggestions suggestions(String commandLine, int cursor) {
        if (this.server.isPrimaryThread()) {
            return this.requestSuggestions(commandLine, cursor).join();
        }

        CompletableFuture<Suggestions> result = new CompletableFuture<>();
        this.server.getGlobalScheduler().execute(null, () -> {
            try {
                this.requestSuggestions(commandLine, cursor).whenComplete((suggestions, exception) -> {
                    if (exception == null) {
                        result.complete(suggestions);
                    } else {
                        result.completeExceptionally(exception);
                    }
                });
            } catch (RuntimeException exception) {
                result.completeExceptionally(exception);
            }
        });

        return result.join();
    }

    private CompletableFuture<Suggestions> requestSuggestions(String commandLine, int cursor) {
        return this.server.getCommandRegistry().suggestions(
                commandLine,
                cursor,
                CloudCommandSourceStack.from(this.server.getConsoleSender()));
    }
}
