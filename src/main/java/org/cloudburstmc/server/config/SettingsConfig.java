package org.cloudburstmc.server.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;

import java.util.List;

@Data
@Setter(AccessLevel.PRIVATE)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SettingsConfig {

    @Builder.Default
    private boolean queryPlugins = true;

    @Builder.Default
    private String shutdownMessage = "Server closed";

    @Builder.Default
    private boolean forceLanguage = false;

    @Builder.Default
    private String language = "en_US";

    @Builder.Default
    private String asyncWorkers = "auto";

    @Builder.Default
    private boolean deprecatedVerbose = true;

}
