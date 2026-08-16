package com.example.fireballpredictor.share;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ShareServer {
    private static final int DEFAULT_PORT = 18989;
    private static final int DEFAULT_WEB_PORT = 18990;
    private static final String DEFAULT_ROOM = "default";
    private static final long DEFAULT_TTL_MILLIS = 10L * 60L * 1000L;
    private static final long DEFAULT_GAME_IDLE_SPLIT_MILLIS = 75L * 1000L;
    private static final int DEFAULT_MAX_SAMPLES_PER_GAME = 600;
    private static final int MAX_LINE_CHARS = 32 * 1024;
    private static final int WEB_DETAIL_MAX_LINES = 800;
    private static final long DEFAULT_STATS_CACHE_MILLIS = 10L * 60L * 1000L;
    private static final int HYPIXEL_BEDWARS_EXP_PER_PRESTIGE = 487000;
    private static final String DEFAULT_HYPIXEL_API_KEY = "92e1efcb-dce7-4087-86fb-4dcf981b4ea2";
    private static final String EXTRA_HYPIXEL_API_KEY = "322cd413-9d3b-486f-84de-9716cca33416";
    private static final String SECOND_EXTRA_HYPIXEL_API_KEY = "429b2968-de16-4c23-84c4-c6856e8465f8";
    private static final String DEFAULT_HYPIXEL_API_KEYS = DEFAULT_HYPIXEL_API_KEY + ","
            + EXTRA_HYPIXEL_API_KEY + "," + SECOND_EXTRA_HYPIXEL_API_KEY;

    private final Object lock = new Object();
    private final Object logLock = new Object();
    private final Map<String, GameSession> activeSessionsByGame = new LinkedHashMap<String, GameSession>();
    private final Map<String, StatsEntry> statsCacheByUuid = new LinkedHashMap<String, StatsEntry>();
    private final long ttlMillis;
    private final long gameIdleSplitMillis;
    private final int maxSamplesPerGame;
    private final File logDir;
    private final String[] hypixelApiKeys;
    private final long statsCacheMillis;
    private final ExecutorService workers = Executors.newCachedThreadPool();
    private int hypixelApiKeyIndex;

    private ShareServer(long ttlMillis, long gameIdleSplitMillis, int maxSamplesPerGame, File logDir,
                        String[] hypixelApiKeys, long statsCacheMillis) {
        this.ttlMillis = ttlMillis;
        this.gameIdleSplitMillis = gameIdleSplitMillis;
        this.maxSamplesPerGame = maxSamplesPerGame;
        this.logDir = logDir;
        this.hypixelApiKeys = hypixelApiKeys == null ? new String[0] : hypixelApiKeys;
        this.statsCacheMillis = statsCacheMillis;
    }

    public static void main(String[] args) throws Exception {
        Config config = Config.parse(args);
        final ShareServer app = new ShareServer(config.ttlMillis, config.gameIdleSplitMillis,
                config.maxSamplesPerGame, config.logDir, config.hypixelApiKeys, config.statsCacheMillis);
        app.ensureLogRoot();
        if (config.webEnabled) {
            app.startWebServer(config.webHost, config.webPort);
        }

        ServerSocket server = new ServerSocket();
        server.bind(new InetSocketAddress(config.host, config.port));
        System.out.println("Fireball share server listening on tcp://" + config.host + ":" + config.port);
        System.out.println("每局日志目录: " + config.logDir.getAbsolutePath());
        System.out.println("Protocol: PING | STATS <uuid> [name] | PUSH <json> | PULL <gameId> [since] [excludeSenderId] [room] | CLEAR [gameId] [room]");
        while (true) {
            final Socket socket = server.accept();
            app.workers.execute(new Runnable() {
                @Override
                public void run() {
                    app.handleTcp(socket);
                }
            });
        }
    }

    private void startWebServer(String host, int port) throws IOException {
        final ServerSocket webServer = new ServerSocket();
        webServer.bind(new InetSocketAddress(host, port));
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        final Socket socket = webServer.accept();
                        workers.execute(new Runnable() {
                            @Override
                            public void run() {
                                handleHttp(socket);
                            }
                        });
                    } catch (IOException e) {
                        System.err.println("Web server accept failed: " + e);
                    }
                }
            }
        }, "fireball-share-web");
        thread.setDaemon(true);
        thread.start();
        System.out.println("日志网页: http://" + printableHost(host) + ":" + port + "/");
    }

    private void handleTcp(Socket socket) {
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
            return "{\"ok\":true,\"name\":\"fireball-share-server\",\"protocol\":\"tcp\",\"web\":true,\"statsProxy\":true}";
        }
        if (startsWithCommand(line, "STATS")) {
            return handleStats(commandParts(line, "STATS"));
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
        String enriched;
        File logFile;
        synchronized (lock) {
            pruneLocked(now);
            GameSession session = activeSessionLocked(room, gameId, now, true);
            enriched = enrichJson(body, now, session.sessionId);
            session.lastTime = now;
            session.samples.add(new Sample(now, senderId, enriched));
            while (session.samples.size() > maxSamplesPerGame) {
                session.samples.remove(0);
            }
            logFile = session.logFile;
        }
        appendSampleLog(room, gameId, logFile, enriched);
        return "{\"ok\":true,\"serverTime\":" + now + "}";
    }

    private void appendSampleLog(String room, String gameId, File file, String json) {
        synchronized (logLock) {
            BufferedWriter writer = null;
            try {
                File parent = file.getParentFile();
                if (!parent.isDirectory() && !parent.mkdirs()) {
                    throw new IOException("failed to create log dir: " + parent);
                }
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
            GameSession session = activeSessionLocked(room, gameId, now, false);
            session.lastTime = now;
            for (Sample sample : session.samples) {
                if (sample.serverTime <= since) {
                    continue;
                }
                if (!isBlank(excludeSenderId) && excludeSenderId.equals(sample.senderId)) {
                    continue;
                }
                result.add(sample.json);
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
                activeSessionsByGame.remove(gameKey(room, gameId));
            } else if (!isBlank(room)) {
                Iterator<String> iterator = activeSessionsByGame.keySet().iterator();
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

    private String handleStats(String[] parts) {
        if (parts.length < 1 || isBlank(parts[0])) {
            return jsonError("missing_uuid");
        }
        String uuid = normalizeUuid(parts[0]);
        String name = parts.length >= 2 ? parts[1] : "";
        if (uuid.length() == 0) {
            return jsonError("invalid_uuid");
        }

        long now = System.currentTimeMillis();
        StatsEntry cached;
        synchronized (lock) {
            cached = statsCacheByUuid.get(uuid);
            if (cached != null && now - cached.timestamp <= statsCacheMillis) {
                return cached.withName(name).toJson(true);
            }
        }

        try {
            StatsEntry fresh = fetchHypixelStats(uuid, name);
            synchronized (lock) {
                statsCacheByUuid.put(uuid, fresh);
            }
            return fresh.toJson(false);
        } catch (StatsHttpException e) {
            return "{\"ok\":false,\"error\":\"hypixel_http_" + e.code + "\",\"http\":" + e.code
                    + ",\"body\":\"" + jsonEscape(shortText(e.body, 180)) + "\"}";
        } catch (Throwable t) {
            return "{\"ok\":false,\"error\":\"stats_fetch_failed\",\"message\":\""
                    + jsonEscape(shortText(String.valueOf(t.getMessage()), 180)) + "\"}";
        }
    }

    private StatsEntry fetchHypixelStats(String uuid, String name) throws Exception {
        if (hypixelApiKeys.length == 0) {
            throw new IOException("no_hypixel_api_key");
        }
        Exception lastError = null;
        int attempts = Math.max(1, hypixelApiKeys.length);
        for (int attempt = 0; attempt < attempts; attempt++) {
            String apiKey = nextHypixelApiKey();
            try {
                return fetchHypixelStatsWithKey(apiKey, uuid, name);
            } catch (StatsHttpException e) {
                lastError = e;
                System.err.println("Hypixel stats key failed for " + name + ": http=" + e.code
                        + " key=" + maskApiKey(apiKey) + " body=" + shortText(e.body, 160));
                if (e.code != 403 || e.body == null || e.body.indexOf("Invalid API key") < 0) {
                    throw e;
                }
            }
        }
        throw lastError == null ? new IOException("hypixel_stats_failed") : lastError;
    }

    private StatsEntry fetchHypixelStatsWithKey(String apiKey, String uuid, String name) throws Exception {
        URL url = new URL("https://api.hypixel.net/v2/player?uuid=" + uuid);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(7000);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("API-Key", apiKey);
        connection.setRequestProperty("User-Agent", "FireballShareServer/1.8.9");

        int code = connection.getResponseCode();
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream(),
                StandardCharsets.UTF_8));
        StringBuilder body = new StringBuilder();
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
        } finally {
            closeQuietly(reader);
            connection.disconnect();
        }
        if (code < 200 || code >= 300) {
            throw new StatsHttpException(code, body.toString());
        }

        String json = body.toString();
        String bedwars = readJsonObject(json, "Bedwars");
        if (bedwars.length() == 0) {
            bedwars = json;
        }
        double experience = readJsonNumber(bedwars, "Experience", 0.0D);
        long kills = (long) readJsonNumber(bedwars, "kills_bedwars", 0.0D);
        long deaths = (long) readJsonNumber(bedwars, "deaths_bedwars", 0.0D);
        long bedsBroken = (long) readJsonNumber(bedwars, "beds_broken_bedwars", 0.0D);
        double kd = deaths <= 0L ? (double) kills : (double) kills / (double) deaths;
        return new StatsEntry(uuid, name, System.currentTimeMillis(), bedwarsStars(experience),
                kills, bedsBroken, kd);
    }

    private String nextHypixelApiKey() {
        synchronized (lock) {
            String key = hypixelApiKeys[hypixelApiKeyIndex % hypixelApiKeys.length];
            hypixelApiKeyIndex++;
            return key;
        }
    }

    private GameSession activeSessionLocked(String room, String gameId, long now, boolean willWrite) {
        String key = gameKey(room, gameId);
        GameSession session = activeSessionsByGame.get(key);
        if (session == null || now - session.lastTime > gameIdleSplitMillis) {
            session = new GameSession(room, gameId, newSessionId(gameId, now), now, willWrite);
            activeSessionsByGame.put(key, session);
        }
        return session;
    }

    private void pruneLocked(long now) {
        Iterator<Map.Entry<String, GameSession>> games = activeSessionsByGame.entrySet().iterator();
        while (games.hasNext()) {
            GameSession session = games.next().getValue();
            if (now - session.lastTime > ttlMillis) {
                games.remove();
            }
        }
    }

    private void ensureLogRoot() throws IOException {
        if (!logDir.isDirectory() && !logDir.mkdirs()) {
            throw new IOException("failed to create log root: " + logDir);
        }
    }

    private String newSessionId(String gameId, long now) {
        String base = fileTime(now) + "-" + safePathPart(gameId);
        return base.length() > 120 ? base.substring(0, 120) : base;
    }

    private File logFileFor(String room, String sessionId) {
        return new File(new File(logDir, safePathPart(room)), safePathPart(sessionId) + ".jsonl");
    }

    private void handleHttp(Socket socket) {
        BufferedReader input = null;
        try {
            socket.setSoTimeout(8000);
            input = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            String requestLine = input.readLine();
            if (requestLine == null || requestLine.length() == 0) {
                sendHttp(socket, 400, "text/plain; charset=utf-8", "Bad Request");
                return;
            }
            while (true) {
                String header = input.readLine();
                if (header == null || header.length() == 0) {
                    break;
                }
            }
            String[] parts = requestLine.split(" ");
            if (parts.length < 2 || !"GET".equalsIgnoreCase(parts[0])) {
                sendHttp(socket, 405, "text/plain; charset=utf-8", "Only GET is supported");
                return;
            }
            String target = parts[1];
            String path = targetPath(target);
            Map<String, String> query = queryParams(target);
            if ("/".equals(path) || "/index.html".equals(path)) {
                sendHttp(socket, 200, "text/html; charset=utf-8", renderIndexPage());
            } else if ("/game".equals(path)) {
                sendHttp(socket, 200, "text/html; charset=utf-8",
                        renderGamePage(query.get("room"), query.get("file")));
            } else if ("/raw".equals(path)) {
                File file = resolveLogFile(query.get("room"), query.get("file"));
                if (file == null) {
                    sendHttp(socket, 404, "text/plain; charset=utf-8", "not found");
                } else {
                    sendHttp(socket, 200, "text/plain; charset=utf-8", readWholeFile(file, 2 * 1024 * 1024));
                }
            } else {
                sendHttp(socket, 404, "text/plain; charset=utf-8", "not found");
            }
        } catch (Throwable t) {
            try {
                sendHttp(socket, 500, "text/plain; charset=utf-8", "server error: " + t);
            } catch (IOException ignored) {
            }
        } finally {
            closeQuietly(input);
            closeQuietly(socket);
        }
    }

    private String renderIndexPage() {
        List<LogFileInfo> files = listLogFiles();
        StringBuilder html = new StringBuilder(pageStart("保护共享日志"));
        html.append("<div class=\"toolbar\"><a href=\"/\">刷新</a><span>日志目录：")
                .append(htmlEscape(logDir.getAbsolutePath())).append("</span></div>");
        html.append("<table><thead><tr><th>房间</th><th>单局日志</th><th>样本数</th><th>最后更新</th><th>操作</th></tr></thead><tbody>");
        if (files.isEmpty()) {
            html.append("<tr><td colspan=\"5\" class=\"empty\">还没有收到保护样本。</td></tr>");
        }
        for (LogFileInfo file : files) {
            String room = urlEncode(file.room);
            String name = urlEncode(file.fileName);
            html.append("<tr><td>").append(htmlEscape(file.room)).append("</td><td>")
                    .append(htmlEscape(file.fileName)).append("</td><td>")
                    .append(file.lines).append("</td><td>")
                    .append(htmlEscape(humanTime(file.lastModified))).append("</td><td>")
                    .append("<a href=\"/game?room=").append(room).append("&file=").append(name).append("\">查看</a>")
                    .append(" <a href=\"/raw?room=").append(room).append("&file=").append(name).append("\">原始</a>")
                    .append("</td></tr>");
        }
        html.append("</tbody></table>").append(pageEnd());
        return html.toString();
    }

    private String renderGamePage(String room, String fileName) {
        File file = resolveLogFile(room, fileName);
        StringBuilder html = new StringBuilder(pageStart("单局详情"));
        html.append("<div class=\"toolbar\"><a href=\"/\">返回列表</a>");
        if (file != null) {
            html.append("<a href=\"/raw?room=").append(urlEncode(room)).append("&file=")
                    .append(urlEncode(fileName)).append("\">原始 JSONL</a>");
        }
        html.append("</div>");
        if (file == null) {
            html.append("<p class=\"empty\">找不到这个日志文件。</p>").append(pageEnd());
            return html.toString();
        }
        List<String> lines = readLines(file, WEB_DETAIL_MAX_LINES);
        html.append("<h2>").append(htmlEscape(fileName)).append("</h2>");
        html.append("<p class=\"muted\">只显示最后 ").append(WEB_DETAIL_MAX_LINES)
                .append(" 条，样本越新越靠上。</p>");
        html.append("<table><thead><tr><th>时间</th><th>队伍</th><th>发送端</th><th>敌方套装</th><th>我方剑</th><th>锋利</th><th>暴击</th><th>原始伤害</th><th>护甲点</th><th>实际伤害</th><th>预测伤害</th><th>误差</th><th>推算保护</th></tr></thead><tbody>");
        for (int i = lines.size() - 1; i >= 0; i--) {
            String line = lines.get(i);
            long serverTime = parseLong(jsonNumberString(line, "serverTime"), 0L);
            html.append("<tr><td>").append(htmlEscape(serverTime > 0L ? humanTime(serverTime) : ""))
                    .append("</td><td>").append(htmlEscape(jsonString(line, "team")))
                    .append("</td><td>").append(htmlEscape(shortSender(jsonString(line, "senderId"))))
                    .append("</td><td>").append(htmlEscape(jsonString(line, "armorLabel")))
                    .append("</td><td>").append(htmlEscape(jsonString(line, "swordLabel")))
                    .append("</td><td>").append(htmlEscape(jsonNumberString(line, "sharpnessLevel")))
                    .append("</td><td>").append(htmlEscape(jsonBooleanString(line, "critical")))
                    .append("</td><td>").append(htmlEscape(jsonNumberString(line, "rawDamage")))
                    .append("</td><td>").append(htmlEscape(jsonNumberString(line, "armorPoints")))
                    .append("</td><td>").append(htmlEscape(jsonNumberString(line, "observedDamage")))
                    .append("</td><td>").append(htmlEscape(jsonNumberString(line, "predictedDamage")))
                    .append("</td><td>").append(htmlEscape(jsonNumberString(line, "error")))
                    .append("</td><td>").append(htmlEscape(jsonNumberString(line, "guessedLevel")))
                    .append("</td></tr>");
        }
        html.append("</tbody></table>").append(pageEnd());
        return html.toString();
    }

    private List<LogFileInfo> listLogFiles() {
        List<LogFileInfo> result = new ArrayList<LogFileInfo>();
        File[] rooms = logDir.listFiles();
        if (rooms == null) {
            return result;
        }
        for (File roomDir : rooms) {
            if (!roomDir.isDirectory()) {
                continue;
            }
            File[] files = roomDir.listFiles();
            if (files == null) {
                continue;
            }
            for (File file : files) {
                if (file.isFile() && file.getName().endsWith(".jsonl")) {
                    result.add(new LogFileInfo(roomDir.getName(), file.getName(), countLines(file), file.lastModified()));
                }
            }
        }
        for (int i = 0; i < result.size(); i++) {
            for (int j = i + 1; j < result.size(); j++) {
                if (result.get(j).lastModified > result.get(i).lastModified) {
                    LogFileInfo tmp = result.get(i);
                    result.set(i, result.get(j));
                    result.set(j, tmp);
                }
            }
        }
        return result;
    }

    private File resolveLogFile(String room, String fileName) {
        if (isBlank(room) || isBlank(fileName)) {
            return null;
        }
        try {
            File roomDir = new File(logDir, safePathPart(room));
            File file = new File(roomDir, safePathPart(fileName));
            String rootPath = logDir.getCanonicalPath();
            String filePath = file.getCanonicalPath();
            if (!filePath.startsWith(rootPath + File.separator) || !file.isFile()) {
                return null;
            }
            return file;
        } catch (IOException ignored) {
            return null;
        }
    }

    private static String pageStart(String title) {
        return "<!doctype html><html><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
                + "<title>" + htmlEscape(title) + "</title><style>"
                + "body{font-family:Segoe UI,Microsoft YaHei,Arial,sans-serif;margin:24px;background:#f7f7f7;color:#1f2933}"
                + "h1{font-size:24px;margin:0 0 16px}h2{font-size:18px;margin:16px 0 8px}.toolbar{display:flex;gap:16px;align-items:center;margin:0 0 16px}"
                + "a{color:#0366d6;text-decoration:none}table{border-collapse:collapse;width:100%;background:#fff;border:1px solid #ddd}"
                + "th,td{padding:8px 10px;border-bottom:1px solid #eee;text-align:left;font-size:14px}th{background:#f0f3f6}.empty{color:#666;text-align:center}.muted{color:#666}"
                + "</style></head><body><h1>" + htmlEscape(title) + "</h1>";
    }

    private static String pageEnd() {
        return "</body></html>";
    }

    private static void sendHttp(Socket socket, int status, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        String reason = status == 200 ? "OK" : status == 404 ? "Not Found" : status == 405 ? "Method Not Allowed" : "Error";
        OutputStream output = socket.getOutputStream();
        String header = "HTTP/1.1 " + status + " " + reason + "\r\n"
                + "Content-Type: " + contentType + "\r\n"
                + "Content-Length: " + bytes.length + "\r\n"
                + "Cache-Control: no-store\r\n"
                + "Connection: close\r\n\r\n";
        output.write(header.getBytes(StandardCharsets.US_ASCII));
        output.write(bytes);
        output.flush();
    }

    private static String targetPath(String target) {
        int query = target.indexOf('?');
        return query >= 0 ? target.substring(0, query) : target;
    }

    private static Map<String, String> queryParams(String target) {
        Map<String, String> result = new LinkedHashMap<String, String>();
        int query = target.indexOf('?');
        if (query < 0 || query + 1 >= target.length()) {
            return result;
        }
        String[] pairs = target.substring(query + 1).split("&");
        for (String pair : pairs) {
            int equals = pair.indexOf('=');
            String key = equals >= 0 ? pair.substring(0, equals) : pair;
            String value = equals >= 0 ? pair.substring(equals + 1) : "";
            result.put(urlDecode(key), urlDecode(value));
        }
        return result;
    }

    private static List<String> readLines(File file, int maxLines) {
        List<String> result = new ArrayList<String>();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                result.add(line);
                if (result.size() > maxLines) {
                    result.remove(0);
                }
            }
        } catch (IOException ignored) {
        } finally {
            closeQuietly(reader);
        }
        return result;
    }

    private static String readWholeFile(File file, int maxBytes) {
        FileInputStream input = null;
        try {
            input = new FileInputStream(file);
            byte[] bytes = new byte[(int) Math.min(file.length(), (long) maxBytes)];
            int read = input.read(bytes);
            return read <= 0 ? "" : new String(bytes, 0, read, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        } finally {
            closeQuietly(input);
        }
    }

    private static int countLines(File file) {
        BufferedReader reader = null;
        int count = 0;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
            while (reader.readLine() != null) {
                count++;
            }
        } catch (IOException ignored) {
        } finally {
            closeQuietly(reader);
        }
        return count;
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

    private static String jsonNumberString(String json, String field) {
        String needle = "\"" + field + "\"";
        int index = json.indexOf(needle);
        if (index < 0) {
            return "";
        }
        int colon = json.indexOf(':', index + needle.length());
        if (colon < 0) {
            return "";
        }
        int start = colon + 1;
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) {
            start++;
        }
        int end = start;
        while (end < json.length()) {
            char c = json.charAt(end);
            if ((c >= '0' && c <= '9') || c == '-' || c == '.') {
                end++;
            } else {
                break;
            }
        }
        return end > start ? json.substring(start, end) : "";
    }

    private static String jsonBooleanString(String json, String field) {
        String needle = "\"" + field + "\"";
        int index = json.indexOf(needle);
        if (index < 0) {
            return "";
        }
        int colon = json.indexOf(':', index + needle.length());
        if (colon < 0) {
            return "";
        }
        int start = colon + 1;
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) {
            start++;
        }
        if (json.regionMatches(true, start, "true", 0, 4)) {
            return "true";
        }
        if (json.regionMatches(true, start, "false", 0, 5)) {
            return "false";
        }
        return "";
    }

    private static double readJsonNumber(String json, String field, double fallback) {
        String value = jsonNumberString(json, field);
        if (value.length() == 0) {
            return fallback;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static String readJsonObject(String json, String field) {
        String needle = "\"" + field + "\"";
        int fieldIndex = json.indexOf(needle);
        if (fieldIndex < 0) {
            return "";
        }
        int braceStart = json.indexOf('{', fieldIndex + needle.length());
        if (braceStart < 0) {
            return "";
        }
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int i = braceStart; i < json.length(); i++) {
            char ch = json.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (ch == '\\' && inString) {
                escaped = true;
                continue;
            }
            if (ch == '"') {
                inString = !inString;
                continue;
            }
            if (inString) {
                continue;
            }
            if (ch == '{') {
                depth++;
            } else if (ch == '}') {
                depth--;
                if (depth == 0) {
                    return json.substring(braceStart, i + 1);
                }
            }
        }
        return "";
    }

    private static String enrichJson(String json, long serverTime, String sessionId) {
        String trimmed = json.trim();
        String withoutEnd = trimmed.substring(0, trimmed.length() - 1).trim();
        String extra = "\"sessionId\":\"" + jsonEscape(sessionId) + "\",\"serverTime\":" + serverTime;
        if (withoutEnd.endsWith("{")) {
            return withoutEnd + extra + "}";
        }
        return withoutEnd + "," + extra + "}";
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
        if (value.length() > 128) {
            value = value.substring(0, 128);
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

    private static String normalizeUuid(String text) {
        if (text == null) {
            return "";
        }
        String value = text.trim().replace("-", "");
        return value.matches("[A-Fa-f0-9]{32}") ? value.toLowerCase(Locale.US) : "";
    }

    private static String[] parseHypixelApiKeys(String text) {
        if (text == null) {
            return new String[0];
        }
        String[] parts = text.split("[,;\\s]+");
        List<String> keys = new ArrayList<String>();
        for (String part : parts) {
            String key = part == null ? "" : part.trim();
            if (key.length() > 0 && !keys.contains(key)) {
                keys.add(key);
            }
        }
        return keys.toArray(new String[keys.size()]);
    }

    private static String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() <= 8) {
            return "****";
        }
        return apiKey.substring(0, 4) + "..." + apiKey.substring(apiKey.length() - 4);
    }

    private static String shortText(String text, int max) {
        if (text == null) {
            return "";
        }
        String value = text.replace('\n', ' ').replace('\r', ' ');
        return value.length() <= max ? value : value.substring(0, max);
    }

    private static int bedwarsStars(double experience) {
        int prestiges = (int) (experience / HYPIXEL_BEDWARS_EXP_PER_PRESTIGE);
        double exp = experience - prestiges * HYPIXEL_BEDWARS_EXP_PER_PRESTIGE;
        int level = 0;
        if (exp >= 500.0D) {
            level++;
            exp -= 500.0D;
        }
        if (exp >= 1000.0D) {
            level++;
            exp -= 1000.0D;
        }
        if (exp >= 2000.0D) {
            level++;
            exp -= 2000.0D;
        }
        if (exp >= 3500.0D) {
            level++;
            exp -= 3500.0D;
        }
        if (level >= 4 && exp > 0.0D) {
            level += (int) (exp / 5000.0D);
        }
        if (level > 99) {
            level = 99;
        }
        return prestiges * 100 + level;
    }

    private static String jsonError(String code) {
        return "{\"ok\":false,\"error\":\"" + code + "\"}";
    }

    private static String jsonEscape(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String htmlEscape(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private static String urlEncode(String text) {
        try {
            return URLEncoder.encode(text == null ? "" : text, "UTF-8");
        } catch (Exception ignored) {
            return "";
        }
    }

    private static String urlDecode(String text) {
        try {
            return URLDecoder.decode(text == null ? "" : text, "UTF-8");
        } catch (Exception ignored) {
            return "";
        }
    }

    private static String shortSender(String senderId) {
        if (senderId == null || senderId.length() <= 14) {
            return senderId;
        }
        return senderId.substring(0, 14) + "...";
    }

    private static String fileTime(long time) {
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US);
        format.setTimeZone(TimeZone.getDefault());
        return format.format(new Date(time));
    }

    private static String humanTime(long time) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        format.setTimeZone(TimeZone.getDefault());
        return format.format(new Date(time));
    }

    private static String printableHost(String host) {
        if ("0.0.0.0".equals(host) || "::".equals(host)) {
            try {
                return InetAddress.getLocalHost().getHostAddress();
            } catch (Exception ignored) {
                return "127.0.0.1";
            }
        }
        return host;
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
            } else if (closeable instanceof FileInputStream) {
                ((FileInputStream) closeable).close();
            }
        } catch (IOException ignored) {
        }
    }

    private final class GameSession {
        final String room;
        final String gameId;
        final String sessionId;
        final long startTime;
        final File logFile;
        final List<Sample> samples = new ArrayList<Sample>();
        long lastTime;

        GameSession(String room, String gameId, String sessionId, long now, boolean willWrite) {
            this.room = room;
            this.gameId = gameId;
            this.sessionId = sessionId;
            this.startTime = now;
            this.lastTime = now;
            this.logFile = logFileFor(room, sessionId);
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

    private static final class LogFileInfo {
        final String room;
        final String fileName;
        final int lines;
        final long lastModified;

        LogFileInfo(String room, String fileName, int lines, long lastModified) {
            this.room = room;
            this.fileName = fileName;
            this.lines = lines;
            this.lastModified = lastModified;
        }
    }

    private static final class StatsEntry {
        final String uuid;
        final String name;
        final long timestamp;
        final int stars;
        final long kills;
        final long bedsBroken;
        final double kd;

        StatsEntry(String uuid, String name, long timestamp, int stars, long kills, long bedsBroken, double kd) {
            this.uuid = uuid;
            this.name = name == null ? "" : name;
            this.timestamp = timestamp;
            this.stars = stars;
            this.kills = kills;
            this.bedsBroken = bedsBroken;
            this.kd = kd;
        }

        StatsEntry withName(String newName) {
            return isBlank(newName) ? this : new StatsEntry(uuid, newName, timestamp, stars, kills, bedsBroken, kd);
        }

        boolean isProbablyNick() {
            return stars == 0 && kills == 0L && bedsBroken == 0L && Math.abs(kd) < 0.0001D;
        }

        String toJson(boolean cached) {
            return "{\"ok\":true,\"cached\":" + cached
                    + ",\"uuid\":\"" + jsonEscape(uuid) + "\""
                    + ",\"name\":\"" + jsonEscape(name) + "\""
                    + ",\"timestamp\":" + timestamp
                    + ",\"stars\":" + stars
                    + ",\"kills\":" + kills
                    + ",\"bedsBroken\":" + bedsBroken
                    + ",\"kd\":" + String.format(Locale.US, "%.4f", Double.valueOf(kd))
                    + ",\"probablyNick\":" + isProbablyNick() + "}";
        }
    }

    private static final class StatsHttpException extends Exception {
        final int code;
        final String body;

        StatsHttpException(int code, String body) {
            super("Hypixel HTTP " + code + ": " + body);
            this.code = code;
            this.body = body;
        }
    }

    private static final class Config {
        String host = "0.0.0.0";
        int port = DEFAULT_PORT;
        boolean webEnabled = true;
        String webHost = "0.0.0.0";
        int webPort = DEFAULT_WEB_PORT;
        long ttlMillis = DEFAULT_TTL_MILLIS;
        long gameIdleSplitMillis = DEFAULT_GAME_IDLE_SPLIT_MILLIS;
        int maxSamplesPerGame = DEFAULT_MAX_SAMPLES_PER_GAME;
        File logDir = new File(new File(System.getProperty("user.dir"), "logs"), "fireball-share-server");
        String[] hypixelApiKeys = parseHypixelApiKeys(DEFAULT_HYPIXEL_API_KEYS);
        long statsCacheMillis = DEFAULT_STATS_CACHE_MILLIS;

        static Config parse(String[] args) {
            Config config = new Config();
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if ("--host".equals(arg) && i + 1 < args.length) {
                    config.host = args[++i];
                } else if ("--port".equals(arg) && i + 1 < args.length) {
                    config.port = (int) parseLong(args[++i], DEFAULT_PORT);
                } else if ("--web-host".equals(arg) && i + 1 < args.length) {
                    config.webHost = args[++i];
                } else if ("--web-port".equals(arg) && i + 1 < args.length) {
                    config.webPort = (int) parseLong(args[++i], DEFAULT_WEB_PORT);
                } else if ("--no-web".equals(arg)) {
                    config.webEnabled = false;
                } else if ("--ttl-seconds".equals(arg) && i + 1 < args.length) {
                    config.ttlMillis = parseLong(args[++i], DEFAULT_TTL_MILLIS / 1000L) * 1000L;
                } else if ("--game-idle-seconds".equals(arg) && i + 1 < args.length) {
                    config.gameIdleSplitMillis = parseLong(args[++i], DEFAULT_GAME_IDLE_SPLIT_MILLIS / 1000L) * 1000L;
                } else if ("--max-samples".equals(arg) && i + 1 < args.length) {
                    config.maxSamplesPerGame = (int) parseLong(args[++i], DEFAULT_MAX_SAMPLES_PER_GAME);
                } else if ("--log-dir".equals(arg) && i + 1 < args.length) {
                    config.logDir = new File(args[++i]);
                } else if ("--hypixel-api-key".equals(arg) && i + 1 < args.length) {
                    config.hypixelApiKeys = parseHypixelApiKeys(args[++i]);
                } else if ("--hypixel-api-keys".equals(arg) && i + 1 < args.length) {
                    config.hypixelApiKeys = parseHypixelApiKeys(args[++i]);
                } else if ("--stats-cache-seconds".equals(arg) && i + 1 < args.length) {
                    config.statsCacheMillis = parseLong(args[++i], DEFAULT_STATS_CACHE_MILLIS / 1000L) * 1000L;
                }
            }
            return config;
        }
    }
}
