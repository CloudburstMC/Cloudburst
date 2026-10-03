package org.cloudburstmc.server.pack;

import org.cloudburstmc.api.util.SemVersion;

import java.util.UUID;

public record CloudPackDependency(UUID uuid, SemVersion version) {
}
