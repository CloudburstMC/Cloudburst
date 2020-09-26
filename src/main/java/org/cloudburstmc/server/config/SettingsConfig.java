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

    private boolean queryPlugins = true;

    private String shutdownMessage = "Server closed";

    private boolean forceLanguage = false;

    private String language = "en_US";

    private String asyncWorkers = "auto";

    private boolean deprecatedVerbose = true;

}
