package org.cloudburstmc.server.locale;

import com.google.common.collect.ImmutableSet;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.TranslationStore;
import org.cloudburstmc.server.Bootstrap;
import tools.jackson.databind.JsonNode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

@Log4j2
public final class LocaleManager {
    public static final Locale FALLBACK_LOCALE = Locale.US;

    private static final Pattern I18N_PATTERN = Pattern.compile("%([A-Za-z0-9._\\-]+)");
    private static final String LANG_FILE_EXTENSION = ".lang";
    private static final String FORMAT_CONVERSIONS = "sdf";
    private static final Locale CONSOLE_LOCALE = Locale.ENGLISH;

    @Getter
    private final Set<Locale> availableLocales;
    private final Map<Locale, String> displayNames;
    private final Properties texts = new Properties();
    private final List<LocaleTextSource> textSources;
    @Getter
    private Locale locale;
    private TranslationStore.StringBased<MessageFormat> adventureRegistry;

    private LocaleManager(Set<Locale> availableLocales, Map<Locale, String> displayNames, List<LocaleTextSource> textSources) {
        this.availableLocales = ImmutableSet.copyOf(availableLocales);
        this.displayNames = Map.copyOf(displayNames);
        this.textSources = List.copyOf(textSources);
    }

    public static LocaleManager from(String languagesPath, String... textPaths) {
        checkNotNull(languagesPath, "languagesPath");
        checkNotNull(textPaths, "textPaths");

        List<LocaleTextSource> sources = new ArrayList<>(textPaths.length);
        for (String textPath : textPaths) {
            sources.add(new ClasspathLocaleTextSource(checkNotNull(textPath, "textPath")));
        }

        return new LocaleManager(loadClasspathLocales(languagesPath), loadClasspathDisplayNames(languagesPath), sources);
    }

    public static LocaleManager from(FileSystem fs, String languagesPath, String... textPaths) {
        checkNotNull(fs, "fs");
        checkNotNull(languagesPath, "languagesPath");
        checkNotNull(textPaths, "textPaths");

        List<LocaleTextSource> sources = new ArrayList<>(textPaths.length);
        for (String textPath : textPaths) {
            sources.add(new PathLocaleTextSource(fs.getPath(checkNotNull(textPath, "textPath"))));
        }

        Path languagePath = fs.getPath(languagesPath);
        return new LocaleManager(loadPathLocales(languagePath), loadPathDisplayNames(languagePath), sources);
    }

    public static LocaleManager from(Path languagesPath, Path... textPaths) {
        checkNotNull(textPaths, "textPaths");

        List<LocaleTextSource> sources = new ArrayList<>(textPaths.length);
        for (Path textPath : textPaths) {
            sources.add(new PathLocaleTextSource(checkNotNull(textPath, "textPath")));
        }

        return new LocaleManager(loadPathLocales(languagesPath), loadPathDisplayNames(languagesPath), sources);
    }

    public static LocaleManager from(Set<Locale> availableLocales, Path... textPaths) {
        checkNotNull(availableLocales, "availableLocales");
        checkNotNull(textPaths, "textPaths");

        List<LocaleTextSource> sources = new ArrayList<>(textPaths.length);
        for (Path textPath : textPaths) {
            sources.add(new PathLocaleTextSource(checkNotNull(textPath, "textPath")));
        }

        return new LocaleManager(availableLocales, Map.of(), sources);
    }

    private static ImmutableSet<Locale> loadClasspathLocales(String resourcePath) {
        checkNotNull(resourcePath, "resourcePath");
        try (InputStream stream = ClasspathLocaleTextSource.openResource(resourcePath)) {
            return readLocales(stream);
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to load language list", e);
        }
    }

    private static ImmutableSet<Locale> loadPathLocales(Path path) {
        log.debug(path.toAbsolutePath());
        try (InputStream stream = Files.newInputStream(path)) {
            return readLocales(stream);
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to load language list", e);
        }
    }

    private static Map<Locale, String> loadClasspathDisplayNames(String languagesPath) {
        try (InputStream stream = ClasspathLocaleTextSource.openResource(languageNamesPath(languagesPath))) {
            return readDisplayNames(stream);
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to load language display names", e);
        }
    }

    private static Map<Locale, String> loadPathDisplayNames(Path languagesPath) {
        Path namesPath = languagesPath.resolveSibling("language_names.json");
        if (Files.notExists(namesPath)) {
            return Map.of();
        }

        try (InputStream stream = Files.newInputStream(namesPath)) {
            return readDisplayNames(stream);
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to load language display names", e);
        }
    }

    private static Map<Locale, String> readDisplayNames(InputStream stream) throws IOException {
        Map<Locale, String> displayNames = new HashMap<>();
        JsonNode array = Bootstrap.JSON_MAPPER.readTree(stream);

        for (JsonNode element : array) {
            checkArgument(element.size() == 2, "Language display name entries must contain locale and display name");
            displayNames.put(parseLocale(element.get(0).asString()), element.get(1).asString());
        }

        return displayNames;
    }

    private static String languageNamesPath(String languagesPath) {
        int slash = languagesPath.lastIndexOf('/');
        String prefix = slash == -1 ? "" : languagesPath.substring(0, slash + 1);
        return prefix + "language_names.json";
    }

    private static ImmutableSet<Locale> readLocales(InputStream stream) throws IOException {
        ImmutableSet.Builder<Locale> builder = ImmutableSet.builder();
        JsonNode array = Bootstrap.JSON_MAPPER.readTree(stream);
        for (JsonNode element : array) {
            builder.add(parseLocale(element.asString()));
        }
        return builder.build();
    }

    private static Locale parseLocale(String localeString) {
        checkNotNull(localeString, "localeString");
        String[] codes = localeString.split("[_-]", -1);
        checkArgument(codes.length == 2 && !codes[0].isBlank() && !codes[1].isBlank(), "Invalid language country code");
        return Locale.of(codes[0], codes[1]);
    }

    public void setLocaleOrFallback(String localeString) {
        if (!setLocale(localeString)) {
            this.setLocale(FALLBACK_LOCALE);
        }
    }

    public boolean setLocale(String localeString) {
        try {
            Locale locale = parseLocale(localeString);
            this.setLocale(locale);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void loadTransforms(InputStream stream) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isEmpty() || line.charAt(0) == '#') {
                    continue;
                }
                int equals = line.indexOf('=');
                if (equals == -1) {
                    continue;
                }
                String key = line.substring(0, equals);
                String value = line.substring(equals + 1);
                this.texts.setProperty(key, value);
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to load transforms", e);
        }
    }

    public String translate(String string, Object... objects) {
        return this.translate(string, null, objects);
    }

    public String translateOnly(String prefix, String string, Object... objects) {
        return this.translate(string, checkNotNull(prefix, "prefix"), objects);
    }

    private String translate(String string, String prefix, Object... objects) {
        checkNotNull(string, "string");
        if (string.isEmpty()) {
            return string;
        }

        TranslationKey key = this.findTranslationKey(string);
        if (!this.canTranslate(key.value(), prefix)) {
            return string;
        }

        Object[] args = this.resolveArguments(objects);
        String translated = this.format(this.texts.getProperty(key.value()), args);
        return string.replace(key.token(), translated);
    }

    private TranslationKey findTranslationKey(String string) {
        Matcher matcher = I18N_PATTERN.matcher(string);
        if (matcher.find()) {
            return new TranslationKey(matcher.group(), matcher.group(1));
        }
        return new TranslationKey(string, string);
    }

    private boolean canTranslate(String key, String prefix) {
        return this.texts.containsKey(key) && (prefix == null || key.startsWith(prefix));
    }

    private Object[] resolveArguments(Object[] objects) {
        Object[] args = objects == null ? new Object[0] : Arrays.copyOf(objects, objects.length);
        for (int i = 0; i < args.length; i++) {
            String arg = Objects.toString(args[i], "");
            Matcher argMatcher = I18N_PATTERN.matcher(arg);
            if (argMatcher.matches()) {
                String match = argMatcher.group(1);
                if (this.canTranslate(match, null)) {
                    args[i] = this.texts.getProperty(match);
                }
            }
        }
        return args;
    }

    private String format(String pattern, Object[] args) {
        return new MessageFormat(toMessageFormatPattern(pattern), this.localeOrFallback()).format(args);
    }

    public String get(String key) {
        return this.texts.getProperty(key);
    }

    public String getDisplayName(Locale locale) {
        checkNotNull(locale, "locale");
        return this.displayNames.getOrDefault(locale, locale.getDisplayName(locale));
    }

    public String getConsoleDisplayName(Locale locale) {
        checkNotNull(locale, "locale");
        return locale.getDisplayName(CONSOLE_LOCALE);
    }

    public synchronized void setLocale(Locale locale) {
        checkNotNull(locale, "locale");
        checkArgument(this.availableLocales.contains(locale), "locale is not available");
        if (this.locale != null && this.locale.equals(locale)) {
            return; // Same locale
        }
        this.locale = locale;

        this.texts.clear(); // Clear any existing

        for (LocaleTextSource source : this.textSources) {
            try {
                this.loadTransforms(source.open(locale));
            } catch (IOException e) {
                throw new IllegalArgumentException("Unable to load transforms", e);
            }
        }

        this.texts.setProperty("language", locale.toString());

        if (this.adventureRegistry != null) {
            GlobalTranslator.translator().removeSource(this.adventureRegistry);
        }

        this.adventureRegistry = TranslationStore.messageFormat(Key.key("cloudburst", "locale"));

        for (String key : this.texts.stringPropertyNames()) {
            String value = this.texts.getProperty(key);
            this.adventureRegistry.register(key, locale, new MessageFormat(toMessageFormatPattern(value), locale));
        }

        GlobalTranslator.translator().addSource(this.adventureRegistry);
    }

    private Locale localeOrFallback() {
        return this.locale != null ? this.locale : FALLBACK_LOCALE;
    }

    private static String toMessageFormatPattern(String value) {
        StringBuilder pattern = new StringBuilder(value.length());
        int implicitArgument = 0;

        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (current == '\'') {
                pattern.append("''");
                continue;
            }

            if (current == '{') {
                pattern.append("'{'");
                continue;
            }

            if (current == '}') {
                pattern.append("'}'");
                continue;
            }

            if (current != '%' || index + 1 >= value.length()) {
                pattern.append(current);
                continue;
            }

            char next = value.charAt(index + 1);
            if (next == '%') {
                pattern.append('%');
                index++;
                continue;
            }

            int numberStart = index + 1;
            int numberEnd = numberStart;
            while (numberEnd < value.length() && Character.isDigit(value.charAt(numberEnd))) {
                numberEnd++;
            }

            if (numberEnd > numberStart) {
                int number = Integer.parseInt(value.substring(numberStart, numberEnd));
                int argument = number - 1;
                if (argument >= 0 && numberEnd < value.length() && value.charAt(numberEnd) == '$') {
                    int conversionIndex = findFormatConversion(value, numberEnd + 1);
                    if (conversionIndex != -1) {
                        pattern.append('{').append(argument).append('}');
                        index = conversionIndex;
                        continue;
                    }
                }

                if (argument >= 0 && numberEnd < value.length() && isFormatConversion(value.charAt(numberEnd))) {
                    pattern.append('{').append(implicitArgument++).append('}');
                    index = numberEnd;
                    continue;
                }

                if (argument >= 0) {
                    pattern.append('{').append(argument).append('}');
                    index = numberEnd - 1;
                    continue;
                }
            }

            if (isFormatConversion(next) && !isTranslationMarker(value, index)) {
                pattern.append('{').append(implicitArgument++).append('}');
                index++;
                continue;
            }

            pattern.append(current);
        }

        return pattern.toString();
    }

    private static boolean isFormatConversion(char value) {
        return FORMAT_CONVERSIONS.indexOf(Character.toLowerCase(value)) != -1;
    }

    private static int findFormatConversion(String value, int startIndex) {
        for (int index = startIndex; index < value.length(); index++) {
            char current = value.charAt(index);
            if (isFormatConversion(current)) {
                return index;
            }

            if (!isFormatModifier(current)) {
                return -1;
            }
        }

        return -1;
    }

    private static boolean isFormatModifier(char value) {
        return Character.isDigit(value) || value == '-' || value == '+' || value == '#'
                || value == ' ' || value == ',' || value == '(' || value == '.';
    }

    private static boolean isTranslationMarker(String value, int percentIndex) {
        for (int index = percentIndex + 2; index < value.length(); index++) {
            char current = value.charAt(index);
            if (current == '.') {
                return true;
            }
            if (!Character.isLetterOrDigit(current) && current != '_' && current != '-') {
                return false;
            }
        }
        return false;
    }

    private record TranslationKey(String token, String value) {
    }

    private interface LocaleTextSource {
        InputStream open(Locale locale) throws IOException;
    }

    private record ClasspathLocaleTextSource(String path) implements LocaleTextSource {
        private static InputStream openResource(String path) {
            String resourcePath = path.startsWith("/") ? path.substring(1) : path;
            InputStream stream = LocaleManager.class.getClassLoader().getResourceAsStream(resourcePath);
            if (stream == null) {
                throw new IllegalArgumentException("Missing resource: " + resourcePath);
            }
            return stream;
        }

        @Override
        public InputStream open(Locale locale) {
            return openResource(this.path + "/" + locale + LANG_FILE_EXTENSION);
        }
    }

    private record PathLocaleTextSource(Path path) implements LocaleTextSource {
        @Override
        public InputStream open(Locale locale) throws IOException {
            return Files.newInputStream(this.path.resolve(locale + LANG_FILE_EXTENSION));
        }
    }
}
