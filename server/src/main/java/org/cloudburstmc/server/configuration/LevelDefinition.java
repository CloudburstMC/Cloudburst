package org.cloudburstmc.server.configuration;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import lombok.*;
import org.cloudburstmc.server.configuration.type.LevelDimension;
import org.cloudburstmc.server.configuration.validation.SettingConstraint;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

/**
 * The startup dimension, seed, and generator selection for one level.
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

    private LevelDimension dimension;

    @SettingConstraint(optional = true, whole = true, types = {String.class, Number.class})
    private Object seed;

    @SettingConstraint(optional = true, nonBlank = true)
    private String generator;

    @SettingConstraint(optional = true)
    private String options;
}
