package org.cloudburstmc.server.diagnostics;

import com.sun.net.httpserver.HttpServer;
import org.cloudburstmc.server.Bootstrap;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class MclogsClientTest {

    @Test
    void uploadsUtf8JsonAndReturnsOnlyThePublicReportLink() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicReference<String> request = new AtomicReference<>();
        AtomicReference<String> method = new AtomicReference<>();
        AtomicReference<String> contentType = new AtomicReference<>();
        server.createContext("/log", exchange -> {
            try (exchange) {
                method.set(exchange.getRequestMethod());
                contentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
                request.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                byte[] response = "{\"success\":true,\"id\":\"Ab123\",\"token\":\"private-deletion-token\"}"
                        .getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
            }
        });

        server.start();

        try {
            MclogsClient client = new MclogsClient(URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/log"));
            String report = "Report with \"quotes\"\nUnicode: \u00e9";
            assertEquals(URI.create("https://mclo.gs/Ab123"), client.upload(report));
            assertEquals("POST", method.get());
            assertEquals("application/json; charset=utf-8", contentType.get());
            assertEquals(Bootstrap.JSON_MAPPER.valueToTree(Map.of("source", "Cloudburst", "content", report)),
                    Bootstrap.JSON_MAPPER.readTree(request.get()));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void rejectsFailedMalformedAndUnsafeResponsesWithoutExposingTheirContents() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicReference<String> body = new AtomicReference<>();
        server.createContext("/log", exchange -> {
            try (exchange) {
                exchange.getRequestBody().readAllBytes();
                byte[] response = body.get().getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
            }
        });

        server.createContext("/limited", exchange -> {
            try (exchange) {
                exchange.getRequestBody().readAllBytes();
                exchange.sendResponseHeaders(429, -1);
            }
        });

        server.start();

        try {
            URI endpoint = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/log");
            MclogsClient client = new MclogsClient(endpoint);
            for (String response : new String[]{
                    "{\"success\":false,\"error\":\"private-deletion-token\"}",
                    "{\"success\":true,\"id\":\"../private-deletion-token\"}",
                    "{\"success\":\"true\",\"id\":\"Ab123\"}",
                    "private-deletion-token",
                    "null"
            }) {
                body.set(response);
                IOException error = assertThrows(IOException.class, () -> client.upload("report"));
                assertFalse(error.toString().contains("private-deletion-token"));
                assertNull(error.getCause());
            }

            MclogsClient limited = new MclogsClient(endpoint.resolve("/limited"));
            assertThrows(IOException.class, () -> limited.upload("report"));
        } finally {
            server.stop(0);
        }
    }
}
