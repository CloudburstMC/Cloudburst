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

    private int level = 1;

    private List<String> ignoredPackets = Collections.emptyList();

    private boolean bugReport = true;

    private boolean commands = false;

}
