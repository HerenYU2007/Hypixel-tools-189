package com.example.fireballpredictor.share;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ShareServer {
    private static final int DEFAULT_PORT = 18989;
    private static final String DEFAULT_ROOM = "default";
    private static final long DEFAULT_TTL_MILLIS = 10L * 60L * 1000L;
    private static final int DEFAULT_MAX_SAMPLES_PER_GAME = 600;
    private static final int MAX_LINE_CHARS = 32 * 1024;

    private final Object lock = new Object();
    private final Object logLock = new Object();
    private final Map<String, List<Sample>> samplesByGame = new LinkedHashMap<String, List<Sample>>();
    private final long ttlMillis;
    private final int maxSamplesPerGame;
    private final File logDir;
    private final ExecutorService workers = Executors.newCachedThreadPool();

    private ShareServer(long ttlMillis, int maxSamplesPerGame, File logDir) {
        this.ttlMillis = ttlMillis;
        this.maxSamplesPerGame = maxSamplesPerGame;
        this.logDir = logDir;
    }

    public static void main(String[] args) throws Exception {
        Config config = Config.parse(args);
        ShareServer app = new ShareServer(config.ttlMillis, config.maxSamplesPerGame, config.logDir);
        ServerSocket server = new ServerSocket();
        server.bind(new InetSocketAddress(config.host, config.port));
        System.out.println("Fireball share server listening on tcp://" + config.host + ":" + config.port);
        System.out.println("Sample logs: " + config.logDir.getAbsolutePath());
        System.out.println("Protocol: PING | PUSH <json> | PULL <gameId> [since] [excludeSenderId] [room] | CLEAR [gameId] [room]");
        while (true) {
            final Socket socket = server.accept();
            app.workers.execute(new Runnable() {
                @Override
                public void run() {
                    app.handle(socket);
                }
            });
        }
    }

    private void handle(Socket socket) {
        BufferedReader input = null;
        BufferedWriter output = null;
        try {
            socket.setSoTimeout(8000);
            input = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            output = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
            String line = input.readLine();
            if (line == null) {
                writeLine(output, jsonError("empty_request"));
                return;
            }
            if (line.length() > MAX_LINE_CHARS) {
                writeLine(output, jsonError("request_too_large"));
                return;
            }
            writeLine(output, handleLine(stripBom(line).trim()));
        } catch (Throwable t) {
            try {
                if (output != null) {
                    writeLine(output, jsonError("server_error"));
                }
            } catch (IOException ignored) {
            }
        } finally {
            closeQuietly(input);
            closeQuietly(output);
            closeQuietly(socket);
        }
    }

    private String handleLine(String line) {
        if (line.length() == 0) {
            return jsonError("empty_request");
        }
        if ("PING".equalsIgnoreCase(line)) {
            return "{\"ok\":true,\"name\":\"fireball-share-server\",\"protocol\":\"tcp\"}";
        }
        if (startsWithCommand(line, "PUSH")) {
            return handlePush(commandPayload(line, "PUSH"));
        }
        if (startsWithCommand(line, "PULL")) {
            return handlePull(commandParts(line, "PULL"));
        }
        if (startsWithCommand(line, "CLEAR")) {
            return handleClear(commandParts(line, "CLEAR"));
        }
        return jsonError("unknown_command");
    }

    private String handlePush(String body) {
        if (!looksLikeJsonObject(body)) {
            return jsonError("invalid_json_object");
        }
        String room = normalizeRoom(jsonString(body, "room"));
        String gameId = jsonString(body, "gameId");
        String senderId = jsonString(body, "senderId");
        if (isBlank(gameId)) {
            return jsonError("missing_gameId");
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
        appendSampleLog(room, gameId, enriched);
        return "{\"ok\":true,\"serverTime\":" + now + "}";
    }

    private void appendSampleLog(String room, String gameId, String json) {
        synchronized (logLock) {
            BufferedWriter writer = null;
            try {
                File roomDir = new File(logDir, safePathPart(room));
                if (!roomDir.isDirectory() && !roomDir.mkdirs()) {
                    throw new IOException("failed to create log dir: " + roomDir);
                }
                File file = new File(roomDir, safePathPart(gameId) + ".jsonl");
                writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, true), StandardCharsets.UTF_8));
                writer.write(json);
                writer.write('\n');
                writer.flush();
            } catch (Throwable t) {
                System.err.println("Failed to append sample log for " + room + "/" + gameId + ": " + t);
            } finally {
                closeQuietly(writer);
            }
        }
    }

    private String handlePull(String[] parts) {
        if (parts.length < 1 || isBlank(parts[0])) {
            return jsonError("missing_gameId");
        }
        String gameId = parts[0];
        long since = parts.length >= 2 ? parseLong(parts[1], 0L) : 0L;
        String excludeSenderId = parts.length >= 3 ? parts[2] : "";
        String room = parts.length >= 4 ? normalizeRoom(parts[3]) : DEFAULT_ROOM;

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
        return json.toString();
    }

    private String handleClear(String[] parts) {
        String gameId = parts.length >= 1 ? parts[0] : "";
        String room = parts.length >= 2 ? normalizeRoom(parts[1]) : DEFAULT_ROOM;
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
        return "{\"ok\":true}";
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

    private static boolean startsWithCommand(String line, String command) {
        return line.equalsIgnoreCase(command)
                || line.regionMatches(true, 0, command + " ", 0, command.length() + 1)
                || line.regionMatches(true, 0, command + "\t", 0, command.length() + 1);
    }

    private static String stripBom(String text) {
        if (text != null && text.length() > 0 && text.charAt(0) == '\uFEFF') {
            return text.substring(1);
        }
        return text;
    }

    private static String commandPayload(String line, String command) {
        if (line.length() <= command.length()) {
            return "";
        }
        return line.substring(command.length()).trim();
    }

    private static String[] commandParts(String line, String command) {
        String payload = commandPayload(line, command);
        if (payload.length() == 0) {
            return new String[0];
        }
        return payload.split("[\\t ]+");
    }

    private static void writeLine(BufferedWriter output, String line) throws IOException {
        output.write(line);
        output.write('\n');
        output.flush();
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

    private static String normalizeRoom(String room) {
        return isBlank(room) ? DEFAULT_ROOM : room.trim();
    }

    private static String safePathPart(String text) {
        String value = isBlank(text) ? "unknown" : text.trim();
        value = value.replaceAll("[^A-Za-z0-9_.-]", "_");
        if (value.length() > 96) {
            value = value.substring(0, 96);
        }
        return value.length() == 0 ? "unknown" : value;
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

    private static String jsonError(String code) {
        return "{\"ok\":false,\"error\":\"" + code + "\"}";
    }

    private static void closeQuietly(Object closeable) {
        if (closeable == null) {
            return;
        }
        try {
            if (closeable instanceof Socket) {
                ((Socket) closeable).close();
            } else if (closeable instanceof BufferedReader) {
                ((BufferedReader) closeable).close();
            } else if (closeable instanceof BufferedWriter) {
                ((BufferedWriter) closeable).close();
            }
        } catch (IOException ignored) {
        }
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
        File logDir = new File(new File(System.getProperty("user.dir"), "logs"), "fireball-share-server");

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
                } else if ("--log-dir".equals(arg) && i + 1 < args.length) {
                    config.logDir = new File(args[++i]);
                }
            }
            return config;
        }
    }
}
