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

    @Builder.Default
    private boolean enabled = false;

    @Builder.Default
    private boolean verbose = false;

    @Builder.Default
    private boolean privacy = false;

    @Builder.Default
    private int historyInterval = 6000;

    @Builder.Default
    private int historyLength = 72000;

    @Builder.Default
    private List<String> ignore = Collections.emptyList();

    @Builder.Default
    private boolean bypassMax = false;

}
