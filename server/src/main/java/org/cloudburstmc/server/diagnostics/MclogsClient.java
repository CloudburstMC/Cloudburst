package org.cloudburstmc.server.diagnostics;

import org.cloudburstmc.server.Bootstrap;
import tools.jackson.databind.JsonNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;

public class MclogsClient {
    private static final URI ENDPOINT = URI.create("https://api.mclo.gs/1/log");
    private final URI endpoint;
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public MclogsClient() {
        this(ENDPOINT);
    }

    public MclogsClient(URI endpoint) {
        this.endpoint = Objects.requireNonNull(endpoint, "endpoint");
        if (!endpoint.isAbsolute() || endpoint.getHost() == null || !("https".equals(endpoint.getScheme()) || "http".equals(endpoint.getScheme()))) {
            throw new IllegalArgumentException("Log endpoint must be an absolute HTTP or HTTPS URI");
        }
    }

    public URI upload(String report) throws IOException, InterruptedException {
        String body = Bootstrap.JSON_MAPPER.writeValueAsString(Map.of("content", report, "source", "Cloudburst"));
        HttpRequest request = HttpRequest.newBuilder(this.endpoint)
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json; charset=utf-8")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = this.client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return readResponse(response.statusCode(), response.body());
    }

    private static URI readResponse(int status, String body) throws IOException {
        if (status != 200) {
            throw new IOException("mclo.gs returned HTTP " + status);
        }

        JsonNode response;
        try {
            response = Bootstrap.JSON_MAPPER.readTree(body);
        } catch (RuntimeException e) {
            throw new IOException("mclo.gs returned invalid JSON");
        }

        if (response == null || !response.isObject() || !response.path("success").isBoolean() || !response.path("success").asBoolean()) {
            throw new IOException("mclo.gs rejected the debug report");
        }

        JsonNode id = response.get("id");
        if (id == null || !id.isString() || !id.asString().matches("[A-Za-z0-9]+")) {
            throw new IOException("mclo.gs returned an invalid report ID");
        }

        return URI.create("https://mclo.gs/" + id.asString());
    }
}
