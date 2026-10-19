package org.cloudburstmc.server.configuration;

import org.cloudburstmc.api.configuration.ConfigurationLoader;
import org.cloudburstmc.api.configuration.YamlConfigurationOptions;
import org.cloudburstmc.server.configuration.file.YamlConfigurationLoader;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class YamlConfigurationLimitsTest {

    @TempDir
    Path directory;

    @Test
    void oversizedFilesAndWhitespaceOnlyDocumentsAreRejected() throws IOException {
        ConfigurationLoader loader = new YamlConfigurationLoader().withOptions(new YamlConfigurationOptions().codePointLimit(8));
        Path file = directory.resolve("large.yml");
        Files.writeString(file, " ".repeat(100));

        assertThrows(IOException.class, () -> loader.load(file));
        assertThrows(IOException.class, () -> loader.parse(" ".repeat(9)));
    }

    @Test
    void oversizedReadersAreStoppedWithoutBeingClosed() {
        ConfigurationLoader loader = new YamlConfigurationLoader().withOptions(new YamlConfigurationOptions().codePointLimit(8));
        CountingReader reader = new CountingReader(" ".repeat(20000));

        assertThrows(IOException.class, () -> loader.load(reader));
        assertTrue(reader.charactersRead < 20000);
        assertFalse(reader.closed);
    }

    @Test
    void supplementaryCharactersCountOnceAcrossReadBoundaries() throws IOException {
        String document = "value: '\uD83D\uDE00'";
        int limit = document.codePointCount(0, document.length());
        ConfigurationLoader loader = new YamlConfigurationLoader().withOptions(new YamlConfigurationOptions().codePointLimit(limit));
        StringReader reader = new StringReader(document) {
            @Override
            public int read(char @NotNull [] buffer, int offset, int length) throws IOException {
                return super.read(buffer, offset, Math.min(length, 1));
            }
        };

        assertEquals("\uD83D\uDE00", loader.load(reader).getString("value"));
    }

    private static class CountingReader extends StringReader {
        private int charactersRead;
        private boolean closed;

        private CountingReader(String text) {
            super(text);
        }

        @Override
        public int read(char @NotNull [] buffer, int offset, int length) throws IOException {
            int count = super.read(buffer, offset, length);
            if (count > 0) {
                charactersRead += count;
            }

            return count;
        }

        @Override
        public void close() {
            closed = true;
            super.close();
        }
    }
}
