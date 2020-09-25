package org.cloudburstmc.server.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@AllArgsConstructor
@Builder
public class SettingsConfig {

    boolean queryPlugins;

    String shutdownMessage;

    boolean forceLanguage;

    String language;

    Object asyncWorkers;

}
