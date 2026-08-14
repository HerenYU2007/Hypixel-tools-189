package com.example.fireballpredictor.share;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public final class ShareServer {
    private static final int DEFAULT_PORT = 18989;
    private static final String DEFAULT_ROOM = "default";
    private static final long DEFAULT_TTL_MILLIS = 10L * 60L * 1000L;
    private static final int DEFAULT_MAX_SAMPLES_PER_GAME = 600;
    private static final int MAX_BODY_BYTES = 16 * 1024;

    private final Object lock = new Object();
    private final Map<String, List<Sample>> samplesByGame = new LinkedHashMap<String, List<Sample>>();
    private final long ttlMillis;
    private final int maxSamplesPerGame;

    private ShareServer(long ttlMillis, int maxSamplesPerGame) {
        this.ttlMillis = ttlMillis;
        this.maxSamplesPerGame = maxSamplesPerGame;
    }

    public static void main(String[] args) throws Exception {
        Config config = Config.parse(args);
        ShareServer app = new ShareServer(config.ttlMillis, config.maxSamplesPerGame);
        HttpServer server = HttpServer.create(new InetSocketAddress(config.host, config.port), 0);
        server.createContext("/health", new HealthHandler());
        server.createContext("/v1/push", app.new PushHandler());
        server.createContext("/v1/pull", app.new PullHandler());
        server.createContext("/v1/clear", app.new ClearHandler());
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        System.out.println("Fireball share server listening on http://" + config.host + ":" + config.port);
        System.out.println("Endpoints: GET /health, POST /v1/push, GET /v1/pull?room=...&gameId=...");
    }

    private final class PushHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleCors(exchange)) {
                return;
            }
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                send(exchange, 405, jsonError("method_not_allowed"));
                return;
            }
            String body = readBody(exchange);
            if (!looksLikeJsonObject(body)) {
                send(exchange, 400, jsonError("invalid_json_object"));
                return;
            }
            String room = firstNonEmpty(query(exchange).get("room"), jsonString(body, "room"));
            String gameId = firstNonEmpty(query(exchange).get("gameId"), jsonString(body, "gameId"));
            String senderId = firstNonEmpty(query(exchange).get("senderId"), jsonString(body, "senderId"));
            room = normalizeRoom(room);
            if (isBlank(gameId)) {
                send(exchange, 400, jsonError("missing_gameId"));
                return;
            }

            long now = System.currentTimeMillis();
            String key = gameKey(room, gameId);
            String enriched = enrichJson(body, now);
            synchronized (lock) {
                pruneLocked(now);
                List<Sample> samples = samplesByGame.get(key);
                if (samples == null) {
                    samples = new ArrayList<Sample>();
                    samplesByGame.put(key, samples);
                }
                samples.add(new Sample(now, senderId, enriched));
                while (samples.size() > maxSamplesPerGame) {
                    samples.remove(0);
                }
            }
            send(exchange, 200, "{\"ok\":true,\"serverTime\":" + now + "}");
        }
    }

    private final class PullHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleCors(exchange)) {
                return;
            }
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                send(exchange, 405, jsonError("method_not_allowed"));
                return;
            }
            Map<String, String> q = query(exchange);
            String room = normalizeRoom(q.get("room"));
            String gameId = q.get("gameId");
            String sinceText = q.get("since");
            String excludeSenderId = q.get("excludeSenderId");
            if (isBlank(gameId)) {
                send(exchange, 400, jsonError("missing_gameId"));
                return;
            }
            long since = parseLong(sinceText, 0L);
            long now = System.currentTimeMillis();
            List<String> result = new ArrayList<String>();
            synchronized (lock) {
                pruneLocked(now);
                List<Sample> samples = samplesByGame.get(gameKey(room, gameId));
                if (samples != null) {
                    for (Sample sample : samples) {
                        if (sample.serverTime <= since) {
                            continue;
                        }
                        if (!isBlank(excludeSenderId) && excludeSenderId.equals(sample.senderId)) {
                            continue;
                        }
                        result.add(sample.json);
                    }
                }
            }
            StringBuilder json = new StringBuilder();
            json.append("{\"ok\":true,\"serverTime\":").append(now).append(",\"samples\":[");
            for (int i = 0; i < result.size(); i++) {
                if (i > 0) {
                    json.append(',');
                }
                json.append(result.get(i));
            }
            json.append("]}");
            send(exchange, 200, json.toString());
        }
    }

    private final class ClearHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleCors(exchange)) {
                return;
            }
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                send(exchange, 405, jsonError("method_not_allowed"));
                return;
            }
            Map<String, String> q = query(exchange);
            String room = normalizeRoom(q.get("room"));
            String gameId = q.get("gameId");
            synchronized (lock) {
                if (!isBlank(room) && !isBlank(gameId)) {
                    samplesByGame.remove(gameKey(room, gameId));
                } else if (!isBlank(room)) {
                    Iterator<String> iterator = samplesByGame.keySet().iterator();
                    String prefix = room + "|";
                    while (iterator.hasNext()) {
                        if (iterator.next().startsWith(prefix)) {
                            iterator.remove();
                        }
                    }
                }
            }
            send(exchange, 200, "{\"ok\":true}");
        }
    }

    private static final class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleCors(exchange)) {
                return;
            }
            send(exchange, 200, "{\"ok\":true,\"name\":\"fireball-share-server\"}");
        }
    }

    private void pruneLocked(long now) {
        Iterator<Map.Entry<String, List<Sample>>> games = samplesByGame.entrySet().iterator();
        while (games.hasNext()) {
            Map.Entry<String, List<Sample>> entry = games.next();
            List<Sample> samples = entry.getValue();
            Iterator<Sample> iterator = samples.iterator();
            while (iterator.hasNext()) {
                if (now - iterator.next().serverTime > ttlMillis) {
                    iterator.remove();
                }
            }
            if (samples.isEmpty()) {
                games.remove();
            }
        }
    }

    private static boolean handleCors(HttpExchange exchange) throws IOException {
        Headers headers = exchange.getResponseHeaders();
        headers.set("Access-Control-Allow-Origin", "*");
        headers.set("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
        headers.set("Access-Control-Allow-Headers", "Content-Type");
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return true;
        }
        return false;
    }

    private static void send(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        OutputStream output = exchange.getResponseBody();
        try {
            output.write(bytes);
        } finally {
            output.close();
            exchange.close();
        }
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        InputStream input = exchange.getRequestBody();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int total = 0;
        int read;
        while ((read = input.read(buffer)) >= 0) {
            total += read;
            if (total > MAX_BODY_BYTES) {
                throw new IOException("request body too large");
            }
            output.write(buffer, 0, read);
        }
        return new String(output.toByteArray(), StandardCharsets.UTF_8).trim();
    }

    private static Map<String, String> query(HttpExchange exchange) throws IOException {
        Map<String, String> result = new LinkedHashMap<String, String>();
        String raw = exchange.getRequestURI().getRawQuery();
        if (raw == null || raw.length() == 0) {
            return result;
        }
        String[] parts = raw.split("&");
        for (String part : parts) {
            int eq = part.indexOf('=');
            String key = eq < 0 ? part : part.substring(0, eq);
            String value = eq < 0 ? "" : part.substring(eq + 1);
            result.put(urlDecode(key), urlDecode(value));
        }
        return result;
    }

    private static String jsonString(String json, String field) {
        String needle = "\"" + field + "\"";
        int index = json.indexOf(needle);
        if (index < 0) {
            return "";
        }
        int colon = json.indexOf(':', index + needle.length());
        if (colon < 0) {
            return "";
        }
        int start = json.indexOf('"', colon + 1);
        if (start < 0) {
            return "";
        }
        StringBuilder value = new StringBuilder();
        boolean escaped = false;
        for (int i = start + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escaped) {
                value.append(c);
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '"') {
                return value.toString();
            } else {
                value.append(c);
            }
        }
        return "";
    }

    private static String enrichJson(String json, long serverTime) {
        String trimmed = json.trim();
        String withoutEnd = trimmed.substring(0, trimmed.length() - 1).trim();
        if (withoutEnd.endsWith("{")) {
            return withoutEnd + "\"serverTime\":" + serverTime + "}";
        }
        return withoutEnd + ",\"serverTime\":" + serverTime + "}";
    }

    private static boolean looksLikeJsonObject(String body) {
        return body != null && body.startsWith("{") && body.endsWith("}");
    }

    private static String gameKey(String room, String gameId) {
        return room + "|" + gameId;
    }

    private static String firstNonEmpty(String a, String b) {
        return !isBlank(a) ? a : b;
    }

    private static String normalizeRoom(String room) {
        return isBlank(room) ? DEFAULT_ROOM : room.trim();
    }

    private static boolean isBlank(String text) {
        return text == null || text.trim().length() == 0;
    }

    private static long parseLong(String text, long fallback) {
        if (isBlank(text)) {
            return fallback;
        }
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static String urlDecode(String text) throws IOException {
        return URLDecoder.decode(text, "UTF-8");
    }

    private static String jsonError(String code) {
        return "{\"ok\":false,\"error\":\"" + code + "\"}";
    }

    private static final class Sample {
        final long serverTime;
        final String senderId;
        final String json;

        Sample(long serverTime, String senderId, String json) {
            this.serverTime = serverTime;
            this.senderId = senderId;
            this.json = json;
        }
    }

    private static final class Config {
        String host = "0.0.0.0";
        int port = DEFAULT_PORT;
        long ttlMillis = DEFAULT_TTL_MILLIS;
        int maxSamplesPerGame = DEFAULT_MAX_SAMPLES_PER_GAME;

        static Config parse(String[] args) {
            Config config = new Config();
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if ("--host".equals(arg) && i + 1 < args.length) {
                    config.host = args[++i];
                } else if ("--port".equals(arg) && i + 1 < args.length) {
                    config.port = (int) parseLong(args[++i], DEFAULT_PORT);
                } else if ("--ttl-seconds".equals(arg) && i + 1 < args.length) {
                    config.ttlMillis = parseLong(args[++i], DEFAULT_TTL_MILLIS / 1000L) * 1000L;
                } else if ("--max-samples".equals(arg) && i + 1 < args.length) {
                    config.maxSamplesPerGame = (int) parseLong(args[++i], DEFAULT_MAX_SAMPLES_PER_GAME);
                }
            }
            return config;
        }
    }
}
