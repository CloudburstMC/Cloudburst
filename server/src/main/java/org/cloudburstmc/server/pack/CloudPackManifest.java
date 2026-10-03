package org.cloudburstmc.server.pack;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.cloudburstmc.api.Server;
import org.cloudburstmc.api.util.SemVersion;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CloudPackManifest {

    @JsonProperty("format_version")
    private int formatVersion;
    private Header header;
    private List<Module> modules = List.of();
    private List<Dependency> dependencies = List.of();

    public static CloudPackManifest load(InputStream stream) throws IOException {
        return Server.JSON_MAPPER.readValue(stream, CloudPackManifest.class);
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class Header {
        private String name;
        private String description;
        private UUID uuid;
        private SemVersion version;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class Module {
        private UUID uuid;
        private SemVersion version;
        private String type;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class Dependency {
        private UUID uuid;
        private SemVersion version;
    }
}
