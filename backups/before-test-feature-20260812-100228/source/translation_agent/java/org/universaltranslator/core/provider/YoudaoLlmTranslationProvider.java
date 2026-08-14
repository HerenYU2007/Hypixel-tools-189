package org.universaltranslator.core.provider;

import org.universaltranslator.core.TranslationOutputValidator;
import org.universaltranslator.core.TranslationProvider;
import org.universaltranslator.core.TranslationRequest;
import org.universaltranslator.core.net.EndpointPolicy;
import org.universaltranslator.core.net.JsonStrings;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

/** Youdao Zhiyun LLM translation provider. */
public final class YoudaoLlmTranslationProvider implements TranslationProvider {
    private final URI endpoint;
    private final String appKey;
    private final String appSecret;
    private final String handleOption;
    private final String prompt;
    private final int connectTimeoutMillis;
    private final int readTimeoutMillis;

    public YoudaoLlmTranslationProvider(
            String endpoint,
            String appKey,
            String appSecret,
            String handleOption,
            String prompt
    ) {
        this.endpoint = EndpointPolicy.requireSafeEndpoint(endpoint);
        this.appKey = requireText("youdao-app-key", appKey);
        this.appSecret = requireText("youdao-app-secret", appSecret);
        this.handleOption = handleOption == null || handleOption.trim().isEmpty()
                ? "3"
                : handleOption.trim();
        this.prompt = prompt == null ? "" : prompt.trim();
        this.connectTimeoutMillis = 5000;
        this.readTimeoutMillis = 30000;
    }

    @Override
    public String id() {
        return "youdao-llm:" + handleOption;
    }

    @Override
    public String translate(TranslationRequest request) throws Exception {
        String text = request.getText();
        String salt = UUID.randomUUID().toString();
        String curtime = String.valueOf(System.currentTimeMillis() / 1000L);
        String sign = sha256(appKey + truncateForSign(text) + salt + curtime + appSecret);

        StringBuilder form = new StringBuilder(text.length() + 320)
                .append("appKey=").append(url(appKey))
                .append("&salt=").append(url(salt))
                .append("&signType=v3")
                .append("&sign=").append(url(sign))
                .append("&curtime=").append(url(curtime))
                .append("&i=").append(url(text))
                .append("&from=").append(url(normalizeSource(request.getSourceLanguage())))
                .append("&to=").append(url(normalizeTarget(request.getTargetLanguage())))
                .append("&streamType=full")
                .append("&handleOption=").append(url(handleOption));
        if (!prompt.isEmpty()) {
            form.append("&prompt=").append(url(prompt));
        }

        String response = postForm(form.toString());
        String translated = readLastFullTranslation(response);
        if (translated == null || translated.trim().isEmpty()) {
            String error = JsonStrings.readStringField(response, "message");
            throw new IOException("Youdao response did not contain transFull"
                    + (error == null ? "" : ": " + error));
        }
        return TranslationOutputValidator.requireValid(text, translated);
    }

    private String postForm(String formBody) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) endpoint.toURL().openConnection();
        try {
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(connectTimeoutMillis);
            connection.setReadTimeout(readTimeoutMillis);
            connection.setDoOutput(true);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestProperty("Accept", "text/event-stream, application/json, */*");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
            connection.setRequestProperty("Connection", "keep-alive");
            connection.setRequestProperty("User-Agent", "MCAutoTranslationTool/1.0");

            byte[] body = formBody.getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(body.length);
            try (OutputStream output = connection.getOutputStream()) {
                output.write(body);
            }

            int status = connection.getResponseCode();
            InputStream stream = status >= 200 && status < 300
                    ? connection.getInputStream()
                    : connection.getErrorStream();
            String response = stream == null ? "" : readBounded(stream);
            if (status < 200 || status >= 300) {
                throw new IOException("Youdao returned HTTP " + status + ": " + response);
            }
            return response;
        } finally {
            connection.disconnect();
        }
    }

    private static String readLastFullTranslation(String response) {
        String result = null;
        String[] lines = response.split("\\r?\\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("data:")) {
                trimmed = trimmed.substring(5).trim();
            }
            if (trimmed.isEmpty() || "[DONE]".equals(trimmed)) {
                continue;
            }
            String code = JsonStrings.readStringField(trimmed, "code");
            if (code != null && !"0".equals(code)) {
                String message = JsonStrings.readStringField(trimmed, "message");
                throw new IllegalStateException("Youdao error " + code
                        + (message == null ? "" : ": " + message));
            }
            String full = JsonStrings.readStringField(trimmed, "transFull");
            if (full != null && !full.trim().isEmpty()) {
                result = full;
            }
        }
        if (result == null) {
            result = JsonStrings.readStringField(response, "transFull");
        }
        return result;
    }

    private static String truncateForSign(String text) {
        if (text.length() <= 20) {
            return text;
        }
        return text.substring(0, 10) + text.length() + text.substring(text.length() - 10);
    }

    private static String sha256(String text) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] bytes = digest.digest(text.getBytes(StandardCharsets.UTF_8));
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            String hex = Integer.toHexString(value & 0xff);
            if (hex.length() == 1) {
                result.append('0');
            }
            result.append(hex);
        }
        return result.toString();
    }

    private static String url(String text) throws IOException {
        return URLEncoder.encode(text, "UTF-8");
    }

    private static String normalizeSource(String language) {
        return language == null || language.trim().isEmpty() ? "auto" : normalizeLanguage(language);
    }

    private static String normalizeTarget(String language) {
        if (language == null || language.trim().isEmpty()) {
            return "zh-CHS";
        }
        return normalizeLanguage(language);
    }

    private static String normalizeLanguage(String language) {
        String normalized = language.trim().toLowerCase(java.util.Locale.ROOT).replace('_', '-');
        if ("zh".equals(normalized) || "zh-cn".equals(normalized) || "zh-chs".equals(normalized)) {
            return "zh-CHS";
        }
        if ("zh-tw".equals(normalized) || "zh-hk".equals(normalized) || "zh-cht".equals(normalized)) {
            return "zh-CHT";
        }
        int separator = normalized.indexOf('-');
        return separator < 0 ? normalized : normalized.substring(0, separator);
    }

    private static String requireText(String name, String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value.trim();
    }

    private static String readBounded(InputStream input) throws IOException {
        try (InputStream source = input; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int total = 0;
            int count;
            while ((count = source.read(buffer)) >= 0) {
                total += count;
                if (total > 1024 * 1024) {
                    throw new IOException("Youdao response exceeded 1 MiB");
                }
                output.write(buffer, 0, count);
            }
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
