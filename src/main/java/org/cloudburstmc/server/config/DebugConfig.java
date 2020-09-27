package org.cloudburstmc.server.config;

import lombok.*;

import java.util.Collections;
import java.util.List;

@Data
@Setter(AccessLevel.PRIVATE)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DebugConfig {

    @Builder.Default
    private int level = 1;

    @Builder.Default
    private List<String> ignoredPackets = Collections.emptyList();

    @Builder.Default
    private boolean bugReport = true;

    @Builder.Default
    private boolean commands = false;

}
