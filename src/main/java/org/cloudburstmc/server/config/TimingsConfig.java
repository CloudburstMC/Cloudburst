package org.cloudburstmc.server.config;

import lombok.*;

import java.util.Collections;
import java.util.List;

@Data
@Setter(AccessLevel.PRIVATE)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TimingsConfig {

    private boolean enabled = false;

    private boolean verbose = false;

    private boolean privacy = false;

    private int historyInterval = 6000;

    private int historyLength = 72000;

    private List<String> ignore = Collections.emptyList();

    private boolean bypassMax = false;

}
