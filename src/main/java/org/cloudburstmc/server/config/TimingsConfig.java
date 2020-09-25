package org.cloudburstmc.server.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@AllArgsConstructor
@Builder
public class TimingsConfig {

    boolean enabled;

    boolean verbose;

    boolean privacy;

    int historyInterval;

    int historyLength;

    List<String> ignore;

    boolean bypassMax;

}
