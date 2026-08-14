package com.example.fireballpredictor.agent;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// 中文注释：这里集中放 agent 各功能共用的纯数据结构。
// 这些类不直接访问 Minecraft，也不负责字节码注入，拆出来后方便继续拆分功能逻辑。
final class HypixelStatsConfig {
    final boolean enabled;
    final String apiKey;
    final String[] apiKeys;
    final long cacheMillis;
    final File cacheFile;

    HypixelStatsConfig(boolean enabled, String apiKey, String[] apiKeys, long cacheMillis, File cacheFile) {
        this.enabled = enabled;
        this.apiKey = apiKey;
        this.apiKeys = apiKeys == null ? new String[0] : apiKeys;
        this.cacheMillis = cacheMillis;
        this.cacheFile = cacheFile;
    }
}

final class HypixelHttpException extends Exception {
    final int code;
    final String body;

    HypixelHttpException(int code, String body) {
        super("Hypixel HTTP " + code + ": " + body);
        this.code = code;
        this.body = body;
    }
}

final class PlayerSnapshot {
    final String uuid;
    final String name;
    final int color;

    PlayerSnapshot(String uuid, String name, int color) {
        this.uuid = uuid;
        this.name = name;
        this.color = color;
    }
}

final class DamageProbe {
    final String uuid;
    final String name;
    final int color;
    final Object target;
    final double beforeHealth;
    final ArmorProfile armor;
    final SwordProfile sword;
    final boolean critical;
    int ticks;

    DamageProbe(String uuid, String name, int color, Object target, double beforeHealth,
                ArmorProfile armor, SwordProfile sword, boolean critical) {
        this.uuid = uuid;
        this.name = name;
        this.color = color;
        this.target = target;
        this.beforeHealth = beforeHealth;
        this.armor = armor;
        this.sword = sword;
        this.critical = critical;
    }
}

final class ArmorProfile {
    final String label;
    final int points;

    ArmorProfile(String label, int points) {
        this.label = label;
        this.points = points;
    }
}

final class SwordProfile {
    final String label;
    final double damage;
    final int sharpnessLevel;

    SwordProfile(String label, double damage, int sharpnessLevel) {
        this.label = label;
        this.damage = damage;
        this.sharpnessLevel = sharpnessLevel;
    }
}

final class ProtectionGuess {
    final int level;
    final double predictedDamage;
    final double error;

    ProtectionGuess(int level, double predictedDamage, double error) {
        this.level = level;
        this.predictedDamage = predictedDamage;
        this.error = error;
    }
}

final class DamageRange {
    final double low;
    final double high;

    DamageRange(double low, double high) {
        this.low = low;
        this.high = high;
    }
}

final class ProtectionUpdate {
    final boolean accepted;
    final String message;

    ProtectionUpdate(boolean accepted, String message) {
        this.accepted = accepted;
        this.message = message;
    }
}

final class ProtectionSample {
    final double rawDamage;
    final int armorPoints;
    final double observedDamage;
    final int guessedLevel;

    ProtectionSample(double rawDamage, int armorPoints, double observedDamage, int guessedLevel) {
        this.rawDamage = rawDamage;
        this.armorPoints = armorPoints;
        this.observedDamage = observedDamage;
        this.guessedLevel = guessedLevel;
    }
}

final class RemoteProtectionSample {
    final String team;
    final double rawDamage;
    final int armorPoints;
    final double observedDamage;
    final int guessedLevel;

    RemoteProtectionSample(String team, double rawDamage, int armorPoints,
                           double observedDamage, int guessedLevel) {
        this.team = team;
        this.rawDamage = rawDamage;
        this.armorPoints = armorPoints;
        this.observedDamage = observedDamage;
        this.guessedLevel = guessedLevel;
    }

    // 中文注释：服务端返回的是一行 JSON，这里只解析保护共享需要的字段。
    static RemoteProtectionSample fromJson(String json) {
        String team = JsonMini.readString(json, "team");
        int guessedLevel = (int) JsonMini.readNumber(json, "guessedLevel", -1.0D);
        if (team.length() == 0 || guessedLevel < 0 || guessedLevel > 4) {
            return null;
        }
        return new RemoteProtectionSample(team,
                JsonMini.readNumber(json, "rawDamage", 0.0D),
                (int) JsonMini.readNumber(json, "armorPoints", 0.0D),
                JsonMini.readNumber(json, "observedDamage", 0.0D),
                guessedLevel);
    }
}

final class ProtectionProbability {
    final int firstLevel;
    final int firstPercent;
    final int secondLevel;
    final int secondPercent;

    ProtectionProbability(int firstLevel, int firstPercent, int secondLevel, int secondPercent) {
        this.firstLevel = firstLevel;
        this.firstPercent = firstPercent;
        this.secondLevel = secondLevel;
        this.secondPercent = secondPercent;
    }
}

final class HypixelStatsEntry {
    final String uuid;
    final String name;
    final long timestamp;
    final int stars;
    final long kills;
    final double kd;

    HypixelStatsEntry(String uuid, String name, long timestamp, int stars, long kills, double kd) {
        this.uuid = uuid;
        this.name = name;
        this.timestamp = timestamp;
        this.stars = stars;
        this.kills = kills;
        this.kd = kd;
    }

    HypixelStatsEntry withName(String newName) {
        return new HypixelStatsEntry(uuid, newName, timestamp, stars, kills, kd);
    }

    String toCache() {
        return timestamp + "|" + name + "|" + stars + "|" + kills + "|" + String.format(java.util.Locale.US, "%.2f", kd);
    }

    static HypixelStatsEntry fromCache(String uuid, String value) {
        if (value == null) {
            return null;
        }
        String[] parts = value.split("\\|");
        if (parts.length < 5) {
            return null;
        }
        try {
            return new HypixelStatsEntry(uuid, parts[1], Long.parseLong(parts[0]),
                    Integer.parseInt(parts[2]), Long.parseLong(parts[3]), Double.parseDouble(parts[4]));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}

final class GenInfo {
    final int x;
    final int y;
    final int z;
    int amount;
    final int distance;
    final boolean looking;

    GenInfo(int x, int y, int z, int amount, int distance, boolean looking) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.amount = amount;
        this.distance = distance;
        this.looking = looking;
    }
}

final class BedPos {
    final int x;
    final int y;
    final int z;

    BedPos(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
}

final class Vec {
    final double x;
    final double y;
    final double z;

    Vec(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    double length() {
        return Math.sqrt(x * x + y * y + z * z);
    }

    Vec normalize() {
        double length = length();
        if (length < 0.0001D) {
            return new Vec(0.0D, 0.0D, 0.0D);
        }
        return new Vec(x / length, y / length, z / length);
    }
}

final class JsonMini {
    private JsonMini() {
    }

    static double readNumber(String json, String field, double fallback) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(field) + "\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)").matcher(json);
        if (!matcher.find()) {
            return fallback;
        }
        try {
            return Double.parseDouble(matcher.group(1));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    static String readString(String json, String field) {
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
}
