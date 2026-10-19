package org.cloudburstmc.server.configuration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class ConfigurationTemplatesTest {

    private static final Pattern PLACEHOLDER = Pattern.compile("%[A-Za-z0-9._-]+");

    @Test
    void allBundledTemplatesRenderForEveryAvailableLocale() throws Exception {
        String[] languages;
        try (InputStream stream = getClass().getResourceAsStream("/locale/cloudburst/languages.json")) {
            assertNotNull(stream);
            languages = JsonMapper.builder().build().readValue(stream, String[].class);
        }

        for (String language : languages) {
            for (String resource : new String[]{"cloudburst-global.yml", "cloudburst-level-defaults.yml", "cloudburst-level.yml"}) {
                String rendered = ConfigurationTemplates.render(resource, language);
                assertFalse(PLACEHOLDER.matcher(rendered).find(), resource + " in " + language);
            }
        }
    }

    @Test
    void untranslatedCommentsDoNotPreventConfigurationCreation() throws Exception {
        String rendered = ConfigurationTemplates.render("config/template-missing-comment.yml", "en_US");

        assertTrue(rendered.contains("# test.configuration.untranslated"));
        assertTrue(rendered.contains("language: \"en_US\""));
        assertFalse(PLACEHOLDER.matcher(rendered).find());
    }

    @Test
    void untranslatedValuesStillFailInsteadOfCreatingInvalidSettings() {
        assertThrows(IOException.class,
                () -> ConfigurationTemplates.render("config/template-missing-value.yml", "en_US"));
    }

    @Test
    void movedLevelSettingsRetainTheirLocalizedComments() throws Exception {
        String rendered = ConfigurationTemplates.render("cloudburst-level-defaults.yml", "fr_FR");

        assertTrue(rendered.contains("# Nombre de chunks envoyés aux joueurs par tick"));
        assertTrue(rendered.contains("# Rayon de chunks autour d'un joueur à cocher"));
    }

    @Test
    void defaultLocaleUsesTheUsEnglishCatalogue() throws Exception {
        String rendered = ConfigurationTemplates.render("cloudburst-global.yml", "en_US");

        assertTrue(rendered.contains("# Multi-language setting"));
        assertTrue(rendered.contains("language: \"en_US\""));
    }

    @Test
    void creationUsesTheChosenLocaleWithoutRewritingAnExistingFile(@TempDir Path directory) throws Exception {
        Path file = directory.resolve("config/cloudburst-global.yml");
        ConfigurationTemplates.ensure(file, "cloudburst-global.yml", "fr_FR", true);

        String original = Files.readString(file);
        assertTrue(original.contains("# Paramètres Multi-langues"));
        assertTrue(original.contains("language: \"fr_FR\""));

        ConfigurationTemplates.ensure(file, "cloudburst-global.yml", "en_US", true);
        assertEquals(original, Files.readString(file));
    }
}
