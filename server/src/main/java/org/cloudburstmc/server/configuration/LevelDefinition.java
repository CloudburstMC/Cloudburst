package org.cloudburstmc.server.configuration;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import lombok.*;
import org.cloudburstmc.server.configuration.type.LevelDimension;
import org.cloudburstmc.server.configuration.validation.SettingConstraint;
import org.cloudburstmc.server.level.provider.migration.LevelDBImportProfile;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

/**
 * Startup configuration for one world in the global configuration's levels map.
 * The map key identifies the world and its storage directory.
 */
@Getter
@Builder
@EqualsAndHashCode
@ToString
@AllArgsConstructor
@NoArgsConstructor
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
public class LevelDefinition {

    /**
     * Required dimension. Worlds whose dimension is disabled are not loaded.
     */
    private LevelDimension dimension;

    /**
     * Numeric or text seed used when saved metadata supplies none.
     * An absent seed is derived from the world name.
     */
    @SettingConstraint(optional = true, whole = true, types = {String.class, Number.class})
    private Object seed;

    /**
     * Registered generator identifier for new worlds and new terrain in imported worlds.
     * An absent value selects the server's fallback generator.
     */
    @SettingConstraint(optional = true, nonBlank = true)
    private String generator;

    /**
     * Options passed to the selected generator. An absent value uses an empty string.
     */
    @SettingConstraint(optional = true)
    private String options;

    /**
     * Explicit source LevelDB encoding when detection is insufficient.
     * An absent value enables detection. Native storage is not an import profile.
     */
    @SettingConstraint(optional = true)
    private LevelDBImportProfile importProfile;
}
